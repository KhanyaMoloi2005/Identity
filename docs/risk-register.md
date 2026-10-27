# Identity: Risk Register

_Last updated: 2026-10-27. Likelihood/Impact: L = low, M = medium, H = high._

| ID | Risk | L | I | Mitigation | Owner milestone |
|---|---|---|---|---|---|
| R-01 | User disables the VPN, force-stops or uninstalls the app, factory-resets, or uses another device | H | H | Disclose clearly; guide the user to enable Always-on VPN + "Block connections without VPN"; detect and record interruptions; backend flags missed check-ins neutrally ("cause unknown") | M3, M5 |
| R-02 | DNS bypasses: Android Private DNS, in-browser DNS-over-HTTPS, another VPN app, hardcoded DNS in apps, adult content on general-purpose domains | H | H | Detect Private DNS and warn; block known DoH endpoints; answer NXDOMAIN for `use-application-dns.net` (Firefox DoH opt-out); SafeSearch enforcement; document what's not covered | M2, M3 |
| R-03 | Google Play policy: `VpnService` declaration, data disclosure, full-screen intent and exact-alarm restrictions, accessibility misuse | M | H | No accessibility service; data minimisation; check current policy before beta; fallbacks for restricted alarm features | M6, M8 |
| R-04 | Alarm unreliability: exact-alarm permission (Android 12+/14+), full-screen intent limits, OEM background killing, Doze, reboot, time changes | H | M | `setAlarmClock()`; runtime permission checks; fallback high-priority notification; reschedule on boot/time/zone change; test on physical devices | M6 |
| R-05 | Tampered or corrupted blocklist update silently weakens protection | L | H | Ed25519-signed updates; public key in app, private key offline; keep the last good list on failure; record "update failed" event | M3 |
| R-06 | Sensitive data exposure (domains, reflections, reports) | M | H | Domains stay on device; backup exclusions; no URLs/tokens in logs; RLS deny-by-default; two-account tests | All |
| R-07 | Coercive use (installed on someone else's phone to monitor them) | M | H | Adults only; device holder owns the account; visible VPN notification; aggregates only; user can revoke anytime | M4, M8 |
| R-08 | VPN breaks connectivity (IPv6, network transitions, captive portals, DNS timeouts) | M | H | DNS-only routing; upstream timeouts + fallback resolver; transition and IPv6 test cases | M2, M3 |
| R-09 | Supabase free-tier limits (inactivity pause, quotas) | M | M | Monitor usage; daily check-ins keep the project active; budget for a paid tier before public launch | M4+ |
| R-10 | Bible translation licensing: **ESV chosen (D-001) is copyrighted by Crossway**; bundling full text offline likely needs a licence | H | H | Product owner checks/requests Crossway permission before any distribution; develop with public-domain WEB placeholder in a translation-neutral format; record sources and licences in `content/scripture/` | M1 |
| R-15 | Code is unverified while no build has run (no local Android tooling, D-013) | L | M | **Mitigated 2026-10-27:** cloud build passes on every PR. Rule stays: never mark work "done" until the GitHub Actions build passes | M0 ✅ |
| R-11 | Blocklist licensing (some lists restrict commercial use) | M | M | Verify each source's licence before bundling; record in `content/blocklists/SOURCES.md` | M3 |
| R-12 | Defects in AI-generated security-critical code (filter, RLS, invitations) | M | H | Tests for every critical path; diff review; independent human review before public release | All |
| R-13 | False positives (legitimate sites blocked) | M | M | User allow rules (with cooldown under commitment mode); easy reporting | M3 |
| R-14 | Battery drain | M | M | DNS-only routing; measure over 24 h on a physical device | M2, M3 |
