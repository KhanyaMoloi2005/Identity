# Identity: Roadmap

_Last updated: 2026-10-27. Status changes are recorded in [PROJECT_LOG.md](PROJECT_LOG.md)._

Each milestone ends with something you can run and verify on a phone, plus a small Git commit. Backlog IDs refer to the [spec §8](spec/identity_sdlc_stitch_handoff.md#8-initial-product-backlog).

| # | Milestone | Status |
|---|---|---|
| M0 | Foundations | ✅ Complete (2026-10-27) |
| M1 | App shell + offline Scripture | ⏸ Paused after 1.2 (D-020); resumes after M2 |
| M2 | DNS filtering proof of concept | 🟡 In progress (2.1, 2.2 merged; 2.3 phone-tested; 2.4 next) |
| M3 | Reliable protection | ⚪ Not started |
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
- Bundled compact blocklist; signed, versioned updates
- SafeSearch enforcement; custom allow/block rules applied predictably
- Restart after reboot; Always-on VPN status detection
- Interruption events stored locally, deleted after 30 days
- Dashboard shows observed status; bypass test checklist documented with results

## M4: Accounts and partners (ACC-01, PRIV-01)
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
- Optional Morning Focus list released on completion (D-006); sharing off by default

## M7: Commitment controls
- Tightening protection applies immediately; loosening waits through a user-chosen cooldown
- Optional partner notification of loosening; all limits disclosed before enabling
- **Uninstall friction + tamper alert (D-024, product owner request 2026-10-29):** optional, off by default. When on, Identity holds the Device Admin role, so the user must first turn that role off in Settings before uninstalling; Identity shows a pause message at that moment, and the partner (if any, M5) is told neutrally that protection was removed or the device stopped checking in. Removing the role can be tied to the loosening cooldown above. **Cannot make uninstalling impossible** (Safe Mode, factory reset and computer uninstall always work); the app says so before it is enabled.
  - **Gate before building:** check the current Google Play Device Admin and "Device and Network Abuse" policies (R-03). If Play does not allow this use, record the outcome and keep only the tamper alert.

## M8: Hardening + private beta
- Security/privacy review, accessibility pass, privacy notice, signed release build
- Google Play VPN/alarm policy check; feedback, support and rollback process
