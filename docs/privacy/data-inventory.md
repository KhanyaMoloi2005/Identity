# Identity: Data Inventory

What data Identity handles, where it lives, who can see it, and how long it's kept. **Update this whenever a feature adds or changes data.**

_Last updated: 2026-10-27 (planned; nothing is implemented yet)._

## Principles
1. Collect only what an enabled feature needs.
2. Browsing-related details stay on the device.
3. Anything sent to the backend is aggregated, consented and revocable.
4. Scripture reflections are private and never shared.

## On-device data

| Data | Store | Leaves device? | Retention | Milestone |
|---|---|---|---|---|
| App settings (theme, text size, reminders) | DataStore | No | Until changed / app removed | M1 |
| Verse library (bundled, public domain) | Room (seeded from app assets) | No | Ships with app | M1 |
| Saved verses | Room | No | Until user removes | M1 |
| Private reflections | Room | **Never** | Until user deletes | M1 |
| Custom allow/block rules | Room | No | Until user removes | M3 |
| Blocked-domain events (domain + time) | Room | **No**, only daily counts are sent if the user consents | 30 days, then auto-deleted | M3 |
| Protection status events (VPN started/stopped, list update failed) | Room | Summary only, if consented | 30 days | M3 |
| Scripture First sessions (scheduled, completed, missed) | Room | Only if user enables sharing (off by default) | 90 days | M6 |
| Auth session token | Encrypted local storage | Sent to Supabase for authentication only | Until sign-out | M4 |

All of the above (except the bundled library) are **excluded from Android cloud backup**.

## Backend data (Supabase)

| Data | Who can read | Retention | Milestone |
|---|---|---|---|
| Account (email, auth id) | User only | Until account deletion | M4 |
| Device record (name, last check-in, protection state) | User; partner only if consented | Until device removed / account deleted | M4 |
| Invitation (token **hash**, expiry, used flag) | Inviter only | Deleted 7 days after expiry/use | M4 |
| Partnership (users, status, consented categories) | Both parties | Until revoked + 30 days, then deleted | M4 |
| Daily report (counts per consented category) | User; active consented partner | 90 days | M5 |

**Never sent to the backend:** domain names, URLs, browsing history, reflections, saved verses, screenshots.
