# M4 plan: accounts, partners and account deletion

_Status: **decisions made 2026-10-29 (D-037); slice 4.0 (the Supabase project) is next.** Nothing in this plan is built yet. It follows the roadmap's M4 ([roadmap](../roadmap.md)), the spec (§4 Backend, §7 Accountability) and decisions D-004 (partner view inside the same app) and D-005 (Supabase)._

## 1. What M4 is, and what it is not

**M4 delivers:** a person can create an account, register their phone, invite an accountability partner with a secure expiring code, the partner can accept it from their own account, either side can end it, and the person can delete their whole account. All of it is protected by database rules that are tested with two accounts and a revoked partner.

**M4 does not deliver:** reports, alerts, "device hasn't checked in" messages or push notifications. Those are **M5**. M4 only stores *who may see what* (the consent choices), so M5 has something to obey.

**Accounts stay optional.** Filtering, the TikTok limit, Shorts/Reels, the blocklist updates and Scripture all work without any account and without internet, exactly as today. An account only adds the accountability partner.

## 2. What the server will know (and what it never will)

| Stored on the server | Why | Never stored on the server |
|---|---|---|
| Account email, a hashed password (held by Supabase Auth, not by us) | Sign-in | Websites visited, domain names, search words |
| A nickname the person chooses | What a partner sees instead of an email | Scripture reflections, saved verses |
| Registered phones: model, Android version, app version, last seen | Device registration, and "last seen" for M5 | The user's custom blocks, TikTok limit, switches |
| Invitations: a **hash** of the code, expiry, used or not | Secure invitations | The invitation code itself (only its hash is kept) |
| Partnerships: who, since when, ended or not, and the **sharing choices** | Consent | Anything about what the person does on the phone (until M5, and then only counts and status) |

This matches the architecture's privacy boundary: *domain names, browsing-related events and Scripture reflections never leave the device.* The data inventory ([privacy/data-inventory.md](../privacy/data-inventory.md)) is updated in M4.4.

## 3. The design

### Backend: Supabase free tier, no custom server

The database rules do the heavy work, so M4 needs **no Edge Functions and no server of our own**:
- **Tables** with Row Level Security (every table: deny by default).
- **Database functions** (security definer, run with care) for the few actions that must be atomic and checked on the server: create an invitation, accept an invitation, end a partnership, delete my account. They run inside the database, so a hacked app cannot skip a check.
- This is a small change from D-005, which named Edge Functions: database functions are simpler for these four actions, are tested the same way as the rules, and need no extra deployment. Edge Functions stay available for M5 (notifications).

### Tables

| Table | Holds | Who can see/change it |
|---|---|---|
| `profiles` | nickname for each account | the owner reads and edits their own; a partner can read the nickname of someone they are linked to |
| `devices` | the owner's registered phones and last seen | the owner only |
| `invitations` | hash of the code, expiry (48 h), used or revoked | the owner only; nobody else can read it; accepted only through the function |
| `invitation_attempts` | who tried a code and when | nobody directly; the accept function uses it to limit guessing |
| `partnerships` | supported person, partner, created, ended, and the sharing choices | both people can read their own partnership; the supported person edits the choices; either can end it; a partner can never read anything about a person they are not (or no longer) linked to |

### Invitations (ACC-01)
1. The person taps **Invite a partner**. The server makes a random 128-bit code, **stores only its hash**, and shows the code once.
2. The app offers Android's share sheet (WhatsApp, SMS...) with a short message and the code. No website or domain is needed: the partner installs Identity, makes an account, and **pastes the code**.
3. The code works **once**, for **48 hours**, and is limited to a few wrong tries per account per hour.
4. On accepting, the partnership starts, and the code is dead. Either person can end it at any time, in the app; the other is told neutrally ("[nickname] ended the connection").

### Sharing choices (stored in M4, used in M5)
Per partnership, set by the supported person, changeable any time:
1. **Protection status and interruptions** (on, off, interrupted, cause unknown). Suggested default: on.
2. **Daily count of blocked requests** (a number, never a name). Suggested default: off.
3. **Changes to protection settings** (for example a switch turned off). Suggested default: off.
Domain names are **never** shared, with no option to (spec §7).

### Account deletion (PRIV-01)
One button in Settings: deletes the account and, by cascade, the person's devices, invitations, partnerships (as supported person and as partner) and nickname. The partner is told the connection ended. What the provider keeps in backups is written down in the retention note (M4.4), and the account email is removed from sign-in. Google Play also requires a way to request deletion from outside the app; that web page is an M8 item.

### The Android side
- **Library:** `supabase-kt` (as the architecture chose), with only the **publishable key**, which is designed to be public. Alternative: plain HTTPS calls with our existing JSON library, fewer dependencies and more code; I'd keep `supabase-kt` unless you prefer fewer third-party libraries.
- **Session storage:** the sign-in token is stored encrypted with a key held in Android's hardware-backed keystore, in the app's private storage (already excluded from backup).
- **The key and project address** are public by design, so they live in the code. The **service-role key, database password and access tokens are never in the repo** (it is public now) and never in the app.
- **Screens:** the *Partner* tab becomes: signed out (create account / sign in), then "Your partners" (invite, list, end), "People you support" (as a partner), and "Enter an invitation code". *Settings* gets an Account section (email, devices, sign out, delete account).

