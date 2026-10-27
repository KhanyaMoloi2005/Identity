# Identity: Project Log

The running record of **every change** made to this project, newest first. Read the latest entry to see where we are and what's next.

Each entry lists: what changed and why, the files touched, how to verify, and the next step.

Related documents: [roadmap](roadmap.md) · [decision log](decision-log.md) · [risk register](risk-register.md) · [architecture](architecture.md) · [original spec](spec/identity_sdlc_stitch_handoff.md)

---

## 2026-10-27: M0 complete: build verified, app running on phone (branch `m0/close-out`)

**Verified (by the product owner)**
- Pull request #1 (`m0/android-skeleton` → `main`): both GitHub Actions checks passed ✅. The app compiles, passes lint, and produces an APK.
- PR #1 merged into `main` (merge commit `a8d87d1`). Actions run #3 was the automatic build of `main` after the merge.
- The debug APK was downloaded from GitHub, installed on the product owner's phone, and opened: it shows the Identity wordmark and *"Foundations build. Protection is not active yet."*

**What changed and why**
- `.github/workflows/android.yml`: each change was being built **twice** (once for the branch push, once for the pull request), wasting free Actions minutes. Now it builds on pull requests, on pushes to `main`, and on manual "Run workflow" only.
- Marked M0 complete in `docs/roadmap.md` and `README.md`; risk R-15 (unverified code) marked mitigated.

**Files updated:** `.github/workflows/android.yml`, `docs/roadmap.md`, `docs/risk-register.md`, `docs/PROJECT_LOG.md`, `README.md`.

**Workflow from now on (each change)**
1. Claude makes changes on a new branch and commits locally.
2. You publish the branch in GitHub Desktop and open a pull request.
3. Wait for the ✅ check, then optionally test the APK from that run.
4. Merge, delete the branch, then in GitHub Desktop switch to `main` and pull.

**Still open (not blocking M1):** ESV licence (D-001), confirming provisional decisions, Android Studio on your personal laptop.

**Next step:** agree the M1 plan (app shell + offline Scripture), then build it in small slices.

---

## 2026-10-27: M0 step 2: Android skeleton + cloud build (branch `m0/android-skeleton`)

> ⚠️ **Status: written but UNVERIFIED.** No build has run yet. Nothing in this entry counts as working until the GitHub Actions build passes.

**What changed and why**
- Product owner is on a **work laptop**, doesn't want Android tooling installed on it, and has no USB cable. Decision D-013: builds run in the cloud with **GitHub Actions**, and the APK can be installed on the phone straight from GitHub (no cable).
- Wrote the smallest possible Android app: it opens one screen showing the Identity wordmark (the I-cross mark drawn in code + "dentity") and the text *"Foundations build. Protection is not active yet."*, in light or dark mode following the phone.
- **The app requests no permissions at all**, not even internet. Backup and device transfer of app data are disabled for privacy.
- Product owner changed D-001 to **ESV**. Recorded, with a licensing condition: ESV is copyrighted by Crossway, and bundling whole chapters offline likely needs their permission. Development will use public-domain WEB text as placeholder content in a format that lets ESV drop in later (risk R-10 raised to High).
- Contrast-checked the colour palette: the light-theme accent is too faint for small text on parchment, so it's used only as a fill, and button text is Ink (DESIGN.md §3).
- Added decisions D-013 (cloud builds), D-014 (placeholder application ID `app.identity`), D-015 (pinned toolchain versions). Added risk R-15 (unverified code).

**How the pieces connect**
```
Phone taps icon → AndroidManifest.xml says "start MainActivity"
MainActivity.kt → wraps everything in IdentityTheme (colours) → shows WelcomeScreen
WelcomeScreen.kt → IdentityWordmark (Wordmark.kt) + status text (strings.xml)
Gradle files → describe how to compile all of the above into an .apk
.github/workflows/android.yml → GitHub's servers run Gradle and publish the .apk
```

**Files created**

