"""Plays the second person when testing partners on one phone (M4.3), so no second phone is needed.

    python backend/tests/partner_helper.py new              # create the test partner account (once)
    python backend/tests/partner_helper.py invite           # make a code for the phone to enter
    python backend/tests/partner_helper.py accept CODE      # accept a code shared from the phone
    python backend/tests/partner_helper.py list             # connections and waiting invitations
    python backend/tests/partner_helper.py end [ID|all]     # end a connection (all active ones if no id)
    python backend/tests/partner_helper.py delete           # delete the test partner account

The account is a throw-away one (identity-partner-<number>@example.com, nickname "Test Partner"). Its
login is kept in build/partner-helper.json, which git ignores. Like rls_test.py it uses only the public
key, and needs "Confirm email" switched OFF in Supabase while developing.
"""

import json
import secrets
import sys

from rls_test import ROOT, call

STATE_FILE = ROOT / "build" / "partner-helper.json"
NICKNAME = "Test Partner"
COLS = "id,supported_id,partner_id,created_at,ended_at,ended_by,share_status,share_blocked_count,share_setting_changes"
INV_COLS = "id,owner_id,created_at,expires_at,used_at,revoked_at"


def load():
    if not STATE_FILE.exists():
        raise SystemExit("No test partner yet: run `partner_helper.py new` first.")
    state = json.loads(STATE_FILE.read_text(encoding="utf-8"))
    status, body = call("POST", "/auth/v1/token?grant_type=password", {"email": state["email"], "password": state["password"]})
    if status != 200:
        raise SystemExit(f"Could not sign in the test partner ({status}): {body}")
    return body["access_token"], body["user"]["id"]


def rpc(token, name, body=None):
    status, answer = call("POST", "/rest/v1/rpc/" + name, body or {}, token=token)
    print(f"{name} -> {status} {json.dumps(answer)}")
    return answer


def new():
    email = f"identity-partner-{secrets.token_hex(3)}@example.com"
    password = "Test-" + secrets.token_hex(8)
    status, body = call("POST", "/auth/v1/signup", {"email": email, "password": password})
    if status != 200 or "access_token" not in body:
        raise SystemExit(f"Could not create the account ({status}): {body}")
    call("POST", "/rest/v1/profiles", {"id": body["user"]["id"], "nickname": NICKNAME}, token=body["access_token"], prefer="return=minimal")
    STATE_FILE.parent.mkdir(exist_ok=True)
    STATE_FILE.write_text(json.dumps({"email": email, "password": password}), encoding="utf-8")
    print(f"Created {email} (nickname {NICKNAME!r}).")


def show(token, me):
    _, rows = call("GET", f"/rest/v1/partnerships?select={COLS}&order=created_at.desc", token=token)
    for row in rows:
        role = "supports" if row["partner_id"] == me else "is supported by"
        other = row["supported_id"] if row["partner_id"] == me else row["partner_id"]
        _, names = call("GET", f"/rest/v1/profiles?id=eq.{other}&select=nickname", token=token)
        name = names[0]["nickname"] if names else "(nickname hidden)"
        state = f"ended {row['ended_at']}" if row["ended_at"] else "active"
        print(f"  {row['id']}  Test Partner {role} {name}  [{state}]")
    _, invites = call("GET", f"/rest/v1/invitations?select={INV_COLS}&order=created_at.desc", token=token)
    for inv in invites:
        print(f"  invitation {inv['id']}  expires {inv['expires_at']}  used {inv['used_at']}  revoked {inv['revoked_at']}")
    if not rows and not invites:
        print("  (nothing yet)")
    return rows


def main(args):
    if not args:
        raise SystemExit(__doc__)
    if args[0] == "new":
        return new()
    token, me = load()
    command = args[0]
    if command == "invite":
        answer = rpc(token, "create_invitation")
        if answer.get("ok"):
            print(f"\nCode for the phone: {answer['code']}")
    elif command == "accept":
        rpc(token, "accept_invitation", {"p_code": " ".join(args[1:])})
    elif command == "list":
        show(token, me)
    elif command == "end":
        ids = args[1:2] if len(args) > 1 and args[1] != "all" else [r["id"] for r in show(token, me) if not r["ended_at"]]
        for pid in ids:
            rpc(token, "end_partnership", {"p_partnership_id": pid})
    elif command == "delete":
        if rpc(token, "delete_my_account").get("ok"):
            STATE_FILE.unlink()
    else:
        raise SystemExit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
