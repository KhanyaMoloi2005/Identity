# Identity: Decision Log

Every significant decision, why it was made, and its status.

- **Accepted:** confirmed by the product owner.
- **Provisional:** Claude's recommended default; work may proceed on it, but the product owner should confirm or change it.
- **Superseded:** replaced by a later decision (kept for history).

_Last updated: 2026-10-27._

| ID | Date | Decision | Status |
|---|---|---|---|
| D-001 | 2026-10-27 | Bible translation: **English Standard Version (ESV)** | Provisional |
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
| D-013 | 2026-10-27 | Builds run in the cloud (GitHub Actions); no Android tooling on the work laptop | Accepted |
| D-014 | 2026-10-27 | Application ID `app.identity` is a placeholder; final ID chosen before first Play upload | Provisional |
| D-015 | 2026-10-27 | Pinned toolchain: AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21, Compose BOM 2024.12.01, compile/target SDK 35 | Provisional |
| D-016 | 2026-10-27 | Bundle Inter + Literata as variable fonts (SIL OFL, licences shipped in the APK) | Accepted |
| D-017 | 2026-10-27 | Navigation Compose 2.8.5 with type-safe routes; kotlinx.serialization 1.7.3 | Accepted |
| D-018 | 2026-10-27 | Custom line icons for the tab bar instead of the large Material extended icon library | Accepted |

---

### D-001: Bible translation: ESV
**History:** Claude first proposed the World English Bible (public domain). On 2026-10-27 the product owner changed this to the **English Standard Version (ESV)**.
**Licensing condition (must resolve before any build is distributed):** the ESV is copyrighted by Crossway. Bundling whole chapters/books offline in an app (needed by Scripture First) requires permission from Crossway. Their free quotation allowance and the ESV API terms are limited and do not, as far as Claude knows, cover storing the full text in an app. Action: product owner to check Crossway's current permissions/API terms (crossway.org / api.esv.org) and request a licence if needed.
**Interim plan:** build the Scripture features against a translation-neutral data format using public-domain WEB text as **development placeholder** content, so ESV can be dropped in once licensed, with no code changes.
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

### D-013: Cloud builds
**Context:** The current laptop is a work machine; the product owner doesn't want Android Studio installed on it and has no USB cable for now.
**Decision:** GitHub Actions builds, lints and unit-tests the app on every push and attaches a debug APK to each run. Local Android Studio setup moves to the product owner's personal laptop later.
**Consequence:** until the repo is pushed to GitHub (or built on the personal laptop), code is **unverified**: it hasn't been compiled. Pushing to GitHub is the product owner's call (consider employer policy about personal repos on a work machine).
**No-cable options for running on a phone:** download the APK from the GitHub run directly on the phone and install it, or use Android 11+ Wireless debugging from the personal laptop.

### D-014: Application ID
**Context:** The application ID uniquely identifies the app on Google Play and can never change after the first upload.
**Decision:** `app.identity` as a placeholder. Pick a final ID before M8, ideally based on a domain the product owner controls (e.g. `za.co.<domain>.identity`).

### D-016: Fonts
**Decision:** download Inter and Literata from Google's official fonts repository (`github.com/google/fonts`, `ofl/` folder) and bundle them in `res/font/`. They're variable fonts (one file holds every weight), about 1.8 MB together. Italic files skipped for now. The SIL Open Font Licence requires the licence to travel with the font, so the OFL texts ship inside the app in `assets/licenses/`. A licences screen in Settings will display them later.
**Why bundle instead of downloading at runtime:** works offline, needs no internet permission, and sends no request to Google when the app opens.

### D-017: Navigation
**Decision:** Google's Navigation Compose library with **type-safe routes**: each screen is a Kotlin type, so a mistyped route fails the build instead of crashing on the phone. Type-safe routes require kotlinx.serialization (JetBrains, Apache 2.0), which 1.2 needs anyway to read verse data files.

