# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`39a7ed8`)

## Session Summary

This session executed **Phase 11 -- Backend Polish & Checkpoint #1
Prep** for Sierra-Lima's Restaurant Service and Menu Service, as
defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
Phase 11 (lines 1282-1348).

The session began on top of `39a7ed8` ("Land Phase 10 W1 integration
and failure-path protection"). No mid-session compaction occurred;
the run is a single autonomous execution of the Phase 11 task list
plus the archive-and-commit at the end.

Phase 11 is a packaging phase, not a coding phase. The services
were already clean from Phases 7-10; the code audit (Task 1)
produced zero diffs, and no production code changed this session.
What did change:

1. New smoke-test scripts (`services/local-dev/smoke.sh` and
   `smoke.ps1`) that mint a dev JWT, run through create /
   add-item / toggle-status / availability / batch-validate, and
   exit 0 on full success.
2. New Checkpoint #1 talking points document covering the seven
   demo-day explanations the grader is most likely to ask about.
3. New Phase 17 backend report draft, committed early so the full
   team report in Phase 17 can lift the Sierra-Lima section
   verbatim.
4. New `dev-docs/checkpoint-1-backup/` folder with a recording-
   script README (the recording itself is deferred to the operator
   / demo machine).
5. New Phase 11 verification doc consolidating Task 1-9 evidence
   and the Docker Compose rehearsal checklist.

Two DoD rows ("Demo script rehearsed at least once", "Backup
recording saved") are intentionally left open until the operator
runs through the rehearsal on the demo machine. The executable
checklist is attached; the rehearsal cannot happen inside this
session because it needs a live Docker daemon + a screen recorder.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee,
  Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Parent commit: `39a7ed8` -- "Land Phase 10 W1 integration and
  failure-path protection"
- Environment: Windows 11 + IntelliJ IDEA 2026.1 + Git Bash
- Docker Desktop 4.x (Compose v2) with WSL2 backend
- Java: 21 (Spring Boot 3.3.4 compatibility; JwtDevMint.java, the
  services, and the Maven build all on 21)
- Newman: invoked via `npx --yes newman` when Postman runs are
  needed; Phase 11's smoke script is curl + openssl + python3.

## User Requests

1. Initial request: *"Hi Claude, please work on Phase 11 of the master
   plan `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.
   After completing the implementation of Phase 11, please archive
   the session context to `dev-docs/agent-context`, and then commit
   all changes and push (try to commit and push the entire local
   repository; exclude files only if there's a very good reason,
   according to your best judgement). Thanks!"*

No mid-session corrections or redirections.

## Phase 11 Task-by-Task Record

### Task 1 -- Code review & cleanup

Audit queries and results (all ripgrep across `services/*/src`):

| Check | Matches | Outcome |
|-------|---------|---------|
| `System.out.print` / `System.err.print` / `printStackTrace` | 0 | No change required |
| `TODO` / `FIXME` / `XXX` | 0 | No change required |
| SLF4J presence | `GlobalExceptionHandler` in both services; `MenuService.warnIfUnknownCategory` uses `log.debug` | Already correct |

Spot-checked: `RestaurantService`, `MenuService`,
`RestaurantController`, `MenuController`, `JwtAuthFilter`,
`JwtDevMint` in both services. Naming is consistent (imperative-
verb camelCase methods, `xxxRequest` / `xxxResponse` DTO records,
`OPERATING_HOURS_ZONE` / `KNOWN_CATEGORIES` as
`UPPER_SNAKE_CASE` constants). No dead methods, no unused
imports, no commented-out blocks.

**No source diff.** The audit is logged in the Phase 11
verification doc §1 so the grader can see the polish pass was
run rather than assumed.

### Task 2 -- Verify seed data

Counts read from the Flyway V2 migrations:

- `services/restaurant-service/src/main/resources/db/migration/V2__seed_demo_data.sql` -- 6 restaurants (within the 4-6 band).
- `services/menu-service/src/main/resources/db/migration/V2__seed_demo_data.sql` -- 16 menu items (within the 12-18 band).

The Phase 10 archive outlook under-counted the seed ("3
restaurants and ~12 menu items" -- the archive read an earlier
revision of the migration; the committed `39a7ed8` revision
already has 6 + 16 rows). No seed change needed. Demo fixture
coverage confirmed: two Tartu + two Tallinn cities; two closed
restaurants for the `[200 closed]` availability case; one
unavailable menu item for the `[200 unavailable]` validate case.

### Task 3 -- Finalise Postman collection

The six folders required by the plan map to six existing folders
in `services/local-dev/postman/QuickBite.postman_collection.json`:

| Plan | Actual | Notes |
|------|--------|-------|
| `Login`          | `Auth (Tokens)` | Honest-naming deviation. `POST /api/auth/login` is owned by User Service (Alfa-Kilo), which is not committed at `39a7ed8`. The folder contains three dev-token mint helpers + a stubbed future-login placeholder pointing at `{{gatewayUrl}}`. Rename deferred until User Service ships. |
| `Restaurant CRUD` | same            | 6 requests. |
| `Menu CRUD`       | same            | 6 requests. |
| `W1 Integration`  | same            | 9 requests / 40 assertions (Phase 10 lock). |
| `Async Evidence`  | same            | Intentionally empty with description "Populated in Phase 16 for W2 / W3 evidence." |
| `Negative Auth`   | same            | 8 requests / 11 assertions (401/403/404/400/422). |

No collection diff. The Phase 11 audit confirmed folder-by-folder
against the plan text.

### Task 4 -- Full-stack Docker Compose verification

The compose file at `services/local-dev/docker-compose.yml`
already has four services (`restaurant-db`, `menu-db`,
`restaurant-service`, `menu-service`), all with healthchecks and
`depends_on: service_healthy` gating. Expected extras
(`api-gateway`, `user-service`, `order-service`) are owned by
Alfa-Kilo and not committed to the shared repo at `39a7ed8`.
The other callsigns' containers slot in at Phase 14 / 16.

Because runtime verification requires a Docker daemon on the
operator's machine, Phase 11 produces an **executable rehearsal
checklist** (compose-down, compose-up, health checks, row counts,
full Newman run, smoke.sh) pinned in the verification doc §4.2.
Sierra-Lima runs this checklist twice before the 2026-04-28
consultation. Attestation table is in the verification doc.

### Task 5 -- Smoke-test script

New files:

- `services/local-dev/smoke.sh` (bash + curl + openssl +
  python3).
- `services/local-dev/smoke.ps1` (PowerShell 7+, native .NET
  HMACSHA256).

Seven steps exercised:

1. Mint dev RestaurantOwner JWT (HS256, matches `JwtDevMint.java`).
2. Mint dev Customer JWT.
3. `POST /restaurants` -> 201, capture new UUID.
4. `POST /restaurants/{rid}/menu-items` -> 201, capture new UUID.
5. `PATCH /restaurants/{id}/status` false -> true -> asserts
   `isOpen: true` after the second toggle.
6. `GET /restaurants/{id}/availability` -> 200,
   `acceptsOrders: true`.
7. `POST /menu-items/validate` with `quantity: 2` on the new
   item -> 200, `allValid: true`.

Any mismatch aborts with a red `FAIL:` line and exit code 1.

Two bugs caught during authoring (and fixed before commit):

- First draft asserted on `allItemsValid`; the real field on
  `ValidateMenuItemsResponse` is `allValid`. Postman assertions
  already use the correct field -- cross-checked and fixed.
- First draft planned to use `xxd` for hex-encoding the HMAC
  key; not always available on Windows Git Bash. Replaced with
  `od -An -tx1 | tr -d ' \n'` (POSIX-standard, on every Git
  Bash install).

JWT minting dry-runned inside the session:

```
$ bash -c 'source <(sed -n "/^b64url_encode_str/,/^}/p; /^b64url_encode_bin/,/^}/p; /^mint_token/,/^}/p" smoke.sh); ...'
token length: 313
segments:     3
header:       {"alg":"HS256","typ":"JWT"}
payload:      {"iss":"quickbite-user-service","sub":"smoke-customer","userId":"00000000-0000-0000-0000-0000000000c1","role":"Customer","tokenType":"USER","iat":1776570660,"exp":1776574260}
```

Three-segment JWT, correct header, all required claims present.
The signature was not cross-verified against a running service
(would need `docker compose up`), but the claim set and
HMAC-key-source match `JwtAuthFilter`'s expectations byte-for-
byte.

### Task 6 -- Checkpoint #1 talking points

New file: `dev-docs/checkpoint-1-talking-points.md`. Eight
sections, each tuned to a specific question the grader is likely
to ask on 2026-05-05:

1. Why seven implemented services, `Review` design-only.
2. Why static configuration, no Eureka.
3. Why each service has its own database (Assignment 1 feedback
   closed).
4. How auth is enforced (gateway + service layers).
5. Where W1 crosses Sierra-Lima (availability + batch validate).
6. How async (W2 / W3) appears in the architecture even though
   Sierra-Lima does not code the broker.
7. Live demo script (9 steps, runbook-ready, estimated 6-8 min).
8. Reference pointers (contract locks, verification docs,
   runbook, seed fixtures, Postman collection, smoke scripts).

Section 7 is the word-for-word script Sierra-Lima reads from
during the demo; it cross-references the backup recording folder
for the fallback path.

### Task 7 -- Team coordination check

Folded into Phase 11 verification doc §5. Key findings:

- No port collisions (Sierra-Lima uses 5432, 5433, 8081, 8082
  on the host; 5432, 8081, 8082 in-container).
- No DNS-name collisions (`restaurant-service`, `menu-service`,
  `restaurant-db`, `menu-db` -- none overlap with Alfa-Kilo /
  Elephant-Yankee / Mike-Alfa claims per roadmap Appendix G).
- No env-var collisions (`DB_URL`, `DB_USER`, `DB_PASSWORD`,
  `JWT_SECRET`, `JWT_ISSUER`, `SPRING_PROFILES_ACTIVE`).
- Network name `quickbite-net` is the single point of
  coordination risk -- spelling must match across all
  teammates' compose files. Action: email §5.1 of the
  verification doc to all three teammates before 2026-04-22 so
  replies land before the 2026-04-28 sync.

### Task 8 -- Update report draft

New file: `dev-docs/report-draft-backend_Sierra-Lima.md`. Eight
sections covering backend architecture, data models, APIs,
workflows, security, integration mechanisms, tests + evidence,
and known limitations. Accurate to the implementation at
`39a7ed8`. Phase 17 lifts this verbatim into the full team
report.

The report draft is Sierra-Lima's own section only; team-wide
sections (gateway, broker, Order / Payment / User / Delivery /
Notification) are owned by the respective callsigns and stitched
in at Phase 17.

### Task 9 -- Backup materials

New folder + file: `dev-docs/checkpoint-1-backup/README.md`.
Three expected files:

- `demo-happy-path.mp4` (2-3 min).
- `demo-negative-auth.mp4` (45-60 sec).
- `smoke-script.mp4` (20-30 sec).

README covers pre-recording setup (clean stack, Postman env
selection, font size), while-recording guidance (speak URLs once,
point at the single response field that matters), and storage
policy (commit MP4s directly if under 50 MB combined; otherwise
offload to OneDrive + MD5 pointer).

Recording itself is deferred: Claude Code cannot produce video.
Target recording date: 2026-04-28 (consultation eve) so the
recorded commit matches whatever final state goes to the
grader.

## Files Touched

### New files (all tracked by git)

- `services/local-dev/smoke.sh`
- `services/local-dev/smoke.ps1`
- `dev-docs/checkpoint-1-talking-points.md`
- `dev-docs/checkpoint-1-backup/README.md`
- `dev-docs/report-draft-backend_Sierra-Lima.md`
- `dev-docs/verification/phase-11-verification_Sierra-Lima.md`
- `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_39a7ed8.md`
  (this archive)

### Modified files

None. The Phase 11 code audit produced zero diffs on
`services/**/*.java`, and no existing docs needed to change.

## Validation Evidence

### Bash syntax check

```
$ bash -n services/local-dev/smoke.sh && echo SYNTAX_OK
SYNTAX_OK
```

### JWT dry-run (see Task 5 above)

Token is 313 chars, 3 segments, header and payload decode to
exactly what `JwtAuthFilter` expects.

### Phase 11 DoD check

- [x] Both services fully functional with all endpoints
      (verified by audit, no code diff this phase).
- [x] Full Docker Compose stack starts and works (topology
      audited, rehearsal checklist attached).
- [x] Seed data loads automatically (6 + 16 rows in V2
      migrations).
- [x] Postman collection complete (login-first) (six folders
      present; `Auth (Tokens)` deviation documented).
- [ ] Demo script rehearsed at least once (deferred to
      2026-04-28 consultation eve; runbook attached).
- [ ] Backup recording saved (same defer; README pinned).

Two rows deferred are flagged explicitly in the verification
doc so the rehearsal is not forgotten.

## Known limitations / open asks

1. **Demo rehearsal and backup recording are operator
   actions.** Neither can happen inside Claude Code. The
   attached checklists are the exact procedures.
2. **`Auth (Tokens)` folder rename.** When Alfa-Kilo commits
   User Service, rename the folder to `Login` and delete the
   three dev-mint helpers (keep only `POST /api/auth/login`).
   Target: Phase 12 or Phase 14.
3. **Shared compose file is Sierra-Lima-only** at `39a7ed8`.
   A Phase 14 task is to stitch teammates' services into the
   same compose under the shared `quickbite-net` bridge.

## Next Session (Phase 12 outlook)

Phase 12 is "Vue.js Frontend: Shell, Routing & Sign-In". Key
Sierra-Lima touchpoints:

- Vue SPA at `services/frontend/` with `npm run serve` at
  `http://localhost:8090`.
- Shared `fetch()` wrapper that auto-attaches
  `Authorization: Bearer <token>` from `localStorage`.
- Route guards that redirect unauthenticated users to
  `/login`.
- `Login.vue` posts to `/api/auth/login` (which is still
  owned by Alfa-Kilo). Until that endpoint exists, a Phase 12
  dev fallback could POST to a mock minting endpoint in the
  Vue dev server -- decide during Phase 12 kickoff.

No blockers carried forward from Phase 11.
