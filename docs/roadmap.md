# Identity: Roadmap

_Last updated: 2026-10-27. Status changes are recorded in [PROJECT_LOG.md](PROJECT_LOG.md)._

Each milestone ends with something you can run and verify on a phone, plus a small Git commit. Backlog IDs refer to the [spec §8](spec/identity_sdlc_stitch_handoff.md#8-initial-product-backlog).

| # | Milestone | Status |
|---|---|---|
| M0 | Foundations | ✅ Complete (2026-10-27) |
| M1 | App shell + offline Scripture | ⏸ Paused after 1.2 (D-020); resumes once functionality is done (D-026) |
| M2 | DNS filtering proof of concept | 🟡 2.1 to 2.3 done; 2.4 battery observation in progress |
| M3 | Reliable protection | 🟡 In progress (3.1 to 3.3c merged; 3.4 written) |
| M4 | Accounts, partners, account deletion | ⚪ Not started |
| M5 | Reports and alerts | ⚪ Not started |
| M6 | Scripture First alarm + reminders | ⚪ Not started |
| M7 | Commitment controls | ⚪ Not started |
| M8 | Hardening + private beta | ⚪ Not started |

---

## M0: Foundations
- [x] Spec committed under `docs/spec/`
- [x] Planning docs: architecture, roadmap, decision log, risk register, data inventory, project log
- [x] Brand concept drafted (`docs/design/`)
- [x] Setup guide for Android Studio + phone
- [x] Android project skeleton written (`android/`): theme, I-cross mark, welcome screen, launcher icon
- [x] Cloud build workflow (`.github/workflows/android.yml`) written
- [x] Repo pushed to GitHub and the cloud build **passes** (PR #1, merged into `main`)
- [x] Debug APK installed on the product owner's phone; welcome screen shows
- Carried forward (not blocking M1):
  - [ ] Android Studio on your **personal** laptop (optional)
  - [ ] Provisional decisions confirmed or changed
  - [ ] ESV licence question answered (D-001); needed before any build is shared with others

## M1: App shell + offline Scripture (SP-01, SP-02, SP-03, CORE-01 screens)

> **Paused after 1.2 (D-020).** 1.3 to 1.6 resume once M2 is proven.

Built in six slices, one pull request each:
- [ ] 1.1 Navigation shell + bundled fonts (written, awaiting build ✅ + phone check)
- [ ] 1.2 Scripture data layer (written; awaiting build ✅ with tests run)
- [ ] 1.3 Browse by topic
- [ ] 1.4 "I'm struggling" flow
- [ ] 1.5 Saved verses + private reflections
- [ ] 1.6 Onboarding + privacy disclosures

Acceptance criteria for M1 as a whole:
- Navigation (Home · Scripture · Partner · Settings), theme in light and dark, bundled fonts
- Onboarding + privacy disclosure screens (static text; no permissions requested yet)
- Verse library from bundled public-domain text; browse by topic
- "I'm struggling" opens a curated passage plus next-step options
- Saved verses and private reflections work in airplane mode
- **The app declares no internet permission at this stage**
- Unit tests for the verse repository; TalkBack labels on all controls

## M2: DNS filtering proof of concept (BLOCK-01)

Built in four slices, one pull request each:
- [x] 2.1 `:filter-core` module: DNS packet parsing + domain matcher, unit-tested (computer only), merged (PR #8)
- [x] 2.2 `IdentityVpnService` skeleton: VPN permission, start/stop, visible notification, nothing blocked yet (phone check), merged (PR #9), phone-tested
- [x] 2.3 Answer queries: block a hard-coded test domain, forward everything else upstream (phone check), phone-tested; a custom Private DNS breaks it (→ M3 detection)
- [ ] 2.4 Wi-Fi to mobile data, IPv6 and 24 h battery observation recorded (phone check); also repeat 2.2/2.3 items not yet reported (Cancel, Settings-off, Android version)

Acceptance criteria for M2 as a whole:
- `:filter-core` module: DNS packet parsing + domain matcher, unit-tested
- `IdentityVpnService` (DNS-only) with a tiny hard-coded test blocklist
- **On a physical phone:** test domain blocked in Chrome; ordinary browsing works
- Wi-Fi ↔ mobile data and IPv6 tested; 24 h battery observation recorded

## M3: Reliable protection (BLOCK-02, BLOCK-03, SEC-01, CORE-02, TEST-01)

Built in seven slices, one pull request each (order agreed 2026-10-29):
- [x] 3.1 Bundled blocklist: HaGeZi NSFW (D-025), packed for low memory, replaces the test domain (merged, phone-tested)
- [x] 3.2 SafeSearch: Google, Bing, DuckDuckGo, YouTube forced into safe mode (D-027) (merged, phone-tested)
- [x] 3.3a Always block X/Reddit and image-search pictures, no user switches (D-028) (merged, phone-tested)
- [ ] 3.3b Your own blocks: websites and words in site names; block-only, 10-minute undo (D-029) — written, 56 filter-core tests pass locally; awaiting CI ✅ and phone check
- [ ] 3.3c App blocking (chosen over cutting the app's internet, D-029)
  - [x] 3.3c-1 TikTok daily time limit: fixed allowances, "time is up" page over TikTok, DNS cut-off, raising starts tomorrow (D-030). Phone-tested by the product owner; the 30-day lock is M7
  - [x] 3.3c-2 YouTube Shorts and Instagram Reels only (not the whole apps): an Accessibility Service sends the user back to the app's home screen or closes the app; they can open it again (D-031). Two switches, one per app. Phone-tested by the product owner. Google Play policy risk (R-03)
- [x] 3.4 Staying on: restart after reboot, update or Android ending the service; Always-on VPN guidance; custom Private DNS warning (M2.3 finding). Phone-tested except the Private DNS card
- [x] 3.5 Observed status on Home (Protected / Needs attention with reasons / Interrupted / Off); protection history stored locally, deleted after 30 days; protection restarts when the app is opened (D-034). Phone-tested except the Needs-attention reasons
- [ ] 3.6 Signed, versioned blocklist updates (ECDSA P-256 instead of Ed25519, D-035; keep the last good list on failure). Written; 104 filter-core tests pass; first signed release published and installed on the phone
- [x] 3.7 Bypass test checklist documented with results: see [bypass-tests.md](bypass-tests.md). Found a confirmed bypass (Chrome's secure DNS set to a custom provider) and that the browser's saved pages load without a lookup. Mozilla's encrypted-DNS canary is now blocked
- [x] 3.7b Close the public-resolver bypass (Chrome secure DNS, apps with their own DNS): refuse encrypted-DNS server names, route well-known resolver addresses into the tunnel, filter plain DNS, refuse encrypted DNS (D-036). Phone-tested with Chrome on Cloudflare

## Later: ideas to investigate (not scheduled)
- **On-device content detection** (product owner idea, 2026-10-29): close a page or app for a few seconds when certain phrases appear on screen, and detect explicit images on screen. Possible only with an Accessibility Service and/or screen capture plus an on-device image model, which **conflicts with D-012 and the spec** (no accessibility service, no screenshots). Before deciding, research: current Google Play policy on Accessibility for blocking apps (R-03), fully on-device and opt-in design, battery cost, false positives, model licences. Then the product owner decides whether to change D-012.

## M4: Accounts and partners (ACC-01, PRIV-01)
_Plan (proposal, awaiting decisions): [plans/m4-accounts-and-partners.md](plans/m4-accounts-and-partners.md)_
- Sign-up / sign-in; device registration
- Invitations: random, hashed, single-use, 48 h expiry, rate-limited
- RLS tested with two accounts + revoked partner
- Account deletion works and retention is documented

## M5: Reports and alerts (ACC-02, ACC-03)
- Daily summary limited to consented categories
- "Device hasn't checked in" alert, labelled neutrally
- Partner never receives domain names unless explicitly opted in

## M6: Scripture First + reminders (SP-04, SP-05)
- Schedule, edit, pause and disable alarms; passage matches the reading plan, offline
- Reboot, time-zone, daylight-saving and missed-alarm behaviour defined and tested
- Fallback notification when Android won't allow a full-screen alarm
- **Focus hour (product owner's idea, D-032):** at the wake-up time, every app except an essential list (Phone, WhatsApp, ...) is covered for a set time while the user reads and prays in Identity; then the apps unlock. Uses the cover-page method from D-030. Questions to settle before building are in D-032
- Optional Morning Focus list released on completion (D-006); sharing off by default

## M7: Commitment controls
- Tightening protection applies immediately; loosening waits through a user-chosen cooldown
- **Decide whether to keep the Shorts/Reels switches (D-031) and whether the TikTok allowance (D-030) needs the same treatment; the product owner will decide here**
- Optional partner notification of loosening; all limits disclosed before enabling (if user switches for the D-028 blocks are ever added, they belong here, behind the cooldown)
- **Uninstall friction + tamper alert (D-024, product owner request 2026-10-29):** optional, off by default. When on, Identity holds the Device Admin role, so the user must first turn that role off in Settings before uninstalling; Identity shows a pause message at that moment, and the partner (if any, M5) is told neutrally that protection was removed or the device stopped checking in. Removing the role can be tied to the loosening cooldown above. **Cannot make uninstalling impossible** (Safe Mode, factory reset and computer uninstall always work); the app says so before it is enabled.
  - **Gate before building:** check the current Google Play Device Admin and "Device and Network Abuse" policies (R-03). If Play does not allow this use, record the outcome and keep only the tamper alert.

## M8: Hardening + private beta
- Security/privacy review, accessibility pass, privacy notice, signed release build
- Google Play VPN/alarm policy check; feedback, support and rollback process
- **Licences screen** in Settings: fonts (OFL) and the HaGeZi blocklist (GPL-3.0) credited and readable; re-check the GPL conditions for the Play release (D-025)
