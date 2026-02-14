from preprocess import clean_text

# Test cases
tests = [
    "The CLASSROOM AC is NOT working!!! Temperature is 35°C",
    "Hostel WiFi @#$% not working!!!",
    "Library    has    too    many    spaces"
]

for test in tests:
    print(f"Original: {test}")
    print(f"Cleaned:  {clean_text(test)}")
    print()