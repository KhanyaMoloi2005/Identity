# Identity: Project Log

The running record of **every change** made to this project, newest first. Read the latest entry to see where we are and what's next.

Each entry lists: what changed and why, the files touched, how to verify, and the next step.

Related documents: [roadmap](roadmap.md) · [decision log](decision-log.md) · [risk register](risk-register.md) · [architecture](architecture.md) · [original spec](spec/identity_sdlc_stitch_handoff.md)

---

## 2026-10-29: M3.4 Staying on (branch `m3/staying-on`)

> ✅ **Verified on a computer:** 75 `:filter-core` unit tests ran (0 failed, 0 skipped): 72 existing plus 3 new. `assembleDebug` and `lintDebug` pass with Gradle 8.9 and Java 17, built and installed from the terminal. ✅ **Phone (checked from the computer over USB, 2026-10-29):** after reinstalling the app, protection kept running; after killing the app's process, Android restarted it under a new process id; after a force-stop it stayed stopped (Android's rule) and Always-on VPN brought it back when the app was opened. ✅ **Phone (product owner):** after a real restart with Always-on off, protection came back (the service was running 2 minutes after boot, by the `BootReceiver`) and Home showed On plus the Always-on card; the product owner then set Always-on VPN and protection came back after a restart.
>
> ⚠️ **Mistake found and fixed:** this entry first recommended also turning on Android's **Block connections without VPN**, and said the restart test passed with it on. That was wrong. Shortly after, the product owner found there was **no internet at all** while protection showed On; turning that one option off restored it at once. Android's "Block connections without VPN" lets only traffic that goes through the VPN out, and Identity's VPN carries DNS lookups only (D-023), so everything else is blocked. The card text and this entry are corrected; see D-033. ⏳ **Not yet verified:** the Private DNS warning card on the phone; the "Interrupted" message (it cannot show while Always-on is set); how many seconds protection takes to return after a restart (reported only as "within minutes"); CI on a pull request.

**Task:** make protection stay on without the user having to remember (M3 reliability), and tell the user about the two settings that can quietly defeat it.

**What changed and why**
- `ProtectionPrefs` (app): one saved choice, "the user wants protection on". Set by Turn on, cleared by Turn off and by turning the VPN off in Android's settings or another VPN taking over (`onRevoke`), so protection never comes back against the user's wish.
- `BootReceiver` (app): after a phone restart (`BOOT_COMPLETED`) or an app update (`MY_PACKAGE_REPLACED`), starts protection, only if the user wants it and still has the VPN permission. New permission `RECEIVE_BOOT_COMPLETED`.
- `IdentityVpnService`: restarts itself if Android ends it while the user wants protection (`START_STICKY`); notes whether Android's Always-on VPN is set; if Android itself starts it for Always-on VPN, that counts as "wanted".
- `PrivateDns` (`:filter-core`): decides whether Android's Private DNS setting is a custom provider ("hostname"), which sends lookups around Identity (R-02). Tested.
- Home: a status of **Interrupted** (meant to be on, not running) instead of a plain "Off"; a card when Private DNS is custom, with the fix and a button; a card suggesting Always-on VPN (and warning to leave Block connections without VPN off) while protection runs without Always-on.

**Files:** `android/filter-core/src/main/kotlin/app/identity/filter/PrivateDns.kt` (new), `android/filter-core/src/test/kotlin/app/identity/filter/PrivateDnsTest.kt` (new), `android/app/src/main/java/app/identity/protection/{ProtectionPrefs.kt,BootReceiver.kt}` (new), `android/app/src/main/java/app/identity/protection/IdentityVpnService.kt`, `android/app/src/main/java/app/identity/feature/home/HomeScreen.kt`, `android/app/src/main/AndroidManifest.xml`, `android/app/src/main/res/values/strings.xml`, `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`.

**Acceptance criteria**
1. Private DNS classification (custom hostname flagged; off, automatic, missing and unknown values not): ✅ tested
2. Build and lint: ✅ (local); CI on a pull request: ⏳
3. Phone: protection returns after an app update, a killed process and a phone restart: ✅
4. Phone: Always-on guidance appears when it is off and disappears when set: ✅
5. Phone: Private DNS card appears for a custom provider and disappears on Automatic: ⏳ product owner to test
6. Phone: Home shows Interrupted when protection was cut off: ⏳ (needs Always-on off and a force-stop)

**Limits to know:** without Always-on VPN, Android only lets Identity restart after the user first unlocks the phone, so the phone is unprotected between switching on and unlocking. Always-on VPN starts earlier than the first unlock, which shortens that gap; **Block connections without VPN** cannot be used to close it (D-033). A user can still turn Always-on off or turn the VPN off in Android's settings; making that harder is M7. Turning the VPN off from Android's settings is treated as the user's choice and protection does not come back by itself until they turn it on. The Private DNS check can only warn: Android doesn't allow an app to change that setting.

**How to verify (phone)**
1. Turn protection on. Restart the phone and unlock it without opening Identity: the key icon and notification should return.
2. Settings → Network → VPN → Identity: set Always-on VPN, with Block connections without VPN **off**. Restart: protection returns.
3. Set Private DNS to a hostname (for example `dns.google`): Home shows "Your phone is set to bypass Identity". Set it to Automatic: the card disappears.

**Next step:** pull request and CI ✅, merge. Then 3.5 (observed status on Home, interruption log kept for 30 days).

---

## 2026-10-29: M3.3c-2 Leave YouTube Shorts and Instagram Reels, with two switches (branch `m3/shorts-reels`)

> ✅ **Verified on a computer:** 72 `:filter-core` unit tests ran (0 failed, 0 skipped): 69 existing plus 3 new. `assembleDebug` and `lintDebug` pass with Gradle 8.9 and Java 17, built and installed from the terminal. ✅ **Phone (product owner, 2026-10-29):** with the service on, Shorts and Reels both show a short message and take the user out of the app; with each switch off, that one plays normally and the other still closes; the switches keep their setting. Opening a Reels link and a Shorts link from the computer also ended on the phone's home screen. ⏳ **Not yet verified:** CI on a pull request; whether normal YouTube/Instagram use (feed, Stories, messages, a Reel shared in a message) is ever blocked by mistake over longer use.

**Task:** when the user opens YouTube Shorts or Instagram Reels, take them out and let them open the app again; the rest of each app keeps working (product owner, D-031). Two switches, one per app, kept for now.

