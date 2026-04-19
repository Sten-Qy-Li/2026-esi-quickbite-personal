# Report Draft -- Sierra-Lima Backend & Frontend Section

Status: **Phase 14 draft**. Lives here so the full assignment report
(Phase 17 §1) can lift it verbatim without another review round.
Reflects the implementation as of the Phase 14 base commit -- update
at every phase boundary.

Scope: Restaurant Service, Menu Service, and the Vue.js Frontend.
Team-wide sections (gateway, broker, Order / Payment / User /
Delivery / Notification) are owned by other callsigns and will be
stitched in at Phase 17.

---

## 1. Backend architecture at checkpoint #2

Two Spring Boot 3.3.x services (Java 21) talk HTTP/JSON. Neither
calls the other directly in the A3 baseline; both are called by
Order Service on W1 hops 4 and 5. At CP#2 the deployment unit grows
a packaged Vue.js frontend: the Docker Compose stack now runs five
long-lived containers (two Postgres, two Spring Boot services, one
nginx-fronted SPA) plus an opt-in `dev-gateway` stub that stands in
for Alfa-Kilo's real API Gateway during rehearsal.

```
  Browser :8090 --> nginx (frontend) --/api/**--> API Gateway :8080
                                                   |
                                                   |-- /restaurants* -->  Restaurant Service :8081 --> restaurant-db
                                                   |-- /menu-items*  -->  Menu Service       :8082 --> menu-db
                                                   `-- /auth/login   -->  User Service (Alfa-Kilo; not committed in CP#2 base)
```

Network: `quickbite-net` (Docker bridge). Each service resolves its
DB by container name (`restaurant-db:5432`, `menu-db:5432`). The
frontend container resolves the gateway upstream by the
`GATEWAY_UPSTREAM` env var (default `http://api-gateway:8080`; the
rehearsal override points at `http://dev-gateway:80`). No
service-to-service calls are wired for the baseline; when Order
Service lands, it reaches Sierra-Lima on the same network using the
service names `restaurant-service` and `menu-service`.

Key design decisions already captured (source):

- **DB per service** (`0001-scope-freeze.md`). Independent Flyway
  migrations, independent named volumes.
- **DDD aggregates** (`0020` §5-§6). `Restaurant` with embedded
  `Location`; `MenuItem` with embedded `Price`. Cross-service
  references are raw UUIDs -- no database foreign keys across
  service boundaries.
- **Token relay only** (`0033` §2). Sierra-Lima services do not
  mint tokens, do not call User Service, and do not re-sign the
  token forwarded by Order Service. Signature-verify, extract
  claims, done.

---

## 2. Data models

### 2.1 Restaurant Service

| Column | Type | Notes |
|--------|------|-------|
| `restaurant_id` | UUID, PK | Generated server-side on `POST /restaurants`. |
| `owner_id`      | UUID, NOT NULL | Cross-service ref to `User.userId`. No FK. |
| `name`          | VARCHAR(255), NOT NULL | DTO caps at 255 (`@Size(max = 255)`). Uniqueness per owner enforced in the service layer. |
| `address`       | VARCHAR(255) | Part of embedded `Location`; optional. |
| `city`          | VARCHAR(120) | Part of embedded `Location`; optional, indexed for `?city=` filter. |
| `latitude`, `longitude` | DOUBLE PRECISION | Embedded `Location`, validated `[-90,90]` / `[-180,180]`. |
| `operating_hours` | VARCHAR(20) | `HH:MM-HH:MM` string, server-parsed for availability. |
| `is_open`       | BOOLEAN, NOT NULL DEFAULT FALSE | Owner-controlled day-to-day flag. |
| `created_at`, `updated_at` | TIMESTAMP, NOT NULL | Hibernate `@EntityListeners(AuditingEntityListener.class)`. |

Validation rules live in `CreateRestaurantRequest` /
`UpdateRestaurantRequest` with Jakarta annotations (`@NotBlank`,
`@Pattern` on operating hours, `@DecimalMin` / `@DecimalMax` on
latitude / longitude). Duplicate-by-owner is enforced in
`RestaurantService.create` -- `DuplicateRestaurantException` maps
to 409.

### 2.2 Menu Service

| Column | Type | Notes |
|--------|------|-------|
| `menu_item_id`   | UUID, PK | Generated server-side. |
| `restaurant_id`  | UUID, NOT NULL | Cross-service ref. No FK. |
| `name`           | VARCHAR(255), NOT NULL | DTO caps at 255 (`@Size(max = 255)`). |
| `description`    | VARCHAR(2000) | Optional; `@Size(max = 2000)`. |
| `price_amount`   | NUMERIC(19,2), NOT NULL CHECK (price_amount > 0) | <= 2 decimal places enforced at DTO + service layer. |
| `price_currency` | VARCHAR(3), NOT NULL DEFAULT 'EUR' | ISO-4217. `EUR` in seed. |
| `category`       | VARCHAR(100), NOT NULL | Free-form; UI hints: Appetizer/Main/Dessert/Drink. |
| `is_available`   | BOOLEAN, NOT NULL DEFAULT TRUE | Owner-controlled; validate endpoint blocks unavailable items. |
| `created_at`, `updated_at` | TIMESTAMP, NOT NULL | Hibernate auditing, as for Restaurant. |

