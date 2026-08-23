from model import predict, is_model_ready

print("Model ready:", is_model_ready())

tests = [
    ("Hostel water problem", "There has been no proper water supply in our hostel for three days"),
    ("Unable to register for exam", "The examination portal is not allowing me to submit the form"),
    ("Library book not available", "The reference book I need for my project is not in the library"),
    ("Bus not on time", "College bus never arrives on time and students are late"),
    ("Medical emergency", "I had a health emergency but the college clinic was closed"),
    ("Sports equipment", "The cricket equipment I requested from sports room is not provided"),
    ("Classroom issue", "Professor has been absent for 3 weeks and no substitute provided"),
    ("Admin document", "I submitted documents to administrative office two weeks ago no response"),
    ("Empty input", ""),
]

print()
for title, desc in tests:
    r = predict(title, desc)
    hi = "HIGH" if r["high_confidence"] else "LOW "
    pred = r["predicted_class"] if r["predicted_class"] else "NONE"
    print(f"[{hi} {r['confidence']*100:5.1f}%] {pred:<12} | {title}")
