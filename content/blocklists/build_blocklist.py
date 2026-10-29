"""Builds the blocklist bundled in the app from HaGeZi's NSFW list (D-025).

Run from the project root:   python content/blocklists/build_blocklist.py

It downloads the list, keeps only plain domain names (lower-case, one per line, sorted,
no duplicates) and writes android/app/src/main/assets/blocklist/nsfw.txt with a header that
credits the source and its licence, as the GPL-3.0 requires. The app re-checks every line when it
loads the file, so a damaged file can't add strange entries.
"""

import datetime
import pathlib
import re
import sys
import urllib.request

SOURCE_URL = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/nsfw-onlydomains.txt"
OUTPUT = pathlib.Path("android/app/src/main/assets/blocklist/nsfw.txt")
DOMAIN = re.compile(r"^[a-z0-9_-]+(\.[a-z0-9_-]+)*$")
# Refuse to write a list that has shrunk suspiciously (a failed or partial download).
MINIMUM_ENTRIES = 50_000


def main() -> int:
    if not pathlib.Path("android").is_dir():
        print("Run this from the project root (the folder that contains 'android').")
        return 1

    with urllib.request.urlopen(SOURCE_URL, timeout=60) as response:
        text = response.read().decode("utf-8")

    version = "unknown"
    names = set()
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("# Version:"):
            version = line.split(":", 1)[1].strip()
        if not line or line.startswith("#"):
            continue
        name = line.lower().rstrip(".")
        if DOMAIN.match(name):
            names.add(name)

    if len(names) < MINIMUM_ENTRIES:
        print(f"Only {len(names)} entries; expected at least {MINIMUM_ENTRIES}. Nothing written.")
        return 1

    today = datetime.date.today().isoformat()
    header = [
        "# HaGeZi's NSFW blocklist, as bundled in Identity.",
        "# Source: https://github.com/hagezi/dns-blocklists (wildcard/nsfw-onlydomains.txt)",
        f"# Source version: {version}",
        "# Licence: GNU General Public License v3.0, see licenses/hagezi-GPL-3.0.txt",
        f"# Changes by Identity ({today}): lower-cased, sorted, duplicates and invalid lines removed.",
        "# Each name also blocks its subdomains.",
        f"# Entries: {len(names)}",
    ]
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text("\n".join(header + sorted(names)) + "\n", encoding="utf-8", newline="\n")
    print(f"Wrote {len(names)} domains (source version {version}) to {OUTPUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
