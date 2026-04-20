# Checkpoint #1 -- Sierra-Lima Talking Points

- **Checkpoint date:** 2026-05-05 (backend only).
- **Target consultation:** 2026-04-28.
- **Owner:** Sierra-Lima (Restaurant + Menu services).
- **Source:** `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 11
  §6; assignment feedback carried forward from Assignment 1.
- **Demo stack base commit:** tracked in
  [`phase-11-verification_Sierra-Lima.md`](./verification/phase-11-verification_Sierra-Lima.md).

This file is the script Sierra-Lima reads from during CP#1. Team
choices that needed justification to the graders live here; anything
already obvious from the running stack is not restated.

---

## 1. Why seven implemented services, `Review` design-only

Assignment 3 §2.4 froze the implementation subset at **seven** of the
eight QuickBite business services, plus the API Gateway and the Kafka
broker. `Review Service` is design-only.

Rationale:

- **Scope fit.** A 4-person team on a ~10-week schedule cannot deliver
  eight CRUD surfaces plus two demo workflows plus a frontend and
  still leave time for integration and testing. Four owners x two
  services = seven implemented slots once `Review` drops out.
- **Contribution to demo workflows.** W1 (Place Order), W2 (Order
  status propagation), and W3 (Notifications on order events) do not
  cross `Review Service`. Omitting it does not hollow out any demo.
  Adding it would require inventing a workflow that lives entirely
  inside `Review` -- there are none in the A3 scope.
- **Artefacts unchanged.** Every business diagram (A1, A2) still
  shows `Review Service` with its responsibilities; the assignment
  report explicitly lists `Review` under "limitations and future
  work" (Phase 17 §1). The graders see no gap in the design.

Team ownership (A3 §7): Alfa-Kilo owns Order / User / Gateway;
Sierra-Lima owns Restaurant / Menu; Elephant-Yankee owns Payment /
Delivery; Mike-Alfa owns Notification and the Kafka broker config.

---

## 2. Why static configuration, no Eureka / service discovery

`0020-sierra-lima-contracts.md` §10 and the Phase-0 scope freeze both
note: **no Eureka, no Consul, no Spring Cloud Config**. The demo
stack wires services statically -- each container knows the others
by DNS name on the shared `quickbite-net` bridge network (see
`services/local-dev/docker-compose.yml`).

Rationale:

- **A3 explicitly scoped it out.** Assignment 3 §3 lists the shared
  components as `API Gateway` and `Event Broker configuration` --
  **not** a discovery server. Adding Eureka would be scope creep.
- **Static ports are deterministic for grading.** The grader (or
  we, during the live demo) can `curl http://localhost:8081/...`
  without bootstrapping a registry first. Fewer failure modes on
  demo day.
- **Four services on one laptop** is the entire universe of the
  demo. Discovery earns its complexity at tens of instances, not
  two.
- **Team-wide alignment.** Phase 9's contract lock (`0030`, `0031`,
  `0033`) never mentions service discovery; every call is a raw
  HTTP hop to a known DNS name.

Talking point if challenged: "The A3 design uses a gateway, not a
discovery server. Every cross-service call in W1 and W2/W3 resolves
by container name on the compose network. Promoting this to Eureka
would be a production hardening exercise, not a course deliverable."

---

## 3. Why each service has its own database (Assignment 1 feedback)

Assignment 1 feedback flagged: **"Explicitly state that each service
has its own database."** We now do, in three places:

- `dev-docs/decisions/0001-scope-freeze.md`: DB-per-service is one
  of the hard scope pins.
- `services/local-dev/docker-compose.yml`: two separate Postgres
  containers -- `quickbite-restaurant-db` (port 5432) and
  `quickbite-menu-db` (port 5433). Separate named volumes
  (`restaurant_db_data`, `menu_db_data`). Zero shared state.
- `services/restaurant-service/src/main/resources/application.properties`
  and the Menu equivalent: each points at its own JDBC URL, its own
  Flyway migrations, its own credentials.

Cross-service references are UUIDs only -- no foreign keys across
service boundaries. For example `MenuItem.restaurantId` is a UUID
that points at Restaurant Service's primary key, but there is no
database-level FK. `0020` §6 calls this out; seed data in both
`V2__seed_demo_data.sql` files uses matching UUIDs so demo joins
work without coupling the schemas.

Why this matters: the first-principle of microservices is that each
service can be deployed, migrated, and scaled independently. A
shared DB defeats that. The checkpoint demo runs two Postgres
instances to make the separation visible, not one with two schemas.

---

## 4. How auth is enforced (gateway and service level)

Two layers of JWT enforcement are live. Both run against every
protected request; failing either gives a 401 / 403.

### 4.1 JWT origin

`0010-auth-contract.md` locks the token shape. Claims carried:

- `iss` -- expected `quickbite-user-service`.
- `sub`, `userId` (UUID), `role` (`Customer` / `RestaurantOwner` /
  `Admin`).
