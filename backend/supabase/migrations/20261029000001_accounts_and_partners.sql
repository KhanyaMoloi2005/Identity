-- Identity M4.1: accounts, devices, invitations and partnerships (D-037).
--
-- Run this whole file once, in Supabase: SQL Editor -> New query -> paste -> Run.
-- It is safe to read first: it only creates things in the `public` schema, and every table has
-- Row Level Security switched on, with no access except what is granted below.
--
-- The people: the "supported person" is the one whose phone is protected. They invite an
-- "accountability partner". A partner can see only what the supported person has chosen to share,
-- and only while the partnership is active. Nothing here stores websites, domain names or
-- Scripture reflections (docs/plans/m4-accounts-and-partners.md, section 2).

create extension if not exists pgcrypto with schema extensions;

-- ---------------------------------------------------------------------------------------------
-- Tables
-- ---------------------------------------------------------------------------------------------

-- The nickname a partner sees instead of an email address.
create table public.profiles (
    id          uuid primary key references auth.users (id) on delete cascade,
    nickname    text not null check (char_length(btrim(nickname)) between 1 and 30),
    created_at  timestamptz not null default now()
);

-- Registered phones. Model and versions only; no identifiers that follow the person.
create table public.devices (
    id              uuid primary key default gen_random_uuid(),
    owner_id        uuid not null references auth.users (id) on delete cascade,
    model           text check (char_length(model) <= 60),
    android_version text check (char_length(android_version) <= 20),
    app_version     text check (char_length(app_version) <= 20),
    registered_at   timestamptz not null default now(),
    last_seen_at    timestamptz not null default now()
);
create index devices_owner_idx on public.devices (owner_id);

-- Invitations. Only a hash of the code is stored: the code itself is shown once, to its creator.
create table public.invitations (
    id           uuid primary key default gen_random_uuid(),
    owner_id     uuid not null references auth.users (id) on delete cascade,
    code_hash    bytea not null unique,
    created_at   timestamptz not null default now(),
    expires_at   timestamptz not null,
    used_at      timestamptz,
    accepted_by  uuid references auth.users (id) on delete set null,
    revoked_at   timestamptz
);
create index invitations_owner_idx on public.invitations (owner_id);

-- Every try at entering a code, so guessing can be limited. Nobody reads this except the functions.
create table public.invitation_attempts (
    id            bigint generated always as identity primary key,
    user_id       uuid not null references auth.users (id) on delete cascade,
    attempted_at  timestamptz not null default now()
);
create index invitation_attempts_user_idx on public.invitation_attempts (user_id, attempted_at);

-- Who supports whom, and what the supported person has chosen to share (used from M5).
create table public.partnerships (
    id                     uuid primary key default gen_random_uuid(),
    supported_id           uuid not null references auth.users (id) on delete cascade,
    partner_id             uuid not null references auth.users (id) on delete cascade,
    created_at             timestamptz not null default now(),
    ended_at               timestamptz,
    ended_by               uuid references auth.users (id) on delete set null,
    share_status           boolean not null default true,   -- protection on / off / interrupted
    share_blocked_count    boolean not null default false,  -- a daily number, never a name
    share_setting_changes  boolean not null default false,  -- e.g. a protection switch turned off
    constraint partnership_two_people check (supported_id <> partner_id)
);
-- A pair can be connected only once at a time (it can be connected again after one ends it).
create unique index partnerships_active_pair_idx
    on public.partnerships (supported_id, partner_id) where ended_at is null;
create index partnerships_partner_idx on public.partnerships (partner_id);

-- ---------------------------------------------------------------------------------------------
-- Limits that must hold even if the app is modified
-- ---------------------------------------------------------------------------------------------

-- At most 10 registered phones per account.
create function public.limit_devices() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    if (select count(*) from public.devices where owner_id = new.owner_id) >= 10 then
        raise exception 'device_limit' using errcode = 'P0001';
    end if;
    return new;
end;
$$;
create trigger limit_devices_trigger before insert on public.devices
    for each row execute function public.limit_devices();

-- ---------------------------------------------------------------------------------------------
-- Row Level Security: everything denied unless a policy says otherwise
-- ---------------------------------------------------------------------------------------------

alter table public.profiles            enable row level security;
alter table public.devices             enable row level security;
alter table public.invitations         enable row level security;
alter table public.invitation_attempts enable row level security;
alter table public.partnerships        enable row level security;

