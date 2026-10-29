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
| D-016 | 2026-10-27 | Bundle Inter + Literata as variable fonts (SIL OFL, licences shipped in the APK) | Accepted |
| D-017 | 2026-10-27 | Navigation Compose 2.8.5 with type-safe routes; kotlinx.serialization 1.7.3 | Accepted |
| D-018 | 2026-10-27 | Custom line icons for the tab bar instead of the large Material extended icon library | Accepted |

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

### D-016: Fonts
**Decision:** download Inter and Literata from Google's official fonts repository (`github.com/google/fonts`, `ofl/` folder) and bundle them in `res/font/`. They're variable fonts (one file holds every weight), about 1.8 MB together. Italic files skipped for now. The SIL Open Font Licence requires the licence to travel with the font, so the OFL texts ship inside the app in `assets/licenses/`. A licences screen in Settings will display them later.
**Why bundle instead of downloading at runtime:** works offline, needs no internet permission, and sends no request to Google when the app opens.

### D-017: Navigation
**Decision:** Google's Navigation Compose library with **type-safe routes**: each screen is a Kotlin type, so a mistyped route fails the build instead of crashing on the phone. Type-safe routes require kotlinx.serialization (JetBrains, Apache 2.0), which 1.2 needs anyway to read verse data files.

### D-018: Tab icons
**Decision:** four hand-drawn line icons (shield, open book, two people, sliders) as vector drawables. Material's built-in core icons have no book icon, and the extended icon library is a very large dependency for four icons.

### D-015: Toolchain versions
**Decision:** a known-compatible, well-established set (see `android/gradle/libs.versions.toml`). Play currently requires a recent target SDK for new uploads, so targetSdk will be raised to the then-required level before beta (M8). Upgrades happen one at a time, each verified by a build.

### D-019: Scripture data storage and source (M1.2)
**Decision:** the read-only starter verses live in a JSON file (`assets/scripture/verses.json`) loaded into memory at startup. No Room database yet: Room arrives in M1.5, where saved verses and reflections actually need to be stored. Topics are fixed in code (`StarterTopics`) and verses refer to them by id in `topic_tags`.
**Source:** verse text is copied by script from the official World English Bible download at ebible.org (public domain), not typed from memory. See `content/scripture/SOURCES.md`. The WEB is a **placeholder** until the ESV licensing question (D-001) is settled.
**Safety:** bad data returns a failed `Result` instead of crashing; unknown topics, blank fields and duplicate ids are rejected.

### D-020: Build the filtering core before the Scripture screens (2026-10-28)
**Decision:** after M1.2, pause M1.3 to M1.6 and do **M2 (DNS filtering proof of concept)** next. Milestone numbers stay the same; only the order changes. M1.3 to M1.6 resume after M2.
**Why:** filtering is the core of the product and its riskiest part (VPN behaviour, battery, network changes, risks R-01, R-02, R-08). If it proves harder than expected, we want to know before building more around it. The Scripture screens are low-risk and independent, so waiting costs nothing. The product owner said the priority is back end and functionality; the UI can change later.
**Trade-off:** M2 needs a real-phone check (VPN permission, test domain, Wi-Fi/mobile-data switching), so the computer-only parts (DNS parsing, domain matching, tests) come first.

### D-021: dnsjava for DNS messages; `:filter-core` as a plain Kotlin module (2026-10-28)
**Decision:** use **dnsjava 3.6.5** (BSD-3-Clause, about 600 KB, runtime dependency only on `slf4j-api`) to read DNS queries and build replies. Only its message parsing is used, not its resolver. The rest of the app sees just `DnsPackets.parseQuery` and `DnsPackets.nxDomainResponse`, so the library can be swapped later.
**Why a library:** packets come from other apps on the phone (untrusted input) and DNS has known traps such as looping name-compression pointers. A mature parser has already met these. M3 SafeSearch also needs to build real answer records.
**Why not 3.7.0:** it was released on 2026-10-25, three days before this decision. We adopt new versions deliberately, one at a time (D-015).
**Module:** `android/filter-core`, plain Kotlin/JVM (Java 11 bytecode like the app), no Android code, so its tests run anywhere. The app does not use it yet (that starts in M2.2); the release-build shrinker (R8) rules for dnsjava will be checked then, on a phone.
**CI:** the workflow now also runs `:filter-core:test`. `testDebugUnitTest` only covers the `app` module and would have skipped these tests silently.

### D-022: How the VPN runs as a foreground service (2026-10-29)
**Decision:** `IdentityVpnService` is a foreground service of type **`systemExempted`** (permission `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`), with a silent, ongoing "Identity protection is on" notification. On Android 13+ the app asks for the notification permission once, just before the VPN request. The M2.2 tunnel has an address but **no routes and no DNS server**, so no traffic goes through it yet.
**Why `systemExempted`:** since Android 14 every foreground service must declare a type. Android's documentation lists "VPN apps" as allowed to use `systemExempted`. Other types (`specialUse`, `dataSync`) don't fit and bring their own Play review or time limits.
**Why no routes yet:** if we sent DNS to the tunnel before we can answer it (M2.3), every website would stop loading while protection is on. This slice proves only the permission, start/stop and notification.
**Not yet:** automatic restart after a crash or reboot, and Always-on VPN detection, are M3. Until then the on/off state lives in memory and shows "Off" if Android ends the app.
**To check on the phone:** that Android 14+ accepts the `systemExempted` type after the user taps OK. The docs don't spell out exactly how a VPN app qualifies. If Android refuses, the service stops cleanly instead of crashing, and we will revisit this decision.
