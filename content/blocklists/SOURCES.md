# Blocklist sources

## Adult-content list (`android/app/src/main/assets/blocklist/nsfw.txt`)

- **Source:** HaGeZi's DNS Blocklists, NSFW list, wildcard format: https://github.com/hagezi/dns-blocklists (`wildcard/nsfw-onlydomains.txt`). Each entry also blocks its subdomains.
- **Licence:** GNU General Public License v3.0. Full text bundled at `android/app/src/main/assets/licenses/hagezi-GPL-3.0.txt`. See D-025 for how we meet its conditions.
- **Bundled version:** 2026.1029.1013.28, downloaded on 2026-10-29: 92,127 domains.
- **How the file is produced:** `python content/blocklists/build_blocklist.py` (run from the project root). It lower-cases, sorts and de-duplicates the list, drops anything that isn't a plain domain name, and writes the credit header. Nothing is added or removed by hand.
- **Wrongly blocked a normal site?** Report it to HaGeZi (https://github.com/hagezi/dns-blocklists/issues). In the app, a personal allow rule (M3.3) will override it.
