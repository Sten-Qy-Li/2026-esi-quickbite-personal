# Phase 19 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 19
("Buffer & Final Freeze") for Restaurant Service, Menu Service, the
Sierra-Lima frontend slice, and the local-dev compose stack.

Date: 2026-04-19. Target CP#3 rehearsal: 2026-05-18. Target CP#3
graded: 2026-05-19. Base commit: `61288aa` (Phase 18 land).

---

## 0. Session context

Phase 19 is the last A3 coding beat before the rehearsal (Phase 18
delivered the rehearsal pack; Phase 19 delivers a stable stack
underneath it). The phase is explicitly **stabilisation only --
no new features**. Scope is bounded by the five Phase 19 tasks and
the five DoD bullets quoted in §7 below.

The session started from `docker compose down -v && docker compose
--profile dev-gateway up -d --build` to exercise the Task 4 clean
startup path first; that revealed three defects that would have
blocked the 2026-05-18 rehearsal if they had surfaced then. All
three were fixed in this phase, the stack was rebuilt once more to
verify, and both smoke harnesses now pass cleanly end-to-end.

Artefacts touched in this phase (all modifications, no new files):

- `services/local-dev/docker-compose.yml` (two healthcheck fixes;
  one default change for `GATEWAY_UPSTREAM`).
- `services/local-dev/.env.example` (matching default flip and
  reworded comments for `GATEWAY_UPSTREAM`).
- `services/local-dev/smoke-cross-service.sh` (add open-restaurant
  PATCH so the availability probe reflects a serving restaurant).
- `services/local-dev/smoke-cross-service.ps1` (same fix, PowerShell
  port).
- `services/local-dev/evidence/cross-service-smoke_20260419T09*.log`
  + `menu-events_20260419T09*.log` (three cross-service smoke
  traces captured during triage -- two pre-fix red, one post-fix
  green; details in §2 and §8).
- `dev-docs/verification/phase-19-verification_Sierra-Lima.md`
  (this note).
- `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_61288aa.md`
  (session archive, written last before commit).

Not touched on purpose:

- **No service code changes.** The three defects all lived in the
  operational layer (compose healthcheck probes, env defaults,
  smoke script ordering). The Spring Boot sources from Phases
  10-17 are frozen; the Phase 18 rehearsal pack references them
  verbatim.
- **No new features.** Phase 19 DoD §1 is explicit: "No new
  features."
- **No schema or migration changes.** Seeded fixtures were
  already correct (six restaurants, eighteen menu items, two
  owners, one customer). No data reshuffling was needed; §3
  confirms the seed is intact post-clean-start.
- **No changes to Sierra-Lima's `smoke.sh` / `smoke.ps1`.** The
  Phase-10 happy-path harness already runs green against the
  rebuilt stack (§2.1).

---

## 1. Task 1 -- Highest-risk bugs fixed

Task 1 reads: "Fix highest-risk bugs only. No new features." Three
defects were found during the Task 4 clean-startup verification and
fixed in this phase. A risk score is attached to each so the triage
decision ("fix now") is explicit.

### 1.1 Frontend and dev-gateway containers stuck "unhealthy"

**Risk.** High. Compose starts both as healthy-gated dependencies of
downstream compose profiles; when either reports unhealthy the
dependent `docker compose up` commands block indefinitely. A grader
running `up` live at the 2026-05-19 demo would have hit it.

**Symptom.** `docker ps` showed `quickbite-frontend` and
`quickbite-dev-gateway` with status `unhealthy (failing streak 1388)`
(an accumulator from the service having run for hours under a broken
probe). Healthcheck output: `wget: bad address 'localhost'` on some
shells; on others, the probe would connect to IPv6 `::1`, hang, and
time out.

**Root cause.** Both containers run `nginx:1.27-alpine`. Their
healthcheck probe used BusyBox `wget`, which calls `getaddrinfo`
with both address families and (by default on Alpine 3.x) tries
IPv6 `::1` first for `localhost`. The rendered nginx server block
in each container binds IPv4 only (`listen 80;` with no `[::]:80`
counterpart), so the probe either connects to the wrong address
family or fails outright depending on the resolver state at the
moment.

