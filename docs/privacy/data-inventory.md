# Identity: Data Inventory

What data Identity handles, where it lives, who can see it, and how long it's kept. **Update this whenever a feature adds or changes data.**

_Last updated: 2026-10-29 (M2.3: DNS forwarding is the first data that leaves the phone)._

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
| Custom block rules: websites and words the user chose to block (no exceptions) | One file in app storage (`custom-rules.json`) | No | Permanent; removable only within 10 minutes of adding (D-029) | M3.3b |
| TikTok daily allowance: the limit chosen and any raise waiting for tomorrow (time used is read from Android's usage history each time and never copied) | One file in app storage (`tiktok-limit.json`) | No | Kept until changed | M3.3c-1 |
| Blocklist update check: an ordinary HTTPS download from GitHub (three small or one 1.4 MB file), at most once a day while protection is on. GitHub can see the phone's IP address, as any website would; nothing else is sent (no account, identifier or browsing) | Downloaded list kept in app storage (`blocklist-update/`), excluded from backup | Sent only to GitHub, only to fetch files | Replaced by each newer list | M3.6 |
| Protection history: when protection was turned on or off, or was interrupted, with the kind of problem. No domain names, no browsing | One file in app storage (`protection-history.json`), excluded from backup | No (partner alerts are M5, and will be opt-in) | Entries deleted after 30 days; at most 200 kept | M3.5 |
| Blocked-domain events (domain + time) | Room | **No**, only daily counts are sent if the user consents | 30 days, then auto-deleted | M3 |
| Protection status events (VPN started/stopped, list update failed) | Room | Summary only, if consented | 30 days | M3 |
| Scripture First sessions (scheduled, completed, missed) | Room | Only if user enables sharing (off by default) | 90 days | M6 |
| Auth session token | Encrypted local storage | Sent to Supabase for authentication only | Until sign-out | M4 |

All of the above (except the bundled library) are **excluded from Android cloud backup**.

## Sent to third parties

| Data | Sent to | Why | Stored by Identity? | Milestone |
|---|---|---|---|---|
| DNS queries that are **not** blocked (the domain name and record type, as any phone sends today) | Cloudflare for Families (`1.1.1.3`, `1.0.0.3`), D-003 | To look up the address so the site loads; a second filtering layer | **No.** Not logged or saved on the device; blocked queries are answered on the phone and go nowhere | M2.3 |

This replaces whichever DNS server the phone used before (usually the mobile network's or the Wi-Fi router's), so Cloudflare, rather than that provider, sees these lookups. The queries are not encrypted (plain DNS, as before). Cloudflare's own policy covers what it keeps.

## Backend data (Supabase)

| Data | Who can read | Retention | Milestone |
|---|---|---|---|
| Account (email, auth id) | User only | Until account deletion | M4 |
| Device record (name, last check-in, protection state) | User; partner only if consented | Until device removed / account deleted | M4 |
| Invitation (token **hash**, expiry, used flag) | Inviter only | Deleted 7 days after expiry/use | M4 |
| Partnership (users, status, consented categories) | Both parties | Until revoked + 30 days, then deleted | M4 |
| Daily report (counts per consented category) | User; active consented partner | 90 days | M5 |

**Never sent to the backend:** domain names, URLs, browsing history, reflections, saved verses, screenshots.
