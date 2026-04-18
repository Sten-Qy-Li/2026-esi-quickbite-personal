# Chat Archive - 2026-04-18 - Charlie-Lima-Alfa (`53af709`)

## Session Summary

This session continued from commit `53af709` (end of the Phase 2-6
build session) and responded to Golf-Papa-Tango's Phase 2-6
verification report
(`dev-docs/verification/phase-2-to-6-verification_Golf-Papa-Tango.md`).

The report's overall status was `FAIL` with two blockers:

1. **Restaurant Service startup** -- `FATAL: password authentication
   failed for user "restaurant_user"`, caused by a local Windows
   service (`postgresql-x64-18`) already bound to port `5432`. The
   Spring Boot app connected to the rival PostgreSQL rather than the
   Compose container.
2. **Menu Service startup** -- Hibernate schema validation rejected
   `menu_item.price_currency` because Flyway created it as `CHAR(3)`
   while the JPA mapping declared `String` with `length = 3` (which
   Hibernate validates against `VARCHAR(3)`).

Blocker 2 is a genuine repository bug. Blocker 1 is
machine-specific, but the repo could be hardened so future runs on
conflicted Windows boxes are not silently broken.

This session applied one code fix and four defensive
configuration/documentation updates, then produced this archive and
committed the lot.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`
- Today: 2026-04-18 (Saturday)
- Active branch: `dev`
- Parent commit: `53af709` -- "Build Phase 2-6: scaffolding, CRUD,
  validation, OpenAPI for Sierra-Lima services"
- Environment: Windows 11 + IntelliJ IDEA + Git Bash
- Java toolchain: Temurin 17, Maven 3.9.14, Docker Desktop

## Files Created or Updated During This Session

### Updated -- Repository code fix

- `services/menu-service/src/main/resources/db/migration/V1__init.sql`
  -- `price_currency` column type changed from `CHAR(3)` to
  `VARCHAR(3)` so Hibernate's `ddl-auto=validate` stops rejecting
  the `MenuItem.price.currency` mapping (`String` with `length = 3`,
  which Hibernate expects to land in `VARCHAR(3)`). Resolves Golf-
  Papa-Tango blocker 2.

### Updated -- Local-dev stack (port-conflict defence)

- `services/local-dev/docker-compose.yml` -- host ports
  parameterised. Was:
  ```yaml
  ports:
    - "5432:5432"    # restaurant-db
    - "5433:5432"    # menu-db
  ```
  Now:
  ```yaml
  ports:
    - "${RESTAURANT_DB_HOST_PORT:-5432}:5432"
    - "${MENU_DB_HOST_PORT:-5433}:5432"
  ```
- `services/local-dev/.env.example` -- added a `Host port overrides`
  section introducing `RESTAURANT_DB_HOST_PORT` and
  `MENU_DB_HOST_PORT`, with `5442` / `5443` recommended as
  collision-free defaults and a pointer to the matching
  `DB_URL` override for IntelliJ Run Configurations.
- `services/local-dev/runbook.md` -- inserted a "Host-port conflict?"
  callout in `§1 One-time setup` describing the Windows
  `postgresql-x64-*` scenario, the override env vars, and the JDBC
  URL that must match.
- `dev-docs/verification/phase-2-to-6.md` -- added a pre-flight
  port-check step (`netstat -ano | findstr :5432` and `:5433`) in
  `§3 Start the databases`, renumbered the subsequent steps (3 -> 4,
  4 -> 5), and expanded the `password authentication failed`
  entry in the troubleshooting table to cover the
  rival-PostgreSQL-on-5432 scenario with the override recipe.

### Created -- Session archive

- `dev-docs/agent-context/2026-04-18_chat-archive_Charlie-Lima-Alfa_53af709.md`
  -- this archive.

### Not staged (intentional)

- `dev-docs/verification/phase-2-to-6-verification_Golf-Papa-Tango.md`
  -- Golf-Papa-Tango's verification report. Read-only input to this
  session; not my artefact to commit. Left untracked for
  Sierra-Lima or Golf-Papa-Tango to decide whether to land it.
- `dev-docs/agent-context/2026-04-18_chat-archive_Golf-Papa-Tango_53af709.md`
  -- Golf-Papa-Tango's own chat archive. Same rationale.

## Verification After Patches

- `mvn -B compile` on `services/menu-service` -- `BUILD SUCCESS`
  after the `VARCHAR(3)` switch.
- `mvn -B compile` on `services/restaurant-service` -- unchanged,
  still green (no code touched there).
- `docker compose --env-file .env.local config` -- parses cleanly
  with parameterised host ports; defaults (`5432` / `5433`) unchanged
  for users without a port conflict.
- No migration content besides the column type was touched, so
  existing data volumes remain compatible; a fresh `docker compose
  down -v && up -d` is sufficient to re-apply `V1`.

## Open Questions Movement

No Open Questions changed status in this session. The fixes are
mechanical and do not renegotiate any decision.

## Suggested Next Steps (Phase 7 entry criteria unchanged)

1. Ask Golf-Papa-Tango (or any teammate) to re-run
   `dev-docs/verification/phase-2-to-6.md` end-to-end against the
   new HEAD. The port-check step should flag the conflict before
   startup; the `VARCHAR(3)` fix should let Menu Service come up
   cleanly.
2. Once verification is green, proceed to Phase 7 (User Service
   integration / authenticated endpoints) per
   `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.
3. If Sierra-Lima chooses to keep Golf-Papa-Tango's report in the
   repo (useful audit trail for CP#1), stage
   `dev-docs/verification/phase-2-to-6-verification_Golf-Papa-Tango.md`
   and the matching chat archive in a separate commit.

## Commit Landed With This Archive

Subject: **Fix price_currency schema mismatch and parameterize DB host ports**

Body covers both Golf-Papa-Tango findings: the `VARCHAR(3)` switch
for the real repo bug, and the Compose/env/runbook/verification
hardening for the Windows port-5432 conflict.
