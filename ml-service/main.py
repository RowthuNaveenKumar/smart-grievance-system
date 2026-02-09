from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="SGMS ML Service", version="1.0.0")

# Request model
class ComplaintRequest(BaseModel):
    complaint_text: str
    title: str = ""

# Response model
class PredictionResponse(BaseModel):
    predicted_category: str
    predicted_priority: str

@app.get("/")
def root():
    return {
        "service": "SGMS ML Service",
        "version": "1.0.0",
        "status": "running"
    }

@app.get("/health")
def health():
    return {
        "status": "ml-service-up",
        "models_loaded": False
    }

@app.post("/predict", response_model=PredictionResponse)
def predict(data: ComplaintRequest):
    """
    Dummy prediction endpoint
    Returns hardcoded values for now
    Will be replaced with actual ML models in Day 3
    """
    return {
        "predicted_category": "classroom",
        "predicted_priority": "Medium"
    }