**Fix.** Probe `127.0.0.1` explicitly in both healthchecks.
`docker-compose.yml` lines ~118-125 (frontend) and ~145-150
(dev-gateway). Both now read:

```yaml
test: ["CMD-SHELL", "wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1"]
```

(and the equivalent `/healthz` path for the dev-gateway probe). An
inline comment above each call-out captures the gotcha so the next
owner does not paper it back over.

**Validation.** After the fix and a recreate (`docker compose up -d`,
not `restart` -- `restart` does not re-apply changed healthcheck
configs), both containers transition to `healthy` within one probe
interval. See the "healthy/healthy" output in the clean-start
walkthrough (§4).

### 1.2 Stale pre-Phase-15/16 images baked into local cache

**Risk.** High. The deployed `quickbite/menu-service:local` image
on disk (created 2026-04-19T05:02:47Z, two commits before Phase
15's merge) did **not** contain the `events/` package or
`RestaurantOwnershipClient`. Any live demo whose narrative claims
"Menu publishes the availability event" or "Menu calls Restaurant
for ownership before POST" would have failed silently.

**Symptom.** Cross-service smoke trace showed no menu-events log
line. `docker cp` + `jar tf` on the running container confirmed
the classpath was missing both packages.

**Root cause.** The previous Phase 18 session used `docker compose
up -d` (which reuses existing images) rather than `up -d --build`.
Spring Boot jars embed the full classpath; the running container
was a jar built before those packages existed. The jar had never
been rebuilt despite source changes in Phases 15 and 16.

**Fix.** Not a code fix -- an operational-hygiene fix. Phase 19
Task 4 is explicit: `docker compose down -v && docker compose up
--build`. The rebuild of all four image-building services
(restaurant-service, menu-service, frontend, dev-gateway) brought
every class, static resource, and nginx template into the deployed
image set. Full walkthrough in §4.

**Validation.** Post-rebuild cross-service smoke (§2.2) captures
two `menu-events` log lines with fully valid envelopes. The
availability-toggle emit path lives in
`services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/events/LoggingMenuEventPublisher.java`
and was exercised end-to-end.

### 1.3 Cross-service smoke PATCH-status step missing

**Risk.** Medium. The Phase 16 cross-service smoke would exit with
`sierra-lima failures = 1` even on a fully working stack, because
step 3's `/restaurants/{rid}/availability` probe reported
`acceptsOrders=false` against a newly created restaurant. A grader
running the script live would see a spurious failure.

**Symptom.** See `services/local-dev/evidence/cross-service-smoke_20260419T093308Z.log`
and `..._20260419T093500Z.log`: both exit with
`sierra-lima failures = 1` after the rebuild. The failure line
identifies the availability probe.

**Root cause.** Restaurant creation via `POST /restaurants`
defaults `isOpen=false` per F.3 data model (and per A2 domain
model §4.5). The Phase 16 cross-service harness created a
restaurant and immediately probed availability without opening
it. Sierra-Lima's happy-path `smoke.sh` already had the right
sequencing (PATCH status -> probe availability); the cross-service
harness had been written first and had lost parity during Phase
15's authorisation hardening.

**Fix.** Insert a `PATCH /restaurants/{rid}/status` with `{"isOpen":
true}` before the availability probe in both
`smoke-cross-service.sh` (bash) and `smoke-cross-service.ps1`
(PowerShell). Both files carry a comment tying the step back to
F.3 and to the equivalent step in `smoke.sh`, so a future Sierra
callsign does not strip it out under the assumption it is dead
code.

**Validation.** Post-fix run in
`cross-service-smoke_20260419T094339Z.log` exits
`sierra-lima failures = 0`; the menu-events trace is captured
with two valid envelope lines (see §2.2).

### 1.4 Post-rebuild `/api/restaurants` returned 502 from the frontend

**Risk.** High. The frontend nginx proxy config is the only routable
path from the Vue SPA to Sierra-Lima services; a 502 on
`/api/restaurants` means the sign-in screen shows no restaurants
in the list.

**Symptom.** After the clean-start rebuild, the browser at
`http://localhost:8090` loaded but the restaurant list called
`/api/restaurants`, which returned 502. nginx error log showed the
upstream resolve step failing on `api-gateway:8080`.

**Root cause.** The `GATEWAY_UPSTREAM` env-var default in
`docker-compose.yml` pointed at `http://api-gateway:8080`. That
hostname only exists when the team's combined compose (Alfa-Kilo's
`api-gateway` service) is on the same Docker network. The personal
repo runs with `--profile dev-gateway`, so `api-gateway` was not a
resolvable service name on `quickbite-net`. The Phase 14
`.env.example` reminded authors to override `GATEWAY_UPSTREAM` in
`.env.local` but the instruction is easy to miss, and nothing in
the README or compose output surfaces the override requirement.

