"""
train.py — Training pipeline for SGMS Department Classifier.

Architecture:
    Dataset (CSV) → Preprocessing → TF-IDF → Logistic Regression → model.pkl

Dataset: dataset.csv (seed/development data — clearly labelled as such)
Labels:  Must match DB department names exactly (case-insensitive match applied at inference)

Evaluation: Stratified train/test split + per-class classification report.

NOTE: This is a seed dataset for development. Metrics reflect seed data performance.
      Real production accuracy cannot be claimed until real historical data is used.

Usage:
    python train.py
"""

import os
import sys
import warnings
import pandas as pd
import joblib

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import StratifiedKFold, cross_val_score, train_test_split
from sklearn.pipeline import Pipeline
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score

from preprocess import combine_fields

warnings.filterwarnings("ignore")

# ──────────────────────────────────────────────────────────────────────────────
# Configuration
# ──────────────────────────────────────────────────────────────────────────────
DATASET_PATH = os.path.join(os.path.dirname(__file__), "dataset.csv")
MODEL_PATH   = os.path.join(os.path.dirname(__file__), "model.pkl")

# Confidence threshold for auto-selection vs. flagging as uncertain
CONFIDENCE_THRESHOLD = 0.60


def load_dataset(path: str) -> tuple[list[str], list[str]]:
    """Load and preprocess the CSV dataset. Returns (texts, labels)."""
    df = pd.read_csv(path)

    required = {"title", "description", "department"}
    if not required.issubset(df.columns):
        print(f"[ERROR] Dataset missing columns. Required: {required}. Got: {set(df.columns)}")
        sys.exit(1)

    # Drop rows with missing values
    before = len(df)
    df = df.dropna(subset=["title", "description", "department"])
    after = len(df)
    if before != after:
        print(f"[WARN] Dropped {before - after} rows with missing values")

    # Standardize department labels to UPPERCASE (must match DB exactly)
    df["department"] = df["department"].str.strip().str.upper()

    # Combine title + description using the shared preprocessing pipeline
    texts  = [combine_fields(row["title"], row["description"]) for _, row in df.iterrows()]
    labels = df["department"].tolist()

    return texts, labels


def print_dataset_stats(labels: list[str]) -> None:
    """Print class distribution summary."""
    from collections import Counter
    counts = Counter(labels)
    total  = sum(counts.values())

    print(f"\n{'='*60}")
    print("DATASET STATISTICS")
    print(f"{'='*60}")
    print(f"Total samples : {total}")
    print(f"Num classes   : {len(counts)}")
    print("\nClass distribution:")
    for dept, count in sorted(counts.items()):
        pct = 100 * count / total
        print(f"  {dept:<20} {count:>4} samples  ({pct:5.1f}%)")
    print()


def build_pipeline() -> Pipeline:
    """Build TF-IDF + Logistic Regression pipeline."""
    return Pipeline([
        ("tfidf", TfidfVectorizer(
            ngram_range=(1, 2),       # unigrams + bigrams
            min_df=1,                 # include rare terms (small dataset)
            max_df=0.95,              # ignore near-universal terms
            sublinear_tf=True,        # apply log scaling to TF
            strip_accents="unicode",
        )),
        ("clf", LogisticRegression(
            C=5.0,                    # regularization strength
            max_iter=1000,
            solver="lbfgs",           # handles multiclass natively
        )),
    ])


def evaluate(pipeline: Pipeline, X: list[str], y: list[str]) -> None:
    """Train/test split evaluation + cross-validation."""
    print(f"{'='*60}")
    print("MODEL EVALUATION")
    print(f"{'='*60}")

    n_classes = len(set(y))
    n_samples = len(y)

    # With a small dataset, use stratified 3-fold cross-validation
    if n_samples < 60:
        print(f"[WARN] Small dataset ({n_samples} samples). "
              f"Metrics are preliminary and not representative of production accuracy.")

    # ── Held-out test split ──────────────────────────────────────────────────
    try:
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.25, random_state=42, stratify=y
        )
    except ValueError:
        # Fallback if some class has too few samples to stratify
        print("[WARN] Could not stratify split — using random split")
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.25, random_state=42
        )

    pipeline.fit(X_train, y_train)
    y_pred = pipeline.predict(X_test)

    acc = accuracy_score(y_test, y_pred)
    print(f"\nTrain samples : {len(X_train)}")
    print(f"Test  samples : {len(X_test)}")
    print(f"Hold-out Accuracy: {acc:.4f}  ({acc*100:.1f}%)")

    print("\nPer-class Report:")
    print(classification_report(y_test, y_pred, zero_division=0))

    print("Confusion Matrix:")
    labels_sorted = sorted(set(y))
    cm = confusion_matrix(y_test, y_pred, labels=labels_sorted)
    header = "           " + "  ".join(f"{l[:6]:>6}" for l in labels_sorted)
    print(header)
    for row_label, row in zip(labels_sorted, cm):
        row_str = "  ".join(f"{v:>6}" for v in row)
        print(f"  {row_label[:10]:>10}  {row_str}")

    # ── Stratified K-Fold cross-validation ───────────────────────────────────
    try:
        k = min(5, n_samples // n_classes)
        if k >= 2:
            cv = StratifiedKFold(n_splits=k, shuffle=True, random_state=42)
            cv_scores = cross_val_score(pipeline, X, y, cv=cv, scoring="accuracy")
            print(f"\n{k}-Fold Cross-Validation Accuracy: "
                  f"{cv_scores.mean():.4f} ± {cv_scores.std():.4f}")
        else:
            print("\n[WARN] Too few samples per class for reliable cross-validation")
    except Exception as e:
        print(f"\n[WARN] Cross-validation skipped: {e}")

    print(f"\nConfidence Threshold: {CONFIDENCE_THRESHOLD}")
    print(f"  Predictions above {CONFIDENCE_THRESHOLD*100:.0f}% confidence -> auto-selected")
    print(f"  Predictions below {CONFIDENCE_THRESHOLD*100:.0f}% confidence -> user prompted to confirm\n")


def train_and_save(X: list[str], y: list[str]) -> Pipeline:
    """Train final model on full dataset and save."""
    pipeline = build_pipeline()
    pipeline.fit(X, y)
    joblib.dump(pipeline, MODEL_PATH)
    print(f"[OK] Model saved to: {MODEL_PATH}")
    return pipeline


def main() -> None:
    print("\nSGMS Department Classifier — Training Pipeline")
    print("=" * 60)
    print(f"Dataset : {DATASET_PATH}")
    print(f"Model   : {MODEL_PATH}")
    print()
    print("NOTE: Using SEED/DEVELOPMENT dataset.")
    print("      Reported metrics are preliminary — not production accuracy.")
    print()

    texts, labels = load_dataset(DATASET_PATH)
    print_dataset_stats(labels)

    # Build a fresh pipeline for evaluation
    eval_pipeline = build_pipeline()
    evaluate(eval_pipeline, texts, labels)

    # Train on full dataset and save
    train_and_save(texts, labels)
    print("\n[OK] Training complete.\n")


if __name__ == "__main__":
    main()
