# Software Development Life Cycle (SDLC)

## Identity — Android-First Pornography Blocker with Accountability

**Document purpose:** Define a realistic development lifecycle for an
affordable, privacy-conscious pornography-blocking and accountability
application, built with Claude Code.

**Initial audience:** Adults who voluntarily want help avoiding
pornography and who may choose a trusted accountability partner.\
**First platform:** Android.\
**Initial budget target:** As close to R0 as possible, assuming the
developer already has a suitable computer, Android device, internet
access, and Claude Code access.\
**Long-term vision:** Android, iOS, Windows, and macOS.

------------------------------------------------------------------------

## Product Name and Visual Identity

**Product name:** Identity

**Brand concept:** The capital “I” in Identity should subtly resemble a
cross. The mark should feel intentional, modern, calm, and recognisable,
not like a generic church logo. Explore a few concepts where the vertical
stroke of the “I” forms a cross, while keeping the word Identity readable
at small sizes and in an app icon.

**Visual direction:**
- Calm, confident, minimal, and premium rather than aggressive or shame-based.
- Use a restrained palette with strong contrast and accessible text sizes.
- Explore a dark charcoal/navy base with warm white and a restrained accent
  colour; validate legibility in light and dark contexts.
- Keep the cross-inspired “I” distinctive but avoid overly complex details
  that disappear at app-icon size.
- The UI should communicate protection, spiritual growth, and personal
  accountability without implying that the app can judge a person's faith
  or intentions.

**Working brand assets:** Create a simple wordmark, app icon concept, colour
tokens, typography rules, spacing rules, and reusable components. Treat the
first Stitch designs as exploration; review and refine them before coding.

------------------------------------------------------------------------

## UI Design Workflow — Google Stitch to Claude Code

Use Google Stitch first to explore and prototype the user interface, then
bring the selected design and its design rules into the project for Claude
Code to implement. Stitch is a design/prototyping step, not the finished
Android application or its security engine.

1. Start a project in Google Stitch and generate the main mobile screens
   from the prompt provided in the project handoff notes.
2. Review the screens and refine them before creating more. Keep the
   navigation, spacing, colours, typography, buttons, cards, and error
   states consistent.
3. Prototype the key journeys in Stitch: onboarding, dashboard, protection
   status, Scripture reading, Scripture First morning alarm, and
   accountability settings.
4. Export the project using the export options currently available in Stitch.
   Keep the generated screen files, screenshots, and `DESIGN.md` if provided.
5. Save the exported assets in the project's design/reference folder. Do
   not assume Stitch's generated web HTML is ready to use directly in a
   native Kotlin/Jetpack Compose app.
6. Ask Claude Code to inspect the exported screens and design system, then
   rebuild the selected UI in Kotlin/Jetpack Compose. Preserve the intended
   visual design while using native Android patterns and accessibility
   practices.
7. Implement functionality in small slices after the design direction is
   agreed. Never treat a visual prototype as proof that alarms, VPN
   filtering, authentication, or security controls work.
8. Commit the initial design references to Git so changes can be reviewed
   and reverted.

**Design handoff checklist**
- [ ] Identity wordmark and cross-inspired “I” direction selected.
- [ ] Main mobile screens generated and reviewed.
- [ ] Primary navigation and key user journeys prototyped.
- [ ] Colours, typography, spacing, and reusable components documented.
- [ ] Exported design files and `DESIGN.md` saved in the project.
- [ ] Claude Code has been instructed to implement native Android UI,
      not blindly copy web-only code.
- [ ] Accessibility, empty states, loading states, permission states, and
      errors have been considered.

------------------------------------------------------------------------

## 1. Product Vision

Build an accessible tool that helps people reduce access to pornography
through domain filtering, protection-status monitoring, accountability
reports, and optional Scripture-centred support.

The product should make bypassing protection more difficult and make
interruptions visible, while being honest about operating-system
limitations. It must **not promise to be impossible to bypass**.

### Product principles

-   **Protection first:** A reliable filtering engine is more important
    than a polished dashboard.
-   **Privacy by default:** Collect the minimum data needed for the
    features the user explicitly enables.