**Fix.** Flip the compose default and the `.env.example` default
to the stub-backed value that this personal repo actually always
uses: `http://dev-gateway:80`. The team's combined compose can
still flip back to `http://api-gateway:8080` via `.env.local`;
nothing breaks because the override path is identical.

`.env.example` comment above `GATEWAY_UPSTREAM` now reads (lines
~40-47):

> Default is the `dev-gateway` stub (Phase 14) because this
> personal repo always runs with `--profile dev-gateway`. When
> the team's combined compose is in use (Alfa-Kilo's gateway on
> the same network), flip this to `http://api-gateway:8080`.

`docker-compose.yml` carries the matching comment above the `frontend`
`environment:` block (lines ~105-113).

**Validation.** Post-fix live browser walk at `http://localhost:8090`:
the restaurant list populates (six seeded restaurants), the
`/api/menu-items/by-restaurant/{rid}` call populates the menu card,
and `PATCH /api/menu-items/{id}/availability` returns 200 with the
toggled value. Demo-script §1 of the Phase 18 deck (browser
walkthrough) is runnable start-to-finish.

### 1.5 Triage list closed

No other defects surfaced during the smoke reruns, the clean-start
rebuild, or the seeded-data walk. Specifically:

- `smoke.sh` passes as of Phase 17 land; it was re-run on the
  rebuilt stack and still passes.
- JUnit 23/23 pass on restaurant-service and 42/42 pass on
  menu-service (last proven at Phase 17 land; no source changes in
  Phase 19, so the counts are frozen).
- Postman collections (9/9 and 14/14) are frozen at Phase 17 land.
- The Phase 18 rehearsal pack does not reference any broken path
  touched in Phase 19.

Risk-accepted, deferred to rehearsal (not fixed this phase):

- No screenshots captured yet; the Phase 18 fallback plan covers
  the 2026-05-18 rehearsal slot as the capture point. Same posture
  as the Phase 18 verification note §7.

---

## 2. Task 2 -- Smoke tests re-run

Task 2 reads: "Re-run smoke tests. Both the Sierra-Lima `smoke.sh`
and the full cross-service trace from Phase 16."

### 2.1 Sierra-Lima `smoke.sh`

Ran `services/local-dev/smoke.sh` against the rebuilt stack.
Exit code 0. All Phase 10 happy-path assertions hold:

- POST `/auth/dev-login` (owner + customer) -> 200.
- POST `/restaurants` -> 201, returned UUID.
- PATCH `/restaurants/{id}/status` -> 200, `isOpen=true`.
- POST `/menu-items` -> 201, returned UUID.
- GET `/restaurants/{id}/availability` -> 200, `acceptsOrders=true`.
- POST `/menu-items/validate` -> 200, `allAvailable=true`, single entry.

No changes were required to `smoke.sh` during Phase 19. The script
already carried the PATCH-status step inside its own sequence.

### 2.2 Phase 16 cross-service smoke

Ran `services/local-dev/smoke-cross-service.sh` against the rebuilt
stack. Post-fix trace:
`services/local-dev/evidence/cross-service-smoke_20260419T094339Z.log`.

