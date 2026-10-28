# Identity: Project Log

The running record of **every change** made to this project, newest first. Read the latest entry to see where we are and what's next.

Each entry lists: what changed and why, the files touched, how to verify, and the next step.

Related documents: [roadmap](roadmap.md) · [decision log](decision-log.md) · [risk register](risk-register.md) · [architecture](architecture.md) · [original spec](spec/identity_sdlc_stitch_handoff.md)

---

## 2026-10-28: Added a completion checklist (branch `docs/completion-checklist`)

**What changed and why**
- Product owner asked for a checklist so the work stays focused. Added `docs/CHECKLIST.md`: a "done means done" list that applies to every task, all milestones with the M1 slices, open items, and the M8 beta gate.
- Docs only; no code changed. Work is paused until the product owner is back with their phone.

**Files:** `docs/CHECKLIST.md` (new), `docs/PROJECT_LOG.md`.

**How to verify:** open `docs/CHECKLIST.md` and confirm it matches the roadmap.

**Next step:** product owner approves M1.2 (Scripture data layer) when ready.

---

## 2026-10-27: Local tooling working: Android Studio + USB debugging (branch `docs/usb-setup`)

> ✅ **Verified by the product owner and by `adb`:** Android Studio installed on the personal laptop; Gradle sync succeeded; ▶ Run installed and opened the app on the phone over USB; **M1.1 confirmed working** (the product owner reported it fully works, which covers the checklist items left open earlier: Back behaviour, fonts, dark mode).

**What happened**
- Installed Android Studio (Standard setup). The SDK is at `%LOCALAPPDATA%\Android\Sdk`.
- Chose **USB debugging** over wireless: simpler, more reliable, charges the phone (wireless remains an optional extra).
- `adb devices` first showed `unauthorized`; tapping **Allow** on the phone fixed it. `adb` then reported `SM-A346E`, Android 16 (API 36).
- The first Gradle sync stopped because the disk had < 1 MB free. After freeing space (43 GB free), re-syncing finished successfully.

**Test device (closes the open question about the phone):** Samsung Galaxy A34 5G (SM-A346E), Android 16. D-007 (minSdk 26) stays as chosen: this one device does not change it.

**Files changed:** `docs/setup/android-studio.md` (rewritten: USB is the main route, with the disk-space and `unauthorized` fixes), `docs/PROJECT_LOG.md`.

**Open item: Gradle wrapper version (not committed).** Android Studio generated `android/gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` and changed `gradle-wrapper.properties` to **Gradle 9.7.1**, plus `android/gradle/gradle-daemon-jvm.properties`. CI and decision D-015 use **Gradle 8.9** (AGP 8.7.3). The local build works with 9.7.1, but local and CI should match. These files are left uncommitted until the version is chosen (default proposal: restore 8.9, commit the wrapper scripts, re-sync to confirm).

**Next step:** decide the wrapper version, then M1.2 (Scripture data layer, see the pause-point entry below for its criteria), pending approval.

---

## 2026-10-27: ⏸ PAUSE POINT: resume here on the personal laptop

**Where we are:** M0 complete. M1.1 (navigation shell) verified and merged. Development rules adopted. Work pauses until the product owner is on their **personal laptop** (with Android Studio, and USB or Wi-Fi debugging available).

**To resume (new Claude Code session):**
1. Clone `KhanyaMoloi2005/Identity` with GitHub Desktop and open Claude Code in the repo folder.
2. Ask Claude to read `docs/DEVELOPMENT_RULES.md`, the spec in `docs/spec/`, and this log, then continue from this entry.
3. Set up Android Studio with [setup/android-studio.md](setup/android-studio.md), then open the `android/` folder. Wireless debugging lets the ▶ Run button install straight onto the phone, so no more APK downloads.
4. With local tooling, Claude can also build and run tests on the laptop before pushing (computer checks), and the phone checks become quick.

**Proposed next task (awaiting product owner approval): M1.2, Scripture data layer**
- *Task:* verse/topic data format, a small bundled starter verse set, a repository that loads it, and the project's first unit tests. No UI change.
- *Scope note:* load the read-only bundled verses from a JSON asset in memory, with no Room database yet. Room arrives in 1.5 for saved verses and reflections. Record this as a decision when implemented.
- *Acceptance criteria:*
  1. Format matches spec §5 (`id`, `reference`, `text`, `translation`, `topic_tags`, `optional_reflection`, `is_active`) and is documented.
  2. Starter set: the eight spec topics (lust, temptation, self-control, purity, renewing the mind, repentance, grace, hope), at least 3 verses each, text copied from the official public-domain WEB source (not typed from memory), source recorded. Placeholder until ESV licensing is resolved; the product owner reviews verse choices.
  3. Repository: list topics, list a topic's verses, get one verse; inactive verses excluded.
  4. Unit tests: real bundled file parses; every verse has a known topic and every topic has at least one verse; malformed/incomplete data rejected safely; topic filtering; inactive verses hidden.
  5. CI ✅ with **more than 0 tests run**.
  6. Still no permissions or internet; no phone check needed (no UI change).
- *Likely files:* `android/app/src/main/assets/scripture/`, `android/app/src/main/java/app/identity/data/scripture/`, `android/app/src/test/java/app/identity/data/scripture/`, `android/gradle/libs.versions.toml`, `android/app/build.gradle.kts` (JUnit, test-only), `content/scripture/SOURCES.md`, docs.

