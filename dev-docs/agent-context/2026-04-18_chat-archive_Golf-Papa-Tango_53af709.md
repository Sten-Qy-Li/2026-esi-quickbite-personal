# Chat Archive - 2026-04-18 - Golf-Papa-Tango (`53af709`)

## Session Summary

This chat covered three connected tasks in the QuickBite early-start repository:

1. Review `dev-docs/roadmaps/Charlie-Lima-Alfa_aac68b0_project-phases.md` and integrate its strongest planning advantages into `dev-docs/roadmaps/Golf-Papa-Tango_aac68b0_project-phases.md`.
2. Read `dev-docs/verification/phase-2-to-6.md`, determine what could be executed fully from the terminal, and identify any parts that would still require user involvement.
3. Run the Phase 2 to Phase 6 verification end-to-end from the terminal, write a separate Markdown verification report with the `Golf-Papa-Tango` call-sign, then move that report to its final top-level verification path.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Agent call-sign for this session: `Golf-Papa-Tango`
- Project context:
  - ESI 2026 QuickBite group project
  - Sierra-Lima is preparing work early in a personal repository before wider team integration
- Working preference established during this chat:
  - use the efficient terminal-based route when possible
  - provide a separate Markdown verification report inside `dev-docs/verification`
  - if anything cannot be verified from the terminal side, include simple user-side verification steps

## Roadmap Review and Integration Work

The roadmap review concluded that `Charlie-Lima-Alfa_aac68b0_project-phases.md` was stronger than the current Golf draft in several planning dimensions:

1. clearer source-of-truth and artifact anchoring
2. better milestone and grading alignment
3. more explicit non-negotiable constraints
4. clearer ownership and auth posture distinctions
5. a more actionable early bootstrap phase
6. stronger risk, rehearsal, and demo-readiness framing

Those strengths were merged into:

- `dev-docs/roadmaps/Golf-Papa-Tango_aac68b0_project-phases.md`

The Golf roadmap was kept as the primary planning document, but it now contains the stronger structure and guardrails imported from Charlie where that improved execution quality.

## Verification Scope Assessment

After reading `dev-docs/verification/phase-2-to-6.md`, the chat established that terminal-based verification could cover most of the practical checks:

- prerequisites
- Docker Compose database startup
- Maven build and test runs
- service startup attempts
- health endpoint checks
- OpenAPI endpoint availability checks
- CRUD and validation checks
- Flyway and schema checks
- CORS checks

The only items identified as inherently user-side or GUI-bound were:

- IntelliJ-specific trust/setup prompts
- Swagger UI visual rendering in a browser
- Postman desktop import and visual execution
- any first-run desktop prompts from Docker Desktop, IntelliJ, or Postman

## Phase 2 to Phase 6 Verification Work Performed

The verification run was executed from the terminal rather than through IntelliJ or Postman GUI flows.

The work completed during the run included:

- checking Java, Maven, Docker, project folders, and verification assets
- creating local environment configuration from the provided examples where required
- starting the local PostgreSQL containers
- running Maven tests and package builds for both services
- attempting to start `restaurant-service`
- attempting to start `menu-service`
- checking database state directly after startup failures
- inspecting controller coverage and exposed endpoints statically where runtime checks were blocked
- writing a standalone verification report

## Verification Result

Overall verification result: `FAIL`

The infrastructure and build layer passed far enough to begin runtime verification, but runtime validation was blocked by two service startup failures.

### Passed or confirmed

- Java 17 available
- Maven 3.9 available
- Docker available
- local service directories present
- Postman/verification assets present
- Docker database containers started successfully
- `mvn test` passed in both services
- `mvn package` passed in both services

### Blocking failures

#### 1. `restaurant-service` startup failure

`restaurant-service` failed to authenticate against PostgreSQL on `localhost:5432`.

Machine-specific context found during the investigation:

- a local Windows PostgreSQL service named `postgresql-x64-18` was already running
- a local `postgres` process was listening on port `5432`

This created a local-environment conflict for the verification run and prevented successful Restaurant Service startup and downstream runtime checks.

#### 2. `menu-service` startup failure

`menu-service` ran Flyway successfully, but then failed Hibernate schema validation.

The mismatch identified was:

- migration file: `services/menu-service/src/main/resources/db/migration/V1__init.sql`
  - defines `price_currency CHAR(3)`
- entity mapping: `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/domain/Price.java`
  - maps `price_currency` as a Java `String` with length `3`
  - Hibernate expected `varchar(3)`

This schema mismatch blocked successful Menu Service startup and therefore blocked runtime API verification for that service.

### Database state observed after failures

- `restaurant_db` had no relations, indicating the Restaurant Service never reached migration/application readiness
- `menu_db` contained `flyway_schema_history` and `menu_item`
- `menu_item.price_currency` existed as `character(3)`, consistent with the Flyway migration and inconsistent with Hibernate's expectation during validation

## Verification Report Artifact

The verification report was first created in a dedicated subdirectory, then moved per the user's follow-up request.

Final report path:

- `dev-docs/verification/phase-2-to-6-verification_Golf-Papa-Tango.md`

The temporary evidence/log folder created during the run was retained:

- `dev-docs/verification/.tmp-phase-2-to-6-golf-papa-tango/`

The intermediate report subdirectory:

- `dev-docs/verification/phase-2-to-6-golf-papa-tango/`

was removed after the report was moved to its final location.

## Files Created or Updated During This Session

### Updated

- `dev-docs/roadmaps/Golf-Papa-Tango_aac68b0_project-phases.md`

### Created

- `dev-docs/verification/phase-2-to-6-verification_Golf-Papa-Tango.md`
- `dev-docs/agent-context/2026-04-18_chat-archive_Golf-Papa-Tango_53af709.md`

### Generated and retained as evidence

- `dev-docs/verification/.tmp-phase-2-to-6-golf-papa-tango/`

## What Could Not Be Fully Verified From The Terminal

The following items were not fully validated from the agent side because they are GUI-bound or visually oriented:

- Swagger UI rendering in the browser
- Postman desktop import success and manual collection execution
- IntelliJ-specific project trust and SDK/run configuration screens

The verification report already includes simple user-side instructions for these items where needed.

## Recommended Next Steps For A Future Agent

1. Resolve the local PostgreSQL `5432` conflict or adjust the Restaurant Service local database targeting so it reaches a healthy startup state.
2. Align the `menu-service` schema definition and Hibernate expectation for `price_currency` so startup succeeds.
3. Rerun the Phase 2 to Phase 6 verification after those two blockers are fixed.
4. If the user wants the verification report upgraded from `FAIL` to a complete pass/fail matrix, continue from the existing report and temp evidence folder rather than starting from scratch.

## Workspace Notes

- The work in this session was documentation and verification focused; no destructive git operations were used.
- The verification report now lives at the top level of `dev-docs/verification`, matching the user's requested final location.
- Existing unrelated files were left untouched.