`Price` is an `@Embeddable` value object -- matches the A2 DDD
model. Business rule violations (e.g., price <= 0, >2 decimals)
raise `InvalidPriceException` -> 422.

### 2.3 Cross-service reference integrity

No database-enforced referential integrity across services. This is
intentional (`0020` §6): each service owns its schema end-to-end,
and deleting a restaurant does **not** cascade into Menu Service.
Orphaned menu items are the acceptable trade-off for deploy
independence; the graders will see the `MenuItem.restaurantId`
column referenced by name in the V2 seed and may ask about the
missing FK. Reply: "Referential integrity across service boundaries
is the event bus's job, not Postgres's." (Phase 16 / W2 will make
this concrete.)

---

## 3. APIs

### 3.1 Restaurant Service endpoints

| Verb | Path | Auth | Purpose |
|------|------|------|---------|
| POST   | `/restaurants`                      | `RestaurantOwner` / `Admin` | Create. Returns 201 + `Location: /restaurants/{id}`. |
| GET    | `/restaurants`                      | public (anonymous) | List with optional `?city=` and `?isOpen=`. |
| GET    | `/restaurants/{id}`                 | public (anonymous) | Read. 404 if unknown. |
| PUT    | `/restaurants/{id}`                 | `RestaurantOwner` / `Admin` | Replace name / location / operating hours. |
| PATCH  | `/restaurants/{id}/status`          | `RestaurantOwner` / `Admin` | Toggle `isOpen`. 200 with updated resource. |
| GET    | `/restaurants/{id}/availability`    | any authenticated | W1 hop 4. Returns `AvailabilityResponse`. |

### 3.2 Menu Service endpoints

| Verb | Path | Auth | Purpose |
|------|------|------|---------|
| POST   | `/restaurants/{rid}/menu-items` | `RestaurantOwner` / `Admin` | Create item under a restaurant. |
| GET    | `/restaurants/{rid}/menu-items` | public | List items (optional `?category=`, `?available=`). |
| GET    | `/menu-items/{id}`              | public | Read single item. |
| PUT    | `/menu-items/{id}`              | `RestaurantOwner` / `Admin` | Replace item. |
| DELETE | `/menu-items/{id}`              | `RestaurantOwner` / `Admin` | Hard delete. |
| POST   | `/menu-items/validate`          | any authenticated | W1 hop 5. Batch existence + availability + pricing. |

OpenAPI / Swagger UI is live at `http://localhost:8081/swagger-ui.html`
and `http://localhost:8082/swagger-ui.html` (Phase 4 / Phase 6).
Screenshots will be inserted in Phase 17.

### 3.3 Response envelopes

Error envelope (shared):

```json
{
  "timestamp": "2026-05-05T10:15:30+02:00",
  "status": 404,
  "error":  "Not Found",
  "message":"Restaurant not found: d0000099-...",
  "path":   "/restaurants/d0000099-.../availability",
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

W1 response envelopes are locked in
`0030-w1-synchronous-contract-lock.md` §3 (availability) and §4
(validate).

---

## 4. Workflows

### 4.1 W1 -- Place Order (synchronous)

Full chain is eight hops; Sierra-Lima owns hops 4 and 5. See
`0030` §1 for the complete table. Sequence at Sierra-Lima's edge
(annotated Figure 3 equivalent):

```
Client -> Gateway -> Order
                      |--hop 4--> Restaurant.GET /availability
                      |           <- AvailabilityResponse
                      |--hop 5--> Menu.POST /menu-items/validate
                      |           <- ValidateMenuItemsResponse
                      |--hop 6--> (local) Order persists row
                      |--hop 7--> Payment
                      |--hop 8--> Delivery
