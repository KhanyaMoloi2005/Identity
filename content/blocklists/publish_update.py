"""Builds and signs a blocklist update for the app (M3.6, R-05, D-035).

Run from the project root:

    python content/blocklists/publish_update.py --key C:\\Users\\you\\IdentitySigning\\blocklist-signing-private.pem

It downloads HaGeZi's list, builds it exactly like the bundled one, and writes three files into
build/blocklist-update/ (git ignores that folder):

    nsfw.txt        the list
    manifest.txt    version, SHA-256 and number of entries
    manifest.sig    the signature over manifest.txt, made with YOUR private key

Upload all three to the GitHub release tagged `blocklist-latest` (see PUBLISHING.md). The app only
installs a list whose signature matches the public key inside it, so the private key must stay
offline and backed up. Needs OpenSSL, which comes with Git for Windows.
"""

import argparse
import datetime
import hashlib
import pathlib
import shutil
import subprocess
import sys

import build_blocklist

OUT_DIR = pathlib.Path("build/blocklist-update")


def find_openssl() -> str:
    found = shutil.which("openssl")
    if found:
        return found
    for candidate in (r"C:\Program Files\Git\mingw64\bin\openssl.exe", r"C:\Program Files\Git\usr\bin\openssl.exe"):
        if pathlib.Path(candidate).exists():
            return candidate
    raise SystemExit("OpenSSL not found. It comes with Git for Windows; add its folder to PATH.")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--key", required=True, help="path to the private signing key (.pem)")
    args = parser.parse_args()

    key = pathlib.Path(args.key)
    if not key.is_file():
        print(f"Key not found: {key}")
        return 1
    if not pathlib.Path("android").is_dir():
        print("Run this from the project root (the folder that contains 'android').")
        return 1
    openssl = find_openssl()

    source_version, names = build_blocklist.fetch()
    if len(names) < build_blocklist.MINIMUM_ENTRIES:
        print(f"Only {len(names)} entries; expected at least {build_blocklist.MINIMUM_ENTRIES}. Nothing written.")
        return 1

    now = datetime.datetime.now(datetime.timezone.utc)
    list_bytes = build_blocklist.render(source_version, names, now.date().isoformat()).encode("utf-8")
    # The version is the UTC time as a number (202610292100), so it always goes up and needs no counter.
    version = int(now.strftime("%Y%m%d%H%M"))
    manifest = (
        f"version={version}\n"
        f"sha256={hashlib.sha256(list_bytes).hexdigest()}\n"
        f"entries={len(names)}\n"
        f"generated={now.strftime('%Y-%m-%dT%H:%M:%SZ')}\n"
        f"source_version={source_version}\n"
    ).encode("utf-8")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / "nsfw.txt").write_bytes(list_bytes)
    (OUT_DIR / "manifest.txt").write_bytes(manifest)
    subprocess.run(
        [openssl, "dgst", "-sha256", "-sign", str(key), "-out", str(OUT_DIR / "manifest.sig"), str(OUT_DIR / "manifest.txt")],
        check=True,
    )

    # Check our own work with the public half before anything is uploaded.
    public = subprocess.run([openssl, "ec", "-in", str(key), "-pubout"], check=True, capture_output=True).stdout
    public_file = OUT_DIR / "public-check.pem"
    public_file.write_bytes(public)
    verified = subprocess.run(
        [openssl, "dgst", "-sha256", "-verify", str(public_file), "-signature", str(OUT_DIR / "manifest.sig"), str(OUT_DIR / "manifest.txt")],
        capture_output=True,
        text=True,
    )
    public_file.unlink()
    if verified.returncode != 0:
        print("The signature did not verify against the key. Nothing should be uploaded.")
        return 1

    print(f"Built version {version}: {len(names)} domains (source version {source_version}).")
    print(f"Signed and verified. Upload these three files from {OUT_DIR}/ to the 'blocklist-latest' release:")
    for name in ("nsfw.txt", "manifest.txt", "manifest.sig"):
        print(f"  {name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