### D-018: Tab icons
**Decision:** four hand-drawn line icons (shield, open book, two people, sliders) as vector drawables. Material's built-in core icons have no book icon, and the extended icon library is a very large dependency for four icons.

### D-015: Toolchain versions
**Decision:** a known-compatible, well-established set (see `android/gradle/libs.versions.toml`). Play currently requires a recent target SDK for new uploads, so targetSdk will be raised to the then-required level before beta (M8). Upgrades happen one at a time, each verified by a build.

### D-019: Scripture data storage and source (M1.2)
**Decision:** the read-only starter verses live in a JSON file (`assets/scripture/verses.json`) loaded into memory at startup. No Room database yet: Room arrives in M1.5, where saved verses and reflections actually need to be stored. Topics are fixed in code (`StarterTopics`) and verses refer to them by id in `topic_tags`.
**Source:** verse text is copied by script from the official World English Bible download at ebible.org (public domain), not typed from memory. See `content/scripture/SOURCES.md`. The WEB is a **placeholder** until the ESV licensing question (D-001) is settled.
**Safety:** bad data returns a failed `Result` instead of crashing; unknown topics, blank fields and duplicate ids are rejected.

### D-020: Build the filtering core before the Scripture screens (2026-10-28)
**Decision:** after M1.2, pause M1.3 to M1.6 and do **M2 (DNS filtering proof of concept)** next. Milestone numbers stay the same; only the order changes. M1.3 to M1.6 resume after M2.
**Why:** filtering is the core of the product and its riskiest part (VPN behaviour, battery, network changes, risks R-01, R-02, R-08). If it proves harder than expected, we want to know before building more around it. The Scripture screens are low-risk and independent, so waiting costs nothing. The product owner said the priority is back end and functionality; the UI can change later.
**Trade-off:** M2 needs a real-phone check (VPN permission, test domain, Wi-Fi/mobile-data switching), so the computer-only parts (DNS parsing, domain matching, tests) come first.

### D-021: dnsjava for DNS messages; `:filter-core` as a plain Kotlin module (2026-10-28)
**Decision:** use **dnsjava 3.6.5** (BSD-3-Clause, about 600 KB, runtime dependency only on `slf4j-api`) to read DNS queries and build replies. Only its message parsing is used, not its resolver. The rest of the app sees just `DnsPackets.parseQuery` and `DnsPackets.nxDomainResponse`, so the library can be swapped later.
**Why a library:** packets come from other apps on the phone (untrusted input) and DNS has known traps such as looping name-compression pointers. A mature parser has already met these. M3 SafeSearch also needs to build real answer records.
**Why not 3.7.0:** it was released on 2026-10-25, three days before this decision. We adopt new versions deliberately, one at a time (D-015).
**Module:** `android/filter-core`, plain Kotlin/JVM (Java 11 bytecode like the app), no Android code, so its tests run anywhere. The app does not use it yet (that starts in M2.2); the release-build shrinker (R8) rules for dnsjava will be checked then, on a phone.
**CI:** the workflow now also runs `:filter-core:test`. `testDebugUnitTest` only covers the `app` module and would have skipped these tests silently.

### D-022: How the VPN runs as a foreground service (2026-10-29)
**Decision:** `IdentityVpnService` is a foreground service of type **`systemExempted`** (permission `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`), with a silent, ongoing "Identity protection is on" notification. On Android 13+ the app asks for the notification permission once, just before the VPN request. The M2.2 tunnel has an address but **no routes and no DNS server**, so no traffic goes through it yet.
**Why `systemExempted`:** since Android 14 every foreground service must declare a type. Android's documentation lists "VPN apps" as allowed to use `systemExempted`. Other types (`specialUse`, `dataSync`) don't fit and bring their own Play review or time limits.
**Why no routes yet:** if we sent DNS to the tunnel before we can answer it (M2.3), every website would stop loading while protection is on. This slice proves only the permission, start/stop and notification.
**Not yet:** automatic restart after a crash or reboot, and Always-on VPN detection, are M3. Until then the on/off state lives in memory and shows "Off" if Android ends the app.
**Lint (found by the first CI run):** lint's `ForegroundServicePermission` check says `systemExempted` also needs `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`. Lint only reads the manifest; Android also accepts the user's VPN approval (given at run time) instead. We do **not** add an alarm permission we don't use. That one check is switched off on the VPN service only, with a comment (`tools:ignore`).
**To check on the phone:** that Android 14+ accepts the `systemExempted` type after the user taps OK. The docs don't spell out exactly how a VPN app qualifies. If Android refuses, the service stops cleanly instead of crashing, and we will revisit this decision.

