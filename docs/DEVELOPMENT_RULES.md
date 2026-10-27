> **Note (added when saved, 2026-10-27):** the specification referred to below lives at [`docs/spec/identity_sdlc_stitch_handoff.md`](spec/identity_sdlc_stitch_handoff.md). The rules text below is unchanged from the product owner's original.

# Identity: Incremental Development Rules for Claude Code

## Purpose

This document defines how Claude Code should work on Identity. The goal is to build the application in small, understandable, testable steps rather than attempting to implement many features at once.

Use this document alongside `identity_sdlc_stitch_handoff.md`. The SDLC remains the primary product specification; this document governs the development workflow.

## 1. Work on One Thing at a Time

- Work on one clearly defined task or milestone at a time.
- Do not begin unrelated features while the current task is unfinished.
- Do not attempt to implement the entire application in one prompt.
- Before coding, state the exact task, why it matters, and what files or components are likely to change.
- Keep the scope small enough that the result can be reviewed and tested.
- If a task reveals a separate issue, record it in a follow-up list unless it blocks the current task or creates a security risk.

## 2. Inspect Before Changing

Before making changes:

1. Read the relevant sections of `identity_sdlc_stitch_handoff.md`.
2. Inspect the existing project structure and the code involved.
3. Identify what already works and avoid rebuilding it unnecessarily.
4. Check for existing conventions, dependencies, and tests.
5. Explain any important assumptions or missing information.

Do not assume that a feature is missing just because it has not been mentioned in the current conversation. Inspect the code first.

## 3. Use This Cycle for Every Task

Follow this cycle, in order:

1. **Define:** State one task and its acceptance criteria.
2. **Inspect:** Read the relevant code and understand its current behaviour.
3. **Plan:** Give a short implementation plan before making significant changes.
4. **Implement:** Make only the changes needed for the task.
5. **Verify:** Run appropriate builds, tests, static checks, or other available checks.
6. **Report:** Summarise what changed, which checks passed or failed, and any limitations.
7. **Pause:** Wait for my approval before moving to the next independent task.

Do not claim that something works unless you have performed an appropriate check. If a check cannot be run, explain why and tell me how I can verify it.

## 4. Keep Changes Small and Reviewable

- Prefer small, focused changes over large rewrites.
- Avoid changing unrelated files.
- Do not replace working code simply to match personal preferences.
- Do not add dependencies unless they are needed; explain their purpose and any relevant cost or maintenance implications.
- Do not delete, overwrite, or migrate important files or data without explaining the impact and obtaining approval.
- Preserve existing working functionality when adding a new feature.
- Keep names, structure, and implementation consistent with the existing project.

## 5. Explain the Work to Me

I am learning software development while building Identity. Be a development mentor as well as a coding assistant.

For each task, explain in plain language:

- What the task accomplishes.
- Why the chosen approach is appropriate.
- Which files were changed and what their roles are.
- What commands or actions were used to verify the result.
- What I should do next, if anything.

Avoid dumping large amounts of unexplained code. Give enough detail for me to understand the implementation without overwhelming me.

## 6. Testing Is Part of the Task

A task is not complete merely because code has been written.

Use the most relevant available verification, such as:

- Building the Android application.
- Running unit or instrumentation tests where available.
- Running static analysis or lint checks.
- Testing error, offline, permission-denied, and recovery states when relevant.
- Installing and testing an APK on a physical Android device when device behaviour matters.

Report the actual result of each check. Never fabricate test output or imply that a physical-device test was performed when it was not.

Because USB debugging may not currently be available, distinguish between checks performed on the development computer and checks that still need to be performed on the phone.

## 7. Security and Privacy Come First

- Follow the security and privacy requirements in the SDLC.
- Keep permissions limited to what the feature needs.
- Do not expose secrets, credentials, signing keys, or backend service-role keys in the Android app or repository.
- Do not collect or retain detailed browsing history unless a clearly justified requirement exists.
- Keep private reflections private by default.
- Make accountability sharing explicit and consent-based.
- Treat authentication, authorisation, data access, and filtering failures as important, not as optional polish.
- Do not promise that Android protections are impossible to bypass.
- If a proposed change creates a security or privacy concern, stop and explain the concern before implementing it.

## 8. Keep Project Documentation Accurate

- Treat `identity_sdlc_stitch_handoff.md` as the main product specification.
- Update relevant documentation when a meaningful architectural or behavioural decision is made.
- Do not silently change the specification.
- If the code and documentation disagree, identify the difference and ask for clarification when the intended behaviour is not clear.
- Keep a concise list of unfinished tasks, discovered issues, and decisions that need my input.

## 9. Git and Safe Progress

When Git is available:

- Check the current branch and working-tree status before making changes.
- Do not overwrite my existing uncommitted work.
- Make focused commits at sensible, working milestones when I approve or request them.
- Never commit secrets, credentials, local machine configuration, or generated sensitive data.
- If the working tree contains changes I did not ask you to make, inspect and preserve them.

## 10. When to Stop and Ask Me

Pause and ask before proceeding if:

- The SDLC does not define an important product decision.
- Two requirements conflict.
- A task requires a paid service or could create unexpected costs.
- A change could delete data, break an existing feature, or introduce a significant security risk.
- You cannot verify an important assumption.
- The next step would substantially expand the agreed scope.

Do not ask me to decide every small technical detail. Make reasonable, secure, maintainable choices when they fit the specification, and explain them.

## 11. Required Response Format

At the start of a task, provide:

- **Task:** One clearly scoped task.
- **Reason:** Why it is the right next step.
- **Acceptance criteria:** How we will know it is complete.
- **Likely files affected:** Based on inspection, where possible.

At the end of a task, provide:

- **Implemented:** What changed.
- **Files changed:** The relevant paths and their purpose.
- **Verification:** Checks performed and their real results.
- **Limitations or risks:** Anything not verified or still unresolved.
- **Next step:** Suggest one next task, but do not start it until I approve.

## 12. Initial Instruction

Read this document and `identity_sdlc_stitch_handoff.md`. Inspect the current Identity project and report:

1. What is already implemented.
2. What currently builds or has been tested, based only on verifiable evidence.
3. The single most valuable next task.
4. Its acceptance criteria and the relevant files.

Do not change any files during this initial assessment. Wait for my approval before implementing the proposed task.
