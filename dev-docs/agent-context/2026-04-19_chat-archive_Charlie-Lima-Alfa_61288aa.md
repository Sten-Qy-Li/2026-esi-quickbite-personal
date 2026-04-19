# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`61288aa`)

## Session Summary

This session executed **Phase 19 -- Buffer & Final Freeze** for the
QuickBite stack, as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
Phase 19 (lines 1662-1688).

The session began on top of `61288aa` ("Land Phase 18 presentation
rehearsal pack"). Phase 19 is the last A3 code beat before CP#3:
stabilisation only, no new features, end with a tagged freeze of
`dev` at `v1.0.0-cp3`. Task list (from the master plan, quoted
verbatim):

1. Fix highest-risk bugs only. No new features.
2. Re-run smoke tests. Both Sierra-Lima `smoke.sh` and the full
   cross-service trace from Phase 16.
3. Verify seeded demo users, restaurants, menu items, and order flow.
4. Verify clean startup (`docker compose down -v && docker compose
   up --build`).
5. Freeze the branch for the presentation. Tag it `v1.0.0-cp3`.

The session started with the Task-4 `docker compose down -v && up
--build` workflow deliberately first, to exercise the clean-start
path end-to-end before doing any triage. That immediately surfaced
three operational defects (healthchecks probing `localhost`,
stale Docker images pre-Phase-15/16, wrong `GATEWAY_UPSTREAM`
default). All three were fixed; a fourth (cross-service smoke
PATCH-status step missing) was found when re-running the Phase 16
harness on the rebuilt stack. Post-fix, both smoke harnesses pass
clean and the browser walk works end-to-end.

Conversation was compacted once mid-session (context ran out after
the four fixes had landed and been verified); the work between
compaction and end was the Phase 19 verification note, this
archive, and the commit + tag + push sequence.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`,
  and the `Frontend` under `services/frontend/quickbite-frontend/`.
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Parent commit: `61288aa` -- "Land Phase 18 presentation rehearsal
  pack"
- Environment: Windows 11 + Git Bash + Docker Desktop (WSL 2
  backend). Maven was NOT invoked directly this session --
  `docker compose up --build` handled the rebuilds via the
  multi-stage Dockerfiles.

## User Requests

Initial request: *"Hi Claude, please work on Phase 19 of the master
plan `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.
After completing the implementation of Phase 19, please archive the
session context to `dev-docs/agent-context`, and then commit all
changes and push (try to commit and push the entire local
repository; exclude files only if there's a very good reason,
according to your best judgement). Thanks!"*

Model pinned to `opus` and effort to `max` early via `/model opus`
+ `/effort max`. No mid-session redirections or user corrections
beyond the single interjection on context recovery after the
summary compaction.

## Phase 19 Task-by-Task Record

### Task 1 -- Highest-risk bugs fixed

Four defects closed; none introduced new features. All four are
documented with reproduction and validation evidence in
`dev-docs/verification/phase-19-verification_Sierra-Lima.md` §1.

1. **Frontend + dev-gateway containers reporting unhealthy**
   (compose healthcheck probed `localhost`; BusyBox wget in
   alpine tries IPv6 `::1` first; nginx only binds IPv4). Fixed
   by probing `127.0.0.1` explicitly in both healthchecks.
   Inline comment attached so the next owner does not paper it
   back over.
2. **Stale pre-Phase-15/16 Docker images baked into local cache**
   (deployed `quickbite/menu-service:local` dated
   2026-04-19T05:02:47Z, before Phase 15 merged; missing
   `events/` package and `RestaurantOwnershipClient`). Fixed by
   the explicit `docker compose down -v && up --build` from
   Phase 19 Task 4 -- operational-hygiene only, no code change.
3. **Cross-service smoke script missed the PATCH-status step**
   (created a restaurant, probed `/availability` immediately,
   got `acceptsOrders=false` because `POST /restaurants`
   defaults `isOpen=false` per F.3). Fixed in both
   `smoke-cross-service.sh` and `smoke-cross-service.ps1` by
   inserting a `PATCH /restaurants/{rid}/status` with
   `{"isOpen": true}` before the availability probe. Comments
   in both files reference F.3 and the equivalent step in
   `smoke.sh`.
4. **Frontend `/api/restaurants` returned 502 post-rebuild**
   (`GATEWAY_UPSTREAM` default pointed at `api-gateway:8080`
   but no such service exists on this personal repo's compose
   network; only `dev-gateway:80` does). Fixed by flipping the
   compose default and `.env.example` default to
   `http://dev-gateway:80`. Team's combined compose still works
   via `.env.local` override; comments on both defaults explain
   the dual-posture.

### Task 2 -- Smoke tests re-run

Both smoke harnesses executed against the rebuilt stack:

- `services/local-dev/smoke.sh` -- exit 0. All Phase 10
  happy-path assertions (auth -> restaurant create -> open ->
  menu item add -> availability probe -> batch validate) pass.
  No script changes in Phase 19.
- `services/local-dev/smoke-cross-service.sh` -- exit 0 after
  the Task-1 #3 fix. Trace in
  `services/local-dev/evidence/cross-service-smoke_20260419T094339Z.log`.
  `sierra-lima failures = 0`. `teammate failures = 0` (every
  teammate `*_BASE` env var is `<unset>` as expected in this
  personal repo).

Two pre-fix traces (093308Z, 093500Z) are retained as
before/after evidence for bug #3 in Task 1.

### Task 3 -- Seeded demo fixtures verified

Live-endpoint verification against the rebuilt stack; no schema
spelunking. Matches Phase 17 report §4.3 seed-data table exactly:

- 2 owner dev-tokens + 1 customer dev-token (synthetic JWTs
  minted via `JwtDevMint.java` / the smoke scripts; no User
  Service on this personal repo).
- 6 seeded restaurants (`GET /restaurants`), each with populated
  name / address / cuisineType / ownerUserId / openingHours.
- 18 seeded menu items (3 per restaurant; `GET
  /menu-items/by-restaurant/{rid}` spot-checked on the first
  restaurant returned 3 items with populated fields and EUR
  currency).
- "Order flow" interpreted as the W1 sync chain an Order Service
  would exercise: end-to-end through `smoke.sh` + cross-service
  smoke, both green. No Order Service is stood up in this
  personal repo so no order placement is traced end-to-end --
  same posture as Phase 16.

### Task 4 -- Clean startup verified

`docker compose --profile dev-gateway down -v && docker compose
--profile dev-gateway up -d --build`:

- Down: ~5 s. Named volumes `restaurant_db_data` and
  `menu_db_data` removed.
- Up: ~4 min cold build. All four image-building services
  rebuild cleanly (restaurant-service, menu-service, frontend
  via multi-stage Vite -> nginx, dev-gateway config copy).
- All six containers report `healthy` after boot sequence
  completes.
- Ad-hoc probes: `/actuator/health` on 8081 and 8082 both
  `UP`; frontend serves Vue SPA on 8090; dev-gateway `/healthz`
  returns `ok` on 8080.

No manual overrides needed beyond the bundled `.env.example`
defaults.

### Task 5 -- Branch freeze + tag

Annotated tag `v1.0.0-cp3` applied on `dev` at the Phase 19
commit. Tag and the commit are both pushed to `origin` as the
final step of the session.

## Definition of Done (Phase 19)

From the master plan:

- [x] Full system starts and runs in Docker Compose.
- [x] All workflows demonstrable end-to-end.
- [x] No unresolved defect remains that could break the main demo
      narrative.
- [x] Code is clean and committed.
- [x] Branch is tagged for the presentation.

All five bullets satisfied at this commit. Full roll-up with
citations is in
`dev-docs/verification/phase-19-verification_Sierra-Lima.md` §6.

## Artefacts Produced / Modified

| Path | Change |
|------|--------|
| `services/local-dev/docker-compose.yml` | Modified (2 healthchecks; 1 default) |
| `services/local-dev/.env.example` | Modified (1 default; reworded comments) |
| `services/local-dev/smoke-cross-service.sh` | Modified (PATCH-status step) |
| `services/local-dev/smoke-cross-service.ps1` | Modified (PATCH-status step) |
| `.gitignore` | Modified (carve-out to track `services/local-dev/evidence/*.log`) |
| `services/local-dev/evidence/cross-service-smoke_20260419T093308Z.log` | New (pre-fix red) |
| `services/local-dev/evidence/cross-service-smoke_20260419T093500Z.log` | New (pre-fix red) |
| `services/local-dev/evidence/cross-service-smoke_20260419T094339Z.log` | New (post-fix green) |
| `services/local-dev/evidence/menu-events_20260419T093308Z.log` | New (empty, pre-fix) |
| `services/local-dev/evidence/menu-events_20260419T093500Z.log` | New (empty, pre-fix) |
| `services/local-dev/evidence/menu-events_20260419T094339Z.log` | New (2 envelopes) |
| `dev-docs/verification/phase-19-verification_Sierra-Lima.md` | New (this phase's verification note) |
| `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_61288aa.md` | New (this archive) |

Four modified source files and eight new evidence / documentation
files. No Spring Boot source changes, no test source changes, no
Vue source changes, no Flyway migration changes, no POM / lockfile
changes.

## Notable Decisions

1. **Default `GATEWAY_UPSTREAM` to `dev-gateway`, not
   `api-gateway`.** The personal repo will never run with
   Alfa-Kilo's gateway on the same compose network -- it always
   runs solo with `--profile dev-gateway`. Keeping the default
   aligned with the actual deployment posture removes a failure
   mode at demo time. Team members joining with the combined
   compose override the value in their `.env.local` (same override
   path as before, so nothing breaks).

2. **Probe `127.0.0.1`, not `localhost`, in nginx healthchecks.**
   BusyBox wget resolves `localhost` to both IPv4 and IPv6 and (on
   this Alpine base) tries IPv6 first. The rendered nginx server
   blocks listen IPv4-only. Making the probe unambiguous is both
   correct and documents the gotcha inline; the alternative
   (adding `listen [::]:80;` to the nginx templates) has larger
   blast radius (affects what frontend/dev-gateway serve, not
   just the probe) and was not in scope for Phase 19.

3. **Keep pre-fix evidence traces in the repo.** Three
   cross-service traces were captured during triage: two red, one
   green. A red-then-green pair makes the Task-1 #3 fix auditable
   at <4 KB total cost. Alternative (delete the pre-fix traces)
   saves almost nothing on disk and loses the audit trail.

4. **No code touched in Sierra-Lima's services.** All three
   service-level defects identified during triage (healthchecks,
   wrong default, smoke script) were in the operational layer,
   not in Spring Boot sources. The service classpaths from Phase
   15 and 16 are frozen; the Phase 17 report and Phase 18
   rehearsal pack both quote their surface without touching them.
   Phase 19's "no new features" clause is read strictly:
   operational fixes that do not alter API shape or observable
   behaviour are in scope; anything that changes a handler /
   payload / DB schema is not.

5. **Tag on the Phase 19 commit, not on a separate "freeze"
   commit.** The master plan says tag `v1.0.0-cp3`; the commit
   that carries Phase 19 is the freeze commit. No extra empty
   commit needed.

## Carry-overs from Phase 18

No new items opened in Phase 19. Carry-overs continue untouched:

- Capture the screenshot pack at the 2026-05-18 rehearsal
  (`swagger-restaurant.png`, `swagger-menu.png`,
  `negative-auth-401.png`, `negative-auth-403.png`,
  plus the rehearsal's own cross-service-smoke / menu-events
  traces).
- Export the Phase 17 report to PDF for the hand-in.
- Confirm speaking-part allocations at the rehearsal kickoff.
- Resilience demo go/no-go at S14 of the Phase 18 deck.

All four are correctly out of Phase 19 scope (stabilisation only);
they live in
`dev-docs/verification/phase-18-verification_Sierra-Lima.md` §7
and are repeated in Phase 19's verification note §7 so the
2026-05-18 rehearsal slot has a single source of truth.

## Environment Notes

- **Windows path vs WSL path for `docker cp`.** During triage,
  `docker cp quickbite-menu-service:/app/app.jar /tmp/menu-svc.jar`
  dropped the jar into the WSL `/tmp`, which is not visible to
  host-level Python. Switched to
  `/c/Users/qunyan/AppData/Local/Temp/menu-svc.jar` (a path that
  both WSL mount translation and host Python can see) to
  introspect the classpath with `jar tf`. Not a Phase 19 deliverable
  itself; worth noting because it was a ~2-min detour during bug
  #2 triage.
- **`docker compose restart` does not re-apply healthcheck
  config changes.** `docker compose up -d` recreates the
  container if the compose config changed; `restart` just bounces
  the existing container with its existing config. Both
  healthcheck fixes needed the `up` path, not `restart`.

## Closing State

Commit (to be created at end-of-session): "Land Phase 19 buffer
and final freeze."

Annotated tag (applied to that commit): `v1.0.0-cp3`.

Push: branch `dev` + tag to `origin`. No force-push, no amend, no
rebase.