**Follow-up list (not started; move into `docs/FOLLOW_UPS.md` with M1.2)**
- Update GitHub Actions versions (Node 20 deprecation, `setup-java@v5`, Ubuntu runner migration warnings from run #4).
- Review the `lint-report` artifact for warnings (lint passing only means no errors).
- Unit tests: none exist yet (fixed by M1.2).
- M1.1 phone checklist items not individually confirmed: Back behaviour, fonts, dark mode, TalkBack.
- In-app licences screen (font OFL texts) in Settings.

**Decisions awaiting the product owner**
- D-001: ESV licence from Crossway (blocks distributing any build to others).
- D-002 to D-008, D-014, D-015: still provisional.
- Phone model and Android version (confirms minimum Android version, D-007).

---

## 2026-10-27: Development rules adopted (branch `docs/development-rules`)

**What changed and why**
- Product owner supplied *"Identity: Incremental Development Rules for Claude Code"*, which governs **how** work is done (one task at a time; define, inspect, plan, implement, verify, report, pause; required start/end response format; honest verification). The SDLC spec remains the product specification.
- Saved as [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md), text unchanged apart from a note at the top giving the spec's location (`docs/spec/`).
- Also recorded: **M1.1 verified.** PR #2 check (Actions run #4) passed ✅ (`assembleDebug lintDebug testDebugUnitTest`, 2m 51s). The product owner installed the APK and saw the navigation, then merged PR #2.

**Files:** `docs/DEVELOPMENT_RULES.md` (new), `docs/PROJECT_LOG.md`, `README.md` (index link).

**Next step:** initial assessment per rules §12 (read-only), then await approval of the proposed task.

---

## 2026-10-27: M1.1: Navigation shell + bundled fonts (branch `m1/navigation-shell`)

> ✅ **Verified 2026-10-27:** PR #2 build passed; APK installed on the product owner's phone and navigation seen; PR #2 merged. (Detailed checklist items such as dark mode and Back behaviour were not individually reported.)

**What changed and why**
- Product owner approved the M1 plan (six slices) and agreed to downloading the fonts and to Claude drafting the starter verse list (in 1.2).
- **Bottom navigation** with four tabs: Home (shield), Scripture (open book), Partner (two people), Settings (sliders). Each tab is a placeholder for now, with honest text about what's coming.
- **Fonts bundled:** Inter for the interface, Literata for Scripture. Downloaded from Google's official fonts repo, checked to be genuine TrueType files (sizes matched GitHub's listing). OFL licences ship inside the APK (D-016).
- **Home** shows the wordmark and a Protection card: *"Not active yet. Filtering arrives in a later update."*
- **Scripture** shows a sample verse (Psalm 119:105, WEB, public domain) in the Literata reading font, so you can judge the reading style.
- Removed the temporary M0 `WelcomeScreen`. Version is now 0.0.2 (versionCode 2) so it installs as an update.
- New libraries: Navigation Compose 2.8.5 and kotlinx.serialization 1.7.3 (D-017). Still **no permissions, no internet**.

**How the pieces connect**
```
MainActivity → IdentityTheme (colours + fonts) → IdentityApp
IdentityApp  → Scaffold: [ NavHost (current screen) ] over [ NavigationBar (4 tabs) ]
Tap a tab    → navController.navigate(route) → NavHost shows HomeScreen / ScriptureScreen / PartnerScreen / SettingsScreen
Each screen  → ScreenColumn (shared padding, scrolling, heading for screen readers)
```

**Files created**

| File | Purpose |
|---|---|
| `android/app/src/main/res/font/inter.ttf`, `literata.ttf` | Bundled variable fonts |
| `android/app/src/main/assets/licenses/inter-OFL.txt`, `literata-OFL.txt` | Font licences, shipped in the app |
| `android/app/src/main/res/drawable/ic_nav_{home,scripture,partner,settings}.xml` | Tab icons |
| `.../ui/theme/Type.kt` | Inter/Literata font families, app typography, `ScriptureBody` style |
| `.../ui/navigation/TopLevelDestination.kt` | The four tabs and their type-safe routes |
| `.../ui/navigation/IdentityApp.kt` | App frame: screen area + bottom bar, tab switching |
| `.../ui/components/ScreenColumn.kt` | Shared layout for tab screens |
| `.../feature/home/HomeScreen.kt` | Home tab |
| `.../feature/scripture/ScriptureScreen.kt` | Scripture tab (sample verse) |
| `.../feature/accountability/PartnerScreen.kt` | Partner tab (placeholder) |
| `.../feature/settings/SettingsScreen.kt` | Settings tab (placeholder) |

(`...` = `android/app/src/main/java/app/identity`)

**Files updated:** `android/gradle/libs.versions.toml`, `android/build.gradle.kts`, `android/app/build.gradle.kts` (plugins, dependencies, version 0.0.2), `MainActivity.kt` (now shows `IdentityApp`), `ui/theme/Theme.kt` (typography), `ui/brand/Wordmark.kt` (Inter Medium), `res/values/strings.xml`, `CHANGELOG.md`, `docs/roadmap.md`, `docs/decision-log.md` (D-016 to D-018).
**Files deleted:** `ui/WelcomeScreen.kt` (replaced by the Home tab).

**How to verify**
1. Publish branch `m1/navigation-shell` in GitHub Desktop and open a pull request into `main`. Wait for the ✅.
2. Install the APK from that run (it installs over the old one).
3. Check:
   - four tabs at the bottom; the selected tab's icon sits in an amber pill
   - tapping each tab switches screens; the Back button from any tab returns to Home, then exits
   - the Scripture tab's sample verse is in a serif font (Literata), and the rest of the app is in a clean sans font (Inter)
   - switch the phone between light and dark mode: colours follow
   - with TalkBack on (optional), tab names are read aloud and screen titles are announced as headings

**Next step:** 1.2, the Scripture data layer and starter verse list.

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