- `tokenType` -- `USER` for end-user requests; reserved for future
  service-to-service tokens (not used in W1).
- HS256 signature, base64-encoded HMAC key shared via env var
  (`JWT_SECRET`). Dev default lives in both services'
  `application.properties` and is overridden in Docker Compose /
  CI.

Tokens are minted by Alfa-Kilo's `User Service`
(`POST /api/auth/login`). Until that service is committed to the
shared repo, the Postman collection's collection-level pre-request
script mints equivalent tokens client-side (same secret, same
claims) so CP#1 demos run without the User Service dependency.

### 4.2 Gateway layer (Alfa-Kilo, shared)

Not Sierra-Lima's code, but visible in the stack once Alfa-Kilo
commits it. Gateway validates signature, strips the `/api` prefix,
forwards to the target service preserving the `Authorization`
header per `0033` §2 (token relay).

### 4.3 Service layer (Sierra-Lima, Phase 7)

Each service runs a `JwtAuthFilter` (extends `OncePerRequestFilter`)
registered in `SecurityConfig`. Responsibilities:

1. Accept `Authorization: Bearer <jwt>`. Missing header on a
   protected route -> 401 (`RestAuthEntryPoints`).
2. Parse and signature-verify with the shared HS256 key.
3. Reject tokens missing `userId` or `role` (401).
4. Build an `AuthenticatedUser` principal and an
   `UsernamePasswordAuthenticationToken` with authority
   `ROLE_<role>`, register with `SecurityContextHolder`.
5. Route matrix enforced via `@PreAuthorize("hasRole('...')")` on
   controllers; current matrix lives in `SecurityRoles.java` and
   is exercised by the `Negative Auth` Postman folder:
   - `POST /restaurants` / `PUT /restaurants/{id}` /
     `PATCH /restaurants/{id}/status` -> `RestaurantOwner` or
     `Admin`. 403 for `Customer`.
   - `GET /restaurants/{id}/availability` -> any authenticated
     user (used by Order Service on W1 hop 4).
   - `POST /menu-items/validate` -> any authenticated user (W1
     hop 5).
   - `POST/PUT/DELETE` menu items -> `RestaurantOwner` or `Admin`.

### 4.4 Negative evidence (live demo)

Postman `Negative Auth` folder runs through the 401/403/404/400/422
matrix live during the checkpoint. Representative assertions:

- `[401] GET /availability no token` -> status 401, response body
  is the `ErrorResponse` envelope (no leaky stack trace).
- `[401] garbage Bearer` -> status 401.
- `[403] POST /restaurants customer token` -> 403.
- `[403] PATCH /restaurants/{id}/status customer token` -> 403.

---

## 5. Where W1 crosses Sierra-Lima (availability + batch validate)

W1 is the synchronous place-order demo. Full chain lives in
[`0030-w1-synchronous-contract-lock.md`](./decisions/0030-w1-synchronous-contract-lock.md) §1.
Sierra-Lima is a **callee** on hops 4 and 5.

| Hop | From | To | Call | What Sierra-Lima returns |
|-----|------|----|------|--------------------------|
| 4   | Order Service | **Restaurant Service** | `GET /restaurants/{id}/availability` | `AvailabilityResponse` (isOpen, acceptsOrders, operatingHours, checkedAt) |
| 5   | Order Service | **Menu Service**       | `POST /menu-items/validate`          | `ValidateMenuItemsResponse` (allValid, per-line exists/available/price/error, totalAmount) |

Key contract decisions (Phase 9):

- **Closed restaurant** returns `200` with `acceptsOrders:false`,
  not a 409. Order Service aggregates the "cannot accept" signal
  in the same response shape as the healthy path
  (`0031-cross-service-status-code-table.md` §1.1).
- **Unknown / unavailable menu items** are carried as per-line
  errors in a `200` response with `allValid:false`. A single call
  returns every reason a batch failed, so Order does not have to
  loop or re-query. Error enum frozen in `0030` §5:
  `MENU_ITEM_NOT_FOUND`, `MENU_ITEM_NOT_AVAILABLE`.
- **Total amount** is computed server-side (price x quantity,
  summed over valid lines). Order hands this to Payment on hop 7
  without recomputing. This lock prevents the classic "client
  trusts client-side prices" vulnerability.

Demo fixtures (seed `V2` migrations):

- Open restaurant: `d0000001-0000-0000-0000-000000000001`
  (Pizza Antonio).
- Closed restaurant: `d0000003-0000-0000-0000-000000000003`
  (Cafe Nero).
- Unknown UUID sentinel: `ffffffff-ffff-ffff-ffff-ffffffffffff`.
- Valid items: `e0000012-...` (Quattro Formaggi, 10.50 EUR),
  `e0000013-...` (Tiramisu, 5.00 EUR) -> total 26.00 EUR for
  2 x Quattro + 1 x Tiramisu.
- Unavailable item: `e0000032-...` (Chocolate Cake,
  isAvailable=false).

