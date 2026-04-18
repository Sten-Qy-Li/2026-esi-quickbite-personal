# Phase 2-6 Verification Report (`Golf-Papa-Tango`)

**Call-sign:** `Golf-Papa-Tango`  
**Date:** `2026-04-18`  
**Repo base:** `aac68b0` workspace  
**Guide executed:** `dev-docs/verification/phase-2-to-6.md`  
**Method:** terminal-first verification on Windows, using Docker, Maven, direct HTTP calls, database inspection, and static controller/schema review where runtime was blocked

## Overall Result

**Overall status:** `FAIL`

The verification did not pass end-to-end in the repository's current state.
The main blockers were:

1. `Restaurant Service` did not start because it could not authenticate to the
   database on `localhost:5432`.
2. `Menu Service` did not start because Hibernate schema validation rejected
   the `menu_item.price_currency` column type created by Flyway.

Because both services failed during startup, the guide's runtime checks for
health endpoints, Swagger UI, CRUD flows, validation envelopes, CORS, and
persistence-after-restart could not be completed as written.

## Environment Snapshot

- `java -version`: `17.0.18`
- `mvn -version`: `3.9.14`
- `docker --version`: `29.4.0`
- `services/local-dev/.env.local`: created from `.env.example`
- Docker containers:
  - `quickbite-restaurant-db`: `healthy`
  - `quickbite-menu-db`: `healthy`
- Existing machine-specific conflict observed:
  - Windows service `postgresql-x64-18` is already running locally
  - a local `postgres` process is also bound to port `5432`

## Pass/Fail Matrix

| Check | Result | Notes |
| --- | --- | --- |
| Prerequisites present (`java`, `mvn`, `docker`) | PASS | All required CLI tools were available |
| Postman assets exist | PASS | Collection and environment JSON files are present |
| `.env.local` setup | PASS | Created from `.env.example` |
| Docker Compose start | PASS | Both DB containers started successfully |
| DB container health | PASS | Both reported `healthy` |
| `mvn test` for `restaurant-service` | PASS | Existing test suite passed |
| `mvn test` for `menu-service` | PASS | Existing test suite passed |
| `mvn package` for `restaurant-service` | PASS | Jar built successfully |
| `mvn package` for `menu-service` | PASS | Jar built successfully |
| Restaurant DB credentials valid inside container | PASS | `restaurant_user` / `restaurant_pw` worked via `psql` inside container |
| Restaurant Service startup | FAIL | App failed with `FATAL: password authentication failed for user "restaurant_user"` |
| Menu Service startup | FAIL | App failed with Hibernate schema validation error on `price_currency` |
| `/actuator/health` for either service | BLOCKED | Both services failed before stable startup |
| Swagger UI runtime rendering | BLOCKED | Both services failed before stable startup |
| Restaurant CRUD runtime flow | BLOCKED | Service never started |
| Menu CRUD runtime flow | BLOCKED | Service never started |
| Batch validation runtime flow | BLOCKED | Menu Service never started |
| Persistence restart test | BLOCKED | Could not create runtime data through the services |
| Runtime CORS verification | BLOCKED | Could not issue successful preflight checks against a running service |

## Detailed Findings

### 1. Phase 2 infrastructure is only partially verified

Verified successfully:

- local prerequisites are installed
- `services/local-dev/.env.local` can be created
- Docker Compose brings up both PostgreSQL containers
- both databases reach `healthy`
- both Maven modules build and package successfully

Not verified at runtime:

- "Both Spring Boot apps start without errors" failed
- therefore the Phase 2 DoD does not pass

### 2. Restaurant Service fails before Flyway can create its schema

Observed behavior:

- `restaurant-service` jar starts Tomcat on `8081`
- startup fails during datasource/Flyway initialization
- error from application log:
  - `FATAL: password authentication failed for user "restaurant_user"`

Important nuance:

- the container itself accepts `restaurant_user` / `restaurant_pw`
- direct `psql` inside `quickbite-restaurant-db` succeeded
- the database container is healthy
- `restaurant_db` contains **no relations**, which confirms the service never
  reached successful migration

Most likely explanation on this machine:

- a separate local PostgreSQL service is already running on Windows
- port `5432` is contested locally
- the guide assumes `localhost:5432` cleanly reaches the Docker database, which
  is not true on this machine

Evidence:

- Windows service: `postgresql-x64-18` is running
- `netstat` showed a local `postgres` process also listening on `5432`

Effect on verification:

- all Restaurant runtime checks in sections 6, 7, 9.1, 10, and 11 of the guide
  are blocked

### 3. Menu Service fails after Flyway migration because entity/schema types do not match

Observed behavior:

- `menu-service` jar connects to PostgreSQL successfully
- Flyway creates `flyway_schema_history` and `menu_item`
- startup then fails during Hibernate schema validation

Exact mismatch:

- migration file:
  - `services/menu-service/src/main/resources/db/migration/V1__init.sql`
  - defines `price_currency CHAR(3)`
- entity mapping:
  - `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/domain/Price.java`
  - maps `price_currency` as `String` with `length = 3`
- Hibernate expects `varchar(3)` for that mapping and rejects the DB column

Database inspection confirmed:

- `menu_item.price_currency` is `character(3)`
- `flyway_schema_history` contains version `1`, description `init`, success `t`

Effect on verification:

