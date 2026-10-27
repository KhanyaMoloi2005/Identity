# Identity: Decision Log

Every significant decision, why it was made, and its status.

- **Accepted:** confirmed by the product owner.
- **Provisional:** Claude's recommended default; work may proceed on it, but the product owner should confirm or change it.
- **Superseded:** replaced by a later decision (kept for history).

_Last updated: 2026-10-27._

| ID | Date | Decision | Status |
|---|---|---|---|
| D-001 | 2026-10-27 | Bible translation: **English Standard Version (ESV)** | Provisional |
| D-002 | 2026-10-27 | Filtering engine: own DNS-only `VpnService` + dnsjava | Provisional |
| D-003 | 2026-10-27 | Upstream DNS: **Cloudflare for Families `1.1.1.3`** | Provisional |
| D-004 | 2026-10-27 | Partner experience V1: partner view in the Android app + push | Provisional |
| D-005 | 2026-10-27 | Backend logic: Supabase Edge Functions instead of a separate Node.js server | Provisional |
| D-006 | 2026-10-27 | Scripture First release = optional Morning Focus blocklist; SP-05 stays "Should have" | Provisional |
| D-007 | 2026-10-27 | Minimum Android version 8.0 (API 26); newer features enabled only when available | Provisional |
| D-008 | 2026-10-27 | Code hosting: private GitHub repo; licence (open vs closed) undecided | Provisional |
| D-009 | 2026-10-27 | No Google Stitch; Claude designs brand and UI directly in Compose | Accepted |
| D-010 | 2026-10-27 | Original spec kept unchanged in `docs/spec/`; all progress recorded in `docs/PROJECT_LOG.md` | Accepted |
| D-011 | 2026-10-27 | Manual dependency wiring (no Hilt) to start | Provisional |
| D-012 | 2026-10-27 | No accessibility service, no analytics SDKs, no HTTPS interception | Accepted (spec) |
| D-013 | 2026-10-27 | Builds run in the cloud (GitHub Actions); no Android tooling on the work laptop | Accepted |
| D-014 | 2026-10-27 | Application ID `app.identity` is a placeholder; final ID chosen before first Play upload | Provisional |
| D-015 | 2026-10-27 | Pinned toolchain: AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, Compose BOM 2024.12.01, compile/target SDK 35 | Provisional |

---

### D-001: Bible translation: ESV
**History:** Claude first proposed the World English Bible (public domain). On 2026-10-27 the product owner changed this to the **English Standard Version (ESV)**.
**Licensing condition (must resolve before any build is distributed):** the ESV is copyrighted by Crossway. Bundling whole chapters/books offline in an app (needed by Scripture First) requires permission from Crossway. Their free quotation allowance and the ESV API terms are limited and do not, as far as Claude knows, cover storing the full text in an app. Action: product owner to check Crossway's current permissions/API terms (crossway.org / api.esv.org) and request a licence if needed.
**Interim plan:** build the Scripture features against a translation-neutral data format using public-domain WEB text as **development placeholder** content, so ESV can be dropped in once licensed, with no code changes.
**Open:** who curates the topic verse lists? Claude can draft references for the product owner to review.

### D-002: Filtering engine
**Context:** Spec says evaluate maintained, appropriately licensed options before writing a packet processor.
**Options:** (a) reuse DNS66 / personalDNSfilter / AdAway: GPLv3 would force the whole app under GPL, and they route more traffic; (b) RethinkDNS engine: large Go codebase, heavy to own; (c) own thin DNS-only VPN routing a single fake DNS address, about 300 lines of packet handling, plus dnsjava (BSD licence) for DNS message parsing.
**Decision:** (c). Less traffic through our code means less battery, fewer connectivity bugs, less sensitive data handled, and a component small enough to test thoroughly.

### D-003: Upstream DNS
**Decision:** forward allowed queries to Cloudflare for Families (`1.1.1.3`, `1.0.0.3`, plus IPv6), a free second filtering layer.
**Trade-off:** the chosen upstream provider sees the DNS queries it resolves (true of any upstream). Alternative: neutral `1.1.1.1`.

### D-004: Partner experience
**Options:** (a) partner view in the same Android app + FCM push, R0; (b) email digests, needs a sending domain at about R150–R250/year; (c) web dashboard, more work.
**Decision:** (a) for V1. Revisit if partners mostly use iPhones.

### D-005: Backend logic
**Context:** Spec recommends Node.js/TypeScript for custom logic, used only when needed.
**Decision:** Supabase Edge Functions (TypeScript on Deno). Same language, R0, no server to host or patch. Move to a Node.js service only if a need appears.

### D-006: Scripture First release behaviour
**Decision:** optionally, a user-chosen "Morning Focus" list (e.g. social media, news) stays blocked by Identity's own filter until the morning reading is marked complete. This is enforceable within the VPN's limits and is honest about them. Default off.

### D-007: Minimum Android version
**Decision:** minSdk 26 (Android 8.0). Always-on/lockdown detection (Android 10+) and exact-alarm checks (Android 12+) are used only where available. Final choice depends on the product owner's test device.

### D-008: Code hosting
**Decision:** private GitHub repository for backup and free CI minutes. Open-source vs closed licence still to be decided. Nothing in D-002 forces GPL.

### D-011: Dependency wiring
**Decision:** a hand-written `AppContainer` creates repositories and passes them to ViewModels. Easier to read for a beginner; Hilt can replace it later without changing feature code much.

### D-013: Cloud builds
**Context:** The current laptop is a work machine; the product owner doesn't want Android Studio installed on it and has no USB cable for now.
**Decision:** GitHub Actions builds, lints and unit-tests the app on every push and attaches a debug APK to each run. Local Android Studio setup moves to the product owner's personal laptop later.
**Consequence:** until the repo is pushed to GitHub (or built on the personal laptop), code is **unverified**: it hasn't been compiled. Pushing to GitHub is the product owner's call (consider employer policy about personal repos on a work machine).
**No-cable options for running on a phone:** download the APK from the GitHub run directly on the phone and install it, or use Android 11+ Wireless debugging from the personal laptop.

### D-014: Application ID
**Context:** The application ID uniquely identifies the app on Google Play and can never change after the first upload.
**Decision:** `app.identity` as a placeholder. Pick a final ID before M8, ideally based on a domain the product owner controls (e.g. `za.co.<domain>.identity`).

### D-015: Toolchain versions
**Decision:** a known-compatible, well-established set (see `android/gradle/libs.versions.toml`). Play currently requires a recent target SDK for new uploads, so targetSdk will be raised to the then-required level before beta (M8). Upgrades happen one at a time, each verified by a build.
