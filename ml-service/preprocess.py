"""
preprocess.py — Text preprocessing for SGMS ML classifier.

IMPORTANT: This same pipeline MUST be used at both training time and inference time
to guarantee consistent feature extraction.
"""

import re
import unicodedata


# Simple English stopwords — minimal set to avoid over-removing domain terms
STOPWORDS = {
    "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
    "have", "has", "had", "do", "does", "did", "will", "would", "could",
    "should", "may", "might", "shall", "can", "to", "of", "in", "on",
    "at", "by", "for", "with", "from", "and", "or", "but", "not", "no",
    "it", "its", "i", "my", "we", "our", "you", "your", "he", "she",
    "they", "their", "this", "that", "these", "those", "there", "here",
    "very", "just", "also", "so", "too", "even", "about", "as", "up",
    "if", "than", "then", "now", "since", "while", "when", "what", "which",
}


def clean_text(text: str) -> str:
    """
    Apply consistent preprocessing to a text string.

    Steps:
    1. Normalize unicode to ASCII-compatible form
    2. Lowercase
    3. Remove special characters (keep letters and spaces)
    4. Collapse multiple whitespace
    5. Strip leading/trailing whitespace
    6. Remove stopwords

    NOTE: Domain terminology like 'hostel', 'exam', 'library', 'transport',
    'medical', 'sports', 'academic' is intentionally preserved — these are
    the strongest signals for department classification.
    """
    if not text or not isinstance(text, str):
        return ""

    # 1. Normalize unicode (e.g. accented characters → ASCII equivalent)
    text = unicodedata.normalize("NFKD", text)
    text = text.encode("ascii", "ignore").decode("ascii")

    # 2. Lowercase
    text = text.lower()

    # 3. Remove non-alphabetic characters (keep spaces)
    text = re.sub(r"[^a-z\s]", " ", text)

    # 4. Collapse whitespace
    text = re.sub(r"\s+", " ", text).strip()

    # 5. Remove stopwords
    tokens = [w for w in text.split() if w not in STOPWORDS]

    return " ".join(tokens)


def combine_fields(title: str, description: str) -> str:
    """
    Combine title + description into a single feature string.
    Title is doubled to give it more weight in TF-IDF.
    """
    safe_title = title.strip() if title else ""
    safe_desc = description.strip() if description else ""

    # Title is repeated to emphasize it (a simple weighting trick for TF-IDF)
    combined = f"{safe_title} {safe_title} {safe_desc}"
    return clean_text(combined)