-   **Consent-based accountability:** The user chooses their
    accountability partner and what information to share.
-   **Honest reporting:** Distinguish observed events from assumptions.
    A domain request does not prove that a person intentionally viewed
    pornography.
-   **Spiritual support without shame:** Scripture is offered as
    supportive guidance, not as punishment or proof of failure.
-   **Incremental delivery:** Build, test, and review one small feature
    at a time.
-   **Cross-platform by design, not by assumption:** Reuse appropriate
    product logic, but implement native protection capabilities
    separately for each operating system.

------------------------------------------------------------------------

## 2. Scope

### Included in the first Android version (V1)

1.  Onboarding and privacy disclosures.
2.  Dashboard showing the actual protection status.
3.  DNS/domain-based filtering for known adult websites.
4.  Adult-domain blocklists and custom allow/block rules.
5.  Filtering-service health checks and observable interruption events.
6.  Local event records and minimal activity summaries.
7.  Account creation and device registration.
8.  Voluntary accountability-partner invitations.
9.  Daily reports and configurable alerts.
10. A Bible verse / Spiritual Support module, using a curated library
    supplied by the product owner.
11. A configurable Scripture First morning alarm and reading-completion
    workflow, with clear Android platform limitations.
12. Basic strict-mode commitments and delayed settings changes, within
    Android's capabilities.
13. A documented recovery and account-deletion process.

### Explicitly out of scope for V1

-   Guaranteeing that the app cannot be bypassed.
-   Rooting devices or silently taking control of a personally owned
    device.
-   Intercepting or decrypting HTTPS traffic.
-   Collecting screenshots or complete browsing histories.
-   Detecting every pornographic image or video inside social media
    apps.
-   Full iOS, Windows, and macOS protection engines.
-   AI-based content classification.
-   A complex subscription and billing system before the blocker is
    proven reliable.

These items may be investigated later, subject to platform APIs, user
consent, licensing, privacy, security, and budget.

------------------------------------------------------------------------

## 3. Software Development Life Cycle

Use an **iterative SDLC**. Each phase produces a deliverable that can be
reviewed before moving to the next. Repeat the plan--build--test--review
cycle for every feature.

### Phase 1 --- Planning and feasibility

**Objectives** - Define the users, problem, product goals, budget, and
success criteria. - Research Android's networking and device-management
limits. - Investigate suitable existing filtering engines and their
licences. - Identify privacy, security, and distribution risks.

**Activities** - Write user stories and acceptance criteria. - Separate
must-have, should-have, and later features. - Estimate costs for
development, testing, hosting, and publishing. - Maintain a risk
register and decision log.

**Deliverables** - Product requirements document. - Initial backlog. -
Feasibility assessment. - Risk register. - Budget assumptions.

**Exit criteria** - V1 scope is agreed. - The team understands that
standard Android apps cannot guarantee resistance to every bypass. - A
feasible approach to DNS filtering has been selected for a proof of
concept.

### Phase 2 --- Requirements analysis and system design

**Objectives** - Define how the app should behave before implementing
it. - Design the application architecture, data model, screens, and
security boundaries.

**Activities** - Map user journeys for setup, blocking, reporting,
partner linking, and Scripture support. - Design database tables and
access policies. - Decide which events stay on the device and which are
sent to the backend. - Design offline behaviour, error states, recovery,
and account deletion. - Define test cases for every critical
requirement.

**Deliverables** - Architecture diagram. - Screen wireframes. - Database
schema and access-control rules. - Data-flow and privacy design. - Test
plan and acceptance criteria.

**Exit criteria** - Every V1 feature has a defined expected behaviour. -
Sensitive data flows and permissions are documented. - The filtering
design is technically reviewed before implementation.

### Phase 3 --- Development

Implement the app in small, reviewable increments.

Suggested order:

1.  Project setup, navigation, design system, and local settings.
2.  DNS-filtering proof of concept.
3.  Protection status and interruption handling.
4.  Blocklists and custom rules.
5.  Local event store and report aggregation.
6.  Authentication, device registration, and partner invitations.
7.  Backend reporting and notifications.
8.  Bible verse library and Spiritual Support workflow.
9.  Scripture First alarm, schedule, reading plan, and completion flow.
10. Strict-mode commitment controls.
11. Accessibility, error handling, and release preparation.

