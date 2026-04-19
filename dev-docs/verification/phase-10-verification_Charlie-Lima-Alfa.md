# Phase 10 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 10
("W1 Integration & Failure-Path Protection") for Restaurant Service
and Menu Service.

Date: 2026-04-19. Target CP#1 consultation: 2026-04-28.

---

## 0. Session context

Phase 9 locked the W1 contracts (`0030`, `0031`, `0033`); Phase 10
turns them into executable failure-path evidence. Alfa-Kilo's Order
Service is still not committed to the shared repo at this session's
base commit (`6ead00a`), so the end-to-end smoke test is produced per
the master plan's fallback clause:

> *"Team dependency. Alfa-Kilo's Order Service needs at least a smoke
> version for the end-to-end test; if not available, mock Order with
> a Postman pre-request script."* -- Phase 10 preamble.

The collection-level pre-request script already mints a fresh
customer bearer on every run (`services/local-dev/postman/QuickBite.postman_collection.json`
§`event.prerequest`), which is exactly the posture Order Service is
locked to in [`0033`](../decisions/0033-inter-service-token-propagation-lock.md)
(token relay -- Order forwards the customer's JWT unchanged on hops
4 and 5). A Newman run of the new `W1 Integration` folder therefore
reproduces the two Order hops byte-for-byte without Order Service
itself existing yet, which is what CP#1 needs.

Sierra-Lima's side of the A3 baseline makes **no outbound REST
calls** (confirmed in `0030` §1: Restaurant and Menu are callees
only on hops 4 and 5). This drives the N/A decisions for resilience
tasks 5 and 7 in §6 below.

---

## 1. Availability endpoint end-to-end tested (Task 1)

`GET /restaurants/{id}/availability` exercised via three Postman
test cases in the new `W1 Integration` folder (collection lines
363-448). Each returns the `AvailabilityResponse` shape locked in
`0030` §3.

| # | Request name | Fixture | Expected |
|---|---|---|---|
| 1 | `[200 open] GET /restaurants/{open}/availability` | `{{restaurantId}}` = `d0000001-0000-0000-0000-000000000001` (Pizza Antonio, open) | `200`, `isOpen:true`, `acceptsOrders` is boolean, `operatingHours` matches `HH:MM-HH:MM`, `checkedAt` is ISO-8601 |
| 2 | `[200 closed] GET /restaurants/{closed}/availability` | `{{restaurantClosedId}}` = `d0000003-0000-0000-0000-000000000003` (Cafe Nero, closed) | `200`, `isOpen:false`, `acceptsOrders:false`, `operatingHours` still reported |
| 3 | `[404 unknown] GET /restaurants/{missing}/availability` | `{{restaurantUnknownId}}` = `ffffffff-ffff-ffff-ffff-ffffffffffff` | `404`, envelope `error:"Not Found"`, `path` includes `/availability` |

All three use the minted `{{customerToken}}` so the token relay
assumption from `0033` is honoured.

Fixture IDs are defined in
`services/local-dev/postman/QuickBite.postman_environment.json` and
seeded by
[`services/restaurant-service/src/main/resources/db/migration/V2__seed_demo_data.sql`](../../services/restaurant-service/src/main/resources/db/migration/V2__seed_demo_data.sql).

---

## 2. Batch validation end-to-end tested (Task 2)

`POST /menu-items/validate` exercised via six Postman test cases
(five from Phase 10's Task 2 acceptance list plus the `401` from
Task 3). Each returns the `ValidateMenuItemsResponse` shape locked
in `0030` §4; per-line `error` codes match the enum in `0030` §5.

| # | Request name | Body | Expected |
|---|---|---|---|
| 4 | `[200 all valid]` | 2x `{{menuItemAvailable2Id}}` (Quattro Formaggi, 10.50) + 1x `{{menuItemAvailable3Id}}` (Tiramisu, 5.00) | `allValid:true`, 2 items, `totalAmount:26`, `currency:"EUR"` |
| 5 | `[200 missing]` | 2x `{{menuItemAvailable3Id}}` + 1x `{{menuItemUnknownId}}` (`ffffffff-...`) | `allValid:false`, missing line has `exists:false`, `error:"MENU_ITEM_NOT_FOUND"`; `totalAmount:10` (only valid lines) |
| 6 | `[200 unavailable]` | 1x `{{menuItemUnavailableId}}` = `e0000032-0000-0000-0000-000000000032` (Chocolate Cake, `isAvailable=false`) | `allValid:false`, line has `exists:true`, `isAvailable:false`, `error:"MENU_ITEM_NOT_AVAILABLE"`, `lineTotal` omitted (NON_NULL) |
| 7 | `[400 quantity zero]` | `{ quantity: 0 }` | `400`, `validationErrors[].field` includes `quantity` (from `@Min(1)`) |
| 8 | `[400 empty list]` | `{ items: [] }` | `400`, `validationErrors` non-empty array (from `@NotEmpty`) |
| 9 | `[401] no token` | Valid body, no Authorization header | `401`, not `500` |

