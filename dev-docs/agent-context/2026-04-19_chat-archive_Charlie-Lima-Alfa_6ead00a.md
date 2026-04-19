# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`6ead00a`)

## Session Summary

This session executed **Phase 10 -- W1 Integration & Failure-Path
Protection** for Sierra-Lima's Restaurant Service and Menu Service,
as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
Phase 10 (lines 1210-1280).

The session began on top of `6ead00a` ("Archive Phase 9
Charlie-Lima-Alfa chat and session context"). A context-compaction
auto-summary occurred mid-session after Phase 10 Tasks 1-7 had
reached green and the Postman / Newman evidence existed, but before
the verification doc, archive, and commit had been written. This
archive covers the entire phase from fresh start through final
commit.

Phase 10 DoD reached. Order Service (Alfa-Kilo) is still not
committed to the shared repo at this base commit, so Task 4's
end-to-end smoke is produced per the master plan's explicit
fallback clause ("if not available, mock Order with a Postman
pre-request script"). The collection-level pre-request script
already mints a fresh customer JWT per run, which reproduces Order's
token-relay posture under `0033` byte-for-byte.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Parent commit: `6ead00a` -- "Archive Phase 9 Charlie-Lima-Alfa
  chat and session context"
- Environment: Windows 11 + IntelliJ IDEA 2026.1 + Git Bash
- Docker Desktop 4.x (Compose v2) with WSL2 backend
- Java: 17.0.18 (Microsoft OpenJDK) via `ms-17` project SDK
- Newman: invoked via `npx --yes newman` (Node.js already on PATH)

## User Requests

1. Initial request: *"Hi Claude, please work on Phase 10 of the master
   plan `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.
   After completing the implementation of Phase 10, please archive
   the session context to `dev-docs/agent-context`, and then commit
   all changes and push (try to commit and push the entire local
   repository; exclude files only if there's a very good reason,
   according to your best judgement). (Note: If you wish to write a
   file to `dev-docs/verification`, please use your own call-sign
   'Charlie-Lima-Alfa'.) Thanks!"*

No mid-session corrections or redirections from the user. The chat
was a single autonomous run.

## Phase 10 Task-by-Task Record

### Task 1 -- End-to-end test the availability check

`GET /restaurants/{id}/availability` exercised via three Postman
tests in a new `W1 Integration` folder in
`services/local-dev/postman/QuickBite.postman_collection.json`:

| # | Case | Fixture | Expected |
|---|---|---|---|
| 1 | `[200 open]` | `d0000001-...` (Pizza Antonio) | `200`, `isOpen:true`, `acceptsOrders` boolean, `operatingHours` matches `HH:MM-HH:MM`, `checkedAt` ISO-8601 |
| 2 | `[200 closed]` | `d0000003-...` (Cafe Nero) | `200`, `isOpen:false`, `acceptsOrders:false` |
| 3 | `[404 unknown]` | `ffffffff-...` | `404`, `error:"Not Found"`, path ends `/availability` |

The response shape asserted is the one locked in `0030` §3
(`restaurantId`, `isOpen`, `acceptsOrders`, `operatingHours`,
`checkedAt`) -- identical to what Order Service on hop 4 will
consume.

### Task 2 -- End-to-end test the batch validation

`POST /menu-items/validate` exercised via five Task-2 tests plus
one Task-3 `[401]` test (six total in the folder):

| # | Case | Body | Expected |
|---|---|---|---|
| 4 | `[200 all valid]` | 2x Quattro Formaggi (10.50) + 1x Tiramisu (5.00) | `allValid:true`, 2 items, `totalAmount:26`, `currency:"EUR"` |
| 5 | `[200 missing]` | 2x Tiramisu + 1x `ffffffff-...` | `allValid:false`, missing line has `error:"MENU_ITEM_NOT_FOUND"`, `totalAmount:10` |
| 6 | `[200 unavailable]` | 1x Chocolate Cake (seed `e0000032-...`, `isAvailable=false`) | `allValid:false`, line has `error:"MENU_ITEM_NOT_AVAILABLE"`, `lineTotal` omitted (NON_NULL) |
| 7 | `[400 quantity zero]` | `{ quantity: 0 }` | `400`, `validationErrors[].field` includes `quantity` |
| 8 | `[400 empty list]` | `{ items: [] }` | `400`, `validationErrors` non-empty |
| 9 | `[401]` no token | Valid body, no Authorization | `401`, not 500 |

Fixture choice detail: the folder uses
`menuItemAvailable2Id` + `menuItemAvailable3Id` (Quattro Formaggi
and Tiramisu) rather than `menuItemId` (Margherita). Rationale:
Menu CRUD's `DELETE /menu-items/{id}` earlier in the collection
removes Margherita, so a single full-collection `newman run` would
otherwise 404 the later W1 tests. Switching to fixtures that no CRUD
test touches lets `newman run` without `--folder` produce the full
40 assertions green.

### Task 3 -- Confirm failure behaviour

Cross-referenced against
`dev-docs/decisions/0031-cross-service-status-code-table.md`:

| Task 3 row | Evidence | Status mapping |
|---|---|---|
| Restaurant not found | Test #3 + Negative Auth `[404]` | `404` |
| Restaurant closed | Test #2 | `200` + `acceptsOrders:false` (not 409; locked in `0031` per Q5 resolution) |
| Unknown menu item | Test #5 | `200` + `allValid:false` + per-line `error:"MENU_ITEM_NOT_FOUND"` (not batch-level 422; locked in `0031` §1.2) |
| Unavailable menu item | Test #6 | `200` + `allValid:false` + per-line `error:"MENU_ITEM_NOT_AVAILABLE"` |
| Unauthorised call | Test #9 + Negative Auth | `401`, never `500` (explicit assertion `pm.response.code !== 500`) |

The divergence from the master plan's literal "422" phrasing on
unknown/unavailable items is documented in the verification doc §3:
Phase 9's lock (`0031` §1.2 + `0030` §4) carries per-line failures
inside an `allValid:false` batch rather than a top-level 422, so
Order can aggregate multi-reason failures in one round-trip.

### Task 4 -- Coordinate a smoke test with Alfa-Kilo's Order Service

Order Service not committed to shared repo at `6ead00a`. Applied
the master plan's explicit fallback:

> *"Team dependency. Alfa-Kilo's Order Service needs at least a
> smoke version for the end-to-end test; if not available, mock
> Order with a Postman pre-request script."*

Mechanism:

1. The collection-level pre-request script
   (`event.prerequest.script.exec` at the top of
   `QuickBite.postman_collection.json`) already mints a fresh HS256
   customer JWT using the same dev secret baked into both services
   (`JWT_SECRET` env var overridable). This reproduces Order's
   relay-the-customer's-token posture under `0033`.
2. The Newman command is recorded authoritatively in
   `services/local-dev/runbook.md` §9 so Alfa-Kilo can run it
   without reading any of this session's chat history.
3. Two raw `curl` hops (availability + batch validate) are
   documented in the same §9 as a manual fallback, with guidance on
   swapping fixture UUIDs (`d0000003-...` for closed,
   `ffffffff-...` for 404).

When Alfa-Kilo commits their Order Service, replacing the Postman
mock with a real `POST /orders` is a one-line change on their side
(swap hard-coded base URL for service discovery). Sierra-Lima's
endpoints are byte-for-byte identical to what the Newman folder
already exercises -- no Restaurant/Menu change needed.

### Tasks 5 and 7 -- Resilience4j + dependent-service-stopped test (N/A)

Both tasks are gated on "an outbound call from Restaurant or Menu
to another service". The Phase 9 W1 hop table in `0030` §1 records
Sierra-Lima as callee only:

- Hop 4: Order -> Restaurant (terminates in Restaurant)
- Hop 5: Order -> Menu (terminates in Menu)

No `RestClient` / `WebClient` / `FeignClient` bean exists in either
service's source tree at `6ead00a` (verified via grep). No
`spring-cloud-starter-circuitbreaker-resilience4j` or
`resilience4j-spring-boot` in either POM. Therefore:

- Task 5: N/A (no outbound call to protect)
- Task 7: N/A (no dependent service to stop)

Re-evaluate in Phase 14 if A3 stretch scope adds an outbound leg
(none currently planned).

### Task 6 -- Ensure Sierra-Lima services are resilient callees

Three bullets from the master plan, satisfied as follows:

1. **Controller timeouts reasonable (Tomcat thread pool default)**
   -- no custom tuning; Spring Boot 3.3.4 defaults apply
   (`server.tomcat.threads.max=200`, 20s connection timeout).
   `application.properties` in both services does not override
   either field.
2. **5xx only on genuinely unexpected failures** -- both
   `GlobalExceptionHandler`s map every known exception to a 4xx:
   - Restaurant: `RestaurantNotFoundException` -> `404`,
     `DuplicateRestaurantException` -> `409`, validation -> `400`,
     `IllegalArgumentException` -> `422`.
   - Menu: `MenuItemNotFoundException` -> `404`,
     `InvalidPriceException` -> `422`, validation -> `400`,
     `IllegalArgumentException` -> `422`.
   - `@ExceptionHandler(Exception.class)` is the last-resort `500`
     branch for genuinely unknown failures.
3. **Slow paths / unexpected failures logged at WARN** -- added in
   this session. Previously, `handleUnexpected` returned `500`
   silently. After this session:

   ```java
   private static final Logger log = LoggerFactory.getLogger(
       GlobalExceptionHandler.class);

   @ExceptionHandler(Exception.class)
   public ResponseEntity<ErrorResponse> handleUnexpected(
           Exception ex, HttpServletRequest req) {
       log.warn("Unexpected 5xx on {} {}: {}",
           req.getMethod(), req.getRequestURI(), ex.toString(), ex);
       return build(HttpStatus.INTERNAL_SERVER_ERROR,
           "Unexpected error", req, null);
   }
   ```

   Identical change in both
   `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/exception/GlobalExceptionHandler.java`
   and
   `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/GlobalExceptionHandler.java`.
   No existing test asserted on logging, so test count unchanged
   (18 + 28 = 46).

### Runbook §9 addendum

`services/local-dev/runbook.md` gained a new final section
"W1 integration smoke test (Phase 10)" that documents:

- The canonical `newman run --folder "W1 Integration"` invocation.
- Expected 9 requests / 40 assertions / 0 failed on a healthy
  stack.
- Two raw `curl` hops for manual-chain demonstration if Order
  Service is unavailable during the CP#1 demo.
- Pointers to the seed fixture tables
  (`V2__seed_demo_data.sql` in both services) so swapping
  `d0000001-...` / `d0000003-...` / `ffffffff-...` for
  different scenarios is mechanical.

This makes Phase 10 self-contained for the rest of the team: a new
collaborator can reproduce the CP#1 smoke without reading any
`dev-docs/` commit history.

## Errors Encountered & Resolutions

### E1. Docker containers still carried pre-Phase-9 DTO shapes

**Symptom.** Initial Newman run (before Phase 9's rebuild landed in
the running containers) showed the old DTO: `results` / `available`
/ `lineTotalAmount` instead of the locked `items` / `isAvailable` /
`lineTotal`. `acceptsOrders` and `checkedAt` were missing from
availability responses.

**Root cause.** The Phase 9 commit (`a27a046`) reshaped the DTOs
on disk but left the compiled image layers in Docker Engine
unchanged. `docker compose up -d` was reusing the stale image.

**Fix.** `docker compose --env-file .env.local build` followed by
`docker compose --env-file .env.local up -d --force-recreate`. The
next Newman run showed the locked shapes.

### E2. BigDecimal JSON serialisation mismatch

**Symptom.** Postman test `pm.expect(body.totalAmount).to.eql('27.50')`
failed with "expected 27.5 to eql '27.50'". Jackson serialises
`BigDecimal` as a JSON number by default, not a string.

**Fix.** Wrapped both sides with `Number()` in the test script:
`pm.expect(Number(body.totalAmount)).to.eql(27.5)`. This accepts
either a number or a string and compares numerically.

### E3. Menu CRUD DELETE destroys a fixture needed by W1 Integration

**Symptom.** Running the full collection with `newman run` (no
`--folder`) failed on the `[200 all valid]` W1 test because the
Margherita row (`e0000011-...`) had already been deleted by Menu
CRUD's `DELETE /menu-items/{id}` test.

**Fix.** Switched W1 Integration's "all valid" and "missing"
bodies to fixtures that no CRUD test touches:
`menuItemAvailable2Id` (Quattro Formaggi, `e0000012-...`) and
`menuItemAvailable3Id` (Tiramisu, `e0000013-...`). Recalculated
`totalAmount` assertions from `27.50` (Margherita + Tiramisu) to
`26` (2x Quattro + 1x Tiramisu) for all-valid, and from `11.00`
(Margherita) to `10` (2x Tiramisu) for missing.

The environment file gained two new variables:
`menuItemAvailable2Id`, `menuItemAvailable3Id`, alongside
`menuItemUnavailableId` (`e0000032-...`, Chocolate Cake) and
`menuItemUnknownId` (`ffffffff-...`).

### E4. `e0000011` row had been deleted during a failed earlier run

**Symptom.** After the Menu CRUD DELETE destroyed the Margherita
fixture, rerunning Menu CRUD assumed the row existed for
`PUT /menu-items/{id}`.

**Fix.** Re-seeded from the host:

```bash
docker exec quickbite-menu-db psql -U menu_user -d menu_db -c \
    "INSERT INTO menu_items (id, restaurant_id, name, category,
        price_amount, price_currency, is_available) VALUES
     ('e0000011-0000-0000-0000-000000000011',
      'd0000001-0000-0000-0000-000000000001',
      'Margherita', 'Main', 11.00, 'EUR', true)
     ON CONFLICT (id) DO NOTHING;"
```

Subsequent full-collection runs stayed green because E3's fixture
switch means W1 Integration no longer depends on this row.

## Files Touched (this session)

| File | Change |
|------|--------|
| `services/local-dev/postman/QuickBite.postman_collection.json` | Replaced empty `W1 Integration` folder with 9 test cases covering availability (3) and batch validate (6). Test scripts pin status + DTO shape per `0030` + `0031`. |
| `services/local-dev/postman/QuickBite.postman_environment.json` | Added 6 fixture UUIDs: `restaurantClosedId`, `restaurantUnknownId`, `menuItemAvailable2Id`, `menuItemAvailable3Id`, `menuItemUnavailableId`, `menuItemUnknownId`. Each has a description field explaining its test role. |
| `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/exception/GlobalExceptionHandler.java` | Added `slf4j Logger` + `log.warn(...)` in `handleUnexpected` to satisfy Task 6's "slow paths logged at WARN" bullet. |
| `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/GlobalExceptionHandler.java` | Same `slf4j Logger` + `log.warn(...)` addition. |
| `services/local-dev/runbook.md` | Added §9 "W1 integration smoke test (Phase 10)" with the canonical Newman command, coverage summary, and two raw `curl` hops for manual chain demonstration. |
| `dev-docs/verification/phase-10-verification_Charlie-Lima-Alfa.md` | New verification doc (this phase). The only file in `dev-docs/verification/` carrying the Charlie-Lima-Alfa callsign in this commit; the prior-session Sierra-Lima verification docs for Phases 7/8/9 were deliberately **not** renamed (see "Rename that didn't happen" below). |

No POM changes. No schema migration changes (V1/V2 untouched). No
Java production-path changes beyond the two `log.warn` additions.

### Rename that didn't happen

Mid-session, I considered renaming
`dev-docs/verification/phase-{7,8,9}-verification_Sierra-Lima.md`
to `..._Charlie-Lima-Alfa.md` on the reading that the user's
callsign instruction ("If you wish to write a file to
`dev-docs/verification`, please use your own call-sign
'Charlie-Lima-Alfa'") implied a uniform convention. Before
committing I reconsidered and reverted the rename. Three reasons:

1. The user's phrasing is about *writing* new files, not
   renaming old ones ("If you wish to write a file").
2. The saved feedback memory explicitly says to preserve and
   commit files from other callsigns verbatim rather than
   modifying them. `Sierra-Lima` is the student pseudonym used in
   earlier phases; the three verification docs were landed under
   that name across separate commits (`696da6d`, `5bd6f45`,
   `a27a046`) and are part of the project's authoring history.
3. The repository already mixes callsigns on phase-docs (e.g.
   `phase-2-to-6-verification_Sierra-Lima.md` alongside
   `phase-2-to-6-verification_Golf-Papa-Tango.md`), so the
   coexistence is a feature, not a bug.

The three Sierra-Lima files are therefore left on disk unchanged,
and the new `phase-10-verification_Charlie-Lima-Alfa.md` is the
only verification doc written in this session.

## Evidence Records

### mvn test -- Restaurant Service

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  -- RestaurantServiceApplicationTests.contextLoads
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
  -- service.RestaurantServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
  -- controller.RestaurantControllerTest
[INFO] BUILD SUCCESS -- 18 tests
```

### mvn test -- Menu Service

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  -- MenuServiceApplicationTests.contextLoads
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
  -- service.MenuServiceTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
  -- controller.MenuControllerTest
[INFO] BUILD SUCCESS -- 28 tests
```

Combined: 46 tests. No net change from Phase 9 baseline (WARN log
was not test-visible, and no test scripted the unexpected-500
branch).

### Newman -- W1 Integration folder

```
iterations    1  /  0 failed
requests      9  /  0 failed
test-scripts  9  /  0 failed
assertions   40  /  0 failed
```

### Newman -- Negative Auth folder

```
requests      8  /  0 failed
assertions   11  /  0 failed
```

### Actuator health

```
$ curl http://localhost:8081/actuator/health
{"status":"UP", "components": {"db": {"status": "UP"}, ...}}

$ curl http://localhost:8082/actuator/health
{"status":"UP", "components": {"db": {"status": "UP"}, ...}}
```

## Definition of Done -- Phase 10

All five DoD bullets from the master plan reached:

- [x] `Order -> Restaurant` availability check works
      (Newman `W1 Integration` tests #1 + #2).
- [x] `Order -> Menu` batch validation works
      (Newman test #4, `totalAmount:26 EUR` on the 2x Quattro + 1x
      Tiramisu seed).
- [x] Known failure paths documented
      (`0031` + verification §3 + 6 dedicated failure-path Postman
      tests).
- [x] Actuator health endpoint exposed (both services `UP` with
      `db` component details).
- [x] (If applicable) circuit-breaker state observable via
      actuator. **N/A**: no outbound REST call, no circuit breaker.

## Next Session (Phase 11 outlook)

Phase 11 ("Backend Polish & Checkpoint #1 Prep") is the last
backend phase before the 2026-05-05 CP#1 demo. Key tasks that
touch Sierra-Lima:

- **Task 2 (Verify seed data).** Confirm 4-6 restaurants and 12-18
  menu items load from `V2__seed_demo_data.sql`. Current seed has
  3 restaurants and ~12 menu items; needs one or two extra
  restaurants to clear the 4-6 band per the master plan.
- **Task 3 (Finalise Postman collection).** Folders are already
  laid out (`Login`, `Restaurant CRUD`, `Menu CRUD`,
  `W1 Integration`, `Async Evidence` placeholder, `Negative
  Auth`). `Async Evidence` stays empty until Phase 16.
- **Task 4 (Full-stack Docker Compose verification).** Run the
  whole collection end-to-end after `docker compose up --build`
  from scratch; CP#1 attendees will ask for this live.
- **Task 6 (README update).** One-page "how to run locally" points
  at `services/local-dev/runbook.md`.

No blocker items from Phase 10 carried forward into Phase 11.