**Deliverables** - Working code. - Unit tests and integration tests. -
Updated technical documentation. - Small Git commits with clear
messages.

**Exit criteria** - Each feature meets its acceptance criteria. - Code
changes have been reviewed and tested. - No secrets or privileged
backend keys are embedded in the Android app.

### Phase 4 --- Testing and verification

**Functional testing** - Known test domains are blocked. - Allowed sites
remain accessible. - Custom allow/block rules work as intended. -
Reports show the correct events and timestamps. - Partner invitations
expire and cannot be reused. - Scripture topics, saved verses, and
reminder settings work.

**Network and reliability testing** - Wi-Fi to mobile-data
transitions. - Device reboot and service restart. - VPN permission
revoked. - VPN service unexpectedly stopped. - DNS timeouts and
blocklist-update failures. - IPv4 and IPv6 behaviour. - DNS-over-HTTPS
and other common bypass scenarios. - Battery consumption and app
responsiveness.

**Security and privacy testing** - A partner cannot access another
user's data. - Authentication and database policies are tested using
separate accounts. - Event data is minimised and retained only as long
as needed. - Logs do not accidentally expose sensitive browsing
information. - Account deletion and partner-link revocation work. -
Reports distinguish verified events from uncertain causes.

**Deliverables** - Test results. - Defect list with severity and
priority. - Security/privacy review notes. - Release-candidate
checklist.

**Exit criteria** - No unresolved critical security defects. - Blocking
and ordinary browsing are tested on physical devices. - Known
limitations are documented. - Recovery behaviour is tested.

### Phase 5 --- Deployment and private beta

**Activities** - Build and sign a release candidate. - Prepare clear
permission disclosures and privacy documentation. - Test with a small
group of consenting adults. - Provide a way to report bugs and request
account deletion. - Monitor crashes, filtering failures, and false
positives. - Confirm applicable Google Play requirements before
publishing.

**Deliverables** - Signed beta build. - Privacy notice and user
instructions. - Beta feedback log. - Release notes and rollback plan.

**Exit criteria** - Beta users understand what is blocked and what is
reported. - No serious data exposure or reliability issue remains
unresolved. - Critical feedback has been addressed or documented.

### Phase 6 --- Maintenance and expansion

**Ongoing activities** - Maintain and update adult-domain lists. -
Update dependencies and address security vulnerabilities. - Monitor
reliability and false positives. - Improve device compatibility and
battery use. - Review privacy practices and data retention. - Add
features based on user feedback. - Investigate native implementations
for Windows, macOS, and iOS.

**Deliverables** - Regular releases. - Updated blocklists and
dependencies. - Security and reliability reports. - Prioritised roadmap.

------------------------------------------------------------------------

## 4. Proposed Technical Architecture

### Android client

-   **Language:** Kotlin.
-   **UI:** Jetpack Compose.
-   **Network filtering:** Android `VpnService` with a carefully
    evaluated DNS-filtering engine.
-   **Local storage:** Room for structured local data.
-   **Background work:** Appropriate Android-supported scheduling and
    service mechanisms.
-   **Testing:** Unit tests, instrumentation tests, Android emulator,
    and physical devices.
-   **Build tooling:** Android Studio, Android SDK, Gradle, and Git.

### Backend — recommended starting architecture

**Recommendation: Node.js with TypeScript for custom server logic, with
Supabase for PostgreSQL database hosting and authentication.** This is a
practical, low-cost starting point for a small team or solo developer.
Node.js is not automatically more secure than other runtimes; security
comes from sound architecture, careful implementation, least-privilege
permissions, dependency maintenance, and testing.

Use the smallest backend that meets the product's needs. The first
prototype may use Supabase Auth and PostgreSQL directly through its
supported client, protected by carefully designed Row Level Security
(RLS) policies. Add a Node.js/TypeScript API when custom server-side
business logic is needed, such as partner invitation acceptance, report
aggregation, abuse prevention, or security-sensitive actions. Use a
maintained framework such as Fastify or NestJS only after evaluating
complexity and maintenance needs; do not add layers without a reason.