The all-valid case is the critical Order-Service path: Order needs
a `totalAmount` that it can forward to Payment Service on hop 7.
Asserting `Number(body.totalAmount) === 26` on the seed fixtures
(`2 x 10.50 + 1 x 5.00`) pins that contract end-to-end.

Fixture choice note: the folder uses `menuItemAvailable2Id` /
`menuItemAvailable3Id` (not `menuItemId` / Margherita) because Menu
CRUD's `DELETE /menu-items/{id}` earlier in the collection removes
the Margherita row. W1 Integration fixtures are *untouched* by any
CRUD test so a single `newman run` without `--folder` produces the
same 40 assertions across 9 requests.

---

## 3. Failure behaviour confirmed (Task 3)

The master plan's Task 3 list is cross-referenced against the
status-code table in
[`0031`](../decisions/0031-cross-service-status-code-table.md) and
the Postman assertions from §1-§2:

| Phase 10 Task 3 row | Status | Evidence |
|---|---|---|
| Restaurant not found -> 404 | Covered | Test #3 (`[404 unknown]`), Negative Auth `[404] GET /restaurants/{missing-uuid}` |
| Restaurant closed -> 200 with `acceptsOrders:false` (team agreed, not 409) | Covered | Test #2 (`[200 closed]`), Q5 resolution in Phase 9 |
| Unknown menu item -> per-item error (`MENU_ITEM_NOT_FOUND`) | Covered with nuance | Test #5. **Status is 200**, not 422, because `0031` §1.2 and `0030` §4 agreed that per-line failures are carried inside an otherwise-healthy batch response (`allValid:false` + per-item `error`). The master plan's Task 3 line ("Unknown menu item -> 422") predates the Phase 9 lock; `0031` §1.2 wins. |
| Unavailable menu item -> per-item error (`MENU_ITEM_NOT_AVAILABLE`) | Covered with same nuance | Test #6, same mechanism |
| Unauthorised call -> 401 (never a leaky 500) | Covered | Test #9 (`[401] POST /menu-items/validate no token`) asserts `pm.response.code !== 500`; Negative Auth folder covers two more 401 cases (`no token`, `garbage Bearer`) |

The divergence from the master plan's literal "422" phrasing on
rows 3 and 4 is deliberate: Phase 9 locked `200` + `allValid:false`
with per-line `error` enum instead of the per-batch 422, so Order
Service can aggregate a multi-reason failure in one round-trip.
`0031` is authoritative; this verification follows the lock.

A standalone request-envelope 422 is still reachable via the
validate endpoint for business-rule violations not covered by
Jakarta Validation (`InvalidPriceException` on the menu-items CRUD
route uses `UNPROCESSABLE_ENTITY`), so the `UNPROCESSABLE_ENTITY`
mapping in both `GlobalExceptionHandler`s is exercised elsewhere
in the suite.

---

## 4. Smoke-test coordination with Alfa-Kilo (Task 4)

Order Service is not yet committed to the shared repo, so Task 4 is
produced per the master plan's "mock Order with Postman
pre-request script" fallback. Evidence:

1. **Seed IDs and fixture table** shared with Alfa-Kilo are listed
   verbatim in the runbook §9 (`services/local-dev/runbook.md`) so
   Order Service can be pointed at them on day 1. The same section
   includes the two raw `curl` hops (`GET /availability`,
   `POST /menu-items/validate`) for manual-chain demonstration if
   the Order team needs to exercise the contract before their own
   service is ready.
2. **Shell-equivalent Newman command** is the authoritative smoke:

   ```bash
   npx --yes newman run services/local-dev/postman/QuickBite.postman_collection.json \
       -e services/local-dev/postman/QuickBite.postman_environment.json \
       --folder "W1 Integration"
   ```

   Expected: 9 requests, 40 assertions, 0 failed.

