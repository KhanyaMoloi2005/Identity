# Identity: Completion Checklist

_The one page to check when you ask "where are we?" Tick boxes here as work finishes. Details live in [roadmap.md](roadmap.md); the history lives in [PROJECT_LOG.md](PROJECT_LOG.md)._

**Current task:** M3.3b your own blocks, branch `m3/custom-rules` (not merged). CI ✅; site blocking works on the phone. Still to check: 10-minute "Permanent", rules survive a restart, a word rule. Then merge and start 3.3c (app blocking, Google Play policy check first). M3.1 to M3.3a merged and phone-tested.

> **Pick up here (2026-10-29):** on any computer, `git fetch` then `git checkout m3/custom-rules`. Read the newest entry in [PROJECT_LOG.md](PROJECT_LOG.md) and the open decisions in [decision-log.md](decision-log.md) (D-022 to D-029 cover M2 and M3). Working rules: [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md). Pending observations: M2.4 battery result, M2.2/2.3 leftovers (Cancel in the VPN dialog, turning off from system Settings, Android version). M2.4 battery observation is running on the phone. M1.2 awaits its verse review (in progress).
**Rule:** one task at a time. A new task starts only when every box in "Done means done" below is ticked for the previous one.

---

## Done means done (applies to every task)

- [ ] Task and acceptance criteria written down **before** coding
- [ ] Existing code inspected first
- [ ] Change is small, on its own branch, and touches only what the task needs
- [ ] Computer checks pass (build, lint, unit tests; CI green, and **tests actually ran**)
- [ ] Phone check done, if the change is visible or device-related
- [ ] `docs/PROJECT_LOG.md` entry added (what, why, files, how to verify, next step), in the same commit
- [ ] `docs/roadmap.md` and this checklist ticked
- [ ] Pull request merged into `main`
- [ ] Product owner says "approved", then the next task begins

---

## Milestones

- [x] **M0** Foundations
- [ ] **M1** App shell + offline Scripture ⏸ paused after 1.2 until functionality is done (D-020, D-026)
  - [x] 1.1 Navigation shell + fonts (merged, phone-verified)
  - [ ] 1.2 Scripture data layer + first unit tests (written; CI green with tests run, then verse review)
  - [ ] 1.3 Browse by topic
  - [ ] 1.4 "I'm struggling" flow
  - [ ] 1.5 Saved verses + private reflections
  - [ ] 1.6 Onboarding + privacy disclosures
  - [ ] M1 acceptance: works in airplane mode, no internet permission, TalkBack labels, verse tests pass
- [ ] **M2** DNS filtering proof of concept ← in progress
  - [x] 2.1 `:filter-core`: DNS parsing + domain matcher, unit-tested (computer only), merged (PR #8)
  - [x] 2.2 VPN service skeleton: permission, start/stop, notification (merged PR #9, phone-tested)
  - [x] 2.3 Block a test domain, forward the rest (phone-tested)
  - [ ] 2.4 Network switching ✅ (Wi-Fi ↔ mobile data, reported 2026-10-29), IPv6, battery (in progress)
  - [ ] M2 acceptance: test domain blocked in Chrome, normal browsing works
- [ ] **M3** Reliable protection ← in progress
  - [x] 3.1 Bundled blocklist (HaGeZi NSFW)
  - [x] 3.2 SafeSearch (phone-tested)
  - [x] 3.3a Always block X/Reddit and image-search pictures, no switches
  - [ ] 3.3b Your own blocks: websites + words, 10-minute undo — written; CI ✅ then phone
  - [ ] 3.3c App blocking (block screen; Play policy check first)
  - [ ] 3.4 Staying on: reboot, Always-on, Private DNS detection
  - [ ] 3.5 Observed status + interruption log
  - [ ] 3.6 Signed list updates
  - [ ] 3.7 Bypass test checklist
- [ ] **M4** Accounts, partners, account deletion
- [ ] **M5** Reports and alerts
- [ ] **M6** Scripture First alarm + reminders
- [ ] **M7** Commitment controls (incl. uninstall friction + tamper alert, after a Play policy check: D-024)
- [ ] **M8** Hardening + private beta

---

## Open items (do not forget, do not start until scheduled)

- [ ] Decide Gradle wrapper version (local 9.7.1 vs CI 8.9), then commit wrapper scripts
- [ ] Confirm or change provisional decisions D-001 … D-008
- [ ] Review the 24 starter verses in `content/scripture/SOURCES.md` (in progress)
- [ ] ESV licence question (D-001); needed before sharing any build with others
- [ ] Update GitHub Actions versions (Node 20 deprecation warnings)
- [ ] Review `lint-report` artifact warnings
- [ ] TalkBack check of M1.1 screens
- [ ] In-app licences screen (font OFL texts)

## Before the private beta (M8 gate)

- [ ] Security and privacy review
- [ ] Accessibility pass
- [ ] Privacy notice published
- [ ] Signed release build
- [ ] Google Play VPN/alarm policy check
- [ ] Support, feedback and rollback process
