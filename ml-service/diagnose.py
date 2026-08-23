from model import predict

tests = [
    ("class room was not good", "class room was not good"),
    ("classroom not good", "classroom not good"),
    ("classroom projector broken", "the projector in our classroom is broken"),
    ("lecture hall problem", "the lecture hall benches are broken"),
    ("hostel room dirty", "hostel room was dirty"),
    ("class room facility", "the class room does not have proper seating"),
    ("room in hostel", "room in hostel not cleaned"),
]

print(f"{'Predicted':<12} {'Conf%':>6}  Input")
print("-" * 60)
for t, d in tests:
    r = predict(t, d)
    flag = "HIGH" if r["high_confidence"] else "LOW "
    pred = r["predicted_class"] if r["predicted_class"] else "NONE"
    print(f"{pred:<12} [{r['confidence']*100:5.1f}% {flag}] | {t}")