-- Start from nothing, then grant only what is needed. `anon` (not signed in) gets nothing at all.
revoke all on public.profiles, public.devices, public.invitations,
              public.invitation_attempts, public.partnerships from anon, authenticated;

-- profiles: your own, and the nickname of someone you are actively connected to (either direction).
grant select on public.profiles to authenticated;
grant insert (id, nickname) on public.profiles to authenticated;
grant update (nickname) on public.profiles to authenticated;

create policy profiles_read on public.profiles for select to authenticated using (
    id = (select auth.uid())
    or exists (
        select 1 from public.partnerships p
        where p.ended_at is null
          and ((p.supported_id = profiles.id and p.partner_id = (select auth.uid()))
            or (p.partner_id = profiles.id and p.supported_id = (select auth.uid())))
    )
);
create policy profiles_insert on public.profiles for insert to authenticated
    with check (id = (select auth.uid()));
create policy profiles_update on public.profiles for update to authenticated
    using (id = (select auth.uid())) with check (id = (select auth.uid()));

-- devices: yours only. (A partner will see "last seen" through a function in M5, if shared.)
grant select, delete on public.devices to authenticated;
grant insert (owner_id, model, android_version, app_version) on public.devices to authenticated;
grant update (model, android_version, app_version, last_seen_at) on public.devices to authenticated;

create policy devices_read on public.devices for select to authenticated
    using (owner_id = (select auth.uid()));
create policy devices_insert on public.devices for insert to authenticated
    with check (owner_id = (select auth.uid()));
create policy devices_update on public.devices for update to authenticated
    using (owner_id = (select auth.uid())) with check (owner_id = (select auth.uid()));
create policy devices_delete on public.devices for delete to authenticated
    using (owner_id = (select auth.uid()));

-- invitations: you can see your own, but never the hash column, and you cannot change them directly
-- (creating, revoking and accepting go through the functions below).
grant select (id, owner_id, created_at, expires_at, used_at, revoked_at) on public.invitations to authenticated;
create policy invitations_read on public.invitations for select to authenticated
    using (owner_id = (select auth.uid()));

-- invitation_attempts: no grants and no policies: only the functions touch it.

-- partnerships: both people can read their own; only the supported person changes the sharing
-- choices, and only while it is active. Nobody changes who is who, or deletes a row, directly.
grant select on public.partnerships to authenticated;
grant update (share_status, share_blocked_count, share_setting_changes) on public.partnerships to authenticated;

create policy partnerships_read on public.partnerships for select to authenticated
    using ((select auth.uid()) in (supported_id, partner_id));
create policy partnerships_update_sharing on public.partnerships for update to authenticated
    using (supported_id = (select auth.uid()) and ended_at is null)
    with check (supported_id = (select auth.uid()) and ended_at is null);

-- ---------------------------------------------------------------------------------------------
-- Functions: the only way to create, revoke or accept invitations, end a partnership, or delete
-- an account. They check who is calling on the server, so a changed app cannot skip a check.
-- Expected failures come back as { "ok": false, "error": "..." } rather than as database errors,
-- so that a failed code attempt is still counted (an error would undo the count).
-- ---------------------------------------------------------------------------------------------

-- Make a one-time invitation code. Returns the code once; only its hash is kept.
create function public.create_invitation() returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
    me uuid := auth.uid();
    raw_code text;
    new_id uuid;
    new_expiry timestamptz;
begin
    if me is null then return jsonb_build_object('ok', false, 'error', 'not_signed_in'); end if;
    if (select count(*) from public.partnerships where supported_id = me and ended_at is null) >= 3 then
        return jsonb_build_object('ok', false, 'error', 'partner_limit');
    end if;
    if (select count(*) from public.invitations
        where owner_id = me and used_at is null and revoked_at is null and expires_at > now()) >= 5 then
        return jsonb_build_object('ok', false, 'error', 'too_many_pending');
    end if;

    raw_code := upper(encode(extensions.gen_random_bytes(16), 'hex'));      -- 128 random bits
    insert into public.invitations (owner_id, code_hash, expires_at)
    values (me, extensions.digest(raw_code, 'sha256'), now() + interval '48 hours')
    returning id, expires_at into new_id, new_expiry;

    return jsonb_build_object(
        'ok', true,
        'invitation_id', new_id,
        'expires_at', new_expiry,
        'code', regexp_replace(raw_code, '(.{4})(?!$)', '\1-', 'g')          -- ABCD-EF01-...
    );
end;
$$;