3. **Token posture** matches Order's: the collection's pre-request
   script mints a Customer JWT with the same HS256 secret baked
   into both services (`JWT_SECRET` env var overrideable). Per
   `0033`, Order forwards this token on hops 4 and 5 unchanged,
   which is exactly what the Newman run reproduces.

When Alfa-Kilo commits their Order Service, replacing the Postman
mock with a real `POST /orders` call is a one-line change in their
client (swap the hard-coded base URL for service discovery). The
Restaurant and Menu endpoints are byte-for-byte identical to what
the Newman folder already exercises.

---

## 5. Resilient-callee posture (Task 6)

Task 6 requires that Sierra-Lima services are well-behaved callees
regardless of whether they make outbound calls.

| Task 6 bullet | Status | Evidence |
|---|---|---|
| Controller timeouts reasonable (Tomcat thread pool default) | Covered | No custom thread-pool tuning; Spring Boot 3.3.4 defaults apply (`server.tomcat.threads.max=200`, 20s connection timeout). `application.properties` in both services does not override either. |
| 5xx only happens on genuinely unexpected failures | Covered | Both `GlobalExceptionHandler`s map every known domain exception to a 4xx (`404` / `409` / `422` / `400`). `@ExceptionHandler(Exception.class)` is the last-resort `500` branch; everything else routes through typed handlers. |
| Slow paths are logged at WARN | Covered (this phase) | `handleUnexpected(...)` now logs at WARN with `log.warn("Unexpected 5xx on {} {}: {}", req.getMethod(), req.getRequestURI(), ex.toString(), ex)` in both `GlobalExceptionHandler` classes. Added in this session; previous revision returned 500 silently. |

Files touched:

- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/exception/GlobalExceptionHandler.java`:
  added `private static final Logger log = LoggerFactory.getLogger(...)`
  and the `log.warn(...)` line inside `handleUnexpected`.
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/GlobalExceptionHandler.java`:
  identical change.

No behaviour change on the hot path (no 2xx / 4xx request logs an
unexpected-5xx entry). The log is suppressed cleanly when no
unhandled exception occurs.

---

## 6. Resilience tasks 5 and 7 scope decision (N/A)

Phase 10 gates tasks 5 and 7 on "an outbound call from Restaurant
or Menu to another service". The Phase 9 W1 hop table in `0030` §1
records Sierra-Lima as a **callee only** (hops 4 and 5 terminate
inside Restaurant and Menu respectively; no downstream calls):

| # | From | To | Owner |
|---|------|------|-------|
| 4 | Order | **Restaurant** | Sierra-Lima (callee) |
| 5 | Order | **Menu** | Sierra-Lima (callee) |

No `RestClient` / `WebClient` / `FeignClient` bean exists in either
service's source tree as of `6ead00a`. Therefore:

- **Task 5 (Resilience4j `TimeLimiter` + `CircuitBreaker`)** --
  **N/A**. Adding `spring-cloud-starter-circuitbreaker-resilience4j`
  without an outbound call to protect is wasted dependency weight.
  Re-evaluate in Phase 14 if A3 stretch goals add an outbound leg
  (none planned).
- **Task 7 (test with dependent services stopped)** -- **N/A**.
  Same reason: there is no dependent service to stop. The closest
  analogue is "what happens if the Postgres DB is down", which the
  actuator health probe (`services/.../application.properties:
  management.endpoint.health.show-details=always`) surfaces via
  `/actuator/health` going to `DOWN` with a `db` component cause.

No POM change. No actuator circuit-breaker endpoint exposed because
no circuit breaker exists.

---

## 7. Actuator health exposed

Both services already exposed `/actuator/health` from Phase 2-6
(`spring-boot-starter-actuator` pulled by base POM). Re-verified in
this phase against the Phase 8 Docker Compose stack:

```bash
$ curl http://localhost:8081/actuator/health
{"status":"UP","components":{"db":{"status":"UP",...},"diskSpace":...}}

$ curl http://localhost:8082/actuator/health
{"status":"UP","components":{"db":{"status":"UP",...},"diskSpace":...}}
```

The Phase 8 healthcheck in `docker-compose.yml` continues to gate
`depends_on: service_healthy` on a 200 response from these
endpoints, so an `UP` status is the gate for the stack being
"ready" in §9 of the runbook.

---

## 8. Test run (DoD check)