Potential responsibilities:

- Authentication and session/account lifecycle.
- Device registration and last check-in status.
- Accountability-partner invitations, consent, revocation, and access.
- User-selected report preferences and aggregated reports.
- Policy and blocklist version metadata.
- Notification delivery and rate-limited security-sensitive actions.

**Security requirements:**

- Never ship Supabase service-role keys, database passwords, signing
  secrets, or other privileged credentials inside the Android app or a
  public web client. Keep them in server-side secret configuration.
- Enable and test RLS for every table exposed to a client. Deny access by
  default; grant users access only to their own records and explicitly
  consented reports. Do not rely only on hiding UI controls.
- Verify authentication and authorization on the server for every
  sensitive operation. Validate inputs, rate-limit login/invitation and
  report endpoints, and use short-lived, single-use partner invitations.
- Use HTTPS/TLS in transit, secure session handling, dependency updates,
  backups, and appropriate monitoring. Avoid logging passwords, tokens,
  full URLs, or sensitive browsing data.
- Minimise collected data. Store aggregated activity where possible,
  define retention and deletion rules, and make sharing optional and
  revocable. A partner must never receive a user's full browsing history
  by default.
- Test RLS and API permissions with separate user accounts, including
  attempts to access another user's records and revoked partner access.

The app's core filtering and Scripture reading should work on-device and
offline. Do not route every website request through the backend. This
reduces sensitive data collection and keeps filtering usable when the
server is unavailable.

### Dashboard

A React and TypeScript web dashboard can be added for accountability
partners. It should be designed as a separate client of the backend,
with access controlled by the same server-side policies.

### Important networking limitation

DNS filtering can block known adult domains, but it generally cannot see
the full page URL or inspect encrypted HTTPS content. It may not
identify adult content hosted on general-purpose domains or inside every
social media app. DNS requests can also originate from background app
activity, so they must not automatically be treated as proof of
intentional viewing.

Do not intercept or decrypt HTTPS traffic in V1.

------------------------------------------------------------------------

## 5. Bible Verses / Spiritual Support Feature

The Scripture module is a separate app feature and can be developed
alongside the interface while the filtering engine is being tested.

### Purpose

Offer a private, Scripture-centred way for users to pause, reflect, and
choose a constructive next step when experiencing temptation or related
struggles.

### Proposed features

-   Curated verse library.
-   Topic categories such as lust, temptation, self-control, purity,
    renewing the mind, repentance, grace, and hope.
-   An "I'm struggling" shortcut.
-   Verse text and reference, with optional context or reflection.
-   Saved verses.
-   Optional personal reflection notes.
-   Daily verse and user-controlled reminders.
-   Offline access.
-   Optional, explicit sharing of selected progress information only if
    the user chooses it.

### Suggested user workflow

1.  User opens **Spiritual Support** from the dashboard or a shortcut.
2.  User selects a topic or chooses "I'm struggling."
3.  App displays a curated Scripture passage.
4.  User can read a reflection, pray, save the passage, read another
    verse, or choose a practical next step.
5.  User returns to their day with no automatic accountability
    notification.

Do not automatically infer a spiritual struggle from a blocked domain
request. Offer optional prompts instead, because a request may be
accidental or generated by an app in the background.

### Suggested data model

Each verse record can contain:

-   `id`
-   `reference`
-   `text`
-   `translation`
-   `topic_tags`
-   `optional_reflection`
-   `is_active`

Store the initial curated library locally so it works offline. The
product owner will provide and review the verse content and
translations. Check translation licensing before distributing
copyrighted Bible text.

### Scripture feature acceptance criteria

-   Users can browse verses by topic.
-   The "I'm struggling" flow opens an appropriate curated passage.
-   Saved verses remain available offline.
-   Reminder preferences can be changed or disabled.
-   Private reflections are not automatically shared with accountability
    partners.
-   Tests verify that the feature does not create accidental data
    sharing.

