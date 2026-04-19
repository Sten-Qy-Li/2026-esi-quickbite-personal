# Phase 7 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 7
("Sierra-Lima Hardening Pass") for Restaurant Service and Menu Service.

Date: 2026-04-18. Target CP#1 consultation: 2026-04-28.

---

## 1. Route-protection matrix (Tasks 1-3)

Live config in `config/SecurityConfig.java` on both services.

| Endpoint                              | Restaurant                             | Menu                                   |
|---------------------------------------|----------------------------------------|----------------------------------------|
| `GET /restaurants`                    | Public                                 | --                                     |
| `GET /restaurants/{id}`               | Public                                 | --                                     |
| `GET /restaurants/{id}/availability`  | Any authenticated user                 | --                                     |
| `POST/PUT/PATCH /restaurants/**`      | `RestaurantOwner` or `Admin`           | --                                     |
| `GET /restaurants/{rid}/menu-items`   | --                                     | Public                                 |
| `GET /menu-items/{id}`                | --                                     | Public                                 |
| `POST /menu-items/validate`           | --                                     | Any authenticated user                 |
| `POST/PUT/DELETE /menu-items/**`      | --                                     | `RestaurantOwner` or `Admin`           |
| `/actuator/health`, `/actuator/info`  | Public                                 | Public                                 |
| Swagger UI + `/v3/api-docs`           | Public                                 | Public                                 |

Filter order: `JwtAuthFilter` is registered `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`.
Session policy: `STATELESS`. CSRF disabled. CORS whitelists
`http://localhost:5173` and `http://localhost:8080`.

---

## 2. Error envelope (Task 4)

Both services return the same shape via `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-04-18T10:00:00+03:00",
  "status": 404,
  "error": "Not Found",
  "message": "...",
  "path": "/restaurants/...",
  "validationErrors": [{ "field": "name", "message": "name is required" }]
}
```

`validationErrors` is only populated on 400 from `@Valid` failures.

---

## 3. Request validation tightening (Task 5)

- **Restaurant** -- duplicate `(ownerId, name)` pair returns **409** via
  `DuplicateRestaurantException`. Repository has
  `existsByOwnerIdAndNameIgnoreCase`.
- **Menu** -- `priceAmount <= 0` or `scale > 2` returns **422** via
  `InvalidPriceException` (service-layer rule, not DTO, so the error
  semantically reads as "unprocessable" rather than "malformed").
- **Menu** -- empty `items` array on `/menu-items/validate` returns **400**
  via bean validation (`@NotEmpty`).
- **Menu** -- unknown `category` (not one of `Appetizer | Main | Dessert
  | Drink`) is accepted but logged at **DEBUG**.

---

## 4. Auditing (Task 6)

`AuditorAware<UUID>` in `config/AuditingConfig.java` of each service
reads the authenticated principal's `userId`. When `SecurityContext` is
empty (tests, migrations, pre-auth requests) it returns
`SYSTEM_USER = 00000000-0000-0000-0000-000000000000`. `createdAt` /
`updatedAt` are populated by `@CreatedDate` / `@LastModifiedDate` on
the JPA entities.

---

## 5. Seed data (Task 7)

`V2__seed_demo_data.sql` in both services. Idempotent (`ON CONFLICT DO
NOTHING`), deterministic UUIDs:

- **Restaurants** (6): `d0000001-...` through `d0000006-...` spread
  across 3 owners (`...0001`, `...0002`, `...0003`). Mix of open (4)
  and closed (2).
- **Menu items** (16): `e0000011-...` through `e0000064-...` linked to
  the 6 restaurants. Mix of available / unavailable across `Main`,
  `Appetizer`, `Dessert`, `Drink`.

---

## 6. Tests (Task 8)

`mvn -B test` at 2026-04-18 16:23 local:

| Service            | Class                                 | Tests |
|--------------------|---------------------------------------|-------|
| restaurant-service | `RestaurantServiceApplicationTests`   | 1     |
| restaurant-service | `service.RestaurantServiceTest`       | 5     |
| restaurant-service | `controller.RestaurantControllerTest` | 12    |
| menu-service       | `MenuServiceApplicationTests`         | 1     |
| menu-service       | `service.MenuServiceTest`             | 10    |
| menu-service       | `controller.MenuControllerTest`       | 17    |
| **Total**          |                                       | **46**|

All green, 0 failures, 0 errors. Test DB is H2 in PostgreSQL mode
(`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`), Flyway disabled for tests,
`ddl-auto=create-drop`. Controller tests boot the full filter chain
via `@SpringBootTest + @AutoConfigureMockMvc` and mint fresh per-role
tokens through `JwtDevMint` in `@BeforeEach`.

---

## 7. Postman collection (Task 9)

`services/local-dev/postman/QuickBite.postman_collection.json`:

- **Collection-level pre-request script** mints fresh HS256 JWTs for
  `customerToken`, `ownerToken`, `adminToken` before every request
  (CryptoJS, same base64 secret as `application.properties` dev
  default). No dependency on a running User Service.
- **Auth (Tokens)** folder has three stub requests that print the
  minted token to the Postman console plus a placeholder for the real
  `POST /api/auth/login` once Alfa-Kilo's User Service ships.
- **Negative Auth** folder has 8 scenarios covering 401 / 403 / 404 /
  400 / 422 with `pm.test` assertions on status and envelope shape.

`QuickBite.postman_environment.json` grew two keys (`jwtSecret`,
`jwtIssuer`) which act as optional overrides if the services start
with a non-default `JWT_SECRET`.

---

## 8. Assignment 1 guard (Task 10)

**Business vs technical architecture.**

- `dev-docs/prior-submissions/assignment-3_figure1_business-architecture.png`
  -- 8 business services only (Order, User, Delivery, Notification,
  Payment, Menu, Restaurant, Review). No API Gateway. No Kafka. No
  database cylinders. Marker legend distinguishes implemented vs.
  design-only services.
- `dev-docs/prior-submissions/assignment-3_figure1b_implementation-architecture.png`
  -- separate figure. API Gateway, Kafka, per-service DB cylinders
  live here and nowhere else. A textual note on the figure calls out
  the deliberate split.

**Per-service databases.**

`services/local-dev/docker-compose.yml` defines two independent
PostgreSQL 15 containers:

| Service          | Container                  | Volume             | Host port (default) |
|------------------|----------------------------|--------------------|---------------------|
| Restaurant       | `quickbite-restaurant-db`  | `restaurant_db_data` | 5432 (5442 in `.env.local`) |
| Menu             | `quickbite-menu-db`        | `menu_db_data`       | 5433                |

No cross-container volume sharing. Credentials, DB names, and host
ports are parameterised via `${...}` env vars so they can diverge
further per environment. Each service's Spring `application.properties`
points at its own `DB_URL`.

Both points from Assignment 1 feedback
(`dev-docs/prior-submissions/Assignment-1_Feedback.txt`) --
"Infrastructure elements mixed into architecture diagram" and "Shared
database across microservices" -- are now prevented by construction in
the repo and will not regress as long as Figure 1 and the compose file
stay the sources of truth.

---

## Definition of Done (Phase 7)

- [x] Missing or invalid tokens produce 401.
- [x] Valid tokens unlock mutation endpoints.
- [x] Consistent, structured error responses across both services.
- [x] Seed data loads automatically on startup.
- [x] Tests pass for critical CRUD and validation paths.
- [x] Postman collection complete, login-first, shareable.
- [x] Both services demonstrable independently through a login-gated flow.
