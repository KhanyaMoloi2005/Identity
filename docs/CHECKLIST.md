# Identity: Completion Checklist

_The one page to check when you ask "where are we?" Tick boxes here as work finishes. Details live in [roadmap.md](roadmap.md); the history lives in [PROJECT_LOG.md](PROJECT_LOG.md)._

**Current task:** M1.2 Scripture data layer (written, awaiting CI and product owner review)
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
- [ ] **M1** App shell + offline Scripture
  - [x] 1.1 Navigation shell + fonts (merged, phone-verified)
  - [ ] 1.2 Scripture data layer + first unit tests (written; CI green with tests run, then verse review)
  - [ ] 1.3 Browse by topic
  - [ ] 1.4 "I'm struggling" flow
  - [ ] 1.5 Saved verses + private reflections
  - [ ] 1.6 Onboarding + privacy disclosures
  - [ ] M1 acceptance: works in airplane mode, no internet permission, TalkBack labels, verse tests pass
- [ ] **M2** DNS filtering proof of concept
- [ ] **M3** Reliable protection
- [ ] **M4** Accounts, partners, account deletion
- [ ] **M5** Reports and alerts
- [ ] **M6** Scripture First alarm + reminders
- [ ] **M7** Commitment controls
- [ ] **M8** Hardening + private beta

---

## Open items (do not forget, do not start until scheduled)

- [ ] Decide Gradle wrapper version (local 9.7.1 vs CI 8.9), then commit wrapper scripts
- [ ] Confirm or change provisional decisions D-001 … D-008
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