------------------------------------------------------------------------

## 6. Scripture First — Morning Accountability Alarm

### Purpose

Give users the option to begin the day with Scripture before moving into
their usual phone routine. The feature should encourage a consistent
habit without shaming users or pretending an ordinary Android app can
make the entire phone impossible to use.

### User setup

- Choose a wake-up time and the days on which the alarm repeats.
- Choose a reading plan or book of the Bible (for example, Proverbs,
  Psalms, or a user-selected plan).
- Set the minimum reading requirement: a chapter or a specific passage.
- Optionally enable a short reflection, prayer prompt, or one practical
  action after reading.
- Configure sound, vibration, snooze rules, and accessibility options.
- Explain clearly which app restrictions can be applied and which can
  still be bypassed through Android system controls.

### Proposed morning workflow

1. The alarm rings at the scheduled time and offers a direct route into
   Scripture First mode.
2. The app displays the assigned chapter or passage, with its book,
   chapter, and verse reference.
3. The user reads the required content. A simple completion control is
   available after the passage has been displayed; the app should not
   claim it can prove that a person genuinely read or understood it.
4. Optionally, the user answers a short reflection question, reads a
   prayer, or selects one practical action for the day.
5. When the required steps are completed, the app marks the session
   complete and releases only the restrictions that the app itself
   controls for this routine.
6. The app records completion locally. Sharing completion with an
   accountability partner must be optional, explicit, and configurable.
7. The user returns to normal phone use. Pornography filtering and any
   separately enabled protection settings remain active.

### Strictness levels

- **Gentle:** Alarm and Scripture reminder; user can leave the screen.
- **Focused:** Full-screen reading flow, optional snooze limits, and a
  reminder if the session is abandoned.
- **Commitment mode:** Use only Android-supported controls and clearly
  disclose their limits. Do not promise a device-wide lock that a
  normal app cannot enforce. Do not silently prevent emergency calls,
  system navigation, or access to essential phone functions.

The user should choose the strictness level during setup and be able to
review the consequences before enabling it.

### Android implementation notes

- Use Android-supported alarm scheduling and request exact-alarm
  permission only if the chosen implementation and Android version
  require it.
- Handle reboot, battery optimisation, notification permission, Do Not
  Disturb, time-zone changes, daylight-saving changes, and missed
  alarms.
- Provide a reliable fallback notification if full-screen alarm
  presentation is restricted by the OS.
- Test on physical devices as well as an emulator because alarm delivery
  and background behaviour vary by Android version and manufacturer.
- Do not rely on a standard app to prevent users from opening Settings,
  force-stopping or uninstalling the app, or using another device.
- Keep Scripture content available offline. Check the licensing terms
  for any Bible translation distributed in the app.

### Acceptance criteria

- Users can configure, edit, pause, and disable their alarm schedule.
- The assigned chapter or passage matches the selected reading plan.
- The reading screen works offline and supports readable text sizes.
- Completing the reading marks the session complete and applies the
  documented release behaviour.
- Leaving or missing a session is recorded neutrally, without claiming
  the user intentionally avoided Scripture.
- Reboot, time changes, permission changes, and missed alarms have
  defined and tested behaviour.
- Any sharing of reading completion is off by default and requires
  explicit user consent.
- The app never claims it can verify genuine attention or guarantee
  device-wide lockout.

### Suggested backlog item

**SP-05 — Scripture First morning alarm**  
Priority: Should have for the first usable beta; may be moved to Must
have if the product owner considers it central to the product.  
Acceptance: User can schedule a morning alarm, read a selected chapter
or passage offline, complete the session, and receive only the
documented app-level restriction changes. Android limitations are
disclosed and tested.

------------------------------------------------------------------------

## 7. Accountability and Reporting Workflow

1.  User creates an account and registers their device.
2.  User invites an accountability partner using a secure, expiring
    invitation.
3.  Partner accepts using their own account.
4.  User selects which report categories to share.
5.  The device records only the minimum necessary events.
6.  Reports are aggregated and delivered securely.
7.  User can review the sharing settings and revoke the relationship.

### Suggested report events

