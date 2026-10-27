# Identity: Project Log

The running record of **every change** made to this project, newest first. Read the latest entry to see where we are and what's next.

Each entry lists: what changed and why, the files touched, how to verify, and the next step.

Related documents: [roadmap](roadmap.md) · [decision log](decision-log.md) · [risk register](risk-register.md) · [architecture](architecture.md) · [original spec](spec/identity_sdlc_stitch_handoff.md)

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