Evidence: Postman `W1 Integration` folder, 9 requests,
40 assertions (Phase 10 verification §2 -- `0 failed`).

---

## 6. How async (W2 / W3) appears in the architecture, even though
Sierra-Lima does not code the broker

W2 (Order-status propagation) and W3 (Notifications) are event-
driven. `0032-w2-w3-event-contract-lock.md` pins the Kafka
envelope and topic names. Sierra-Lima is a **non-participant** at
the code level -- neither Restaurant nor Menu publishes or
consumes any of the W2/W3 events in the A3 baseline.

What Sierra-Lima talks to on CP#1:

- **Architecture slide** shows Kafka as the async backbone, with
  producer (Order Service) on one side and consumers (Delivery,
  Notification) on the other. Restaurant / Menu sit outside the
  event path; this is visible in Figure 4 of the A3 submission.
- **Postman collection** includes an `Async Evidence` folder. It is
  intentionally empty at CP#1 and is populated in Phase 16 by
  Mike-Alfa once the broker is running -- the folder exists so the
  placeholder does not look like an oversight during the demo.
- **If the grader asks "why doesn't your service publish anything?"**
  the answer is in `0002-workflows.md`: the workflows chosen for
  implementation (W1/W2/W3) do not require Restaurant or Menu to
  emit events. Adding one would be out-of-scope architectural
  change. If the menu or availability changes needed to propagate
  asynchronously (e.g., Order Service caching availability), that
  would be a W4 -- future work.

Sierra-Lima's architecture slide explicitly annotates this: "Menu
and Restaurant are synchronous callees. Async integration is
demonstrated via Order, Delivery, and Notification."

---

## 7. Live demo script

Read-through order. Backup recording (see
`dev-docs/checkpoint-1-backup/`) covers the same flow byte-for-byte
so the graders can see the exact same happy path even if the live
demo hits a glitch.

1. **Bring up the stack** (already running before the presentation
   starts):
   ```bash
   cd services/local-dev
   docker compose --env-file .env.local up -d
   ```
   Show `docker ps` -- four containers, all `Up (healthy)`.
2. **Login** (Postman `Auth (Tokens)` folder). Run "Mint Owner
   Token" -- point at the console showing the JWT, then open the
   payload at <https://jwt.io> or call out the `role:
   RestaurantOwner` claim in the console log.
3. **Create a restaurant** (Postman `Restaurant CRUD` ->
   `POST /restaurants`). Show the 201 response with the fresh
   UUID.
4. **Add a menu item** (Postman `Menu CRUD` ->
   `POST /restaurants/{rid}/menu-items`, using the UUID just
   captured).
5. **Toggle status** (Postman `Restaurant CRUD` ->
   `PATCH /restaurants/{id}/status`, body `{"isOpen": false}`, then
   again with `true`). Show the `isOpen` flip in the response.
6. **Hit availability** (Postman `W1 Integration` ->
   `[200 open] GET /restaurants/{open}/availability`). Point at
   `acceptsOrders: true`. Then run `[200 closed]` on Cafe Nero and
   show `acceptsOrders: false`.
7. **Hit batch validate** (Postman `W1 Integration` ->
   `[200 all valid]`). Show `allValid: true`, `totalAmount: 26.00`,
   `currency: EUR`. Then run `[200 missing]` to show the per-line
   `error: MENU_ITEM_NOT_FOUND` with `allValid: false`.
8. **Error paths** (Postman `Negative Auth` folder run via
   `newman --folder "Negative Auth"`). Call out the 401 / 403 /
   404 / 400 / 422 coverage on the summary table.
9. **Smoke script** (one terminal window, already open). Run
   `bash services/local-dev/smoke.sh` and point at the green
   "Sierra-Lima smoke test passed." line. This replays the
   Login -> Create -> Add item -> Toggle -> Availability ->
   Validate chain in under 10 seconds as an independent sanity
   check.

Estimated runtime of the scripted demo: 6-8 minutes.

### 7.1 If something breaks live

- Postman call fails -> play the backup recording from
  `dev-docs/checkpoint-1-backup/` and narrate over it.
- Services do not start -> open the last green run's Newman report
  from `dev-docs/verification/phase-10-verification_Charlie-Lima-Alfa.md`
  §8.3/8.4.
- `smoke.sh` fails -> fall back to running the Postman collection
  via the desktop app. Same coverage, less automation.

---

## 8. Reference pointers

- Contract locks: `0020`, `0030`, `0031`, `0032`, `0033` under
  `dev-docs/decisions/`.
- Phase-by-phase evidence: `dev-docs/verification/` (Phase 7, 8, 9,
  10, 11 for Sierra-Lima; Phase 2-6 for shared groundwork).
- Runbook: `services/local-dev/runbook.md`.
- Seed fixtures: `V2__seed_demo_data.sql` in each service.
- Postman collection: `services/local-dev/postman/QuickBite.postman_collection.json`.
- Smoke script: `services/local-dev/smoke.sh` (bash) and `smoke.ps1`
  (Windows PowerShell).
