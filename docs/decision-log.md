# Identity: Decision Log

Every significant decision, why it was made, and its status.

- **Accepted:** confirmed by the product owner.
- **Provisional:** Claude's recommended default; work may proceed on it, but the product owner should confirm or change it.
- **Superseded:** replaced by a later decision (kept for history).

_Last updated: 2026-10-27._

| ID | Date | Decision | Status |
|---|---|---|---|
| D-001 | 2026-10-27 | Bible translation: **World English Bible (WEB)** | Provisional |
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

---

### D-001: Bible translation: WEB
**Context:** Scripture First needs whole chapters offline, so a full translation must be bundled. ESV and NIV require paid or negotiated licences.
**Decision:** World English Bible: modern English, public domain. BSB and KJV are also public domain and could be added later.
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
