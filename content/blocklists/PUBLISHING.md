# Publishing a blocklist update

How a new blocklist reaches phones (M3.6, D-035). Do this whenever you want phones to get a newer list; the app checks once a day while protection is on.

**What you need:** the private signing key (`blocklist-signing-private.pem`, kept offline and backed up), Python, and OpenSSL (comes with Git for Windows). The matching public key is inside the app (`BlocklistUpdate.PUBLIC_KEY_BASE64`).

## Each time

1. From the project root, build and sign:

   ```
   python content/blocklists/publish_update.py --key C:\Users\<you>\IdentitySigning\blocklist-signing-private.pem
   ```

   It downloads HaGeZi's current list, builds it the same way as the bundled one, signs it, checks its own signature, and writes three files into `build/blocklist-update/`:
   `nsfw.txt` (the list), `manifest.txt` (version, SHA-256, entries), `manifest.sig` (the signature).

2. On GitHub, open the repo's **Releases** page.
   - **First time only:** *Draft a new release*, choose **Create new tag** `blocklist-latest`, title it "Blocklist (latest)", tick **Set as a pre-release** (so it is never shown as the app's latest version), drag in the three files, and **Publish release**.
   - **Every later time:** open that release, **Edit**, delete the three old files, drag in the three new ones, **Update release**.

3. Check it: on a phone with protection on, Settings, **Check for update**. It should say "Updated to a newer list", and the Blocklist line shows the new count and date.

## Notes

- The version is the UTC time as a number (for example `202610291841`), so each build is newer than the last and no counter needs keeping.
- The app refuses a list that is not newer, has a bad signature, does not match its SHA-256, has a different number of names than the manifest says, has fewer than 50,000 names, or is less than half the size of the list in use. A refused update is written to the Protection history and the old list stays.
- Replacing the three files is not instant: for a moment the manifest, signature and list may not match, and a phone that checks at that moment refuses the update and logs it once. It tries again later.
- **Never** commit or share the private key. `.gitignore` already blocks `*.pem`. If it is lost, ship a new app version with a new public key; if it leaks, do the same and stop publishing with the old key.
- The list is HaGeZi's, licensed GPL-3.0; the header inside the list file carries the credit and is generated for you.
