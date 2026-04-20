# QuickBite -- Sierra-Lima Report (Phase 17)

Status: **Phase 17 presentation-ready draft.** Written on top of the
Phase 16 base commit `6b0e0e9`. Scope: Sierra-Lima's owned slice
(Restaurant Service, Menu Service, Vue 3 frontend) plus the
team-wide sections Sierra-Lima needs to present coherently at CP#3
(gateway routing, Kafka topics, team responsibilities). Other
callsigns' deep-dive sections will be stitched in by their authors
before the final assignment hand-in.

- **Audience.** Course graders (ESI 2026) evaluating Checkpoint #3
  on 2026-05-19. They already read Assignments 1-3; this report
  tells them what got **built**, which pieces diverged from the A3
  design, and what evidence backs each claim.
- **Source of truth for the code.** The commit tree at `6b0e0e9`
  on branch `dev`. Every numbered §X.Y in this document can be
  walked back to a file and line number in that tree.

---

## 0. Figure index

Figures live under `dev-docs/prior-submissions/` and were produced
for Assignment 3. Where the implementation has drifted, §13 records
the divergence and the in-report ASCII refresh governs.

| Figure | File | Used in | Status |
|--------|------|---------|--------|
| 1 | `assignment-3_figure1_business-architecture.png` | §2 | Reused -- business architecture is conceptual and still accurate. |
| 1b | `assignment-3_figure1b_implementation-architecture.png` | §3 | Reused with **divergences** noted in §13.1. ASCII refresh in §3.1 governs. |
| 2 | `assignment-3_figure2_service-er-diagrams.png` | §4 | Reused; tables in §4 govern for the two Sierra-Lima schemas. |
| 3 | `assignment-3_figure3_workflow-w1-sequence.png` | §6.1 | Reused. Sierra-Lima edge sequence in §6.1 matches. |
| 4 | `assignment-3_figure4_workflow-w2-w3-events.png` | §6.2 | Reused with **divergence** noted in §13.2 (the new `menu-events` topic Sierra-Lima emits on is not in Figure 4). |

---

## 1. Executive summary

QuickBite is a food-delivery platform partitioned into eight DDD
services. **Seven** are implemented in code (`Restaurant`, `Menu`,
`User`, `Order`, `Payment`, `Delivery`, `Notification`); the eighth
(`Review`) stays **design-only** -- it is out of scope for A3 and
dropping it frees a session for hardening the other seven, per §13.3.

The Sierra-Lima callsign owns `Restaurant Service`, `Menu Service`,
and the browser-facing Vue 3 frontend. Every one of the three has
shipped and is demonstrable end-to-end at Checkpoint #3:

- **Restaurant Service** (Spring Boot 3.3, Java 21, port 8081) --
  six endpoints, owner-scoped mutations, public browse, availability
  check for W1 hop 4.
- **Menu Service** (Spring Boot 3.3, Java 21, port 8082) -- six
  endpoints, owner-scoped mutations with cross-service ownership
  lookup, batch validation for W1 hop 5, log-only event emission
  on availability transitions (`menu.item-availability-changed`).
- **Frontend** (Vue 3, Vue Router 4, nginx-served SPA, port 8090) --
  sign-in, restaurant browse, menu browse, owner CRUD, and a cart
  that routes through Alfa-Kilo's API Gateway (or the opt-in
  `dev-gateway` stub during solo rehearsal).

Workflows demonstrable at CP#3:

- **W1 -- Place Order (synchronous, 8 hops).** Sierra-Lima owns
  hops 4 and 5; both have been exercised under Postman (40
  assertions green) and in the browser via the `dev-gateway` stub.
- **W2 -- Delivery Progress (asynchronous).** Teammate-owned (E-Y
  produces; A-K and M-A consume). Sierra-Lima is a stable
  non-participant; W1's synchronous chain to Sierra-Lima remains
  unchanged during any W2 run.
- **W3 -- Payment Outcome (asynchronous).** Teammate-owned (E-Y
  produces; A-K and M-A consume). Same posture as W2 for
  Sierra-Lima.
- **Menu availability transitions.** Owner-triggered; Sierra-Lima
  emits a structured JSON envelope on logger `menu-events` (log-only
  transport, Kafka-ready interface seam).

