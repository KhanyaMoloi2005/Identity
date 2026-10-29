# Identity backend (Supabase)

The accounts-and-partners database for M4 ([plan](../docs/plans/m4-accounts-and-partners.md), decisions D-037). There is **no server of our own**: the database enforces every rule, and the app talks to it with the project's public key.

| Path | What |
|---|---|
| `supabase/migrations/` | The database, as SQL. Run in order. Each file is run **once**. |
| `tests/rls_test.py` | Proves the rules, using the real project, two or more test accounts, a stranger and a revoked partner. |
| `project.properties` | The project address and its **publishable** (anon) key. Both are public by design. |

**Never put in this repository** (it is public): the `service_role` / secret key, the database password, an access token, or any user's data.

## Set up (once)

1. In Supabase, **Authentication, Sign In / Providers, Email**: switch **"Confirm email" OFF** (the test script cannot read email). Switch it back **ON** before real users, with real email sending (decision D-037).
2. **SQL Editor, New query**, paste the whole of `supabase/migrations/20261029000001_accounts_and_partners.sql`, **Run**. It should end with "Success. No rows returned".
3. Run the tests from the project root: `python backend/tests/rls_test.py`. Then follow the printed expiry step and run `python backend/tests/rls_test.py --expiry-check`.

## If something goes wrong

- A test account left behind (a crashed run) is harmless: the addresses start with `identity-test-`. Remove them in **Authentication, Users**.
- To start the database again from nothing (only while there is no real data): SQL Editor, run
  `drop table if exists public.invitation_attempts, public.invitations, public.partnerships, public.devices, public.profiles cascade;` and then the migration again.