### D-023: How the DNS tunnel works (M2.3, 2026-10-29)
**Decision:** the VPN gives Android one DNS server address, `10.111.222.1`, and routes **only that address** into the tunnel. Every app's DNS lookups therefore come to Identity, while all other traffic (web pages, video, messages) goes straight to the network as before. The packet code (IPv4 + UDP) lives in `:filter-core` (`IpPackets`, `DnsFilter`) so it is unit-tested on a computer; the app's `DnsTunnel` only moves bytes.
**Blocked names** get "no such domain" from the phone itself. **Allowed names** are forwarded to Cloudflare for Families (D-003, now in use), `1.1.1.3` first and `1.0.0.3` if that doesn't answer within 5 seconds, through a socket marked to skip our own VPN.
**Test blocklist:** only `example.com` (and its subdomains). Harmless, always online and normally loads, so a block is easy to see. The real list is M3.
**If the tunnel's reader stops** (an unexpected error), DNS stops working while the VPN is on: websites fail to load, rather than load unfiltered. This "fail closed" behaviour suits a protection app; M3 adds detection and recovery.
**Not covered yet:** IPv6-only DNS, DNS over TCP (rare; used for very large answers), Private DNS set to a specific provider, and browsers using their own secure DNS (risk R-02). These are M2.4 and M3 items.
**Battery:** the reader thread sleeps until a packet arrives (`poll`), with no timer waking it; M2.4 measures it.

### D-024: Uninstall protection becomes "uninstall friction + tamper alert", in M7 (2026-10-29)
**Request:** the product owner asked for a switch that prevents the user from ever uninstalling Identity.
**What Android allows:** no ordinary app can make itself impossible to uninstall. The strongest option is the **Device Admin** role: while it is active, Android refuses to uninstall the app until the user turns the role off in Settings. That is a deliberate extra step, not a lock. Safe Mode, a factory reset and uninstalling from a computer always remain possible. (Full control, "Device Owner", needs a factory-reset phone set up from a computer; not suitable for consumers.) The architecture's "cannot enforce" table already lists uninstalling, and the development rules forbid promising protections that can't be bypassed.
**Decision:** build an optional, off-by-default **uninstall friction** feature in **M7** (with the cooldowns it belongs with): Device Admin role; a pause message when the user tries to remove it; neutral partner alert when protection is removed (builds on the M5 missed check-in alert); clear disclosure of what it can and cannot do before it's switched on.
**Gate:** before any code, check the current Google Play policies on Device Admin and on apps that resist uninstalling (R-03). If Play doesn't allow it, keep only the tamper alert and record the result here.
**Why not M2:** M2 proves DNS filtering works. Friction only matters once there is real protection to keep on.

