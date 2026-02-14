import re

def clean_text(text: str) -> str:
    if not text:
        return ""

    text = text.lower()
    text = re.sub(r'[^a-z\s]', '', text)
    text = ' '.join(text.split())
    return text
