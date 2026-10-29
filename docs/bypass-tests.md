# Identity: Bypass and reliability test results

_TEST-01 (spec section 8): known ways around protection, how each was tested, and what Identity does about it. Identity must not promise to be impossible to bypass; this page says plainly what it can and cannot stop. Related: [risk register](risk-register.md) (R-01, R-02), [architecture §5](architecture.md), [decision log](decision-log.md)._

**Test phone:** Samsung Galaxy A34 5G (SM-A346E), Android 16. **Last full run:** 2026-10-29, build from branch `m3/bypass-tests`, protection on, Always-on VPN on, "Block connections without VPN" off, Private DNS Automatic, Chrome as the browser. The blocked site used for tests is `example.com` (blocked in test builds only).

**Key:** ✅ blocked or handled · ⚠️ partly or not stoppable by a DNS filter (limit stated) · ❌ bypasses protection · ⏳ not yet tested on a phone

| # | Way around it | How tested | Result | What Identity does | Next |
|---|---|---|---|---|---|
| 1 | Open a blocked site (fresh address, no saved copy) | Chrome, `example.com/?x=<random>` | ✅ "This site can't be reached", `DNS_PROBE_FINISHED_NXDOMAIN` | Answers "no such domain" | n/a |
| 2 | Normal sites and apps still work | Chrome, Wikipedia; apps in daily use | ✅ | Forwards to Cloudflare for Families | n/a |
| 3 | **A page the browser already has saved** (opened before protection, or before the site was blocked), or the browser's Back button | Back button 2026-10-29; Chrome opened plain `example.com` after protection was on: loaded from its saved copy, not blocked | ⚠️ loads, no lookup happens | Cannot act: the browser never asks DNS | Disclosed. Only pages visited before; clearing browser data removes them. Nothing more is possible in a DNS filter |
| 4 | **Typing a site's IP address** instead of its name | Chrome, `http://104.20.23.154/` and its IPv6 form (example.com's addresses) | ⚠️ connection is made (no lookup to block). That site's host refused it (Cloudflare "error code: 1003"), but a site that serves its default page on its IP would load | Cannot act: no DNS lookup | Disclosed. Rarely done; M7 or later could add more |
| 5 | **Chrome "Use secure DNS" set to a custom provider (Cloudflare)** | Chrome set to Cloudflare, fresh `example.com` address, 2026-10-29 | ❌ **bypass confirmed**: the blocked site loaded | Cannot see it: Chrome sends lookups straight to Cloudflare over HTTPS | **Decision needed** (see "Proposed fix" below) |
| 6 | Chrome secure DNS left on the default | Every other Chrome test in this table | ✅ | Chrome uses the phone's DNS (Identity) | n/a |
| 7 | **Android Private DNS set to a custom hostname** | Product owner, M2.3 and 3.4; Home card and history entry confirmed 2026-10-29 | ❌ bypasses; ✅ detected | Warns on Home within 20 seconds with the fix, writes a history entry | Only the user can change it |
| 8 | Private DNS Automatic or Off | Phone | ✅ | n/a | n/a |
| 9 | Another VPN app takes over | Code path (`onRevoke`) and history entry exist; no second VPN app installed to try | ⏳ | Logs "turned off in Android settings or by another VPN"; does not restart by itself | Test with a second VPN app |
| 10 | Turning the VPN off in Android's settings | M2.2 phone check (partly reported) | ⚠️ works; logged | Logs it; does not restart until the user turns it on | M7 may add friction |
| 11 | Force-stop the app | `am force-stop`, 2026-10-29 | ✅ with Always-on (restarts when the app opens); ⚠️ otherwise "Interrupted" until opened | Home says Interrupted; protection restarts on open (D-034) | n/a |
| 12 | Force-stop turns off the Shorts/Reels service | Reproduced 2026-10-29 | ⚠️ then ✅ | Home card says the blocker is off | n/a |
| 13 | Restart the phone | Product owner, 2026-10-29 | ✅ returns; ⚠️ unprotected until unlock or Always-on | Restart receiver; Always-on VPN (D-033) | Gap is Android's rule |
| 14 | Wi-Fi to mobile data and back | M2.4 (product owner) | ✅ | n/a | n/a |
| 15 | "Block connections without VPN" | Product owner's phone, 2026-10-29 | ❌ cuts all internet | Home says to leave it off (D-033) | n/a |
| 16 | Uninstall, factory reset, another device | By design | ❌ cannot be stopped | Disclosed | M7: uninstall friction + tamper alert (D-024) |
| 17 | An app that uses its own DNS server (for example 8.8.8.8) | Not tried | ⏳ expected ❌ | Only Identity's own DNS address is routed through the tunnel | Proposed fix covers this |
| 18 | Firefox's built-in encrypted DNS | Canary name blocked (unit-tested); Firefox is not on the phone | ⏳ expected ✅ | Answers "no such domain" for Mozilla's `use-application-dns.net`, which tells Firefox to keep using the phone's DNS | Test when Firefox is installed |
| 19 | Adult content on general-purpose sites, proxies, translators, web archives | By design | ⚠️ not covered | X and Reddit blocked outright (D-028); SafeSearch (D-027) | Disclosed (R-02) |
| 20 | Change the phone's date or clock | Custom-rule undo window uses boot count and time since start (unit-tested); the TikTok daily allowance uses the date | ⚠️ undo ✅; allowance ⏳ expected to be reset by moving the date forward | n/a | Test it; M7 |
| 21 | Turning off the Accessibility service or its switches | Phone | ⚠️ works; the service being off is shown; the switches are the user's choice (D-031) | Home card; M7 to decide on switches | n/a |
| 22 | Safe mode, guest or second user, Secure Folder, work profile | Not tried | ⏳ | VPN may not apply there | Test and disclose |

## Proposed fix for #5 and #17 (not built, needs the product owner's decision)

Chrome's custom secure DNS and apps with their own DNS server both talk to well-known public resolver addresses (1.1.1.1, 8.8.8.8, 9.9.9.9 and a few more). Identity could **route those few addresses into its tunnel**:
- plain DNS sent to them (port 53) would be **answered by Identity's filter**, so apps with their own DNS are filtered too (fixes #17);
- everything else sent to them (encrypted DNS on port 443 or 853) would be **dropped**.

**What the user would see:** with Chrome's secure DNS on a custom provider, **Chrome could not look up any site** (it has no fallback in that mode) until the user turns the setting off. That stops the bypass, but it looks like "Chrome stopped working", so the Home screen would explain it. Side effects: Cloudflare's 1.1.1.1 and Google's 8.8.8.8 websites, and the 1.1.1.1 WARP app, would stop working while protection is on.

**Cost and risk:** a small change to the VPN's routes and packet handling, on the part of the app that every lookup passes through, so it needs careful testing; it does not change battery use noticeably.

**Not fixable by this:** #3, #4, #16, #19.