### D-025: Bundled blocklist is HaGeZi's NSFW list (2026-10-29)
**Decision:** bundle **HaGeZi NSFW** (`wildcard/nsfw-onlydomains.txt`, 92,127 domains on 2026-10-29; each entry also covers its subdomains). Chosen by the product owner on Claude's recommendation.
**Why:** it is updated several times a day and actively cleaned of wrongly-blocked normal sites, and it is small enough to keep in memory all day. The alternative, Block List Project's porn list (Unlicense), was archived on 2026-10-27 and will never be updated again, and it is larger.
**Licence conditions (GPL-3.0):** the list ships as its own readable file (`assets/blocklist/nsfw.txt`) with a header crediting the source, version and our changes; the full licence text ships beside it (`assets/licenses/hagezi-GPL-3.0.txt`); our build script is in the repo. The list is data loaded at run time, kept separate from the app's code. Before the Play release (M8): add a visible licences screen and re-check these conditions (not legal advice; Claude is not a lawyer).
**How it's built:** `python content/blocklists/build_blocklist.py` downloads, lower-cases, sorts and de-duplicates the list, and refuses to write if fewer than 50,000 entries arrive. The app re-checks every line when loading.
**Memory:** packed into one sorted byte array (`CompactDomainSet`): about 2.4 MB measured on a computer, versus roughly 9 MB as ordinary strings. Loaded on the tunnel's thread when protection turns on.
**Test builds** also block `example.com`, so blocking can be checked without visiting an adult site. Release builds don't.
**Updates:** the list is frozen at the bundled version until signed updates arrive (slice 3.6). Cloudflare for Families (D-003) still filters behind it.

### D-026: M3 next; the paused Scripture screens wait until functionality is done (2026-10-29)
**Decision:** after M2, continue with M3 (reliable protection) and the other functionality milestones. M1.3 to M1.6 (Scripture screens) stay paused until the functionality is done, and then everything is put together. Product owner's call, extending D-020.