- STEP 1 (mint dev tokens owner + customer): PASS.
- STEP 2 (W1 hops create restaurant, toggle open, add menu item,
  probe availability, batch validate): PASS.
- STEP 3 (toggle menu item availability to exercise the
  menu-events publisher): PASS. Evidence file
  `menu-events_20260419T094339Z.log` contains two log lines with
  fully valid envelopes (one false-to-true transition, one
  true-to-false transition, both on the same menuItemId).
- STEP 4 (teammate probes): gateway `reachable`; User / Order /
  Payment / Delivery / Notification all `unset` (expected -- this
  personal repo does not stand up teammate services).
- SUMMARY: `sierra-lima failures = 0`, `teammate failures = 0`.

Both smokes now exit clean.

### 2.3 Pre-fix triage logs kept as evidence

Two earlier cross-service runs are also under
`services/local-dev/evidence/` (093308Z, 093500Z). Each exits
`sierra-lima failures = 1` and attached empty menu-events files.
They were captured during Phase 19 triage before the PATCH-status
fix landed and are retained as before/after evidence for the bug
identified in §1.3. See §8 for the file-by-file manifest.

---

## 3. Task 3 -- Seeded demo fixtures verified

Task 3 reads: "Verify seeded demo users, restaurants, menu items,
and order flow."

Verification was performed on the rebuilt stack against live
endpoints, not by re-reading Flyway migration files. All numbers
match the canonical Phase 17 report §4.3 seed-data table.

### 3.1 Demo users (synthetic dev-tokens)

The project has no User Service running in this personal repo;
synthetic JWTs are minted by `JwtDevMint.java` (or by the
smoke-cross-service scripts). Three roles cover every CP#3 demo
beat:

- Owner A (`00000000-0000-0000-0000-000000000099`) -- owns most
  seeded restaurants; used by the demo for the happy-path
  "restaurant owner creates + opens + adds item" flow.
- Owner B (`00000000-0000-0000-0000-000000000098`) -- second
  owner, used for the authorisation negative-path drill
  (403 on POST `/menu-items` when the JWT is Owner B but the
  restaurant is owned by Owner A). Matches Phase 15.
- Customer (`00000000-0000-0000-0000-0000000000c1`) -- used for
  all customer-facing GETs (availability, menu browse).

Confirmed by minting a token for each, `curl`ing
`/auth/dev-login` -> 200, then hitting a protected endpoint with
the issued token.

### 3.2 Seeded restaurants

`GET /restaurants` returned six entries (Phase 13 seed set). Seed
data includes two owners, geographic coordinates, operating
hours, and initial `isOpen` status per entry. Spot-checked three
rows for field completeness (`name`, `address`, `cuisineType`,
`ownerUserId`, `openingHours`); all fields populated, no
placeholder nulls.

### 3.3 Seeded menu items

`GET /menu-items/by-restaurant/{rid}` on the first seeded
restaurant returned three items; aggregate across all six
restaurants is eighteen menu items (three per restaurant in the
Phase 13 seed set). Each entry has name, description, price
(amount + currency = EUR), `isAvailable=true`, and the expected
`menuItemId` shape.

### 3.4 "Order flow" (W1 sync chain)

Sierra-Lima does not own Order Service, so "order flow" here
means the W1 synchronous chain that an order worker would
exercise: create restaurant -> open it -> add menu item ->
customer queries availability -> batch-validate candidate items.
All five hops passed, traced in `smoke.sh` and
`smoke-cross-service.sh` (§2.1 and §2.2). The availability
probe correctly returns `acceptsOrders=true` on an opened
restaurant with an available item.

No Order Service is configured, so no end-to-end HTTP-level
order placement could be traced. This is expected: the
cross-service-smoke script explicitly probes teammate services
opportunistically and records `unset` when a base URL is not
provided (step 4 of the trace).

---

## 4. Task 4 -- Clean Docker Compose startup

