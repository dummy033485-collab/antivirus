#!/usr/bin/env python3
"""Print failing JUnit test cases from Gradle's XML results."""
import glob
import sys
import xml.etree.ElementTree as ET

found = 0
for path in glob.glob("app/build/test-results/**/*.xml", recursive=True):
    try:
        root = ET.parse(path).getroot()
    except Exception:
        continue
    for case in root.iter("testcase"):
        problems = list(case.findall("failure")) + list(case.findall("error"))
        for problem in problems:
            found += 1
            print(f"{case.get('classname')}.{case.get('name')}")
            message = (problem.get("message") or "").strip()
            if message:
                print(f"  message: {message[:800]}")
            body = (problem.text or "").strip()
            if body:
                print("  " + "\n  ".join(body.splitlines()[:25])[:2000])
            print("-" * 70)

if not found:
    print("No failing test cases found in XML (failure was earlier, e.g. compilation).")
sys.exit(0)
