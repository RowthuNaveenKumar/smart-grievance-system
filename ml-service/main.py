"""
main.py — FastAPI entry point for SGMS ML Classification Service.

Endpoints:
    GET  /health   — Service health + model status
    POST /predict  — Department prediction from complaint title + description

Response for /predict:
    {
        "predicted_department": "HOSTEL",     # Uppercase, matches DB department name
        "confidence": 0.87,                    # Real probability from Logistic Regression
        "high_confidence": true,               # confidence >= threshold (0.60)
        "predicted_priority": "MEDIUM"         # Safe default (priority not modelled)
    }
"""

from typing import Optional
import logging
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, field_validator

from model import predict, is_model_ready, CONFIDENCE_THRESHOLD

# ──────────────────────────────────────────────────────────────────────────────
# Logging
# ──────────────────────────────────────────────────────────────────────────────
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# ──────────────────────────────────────────────────────────────────────────────
# App
# ──────────────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="SGMS ML Service",
    description="ML Class prediction for Smart Grievance Management System",
    version="2.0.0",
)


# ──────────────────────────────────────────────────────────────────────────────
# DTOs
# ──────────────────────────────────────────────────────────────────────────────
class ComplaintRequest(BaseModel):
    """Input DTO — matches what the Spring Boot backend sends."""
    title: str
    complaint_text: str              # Description field from Java MLRequest

    @field_validator("title", "complaint_text", mode="before")
    @classmethod
    def must_not_be_empty(cls, v):
        if v is None:
            return ""
        return str(v).strip()


class PredictionResponse(BaseModel):
    """Output DTO — matches what Spring Boot's MLResponse expects."""
    predicted_class: Optional[str] = None
    confidence: float
    high_confidence: bool
    predicted_priority: str


# ──────────────────────────────────────────────────────────────────────────────
# Endpoints
# ──────────────────────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    """Health check endpoint — returns model readiness status."""
    model_ready = is_model_ready()
    return {
        "status": "ok" if model_ready else "degraded",
        "model_loaded": model_ready,
        "confidence_threshold": CONFIDENCE_THRESHOLD,
        "message": "Model is ready" if model_ready
                   else "Model not loaded — model.pkl missing",
    }


@app.post("/predict", response_model=PredictionResponse)
def predict_class(request: ComplaintRequest) -> PredictionResponse:
    """
    Predict the ML class for a complaint from its title and description.
    Returns the uppercase ML class that resolves via ml_class_config in Spring Boot.
    """
    if not is_model_ready():
        raise HTTPException(
            status_code=503,
            detail="ML model is not loaded. Please ensure model.pkl exists.",
        )

    try:
        result = predict(
            title=request.title,
            description=request.complaint_text,
        )

        logger.info(
            "Prediction: class=%s conf=%.4f hi=%s | title='%s'",
            result["predicted_class"],
            result["confidence"],
            result["high_confidence"],
            request.title[:50],
        )

        return PredictionResponse(**result)

    except Exception as e:
        logger.error("Prediction error: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")
