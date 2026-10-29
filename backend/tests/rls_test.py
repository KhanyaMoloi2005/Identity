"""Tests the Identity database rules against a real Supabase project (M4.1, D-037).

    python backend/tests/rls_test.py                 # the main run (about 1 minute)
    python backend/tests/rls_test.py --expiry-check  # second step, after pasting the SQL it prints

It talks to the project exactly as the app will: with the public (anon) key and a signed-in
person's token. No secret key is used or needed. It creates throw-away test accounts with
addresses like identity-test-<number>-<name>@example.com, and deletes them again at the end (which
also tests account deletion). Needs "Confirm email" switched OFF in Supabase (Authentication,
Sign In / Providers, Email), because it cannot read email; switch it back ON before real users.
"""

import json
import pathlib
import secrets
import sys
import urllib.error
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
STATE_FILE = ROOT / "build" / "expiry-check.json"
PASSWORD = "Test-" + secrets.token_hex(8)  # throw-away accounts only
# The invitation columns a person may read. The hash column is deliberately not readable, so "select everything"
# is refused: the app must always name these columns.
INV_COLS = "id,owner_id,created_at,expires_at,used_at,revoked_at"


def load_config():
    values = {}
    for line in (ROOT / "backend" / "project.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values["url"].rstrip("/"), values["anon_key"]


URL, ANON = load_config()


def call(method, path, body=None, token=None, prefer=None):
    """Returns (http_status, parsed_json_or_text)."""
    headers = {"apikey": ANON, "Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if prefer:
        headers["Prefer"] = prefer
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(URL + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            text = response.read().decode()
            status = response.status
    except urllib.error.HTTPError as error:
        text = error.read().decode()
        status = error.code
    try:
        return status, json.loads(text) if text else None
    except ValueError:
        return status, text


results = []


def check(name, condition, detail=""):
    results.append((name, bool(condition)))
    print(("  PASS  " if condition else "  FAIL  ") + name + ("" if condition else f"   <- {detail}"))
    return bool(condition)


class Person:
    def __init__(self, label, run):
        self.label = label
        self.email = f"identity-test-{run}-{label}@example.com"
        status, body = call("POST", "/auth/v1/signup", {"email": self.email, "password": PASSWORD})
        if status != 200 or not isinstance(body, dict) or "access_token" not in body:
            raise SystemExit(
                f"Could not create test account ({status}): {str(body)[:300]}\n"
                "If this says the email needs confirming, switch OFF 'Confirm email' in Supabase "
                "(Authentication, Sign In / Providers, Email) and run again."
            )
        self.token = body["access_token"]
        self.id = body["user"]["id"]

    def sign_in_works(self):
        status, _ = call("POST", "/auth/v1/token?grant_type=password", {"email": self.email, "password": PASSWORD})
        return status == 200

    def get(self, path):
        return call("GET", "/rest/v1/" + path, token=self.token)

    def post(self, path, body, prefer="return=minimal"):
        return call("POST", "/rest/v1/" + path, body, token=self.token, prefer=prefer)

    def patch(self, path, body):
        return call("PATCH", "/rest/v1/" + path, body, token=self.token, prefer="return=representation")

    def delete(self, path):
        return call("DELETE", "/rest/v1/" + path, token=self.token, prefer="return=representation")

    def rpc(self, name, body=None):
        return call("POST", "/rest/v1/rpc/" + name, body or {}, token=self.token)[1]

    def nickname_seen(self, other_id):
        status, rows = self.get(f"profiles?id=eq.{other_id}&select=nickname")
        return rows if status == 200 else None


def denied(status):
    return status in (401, 403)


def main_run():
    run = secrets.token_hex(3)
    print(f"Test run {run} against {URL}\n")
    status, settings = call("GET", "/auth/v1/settings")
    if not (isinstance(settings, dict) and settings.get("mailer_autoconfirm")):
        raise SystemExit(
            "'Confirm email' is still ON in Supabase, so test accounts cannot sign in.\n"
            "Switch it OFF: Authentication, Sign In / Providers, Email, 'Confirm email'. Then run again."
        )

    a = Person("a-supported", run)
    b1, b2, b3, b4 = (Person(f"b{i}-partner", run) for i in range(1, 5))
    c = Person("c-stranger", run)
    e = Person("e-guesser", run)
    everyone = [a, b1, b2, b3, b4, c, e]

    print("1. Not signed in: nothing is reachable")
    for table in ("profiles", "devices", "invitations", "invitation_attempts", "partnerships"):
        status, _ = call("GET", f"/rest/v1/{table}")
        check(f"anonymous cannot read {table}", denied(status), status)
    for fn in ("create_invitation", "accept_invitation", "revoke_invitation", "end_partnership", "delete_my_account"):
        status, _ = call("POST", f"/rest/v1/rpc/{fn}", {"p_code": "x", "p_invitation_id": a.id, "p_partnership_id": a.id})
        check(f"anonymous cannot call {fn}", denied(status) or status == 404, status)

    print("\n2. Profiles")
    check("a creates own profile", a.post("profiles", {"id": a.id, "nickname": "Alex"})[0] == 201)
    for person, nick in ((b1, "Bea"), (b2, "Ben"), (b3, "Bo"), (b4, "Bay"), (c, "Cy"), (e, "Eve")):
        person.post("profiles", {"id": person.id, "nickname": nick})
    check("c cannot create a profile for someone else", denied(c.post("profiles", {"id": a.id, "nickname": "Fake"})[0]))
    check("nickname cannot be empty", c.post("profiles", {"id": c.id, "nickname": "  "})[0] >= 400)
    check("a stranger cannot read a's profile", c.nickname_seen(a.id) == [])
    check("a can read own profile", a.nickname_seen(a.id) == [{"nickname": "Alex"}])
    status, rows = c.patch(f"profiles?id=eq.{a.id}", {"nickname": "Hacked"})
    check("c cannot rename a", status in (200, 403) and not rows, (status, rows))
    check("a's nickname is unchanged", a.nickname_seen(a.id) == [{"nickname": "Alex"}])

    print("\n3. Phones (devices)")
    check("a registers a phone", a.post("devices", {"owner_id": a.id, "model": "SM-A346E", "android_version": "16", "app_version": "0.0.2"})[0] == 201)
    check("c cannot register a phone for a", denied(c.post("devices", {"owner_id": a.id, "model": "x"})[0]))
    check("a stranger cannot see a's phones", c.get("devices")[1] == [])
    check("a sees own phone", len(a.get("devices")[1]) == 1)
    status, rows = a.post("devices", {"owner_id": a.id, "model": "x", "id": c.id})
    check("a cannot choose a phone's id", status >= 400)
    for i in range(9):
        a.post("devices", {"owner_id": a.id, "model": f"extra{i}"})
    check("a is limited to 10 phones", a.post("devices", {"owner_id": a.id, "model": "eleventh"})[0] >= 400)
    check("a still has exactly 10", len(a.get("devices")[1]) == 10)
    for row in a.get("devices")[1][1:]:
        a.delete(f"devices?id=eq.{row['id']}")
    check("a can remove phones", len(a.get("devices")[1]) == 1)

    print("\n4. Invitations")
    inv = [a.rpc("create_invitation") for _ in range(5)]
    check("a makes 5 invitations", all(i.get("ok") for i in inv), inv)
    check("a 6th pending invitation is refused", a.rpc("create_invitation").get("error") == "too_many_pending")
    code = [i["code"] for i in inv]
    ids = [i["invitation_id"] for i in inv]
    check("code looks like 8 groups of 4", len(code[0].split("-")) == 8 and len(code[0].replace("-", "")) == 32, code[0])
    check("a sees own invitations", len(a.get(f"invitations?select={INV_COLS}")[1]) == 5)
    check("asking for every column of an invitation is refused (the hash is hidden)", denied(a.get("invitations")[0]))
    check("a stranger sees no invitations", c.get(f"invitations?select={INV_COLS}")[1] == [])
    check("the code hash cannot be read", denied(a.get("invitations?select=code_hash")[0]))
    check("a cannot create an invitation directly", denied(a.post("invitations", {"owner_id": a.id, "code_hash": "\\x00", "expires_at": "2099-01-01"})[0]))
    status, rows = a.patch(f"invitations?id=eq.{ids[0]}", {"expires_at": "2099-01-01"})
    check("a cannot edit an invitation directly", denied(status), (status, rows))
    status, rows = a.delete(f"invitations?id=eq.{ids[0]}")
    check("a cannot delete an invitation directly", denied(status), (status, rows))
    check("a stranger cannot read a's invitation by id", c.get(f"invitations?id=eq.{ids[0]}&select={INV_COLS}")[1] == [])

    print("\n5. Accepting a code")
    wrong = [e.rpc("accept_invitation", {"p_code": f"WRONG-{i}"}) for i in range(10)]
    check("wrong codes are 'invalid'", all(w.get("error") == "invalid" for w in wrong), wrong[:2])
    check("after 10 tries e is rate limited", e.rpc("accept_invitation", {"p_code": code[0]}).get("error") == "rate_limited")
    check("a wrong code from someone else is just invalid", c.rpc("accept_invitation", {"p_code": "0" * 32}).get("error") == "invalid")
    check("a cannot accept own invitation", a.rpc("accept_invitation", {"p_code": code[1]}).get("error") == "self")
    ok1 = b1.rpc("accept_invitation", {"p_code": code[0]})
    check("b1 accepts with the code", ok1.get("ok") is True, ok1)
    check("the same code cannot be used twice", c.rpc("accept_invitation", {"p_code": code[0]}).get("error") == "used")
    check("b1 accepts again: still used", b1.rpc("accept_invitation", {"p_code": code[0]}).get("error") == "used")
    check("lower-case and spaces in a code are fine", b2.rpc("accept_invitation", {"p_code": " " + code[1].lower().replace("-", " ") + " "}).get("ok") is True)
    check("b3 accepts (3 partners now)", b3.rpc("accept_invitation", {"p_code": code[2]}).get("ok") is True)
    check("a 4th partner is refused", b4.rpc("accept_invitation", {"p_code": code[3]}).get("error") == "partner_limit")
    check("a cannot invite a 4th partner either", a.rpc("create_invitation").get("error") == "partner_limit")
    check("a revokes an unused invitation", a.rpc("revoke_invitation", {"p_invitation_id": ids[3]}).get("ok") is True)
    check("a revoked code does not work", b4.rpc("accept_invitation", {"p_code": code[3]}).get("error") == "invalid")
    check("a used invitation cannot be revoked", a.rpc("revoke_invitation", {"p_invitation_id": ids[0]}).get("error") == "not_found")
    check("c cannot revoke a's invitation", c.rpc("revoke_invitation", {"p_invitation_id": ids[4]}).get("error") == "not_found")

    print("\n6. What partners and strangers can see")
    status, mine = a.get("partnerships")
    check("a sees 3 partnerships", status == 200 and len(mine) == 3, mine)
    partnership_b1 = next(p["id"] for p in mine if p["partner_id"] == b1.id)
    check("b1 sees only own partnership", len(b1.get("partnerships")[1]) == 1)
    check("a stranger sees no partnerships", c.get("partnerships")[1] == [])
    check("b1 can read a's nickname", b1.nickname_seen(a.id) == [{"nickname": "Alex"}])
    check("a can read b1's nickname", a.nickname_seen(b1.id) == [{"nickname": "Bea"}])
    check("b2 cannot read b1's nickname (partners are not linked to each other)", b2.nickname_seen(b1.id) == [])
    check("a stranger cannot read a's nickname", c.nickname_seen(a.id) == [])
    check("a partner cannot see a's phones", b1.get("devices")[1] == [])
    check("a partner cannot see a's invitations", b1.get(f"invitations?select={INV_COLS}")[1] == [])
    check("a partner cannot read attempts", denied(b1.get("invitation_attempts")[0]))

    print("\n7. Sharing choices")
    p = next(x for x in mine if x["id"] == partnership_b1)
    check("defaults: status on, counts off, changes off", (p["share_status"], p["share_blocked_count"], p["share_setting_changes"]) == (True, False, False), p)
    status, rows = a.patch(f"partnerships?id=eq.{partnership_b1}", {"share_blocked_count": True})
    check("a changes a sharing choice", status == 200 and rows and rows[0]["share_blocked_count"] is True, (status, rows))
    status, rows = b1.patch(f"partnerships?id=eq.{partnership_b1}", {"share_blocked_count": False, "share_status": False})
    check("the partner cannot change sharing choices", not rows, (status, rows))
    check("the choice is still on", a.get(f"partnerships?id=eq.{partnership_b1}")[1][0]["share_blocked_count"] is True)
    status, rows = a.patch(f"partnerships?id=eq.{partnership_b1}", {"partner_id": c.id})
    check("a cannot swap in a different partner", denied(status), (status, rows))
    status, rows = a.patch(f"partnerships?id=eq.{partnership_b1}", {"ended_at": None, "supported_id": c.id})
    check("a cannot change who is supported", denied(status), (status, rows))
    check("nobody can create a partnership directly", denied(c.post("partnerships", {"supported_id": a.id, "partner_id": c.id})[0]))
    check("nobody can delete a partnership directly", denied(b1.delete(f"partnerships?id=eq.{partnership_b1}")[0]))

    print("\n8. Ending a connection (a revoked partner)")
    check("a stranger cannot end it", c.rpc("end_partnership", {"p_partnership_id": partnership_b1}).get("error") == "not_found")
    check("b1 ends it", b1.rpc("end_partnership", {"p_partnership_id": partnership_b1}).get("ok") is True)
    check("ending twice says not found", b1.rpc("end_partnership", {"p_partnership_id": partnership_b1}).get("error") == "not_found")
    ended = a.get(f"partnerships?id=eq.{partnership_b1}")[1][0]
    check("both sides see it ended, by b1", ended["ended_at"] is not None and ended["ended_by"] == b1.id, ended)
    check("the revoked partner can no longer read a's nickname", b1.nickname_seen(a.id) == [])
    check("a can no longer read the revoked partner's nickname", a.nickname_seen(b1.id) == [])
    check("the revoked partner sees nothing of a's phones", b1.get("devices")[1] == [])
    status, rows = a.patch(f"partnerships?id=eq.{partnership_b1}", {"share_status": False})
    check("sharing choices cannot be edited on an ended connection", not rows, (status, rows))
    check("b1 can no longer reuse the old code", b1.rpc("accept_invitation", {"p_code": code[0]}).get("error") == "used")
    fresh = a.rpc("create_invitation")
    again = a.rpc("create_invitation")
    check("a can invite again (2 active partners)", fresh.get("ok") is True and again.get("ok") is True, (fresh, again))
    check("b1 can be connected again with a new code", b1.rpc("accept_invitation", {"p_code": fresh["code"]}).get("ok") is True)
    check("an active pair cannot be connected twice", b1.rpc("accept_invitation", {"p_code": again.get("code", "")}).get("error") == "already_partners", again)

    print("\n9. Deleting accounts")
    check("c deletes own account", c.rpc("delete_my_account").get("ok") is True)
    check("c can no longer sign in", not c.sign_in_works())
    check("b3 deletes own account", b3.rpc("delete_my_account").get("ok") is True)
    alive = [p for p in a.get("partnerships")[1] if p["ended_at"] is None]
    check("a's connection to b3 is gone", all(p["partner_id"] != b3.id for p in a.get("partnerships")[1]), alive)
    check("a deletes own account", a.rpc("delete_my_account").get("ok") is True)
    check("a can no longer sign in", not a.sign_in_works())
    check("b2 no longer has any connection to a", b2.get("partnerships")[1] == [])
    check("b2 cannot read a's nickname", b2.nickname_seen(a.id) == [])
    for person in (b1, b2, b4, e):
        person.rpc("delete_my_account")
    check("all other test accounts removed", not any(p.sign_in_works() for p in (b1, b2, b3, b4, e)))

    start_expiry_check(run)
    return summarise()


def start_expiry_check(run):
    """Makes an invitation whose expiry has to be moved into the past by hand (a client cannot do that)."""
    print("\n10. Expiry (needs one manual step)")
    x, y = Person("x-expiry-inviter", run), Person("y-expiry-accepter", run)
    x.post("profiles", {"id": x.id, "nickname": "Xi"})
    y.post("profiles", {"id": y.id, "nickname": "Yu"})
    invitation = x.rpc("create_invitation")
    STATE_FILE.parent.mkdir(exist_ok=True)
    STATE_FILE.write_text(json.dumps({"run": run, "code": invitation["code"], "invitation_id": invitation["invitation_id"]}))
    print("  Created a pending invitation. To test expiry, paste this in Supabase, SQL Editor, then Run:\n")
    print(f"    update public.invitations set expires_at = now() - interval '1 minute' where id = '{invitation['invitation_id']}';\n")
    print("  then run:  python backend/tests/rls_test.py --expiry-check")
    # Leave both accounts in place for the second step.
    STATE_FILE.with_suffix(".people.json").write_text(json.dumps({
        "x": {"email": x.email, "token": x.token}, "y": {"email": y.email, "token": y.token},
    }))


def expiry_check():
    if not STATE_FILE.exists():
        raise SystemExit("Run the main test first (it prepares this step).")
    state = json.loads(STATE_FILE.read_text())
    # The test accounts' passwords differ per run, so use the tokens the main run saved.
    people = json.loads(STATE_FILE.with_suffix(".people.json").read_text())
    token_y, token_x = people["y"]["token"], people["x"]["token"]
    result = call("POST", "/rest/v1/rpc/accept_invitation", {"p_code": state["code"]}, token=token_y)[1]
    check("an expired invitation is refused as 'expired'", isinstance(result, dict) and result.get("error") == "expired", result)
    for token in (token_y, token_x):
        call("POST", "/rest/v1/rpc/delete_my_account", {}, token=token)
    STATE_FILE.unlink(missing_ok=True)
    STATE_FILE.with_suffix(".people.json").unlink(missing_ok=True)
    print("  Test accounts for this step removed.")
    return summarise()


def summarise():
    failed = [name for name, ok in results if not ok]
    print(f"\n{len(results) - len(failed)} passed, {len(failed)} failed")
    for name in failed:
        print("  FAILED:", name)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(expiry_check() if "--expiry-check" in sys.argv else main_run())
