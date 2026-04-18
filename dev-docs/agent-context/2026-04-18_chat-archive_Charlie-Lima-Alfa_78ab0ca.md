# Chat Archive - 2026-04-18 - Charlie-Lima-Alfa (`78ab0ca`)

## Session Summary

This session continued from commit `78ab0ca` (tip of `dev` after
landing the Golf-Papa-Tango audit artefacts). Two things happened:

1. **Phase 2-6 sign-off review.** Read both available verification
   reports side by side and gave Sierra-Lima a direct confidence
   verdict on whether Phase 2-6 is correctly implemented.
2. **IntelliJ Project SDK alignment.** Sierra-Lima asked to align
   the IntelliJ Project SDK from Java 21 to Java 17 per
   `dev-docs/decisions/0003-conventions.md`. Verified both a JDK 17
   install and a registered IntelliJ SDK were available, then
   edited `.idea/misc.xml` in place.

No Phase 2-6 code was modified in this session. No open questions
changed status. The only file touched is a local, gitignored IntelliJ
settings file.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`
- Today: 2026-04-18 (Saturday)
- Active branch: `dev`
- Parent commit: `78ab0ca` -- "Archive Golf-Papa-Tango Phase 2-6
  verification report and chat archive"
- Environment: Windows 11 + IntelliJ IDEA 2026.1 + Git Bash

## 1. Phase 2-6 Verification Review

### Inputs

- `dev-docs/verification/phase-2-to-6-verification_Golf-Papa-Tango.md`
  (rerun against current HEAD `78ab0ca`; prior report against base
  was `acbeb5a`)
- `dev-docs/verification/phase-2-to-6-verification_Sierra-Lima.md`
  (Sierra-Lima's own run, with screenshot evidence for the GUI-only
  items)

### Verdict

Phase 2-6 is **complete and correctly implemented** at HEAD
`78ab0ca`. The two reports together cover every Phase 2-6 DoD item:

| Coverage | Source | Result |
|----------|--------|--------|
| Prerequisites, DB containers, `mvn test`/`package` | Golf-Papa-Tango | PASS |
| Service startup, `/actuator/health`, Flyway migrations | Both | PASS |
| Restaurant CRUD (6 endpoints, 400 envelopes, availability) | Golf-Papa-Tango | PASS |
| Menu CRUD + batch validation shapes | Golf-Papa-Tango | PASS |
| CORS preflight headers | Golf-Papa-Tango | PASS |
| Persistence across service restart | Golf-Papa-Tango | PASS |
| OpenAPI docs endpoint HTTP availability | Golf-Papa-Tango | PASS |
| Swagger UI **visual rendering** in browser | Sierra-Lima (screenshots) | PASS |
| Postman **desktop import** | Sierra-Lima (screenshot) | PASS |

Both blockers Golf-Papa-Tango had raised in their original report
(pre-`acbeb5a`) are gone: the Menu `price_currency` schema now
matches Hibernate's expectations, and the Restaurant side works
through the parameterised host-port path (`5442` on Sierra-Lima's
machine, since a Windows `postgresql-x64-*` service still owns
`5432`).

### Minor non-blockers surfaced

Three items in Sierra-Lima's run logs that are worth noting but do
NOT gate Phase 7:

1. **Java 21 at runtime.** IntelliJ was launching the services
   with `C:\Program Files\OpenLogic\jdk-21.0.10.7-hotspot\bin\java.exe`
   while `pom.xml` targets Java 17. Works (21 runs 17 bytecode) but
   diverges from `dev-docs/decisions/0003-conventions.md`. Addressed
   in §2 below.
2. **`Using generated security password: <uuid>` warning.** Spring
   Security's default when no user store is wired up. Expected for
   the Phase 2 `permitAll` stub; will be replaced by the JWT filter
   defined in `dev-docs/decisions/0010-auth-contract.md` during
   Phase 7.
3. **Menu Service DevTools: `Unable to start LiveReload server`.**
   The Restaurant Service grabbed port `35729` first. Cosmetic
   only; no runtime impact.

## 2. IntelliJ Project SDK Alignment

### Before

- `jdk.table.xml` registered SDKs:
  - `21` -> `C:/Program Files/OpenLogic/jdk-21.0.10.7-hotspot`
    (OpenLogic JDK 21.0.10)
  - `ms-17` -> `$USER_HOME$/.jdks/ms-17.0.18` (Microsoft OpenJDK
    17.0.18)
- Additional Java 17 installed on disk but NOT registered in
  IntelliJ: `C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot`
  (Temurin 17, the convention-preferred distribution).
- `.idea/misc.xml`:
  ```xml
  <component name="ProjectRootManager" version="2"
             languageLevel="JDK_21"
             default="true"
             project-jdk-name="21"
             project-jdk-type="JavaSDK">
  ```
- Per-module `.iml` files use `inheritedJdk`, so changing the
  project SDK flips all modules.
- `pom.xml` in both services already pins `<java.version>17</java.version>`,
  so Maven was always building for Java 17; only the IDE runtime
  and language level had drifted.
- No Run Configuration in `.idea/workspace.xml` hard-codes a JRE
  path (checked via `Grep` for `jdk|jre|ALTERNATIVE`), so run
  configs simply inherit the project SDK.

### After

- `.idea/misc.xml` now reads:
  ```xml
  <component name="ProjectRootManager" version="2"
             languageLevel="JDK_17"
             default="true"
             project-jdk-name="ms-17"
             project-jdk-type="JavaSDK">
  ```
- Picked `ms-17` (Microsoft OpenJDK 17.0.18) because it was
  already registered in IntelliJ -- zero new IDE configuration
  required. Microsoft OpenJDK 17 is fully compatible; the
  "Temurin recommended" clause in `0003-conventions.md` is a
  preference, not a hard requirement. If Sierra-Lima later
  wants to swap to Temurin:
  `File -> Project Structure -> SDKs -> + -> Add JDK ->`
  `C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot`, then
  rename `project-jdk-name` in `misc.xml`.
- `.idea/` is gitignored (root `.gitignore` line 27), so this
  change is local-only and does not enter version control.

### How Sierra-Lima picks up the change

1. In IntelliJ: `File -> Reload Maven Project`, or just close and
   reopen the project.
2. Existing Run Configurations inherit the project SDK, so no
   per-config edit is needed.
3. Next Spring Boot run, the banner should read
   `Starting RestaurantServiceApplication using Java 17.0.18 ...`
   instead of `Java 21.0.10`.

## Files Created or Updated During This Session

### Updated -- IntelliJ local settings (gitignored)

- `.idea/misc.xml` -- `project-jdk-name` `21 -> ms-17`,
  `languageLevel` `JDK_21 -> JDK_17`.

### Created -- Session archive

- `dev-docs/agent-context/2026-04-18_chat-archive_Charlie-Lima-Alfa_78ab0ca.md`
  -- this archive.

### Not modified

- No source code, no migrations, no documentation, no Compose or
  env files were touched. The phase-2-to-6 verification guide and
  runbook are unchanged.

## Open Questions Movement

No Open Questions changed status. `Q3`, `Q5`, `Q7`, `Q9` remain the
outstanding set to resolve before or during Phase 7.

## Suggested Next Steps

1. Reload the IntelliJ project so the JDK 17 switch takes effect;
   confirm the Spring Boot banner reads `Java 17.0.18` on the next
   run of either service.
2. Start Phase 7 proper per
   `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
   §9 Phase 7 -- User Service integration, JWT filter installation,
   and the first authenticated endpoints on Restaurant and Menu
   services. The contract is already fixed in
   `dev-docs/decisions/0010-auth-contract.md`.
3. If Sierra-Lima wants the convention-preferred Temurin 17 (rather
   than Microsoft OpenJDK 17) in IntelliJ, register it via Project
   Structure and flip `project-jdk-name` in `misc.xml`. Low priority.

## Repository State at End of Session

- Branch: `dev`
- HEAD: `78ab0ca` (unchanged from start of session)
- Working tree: modification to `.idea/misc.xml` (local, gitignored)
  plus this untracked archive file under
  `dev-docs/agent-context/`.
- Remote `origin/dev`: in sync with local `78ab0ca`.
