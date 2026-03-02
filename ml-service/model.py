def predict_department(text: str) -> str:
    text = text.lower()

    technical_keywords = ["wifi", "internet", "computer", "software", "network"]
    exam_keywords = ["exam", "marks", "result", "internal","external"]
    hostel_keywords = ["hostel", "mess", "food", "water","bed","tap","study_table"]
    infrastructure_keywords = ["bench", "classroom"]
    electrical_keywords = ["fan", "light","ac","projector"]

    # Priority-based matching
    if any(word in text for word in technical_keywords):
        return "technical"

    if any(word in text for word in exam_keywords):
        return "exam_cell"

    if any(word in text for word in hostel_keywords):
        return "hostel"

    if any(word in text for word in infrastructure_keywords):
        return "infrastructure"
    
    if any(word in text for word in electrical_keywords):
        return "electrical"

    return "general"

def predict_priority(text: str) -> str:
    text = text.lower()

    if any(word in text for word in ["urgent", "immediately", "danger", "accident", "fire", "exposed"]):
        return "High"

    if any(word in text for word in ["not working", "broken", "issue", "problem"]):
        return "Medium"
    
    if any(word in text for word in ["", "broken", "issue", "problem"]):
        return "Low"

def calculate_confidence(text: str) -> float:
    text = text.lower()

    if any(word in text for word in ["urgent", "danger", "accident", "fire"]):
        return 0.92

    return 0.78