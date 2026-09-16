#!/usr/bin/env python3
"""Every translation must cover every key in values/strings.xml.

A missing key silently falls back to English at runtime — invisible in a build
log and easy to miss in review, so it's checked here instead. Also flags keys a
translation has but the base doesn't (a typo or a leftover after a rename).
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "composeApp/src/commonMain/composeResources"


def keys(path: Path) -> set[str]:
    root = ET.parse(path).getroot()
    return {f"{el.tag}/{el.get('name')}" for el in root if el.get("name")}


def main() -> int:
    base_file = RES / "values/strings.xml"
    base = keys(base_file)
    problems = []
    translations = sorted(RES.glob("values-*/strings.xml"))
    if not translations:
        print("no translations found — nothing to check")
        return 0
    for path in translations:
        lang = path.parent.name
        found = keys(path)
        for k in sorted(base - found):
            problems.append(f"{lang}: missing {k}")
        for k in sorted(found - base):
            problems.append(f"{lang}: {k} not in values/strings.xml")
    for p in problems:
        print(f"FAIL {p}")
    if problems:
        return 1
    print(f"OK {len(base)} keys x {len(translations)} translation(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