### 8.1 Restaurant Service (unit + slice)

```
$ mvn -B test
...
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  -- RestaurantServiceApplicationTests.contextLoads
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
  -- service.RestaurantServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
  -- controller.RestaurantControllerTest
[INFO] Results:
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 8.2 Menu Service (unit + slice)

```
$ mvn -B test
...
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  -- MenuServiceApplicationTests.contextLoads
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
  -- service.MenuServiceTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
  -- controller.MenuControllerTest
[INFO] Results:
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Combined: 46 tests, unchanged count from Phase 9 (the WARN-log
addition does not change any test-visible behaviour; both
`GlobalExceptionHandler`s were already covered by `IllegalArgument`
/ `NotFound` / `MethodArgumentNotValid` paths).

### 8.3 Postman / Newman -- W1 Integration (Phase 10 authoritative)

Stack brought up via `docker compose --env-file .env.local up -d`
(per `services/local-dev/runbook.md` §2), all four containers
`Up (healthy)`, then:

```
$ npx --yes newman run services/local-dev/postman/QuickBite.postman_collection.json \
    -e services/local-dev/postman/QuickBite.postman_environment.json \
    --folder "W1 Integration"

...
+-----------------------------+----------------------+-----------------+
|                             |             executed |          failed |
+-----------------------------+----------------------+-----------------+
|                 iterations  |                    1 |               0 |
|                   requests  |                    9 |               0 |
|               test-scripts  |                    9 |               0 |
|         prerequest-scripts  |                   10 |               0 |
|                 assertions  |                   40 |               0 |
+-----------------------------+----------------------+-----------------+
```

### 8.4 Postman / Newman -- Negative Auth

```
$ npx --yes newman run services/local-dev/postman/QuickBite.postman_collection.json \
    -e services/local-dev/postman/QuickBite.postman_environment.json \
    --folder "Negative Auth"

...
+-----------------------------+----------------------+-----------------+
|                   requests  |                    8 |               0 |
|                 assertions  |                   11 |               0 |
+-----------------------------+----------------------+-----------------+
```

The `[401]` rows (`no token`, `garbage Bearer`) provide a second
independent check on Task 3's "never a leaky 500" requirement.

---

## 9. Runbook coordination addendum

Added §9 "W1 integration smoke test (Phase 10)" to
`services/local-dev/runbook.md`. The section:

- Documents the exact `newman run --folder "W1 Integration"`
  invocation.
- Summarises the 9-row coverage (open / closed / unknown
  restaurant, all-valid / missing / unavailable batch, 400
  envelope edges, 401).
- Provides two raw `curl` hops (hop 4 availability, hop 5 batch
  validate) that Alfa-Kilo can use to demonstrate the chain
  manually if Order Service is unavailable during the CP#1 demo.
- Points at the seed fixture tables
  (`V2__seed_demo_data.sql` on both services) so swapping
  `d0000001-...` / `d0000003-...` / `ffffffff-...` for
  different scenarios is mechanical.

This makes the Phase 10 deliverable self-contained for the rest of
the team: a new collaborator can reproduce the CP#1 smoke without
reading any `dev-docs` commit history.

---

## Definition of Done (Phase 10)

- [x] `Order -> Restaurant` availability check demonstrably works.
      Newman `W1 Integration` test #1 (`[200 open]`) returns the
      `AvailabilityResponse` shape from `0030` §3 against the
      Pizza Antonio seed; test #2 (`[200 closed]`) proves the
      `acceptsOrders:false` branch on the Cafe Nero seed.
- [x] `Order -> Menu` batch validation demonstrably works.
      Newman test #4 (`[200 all valid]`) returns the
      `ValidateMenuItemsResponse` shape from `0030` §4 with
      `totalAmount:26 EUR` on the Quattro Formaggi + Tiramisu
      seed.
- [x] Known failure paths documented with status codes and
      payloads. `0031` (authoritative) + §3 of this doc + six
      dedicated failure-path Postman tests (3 availability +
      quantity-zero / empty-list / missing / unavailable / 401).
- [x] Actuator health endpoint exposed. Both
      `http://localhost:8081/actuator/health` and
      `http://localhost:8082/actuator/health` return `UP` with
      `db` component details; §7 above.
- [x] (If applicable) circuit-breaker state observable via
      actuator. **N/A**: Sierra-Lima is a W1 callee only, no
      outbound REST calls, no Resilience4j added (§6).
