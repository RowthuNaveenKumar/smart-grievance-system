from fastapi import FastAPI
from pydantic import BaseModel
from preprocess import clean_text
from model import predict_department, predict_priority

app = FastAPI(
    title="SGMS ML Service",
    version="1.0.0"
)

class ComplaintRequest(BaseModel):
    complaint_text: str
    title: str = ""

class PredictionResponse(BaseModel):
    predicted_department: str
    predicted_priority: str
    confidence: float

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
    cleaned_text = clean_text(data.complaint_text)

    department = predict_department(cleaned_text)
    priority = predict_priority(cleaned_text)

    return {
        "predicted_department": department,
        "predicted_priority": priority,
        "confidence": 0.78
    }