-   Filtering service activated.
-   Adult-domain request blocked.
-   VPN permission revoked or connection interrupted.
-   Blocklist update failed.
-   Protection status could not be verified.
-   Relevant configuration change detected.

Use careful event labels. For example, report "VPN connection
interrupted; cause unknown" rather than claiming that the user
deliberately bypassed protection when the app cannot establish intent.

------------------------------------------------------------------------

## 8. Initial Product Backlog

  -----------------------------------------------------------------------
  ID                Feature           Priority          Acceptance
                                                        criteria
  ----------------- ----------------- ----------------- -----------------
  CORE-01           Onboarding and    Must have         User understands
                    privacy                             permissions and
                    disclosure                          reporting

  CORE-02           Protection        Must have         Displays observed
                    dashboard                           protection status

  BLOCK-01          DNS filtering     Must have         Test domain is
                    proof of concept                    blocked without
                                                        breaking ordinary
                                                        browsing

  BLOCK-02          Adult-domain      Must have         Rules can be
                    blocklists                          updated and
                                                        versioned

  BLOCK-03          Custom            Should have       User rules are
                    allow/block rules                   applied
                                                        predictably

  SEC-01            Interruption      Must have         Observable
                    monitoring                          protection
                                                        interruptions are
                                                        recorded

  ACC-01            Partner           Must have         Secure, expiring
                    invitation                          invitation works

  ACC-02            Daily reports     Must have         Partner receives
                                                        only consented
                                                        report data

  ACC-03            Tampering alerts  Should have       Observable
                                                        changes trigger
                                                        accurate alerts

  SP-01             Scripture library Must have         Verses can be
                                                        browsed by topic

  SP-02             "I'm struggling"  Must have         User can access a
                    flow                                curated verse and
                                                        next step

  SP-03             Saved verses      Should have       Saved content
                                                        works offline

  SP-04             Daily verse       Could have        User controls
                    reminders                           schedule and
                                                        opt-out

  SP-05             Scripture First    Should have       User can schedule
                    morning alarm                        a reading session
                                                        and complete it
                                                        offline

  PRIV-01           Account deletion  Must have         User can request
                                                        deletion and
                                                        understand
                                                        retention

  TEST-01           Bypass and        Must have         Known scenarios
                    reliability test                    are documented
                    suite                               and tested
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 9. Claude Code Development Workflow

Use Claude Code as an implementation assistant, not as a substitute for
testing or engineering decisions.

### For each feature

1.  **Plan:** Ask Claude Code to inspect the relevant code and propose a
    small implementation plan.
2.  **Review:** Check the plan, dependencies, security implications, and
    acceptance criteria.
3.  **Implement:** Ask it to make only the agreed change.
4.  **Test:** Run the tests and manually verify behaviour on an emulator
    or physical device.
5.  **Review the diff:** Check what changed and ask for explanations of
    unfamiliar code.
6.  **Commit:** Use Git to create a small, descriptive commit after
    verification.
7.  **Document:** Update setup instructions, known limitations, and test
    results.

### Initial Claude Code prompt

> Act as a senior Android engineer and a security-conscious technical
> mentor. We are building an Android-first pornography blocker with DNS
> filtering, voluntary accountability reports, strict-mode controls, a
> private Bible verse / Spiritual Support module, and a Scripture First
> morning alarm.
>
> I am learning development and want to build incrementally with minimal
> initial spending. Use Kotlin and Jetpack Compose for Android. For the
> backend, prefer Node.js with TypeScript for custom server logic and
> Supabase PostgreSQL/Auth where appropriate. Design and test strict RLS
> policies; never expose privileged keys in clients. Evaluate maintained,
> appropriately licensed DNS-filtering options before writing a packet
> processor from scratch.
>
> First, inspect the repository and produce an architecture proposal,
> feasibility assessment, dependency/licence review, threat model, RLS
> policy plan, risk register, test plan, and staged backlog. Do not build
> the entire app yet. Explain Android VPN and alarm limitations, privacy
> risks, emulator-versus-physical-device testing, and manual setup steps.
> Never claim a feature is implemented or tested unless you have actually
> verified it. Wait for approval before implementation.