| File | Purpose |
|---|---|
| `android/settings.gradle.kts` | Lists the project's modules and where libraries may be downloaded from |
| `android/build.gradle.kts` | Declares the build plugins once for the whole project |
| `android/gradle/libs.versions.toml` | All library/plugin versions in one place (D-015) |
| `android/gradle.properties` | Gradle settings (memory, AndroidX) |
| `android/gradle/wrapper/gradle-wrapper.properties` | Which Gradle version to use (8.9) |
| `android/app/build.gradle.kts` | App module: app ID, min/target Android version, dependencies |
| `android/app/proguard-rules.pro` | Release shrinking rules (empty for now) |
| `android/app/src/main/AndroidManifest.xml` | Declares the app and its one screen; no permissions |
| `android/app/src/main/res/xml/data_extraction_rules.xml` | Excludes app data from cloud backup / device transfer |
| `android/app/src/main/res/values/strings.xml` | User-visible text |
| `android/app/src/main/res/values/colors.xml`, `values-night/colors.xml` | Startup colours (light/dark) |
| `android/app/src/main/res/values/themes.xml` | Startup window theme |
| `android/app/src/main/res/drawable/ic_launcher_foreground.xml` | I-cross launcher icon artwork |
| `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | Adaptive launcher icon (normal + round) |
| `android/app/src/main/java/app/identity/MainActivity.kt` | App entry point |
| `android/app/src/main/java/app/identity/ui/theme/Color.kt`, `Theme.kt` | Colour tokens and light/dark theme |
| `android/app/src/main/java/app/identity/ui/brand/Wordmark.kt` | I-cross mark + wordmark drawn in code |
| `android/app/src/main/java/app/identity/ui/WelcomeScreen.kt` | Temporary M0 screen (+ light/dark previews) |
| `.github/workflows/android.yml` | Cloud build: compile, lint, unit tests, upload APK |

**Files updated:** `docs/decision-log.md`, `docs/roadmap.md`, `docs/risk-register.md`, `docs/design/DESIGN.md`, `docs/setup/android-studio.md`, `docs/architecture.md`, `README.md`.

**Known gaps (intentional, for now)**
- No Gradle wrapper scripts (`gradlew`) yet: they include a small binary file that is normally generated by Gradle itself. CI uses Gradle 8.9 directly; Android Studio on the personal laptop will generate the wrapper.
- Fonts (Inter, Literata) not bundled yet; the system font is used until M1.
- No tests yet: there's no logic to test. Unit tests start in M1 with the verse repository.

**How to verify (once the repo is on GitHub)**
1. GitHub → repo → **Actions** → "Android build" should finish with a green ✅ (about 5–10 min).
2. If it fails ❌: open the failed step, copy the red error lines, and paste them to Claude.
3. Download the `identity-debug-apk` artifact on your phone and install it (see [setup guide](setup/android-studio.md#no-cable-install-from-a-github-actions-build)). Expected: an "Identity" icon (cream I-cross with an amber bar on navy); opening it shows the wordmark and the status text.

**Next step**
- **You:** push `main` and `m0/android-skeleton` to `github.com/KhanyaMoloi2005/Identity` yourself using **GitHub Desktop** (decided 2026-10-27; Claude does not push).
- **Then:** fix anything the first build reports, merge `m0/android-skeleton` into `main`, and M0 is complete.

---

## 2026-10-27: M0 step 1: Project foundations and planning docs

**What changed and why**
- Read the full specification and checked the development environment. Git is installed; Android Studio, Android SDK, Java and Node.js are **not** installed yet.
- Agreed approach: Kotlin + Compose app, our own DNS-only VPN for filtering, Supabase (free tier) backend, offline-first Scripture.
- Product owner said "go ahead" without answering the open questions, so Claude's recommended defaults were recorded as **Provisional** decisions D-001 … D-008 (see [decision-log.md](decision-log.md)). They can be changed at any time.
- Product owner asked that every change be recorded in a markdown file. This log is that file (D-010). The original spec is kept unchanged as the requirements baseline.
- Created the first brand concept: the "I-cross" mark, colour tokens and typography (see [design/DESIGN.md](design/DESIGN.md)).

**Files created / moved**

| File | Purpose |
|---|---|
| `docs/spec/identity_sdlc_stitch_handoff.md` | Spec, **moved** from the project root (content unchanged) |
| `README.md` | Project overview and index of documents |
| `CHANGELOG.md` | User-visible changes per app version (empty until M1) |
| `.gitignore` | Keeps build output and secrets (keystores, `.env`, signing keys) out of Git |
| `.editorconfig` | Consistent formatting |
| `.gitattributes` | Fixes line endings (stops Windows breaking the Gradle `gradlew` script later) |
| `docs/PROJECT_LOG.md` | This log |
| `docs/roadmap.md` | Milestones M0–M8 with acceptance criteria and status |
| `docs/architecture.md` | Stack, architecture diagram, filtering design, can/cannot enforce table, security baseline |
| `docs/decision-log.md` | Decisions D-001 … D-012 |
| `docs/risk-register.md` | Risks R-01 … R-14 |
| `docs/privacy/data-inventory.md` | What data exists, where, retention |
| `docs/design/DESIGN.md` | Brand and design system draft |
| `docs/design/brand/identity-icon-concept.svg` | App icon concept |
| `docs/design/brand/identity-wordmark-concept.svg` | Wordmark concept (dark + light) |
| `docs/setup/android-studio.md` | Step-by-step setup of Android Studio and your phone |

**How to verify**
- Open the two `.svg` files in `docs/design/brand/` with a web browser (double-click, or drag them into Chrome/Edge) to see the brand concepts.
- Run `git log --oneline` in the project folder: you should see the initial commit.

**Next step (M0 step 2)**
1. **You:** follow [setup/android-studio.md](setup/android-studio.md) until `adb devices` shows your phone.
2. **You:** tell Claude your phone model, Android version and PC RAM. Also confirm or change decisions D-001 … D-008.
3. **Claude:** create the `android/` project skeleton (on a feature branch), build it, and help you run a blank Identity app on your phone.