## 4. Slices (each small, tested, and merged before the next)

| Slice | What | Needs your phone? | Acceptance (summary) |
|---|---|---|---|
| **4.0** | **You** create the Supabase project (steps below) and send me the project address and publishable key | no | project exists; key is the publishable one, not the secret one |
| **4.1 ✅** | _Done 2026-10-29: 91 of 91 checks pass._ Database: tables, rules, the five functions, as SQL files in the repo; a **test script** that signs up two accounts and checks every rule over the real Supabase interface, including a revoked partner | no | a stranger sees nothing; a partner sees only their partnership; a revoked partner sees nothing; an expired, reused or guessed code fails; deletion removes everything |
| **4.2** | App: sign up, sign in, sign out, secure session storage, register the phone, optional-account behaviour (offline and signed-out still work) | yes | account works; protection unaffected by signing in or out or by being offline |
| **4.3** | App: invite (code and share sheet), accept, list, end from either side, sharing choices | yes (plus the test script as the second person) | full invitation round trip; expiry; one use; revocation shows on both sides |
| **4.4** | Account deletion in the app; retention note; data inventory and a draft privacy notice | yes | deleting the account removes all rows and signs out; retention documented |
| **4.5** | Hardening and M4 acceptance: the whole test script on the final build, error messages, offline behaviour, docs | yes | the roadmap's four M4 criteria all ✅ |

**4.0, the steps for you (about 10 minutes, free):** create an account at supabase.com; create a project (name `identity`; region: the closest available, for South Africa the Cape Town region if offered; a strong database password saved in your password manager and **not** sent to me); then from Project Settings, API, copy the **Project URL** and the **publishable (anon) key** to me. Never send the `service_role` or "secret" key, or the database password.

## 5. Risks and things to verify (nothing here is a blocker)

| Risk | What to do |
|---|---|
| **Free projects can be paused after a week without use** (to verify with Supabase's current terms). A paused project means accounts and (from M5) partner alerts stop working; filtering is unaffected | Fine for development and a small test. Before real beta users, plan for the paid plan (about R450 a month at today's rates, to verify) or a keep-alive |
| **Sign-up emails:** Supabase's built-in email sender is for testing and very limited (to verify), so confirmation and password-reset emails will not scale | Sign-in decision below; custom email sending (needs a domain, about R150 to R250 a year, D-004) before beta |
| **Sensitive data in law:** an account that links a person to a pornography-accountability app can reveal sexual behaviour and religious belief, which South Africa's POPIA treats as *special personal information* needing explicit consent and care | Explicit consent screens in M4.2 and M4.3; privacy notice in M4.4; **a legal review before beta (M8 gate)**. This is not legal advice |
| **Guessing invitation codes** | 128-bit random codes, hashed at rest, 48 hours, single use, rate-limited per account |
| **A partner keeps access after being removed** | Access is checked by the database on every request, not only by the app; the test script proves a revoked partner sees nothing |
| **Public repo** | Only publishable values in the code; a secrets scan before each merge; `.gitignore` already blocks keys |
| **Backups** | Free projects may have none or little (to verify); the retention note will say what is true |

## 6. Decisions (answered 2026-10-29, recorded as D-037)

The product owner chose the recommended option for each of the four questions asked; the Supabase free tier (D-005) was not re-asked and stands as planned.

| # | Decision | Answer |
|---|---|---|
| 2 | Sign-in method | **Email and password** |
| 3 | Invitations | **A one-time code shared with the share sheet and pasted in the partner's app** (no website needed) |
| 4 | Limits | **Up to 3 partners per person; either side can end a connection at any time** |
| 5, 6 | Sharing and what a partner sees | **Nickname only (never the email); status and interruptions on, daily blocked count off, changes to protection settings off by default; changeable any time; domain names never shared** |

The original questions, for reference:

1. **Backend:** go ahead with Supabase on the free tier, as planned in D-005 (database functions instead of Edge Functions for M4)?
2. **Sign-in method:** (a) **email and password** (recommended: works on any phone, partner can use any email); (b) Google sign-in (no emails to send, but needs Google Cloud setup and ties accounts to Google); (c) no email at all, with a private account per phone (best for privacy, but a lost phone loses the link, and a partner on another phone can't be recovered).
3. **Invitations:** a code that is shared with the share sheet and pasted in the partner's app (recommended: needs no website), rather than a tap-to-open link (needs a domain)?
4. **Limits:** up to **3 partners per person**, and either side can end a connection at any time (the other is told in plain words)?
5. **Sharing defaults:** the three choices in §3 with the suggested defaults?
6. **What a partner sees of a person:** only the **nickname** the person chooses (not their email)?