### Recommended Git practice

-   Keep the main branch in a working state.
-   Create a branch for each feature or meaningful change.
-   Commit after tests pass.
-   Avoid combining filtering, backend, and UI changes into one large
    commit.
-   Never commit API keys, signing secrets, or personal user data.
-   Keep a `README.md`, `CHANGELOG.md`, and a simple architecture
    document.

------------------------------------------------------------------------

## 10. Risks and Constraints

  -----------------------------------------------------------------------
  Risk                                Mitigation
  ----------------------------------- -----------------------------------
  Android user can disable or         Explain limits; use supported
  uninstall the app                   controls and report observable
                                      interruptions

  DNS filtering misses some adult     Maintain blocklists, document
  content                             limitations, and test common
                                      evasion methods

  False positives                     Allow careful review and safe
                                      allowlisting

  VPN causes connectivity problems    Test network transitions, IPv6, DNS
                                      failures, and recovery

  Sensitive reports are exposed       Minimise data, use encryption, and
                                      enforce backend access policies

  Accountability becomes coercive     Require explicit consent, clear
                                      sharing settings, and revocation
                                      controls

  Free backend limits are exceeded    Monitor usage and budget for
                                      eventual production hosting

  Bible text licence restrictions     Verify translation permissions
                                      before distribution

  AI-generated code contains defects  Review changes, run tests, and
                                      obtain expert review for
                                      security-critical components
  -----------------------------------------------------------------------

### Android platform note

Android's `VpnService`, always-on VPN, and lockdown capabilities have
platform-specific constraints. On ordinary personally owned devices, the
app cannot guarantee protection against every deliberate bypass or
factory reset. Stronger device-management controls may require
managed-device provisioning and are not automatically available to a
standard consumer app.

Check current Google Play requirements for VPN applications,
disclosures, consent, and data handling before public distribution.

------------------------------------------------------------------------

## 11. Budget and Milestones

### Initial-cost approach

-   Development tools: use free tools where possible.
-   Backend: start on a free tier and monitor limits.
-   Scripture content: store the curated initial library locally.
-   Testing: use an existing Android phone and emulator.
-   Publishing: delay store registration until the prototype has been
    tested.
-   Cross-platform: postpone iOS and desktop protection engines until
    Android is credible.

A R0 prototype is possible if the required equipment and Claude Code
access are already available. A dependable commercial product will
eventually require spending on maintenance, testing, support, and
potentially hosting.

### Milestones

**M1 --- App shell:** Navigation, dashboard, settings, and local
Scripture library work.

**M2 --- Filtering proof of concept:** A test domain is blocked on a
physical Android device while ordinary browsing continues to work.

**M3 --- Reliability:** VPN interruptions and blocklist updates are
handled and reported accurately.

**M4 --- Accountability:** Partner linking and consented reports work
with separate test accounts.

**M5 --- Spiritual Support:** Topic selection, saved verses, and
optional reminders work offline.

**M6 --- Private beta:** Security, privacy, reliability, and
user-experience testing is completed.

**M7 --- Expansion decision:** Use beta results to decide what to
improve before investing in iOS and desktop.

------------------------------------------------------------------------

## Definition of Done for V1

V1 is ready for a limited beta when:

-   [ ] The app builds and runs on supported Android test devices.
-   [ ] DNS filtering blocks the agreed test domains.
-   [ ] Normal browsing and network transitions are tested.
-   [ ] The dashboard reflects observed protection status.
-   [ ] Interruption and reporting behaviour is tested.
-   [ ] Partner invitations and report permissions are secure.
-   [ ] The Scripture library works offline and remains private by
    default.
-   [ ] Account deletion and relationship revocation are documented and
    tested.
-   [ ] No unresolved critical security issue remains.
-   [ ] Known platform limits and bypass risks are clearly disclosed.
-   [ ] The beta has a support, feedback, and rollback process.

**Guiding rule:** Build the smallest reliable version first. A blocker
that works consistently, reports honestly, respects privacy, and
provides useful spiritual support is a stronger foundation than a large
feature list with unverified protection.
