"""
model.py — Inference layer for SGMS Department Classifier.

Loads the trained TF-IDF + Logistic Regression model from model.pkl
and provides predict() with real confidence values from predict_proba().

The CONFIDENCE_THRESHOLD determines whether a prediction is considered
reliable enough for automatic department selection or should prompt
the user to confirm.
"""

import os
import joblib
import logging

from preprocess import combine_fields

logger = logging.getLogger(__name__)

# ──────────────────────────────────────────────────────────────────────────────
# Configuration
# ──────────────────────────────────────────────────────────────────────────────
MODEL_PATH = os.path.join(os.path.dirname(__file__), "model.pkl")

# Confidence threshold for "high confidence" auto-selection.
# Below this value, the frontend should prompt the user to confirm.
CONFIDENCE_THRESHOLD = 0.60

# ──────────────────────────────────────────────────────────────────────────────
# Model loading (lazy singleton)
# ──────────────────────────────────────────────────────────────────────────────
_pipeline = None


def _load_model():
    """Load model from disk (once) and cache in module-level variable."""
    global _pipeline
    if _pipeline is None:
        if not os.path.exists(MODEL_PATH):
            raise FileNotFoundError(
                f"Model file not found: {MODEL_PATH}. "
                f"Run train.py first to generate the model."
            )
        _pipeline = joblib.load(MODEL_PATH)
        logger.info("ML model loaded from %s", MODEL_PATH)
    return _pipeline


def is_model_ready() -> bool:
    """Check whether the model file exists and is loadable."""
    if not os.path.exists(MODEL_PATH):
        return False
    try:
        _load_model()
        return True
    except Exception:
        return False


def predict(title: str, description: str) -> dict:
    """
    Predict the ML class for a complaint.

    Parameters
    ----------
    title : str
        Complaint title
    description : str
        Complaint description

    Returns
    -------
    dict with keys:
        predicted_class    : str | None — Uppercase ML class matching ml_class_config
        confidence         : float      — Probability [0.0–1.0] from model
        high_confidence    : bool       — True if confidence >= CONFIDENCE_THRESHOLD
        predicted_priority : str        — "LOW" for empty, "MEDIUM" default
    """
    pipeline = _load_model()

    # Combine and preprocess using the SAME pipeline as training (2x title weighting preserved)
    text = combine_fields(title, description)

    if not text.strip():
        logger.warning("Empty text after preprocessing. Returning fallback.")
        return {
            "predicted_class": None,
            "confidence": 0.0,
            "high_confidence": False,
            "predicted_priority": "LOW",
        }

    # Predict with probabilities
    proba = pipeline.predict_proba([text])[0]
    classes = pipeline.classes_

    best_idx = proba.argmax()
    predicted_cls = classes[best_idx]
    confidence = float(proba[best_idx])

    # All probabilities per class (for debugging/logging)
    proba_map = {cls: round(float(p), 4) for cls, p in zip(classes, proba)}
    logger.debug("Prediction probabilities: %s", proba_map)

    return {
        "predicted_class": predicted_cls,                # e.g. "HOSTEL"
        "confidence": round(confidence, 4),               # e.g. 0.8742
        "high_confidence": confidence >= CONFIDENCE_THRESHOLD,
        "predicted_priority": "MEDIUM",                   # Not modelled — safe default
    }