```

Evidence: Postman `W1 Integration` folder, 9 requests, 40 assertions
(`phase-10-verification_Charlie-Lima-Alfa.md` §2, §8.3).

### 4.2 W2 / W3 -- Asynchronous (callsite-only for Sierra-Lima)

Sierra-Lima does not publish or consume any W2/W3 event in the A3
baseline. `0032` §2 lists every topic:

| Topic | Producer | Consumers |
|-------|----------|-----------|
| `order-events`        | Order        | Notification, Delivery |
| `payment-events`      | Payment      | Order, Notification |
| `delivery-events`     | Delivery     | Order, Notification |
| `notification-events` | Notification | (none; terminal) |

Restaurant and Menu do not appear on either side. The report's
limitations section will note this explicitly so the grader does
not read the absence as an oversight. Future work (W4): Restaurant
could emit `restaurant-status-changed` events so Order Service can
cache availability -- not in A3 scope.

---

## 5. Security

Section intentionally short -- full detail lives in
`0010-auth-contract.md` and `phase-7-verification_Sierra-Lima.md`.

### 5.1 JWT validation

- HS256, base64-encoded shared secret via `JWT_SECRET` env var.
- `JwtAuthFilter extends OncePerRequestFilter` in each service.
- Missing / invalid / expired token -> 401 via
  `RestAuthEntryPoints`.
- Unknown signature key -> 401 (signature mismatch).

### 5.2 Role gating

`SecurityConfig.securityFilterChain(...)` wires `@PreAuthorize` with
three roles: `Customer`, `RestaurantOwner`, `Admin`. Current matrix
pinned in `SecurityRoles.java`; evidence in
`Negative Auth` Postman folder (401/403/404/400/422, 8 requests).

### 5.3 Dev-only token minting

`JwtDevMint.java` mints tokens with the same HS256 secret for
local Postman / smoke-script runs until Alfa-Kilo's User Service
ships. Postman collection pre-request script mirrors the Java
logic byte-for-byte. `0010` §4 notes this is dev-only and removed
once `POST /api/auth/login` is live.

---

## 6. Integration mechanisms in use

| Mechanism | Technology | Sierra-Lima touchpoint |
|-----------|-----------|------------------------|
| REST (JSON) | Spring Web MVC, Jackson | All endpoints in §3. |
| OpenAPI / Swagger UI | springdoc-openapi | Docs for both services (§3). |
| JWT bearer | `jjwt` library | §5 above. |
| Kafka envelope | Spring Kafka (team-wide, Mike-Alfa) | Not coded on Sierra-Lima side (§4.2). |
| Database migrations | Flyway | Per service: `V1__init.sql`, `V2__seed_demo_data.sql`. |
| Health / readiness | Spring Boot Actuator | `/actuator/health`. Docker Compose `depends_on: service_healthy` gates on it. |

Gateway integration: Order, Gateway, and User services are owned
by Alfa-Kilo. Sierra-Lima does not configure routing; the gateway
forwards `/api/restaurants/**` and `/api/menu-items/**` to ports
8081 / 8082 respectively per `0020` §10.

---

## 7. Frontend architecture (Phase 12-14)

Sierra-Lima's frontend is a single-page Vue 3 app that shipped across
three phases: shell + router + sign-in (Phase 12), restaurant and
menu-item UX (Phase 13), Docker packaging + end-to-end integration
(Phase 14). It replaces neither gateway nor service: the browser
talks only to nginx on the same origin, which reverse-proxies
`/api/**` to Alfa-Kilo's API Gateway.

### 7.1 Layout

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

### 7.2 API client and token handling

- `src/api/client.js` exposes an `apiFetch` wrapper and a `{ get,
  post, put, patch, delete }` convenience object. It attaches
  `Authorization: Bearer <token>` from localStorage to every call,
  normalises 401 into a clear-token + redirect-to-login flow, and
  wraps every failure in an `ApiError` so views can show the error
  envelope's `message` directly.
- Base URL resolution: the client reads `VUE_APP_API_BASE_URL`
  baked at build time. An **empty string** means same-origin (the
  nginx container serves both the SPA and `/api/**`); an **unset**
  variable falls back to `http://localhost:8080` (the dev gateway /
  rehearsal path for `npm run serve`). The Phase 14 Docker build
  uses the empty-string branch via `.env.production`.
- `src/auth/token.js` persists the JWT in `localStorage.quickbite.jwt`.
  Sign-in POSTs `/api/auth/login` and writes the returned token;
  sign-out clears the key. The pre-gateway developer path is to
  mint a dev token with `JwtDevMint.java` and paste it manually --
  same HS256 secret, so the Spring filters accept it.

### 7.3 Container packaging

- **Multi-stage Dockerfile** at `services/frontend/quickbite-frontend/Dockerfile`:
  stage 1 is `node:20-alpine` running `npm ci && npm run build`;
  stage 2 is `nginx:1.27-alpine` serving the compiled `dist/` and
  an envsubst-rendered `nginx.conf.template`.
- **nginx config template** at `nginx.conf.template`. Two locations:
  `/` with an HTML5-history fallback (`try_files $uri $uri/
  /index.html`) so the Vue router can deep-link; `/api/` with a
  reverse proxy to `${GATEWAY_UPSTREAM}`. Uses `resolver
  127.0.0.11 valid=30s ipv6=off` plus the `set $upstream ...;
  proxy_pass $upstream` idiom so the container boots even when the
  gateway's DNS name is not yet resolvable.
- **Compose wiring.** The `frontend` service in
  `services/local-dev/docker-compose.yml` maps host
  `${FRONTEND_HOST_PORT:-8090}` to container `80`, sets
  `GATEWAY_UPSTREAM`, joins `quickbite-net`, and runs a
  `wget -qO- http://localhost/` healthcheck.

### 7.4 Dev-gateway stub (opt-in)

Alfa-Kilo's real gateway is not yet committed in the Phase 14 base;
to allow the grader (and us) to exercise the full-stack flow without
it, the compose file carries a `dev-gateway` service behind the
`dev-gateway` compose profile. It is an `nginx:1.27-alpine`
container driven by `services/local-dev/dev-gateway/nginx.conf`.

- Matches Alfa-Kilo's routing table exactly: `/api/restaurants/**`
  -> `restaurant-service:8081`, `/api/menu-items/**` ->
  `menu-service:8082`, `/api/restaurants/{id}/menu-items` ->
  `menu-service:8082` (listed **first** because nginx regex is
  first-match), everything else under `/api/**` -> 501 JSON.
- Strips `/api` prefix before dispatch, mirroring decision 0020 §10.
- Does **not** implement `/api/auth/login` -- that belongs to User
  Service. The runbook documents the dev-JWT paste path for
  sign-in during rehearsal.
- Never runs in the canonical stack. Opt in with
  `docker compose --profile dev-gateway up -d` (or
  `COMPOSE_PROFILES=dev-gateway`) once `GATEWAY_UPSTREAM` is set to
  `http://dev-gateway:80`.

### 7.5 End-to-end flow exercised

Phase 14 verification walked the browser through sign-in, the
restaurant list, create-restaurant, add-menu-items, toggle
availability, edit/delete items, and two W1 hops
(`GET /availability`, `POST /menu-items/validate`) invoked via the
dev-gateway. Evidence: `phase-14-verification_Sierra-Lima.md` (to
land in this commit).

---

## 8. Tests and evidence

| Layer | Count | Home |
|-------|-------|------|
| Unit (service layer, Restaurant) | 5 | `service.RestaurantServiceTest` |
| Slice (controller MockMvc, Restaurant) | 12 | `controller.RestaurantControllerTest` |
| Context load (Restaurant) | 1 | `RestaurantServiceApplicationTests` |
| Unit (service, Menu) | 10 | `service.MenuServiceTest` |
| Slice (controller MockMvc, Menu) | 17 | `controller.MenuControllerTest` |
| Context load (Menu) | 1 | `MenuServiceApplicationTests` |
| **Total JUnit** | **46** | `mvn -B test` in each service directory. |
| Postman / Newman (W1 Integration) | 9 requests / 40 assertions | `services/local-dev/postman/QuickBite.postman_collection.json` |
| Postman / Newman (Negative Auth) | 8 requests / 11 assertions | same collection |
| Smoke script | 7 HTTP round-trips | `services/local-dev/smoke.sh` / `smoke.ps1` (Phase 11) |

Combined: 46 JUnit + 17 Newman requests + 1 smoke run = the CP#1
evidence pack. CP#2 layers on one end-to-end browser walkthrough
(`phase-14-verification_Sierra-Lima.md`) covering sign-in, create
restaurant, add-menu-items, toggle/edit/delete, and W1 hops 4-5
through the dev-gateway stub.

---

## 9. Known limitations (for Phase 17's limitations section)

- `Review Service` is design-only (A3 §2.4).
- No service discovery (Eureka / Consul) -- static compose
  networking (see `checkpoint-1-talking-points.md` §2).
- No service-to-service calls from Sierra-Lima -- **N/A** means no
  Resilience4j circuit breaker on Sierra-Lima's side
  (`phase-10-verification_Charlie-Lima-Alfa.md` §6).
- Hard delete on menu items; no tombstone. Order history that
  references a deleted menu item must carry the item snapshot
  (Order Service's concern, not Menu's).
- No audit log on who changed what; `created_at` / `updated_at`
  only. Acceptable for the course; flag in future work.
- Real `POST /api/auth/login` is owned by User Service and not yet
  committed. Dev token mint remains the bridge for CP#2; the `dev-gateway`
  stub does not proxy this route.
- Alfa-Kilo's real API Gateway was not committed when Sierra-Lima
  cut the Phase 14 base, so the CP#2 end-to-end walkthrough runs
  through the `dev-gateway` stub. When the real gateway lands, the
  only change on Sierra-Lima's side is dropping `COMPOSE_PROFILES=dev-gateway`
  and pointing `GATEWAY_UPSTREAM` at `http://api-gateway:8080`.