**What changed and why**
- **Found on the phone:** with USB debugging, the screens were inspected (`uiautomator`). YouTube's Shorts screen contains `reel_watch_fragment_root` and `reel_recycler`; Instagram's Reels viewer contains `clips_viewer_view_pager`, `root_clips_layout` and `clips_viewer_container`. (Instagram's dump only worked with the Reel paused.)
- `ShortsReels` (`:filter-core`): the two watched apps and their markers in one place. Tests check that other apps (WhatsApp, TikTok, Chrome, Identity itself) are never watched.
- `ShortsReelsService` (app, an Accessibility Service): Android only sends it events from YouTube and Instagram. It checks, at most every 0.3 s, whether a marker is on screen. If so it presses **Back**, shows a short message, and presses **Home** if Back didn't leave within 4 seconds. It reads no text, and stores, logs and sends nothing.
- `ShortsReelsPrefs`: the two switches, both on by default.
- Settings: "YouTube Shorts and Instagram Reels" with a plain-language explanation of what the service sees, the two switches, a status line, and a button to Android's Accessibility settings (where only the user can turn the service on).
- **New permission:** the Accessibility Service itself (declared with `BIND_ACCESSIBILITY_SERVICE`). No internet or other permission added.

**Files:** `android/filter-core/src/main/kotlin/app/identity/filter/ShortsReels.kt` (new), `android/filter-core/src/test/kotlin/app/identity/filter/ShortsReelsTest.kt` (new), `android/app/src/main/java/app/identity/protection/{ShortsReelsService.kt,ShortsReelsPrefs.kt}` (new), `android/app/src/main/java/app/identity/feature/settings/SettingsScreen.kt`, `android/app/src/main/res/xml/shorts_reels_service.xml` (new), `android/app/src/main/AndroidManifest.xml`, `android/app/src/main/res/values/strings.xml`, `docs/decision-log.md` (D-031, D-032), `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`.

**Acceptance criteria**
1. Only YouTube and Instagram are watched; markers are full ids inside their own app: ✅ tested
2. Build and lint: ✅ (local); CI on a pull request: ⏳
3. Phone: Shorts and Reels close the app with a message; each switch controls only its own app; settings are kept: ✅
4. Phone: normal use of both apps not blocked by mistake over longer use: ⏳

**Also recorded (docs only):** the product owner's **focus hour** idea (a wake-up alarm that covers every app except essentials such as Phone and WhatsApp while the user reads and prays inside Identity; then the apps unlock) is in **D-032** and M6. The product owner will decide in **M7** whether the Shorts/Reels switches stay.

**Limits to know:** YouTube and Instagram can rename these screens in any update, so the markers need re-checking on a phone after app updates. The user can turn the service or either switch off. Works whether or not protection (the VPN) is on. The Google Play review of the Accessibility Service is the largest open policy risk (R-03) and only matters before sharing a build. Two small deprecation warnings (`LocalLifecycleOwner`) are left for a later tidy-up.

**How to verify (phone)**
1. Install the build. Settings → "YouTube Shorts and Instagram Reels" → Turn on in Accessibility settings → switch on "Identity: leave Shorts and Reels".
2. Open YouTube Shorts and Instagram Reels: each should close with a message. Use the rest of both apps normally.
3. Turn one switch off at a time and repeat.

**Next step:** pull request and CI ✅, merge. Then 3.4: staying on (restart after reboot, Always-on VPN, custom Private DNS detection).

---

## 2026-10-29: M3.3c-1 TikTok daily time limit (branch `m3/app-time-limit`)

> ✅ **Verified on a computer:** 69 `:filter-core` unit tests ran (0 failed, 0 skipped): 56 existing plus 13 new. `assembleDebug` and `lintDebug` pass with Gradle 8.9 and Java 17, built and installed from the terminal. ✅ **Phone (product owner, 2026-10-29):** granted both permissions, set 15 minutes; having already used TikTok for longer today, opening TikTok brought up Identity's page and closed TikTok ("it works fine"). ⏳ **Not yet verified:** CI on a pull request; raising the limit (starts tomorrow); midnight reset; the DNS cut-off on its own.

**Task:** let the user give TikTok a daily allowance. When it is used up, cover TikTok with a page and block its servers until tomorrow. Product owner's choices: TikTok is time-limited, not always blocked; a page on top of TikTok; fixed choices only; raising starts tomorrow (D-030).

**What changed and why**
- `TimeLimit` (`:filter-core`): the six choices, adding up time on screen from usage events, and the change rule (lower now, raise tomorrow). Pure logic, unit-tested.
- `BlockPresets.TIKTOK`: TikTok's servers, blocked only while the allowance is used up.
- `TikTokLimit` (app): saves the allowance (atomic file, no backup); reads today's time from Android's usage history; needs Usage access.
- `TikTokLimitMonitor` + `LimitReachedOverlay` (app): inside the protection service, a light timer checks TikTok; over the limit it shows a full-screen page (verse from the bundled library, Leave button, Back leaves) and tells the service to rebuild the filter with TikTok blocked.
- Settings: "TikTok time limit" with the two permission steps, six chips, used-today status, a note when a raise is waiting.
- **New permissions:** `PACKAGE_USAGE_STATS` (Usage access) and `SYSTEM_ALERT_WINDOW` (Display over other apps), both switched on by the user in system Settings.

**Files:** `android/filter-core/src/main/kotlin/app/identity/filter/{TimeLimit.kt (new),BlockPresets.kt}`, `android/filter-core/src/test/kotlin/app/identity/filter/{TimeLimitTest.kt (new),BlockPresetsTest.kt}`, `android/app/src/main/java/app/identity/protection/{TikTokLimit.kt,TikTokLimitMonitor.kt,LimitReachedOverlay.kt (all new),Blocklist.kt,IdentityVpnService.kt}`, `android/app/src/main/java/app/identity/feature/settings/SettingsScreen.kt`, `android/app/src/main/AndroidManifest.xml`, `android/app/src/main/res/values/strings.xml`, `docs/decision-log.md` (D-030), `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/privacy/data-inventory.md`, `docs/PROJECT_LOG.md`.

**Acceptance criteria**
1. Time adding, edge cases, change rule, TikTok list and look-alikes: ✅ tested
2. Build and lint: ✅ (local); CI on a pull request: ⏳
3. Phone: allowance chosen, TikTok covered by the page and closed once over the limit: ✅
4. Phone: raising the limit starts tomorrow; the limit resets at midnight; TikTok's servers stay blocked after Leave: ⏳ not yet checked

**Limits to know:** only works while protection is on. Cached videos may play briefly after the cut-off. Overlapping screens in TikTok are counted slightly low, never high. The Google Play review of Usage access and Display over other apps is still open (R-03). The 30-day lock is M7. One deprecation warning (`LocalLifecycleOwner`) is left for a later tidy-up.

**How to verify (phone)**
1. Install the build, protection on. Settings: allow Usage access, then Display over other apps.
2. Pick a limit. Once TikTok's time today passes it, opening TikTok shows the page; Leave goes Home.
3. Lower the limit: applies at once. Raise it: the status says it starts tomorrow.

**Next step:** pull request and CI ✅, merge. Then 3.3c-2 (YouTube Shorts and Instagram Reels only) once the product owner decides whether to allow an Accessibility Service (D-030).

---

## 2026-10-29: M3.3b Your own blocks: websites and words (branch `m3/custom-rules`)

> ✅ **Verified on a computer:** 56 `:filter-core` unit tests ran (0 failed, 0 skipped): 50 existing plus 6 new. ⏳ **Not yet verified:** the `app` module (CI). 📱 **Phone check pending.**

**M3.3a phone result (merged as PR #14):** the product owner reported "everything works", and observed that the app already blocks most of the sites and search phrases they tried.

**Task:** let users add their own blocks: websites and words in site names. Block-only, with a 10-minute undo (product owner, D-029). App blocking becomes slice 3.3c (block screen). The product owner asked why not cut the app's internet instead; the reasons are in D-029.

**What changed and why**
- `CustomRules` (`:filter-core`): cleans what the user types (URL → site name, drops `www.`), checks words (4+ letters), matches words inside names, and decides whether the undo window is still open. Tests cover all of these, plus the safety net that keeps Android's connectivity check working.
- `CustomRulesStore` (app): saves rules in one small file, safely (`AtomicFile`). The undo window is measured from the phone's start-up time, not its clock, so changing the time in Settings can't reopen it.
- `Blocklist.withCustomRules`: the bundled list plus the user's sites and words.
- **Live changes:** adding or undoing a rule rebuilds the filter in the background while protection keeps running.
- **Settings screen:** "Your own blocks" with two boxes (website, word), an Add button each, clear error messages, and a "Blocked by you" list showing **Undo** for 10 minutes, then **Permanent**.

**Files:** `android/filter-core/src/main/kotlin/app/identity/filter/CustomRules.kt` (new), `android/filter-core/src/test/kotlin/app/identity/filter/CustomRulesTest.kt` (new), `android/app/src/main/java/app/identity/protection/{CustomRulesStore.kt (new),Blocklist.kt,DnsTunnel.kt,IdentityVpnService.kt}`, `android/app/src/main/java/app/identity/feature/settings/SettingsScreen.kt`, `android/app/src/main/res/values/strings.xml`, `docs/privacy/data-inventory.md`, `docs/decision-log.md` (D-029), `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`.

**Acceptance criteria**
1. Typed/pasted sites cleaned; invalid input rejected; words need 4+ letters; word rules match names containing them; undo only within 10 minutes of the same boot; connectivity check never blocked: ✅ tested
2. CI green: ✅ (the product owner installed the CI build)
3. Phone: add a site → it stops loading: ✅ `wikipedia.org` blocked (product owner, 2026-10-29). Undo: ⏳ not reported yet
4. Phone: after 10 minutes a rule shows "Permanent" with no Undo: ⏳ product owner testing on the way home
5. Phone: restarting the phone keeps the rules: ⏳ product owner testing
6. Phone: a word rule blocks sites containing it: ⏳ not tested yet (needs a word the product owner wants blocked permanently)

**Limits to know:** a word rule blocks every site containing it, and after 10 minutes it can't be removed (until M7), so choose words carefully. Rules match site names only, not page text. Apps remember lookups for a few minutes.

**How to verify (phone)**
1. Install the CI build; protection on.
2. Settings → "Block a website" → `wikipedia.org` → Add. Force-stop Chrome, open wikipedia.org: blocked. Tap **Undo** (within 10 minutes), force-stop Chrome: it loads again.
3. Add a site you're happy to lose for good, wait 10 minutes, and check its row says **Permanent** with no Undo. Only add **words** you really want blocked forever.
4. Restart the phone: your rules are still listed.

**Next step:** after CI ✅ and the phone check, 3.3c (app blocking), starting with the Google Play policy check.

---

## 2026-10-29: M3.3a Always block X/Reddit and image-search pictures (branch `m3/rule-presets`, stacked on `m3/safesearch`)

> ✅ **Verified on a computer:** 50 `:filter-core` unit tests ran (0 failed, 0 skipped): 45 existing plus 5 new. ⏳ **Not yet verified:** the `app` module (CI). 📱 **Phone check pending.**

**Task:** answer the two M3.2 findings by blocking X and Reddit completely and blanking out image-search pictures. **Always on, no switches for users** (product owner, D-028).

**What changed and why**
- `BlockPresets` (`:filter-core`): the two lists of domains (D-028). Tests prove X/Reddit's app and image servers are caught, look-alikes (`max.com`, `t.com`) aren't, and Google Search, Gmail, Maps, Bing and DuckDuckGo search keep working.
- `Blocklist.load` (app): adds both lists to the bundled blocklist, so they're applied whenever protection is on.
- **Settings screen:** the placeholder is replaced by a short explanation of what protection always blocks. Nothing to switch.
- **History:** switches were built first, then removed before pushing at the product owner's request; the unpushed commits were replaced by this one.

**Files:** `android/filter-core/src/main/kotlin/app/identity/filter/BlockPresets.kt` (new), `android/filter-core/src/test/kotlin/app/identity/filter/BlockPresetsTest.kt` (new), `android/app/src/main/java/app/identity/protection/Blocklist.kt`, `android/app/src/main/java/app/identity/feature/settings/SettingsScreen.kt`, `android/app/src/main/res/values/strings.xml`, `docs/decision-log.md` (D-028), `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`.

**Acceptance criteria**
1. Lists catch X/Reddit (incl. app and image servers) and image-search picture servers; don't catch look-alikes, Google Search, Gmail, Maps: ✅ tested
2. CI green: ⏳ pending push
3. Phone: X and Reddit websites and apps don't load; Google Images shows blank pictures; normal Google search, YouTube, Gmail, Maps work; Settings shows the explanation and no switches: 📱 pending

**Limits to know:** blanking image-search pictures isn't a wall: opening a result may still load the full picture from its own site (D-028). Thumbnails in ordinary results, such as news pictures, also go blank. Apps remember lookups for a few minutes.

**How to verify (phone)**
1. Install the CI build, turn protection on, and **force-stop Chrome, the X app and the Reddit app**.
2. `x.com` and `reddit.com` in Chrome and in their apps: shouldn't load.
3. Google something and open **Images**: pictures blank or missing. Normal results, YouTube, Gmail and Maps work.
4. Settings: shows the "Protection" explanation, no switches.

**Next step:** after CI ✅ and the phone check, M3.3b: custom allow/block rules (to be confirmed: see the question in chat about whether users may add rules).

---

## 2026-10-29: M3.2 SafeSearch (branch `m3/safesearch`, stacked on `m3/bundled-blocklist`)

> ✅ **Verified on a computer:** 45 `:filter-core` unit tests ran (0 failed, 0 skipped): 38 existing plus 7 new. Each safe hostname was also resolved through Cloudflare for Families from the laptop: `forcesafesearch.google.com` → 216.239.38.120, `strict.bing.com` → 150.171.27.16, `safe.duckduckgo.com` → 191.235.126.53, `restrictmoderate.youtube.com` → 216.239.38.119. ✅ **CI and phone:** the product owner installed the CI build and reported "safe search works and I cannot view explicit content" (2026-10-29).

**Phone findings (follow-ups, not failures of this slice)**
- Some images on **X/Twitter and Reddit** got through: they have no enforceable safe mode, so they need blocking as whole sites → slice 3.3a ("Block X and Reddit", on by default).
- **"View image"** on a SafeSearch result opens the original picture from its own site, which DNS can't judge → slice 3.3a ("Block image search", on by default; reduces but cannot fully stop this).

**Also recorded**
- **M3.1 phone feedback:** nothing feels slow with the full list.
- **Back-button finding:** after visiting another site, pressing Back can show `example.com` again until the page is refreshed. The browser shows the page from its own memory without a new lookup, so DNS filtering can't act. Only pages opened before protection was on are affected. Added to the bypass checklist (3.7).
- **Idea for later:** the product owner suggested on-screen phrase and explicit-image detection. It needs an Accessibility Service or screen capture, which conflicts with D-012 and the spec, so it is recorded under "Later: ideas to investigate" in the roadmap, to be researched (Play policy, privacy, battery) before any decision.

**Task:** force Google, Bing, DuckDuckGo and YouTube into safe mode, so explicit results (including image search) are filtered and can't be turned back on.

**What changed and why**
- `SafeSearch` (`:filter-core`): maps each engine's normal hostnames to its official safe-mode hostname (D-027). Google covers all 187 country domains, including `google.co.za`.
- `DnsPackets.redirectedQuery` / `redirectedResponse`: ask upstream about the safe hostname, then reply to the phone's original question with an alias plus the safe addresses. An upstream answer about any other name is rejected.
- `DnsFilter`: blocked names stay blocked; search engines get a new `Redirect` decision; everything else is forwarded as before.
- `DnsTunnel`: the "ask Cloudflare" step is shared by forwarding and SafeSearch.
- YouTube uses **Moderate** restricted mode (D-027); one line to switch to Strict.

**Files:** `android/filter-core/src/main/kotlin/app/identity/filter/{SafeSearch.kt (new),DnsPackets.kt,DnsFilter.kt}`, `android/filter-core/src/main/resources/app/identity/filter/google-search-domains.txt` (new), `android/filter-core/src/test/kotlin/app/identity/filter/SafeSearchTest.kt` (new), `android/app/src/main/java/app/identity/protection/DnsTunnel.kt`, `docs/decision-log.md` (D-027), `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`.

**Acceptance criteria**
1. Engines map to the right safe hostnames; Gmail/Maps and look-alikes untouched: ✅ tested
2. Redirected reply carries the phone's own question, an alias and the safe address; wrong upstream answers rejected; IPv6 lookups redirected too; blocking still wins: ✅ tested
3. Safe hostnames resolve through Cloudflare for Families: ✅ checked from the laptop
4. CI green: ✅
5. Phone: SafeSearch enforced, explicit content not viewable: ✅ (product owner)

**Limits to know:** other search engines aren't covered (D-027). Browsers and apps remember lookups for a few minutes, so changes show after a short wait or a restart of the app. Explicit content inside social apps isn't affected.

**How to verify (phone)**
1. Install the CI build. Turn protection **off and on again**, and **force-stop Chrome and YouTube** (Settings → Apps → Force stop) so they forget old lookups.
2. Google: search anything, open the settings (☰ or your profile picture → SafeSearch). It should say SafeSearch is on and **locked** (e.g. "your network has turned on SafeSearch").
3. YouTube app or site: Settings → General → **Restricted Mode** should be on, with a message that it's enabled by the network.
4. Bing (bing.com) and DuckDuckGo: their safe-search setting should show strict and not be changeable.
5. Gmail and Google Maps still work.

**Next step:** after CI ✅ and the phone check, M3.3: custom allow/block rules.

---

## 2026-10-29: M3.1 Bundled blocklist (branch `m3/bundled-blocklist`)

> ✅ **Verified on a computer:** 38 `:filter-core` unit tests ran (0 failed, 0 skipped) with Gradle 8.9 and Java 17, the same versions CI uses: 32 existing plus 6 new. A one-off local check (not committed; it needs the real file path) loaded the real bundled list: 92,127 entries, about 2.4 MB of memory, 0.12 s to load, about 2 µs per lookup. Every entry and its `www.` subdomain was blocked; wikipedia.org, google.com, instagram.com, youtube.com, bible.com and whatsapp.net were not. ⏳ **Not yet verified:** the `app` module (CI). 📱 **Phone check pending.**

**Also recorded:** product owner reported M2.4 network switching (Wi-Fi ↔ mobile data) works; the battery observation is running. M3 goes next and the Scripture screens wait until functionality is done (D-026).

**Task:** replace the one test domain with a real adult-content blocklist that ships inside the app.

**What changed and why**
- **List:** HaGeZi's NSFW list (D-025), 92,127 domains, each also blocking its subdomains. Chosen because it is updated several times a day and keeps wrongly-blocked normal sites out.
- **Licence (GPL-3.0):** the list is its own readable file with a header crediting the source, version and our changes; the full licence text ships beside it; the build script is in the repo. A visible licences screen is added to M8.
- **Build script** `content/blocklists/build_blocklist.py`: downloads, cleans, sorts and de-duplicates the list, and refuses to write a suspiciously short one.
- **`CompactDomainSet`** (`:filter-core`): stores the whole list in one sorted byte array and finds names by binary search. About a quarter of the memory of a normal set, which matters for a VPN running all day. `DomainMatcher` now accepts it; the allow-beats-block rule is unchanged.
- **App:** `Blocklist` loads the file on the tunnel's background thread when protection turns on (lookups wait a fraction of a second, then are answered). If loading fails, DNS fails closed (D-023). Test builds also block `example.com` for easy checking; release builds don't.
- Status and notification text: "Adult sites are blocked".

**Files:** `content/blocklists/{build_blocklist.py,SOURCES.md}` (new), `android/app/src/main/assets/blocklist/nsfw.txt` (new, generated), `android/app/src/main/assets/licenses/hagezi-GPL-3.0.txt` (new), `android/filter-core/src/main/kotlin/app/identity/filter/DomainSet.kt` (new), `android/filter-core/src/main/kotlin/app/identity/filter/DomainMatcher.kt`, `android/filter-core/src/test/kotlin/app/identity/filter/CompactDomainSetTest.kt` (new), `android/app/src/main/java/app/identity/protection/{Blocklist.kt (new),DnsTunnel.kt,IdentityVpnService.kt}`, `android/app/src/main/res/values/strings.xml`, `docs/decision-log.md` (D-025, D-026), `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`.

**Acceptance criteria**
1. List built reproducibly by a script, with source, version and licence credited: ✅
2. Packed set agrees with a normal set (5,000 names, 20,000 lookups), skips invalid lines: ✅ tested
3. Real list loads, blocks its entries and subdomains, leaves common sites alone: ✅ checked on computer
4. CI green: ⏳ pending push
5. Phone: protection on → `example.com` blocked; normal sites and apps work; turning on feels no slower: 📱 pending
6. Phone: battery over a few hours similar to M2.4's result: 📱 pending

**Limits to know:** the list is frozen at today's version until signed updates (3.6). No list catches every adult site; Cloudflare for Families (D-003) and SafeSearch (3.2) are the other layers. Blocks are by domain, so adult content hosted on general sites (e.g. social media) is not caught (R-02). There is no licences screen yet (M8).

**How to verify (phone)**
1. Install the CI build. Turn protection on: the status should read "On. Adult sites are blocked on this phone."
2. `example.com` → "This site can't be reached". `wikipedia.org`, Instagram and WhatsApp work.
3. Optional: an adult site's address now fails with the code `DNS_PROBE_FINISHED_NXDOMAIN` under the message; that code means Identity blocked it (Cloudflare's block shows a different error). Not required: the list was checked on the computer.

**Next step:** after CI ✅ and the phone check, M3.2: SafeSearch.

---

## 2026-10-29: Recorded "uninstall protection" request for M7 (D-024) (branch `docs/m7-uninstall-friction`)

**What changed and why**
- The product owner asked for a switch that stops the user from ever uninstalling Identity.
- Android doesn't allow any ordinary app to make itself impossible to uninstall. The closest option is the Device Admin role, which adds a deliberate extra step (turn the role off in Settings, then uninstall). Agreed to plan it as **uninstall friction + tamper alert**: optional, off by default, honest about its limits.
- Placed in **M7** (commitment controls), because it belongs with the cooldowns and needs real protection (M3) to be worth having. It is gated on a **Google Play policy check** (R-03) before any code.

**Files:** `docs/decision-log.md` (D-024), `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`. Docs only.

**Next step:** unchanged. M2.4 phone testing continues.

---

## 2026-10-29: M2.3 Block a test domain, forward the rest (branch `m2/block-test-domain`)

> ✅ **Verified on a computer:** 32 `:filter-core` unit tests ran (0 failed, 0 skipped) using Gradle 8.9 and Java 17, the same versions CI uses: the 18 existing tests plus 14 new ones. ✅ **CI:** the product owner reported the branch build had no failures. 📱 **Phone-checked by the product owner** (2026-10-29): see "Phone result" below.

**Phone result**
- Protection on: `example.com` → "This site can't be reached". Protection off: it loads. ✅
- Protection on: `wikipedia.org` loads in Chrome, Instagram works. ✅
- **Finding:** the phone's Private DNS was first set to a **custom hostname**. With protection on, Android then showed "Network has no internet access" and "Private DNS server cannot be accessed". After switching Private DNS to **Automatic**, the warnings stopped and everything worked. Android sends all lookups to a custom Private DNS provider, so it fights a DNS-filtering VPN and can bypass it (risk R-02). **M3 must detect a custom Private DNS and guide the user to Automatic** (already listed as an R-02 mitigation).
- Not reported separately: Cancel in the VPN dialog, turning it off from system Settings, and the Android version. To be repeated in M2.4.

**M2.2 phone result (recorded here; merged as PR #9):** the product owner installed the CI build and reported that the VPN turns on, the notification shows, and "everything looks good". Individual criteria 3 to 6 (Cancel, browsing while on, Turn off, turning it off from Settings) and the Android version were not reported separately; they are repeated in this slice's phone check.

**Task:** protection does its first real job. While it is on, `example.com` should fail to load; every other site should load normally.

**What changed and why**
- The VPN now gives Android a DNS server address (`10.111.222.1`) and routes **only that address** into the tunnel. Lookups come to Identity; all other traffic still bypasses the VPN (D-023).
- New `DnsTunnel` (app): reads each lookup from the tunnel. Blocked names get "no such domain" from the phone. Others go to **Cloudflare for Families** (`1.1.1.3`, backup `1.0.0.3`, D-003), and the answer is passed back. A sleeping reader thread (no timer) keeps battery use low; four helper threads do the forwarding.
- New in `:filter-core`: `IpPackets` (reads and builds the IPv4/UDP packets that carry DNS, with correct checksums) and `DnsFilter` (block / forward / drop). Keeping this in the plain-Kotlin module means it is tested on a computer.
- The app now uses `:filter-core` (and so dnsjava) for the first time.
- **New permission: `INTERNET`**, needed to send allowed lookups to Cloudflare. Nothing else goes online. Recorded in the data inventory: the first data to leave the phone.
- Status and notification text now say "only example.com is blocked".
- Domain names are never logged or saved.

**Files:** `android/filter-core/src/main/kotlin/app/identity/filter/{IpPackets,DnsFilter}.kt` (new), `android/filter-core/src/test/kotlin/app/identity/filter/{IpPacketsTest,DnsFilterTest}.kt` (new), `android/app/src/main/java/app/identity/protection/DnsTunnel.kt` (new), `android/app/src/main/java/app/identity/protection/IdentityVpnService.kt`, `android/app/src/main/AndroidManifest.xml`, `android/app/build.gradle.kts`, `android/app/src/main/res/values/strings.xml`, `docs/privacy/data-inventory.md`, `docs/decision-log.md` (D-023), `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`.

**Acceptance criteria**
1. Packet reading/building and block/forward decisions unit-tested, incl. checksums, IP options, fragments, junk input: ✅ tested (computer)
2. CI green (build, lint, all unit tests): ✅ (reported by product owner)
3. Protection on → `example.com` fails in Chrome ("site can't be reached"): ✅ phone (`www.example.com` not reported separately)
4. Protection on → other sites load normally; apps work: ✅ phone (wikipedia.org, Instagram), with Private DNS on Automatic
5. Turn off → `example.com` loads again: ✅ phone
6. Cancel in the VPN dialog; turning it off from system Settings: ⏳ not reported; repeat in M2.4

**Limits to know:** only one test domain. IPv4 DNS only; IPv6, network switching and battery are M2.4. If the phone's **Private DNS** is set to a specific provider, or Chrome's **"Use secure DNS"** is set to a custom provider, lookups skip Identity (risk R-02). If the reader thread hits an unexpected error, websites stop loading while protection is on ("fail closed", D-023). The release (shrunk) build is still not checked with dnsjava (D-021); CI builds debug only.

**How to verify**
1. Push the branch, open a pull request, and wait for "Android build" ✅. If ❌, paste the red lines to Claude.
2. On the phone, first check two settings, and write down what they were set to:
   - Settings → Network & internet → **Private DNS**: should be "Automatic" or "Off".
   - Chrome → Settings → Privacy and security → **Use secure DNS**: should be off or "current service provider".
3. Install the APK from the run, turn protection on, then walk through criteria 3 to 6. Chrome remembers lookups for a minute, so if `example.com` still loads, close the tab, open an **Incognito** tab and try again.
4. Note your Android version (Settings → About phone).

**Next step:** after CI ✅ and the phone check, M2.4: Wi-Fi ↔ mobile data, IPv6 and a 24-hour battery observation.

---

## 2026-10-29: M2.2 VPN service skeleton (branch `m2/vpn-skeleton`)

> ⏳ **Not yet verified:** this laptop has no Android SDK, so the app was **not built here**. CI (GitHub Actions) builds it and runs lint and the unit tests once the branch is pushed. 📱 **Phone check pending:** the product owner will test at home. Nothing below has been run on a device.

**Task:** Identity can switch its VPN on and off with the user's permission and shows a notification while it's on. Nothing is blocked and no traffic goes through the VPN yet.

**What changed and why**
- New `IdentityVpnService` (in `protection/`). It creates a tunnel with an address but **no routes and no DNS server**, so browsing works exactly as before. Sending DNS into the tunnel before we can answer it (M2.3) would break every website.
- It runs as a foreground service with a silent "Identity protection is on" notification. Tapping it opens the app. Type `systemExempted` on Android 14+ (D-022).
- It stops cleanly when the user taps Turn off, turns it off in system Settings, or another VPN app takes over (`onRevoke`). If Android refuses to start it, it stops instead of crashing.
- Home card: **Turn on** asks for the notification permission (Android 13+, once), then shows Android's own "Connection request" VPN dialog. Only "OK" starts it. Status reads On/Off; **Turn off** stops it.
- **New permissions:** `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`, `POST_NOTIFICATIONS`. **Still no internet permission.** The service is not exported and is protected by `BIND_VPN_SERVICE`, so other apps can't control it.
- New status-bar icon `ic_stat_protection` (a filled version of the Home shield).

**Files:** `android/app/src/main/java/app/identity/protection/IdentityVpnService.kt` (new), `android/app/src/main/java/app/identity/feature/home/HomeScreen.kt`, `android/app/src/main/AndroidManifest.xml`, `android/app/src/main/res/values/strings.xml`, `android/app/src/main/res/drawable/ic_stat_protection.xml` (new), `docs/decision-log.md` (D-022), `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`.

**First CI run: ❌ lint, fixed.** The code compiled and all unit tests passed. Lint failed with 1 error: it believes the `systemExempted` service type also needs an exact-alarm permission. Android also accepts the user's VPN approval instead, which lint can't see because it is given at run time. Rather than add an alarm permission we don't use, that single check is switched off on the VPN service only, with a comment (D-022). The phone test confirms whether Android agrees; if it refuses, the service stops cleanly instead of crashing.

**Acceptance criteria**
1. CI green (build, lint, unit tests): ⏳ re-run pending (first run: compile ✅, tests ✅, lint ❌ fixed)
2. Turn on → Android's VPN dialog → OK → status "On", key icon in the status bar, notification shown: 📱 pending
3. Cancel in the dialog → stays "Off", no crash: 📱 pending
4. While on, websites in Chrome load normally: 📱 pending
5. Turn off → status "Off", notification and key icon gone: 📱 pending
6. Turning it off in Settings → Network → VPN also stops it, and the app shows "Off" when reopened: 📱 pending

**Limits to know:** it doesn't restart after a reboot or if Android ends the app (M3). The On/Off status is kept in memory only, so after Android ends the app it shows "Off". If the user refuses the notification permission, Android hides the notification, but the VPN key icon still shows. On Android 14+ users can swipe the notification away; the key icon stays.

**How to verify**
1. Push the branch and open a pull request. GitHub → Actions → "Android build" must be ✅. If ❌, paste the red lines to Claude.
2. At home: download `identity-debug-apk` from that run, unzip it, install the APK, and walk through acceptance criteria 2 to 6. Note your Android version.

**Next step:** after CI ✅ and the phone check, M2.3: point DNS at the tunnel, block one hard-coded test domain, forward everything else (this adds the internet permission).

---

## 2026-10-28: M2.1 `:filter-core`: DNS parsing and domain matcher (branch `m2/filter-core`)

> ✅ **Verified on a computer:** 18 unit tests ran (0 failed, 0 skipped) using Gradle 8.9 and Java 17, the same versions CI uses. ⏳ **Not yet verified:** the full-project CI run, because the module was tested in a scratch copy (this laptop has no Android SDK, so the `app` module could not be built here). No phone check is needed for this slice.

**Task:** a pure-Kotlin module that reads a DNS query, answers "no such domain", and decides whether a domain is blocked. No VPN, permission, UI or real blocklist yet.

**What changed and why**
- `DnsPackets.parseQuery`: reads a packet and returns the domain, record type and id. It rejects responses, packets with zero or several questions, truncated packets and a looping compression pointer, without crashing.
- `DnsPackets.nxDomainResponse`: builds a valid NXDOMAIN reply with the same id and question, response and recursion-available flags set, and no answers.
- `DomainMatcher`: `example.com` blocks itself and subdomains but not look-alikes such as `notexample.com`. Case and trailing dots are ignored. **Any matching allow rule wins over any block rule**, however specific.
- dnsjava 3.6.5 does the packet work (D-021). Added to the version catalog; the new module is registered in `settings.gradle.kts`.
- **CI fix:** the workflow now also runs `:filter-core:test`; the old command would have skipped these tests.

**Found by running the tests locally:** the first version did not compile (dnsjava's flag constants are bytes; its methods take ints). Fixed with `.toInt()` before committing. Running locally caught what would otherwise have been a red CI run.

**Files:** `android/filter-core/build.gradle.kts`, `android/filter-core/src/main/kotlin/app/identity/filter/{DnsPackets,DomainMatcher}.kt`, `android/filter-core/src/test/kotlin/app/identity/filter/{DnsPacketsTest,DomainMatcherTest}.kt`, `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/gradle/libs.versions.toml`, `.github/workflows/android.yml`, `docs/decision-log.md`, `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`.

**Acceptance criteria**
1. Parses a real, hand-built DNS packet: ✅ tested
2. Matcher: subdomains, case, trailing dot, look-alikes: ✅ tested
3. Allow beats block, documented order: ✅ tested
4. Valid NXDOMAIN reply matching the query: ✅ tested
5. Malformed or truncated packets rejected safely (incl. 200 random byte strings): ✅ tested
6. CI green with the new tests actually run: ⏳ pending

**Limits to know:** the matcher decides on the name only, so it cannot tell a blocked site from an allowed page on the same domain. Everything in the risk register for DNS (R-02) still applies. Internationalised names arrive as the form the packet carries them in; a real blocklist (M3) must use that form.

**How to verify**
1. Push `docs/reorder-m2` first, then `m2/filter-core` (this branch builds on it). Open pull requests.
2. GitHub → Actions → "Android build" must be ✅. In the "Build, lint and unit-test" step, find `:filter-core:test` and confirm it did not say NO-SOURCE.
3. If ❌, paste the red lines to Claude.

**Next step:** after CI ✅ and merge, M2.2 (VPN service skeleton): the first slice that needs your phone.

---

## 2026-10-28: Reordered the roadmap: filtering (M2) before more Scripture screens (D-020)

**What changed and why**
- Product owner said the priority is the back end and functionality; screens can change later. Agreed to do M2 (DNS filtering proof of concept) right after M1.2, because filtering is the riskiest core part and we should learn early whether it works on the phone.
- M1.3 to M1.6 are paused, not dropped. M2 is split into four slices (2.1 to 2.4).
- M1.2 stays as committed. Its CI check is still pending and the product owner's verse review is **in progress**; neither blocks starting M2.1.

**Files:** `docs/decision-log.md` (D-020), `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`. Docs only.

**Next step:** product owner approves the M2.1 plan (proposed in chat), then Claude builds `:filter-core`.

---

## 2026-10-28: M1.2 Scripture data layer (branch `m1/scripture-data-layer`)

> ⚠️ **Not yet verified.** This work laptop has no Java or Gradle, so nothing was compiled or tested here. The first real check is the GitHub Actions run (see "How to verify").

**Task:** load a curated verse set from a bundled file, with a repository and the project's first unit tests. No UI change, no permissions, no internet.

**What changed and why**
- `verses.json`: 24 verses (3 for each of the 8 spec topics) in the spec §5 format. Text was extracted **by script** from the official WEB download at ebible.org, not typed from memory (D-019, `content/scripture/SOURCES.md`).
- `Verse.kt`: the verse record and the fixed list of 8 topics.
- `VerseLibraryParser.kt`: reads the JSON and rejects broken data (unknown topic, blank field, duplicate id, missing field, bad JSON) with a failed result instead of a crash.
- `ScriptureRepository.kt`: list topics, list a topic's verses, get one verse; inactive verses are hidden. Held in memory; Room waits for M1.5.
- `ScriptureDataTest.kt`: 10 unit tests covering the acceptance criteria below.
- Added JUnit 4.13.2 as a **test-only** dependency (not shipped in the app).

**Files:** `android/app/src/main/assets/scripture/verses.json`, `android/app/src/main/java/app/identity/data/scripture/{Verse,VerseLibraryParser,ScriptureRepository}.kt`, `android/app/src/test/java/app/identity/data/scripture/ScriptureDataTest.kt`, `android/gradle/libs.versions.toml`, `android/app/build.gradle.kts`, `content/scripture/SOURCES.md`, `docs/decision-log.md`, `docs/roadmap.md`, `docs/CHECKLIST.md`, `docs/PROJECT_LOG.md`.

**Things to know**
- The repository is not wired to any screen yet (that is 1.3), so there is nothing to see on the phone.
- Some verses begin mid-sentence in lower case (for example Matthew 5:28 "but I tell you…") or carry a stray quotation mark (Job 31:1, Matthew 26:41). That is how the WEB text is divided; I left it unedited. Worth deciding in 1.3 whether to show these with a leading "…".
- The WEB writes God's name as "Yahweh" in the Old Testament.
- The verse choices are mine and **need your review**.
- A PowerShell script was blocked by the machine's execution policy; I did not bypass it and used bash tools instead.

**Acceptance criteria and status**
1. Format matches spec §5, documented: ✅ written (`Verse.kt`, `SOURCES.md`)
2. 8 topics × ≥3 verses from official WEB: ✅ written; 🔶 awaiting your review
3. Repository behaviour: ✅ written; ⏳ not run
4. Unit tests: ✅ written; ⏳ not run
5. CI green with more than 0 tests run: ⏳ pending
6. No permissions or internet added: ✅ manifest untouched

**How to verify**
1. Push `m1/scripture-data-layer` (GitHub Desktop; push `docs/completion-checklist` first, since this branch builds on it) and open a pull request.
2. GitHub → Actions → "Android build" must be ✅. Open the "Build, lint and unit-test" step and look for the test task running (not "NO-SOURCE").
3. If ❌, paste the red lines to Claude.
4. Read `content/scripture/SOURCES.md` and tell me which verses to swap.

**Next step:** once CI is green and you approve the verses, merge, then M1.3 (browse by topic).

---

## 2026-10-28: Added a completion checklist (branch `docs/completion-checklist`)

**What changed and why**
- Product owner asked for a checklist so the work stays focused. Added `docs/CHECKLIST.md`: a "done means done" list that applies to every task, all milestones with the M1 slices, open items, and the M8 beta gate.
- Docs only; no code changed. Work is paused until the product owner is back with their phone.

**Files:** `docs/CHECKLIST.md` (new), `docs/PROJECT_LOG.md`.

**How to verify:** open `docs/CHECKLIST.md` and confirm it matches the roadmap.

**Next step:** product owner approves M1.2 (Scripture data layer) when ready.

---

## 2026-10-27: Local tooling working: Android Studio + USB debugging (branch `docs/usb-setup`)

> ✅ **Verified by the product owner and by `adb`:** Android Studio installed on the personal laptop; Gradle sync succeeded; ▶ Run installed and opened the app on the phone over USB; **M1.1 confirmed working** (the product owner reported it fully works, which covers the checklist items left open earlier: Back behaviour, fonts, dark mode).

**What happened**
- Installed Android Studio (Standard setup). The SDK is at `%LOCALAPPDATA%\Android\Sdk`.
- Chose **USB debugging** over wireless: simpler, more reliable, charges the phone (wireless remains an optional extra).
- `adb devices` first showed `unauthorized`; tapping **Allow** on the phone fixed it. `adb` then reported `SM-A346E`, Android 16 (API 36).
- The first Gradle sync stopped because the disk had < 1 MB free. After freeing space (43 GB free), re-syncing finished successfully.

**Test device (closes the open question about the phone):** Samsung Galaxy A34 5G (SM-A346E), Android 16. D-007 (minSdk 26) stays as chosen: this one device does not change it.

**Files changed:** `docs/setup/android-studio.md` (rewritten: USB is the main route, with the disk-space and `unauthorized` fixes), `docs/PROJECT_LOG.md`.

**Open item: Gradle wrapper version (not committed).** Android Studio generated `android/gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` and changed `gradle-wrapper.properties` to **Gradle 9.7.1**, plus `android/gradle/gradle-daemon-jvm.properties`. CI and decision D-015 use **Gradle 8.9** (AGP 8.7.3). The local build works with 9.7.1, but local and CI should match. These files are left uncommitted until the version is chosen (default proposal: restore 8.9, commit the wrapper scripts, re-sync to confirm).

**Next step:** decide the wrapper version, then M1.2 (Scripture data layer, see the pause-point entry below for its criteria), pending approval.

---

## 2026-10-27: ⏸ PAUSE POINT: resume here on the personal laptop

**Where we are:** M0 complete. M1.1 (navigation shell) verified and merged. Development rules adopted. Work pauses until the product owner is on their **personal laptop** (with Android Studio, and USB or Wi-Fi debugging available).

**To resume (new Claude Code session):**
1. Clone `KhanyaMoloi2005/Identity` with GitHub Desktop and open Claude Code in the repo folder.
2. Ask Claude to read `docs/DEVELOPMENT_RULES.md`, the spec in `docs/spec/`, and this log, then continue from this entry.
3. Set up Android Studio with [setup/android-studio.md](setup/android-studio.md), then open the `android/` folder. Wireless debugging lets the ▶ Run button install straight onto the phone, so no more APK downloads.
4. With local tooling, Claude can also build and run tests on the laptop before pushing (computer checks), and the phone checks become quick.

**Proposed next task (awaiting product owner approval): M1.2, Scripture data layer**
- *Task:* verse/topic data format, a small bundled starter verse set, a repository that loads it, and the project's first unit tests. No UI change.
- *Scope note:* load the read-only bundled verses from a JSON asset in memory, with no Room database yet. Room arrives in 1.5 for saved verses and reflections. Record this as a decision when implemented.
- *Acceptance criteria:*
  1. Format matches spec §5 (`id`, `reference`, `text`, `translation`, `topic_tags`, `optional_reflection`, `is_active`) and is documented.
  2. Starter set: the eight spec topics (lust, temptation, self-control, purity, renewing the mind, repentance, grace, hope), at least 3 verses each, text copied from the official public-domain WEB source (not typed from memory), source recorded. Placeholder until ESV licensing is resolved; the product owner reviews verse choices.
  3. Repository: list topics, list a topic's verses, get one verse; inactive verses excluded.
  4. Unit tests: real bundled file parses; every verse has a known topic and every topic has at least one verse; malformed/incomplete data rejected safely; topic filtering; inactive verses hidden.
  5. CI ✅ with **more than 0 tests run**.
  6. Still no permissions or internet; no phone check needed (no UI change).
- *Likely files:* `android/app/src/main/assets/scripture/`, `android/app/src/main/java/app/identity/data/scripture/`, `android/app/src/test/java/app/identity/data/scripture/`, `android/gradle/libs.versions.toml`, `android/app/build.gradle.kts` (JUnit, test-only), `content/scripture/SOURCES.md`, docs.

**Follow-up list (not started; move into `docs/FOLLOW_UPS.md` with M1.2)**
- Update GitHub Actions versions (Node 20 deprecation, `setup-java@v5`, Ubuntu runner migration warnings from run #4).
- Review the `lint-report` artifact for warnings (lint passing only means no errors).
- Unit tests: none exist yet (fixed by M1.2).
- M1.1 phone checklist items not individually confirmed: Back behaviour, fonts, dark mode, TalkBack.
- In-app licences screen (font OFL texts) in Settings.

**Decisions awaiting the product owner**
- D-001: ESV licence from Crossway (blocks distributing any build to others).
- D-002 to D-008, D-014, D-015: still provisional.
- Phone model and Android version (confirms minimum Android version, D-007).

---

## 2026-10-27: Development rules adopted (branch `docs/development-rules`)

**What changed and why**
- Product owner supplied *"Identity: Incremental Development Rules for Claude Code"*, which governs **how** work is done (one task at a time; define, inspect, plan, implement, verify, report, pause; required start/end response format; honest verification). The SDLC spec remains the product specification.
- Saved as [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md), text unchanged apart from a note at the top giving the spec's location (`docs/spec/`).
- Also recorded: **M1.1 verified.** PR #2 check (Actions run #4) passed ✅ (`assembleDebug lintDebug testDebugUnitTest`, 2m 51s). The product owner installed the APK and saw the navigation, then merged PR #2.

**Files:** `docs/DEVELOPMENT_RULES.md` (new), `docs/PROJECT_LOG.md`, `README.md` (index link).

**Next step:** initial assessment per rules §12 (read-only), then await approval of the proposed task.

---

## 2026-10-27: M1.1: Navigation shell + bundled fonts (branch `m1/navigation-shell`)

> ✅ **Verified 2026-10-27:** PR #2 build passed; APK installed on the product owner's phone and navigation seen; PR #2 merged. (Detailed checklist items such as dark mode and Back behaviour were not individually reported.)

**What changed and why**
- Product owner approved the M1 plan (six slices) and agreed to downloading the fonts and to Claude drafting the starter verse list (in 1.2).
- **Bottom navigation** with four tabs: Home (shield), Scripture (open book), Partner (two people), Settings (sliders). Each tab is a placeholder for now, with honest text about what's coming.
- **Fonts bundled:** Inter for the interface, Literata for Scripture. Downloaded from Google's official fonts repo, checked to be genuine TrueType files (sizes matched GitHub's listing). OFL licences ship inside the APK (D-016).
- **Home** shows the wordmark and a Protection card: *"Not active yet. Filtering arrives in a later update."*
- **Scripture** shows a sample verse (Psalm 119:105, WEB, public domain) in the Literata reading font, so you can judge the reading style.
- Removed the temporary M0 `WelcomeScreen`. Version is now 0.0.2 (versionCode 2) so it installs as an update.
- New libraries: Navigation Compose 2.8.5 and kotlinx.serialization 1.7.3 (D-017). Still **no permissions, no internet**.

**How the pieces connect**
```
MainActivity → IdentityTheme (colours + fonts) → IdentityApp
IdentityApp  → Scaffold: [ NavHost (current screen) ] over [ NavigationBar (4 tabs) ]
Tap a tab    → navController.navigate(route) → NavHost shows HomeScreen / ScriptureScreen / PartnerScreen / SettingsScreen
Each screen  → ScreenColumn (shared padding, scrolling, heading for screen readers)
```

**Files created**

| File | Purpose |
|---|---|
| `android/app/src/main/res/font/inter.ttf`, `literata.ttf` | Bundled variable fonts |
| `android/app/src/main/assets/licenses/inter-OFL.txt`, `literata-OFL.txt` | Font licences, shipped in the app |
| `android/app/src/main/res/drawable/ic_nav_{home,scripture,partner,settings}.xml` | Tab icons |
| `.../ui/theme/Type.kt` | Inter/Literata font families, app typography, `ScriptureBody` style |
| `.../ui/navigation/TopLevelDestination.kt` | The four tabs and their type-safe routes |
| `.../ui/navigation/IdentityApp.kt` | App frame: screen area + bottom bar, tab switching |
| `.../ui/components/ScreenColumn.kt` | Shared layout for tab screens |
| `.../feature/home/HomeScreen.kt` | Home tab |
| `.../feature/scripture/ScriptureScreen.kt` | Scripture tab (sample verse) |
| `.../feature/accountability/PartnerScreen.kt` | Partner tab (placeholder) |
| `.../feature/settings/SettingsScreen.kt` | Settings tab (placeholder) |

(`...` = `android/app/src/main/java/app/identity`)

**Files updated:** `android/gradle/libs.versions.toml`, `android/build.gradle.kts`, `android/app/build.gradle.kts` (plugins, dependencies, version 0.0.2), `MainActivity.kt` (now shows `IdentityApp`), `ui/theme/Theme.kt` (typography), `ui/brand/Wordmark.kt` (Inter Medium), `res/values/strings.xml`, `CHANGELOG.md`, `docs/roadmap.md`, `docs/decision-log.md` (D-016 to D-018).
**Files deleted:** `ui/WelcomeScreen.kt` (replaced by the Home tab).

**How to verify**
1. Publish branch `m1/navigation-shell` in GitHub Desktop and open a pull request into `main`. Wait for the ✅.
2. Install the APK from that run (it installs over the old one).
3. Check:
   - four tabs at the bottom; the selected tab's icon sits in an amber pill
   - tapping each tab switches screens; the Back button from any tab returns to Home, then exits
   - the Scripture tab's sample verse is in a serif font (Literata), and the rest of the app is in a clean sans font (Inter)
   - switch the phone between light and dark mode: colours follow
   - with TalkBack on (optional), tab names are read aloud and screen titles are announced as headings

**Next step:** 1.2, the Scripture data layer and starter verse list.

---

## 2026-10-27: M0 complete: build verified, app running on phone (branch `m0/close-out`)

**Verified (by the product owner)**
- Pull request #1 (`m0/android-skeleton` → `main`): both GitHub Actions checks passed ✅. The app compiles, passes lint, and produces an APK.
- PR #1 merged into `main` (merge commit `a8d87d1`). Actions run #3 was the automatic build of `main` after the merge.
- The debug APK was downloaded from GitHub, installed on the product owner's phone, and opened: it shows the Identity wordmark and *"Foundations build. Protection is not active yet."*

**What changed and why**
- `.github/workflows/android.yml`: each change was being built **twice** (once for the branch push, once for the pull request), wasting free Actions minutes. Now it builds on pull requests, on pushes to `main`, and on manual "Run workflow" only.
- Marked M0 complete in `docs/roadmap.md` and `README.md`; risk R-15 (unverified code) marked mitigated.

**Files updated:** `.github/workflows/android.yml`, `docs/roadmap.md`, `docs/risk-register.md`, `docs/PROJECT_LOG.md`, `README.md`.

**Workflow from now on (each change)**
1. Claude makes changes on a new branch and commits locally.
2. You publish the branch in GitHub Desktop and open a pull request.
3. Wait for the ✅ check, then optionally test the APK from that run.
4. Merge, delete the branch, then in GitHub Desktop switch to `main` and pull.

**Still open (not blocking M1):** ESV licence (D-001), confirming provisional decisions, Android Studio on your personal laptop.

**Next step:** agree the M1 plan (app shell + offline Scripture), then build it in small slices.

---

## 2026-10-27: M0 step 2: Android skeleton + cloud build (branch `m0/android-skeleton`)

> ⚠️ **Status: written but UNVERIFIED.** No build has run yet. Nothing in this entry counts as working until the GitHub Actions build passes.

**What changed and why**
- Product owner is on a **work laptop**, doesn't want Android tooling installed on it, and has no USB cable. Decision D-013: builds run in the cloud with **GitHub Actions**, and the APK can be installed on the phone straight from GitHub (no cable).
- Wrote the smallest possible Android app: it opens one screen showing the Identity wordmark (the I-cross mark drawn in code + "dentity") and the text *"Foundations build. Protection is not active yet."*, in light or dark mode following the phone.
- **The app requests no permissions at all**, not even internet. Backup and device transfer of app data are disabled for privacy.
- Product owner changed D-001 to **ESV**. Recorded, with a licensing condition: ESV is copyrighted by Crossway, and bundling whole chapters offline likely needs their permission. Development will use public-domain WEB text as placeholder content in a format that lets ESV drop in later (risk R-10 raised to High).
- Contrast-checked the colour palette: the light-theme accent is too faint for small text on parchment, so it's used only as a fill, and button text is Ink (DESIGN.md §3).
- Added decisions D-013 (cloud builds), D-014 (placeholder application ID `app.identity`), D-015 (pinned toolchain versions). Added risk R-15 (unverified code).

**How the pieces connect**
```
Phone taps icon → AndroidManifest.xml says "start MainActivity"
MainActivity.kt → wraps everything in IdentityTheme (colours) → shows WelcomeScreen
WelcomeScreen.kt → IdentityWordmark (Wordmark.kt) + status text (strings.xml)
Gradle files → describe how to compile all of the above into an .apk
.github/workflows/android.yml → GitHub's servers run Gradle and publish the .apk
```

**Files created**

| File | Purpose |
|---|---|
| `android/settings.gradle.kts` | Lists the project's modules and where libraries may be downloaded from |
| `android/build.gradle.kts` | Declares the build plugins once for the whole project |
| `android/gradle/libs.versions.toml` | All library/plugin versions in one place (D-015) |
| `android/gradle.properties` | Gradle settings (memory, AndroidX) |
| `android/gradle/wrapper/gradle-wrapper.properties` | Which Gradle version to use (8.9) |
| `android/app/build.gradle.kts` | App module: app ID, min/target Android version, dependencies |
| `android/app/proguard-rules.pro` | Release shrinking rules (empty for now) |
| `android/app/src/main/AndroidManifest.xml` | Declares the app and its one screen; no permissions |
| `android/app/src/main/res/xml/data_extraction_rules.xml` | Excludes app data from cloud backup / device transfer |
| `android/app/src/main/res/values/strings.xml` | User-visible text |
| `android/app/src/main/res/values/colors.xml`, `values-night/colors.xml` | Startup colours (light/dark) |
| `android/app/src/main/res/values/themes.xml` | Startup window theme |
| `android/app/src/main/res/drawable/ic_launcher_foreground.xml` | I-cross launcher icon artwork |
| `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | Adaptive launcher icon (normal + round) |
| `android/app/src/main/java/app/identity/MainActivity.kt` | App entry point |
| `android/app/src/main/java/app/identity/ui/theme/Color.kt`, `Theme.kt` | Colour tokens and light/dark theme |
| `android/app/src/main/java/app/identity/ui/brand/Wordmark.kt` | I-cross mark + wordmark drawn in code |
| `android/app/src/main/java/app/identity/ui/WelcomeScreen.kt` | Temporary M0 screen (+ light/dark previews) |
| `.github/workflows/android.yml` | Cloud build: compile, lint, unit tests, upload APK |

**Files updated:** `docs/decision-log.md`, `docs/roadmap.md`, `docs/risk-register.md`, `docs/design/DESIGN.md`, `docs/setup/android-studio.md`, `docs/architecture.md`, `README.md`.

**Known gaps (intentional, for now)**
- No Gradle wrapper scripts (`gradlew`) yet: they include a small binary file that is normally generated by Gradle itself. CI uses Gradle 8.9 directly; Android Studio on the personal laptop will generate the wrapper.
- Fonts (Inter, Literata) not bundled yet; the system font is used until M1.
- No tests yet: there's no logic to test. Unit tests start in M1 with the verse repository.

**How to verify (once the repo is on GitHub)**
1. GitHub → repo → **Actions** → "Android build" should finish with a green ✅ (about 5–10 min).
2. If it fails ❌: open the failed step, copy the red error lines, and paste them to Claude.
3. Download the `identity-debug-apk` artifact on your phone and install it (see [setup guide](setup/android-studio.md#no-cable-install-from-a-github-actions-build)). Expected: an "Identity" icon (cream I-cross with an amber bar on navy); opening it shows the wordmark and the status text.

**Next step**
- **You:** push `main` and `m0/android-skeleton` to `github.com/KhanyaMoloi2005/Identity` yourself using **GitHub Desktop** (decided 2026-10-27; Claude does not push).
- **Then:** fix anything the first build reports, merge `m0/android-skeleton` into `main`, and M0 is complete.

---

## 2026-10-27: M0 step 1: Project foundations and planning docs

**What changed and why**
- Read the full specification and checked the development environment. Git is installed; Android Studio, Android SDK, Java and Node.js are **not** installed yet.
- Agreed approach: Kotlin + Compose app, our own DNS-only VPN for filtering, Supabase (free tier) backend, offline-first Scripture.
- Product owner said "go ahead" without answering the open questions, so Claude's recommended defaults were recorded as **Provisional** decisions D-001 … D-008 (see [decision-log.md](decision-log.md)). They can be changed at any time.
- Product owner asked that every change be recorded in a markdown file. This log is that file (D-010). The original spec is kept unchanged as the requirements baseline.
- Created the first brand concept: the "I-cross" mark, colour tokens and typography (see [design/DESIGN.md](design/DESIGN.md)).

**Files created / moved**

| File | Purpose |
|---|---|
| `docs/spec/identity_sdlc_stitch_handoff.md` | Spec, **moved** from the project root (content unchanged) |
| `README.md` | Project overview and index of documents |
| `CHANGELOG.md` | User-visible changes per app version (empty until M1) |
| `.gitignore` | Keeps build output and secrets (keystores, `.env`, signing keys) out of Git |
| `.editorconfig` | Consistent formatting |
| `.gitattributes` | Fixes line endings (stops Windows breaking the Gradle `gradlew` script later) |
| `docs/PROJECT_LOG.md` | This log |
| `docs/roadmap.md` | Milestones M0–M8 with acceptance criteria and status |
| `docs/architecture.md` | Stack, architecture diagram, filtering design, can/cannot enforce table, security baseline |
| `docs/decision-log.md` | Decisions D-001 … D-012 |
| `docs/risk-register.md` | Risks R-01 … R-14 |
| `docs/privacy/data-inventory.md` | What data exists, where, retention |
| `docs/design/DESIGN.md` | Brand and design system draft |
| `docs/design/brand/identity-icon-concept.svg` | App icon concept |
| `docs/design/brand/identity-wordmark-concept.svg` | Wordmark concept (dark + light) |
| `docs/setup/android-studio.md` | Step-by-step setup of Android Studio and your phone |

**How to verify**
- Open the two `.svg` files in `docs/design/brand/` with a web browser (double-click, or drag them into Chrome/Edge) to see the brand concepts.
- Run `git log --oneline` in the project folder: you should see the initial commit.

**Next step (M0 step 2)**
1. **You:** follow [setup/android-studio.md](setup/android-studio.md) until `adb devices` shows your phone.
2. **You:** tell Claude your phone model, Android version and PC RAM. Also confirm or change decisions D-001 … D-008.
3. **Claude:** create the `android/` project skeleton (on a feature branch), build it, and help you run a blank Identity app on your phone.