-- Use a code someone shared: connects you, as their partner. The code works once.
create function public.accept_invitation(p_code text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
    me uuid := auth.uid();
    normalized text;
    inv public.invitations;
    new_partnership uuid;
begin
    if me is null then return jsonb_build_object('ok', false, 'error', 'not_signed_in'); end if;

    delete from public.invitation_attempts where attempted_at < now() - interval '1 day';
    if (select count(*) from public.invitation_attempts
        where user_id = me and attempted_at > now() - interval '1 hour') >= 10 then
        return jsonb_build_object('ok', false, 'error', 'rate_limited');
    end if;
    insert into public.invitation_attempts (user_id) values (me);

    normalized := upper(regexp_replace(coalesce(p_code, ''), '[^0-9A-Za-z]', '', 'g'));
    if length(normalized) <> 32 then return jsonb_build_object('ok', false, 'error', 'invalid'); end if;

    select * into inv from public.invitations
    where code_hash = extensions.digest(normalized, 'sha256') for update;

    if not found or inv.revoked_at is not null then return jsonb_build_object('ok', false, 'error', 'invalid'); end if;
    if inv.used_at is not null then return jsonb_build_object('ok', false, 'error', 'used'); end if;
    if inv.expires_at <= now() then return jsonb_build_object('ok', false, 'error', 'expired'); end if;
    if inv.owner_id = me then return jsonb_build_object('ok', false, 'error', 'self'); end if;
    if exists (select 1 from public.partnerships
               where supported_id = inv.owner_id and partner_id = me and ended_at is null) then
        return jsonb_build_object('ok', false, 'error', 'already_partners');
    end if;
    if (select count(*) from public.partnerships where supported_id = inv.owner_id and ended_at is null) >= 3 then
        return jsonb_build_object('ok', false, 'error', 'partner_limit');
    end if;

    insert into public.partnerships (supported_id, partner_id)
    values (inv.owner_id, me) returning id into new_partnership;
    update public.invitations set used_at = now(), accepted_by = me where id = inv.id;

    return jsonb_build_object('ok', true, 'partnership_id', new_partnership);
end;
$$;

-- Cancel an invitation that has not been used yet.
create function public.revoke_invitation(p_invitation_id uuid) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
    me uuid := auth.uid();
    done uuid;
begin
    if me is null then return jsonb_build_object('ok', false, 'error', 'not_signed_in'); end if;
    update public.invitations set revoked_at = now()
    where id = p_invitation_id and owner_id = me and used_at is null and revoked_at is null
    returning id into done;
    if done is null then return jsonb_build_object('ok', false, 'error', 'not_found'); end if;
    return jsonb_build_object('ok', true);
end;
$$;

-- End a connection. Either person can do it, at any time.
create function public.end_partnership(p_partnership_id uuid) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
    me uuid := auth.uid();
    done uuid;
begin
    if me is null then return jsonb_build_object('ok', false, 'error', 'not_signed_in'); end if;
    update public.partnerships set ended_at = now(), ended_by = me
    where id = p_partnership_id and ended_at is null and me in (supported_id, partner_id)
    returning id into done;
    if done is null then return jsonb_build_object('ok', false, 'error', 'not_found'); end if;
    return jsonb_build_object('ok', true);
end;
$$;

-- Delete the caller's whole account. Everything that belongs to it goes with it (the foreign keys
-- above cascade): profile, phones, invitations, and every partnership, on either side.
create function public.delete_my_account() returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
    me uuid := auth.uid();
begin
    if me is null then return jsonb_build_object('ok', false, 'error', 'not_signed_in'); end if;
    delete from auth.users where id = me;
    return jsonb_build_object('ok', true);
end;
$$;

-- Only signed-in people may call these (never anonymous visitors).
revoke all on function public.create_invitation()          from public, anon, authenticated;
revoke all on function public.accept_invitation(text)      from public, anon, authenticated;
revoke all on function public.revoke_invitation(uuid)      from public, anon, authenticated;
revoke all on function public.end_partnership(uuid)        from public, anon, authenticated;
revoke all on function public.delete_my_account()          from public, anon, authenticated;
revoke all on function public.limit_devices()              from public, anon, authenticated;

grant execute on function public.create_invitation()       to authenticated;
grant execute on function public.accept_invitation(text)   to authenticated;
grant execute on function public.revoke_invitation(uuid)  to authenticated;
grant execute on function public.end_partnership(uuid)     to authenticated;
grant execute on function public.delete_my_account()       to authenticated;