### D-027: How SafeSearch is enforced (M3.2, 2026-10-29)
**Decision:** when a lookup asks for a search engine, Identity asks upstream about that engine's official safe-mode hostname instead and replies with an alias ("this name is that one") plus the safe hostname's addresses. The browser keeps talking to the engine normally; results come back filtered, and the setting can't be switched off on the phone. This is the method each engine documents for network administrators.
- **Google Search** → `forcesafesearch.google.com`, for `google.<domain>` and `www.google.<domain>` on all 187 of Google's country domains (bundled from google.com/supported_domains). Gmail, Maps and other Google services are untouched.
- **Bing** → `strict.bing.com`. **DuckDuckGo** → `safe.duckduckgo.com`.
- **YouTube** → `restrictmoderate.youtube.com` (**Moderate** restricted mode) for the website, the app's API hosts and youtube-nocookie. Claude's default: Strict hides a lot of ordinary videos, which tempts users to switch protection off. Can be changed to Strict in one line, or later offered as a setting.
**Order:** a blocked name stays blocked (NXDOMAIN) even if it's a search engine; otherwise SafeSearch applies; otherwise the query is forwarded.
**Safety:** an upstream answer that isn't about the safe hostname is rejected.
**Not covered:** other search engines (e.g. Brave Search, Yandex, Ecosia's own pages) and image searches inside other apps. Listed for the bypass checklist (3.7).

### D-028: Always block X/Reddit and image-search pictures, with no user switches (M3.3a, 2026-10-29)
**Why:** in the M3.2 phone test, explicit images got through on X/Twitter and Reddit, and "View image" on a SafeSearch result showed the original picture. Neither X nor Reddit has a safe mode that can be enforced on the network, and DNS can't see what's inside a picture.
**Decision (product owner):** both are **always on, with no switches for users**. They are added to the bundled blocklist itself (`Blocklist.load`). Settings explains what is blocked but offers nothing to change.
- **X and Reddit:** `x.com`, `twitter.com`, `twimg.com`, `t.co`, `reddit.com`, `redd.it`, `redditmedia.com`, `redditstatic.com`, `reddituploads.com` and their subdomains. Blocks the websites, the apps, and their posts embedded in other sites.
- **Image-search pictures:** Google `encrypted-tbn0..3.gstatic.com`, Bing `mm.bing.net` and `th.bing.com`, DuckDuckGo `external-content.duckduckgo.com`. Normal search, Gmail and Maps use other servers and keep working (tested). Thumbnails in ordinary search results (e.g. news pictures) also go blank.
**Limit, stated plainly:** blanking image-search pictures reduces exposure but isn't a wall: opening a result can still load the full picture from the website that hosts it, unless that site is on the blocklist.
**History:** switches (on by default) were built first, then removed before pushing at the product owner's request. If switches return, it will be in M7, behind the loosening cooldown.

### D-029: Users' own blocks: block-only, 10-minute undo; app blocking by block screen (M3.3b, 2026-10-29)
**Decision (product owner):** users can add their own **websites** and **words in site names** to block. There are **no user exceptions** (an exception would loosen protection, and users have no switches, D-028).
- **Website:** cleaned from whatever is typed or pasted (`https://www.Example.com/page` → `example.com`); the whole site and its subdomains are blocked.
- **Word in site names:** blocks every site whose name contains it (`casino` → `bestcasino.net`). At least 4 letters, because short words catch innocent sites (`sex` would block `essex.ac.uk`). Identity sees site names only, so "phrases" means words in names, not words on a page.
- **Undo:** a new rule can be removed for **10 minutes** (to fix typos), then it is permanent until the M7 cooldown exists. The window uses time since the phone started plus the boot count, not the clock, so changing the phone's clock can't reopen it; restarting the phone closes it.
- **Safety net:** custom rules never block Android's connectivity-check names, so a broad word can't make the phone report "no internet" forever. Up to 500 rules.
- **Storage:** one JSON file in app storage, written atomically (a crash mid-save can't leave half a file), excluded from backup.
- **Phrases on screen or in searches:** not possible without an Accessibility Service (D-012); stays a research item. SafeSearch covers searches on the major engines.
**Apps (later slice 3.3c):** a blocked app will be covered by a full-screen "This app is blocked" page with a verse, using Android's Usage access permission. Chosen over cutting the app's internet because (1) cut-off apps still show saved or cached content, and (2) cutting one app's traffic would require sending all phone traffic through Identity instead of DNS only, which is far more complex and worse for battery and reliability (R-08). Google Play policy check before building.

### D-030: TikTok daily time limit; Shorts and Reels are a separate, riskier step (M3.3c-1, 2026-10-29)
**Decision (product owner):** TikTok is **not** always blocked (unlike X and Reddit, D-028). The user picks a daily allowance from fixed choices (15 min, 30 min, 1 h, 2 h, 3 h, 5 h; no free typing). When it is used up, TikTok is covered by a full-screen "time is up" page with a verse and a Leave button, and its servers are blocked until midnight.
- **Counting:** Android's Usage access permission (`PACKAGE_USAGE_STATS`, switched on by the user in system Settings). Time is read from Android's own usage history each time; only the chosen allowance is saved. Counting resets at local midnight.
- **Changing it:** lowering takes effect at once; raising starts **tomorrow** (so a bad day can't be talked away). The 30-day lock the product owner asked for is M7.
- **The page:** a window drawn by the protection service using "Display over other apps" (`SYSTEM_ALERT_WINDOW`), so it appears even when Identity isn't open. Plain Android views, no separate screen. Without the permission, the DNS cut-off still applies.
- **Cut-off:** TikTok's domains (`BlockPresets.TIKTOK`) join the filter only while the allowance is used up; the filter is rebuilt when that changes.
- **Cost:** a timer on the main thread every 10 s (1.5 s when over the limit) that does not wake a sleeping phone, and none at all until an allowance is set.
**Limits, stated plainly:** works only while protection is on. Apps remember lookups and cached videos may play briefly after the cut-off. Overlapping screens inside TikTok are counted slightly low, never high. The **Google Play review of these two permissions is still open** (R-03).
**Shorts and Reels (3.3c-2, not built):** the product owner wants only YouTube Shorts and Instagram Reels blocked, not the whole apps. DNS and Usage access cannot tell those screens apart; only an **Accessibility Service** can. That would amend D-012 and carries the highest Play-policy risk in the project. Awaiting a decision before any code.

### D-031: Accessibility Service approved for YouTube Shorts and Instagram Reels only; amends D-012 (M3.3c-2, 2026-10-29)
**Decision (product owner):** when the user opens **YouTube Shorts** or **Instagram Reels**, Identity takes them out of it: back to the app's home screen, or closes the app, whichever works reliably. The user can open YouTube or Instagram again straight away and use everything else in them. Only those two screens are affected, not the whole apps.
- **Why an Accessibility Service:** Shorts and Reels are screens inside the apps. DNS cannot tell them from normal videos and posts, and Usage access only says which app is in front (D-030). Only an Accessibility Service can see which screen is showing.
- **Amends D-012** ("no accessibility service") for this one purpose. It stays off until the user switches it on in system Settings, after a plain explanation screen inside Identity.
- **Scope, to be enforced in the code and tests:** the service listens only to YouTube and Instagram (by package name), looks only for the Shorts and Reels screens, reads no text the user types or messages, and stores, logs and sends nothing. It takes no action in any other app.
- **Action:** a "back to the home screen" action on those two screens, falling back to a Home press if that does not leave the screen. The app is never uninstalled, disabled or force-stopped.
**Switches (product owner, 2026-10-29):** two switches in Settings, **Leave YouTube Shorts** and **Leave Instagram Reels**, each on by default and independent of the other. They are kept for now, even though a user who is tempted can switch them off in a few taps, which D-028 avoided for the always-on blocks. **To decide in M7:** whether to keep these switches (and whether the TikTok allowance needs the same treatment) or put them behind the loosening cooldown.
**Limits, stated plainly:** detection depends on how YouTube and Instagram label their screens, which they can change in any update, so it needs checking after app updates. The user can switch the service off in system Settings (M7 may add friction). The **Google Play review of this permission is the biggest open policy risk (R-03)** and matters before sharing a build; building and testing on the product owner's phone does not depend on it.

### D-032: Scripture First focus hour: idea recorded for M6 (2026-10-29)
**Idea (product owner):** at a wake-up time the user sets, Identity blocks every app except essential ones (Phone, WhatsApp, and similar) for a set time, such as an hour, during which the user reads the Bible and prays inside Identity. When the time is done, the other apps unlock.
- **Fits:** spec section 6, "Focused" and "Commitment mode" strictness levels. The alarm, reading plan and completion flow are M6.
- **How it could work:** reuse the "cover the app with a page" method from D-030 for every app that is not on an essential list the user chooses. No new kind of permission beyond the alarm ones.
- **Spec limits still apply:** an ordinary app cannot make a phone impossible to use. Emergency calls, system navigation and the essential apps are never blocked. The user can still leave through Android's own settings, and Identity says so during setup.
- **Open before building:** who picks the essential apps and whether they can be changed during the hour; what "finished" means (time, a chapter, or both); behaviour if the phone restarts mid-hour; Google Play policy for full-screen alarms and app blocking (R-03).
Not scheduled yet: M6.

### D-033: Always-on VPN yes; "Block connections without VPN" never (M3.4, 2026-10-29)
**Finding (product owner's phone):** with Always-on VPN set for Identity and Android's **Block connections without VPN** on, the phone had **no internet** although Identity showed "On". Turning only that option off restored it immediately.
**Why:** that option allows only traffic that goes through the VPN. Identity's VPN routes DNS lookups only (D-023), so all other traffic has no allowed path.
**Decision:** Home recommends **Always-on VPN** (it restarts protection earlier in the boot, and after Android ends it) and tells the user to leave **Block connections without VPN** off. Closing the "unprotected until unlock or app start" gap with that option would need a full-traffic VPN, which D-023 rules out for battery and reliability (R-08).
**Consequence:** the gap between switching the phone on and protection starting stays; it is as short as Android's Always-on allows. Listed in the risk register as part of R-02.

### D-034: Protection status is observed, not assumed; history kept 30 days; protection restarts when the app opens (M3.5, 2026-10-29)
**Decision:** Home shows what Identity can observe every 20 seconds while protection runs (`ProtectionHealth`): **Protected**; **Needs attention** with the reason (the lookup handler stopped, the filtering service could not be reached 5 lookups in a row, or Private DNS is custom); **Interrupted** (the user wants it on but it is not running); **Off**. This was prompted by a real case (D-033) where Home said "On" but the internet was down: Home must not rely on a saved flag alone.
- **History (`InterruptionLog`):** turn-ons, turn-offs, turned off in Android, ended unexpectedly, and each new problem, with the time. Kept on the phone for **30 days**, at most 200 entries. No domain names or browsing. Sharing any of it with a partner is M5 and opt-in.
- **"Ended unexpectedly":** a session flag stays set from start until the user (or Android's VPN settings) ends it on purpose. If it is still set at the next start, the last session never ended cleanly (Android stopped it, or the phone restarted). Cause unknown, said plainly.
- **Self-heal:** whenever the app is opened, protection that should be on but isn't running is started again. Android's own restart of a stopped service can take minutes or never come (it backs off after repeated stops); this does not wait for it.
**Limits, stated plainly:** "cannot reach the filtering service" also appears when the phone simply has no internet; M5 should check the network before alerting a partner. An app update can still log one "stopped unexpectedly" entry when Always-on VPN restarts the service first. The checks cannot see in-browser DNS-over-HTTPS or hardcoded DNS (R-02).

### D-035: Signed blocklist updates: ECDSA P-256, GitHub Releases, checked every time (M3.6, 2026-10-29)
**Decision:** the blocklist can update itself, but only to a list signed by the product owner's key. An update is three files: the list, a manifest (version, SHA-256, number of names) and a signature over the manifest. Published as assets of one fixed GitHub release (`blocklist-latest`) on the public repo; the app only ever contacts that address.
- **Signature type: ECDSA P-256, not Ed25519** (the roadmap's original choice). Android only supports Ed25519 natively from Android 13, and the app's minimum is Android 8 (D-007). P-256 is built into every Android version, equally strong, and avoids adding a cryptography library to a security-critical path.
- **The key:** generated on the product owner's laptop with OpenSSL; the private half never enters the repo or any chat, and is backed up by the product owner. The public half is in `BlocklistUpdate.PUBLIC_KEY_BASE64`. If the private key is lost or leaks, a new app version with a new public key is needed.
- **The app accepts an update only if all hold:** signature valid; manifest well-formed; version newer than the one in use (stops rollback to an old list); list no bigger than 20 MB; SHA-256 matches; the number of names matches the manifest; at least 50,000 names; at least half as many names as the list in use. Otherwise the old list stays and a history entry is written.
- **Checked again every time the list is loaded**, so a damaged or swapped file on the phone is ignored and the bundled list is used.
- **Schedule:** 2 minutes after protection starts, then at most once a day, while protection is on, on a timer that does not wake a sleeping phone. A connection failure is retried later and logged only after 3 failures in a row; a refusal is logged at once. A "Check for update" button in Settings does it on demand.
- **Version numbers** are the UTC time as a number (`202610291841`), so no counter is needed.
**Privacy:** the download is a plain HTTPS request; GitHub sees the phone's IP address like any website. Nothing else is sent. In the data inventory.
**Limits, stated plainly:** an update can only add or remove names from the adult list; X and Reddit, image-search servers, SafeSearch and TikTok are fixed in the app and unaffected. The signing key is a single point of trust. GitHub being unreachable just means no update. Files are replaced one at a time on GitHub, so a phone that checks mid-replacement refuses once and retries later.

### D-036: Refuse outside encrypted DNS: server names, resolver addresses, and a Home notice (M3.7b, 2026-10-29)
**Decision (product owner agreed with the recommendation):** Identity stops the browser setting "Use secure DNS" (and apps with their own DNS) from going around it, by two means, and says so.
1. **Refuse the names** of well-known encrypted-DNS servers (`BlockPresets.DOH_ENDPOINTS`): "no such domain". The phone test showed this is the part that matters: with only the addresses routed, Chrome still bypassed Identity, because it learns Cloudflare's encrypted-DNS addresses from the server's name.
2. **Route the main addresses** of the big public resolvers (`PublicResolvers`, IPv4 and IPv6) into the tunnel: plain DNS to them is answered by Identity's filter (so apps with their own DNS are filtered), and every other connection to them (DNS over HTTPS or TLS) is refused with a TCP reset (`TcpResets`), so apps fall back quickly. The reset code builds and checks IPv4 and IPv6 packets and is tested for valid checksums and bad input.
3. **Tell the user.** When a name or connection like this is refused, Home shows "On, but needs attention" for 5 minutes with what happened and how to change the browser setting, and a history entry is written (`OUTSIDE_DNS_BLOCKED`).
- Identity's own upstream (Cloudflare for Families, `1.1.1.3` and `1.0.0.3`) is never routed into the tunnel (a test enforces it).
**Consequences, stated plainly:** with Chrome's secure DNS set to a custom provider, **Chrome loads nothing** until the setting is turned off (its secure mode has no fallback). The websites of 1.1.1.1 and 8.8.8.8, the 1.1.1.1 WARP app, and a Private DNS setting pointing at a listed provider also stop working while protection is on. A resolver on neither list still bypasses (R-02). The lists are in the app and change only with an app update.
**Not changed:** the always-on blocks, SafeSearch, custom rules and signed list updates; the names are blocked as part of the always-on list, not through the signed update.

### D-037: M4 decisions: email and password, code invitations, 3 partners, nickname only (2026-10-29)
**Decision (product owner, accepting the recommendations in [plans/m4-accounts-and-partners.md](plans/m4-accounts-and-partners.md)):**
- **Backend:** Supabase free tier, as D-005 (not re-asked). For M4 the server-side actions are database functions rather than Edge Functions.
- **Sign-in:** email and password. Custom email sending (a domain, about R150 to R250 a year, D-004) is needed before the beta, because Supabase's built-in sender is for testing only.
- **Invitations:** a random one-time code, stored only as a hash, valid for 48 hours, shared with Android's share sheet and pasted into the partner's app. No website or domain needed.
- **Limits:** up to 3 partners per person. Either side can end a connection at any time; the other is told in plain words.
- **What a partner sees:** only the nickname the person chooses, never their email. Sharing choices per partnership, changeable at any time: protection status and interruptions (default on), daily blocked count (default off), changes to protection settings (default off). Domain names are never shared.
- **Accounts are optional:** nothing in protection, Scripture or the other features depends on one.
**Still to do before real users:** verify the free-tier terms (pausing, email limits, backups), custom email sending, and a legal review of special personal information under POPIA (M8 gate).

### D-038: Our own small web client for accounts, not a library (M4.2, 2026-10-29)
**Decision:** the app talks to Supabase with Android's built-in HTTPS client and the JSON library it already uses, in about 300 lines (`app.identity.account`), instead of the `supabase-kt` library that the architecture note and the M4 plan suggested.
**Why:** sign-in needs six web requests (sign up, sign in, refresh, sign out, and a few table calls). `supabase-kt` would add a whole networking stack (Ktor and its engines) and tie us to compatible Kotlin versions, which matters because the project's toolchain is pinned (D-015); it would also grow the app and add outside code to a security-critical path. Our client adds **no new library**, and every line is covered by tests that use the server's real answers (captured from the project on 2026-10-29, which caught a real bug: a dead refresh token is reported as "validation failed", not as an email problem).
**Consequences:** we maintain the small client ourselves. If accounts later need more than this (realtime, storage), we can adopt the library then; the repository hides the client behind `AccountApi`.
**Also decided in this slice:** the sign-in is stored encrypted with a hardware-backed key (`KeystoreCipher`); passwords are never stored; signing out ends only this phone's session (`scope=local`) and removes this phone from the account; accounts are optional and nothing in protection waits for the network.