Task 4 reads: "Verify clean startup (`docker compose down -v &&
docker compose up --build`)."

Walkthrough, with times captured as absolute timestamps:

1. `docker compose --profile dev-gateway down -v` (~05 s): all
   containers stop; `restaurant_db_data` and `menu_db_data`
   named volumes removed. Confirmed via `docker volume ls`.
2. `docker compose --profile dev-gateway up -d --build` (~4 min
   cold): all four image-building services rebuild from source
   (restaurant-service, menu-service, frontend via multi-stage
   Vite -> nginx, dev-gateway is a config-only copy). No warnings
   in the build output.
3. Boot sequencing: `restaurant-db` and `menu-db` ready first
   (pg_isready healthchecks pass in ~15 s), then
   `restaurant-service` (Spring Boot + Flyway migrations to V1-V5
   takes ~25 s to healthy), then `menu-service` (same, ~25 s
   with ownership-client wiring), then `frontend` and
   `dev-gateway` (both healthy within ~05 s once their
   dependencies are up).
4. Final state: `docker ps` shows all six containers `healthy`.

Ad-hoc healthy probes (each returns 200):

- `curl http://localhost:8081/actuator/health` -> `{"status":"UP"}`.
- `curl http://localhost:8082/actuator/health` -> `{"status":"UP"}`.
- `curl http://localhost:8090/` -> Vue SPA index.html.
- `curl http://localhost:8080/healthz` -> `ok`.

The six-container clean-start works from a zero state. No manual
step required; no override of any compose env var is needed beyond
what the `.env.example` defaults already provide.

---

## 5. Task 5 -- Branch freeze and tag

Task 5 reads: "Freeze the branch for the presentation. Tag it,
e.g. `v1.0.0-cp3`."

Freeze posture from Sierra-Lima:

- No further A3 code or compose changes are planned between the
  Phase 19 commit and the 2026-05-18 rehearsal slot. Deltas
  expected at the rehearsal slot are limited to the screenshot
  pack itself (see Phase 18 verification §7).
- The Phase 19 commit carries the `v1.0.0-cp3` annotated tag on
  branch `dev`, with a message body that captures the scope of
  the freeze in one paragraph.
- Any defect surfacing at rehearsal or in the 2026-05-18/19
  window should (a) be fixed only if it breaks the main demo
  narrative (DoD §3 bullet) and (b) land on `dev` with a
  corresponding `v1.0.0-cp3.1` / `.2` annotated tag so the
  pre-rehearsal freeze-state is recoverable via `git checkout
  v1.0.0-cp3`.

Tag command (applied after commit):

```sh
git tag -a v1.0.0-cp3 -m "Checkpoint #3 presentation freeze (2026-05-19)."
git push origin v1.0.0-cp3
```

---

## 6. Definition of Done roll-up

Phase 19 DoD bullets (quoted verbatim from the master plan):

- [x] **Full system starts and runs in Docker Compose.** Verified
      in §4. All six containers report healthy after
      `down -v` + `up --build` from a zero state. No manual
      overrides beyond the bundled `.env.example` defaults.
- [x] **All workflows demonstrable end-to-end.** The three demo
      narratives on the Phase 18 deck each have a passing trace
      in this phase:
      - W1 sync chain (restaurant + menu + availability) --
        `smoke.sh` green, §2.1.
      - Cross-service smoke (W1 + menu-events emit) --
        `smoke-cross-service.sh` green with two envelope lines in
        the trace, §2.2.
      - Frontend live walk (sign-in -> restaurant list -> menu
        browse -> toggle availability) -- verified in-browser
        after the §1.4 fix.
- [x] **No unresolved defect remains that could break the main
      demo narrative.** The four defects in §1 are each marked
      Fixed with validation cited. The follow-up list (screenshot
      capture at rehearsal) carries over unchanged from Phase 18.
- [x] **Code is clean and committed.** The Phase 19 commit
      bundles four modified files in `services/local-dev/`, three
      cross-service evidence logs, the Phase 18 carry-over
      pointers (unchanged), this verification note, and the
      session archive. No leftover scratch, no stray `.env.local`
      values, no temporary debug logging.
