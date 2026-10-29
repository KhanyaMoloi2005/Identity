# Identity: Architecture

_Last updated: 2026-10-27 (M0). Changes to this file are recorded in [PROJECT_LOG.md](PROJECT_LOG.md)._

## 1. What Identity does

1. **Protection:** an on-device, DNS-only VPN blocks known adult domains, enforces SafeSearch, and applies the user's own allow/block rules. A dashboard shows the *observed* protection status.
2. **Honest monitoring:** observable events ("VPN interrupted; cause unknown", "blocklist update failed") are recorded locally and never presented as proof of intent.
3. **Opt-in accountability:** a secure, expiring, single-use invitation links a partner, who receives **only** the aggregated categories the user consents to. The user can revoke at any time.
4. **Private spiritual support:** offline Scripture by topic, "I'm struggling" flow, saved verses, private reflections, and the Scripture First morning alarm. Nothing is shared unless explicitly enabled.

## 2. Technology stack

| Layer | Choice | Reason |
|---|---|---|
| Language / UI | Kotlin + Jetpack Compose (Material 3, custom theme) | Required by spec; modern official Android toolkit |
| Navigation | Navigation Compose | Official, no third-party risk |
| Dependency wiring | Manual `AppContainer` (no Hilt initially) | Fewer hidden layers while learning; can migrate later |
| Local data | Room (verses, events, rules) + DataStore (settings) | Official; SQL checked when the app compiles |
| Filtering | Own thin DNS-only `VpnService` + **dnsjava** (BSD) for DNS message parsing | See §4 |
| Background work | Foreground `VpnService`, WorkManager (check-ins, list updates), `AlarmManager.setAlarmClock()` (alarm) | The supported mechanism for each job |
| Backend | Supabase free tier: Postgres, Auth, RLS, Edge Functions (TypeScript/Deno) | R0, no server to host (decision D-005) |
| Android ↔ backend | `supabase-kt` with the **publishable (anon) key only** | That key is designed to be public; RLS protects the data |
| Partner alerts | Firebase Cloud Messaging (free) | Only when partner uses the app (D-004) |
| Tests | JUnit, Compose UI tests, physical device, RLS tests against a dev Supabase project | No Docker needed |

## 3. High-level architecture

```
┌──────────────────────── Android device ────────────────────────┐
│  Compose UI (screens) → ViewModels → Repositories              │
│                                         │                      │
│      Room / DataStore (local, private) ◄┘                      │
│                                                                │
│  IdentityVpnService ── uses ──► :filter-core (pure Kotlin)     │
│   (DNS only)                    DNS parse · matcher · SafeSearch│
│                                                                │
│  AlarmManager → Scripture First   WorkManager → check-in/lists │
└──────────────┬──────────────────────────────┬──────────────────┘
               │ HTTPS: daily counts,         │ signed blocklist
               │ device check-ins only        │ download
               ▼                              ▼
   Supabase (Auth · Postgres+RLS · Edge Functions)   static file host
               │
               ▼
   Partner: Android app partner view (+ email/web later)
```

**Layers inside the app:**
- **UI (Compose screens):** draws state and sends user actions. No business logic.
- **ViewModel:** holds screen state and survives screen rotation.
- **Repository:** the single place a feature gets its data (local DB, settings, or backend).
- **Data sources:** Room, DataStore, Supabase client.

**Privacy boundary:** domain names, browsing-related events and Scripture reflections **never leave the device**. The backend receives only counts and status (e.g. *"Mon: 3 blocked requests, protection interrupted once for 12 min, cause unknown"*).

## 4. Filtering engine

**Choice:** a DNS-only VPN that we write ourselves, with dnsjava for parsing DNS messages.