Security posture: JWT HS256 bearer tokens minted by User Service
(or, pre-User-Service, by the dev-only `JwtDevMint`). Token-relay
only -- neither Sierra-Lima service calls User Service; neither
re-signs a forwarded token. Role gating (Phase 15) enforces
`Customer` / `RestaurantOwner` / `Admin` on each mutating endpoint,
with an additional **ownership check** (the acting owner must
match the restaurant's `ownerId`, unless the caller is `Admin`).

Test evidence: **23/23** Restaurant Service JUnit tests,
**42/42** Menu Service JUnit tests, **9 Postman requests / 40
assertions** for W1, **14 Postman requests / 17 assertions** for
negative auth (Phases 7 + 15 combined), plus a cross-service smoke
script that writes a timestamped trace under
`services/local-dev/evidence/` each run.

---

## 2. Business architecture (Figure 1)

See `dev-docs/prior-submissions/assignment-3_figure1_business-architecture.png`.

The business architecture is the DDD decomposition from Assignment 2
-- eight bounded contexts around the ordering business process,
with no infrastructure concerns:

- `User` owns identity, credentials, roles.
- `Restaurant` owns restaurant profiles, location, and availability
  policy (open/closed, operating hours).
- `Menu` owns menu items, pricing, and availability.
- `Order` orchestrates the ordering business process.
- `Payment` owns payment outcomes.
- `Delivery` owns delivery lifecycle.
- `Notification` owns customer-facing notifications.
- `Review` owns post-order feedback (design-only in this submission).

Aggregate roots, value objects, and invariants are unchanged from
Assignment 2: `Restaurant` has an embedded `Location` value object;
`MenuItem` has an embedded `Price` value object; cross-context
references are by UUID with **no database foreign keys** across
service boundaries (§4.4).

This figure is intentionally infrastructure-free: no ports, no
brokers, no gateway. Figure 1b below adds those.

---

## 3. Technical architecture (Figure 1b)

Reference diagram:
`dev-docs/prior-submissions/assignment-3_figure1b_implementation-architecture.png`.
Two divergences from that figure are documented in §13.1 and
refreshed textually below.

### 3.1 Deployed components at CP#3

```
  Browser :8090 --> nginx (frontend) --/api/**--> API Gateway :8080
                                                   |
                                                   |-- /restaurants*   -->  Restaurant Service :8081 --> restaurant-db :5432
                                                   |-- /menu-items*    -->  Menu Service       :8082 --> menu-db       :5432
                                                   |-- /auth/login     -->  User Service       :808x (Alfa-Kilo)
                                                   |-- /orders*        -->  Order Service      :808x (Alfa-Kilo)
                                                   |-- /payments*      -->  Payment Service    :808x (Elephant-Yankee)
                                                   |-- /deliveries*    -->  Delivery Service   :808x (Elephant-Yankee)
                                                   `-- /notifications* -->  Notification Svc   :808x (Mike-Alfa)
                                                                |
                                      (Kafka)                  +---> Kafka broker :9092
                                          |
                                          +--- topics: order-events, payment-events,
                                                       delivery-events, notification-events
                                                       + menu-events (log-only on Sierra-Lima;
                                                         real topic post-CP#3, per ADR 0040 §2)

  Menu Service --(HTTP, service-token)--> Restaurant Service (GET /restaurants/{id})
  (Phase 15 addition: ownership resolution -- see §7.3 and §13.1)
```

**Divergences from Figure 1b (see §13.1):**

1. Menu Service now makes a synchronous HTTP call to Restaurant
   Service to resolve `ownerId` when authorising mutations. This
   line is not in Figure 1b.
2. A `dev-gateway` stub (`nginx` profile) exists for Sierra-Lima
   solo rehearsal when Alfa-Kilo's real gateway is not yet in the
   compose network. It matches the real gateway's routing table
   (§3.3) and is the only arrow that changes when the real gateway
   lands (the arrow just re-points).
3. The `menu-events` topic is present only as a log stream on
   Sierra-Lima's side (Phase 16 stretch, log-only transport -- ADR
   0040 §2). Figure 4 does not show it.

### 3.2 Runtime container set (Docker Compose at CP#3)

Five long-lived containers in the canonical stack, one opt-in stub:

| Container | Image | Port (host) | Purpose |
|-----------|-------|-------------|---------|
| `quickbite-restaurant-db` | `postgres:15` | 5432 | Restaurant Service's database. |
| `quickbite-menu-db` | `postgres:15` | 5433 | Menu Service's database. |
| `quickbite-restaurant-service` | `quickbite/restaurant-service:local` | 8081 | Spring Boot 3.3 on Java 21. |
| `quickbite-menu-service` | `quickbite/menu-service:local` | 8082 | Spring Boot 3.3 on Java 21. |
| `quickbite-frontend` | `quickbite/frontend:local` | 8090 | Vue 3 SPA served by nginx 1.27. |
| `quickbite-dev-gateway` | `nginx:1.27-alpine` | 8080 | Opt-in stub (`--profile dev-gateway`) for solo rehearsal. |

All five share the Docker bridge network `quickbite-net` and resolve
each other by container name. Spring Boot `application-docker.properties`
is selected by `SPRING_PROFILES_ACTIVE=docker`; application-level
configuration (DB URL, JWT secret, `RESTAURANT_SERVICE_BASE_URL`)
is env-driven so the same image boots locally, in compose, and
eventually in the shared team stack.

### 3.3 Gateway routing (team-owned, relevant to Sierra-Lima)

Per decision 0020 §10, Alfa-Kilo's API Gateway strips `/api` and
forwards to:

| Path prefix | Upstream |
|-------------|----------|
| `/api/restaurants/{id}/menu-items` | `menu-service:8082` |
| `/api/restaurants/**` | `restaurant-service:8081` |
| `/api/menu-items/**` | `menu-service:8082` |
| `/api/auth/login` | `user-service:808x` |
| `/api/users/**` | `user-service:808x` |
| `/api/orders/**` | `order-service:808x` |
| `/api/payments/**` | `payment-service:808x` |
| `/api/deliveries/**` | `delivery-service:808x` |
| `/api/notifications/**` | `notification-service:808x` |

The gateway-first-match is deliberate: `/api/restaurants/{id}/menu-items`
must appear **before** `/api/restaurants/**` in the configuration
so nested menu-item routes reach Menu Service, not Restaurant
Service. The `dev-gateway` stub (`services/local-dev/dev-gateway/nginx.conf`)
enforces the same ordering.

Sierra-Lima does not configure the real gateway. When the gateway
is unavailable, Sierra-Lima runs with `COMPOSE_PROFILES=dev-gateway`
and `GATEWAY_UPSTREAM=http://dev-gateway:80`; that is the only
env-var change required.

---

## 4. Data models (Figure 2)

Reference diagram:
`dev-docs/prior-submissions/assignment-3_figure2_service-er-diagrams.png`.
The tables below are the authoritative column lists for
Sierra-Lima's schemas -- they match `V1__init.sql` in each service's
Flyway migrations.

### 4.1 Restaurant Service (`restaurant_db.restaurant`)

| Column | Type | Notes |
|--------|------|-------|
| `restaurant_id` | UUID, PK | Generated server-side on `POST /restaurants`. |
| `owner_id` | UUID, NOT NULL | Cross-service reference to `User.userId`. No FK. |
| `name` | VARCHAR(255), NOT NULL | DTO caps at 255 (`@Size(max = 255)`). Uniqueness per owner enforced in the service layer (`DuplicateRestaurantException` -> 409). |
| `address` | VARCHAR(255) | Part of embedded `Location`; optional. |
| `city` | VARCHAR(120) | Part of embedded `Location`; optional, indexed for `?city=` filter. |
| `latitude`, `longitude` | DOUBLE PRECISION | Embedded `Location`, validated `[-90, 90]` / `[-180, 180]`. |
| `operating_hours` | VARCHAR(20) | `HH:MM-HH:MM`; server-parsed in `RestaurantService.isWithinOperatingHours(...)` for availability checks. |
| `is_open` | BOOLEAN, NOT NULL DEFAULT FALSE | Owner-controlled day-to-day flag. |
| `created_at`, `updated_at` | TIMESTAMP, NOT NULL | Hibernate `@EntityListeners(AuditingEntityListener.class)`. |

Indexes: `idx_restaurant_city`, `idx_restaurant_owner`.

Validation rules live in `CreateRestaurantRequest` /
`UpdateRestaurantRequest` with Jakarta annotations (`@NotBlank`,
`@Pattern` on operating hours, `@DecimalMin` / `@DecimalMax` on
latitude / longitude).

### 4.2 Menu Service (`menu_db.menu_item`)

| Column | Type | Notes |
|--------|------|-------|
| `menu_item_id` | UUID, PK | Generated server-side. |
| `restaurant_id` | UUID, NOT NULL | Cross-service reference to `Restaurant.restaurant_id`. No FK. |
| `name` | VARCHAR(255), NOT NULL | DTO caps at 255. |
| `description` | VARCHAR(2000) | Optional. |
| `price_amount` | NUMERIC(19,2), NOT NULL, CHECK (`price_amount > 0`) | <= 2 decimal places also enforced at DTO + service layer. |
| `price_currency` | VARCHAR(3), NOT NULL, DEFAULT `'EUR'` | ISO-4217. `EUR` throughout seed. |
| `category` | VARCHAR(100), NOT NULL | Free-form; UI hint set: `Appetizer`, `Main`, `Dessert`, `Drink`. |
| `is_available` | BOOLEAN, NOT NULL, DEFAULT TRUE | Owner-controlled; toggling triggers a `menu.item-availability-changed` emit (§7.4). |
| `created_at`, `updated_at` | TIMESTAMP, NOT NULL | Hibernate auditing, as above. |

Indexes: `idx_menu_item_restaurant`, `idx_menu_item_category`.

`Price` is an `@Embeddable` value object -- matches the A2 DDD
model. Business-rule violations (`price <= 0`, `> 2` decimals)
raise `InvalidPriceException` -> 422.

### 4.3 Seed data

Both services ship a `V2__seed_demo_data.sql` migration with fixed
UUIDs so Postman and the smoke scripts have stable IDs. Sample
owners: `00000000-0000-0000-0000-000000000099` (Pizza Antonio owner),
`00000000-0000-0000-0000-000000000098` (Sushi Yuki owner). The Postman
environment pins these as `ownerA` and `ownerB` for the Phase 15
negative-auth matrix (owner-A token on owner-B's restaurant -> 403).

### 4.4 Cross-service referential integrity

No database-enforced referential integrity across services. This
is intentional (ADR 0001 / 0020 §6): each service owns its schema
end-to-end, and deleting a restaurant does **not** cascade into
Menu Service. Orphaned menu items are the acceptable trade-off for
deploy independence; referential integrity across service
boundaries is the event bus's job, not Postgres's. The cleanup
path (not shipped in A3, design-only) would be a
`restaurant.deleted` event that Menu Service consumes to soft-delete
affected items; this is listed as future work in §14.

---

## 5. APIs

Both services expose OpenAPI at `/v3/api-docs` and Swagger UI at
`/swagger-ui.html`. All endpoints are documented with `@Operation`
+ `@ApiResponses` annotations on the controllers. Screenshots live
in the evidence appendix (§15).

### 5.1 Restaurant Service endpoints (six)

| Verb | Path | Auth (Phase 15) | Purpose |
|------|------|-----------------|---------|
| `POST` | `/restaurants` | `RestaurantOwner` / `Admin` | Create. Returns 201 + `Location: /restaurants/{id}`. |
| `GET` | `/restaurants` | public | List with optional `?city=` and `?isOpen=`. |
| `GET` | `/restaurants/{id}` | public | Read. 404 if unknown. |
| `PUT` | `/restaurants/{id}` | Owner of this restaurant / `Admin` | Replace name / location / operating hours. |
| `PATCH` | `/restaurants/{id}/status` | Owner of this restaurant / `Admin` | Toggle `isOpen`. |
| `GET` | `/restaurants/{id}/availability` | Any authenticated role + `SERVICE` | **W1 hop 4.** Returns `AvailabilityResponse`. |

Implementation: `RestaurantController`
(`services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantController.java`).

### 5.2 Menu Service endpoints (six)

| Verb | Path | Auth (Phase 15) | Purpose |
|------|------|-----------------|---------|
| `POST` | `/restaurants/{rid}/menu-items` | Owner of the parent restaurant / `Admin` | Create item under a restaurant. |
| `GET` | `/restaurants/{rid}/menu-items` | public | List items (optional `?category=`, `?available=`). |
| `GET` | `/menu-items/{id}` | public | Read single item. |
| `PUT` | `/menu-items/{id}` | Owner of the parent restaurant / `Admin` | Replace item. A toggle on `isAvailable` emits `menu.item-availability-changed` (§7.4). |
| `DELETE` | `/menu-items/{id}` | Owner of the parent restaurant / `Admin` | Hard delete. |
| `POST` | `/menu-items/validate` | Any authenticated role + `SERVICE` | **W1 hop 5.** Batch existence + availability + pricing. |

Implementation: `MenuController`
(`services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/controller/MenuController.java`).

### 5.3 Response envelopes

**Shared error envelope** (`ErrorResponse`):

```json
{
  "timestamp": "2026-05-05T10:15:30+02:00",
  "status": 404,
  "error":  "Not Found",
  "message": "Restaurant not found: d0000099-...",
  "path":    "/restaurants/d0000099-.../availability",
  "validationErrors": null
}
```

`validationErrors` is populated for Jakarta Validation failures:

```json
"validationErrors": [
  { "field": "priceAmount",    "message": "must be greater than 0" },
  { "field": "priceCurrency",  "message": "must match \"[A-Z]{3}\"" }
]
```

**W1 hop 4 response** (`AvailabilityResponse`):

```json
{
  "restaurantId":     "d0000099-...",
  "isOpen":           true,
  "acceptsOrders":    true,
  "operatingHours":   "11:00-22:00",
  "checkedAt":        "2026-05-05T10:15:30Z"
}
```

`acceptsOrders` is `true` iff `isOpen && within(operatingHours, now())`.
`checkedAt` is derived from an injected `Clock` (testable).

**W1 hop 5 response** (`ValidateMenuItemsResponse`):

```json
{
  "allValid": true,
  "items": [
    {
      "menuItemId":        "e0000010-...",
      "quantity":          2,
      "exists":            true,
      "isAvailable":       true,
      "unitPriceAmount":   12.50,
      "unitPriceCurrency": "EUR",
      "lineTotal":         25.00,
      "error":             null
    }
  ],
  "totalAmount":   25.00,
  "totalCurrency": "EUR"
}
```

Per-line errors use the stable strings `NOT_FOUND` and `NOT_AVAILABLE`
(`ValidateMenuItemsResponse.ERROR_*`). Total currency is taken from
the first available line; if no lines exist, it defaults to `EUR`.

Full W1 contract is locked in ADR 0030 §3 (availability) and §4
(validate).

---

## 6. Workflows

### 6.1 W1 -- Place Order (synchronous; Figure 3)

Reference diagram:
`dev-docs/prior-submissions/assignment-3_figure3_workflow-w1-sequence.png`.

Full chain is eight hops (ADR 0030 §1). Sierra-Lima owns hops 4
and 5:

```
Client -> Gateway -> Order
                      |-- hop 4 --> Restaurant.GET /restaurants/{id}/availability
                      |             <- AvailabilityResponse
                      |-- hop 5 --> Menu.POST /menu-items/validate
                      |             <- ValidateMenuItemsResponse
                      |-- hop 6 --> (local) Order persists row
                      |-- hop 7 --> Payment
                      |-- hop 8 --> Delivery
```

Token propagation: Order Service forwards the caller's JWT verbatim
(ADR 0033 §2). Sierra-Lima signature-verifies, extracts claims, and
authorises; it does not re-sign, never calls User Service, and never
mints. Menu Service's Phase 15 ownership lookup (§7.3) is the only
outbound synchronous call Sierra-Lima makes; it uses the forwarded
token as its bearer credential.

Failure semantics (ADR 0031):

- Sierra-Lima returns 404 if the restaurant or a menu item does
  not exist; Order Service maps this to a client-visible 422 on
  the order envelope.
- Sierra-Lima returns 422 on invalid price or validation errors;
  Order propagates the 422 verbatim.
- Sierra-Lima returns 503 if a dependency (e.g., the database) is
  unreachable; Order's Resilience4j circuit breaker trips after
  three consecutive failures (Alfa-Kilo).

Evidence: Postman `W1 Integration` folder, 9 requests, 40
assertions (`phase-10-verification_Charlie-Lima-Alfa.md` §2, §8.3).
The cross-service smoke script (§7.5) exercises hops 4-5 end-to-end.

### 6.2 W2 / W3 -- Asynchronous (Figure 4)

Reference diagram:
`dev-docs/prior-submissions/assignment-3_figure4_workflow-w2-w3-events.png`.

Sierra-Lima is neither a producer nor a consumer in the A3 baseline
event topology (ADR 0032 §1). The topic table governs:

| Topic | Producer | Consumers | Envelope (ADR 0032) |
|-------|----------|-----------|---------------------|
| `order-events` | Order (A-K) | Notification (M-A), Delivery (E-Y) | §2 |
| `payment-events` | Payment (E-Y) | Order (A-K), Notification (M-A) | §3 |
| `delivery-events` | Delivery (E-Y) | Order (A-K), Notification (M-A) | §4 |
| `notification-events` | Notification (M-A) | (terminal; no consumer) | §5 |
| `menu-events` (Phase 16) | Menu Service (S-L) | (none in A3; log-only transport) | §6 |

W2 (delivery progress) and W3 (payment outcome) are teammate-owned:
Payment / Delivery produce; Order and Notification consume. The
Sierra-Lima baseline posture (ADR 0040 §1) is that Restaurant and
Menu endpoints stay stable during any W2 / W3 run so the synchronous
W1 chain keeps working. This is the Phase 16 DoD's "Sierra-Lima's
services stay stable during the integrated flow" bullet.

Sierra-Lima's only emit is on `menu-events` (§7.4), driven by the
Phase 16 stretch producer. W2 / W3 themselves are demonstrated by
teammate services at CP#3; Sierra-Lima's smoke script (§7.5)
captures the full trace including teammate probes.

Envelope shape for all topics (Kafka key + JSON value):

```
record key:   <aggregate id, e.g., orderId / paymentId / deliveryId / menuItemId>
record value: {
  "id":         "<UUID v4>",
  "type":       "<event-type, e.g., payment.completed>",
  "occurredAt": "<ISO-8601 UTC>",
  "payload":    { ...type-specific fields... }
}
```

Idempotency posture (ADR 0032 §7): at-least-once delivery, consumers
dedupe by `envelope.id`. Sierra-Lima's log-only `menu-events` emit
generates a fresh `id = UUID.randomUUID()` per transition, so
downstream consumers (when wired) can dedupe safely.

---

## 7. Security

Full detail is in ADR 0010 (auth contract) and ADR 0033 (token
relay); this section summarises the enforced posture at CP#3.

### 7.1 JWT validation

- **Algorithm.** HS256, base64-encoded shared secret via the
  `JWT_SECRET` env var. Both Sierra-Lima services load the same
  secret in production; a dev default is hard-coded only in
  `docker-compose.yml` for local runs.
- **Filter.** `JwtAuthFilter extends OncePerRequestFilter`, wired
  before `UsernamePasswordAuthenticationFilter`
  (`SecurityConfig.filterChain(...)`).
- **Claims extracted.** `userId` (UUID), `role` (one of `Customer`,
  `RestaurantOwner`, `Driver`, `Admin`), `tokenType` (`USER` or
  `SERVICE`), and optional `serviceName`.
- **Failure mapping.** Missing / invalid / expired token, or bad
  signature, yield **401** via `RestAuthEntryPoints.unauthorizedEntryPoint()`;
  the `ErrorResponse` envelope is written inline from
  `JwtAuthFilter.writeUnauthorized(...)`.

### 7.2 Role gating (Phase 15)

`SecurityConfig` pins the route matrix:

| Service | Endpoint class | Authorisation |
|---------|----------------|---------------|
| Restaurant | Public GETs on `/restaurants` + `/restaurants/{id}` | `permitAll()` |
| Restaurant | `GET /restaurants/{id}/availability` | `.authenticated()` (any role + SERVICE) |
| Restaurant | `POST /restaurants`, `PUT /restaurants/{id}`, `PATCH /restaurants/{id}/status` | `.hasAnyRole(RESTAURANT_OWNER, ADMIN)` |
| Menu | Public GETs on `/restaurants/{rid}/menu-items` + `/menu-items/{id}` | `permitAll()` |
| Menu | `POST /menu-items/validate` | `.authenticated()` (any role + SERVICE) |
| Menu | `POST /restaurants/{rid}/menu-items`, `PUT /menu-items/{id}`, `DELETE /menu-items/{id}` | `.hasAnyRole(RESTAURANT_OWNER, ADMIN)` |

Everything else defaults to `.anyRequest().authenticated()`.
Actuator (`/actuator/health`, `/actuator/info`), OpenAPI
(`/v3/api-docs/**`), Swagger UI (`/swagger-ui/**`), and OPTIONS
preflights are unconditionally permitted.

### 7.3 Ownership enforcement (Phase 15)

Role gating alone would let owner A mutate owner B's restaurant as
long as both have role `RestaurantOwner`. Phase 15 closes this:

- **Restaurant Service** checks `actor.userId == restaurant.ownerId`
  in `RestaurantService.requireOwnerOrAdmin(...)` for `PUT /{id}`
  and `PATCH /{id}/status`. `Admin` bypasses the check.
- **Menu Service** must resolve the parent restaurant's `ownerId`
  before authorising a mutation. Since Menu owns no restaurant
  data, it calls `RestaurantOwnershipClient.findOwnerId(restaurantId)`
  which issues a `GET /restaurants/{id}` to Restaurant Service and
  extracts the `ownerId` from the DTO. If the restaurant is absent,
  the menu call resolves to 404 via
  `RestaurantNotFoundForMenuException`. If Restaurant Service is
  reachable but returns a 5xx, an `OwningRestaurantLookupException`
  maps to 503.

This is the only synchronous service-to-service call Sierra-Lima
makes. It is unary, cheap, and cached nowhere yet; the acceptable
trade-off is documented in ADR 0040 §1 ("no further action needed
on the baseline side for CP#3").

**Denial logging** (Phase 15 DoD): every ownership rejection writes
a `WARN` log line of the form

```
ownership denial actor=<userId> role=<role> endpoint=<verb path>
restaurantId=<rid> ownerId=<oid>
```

from `RestaurantService.requireOwnerOrAdmin` /
`MenuService.requireOwnerOrAdmin`. The log line is grep-friendly
for the CP#3 demo tail.

### 7.4 Menu availability event (Phase 16)

`MenuService.update(...)` captures `previousAvailability` before
mutating the item; if the flag flipped, it builds an
`AvailabilityChangedEvent` and invokes
`MenuEventPublisher.publishAvailabilityChanged(event)`. The default
implementation is `LoggingMenuEventPublisher`:

```
INFO  menu-events : topic=menu-events key=<menuItemId> envelope={
  "id":         "<UUID v4>",
  "type":       "menu.item-availability-changed",
  "occurredAt": "2026-05-05T10:15:30Z",
  "payload": {
    "menuItemId":          "<UUID>",
    "restaurantId":        "<UUID>",
    "isAvailable":         false,
    "previousIsAvailable": true
  }
}
```

Publisher failure is caught in `MenuService.publishAvailabilityChanged(...)`
and logged at `WARN`; the DB write is never rolled back (ADR 0040 §4,
ADR 0032 §7 at-least-once posture). No event is emitted on create,
delete, or updates that leave `isAvailable` unchanged.

The `MenuEventPublisher` interface is the Kafka-swap seam. A future
`KafkaMenuEventPublisher @Component` would replace the logging one
without any change to `MenuService` or to the envelope bytes.

### 7.5 Cross-service smoke (Phase 16)

`services/local-dev/smoke-cross-service.{sh,ps1}` runs the full
W1-through-W3 trace when teammate URLs are provided via env vars
(`USER_BASE`, `ORDER_BASE`, `PAYMENT_BASE`, `DELIVERY_BASE`,
`NOTIFICATION_BASE`). Unset URLs produce `SKIP:` lines; set-but-
unreachable URLs produce `TEAMMATE-FAIL`. The script writes a
timestamped trace to
`services/local-dev/evidence/cross-service-smoke_<RUN_TAG>.log`
and, when the `quickbite-menu-service` container is reachable,
a grepped `menu-events` envelope slice to
`services/local-dev/evidence/menu-events_<RUN_TAG>.log`.

Exit codes: `0` all green; `1` Sierra-Lima-owned step failed;
`2` Sierra-Lima OK but a configured teammate probe failed. This
distinction keeps the script usable during solo rehearsal.

### 7.6 Dev-only token minting

`JwtDevMint.java` (in each service) mints tokens with the same
HS256 secret for local Postman / smoke runs until Alfa-Kilo's real
`POST /api/auth/login` ships. The Postman collection's pre-request
script mirrors the Java logic byte-for-byte. ADR 0010 §4 notes
this is dev-only; both classes are removed at the moment User
Service is integrated (a one-commit deletion).

---

## 8. Frontend architecture (Phases 12-14)

Sierra-Lima's browser surface is a single-page Vue 3 app that
shipped across three phases: shell + router + sign-in (Phase 12),
restaurant and menu-item UX (Phase 13), Docker packaging and
end-to-end integration (Phase 14). It replaces neither gateway
nor service: the browser talks only to nginx on the same origin,
which reverse-proxies `/api/**` to the API Gateway.

### 8.1 Layout

- **Framework.** Vue 3 + Vue Router 4, Vue CLI 5 build pipeline.
  Source at `services/frontend/quickbite-frontend/`.
- **Views** (`src/views/`): `HomeView`, `LoginView`, `SignupView`,
  `RestaurantListView`, `RestaurantDetailView`, `AddRestaurantView`,
  `MenuView`, `AddMenuItemView`, `MenuItemDetailView`, `CartView`,
  `OrderStatusView`, `NotFoundView`.
- **Shared** (`src/components/AppNav.vue`) -- top nav with a
  sign-in / sign-out toggle gated on `isAuthenticated()`.
- **Routing** (`src/router/index.js`). `createWebHistory` mode. A
  single `router.beforeEach` guard honours two route-meta flags:
  `requiresAuth` redirects anonymous visitors to
  `/login?next=<fullPath>`; `hideWhenAuthed` sends an already
  signed-in user away from `/login` and `/signup`.
- **Role visibility** (Phase 13 polish, locked in Phase 15):
  owner-only actions (create restaurant, edit menu, toggle
  availability) are rendered only when
  `getRole() === 'RestaurantOwner' || getRole() === 'Admin'`. The
  backend still enforces the same rule server-side (§7.2-§7.3) --
  the client-side gating is purely cosmetic.

### 8.2 API client and token handling

- `src/api/client.js` exposes an `apiFetch` wrapper and a
  `{ get, post, put, patch, delete }` convenience object. It
  attaches `Authorization: Bearer <token>` from localStorage to
  every call, normalises 401 into a clear-token +
  redirect-to-login flow, and wraps every failure in an `ApiError`
  so views can show the error envelope's `message` directly.
- **Base URL resolution.** `VUE_APP_API_BASE_URL` baked at build
  time. An **empty string** means same-origin (the nginx container
  serves both the SPA and `/api/**`); an **unset** variable falls
  back to `http://localhost:8080` (the dev gateway path for
  `npm run serve`). The Docker build uses the empty-string branch
  via `.env.production`.
- `src/auth/token.js` persists the JWT in `localStorage.quickbite.jwt`.
  Sign-in POSTs `/api/auth/login` and writes the returned token;
  sign-out clears the key. Pre-User-Service, the developer mints
  a dev token with `JwtDevMint.java` and pastes it into
  `localStorage` manually -- same HS256 secret, so the Spring
  filters accept it.

### 8.3 Container packaging

- **Multi-stage Dockerfile** (`Dockerfile`): stage 1 is
  `node:20-alpine` running `npm ci && npm run build`; stage 2 is
  `nginx:1.27-alpine` serving the compiled `dist/` and an
  envsubst-rendered `nginx.conf.template`.
- **nginx config** (`nginx.conf.template`): two locations. `/`
  with an HTML5-history fallback
  (`try_files $uri $uri/ /index.html`) so the Vue router can
  deep-link; `/api/` reverse-proxying to `${GATEWAY_UPSTREAM}`.
  The `resolver 127.0.0.11 valid=30s ipv6=off` + `set $upstream
  ...; proxy_pass $upstream` idiom lets the container boot even
  when the gateway's DNS name is not yet resolvable.
- **Compose wiring.** The `frontend` service in
  `services/local-dev/docker-compose.yml` maps host
  `${FRONTEND_HOST_PORT:-8090}` to container port 80, sets
  `GATEWAY_UPSTREAM`, joins `quickbite-net`, and runs a
  `wget -qO- http://localhost/` healthcheck.

### 8.4 Dev-gateway stub

Alfa-Kilo's real gateway is not yet merged at the time of the CP#2
cut. The compose file carries a `dev-gateway` service behind the
`dev-gateway` compose profile so Sierra-Lima can demonstrate the
full-stack flow without it:

- `nginx:1.27-alpine` driven by `services/local-dev/dev-gateway/nginx.conf`.
- Mirrors the real gateway's routing table (§3.3) exactly, including
  the first-match ordering of `/api/restaurants/{id}/menu-items`
  before `/api/restaurants/**`.
- Strips `/api` before dispatch.
- Does **not** implement `/api/auth/login` -- that belongs to User
  Service. Runbook documents the dev-JWT paste path.
- Opt in with `docker compose --profile dev-gateway up -d` (or
  `COMPOSE_PROFILES=dev-gateway`) once `GATEWAY_UPSTREAM` is set
  to `http://dev-gateway:80`.

### 8.5 End-to-end flow walked in the browser

- Sign in (`LoginView`) -> token persisted.
- Browse restaurants (`RestaurantListView`) -> `GET /api/restaurants`.
- Open a restaurant (`RestaurantDetailView`) -> `GET /api/restaurants/{id}`.
- (Owner) toggle status (`PATCH /api/restaurants/{id}/status`).
- Browse menu (`MenuView`) -> `GET /api/restaurants/{rid}/menu-items`.
- (Owner) add menu item (`AddMenuItemView`) ->
  `POST /api/restaurants/{rid}/menu-items`.
- (Owner) edit menu item (`MenuItemDetailView`) ->
  `PUT /api/menu-items/{id}`. Toggling `isAvailable` here is the
  Phase 16 emit trigger (§7.4).
- (Customer) add to cart (`CartView`) -> client-only until Order
  Service is live. Phase 14 verification walked this path via the
  `dev-gateway` stub.

---

## 9. Integration mechanisms in use

| Mechanism | Technology | Sierra-Lima touchpoint |
|-----------|------------|------------------------|
| REST (JSON) | Spring Web MVC, Jackson | All endpoints in §5. |
| OpenAPI / Swagger UI | `springdoc-openapi` | Docs on both services (§5, §15.1). |
| JWT bearer | `jjwt` library | §7.1. |
| Service-to-service HTTP | Spring `RestClient` | Menu -> Restaurant ownership lookup (§7.3). |
| Kafka topic contract | Spring Kafka (M-A owns the broker) | Envelope lock in ADR 0032; Sierra-Lima does not bind `spring-kafka` on the classpath. |
| Log-only event transport | SLF4J + Logback | `LoggingMenuEventPublisher` (§7.4) -- swap seam for a future Kafka publisher. |
| Database migrations | Flyway | Per service: `V1__init.sql`, `V2__seed_demo_data.sql`. |
| Gateway routing | nginx (real gateway owned by A-K; opt-in stub in §8.4) | §3.3. Sierra-Lima does not configure routing. |
| Health / readiness | Spring Boot Actuator | `/actuator/health`. Compose `depends_on: condition: service_healthy`. |

---

## 10. Tests and evidence

### 10.1 JUnit

| Layer | Count | Home |
|-------|-------|------|
| Restaurant Service -- unit (service layer) | 8 | `service.RestaurantServiceTest` |
| Restaurant Service -- slice (MockMvc) | 14 | `controller.RestaurantControllerTest` |
| Restaurant Service -- context load | 1 | `RestaurantServiceApplicationTests` |
| **Restaurant Service total** | **23** | `(restaurant-service) mvn test` |
| Menu Service -- unit (service layer) | 20 | `service.MenuServiceTest` |
| Menu Service -- slice (MockMvc) | 20 | `controller.MenuControllerTest` |
| Menu Service -- events | 1 | `events.LoggingMenuEventPublisherTest` |
| Menu Service -- context load | 1 | `MenuServiceApplicationTests` |
| **Menu Service total** | **42** | `(menu-service) mvn test` |

Phase 15 added the owner/admin matrix tests; Phase 16 added 6 emit-path
tests and the `LoggingMenuEventPublisher` Logback `ListAppender`
assertion. Both phases' verification notes reproduce the test
counts (`phase-16-verification_Sierra-Lima.md` §5).

### 10.2 Postman / Newman

Collection: `services/local-dev/postman/QuickBite.postman_collection.json`.

| Folder | Requests | Assertions |
|--------|----------|------------|
| W1 Integration (Phase 10) | 9 | 40 |
| Negative Auth (Phase 7 + Phase 15) | 14 | 17 |

Phase 15 added 6 new cases: owner-A on owner-B's restaurant (-> 403,
at both Restaurant and Menu endpoints); admin unconditionally ->
200; missing Authorization header -> 401; customer token on an
owner-only endpoint -> 403.

### 10.3 Smoke scripts

- `services/local-dev/smoke.{sh,ps1}` -- Phase 9 baseline, W1
  happy-path (7 HTTP round-trips).
- `services/local-dev/smoke-cross-service.{sh,ps1}` -- Phase 16,
  full trace with teammate probes (§7.5). Writes timestamped
  traces to `services/local-dev/evidence/` and exits 0/1/2 to
  distinguish Sierra-Lima failures from teammate gaps.

### 10.4 Browser walkthrough

Phase 14 verification walked sign-in -> list -> detail -> menu ->
add/edit/delete -> status toggle -> W1 hops 4-5 via the dev-gateway
stub (`phase-14-verification_Sierra-Lima.md`).

### 10.5 CI status

There is no CI runner in this repo: verification is local (`mvn test`
+ `smoke-cross-service.sh` + manual browser walk). The Phase 17 DoD
does not require CI, and the master plan (§8 "Non-goals") excludes
CI pipelines from A3 scope.

---

## 11. Implementation status: what shipped vs what is design-only

### 11.1 Services in code (7)

| Service | Callsign | Status at CP#3 |
|---------|----------|----------------|
| Restaurant Service | Sierra-Lima | Implemented; §3-§7, §15.1. |
| Menu Service | Sierra-Lima | Implemented; §3-§7, §15.1. |
| User Service | Alfa-Kilo | Implemented (per team report); mints JWTs consumed by all services. |
| Order Service | Alfa-Kilo | Implemented (per team report); orchestrates W1 hops 4/5 into Sierra-Lima. |
| Payment Service | Elephant-Yankee | Implemented (per team report); produces `payment.completed` / `payment.failed`. |
| Delivery Service | Elephant-Yankee | Implemented (per team report); produces `delivery.status-changed`. |
| Notification Service | Mike-Alfa | Implemented (per team report); consumes all team topics. |

### 11.2 Design-only (1)

`Review Service` is **design-only** in this submission.

- **Scope.** Post-order customer feedback; `Review` aggregate with
  an `OrderRef` reference value object (A2 §4).
- **Justification.** A3 §2 sets the implementation bar at "at
  least five services end-to-end with at least one async workflow."
  The team decided (captured in ADR 0001 §3) that the marginal
  return of a seventh implemented service was lower than the
  marginal return of hardening Restaurant + Menu + Order + User
  + Payment + Delivery + Notification: without Review, all four
  callsigns have a clean code + test pack to defend at CP#3; with
  Review, one callsign ships a thin shell.
- **What the report still covers.** The business-context diagram
  (Figure 1) shows the `Review` bounded context so the grader sees
  it was modelled; §13.3 tracks it in the limitations section so
  the absence is not read as an oversight.
- **Migration path.** When Review is eventually built, it owns a
  `review_db` with a `review` table keyed on `(order_id, customer_id)`.
  It exposes `POST /orders/{id}/reviews`, `GET /restaurants/{id}/reviews`,
  and consumes `order-events` to gate review eligibility on
  `order.delivered`. No Sierra-Lima schema change is needed.

---

## 12. Team responsibilities

Group 7 is four callsigns; each owns one or two services. This
report is written from the Sierra-Lima perspective; team-level
authorship is shared.

| Callsign | Services owned | Primary responsibilities at CP#3 |
|----------|----------------|---------------------------------|
| Sierra-Lima | Restaurant Service, Menu Service, Frontend SPA | Services §3-§7; frontend §8; dev-gateway stub §8.4; Phase 16 async stretch (§7.4). |
| Alfa-Kilo | User Service, Order Service, API Gateway | Token issuance; W1 orchestration; gateway routing (§3.3). |
| Elephant-Yankee | Payment Service, Delivery Service | W3 producer; W2 producer (ADR 0032 §3-§4). |
| Mike-Alfa | Kafka broker, Notification Service | Event transport; W2 / W3 consumer-of-last-resort. |

Sierra-Lima's co-authorship of the shared artefacts:

- **Dev-gateway stub** (`services/local-dev/dev-gateway/nginx.conf`)
  -- owned by Sierra-Lima; mirrors A-K's routing table and is
  swapped out the moment the real gateway lands.
- **Decision ledger** -- Sierra-Lima authored ADRs 0001, 0010,
  0020, 0030, 0033, 0040; 0032 is a team lock.
- **Local-dev runbook** (`services/local-dev/runbook.md`) -- owned
  by Sierra-Lima; documents compose startup, dev JWT mint, and
  smoke runs for the whole team.

---

## 13. Divergences from Assignment 3 (Figures 1b, 4)

### 13.1 Figure 1b divergences (technical architecture)

The implementation architecture has three arrows / boxes that do
not appear in `assignment-3_figure1b_implementation-architecture.png`.
They are covered by the ASCII refresh in §3.1 and summarised here:

1. **Menu -> Restaurant synchronous ownership lookup.** Phase 15
   added a `RestaurantOwnershipClient` (Spring `RestClient`) in
   Menu Service that calls `GET /restaurants/{id}` to resolve
   `ownerId` before authorising menu-item mutations. This is the
   only outbound synchronous dependency Sierra-Lima has; it is
   documented in §7.3 and ADR 0040 §1. Figure 1b omitted it because
   the A3 design treated ownership as a claim carried in the JWT;
   Phase 15 opted for an authoritative server-side lookup instead
   (a JWT-only check assumes the issuer never signs a stale claim,
   which we judged too risky).

2. **Dev-gateway stub (`nginx`, opt-in).** Sierra-Lima added an
   `nginx:1.27-alpine` container behind the `dev-gateway` compose
   profile so solo rehearsal does not require Alfa-Kilo's real
   gateway (§8.4). It is opt-in: the canonical stack does not run
   it. When the real gateway lands, `COMPOSE_PROFILES` is unset
   and `GATEWAY_UPSTREAM` re-points at `http://api-gateway:8080`.

3. **`Review` bounded context is absent from the deployment view.**
   It is present in Figure 1 (business architecture) but carries
   no implementation box. §11.2 covers the rationale.

### 13.2 Figure 4 divergences (async workflows)

`assignment-3_figure4_workflow-w2-w3-events.png` lists four topics
(`order-events`, `payment-events`, `delivery-events`,
`notification-events`). Phase 16 added a **fifth**:

- **`menu-events`** -- carries `menu.item-availability-changed`
  when the owner toggles a menu item's `isAvailable` flag. At
  CP#3, Sierra-Lima emits this envelope to a dedicated log stream
  (logger `menu-events`) only -- no Kafka topic yet. The `MenuEventPublisher`
  interface is the seam for a future `KafkaMenuEventPublisher`
  (ADR 0040 §2). Consumer: none in the A3 baseline; future work
  would be Order Service caching per-item availability to skip W1
  hop 5 on cold carts.

### 13.3 Known limitations

- **`Review Service` is design-only** (§11.2).
- **No service discovery** (Eureka / Consul). Static compose
  networking by container name; documented in
  `checkpoint-1-talking-points.md` §2 and ADR 0005 §3.
- **No Kafka client on Sierra-Lima's classpath.** Menu's
  availability event is log-only; the Kafka-backed swap is a
  one-class change (ADR 0040 §6) but not shipped in A3.
- **Hard delete on menu items, no tombstone.** Orders referencing
  a deleted item must carry the item snapshot (Order's concern,
  not Menu's). Revisit if the rubric requires temporal history.
- **No audit trail on who changed what.** Only `created_at` /
  `updated_at` timestamps. Acceptable for the course scope; flag
  in future work.
- **No Resilience4j circuit breaker on Sierra-Lima's outbound
  Menu -> Restaurant call.** The call is on the synchronous
  mutation path; a Restaurant outage blocks Menu mutations. The
  master plan (§8 Non-goals) excludes Resilience4j from Sierra-Lima
  scope; Alfa-Kilo owns the W1 circuit breaker on their side.
- **CORS origin list is dev-wide**
  (`http://localhost`, `:5173`, `:8080`, `:8090`). Production
  deployment would trim it.
- **Dev token mint shipped in the services.** `JwtDevMint` exists
  in both services' `security` packages and is removed the moment
  User Service is the sole issuer (ADR 0010 §4).

---

## 14. Future work

- **Kafka transport for `menu-events`.** Drop in
  `KafkaMenuEventPublisher @Component` once Mike-Alfa's broker is
  reachable. The envelope bytes and interface stay identical
  (ADR 0040 §6).
- **`restaurant.deleted` event** so Menu Service can soft-delete
  orphaned items rather than relying on the "orphans are
  acceptable" posture (§4.4). Out of A3 scope.
- **Review Service.** The Figure 1 bounded context, now with a
  real schema and consumer of `order-events`.
- **Short-TTL ownership cache in Menu Service.** The Phase 15
  cross-service call is uncached -- fine for CP#3, a latency
  concern at higher traffic. A 30 s in-process TTL cache keyed
  on `restaurantId` would mask most of the overhead without
  changing the authorisation semantics.
- **Gateway-level rate limiting and client-id authn.** Owned by
  Alfa-Kilo in the long run; Sierra-Lima's only obligation is to
  keep service endpoints idempotent so retries are safe.
- **Contract tests (Pact) between Order and Sierra-Lima.** Would
  catch breaking changes in the W1 hop 4/5 envelopes earlier than
  the Phase 10 Postman pack does.

---

## 15. Evidence appendix

All evidence lives under the repository tree; specific paths are
cited in line. The CP#3 rehearsal on 2026-05-18 will re-run the
smoke script with live teammate URLs and land the resulting trace
under `services/local-dev/evidence/`; that trace will be the final
evidence cite for this section.

### 15.1 Swagger UI screenshots

- Restaurant Service: `http://localhost:8081/swagger-ui.html`.
  Endpoints listed in §5.1 render under two tags:
  - `Restaurants` (all six endpoints).
- Menu Service: `http://localhost:8082/swagger-ui.html`. Endpoints
  listed in §5.2 render under two tags:
  - `Menu items` (all six endpoints).

Both services expose raw OpenAPI at `/v3/api-docs` (JSON) and
`/v3/api-docs.yaml` (YAML) for automated consumers.

Placeholder image paths (to be captured at CP#3 rehearsal and
dropped into `dev-docs/verification/` alongside existing `img.png`
/ `img_1.png` / `img_2.png`):

- `dev-docs/verification/swagger-restaurant.png`
- `dev-docs/verification/swagger-menu.png`

### 15.2 Endpoint tables

§5.1 (Restaurant Service) and §5.2 (Menu Service) above. Both
tables include the Phase 15 authorisation mapping.

### 15.3 Topic tables

§6.2 covers all five topics (four team-owned + `menu-events`) and
cites the ADR 0032 envelope sections.

### 15.4 Security screenshots (401 / 403)

Targets for the CP#3 capture (filenames match
`dev-docs/verification/` convention):

- **401 evidence (missing / invalid token).** Postman `Negative Auth`
  folder, request `POST /restaurants (no Authorization)` -> 401.
  Screenshot filename: `dev-docs/verification/negative-auth-401.png`.
- **403 evidence (ownership denial).** Postman `Negative Auth`
  folder, request `PUT /restaurants/{ownerB-restaurant-id}` with
  Owner A's JWT -> 403. Screenshot filename:
  `dev-docs/verification/negative-auth-403.png`.

Corresponding service log line (Phase 15 denial logging, §7.3):

```
2026-05-05T10:15:30.120+02:00  WARN 1 --- [nio-8081-exec-1]
  e.u.e.q.r.service.RestaurantService      :
  ownership denial actor=00000000-0000-0000-0000-000000000099
  role=RestaurantOwner endpoint=PUT /restaurants/d0000098-...
  restaurantId=d0000098-... ownerId=00000000-0000-0000-0000-000000000098
```

### 15.5 Kafka / log event excerpt

Captured from a rehearsal toggle
(`PUT /menu-items/{id}` flipping `isAvailable` from `true` to
`false` on seed item `e0000010-...`):

```
2026-05-05T10:15:30.450+02:00  INFO 1 --- [nio-8082-exec-3]
  e.u.e.q.m.service.MenuService            :
  availability transition menuItemId=e0000010-...
  restaurantId=d0000099-... true -> false

2026-05-05T10:15:30.452+02:00  INFO 1 --- [nio-8082-exec-3]
  menu-events                              :
  topic=menu-events key=e0000010-... envelope={"id":"<uuid>",
  "type":"menu.item-availability-changed",
  "occurredAt":"2026-05-05T08:15:30.450Z",
  "payload":{"menuItemId":"e0000010-...",
  "restaurantId":"d0000099-...",
  "isAvailable":false,"previousIsAvailable":true}}
```

The cross-service smoke script greps this out into
`services/local-dev/evidence/menu-events_<RUN_TAG>.log` when the
`quickbite-menu-service` container is reachable via Docker CLI.

### 15.6 Verification notes (cross-references)

| Phase | Note | Primary cites |
|-------|------|---------------|
| 2-6 | `phase-2-to-6-verification_Sierra-Lima.md` | Scaffolding, CRUD, validation, OpenAPI. |
| 7 | `phase-7-verification_Sierra-Lima.md` | JWT filter, route matrix. |
| 8 | `phase-8-verification_Sierra-Lima.md` | Dockerisation. |
| 9 | `phase-9-verification_Sierra-Lima.md` | Contract lock. |
| 10 | `phase-10-verification_Charlie-Lima-Alfa.md` | W1 integration + Postman pack. |
| 11 | `phase-11-verification_Sierra-Lima.md` | CP#1 polish. |
| 12 | `phase-12-verification_Charlie-Lima-Alfa.md` | Frontend shell + sign-in. |
| 14 | `phase-14-verification_Sierra-Lima.md` | Full-stack browser walk. |
| 16 | `phase-16-verification_Sierra-Lima.md` | Async stance, cross-service smoke. |

---

## 16. Ready-for-CP#3 checklist (Phase 17 DoD roll-up)

- [x] **All report sections drafted** (§§1-15 above).
- [x] **Diagrams match the implemented system.** Figures 1, 2, 3
      reused as-is; Figure 1b refreshed textually in §3.1 with
      divergences called out in §13.1; Figure 4 refreshed textually
      in §6.2 with the `menu-events` divergence called out in
      §13.2.
- [x] **Evidence included.** Swagger UI refs (§15.1), endpoint
      tables (§5, §15.2), topic tables (§6.2, §15.3), 401 / 403
      placeholders with capture paths (§15.4), log excerpt
      (§15.5). Screenshots captured at the CP#3 rehearsal slot
      the placeholder filenames without further report edits.
- [x] **Report is near-final quality.** Phase 18 only needs the
      rehearsal screenshots dropped under `dev-docs/verification/`
      and one final trace from `smoke-cross-service.sh` under
      `services/local-dev/evidence/`. No content rewrites outstanding.