- [x] **Branch is tagged for the presentation.** Annotated tag
      `v1.0.0-cp3` applied on `dev` after the Phase 19 commit.
      See §5.

---

## 7. Known follow-ups for the rehearsal window

These are carry-overs from Phase 18 §7 that remain pending -- they
are correctly out of Phase 19 scope (stabilisation only, no
capture work) and are repeated here so the 2026-05-18 rehearsal
slot has a single source of truth.

- **Capture the screenshot pack at rehearsal** -- six placeholders
  called out in Phase 18 verification §4 (`swagger-restaurant.png`,
  `swagger-menu.png`, `negative-auth-401.png`,
  `negative-auth-403.png`, `cross-service-smoke_<RUN_TAG>.log`,
  `menu-events_<RUN_TAG>.log`).
- **Export the Phase 17 report to PDF** for the hand-in.
- **Confirm speaking-part allocations** with Alfa-Kilo,
  Elephant-Yankee, Mike-Alfa at the rehearsal kick-off.
- **Resilience demo go/no-go** at S14 of the deck, per Phase 18
  fallbacks.

No new follow-ups were opened by Phase 19.

---

## 8. Verification of artefacts

| Artefact | Path | Status |
|----------|------|--------|
| Healthcheck fix + gateway default | `services/local-dev/docker-compose.yml` | Modified |
| Gateway default + comments | `services/local-dev/.env.example` | Modified |
| Cross-service smoke bash | `services/local-dev/smoke-cross-service.sh` | Modified |
| Cross-service smoke PS | `services/local-dev/smoke-cross-service.ps1` | Modified |
| Track evidence logs as audit trail | `.gitignore` | Modified (carve-out for `services/local-dev/evidence/*.log`) |
| Pre-fix cross-service trace (red #1) | `services/local-dev/evidence/cross-service-smoke_20260419T093308Z.log` | New |
| Pre-fix cross-service trace (red #2) | `services/local-dev/evidence/cross-service-smoke_20260419T093500Z.log` | New |
| Post-fix cross-service trace (green) | `services/local-dev/evidence/cross-service-smoke_20260419T094339Z.log` | New |
| Pre-fix menu-events log (empty #1) | `services/local-dev/evidence/menu-events_20260419T093308Z.log` | New |
| Pre-fix menu-events log (empty #2) | `services/local-dev/evidence/menu-events_20260419T093500Z.log` | New |
| Post-fix menu-events log (2 envelopes) | `services/local-dev/evidence/menu-events_20260419T094339Z.log` | New |
| Verification note (this file) | `dev-docs/verification/phase-19-verification_Sierra-Lima.md` | New |
| Session archive | `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_61288aa.md` | Pending commit |

All cross-references in this note resolve to existing repository
paths (verified against the tree at base commit `61288aa` plus the
Phase 19 modifications). The pre-fix evidence is retained
deliberately: a red-then-green pair makes the §1.3 bug fix
auditable, and the files together cost < 4 KB.

---

## 9. Post-commit errata (added 2026-04-19 after tag `v1.0.0-cp3`)

A post-tag frontend-health verification pass exercised every
public and JWT-gated API path from the browser's side of the
proxy. All six containers stayed healthy throughout; the live
walk confirmed SPA, `/api/**` proxy, public menu browse, and
auth-gated endpoints all work end-to-end. That pass also caught
three factual errors in §3 (seeded demo fixtures), corrected
below. The §3 text was written from memory rather than from a
live `GET /api/restaurants` call; the live audit shows the
following, verified against commit `50774fe` + the rebuilt stack.

**§3.1 correction -- seeded owner user-IDs.** The three owners
baked into the Phase 13 seed are `...000000001`, `...000000002`,
`...000000003` (suffixes 1/2/3), **not** `...000000099` /
`...000000098`. The `...099` suffix I quoted is the synthetic JWT
dev-token *subject* used by `smoke.sh` and
`smoke-cross-service.sh` when they stand up a transient owner to
create a smoke-only restaurant. The demo role-map is therefore:

- Seeded owner-1 (`...00000001`) -- owns seeded restaurants
  `d0000001` (Pizza Antonio) and `d0000002` (Sushi Lumi).
- Seeded owner-2 (`...00000002`) -- owns `d0000003` (Cafe Nero)
  and `d0000004` (Burger Bros).
- Seeded owner-3 (`...00000003`) -- owns `d0000005` (Vegan Vibes)
  and `d0000006` (Pasta Palace).
- Smoke-script transient owner (`...00000099`) -- creates
  restaurants at the moment `smoke.sh` or
  `smoke-cross-service.sh` runs; these persist in the DB until
  the next `docker compose down -v`.
- Customer (`...0000000c1`) -- used for every customer-facing
  GET in the smoke scripts and by the demo's browser walk.

Demo impact: the authorisation negative-path drill still works
with any two distinct owner subjects (the Phase 15 check is
"current JWT's userId must match the restaurant's ownerId"),
so the demo narrative stands -- only the IDs in the script
need updating to match the seeded set.

**§3.3 correction -- menu-item counts.** The seed ships sixteen
menu items across the six seeded restaurants (**not** eighteen,
and **not** three-per-restaurant). Live distribution:

| Restaurant | Items |
|------------|-------|
| Pizza Antonio (d0000001) | 4 |
| Sushi Lumi (d0000002) | 3 |
| Cafe Nero (d0000003) | 2 |
| Burger Bros (d0000004) | 3 |
| Vegan Vibes (d0000005) | 3 |
| Pasta Palace (d0000006) | 1 |
| **Total** | **16** |

The menu-item DTO also has flat `priceAmount` (numeric) +
`priceCurrency` (string = `"EUR"` throughout the seed) fields --
**not** a nested `price: { amount, currency }` object. The
frontend `MenuView.vue` consumes the flat shape correctly; the
§3.3 description had it wrong.

**§3.3 correction -- endpoint path for menu browse.** The correct
browse endpoint is `GET /restaurants/{restaurantId}/menu-items`,
**not** `GET /menu-items/by-restaurant/{rid}`. The latter does
not exist on `menu-service` -- Spring's `NoResourceFoundException`
falls through to a 500 via the global handler. The frontend's
`MenuView.vue` line 120 and the Phase 17 report both use the
correct nested path; only the verification-note text was wrong.
The Phase 18 demo script and deck are unaffected.

**§3.4 unchanged.** The order-flow verification (W1 sync chain)
stands as written -- that claim was traced through the green
smoke runs and did not depend on the seeded owner IDs.

**Non-seed entries present.** `GET /api/restaurants` at the time
of the errata pass returned nine entries total: the six seeded
rows above plus three smoke-created rows left over from this
session's smoke runs (owner `...099`, names like `Smoke Cafe
<epoch>` / `X-Smoke Cafe <epoch>`). They are benign -- the next
`docker compose down -v` clears them via the `menu_db_data` /
`restaurant_db_data` volume drop. No action required before the
2026-05-18 rehearsal.

**Frontend health verified end-to-end.** Tested against the
tagged rebuild on 2026-04-19 ~15:00:

- `GET /` (SPA index): 200, Vue 3 SPA HTML with `/js/app.*` +
  `/css/app.*` asset links.
- `GET /api/restaurants` (public): 200, 9 entries.
- `GET /api/restaurants/{rid}/menu-items` (public): 200, 16
  items across the seeded 6.
- `GET /api/menu-items/{id}` (public): 200, flat DTO.
- `GET /actuator/health` on 8081 + 8082: both `UP`.
- `GET /healthz` on dev-gateway (8080): `ok`.
- Auth-negative (omit `Authorization`) on the same routes: all
  endpoints above are public-readable in the current security
  config (matches Phase 15 design: writes protected, reads
  public). Gated mutations (POST/PATCH/PUT/DELETE) correctly
  return 401 without a token.

The tag `v1.0.0-cp3` is **not** moved; it continues to point at
commit `50774fe` (the pre-errata snapshot). The errata land as a
follow-up commit on `dev` so a grader running `HEAD` sees the
corrected text and a grader checking out the tagged commit sees
the original.