**How it works:**
1. `IdentityVpnService` creates a virtual network interface and tells Android to use a fake DNS server address (e.g. `10.111.222.1`).
2. Only that single address is routed into the tunnel. **Normal web traffic never passes through our code.**
3. For each DNS query: if the domain matches the blocklist (or the user's block rules) and isn't allow-listed, we answer "no such domain". SafeSearch domains get their official safe-mode answer (e.g. `forcesafesearch.google.com`). Otherwise we forward the query to the upstream resolver through a socket that bypasses our own VPN, and return its answer.

**Alternatives considered:** reusing DNS66 / personalDNSfilter / AdAway code (GPLv3 would bind the whole app, and they route more traffic than needed); RethinkDNS's engine (a large Go codebase, too heavy to own). See decision D-002.

## 5. What Identity can and cannot enforce

| Identity **can** | Identity **cannot** |
|---|---|
| Block known adult domains while its VPN is running | Prevent the user disabling the VPN, force-stopping, or uninstalling the app |
| Enforce SafeSearch on major search engines and YouTube | Turn on Android's "Always-on VPN" itself (only the user can, in system Settings). "Block connections without VPN" must stay off: it would cut the internet for a DNS-only VPN (D-033) |
| Detect and record that the VPN stopped or permission was revoked | See full URLs or content inside HTTPS pages, or decrypt traffic (out of scope) |
| Detect whether Always-on VPN is enabled (Android 10+) and show it | Block adult content on general-purpose domains (X, Reddit, etc.) by domain alone |
| Notice (via the backend) that a device stopped checking in | Know *why* a device stopped checking in (offline, phone off, app removed…) |
| Delay its *own* settings changes (commitment cooldowns) | Prevent a factory reset, use of another device, or another VPN app replacing ours |
| Ring a morning alarm and show the reading flow | Lock the whole phone, block emergency calls, or prove that someone actually read the passage |

Known DNS bypass paths, and how we'll handle them, are tracked in the [risk register](risk-register.md) (R-02).

## 6. Folder structure (target)

```
Identity/
├── README.md · CHANGELOG.md · .gitignore · .gitattributes · .editorconfig
├── .github/workflows/   cloud build (GitHub Actions, D-013)
├── docs/
│   ├── spec/            original specification (unchanged baseline)
│   ├── PROJECT_LOG.md   running log of every change
│   ├── roadmap.md · architecture.md · decision-log.md · risk-register.md
│   ├── privacy/data-inventory.md
│   ├── design/DESIGN.md + brand/
│   └── setup/android-studio.md
├── android/                     ← skeleton created in M0 step 2 (built by GitHub Actions)
│   ├── filter-core/             pure Kotlin (no Android code) → fast tests, reusable later
│   └── app/src/main/java/app/identity/
│       ├── ui/          theme, shared components, navigation
│       ├── feature/     onboarding · dashboard · rules · scripture · morning · accountability · settings
│       ├── protection/  VpnService, status monitor, interruption events
│       ├── alarm/       scheduling, boot/time-change handling
│       └── data/        Room, DataStore, repositories, remote (Supabase)
├── backend/supabase/    ← created in M4
│   ├── migrations/      SQL tables + RLS policies
│   ├── functions/       Edge Functions (invites, reports, deletion)
│   └── tests/           RLS tests with two accounts
└── content/
    ├── scripture/       public-domain Bible text + curated topic lists   ← M1
    └── blocklists/      sources + build script → compact signed file     ← M3
```

## 7. Security baseline (applies to every milestone)

- No privileged secrets in the app. The only backend credential shipped is the Supabase publishable key.
- HTTPS only; cleartext traffic disabled by the network security config.
- Sensitive local data (events, reflections, rules) excluded from Android cloud backup.
- Logs never contain URLs, domains, tokens or passwords.
- Every Supabase table: RLS enabled, deny-by-default, tested with two accounts plus a revoked partner.
- Partner invitations: 128-bit random token, only its hash stored, single-use, 48 h expiry, rate-limited.
- Blocklist updates are signed; the app holds only the public verification key.
- Minimal exported Android components; the VPN service is protected by `BIND_VPN_SERVICE`.
- No accessibility service, no analytics SDKs, no HTTPS interception.