- Menu runtime checks in sections 6, 7, 9.2, 10, and 11 are blocked

### 4. Static API surface looks present even though runtime verification is blocked

Static controller inspection confirms the expected endpoint counts exist in code:

- Restaurant Controller declares 6 endpoints:
  - `POST /restaurants`
  - `GET /restaurants/{id}`
  - `GET /restaurants`
  - `PUT /restaurants/{id}`
  - `PATCH /restaurants/{id}/status`
  - `GET /restaurants/{id}/availability`
- Menu Controller declares 6 endpoints:
  - `POST /restaurants/{restaurantId}/menu-items`
  - `GET /restaurants/{restaurantId}/menu-items`
  - `GET /menu-items/{id}`
  - `PUT /menu-items/{id}`
  - `DELETE /menu-items/{id}`
  - `POST /menu-items/validate`

Static contract checks also showed:

- both services have `GlobalExceptionHandler` classes with the expected error
  envelope fields
- `ValidateMenuItemsResponse` matches the guide's expected `results[]` shape

This means the runtime blockers are not caused by missing controllers. They are
startup-time infrastructure/schema problems.

### 5. Existing automated test coverage is too thin for this verification scope

Both modules passed `mvn test`, but each service currently has only one test:

- `RestaurantServiceApplicationTests.java`
- `MenuServiceApplicationTests.java`

These tests are application-context smoke tests only. They do not verify:

- CRUD behavior
- validation errors
- Swagger/OpenAPI exposure
- Flyway schema correctness
- batch validation semantics
- CORS behavior

## Phase-by-Phase Verdict

| Phase | Verdict | Notes |
| --- | --- | --- |
| Phase 2 - Scaffolding | FAIL | Tooling and DB containers pass, but both apps do not start cleanly |
| Phase 3 - Restaurant foundation | FAIL | Restaurant Service cannot authenticate to DB on this machine; no schema created |
| Phase 4 - Restaurant polish | BLOCKED | Runtime Swagger, validation, CORS, and CRUD checks could not be completed |
| Phase 5 - Menu foundation | FAIL | Menu migration runs, but service fails Hibernate schema validation and never becomes healthy |
| Phase 6 - Menu polish | BLOCKED | Runtime Swagger, CRUD, validation, and batch-validate checks could not be completed |

## What I Could Not Verify From My End

These checks remained unverified because the services did not reach a stable
running state or because they are inherently GUI-oriented:

- Swagger UI page rendering in a browser
- Postman collection import and environment selection in the desktop app
- interactive "Try it out" behavior in Swagger UI
- full CRUD happy-path and validation-path requests through Postman
- runtime CORS preflight response headers from a healthy app
- persistence-after-restart using service-created records

## Simple Instructions For You To Verify On Your End

Use these only after the two startup blockers are resolved.

### A. Verify the Restaurant side on your machine

1. Check whether local PostgreSQL is still occupying `5432`.
2. If you can safely stop it temporarily, stop the Windows service
   `postgresql-x64-18`.
3. Recreate the Restaurant DB container:
   - `cd services/local-dev`
   - `docker compose down -v`
   - `docker compose --env-file .env.local up -d`
4. Start `RestaurantServiceApplication`.
5. Verify:
   - `http://localhost:8081/actuator/health`
   - `http://localhost:8081/swagger-ui.html`
   - the Restaurant requests in section `9.1` of `dev-docs/verification/phase-2-to-6.md`

### B. Verify the Menu side on your machine

1. Align the `price_currency` type between:
   - `services/menu-service/src/main/resources/db/migration/V1__init.sql`
   - `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/domain/Price.java`
2. Recreate the Menu DB volume so Flyway reruns from a clean state:
   - `cd services/local-dev`
   - `docker compose down -v`
   - `docker compose --env-file .env.local up -d`
3. Start `MenuServiceApplication`.
4. Verify:
   - `http://localhost:8082/actuator/health`
   - `http://localhost:8082/swagger-ui.html`
   - the Menu requests in section `9.2` of `dev-docs/verification/phase-2-to-6.md`

### C. Verify the GUI-only items

1. Open Swagger UI in the browser for both services and confirm all 6 endpoints
   render.
2. Import:
   - `services/local-dev/postman/QuickBite.postman_collection.json`
   - `services/local-dev/postman/QuickBite.postman_environment.json`
3. In Postman, run the Restaurant folder first, then the Menu folder, matching
   the guide.
4. For CORS, run:
   - `curl -I -X OPTIONS http://localhost:8081/restaurants -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: GET"`
   - `curl -I -X OPTIONS http://localhost:8082/menu-items/validate -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: POST"`

## Raw Evidence

Raw startup logs from this verification run were captured under:

- `dev-docs/verification/.tmp-phase-2-to-6-golf-papa-tango/`

Most relevant artifacts:

- `restaurant.stdout.log`
- `menu.stdout.log`
- `restaurant.override.stdout.log`
- `restaurant.cliargs.stdout.log`

## Bottom Line

The repository does **not** currently satisfy the Phase 2-6 verification guide
end-to-end.

The blocking defects are:

1. a machine-specific Restaurant DB connectivity conflict around `localhost:5432`
2. a repo-level Menu schema/entity mismatch on `price_currency`

Until those are resolved, the runtime DoD for Phases 2-6 should be treated as
**not achieved**.
