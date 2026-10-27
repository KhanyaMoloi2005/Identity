# Identity

An Android-first, privacy-focused pornography protection and personal accountability app, with optional private Scripture support and a **Scripture First** morning alarm.

> **Honesty note:** Identity makes bypassing protection harder and makes interruptions visible. It **cannot** make an Android phone impossible to bypass. See [docs/architecture.md](docs/architecture.md#what-identity-can-and-cannot-enforce).

## Status

**Milestone M0: Foundations** (in progress). A minimal Android app skeleton exists in `android/` but has **not been built or verified yet**. Builds run in the cloud via GitHub Actions. See [docs/PROJECT_LOG.md](docs/PROJECT_LOG.md) for the latest state.

## Where to find things

| Document | Purpose |
|---|---|
| [docs/spec/identity_sdlc_stitch_handoff.md](docs/spec/identity_sdlc_stitch_handoff.md) | Original product specification (requirements baseline, not edited) |
| [docs/PROJECT_LOG.md](docs/PROJECT_LOG.md) | **Running log of every change**: start here |
| [docs/roadmap.md](docs/roadmap.md) | Milestones M0–M8 with acceptance criteria |
| [docs/architecture.md](docs/architecture.md) | Tech stack, architecture, folder structure, platform limits |
| [docs/decision-log.md](docs/decision-log.md) | Every significant decision and why |
| [docs/risk-register.md](docs/risk-register.md) | Known risks and mitigations |
| [docs/privacy/data-inventory.md](docs/privacy/data-inventory.md) | What data exists, where it lives, how long it's kept |
| [docs/design/DESIGN.md](docs/design/DESIGN.md) | Brand, colours, typography, components |
| [docs/setup/android-studio.md](docs/setup/android-studio.md) | Setting up your Windows PC and Android phone |
| [CHANGELOG.md](CHANGELOG.md) | User-visible changes per app version |

## Tech stack (summary)

Kotlin · Jetpack Compose · Room · DataStore · Android `VpnService` (DNS-only filtering) · WorkManager · AlarmManager · Supabase (Postgres + RLS + Auth + Edge Functions in TypeScript).

## Security rules for contributors

- Never commit secrets: no Supabase service-role key, database password, signing keystore or blocklist-signing private key. The `.gitignore` blocks common cases, but always check `git status` before committing.
- Browsing-related data stays on the device. Only consented, aggregated summaries go to the backend.
- Every backend table has Row Level Security, deny-by-default, tested with two separate accounts.
