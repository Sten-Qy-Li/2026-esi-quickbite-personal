# Phase 2-6 Verification Report (`Golf-Papa-Tango`)

**Call-sign:** `Golf-Papa-Tango`  
**Date:** `2026-04-18`  
**Current commit verified:** `78ab0ca`  
**Previous short commit:** `acbeb5a`  
**Guide executed:** `dev-docs/verification/phase-2-to-6.md`  
**Method:** terminal-first verification on Windows, using Docker, Maven, direct HTTP calls, database inspection, restart checks, and limited GUI-equivalent HTTP checks

## Overall Result

**Overall status:** `PASS`

All terminal-verifiable Phase 2-6 checks passed for the current commit.

Two items remain **user-side only** because they are inherently GUI-based:

1. Postman desktop import and environment selection
2. visual confirmation that Swagger UI renders correctly in the browser

The earlier runtime blockers are no longer blocking on the current codebase:

- the Menu Service schema mismatch is fixed
- the Restaurant side can now be verified successfully on this machine by using the new host-port override support and pointing the app at `localhost:5442`

## Important Machine Note

This Windows machine still has a local PostgreSQL process listening on `localhost:5432`.

To avoid that host-port collision during this rerun, verification used:

- Restaurant DB container exposed on host port `5442`
- Menu DB container exposed on host port `5433`
- Restaurant Service `DB_URL=jdbc:postgresql://localhost:5442/restaurant_db`
- Menu Service `DB_URL=jdbc:postgresql://localhost:5433/menu_db`

That means the repository now supports successful verification on this machine, but the local `.env.local` file in this workspace still needs a matching host-port override if you want to reproduce the same run manually without stopping the Windows PostgreSQL service.

## Environment Snapshot

- `java -version`: `17.0.18`
- `mvn -version`: `3.9.14`
- `docker --version`: `29.4.0`
- Postman assets present:
  - `services/local-dev/postman/QuickBite.postman_collection.json`
  - `services/local-dev/postman/QuickBite.postman_environment.json`
- Docker DB containers started successfully and reached `healthy`
- Maven `test` passed for both services
- Maven `package` passed for both services

## Pass/Fail Matrix

| Check | Result | Notes |
| --- | --- | --- |
| Prerequisites present (`java`, `mvn`, `docker`) | PASS | All required CLI tools available |
| Postman assets exist | PASS | Collection and environment files present |
| Docker Compose start | PASS | DB stack started with Restaurant on host `5442`, Menu on `5433` |
| DB container health | PASS | Both containers reported `healthy` |
| `mvn test` for `restaurant-service` | PASS | Existing test suite passed |
| `mvn test` for `menu-service` | PASS | Existing test suite passed |
| `mvn package` for `restaurant-service` | PASS | Jar built successfully |
| `mvn package` for `menu-service` | PASS | Jar built successfully |
| Restaurant Service startup | PASS | Healthy on `http://localhost:8081` |
| Menu Service startup | PASS | Healthy on `http://localhost:8082` |
| `/actuator/health` for both services | PASS | Both returned `status=UP` |
| Restaurant DB schema creation | PASS | `flyway_schema_history`, `restaurant` present |
| Menu DB schema creation | PASS | `flyway_schema_history`, `menu_item` present |
| Swagger UI HTTP availability | PASS | `swagger-ui.html` returned `200` for both services |
| OpenAPI docs HTTP availability | PASS | `/v3/api-docs` returned `200` for both services |
| Restaurant CRUD flow | PASS | Create, get, filter, update, status toggle, availability all worked |
| Restaurant invalid UUID handling | PASS | Returned `400` with expected error envelope |
| Restaurant validation handling | PASS | Returned `400` with `validationErrors[]` |
| Menu CRUD flow | PASS | Create, get, list, update, delete all worked |
| Menu invalid price validation | PASS | Returned `400` with `validationErrors[]` |
| Menu batch validation | PASS | Valid and mixed-item cases returned expected shapes |
| Runtime CORS verification | PASS | `Access-Control-Allow-Origin: http://localhost:5173` present for both services |
| Persistence across restart | PASS | Restaurant and Menu data survived service restart |
| Postman desktop import | USER VERIFY | Files exist, but desktop import itself is GUI-only |
| Visual Swagger UI rendering | USER VERIFY | HTML served successfully, but browser rendering is GUI-only |

## Detailed Findings

### 1. Phase 2 scaffolding now passes from the terminal

Confirmed during this rerun:

- both Spring Boot apps started without runtime errors
- both PostgreSQL containers became healthy
- both health endpoints returned `UP`
- both Maven modules tested and packaged successfully

### 2. The previous blocking defects are resolved for the current verification path

#### Restaurant side

The previous failure came from the machine's local PostgreSQL service occupying `5432`.

The current repo now supports host-port overrides cleanly. With the Restaurant DB exposed on `5442` and the app launched against `jdbc:postgresql://localhost:5442/restaurant_db`, the Restaurant Service passed startup and all runtime checks.

#### Menu side

The previous Hibernate/Flyway mismatch on `price_currency` is no longer present. The current migration defines:

- `price_currency VARCHAR(3)`

and the service starts cleanly against that schema.

### 3. Restaurant runtime checks passed

Observed results:

- `POST /restaurants` returned `201 Created`
- `GET /restaurants/{id}` returned `200 OK`
- invalid UUID returned `400 Bad Request` with:
  - `message: "Invalid value for parameter 'id'"`
- `GET /restaurants?city=Tartu&isOpen=true` returned an empty array before opening the restaurant
- `GET /restaurants?city=Tartu&isOpen=false` returned the created row
- `PUT /restaurants/{id}` updated the business fields successfully
- `PATCH /restaurants/{id}/status` returned `200 OK` with `"isOpen": true`
- `GET /restaurants/{id}/availability` returned the expected availability shape
- invalid create body returned `400 Bad Request` with:
  - `validationErrors[0].field = "name"`
  - `validationErrors[0].message = "name is required"`

### 4. Menu runtime checks passed

Observed results:

- `POST /restaurants/{rid}/menu-items` returned `201 Created`
- `GET /menu-items/{id}` returned `200 OK`
- `GET /restaurants/{rid}/menu-items?category=Main&available=true` returned the created row
- invalid price body returned `400 Bad Request` with:
  - `validationErrors[0].field = "priceAmount"`
  - `validationErrors[0].message = "priceAmount must be greater than 0"`
- `PUT /menu-items/{id}` updated the item successfully
- `POST /menu-items/validate` with one valid row returned:
  - `allValid: true`
  - correct `unitPriceAmount`, `unitPriceCurrency`, and `lineTotalAmount`
- `POST /menu-items/validate` with one nonexistent id returned:
  - `allValid: false`
  - second entry with `exists: false`
  - `reason: "not_found"`
- `DELETE /menu-items/{id}` returned `204 No Content`
- follow-up `GET /menu-items/{id}` returned `404 Not Found`

### 5. Persistence check passed

After stopping and restarting both Spring Boot services:

- `GET /restaurants/{id}` still returned the updated Restaurant
- `GET /menu-items/{id}` still returned the updated Menu item
- both post-restart health endpoints still returned `UP`

This confirms that the created schema and data persisted across service restarts as required by the guide.

### 6. CORS and docs exposure passed

Observed results:

- `OPTIONS http://localhost:8081/restaurants` returned `Access-Control-Allow-Origin: http://localhost:5173`
- `OPTIONS http://localhost:8082/menu-items/validate` returned `Access-Control-Allow-Origin: http://localhost:5173`
- both `swagger-ui.html` endpoints returned `200`
- both `/v3/api-docs` endpoints returned `200`

## Phase-by-Phase Verdict

| Phase | Verdict | Notes |
| --- | --- | --- |
| Phase 2 - Scaffolding | PASS | Tooling, DB containers, app startup, and health checks all passed |
| Phase 3 - Restaurant foundation | PASS | Schema exists, all Restaurant endpoints exercised, data persisted across restart |
| Phase 4 - Restaurant polish | PASS* | Validation, error envelope, CORS, and docs HTTP checks passed; final visual Swagger confirmation is user-side |
| Phase 5 - Menu foundation | PASS | Schema exists, all Menu endpoints exercised, batch validation passed, data persisted across restart |
| Phase 6 - Menu polish | PASS* | Validation, locked-down batch response shape, CORS, and docs HTTP checks passed; final visual Swagger confirmation is user-side |

## What I Could Not Verify From My End

These remain outside terminal-only verification:

- importing the Postman collection into the Postman desktop app
- confirming Swagger UI renders correctly in a real browser window

Everything else in the guide that can be validated from the terminal was validated during this rerun.

## Simple Instructions For You To Verify On Your End

### A. Prepare the local DB ports

This machine already has another PostgreSQL process using `localhost:5432`.

You have two choices:

1. stop that Windows PostgreSQL service temporarily, or
2. keep it running and use the same port override that I used during verification

The simpler and safer option is usually option `2`.

If you choose option `2`, open `services/local-dev/.env.local` and make sure these two lines exist:

```env
RESTAURANT_DB_HOST_PORT=5442
MENU_DB_HOST_PORT=5433
```

Save the file after editing it.

### B. Start the two database containers

1. Open PowerShell.
2. Go to the local-dev folder:

```powershell
cd C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal\services\local-dev
```

3. Start the databases:

```powershell
docker compose --env-file .env.local up -d
```

4. Check that both containers are healthy:

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}"
```

You should see both:

- `quickbite-restaurant-db`
- `quickbite-menu-db`

and both should say `healthy`.

### C. Start the two Spring Boot services in IntelliJ

Open the two service folders in IntelliJ in separate windows if needed.

#### Restaurant Service

1. Open `services/restaurant-service`.
2. Open the file:
   - `src/main/java/ee/ut/esi/quickbite/restaurant/RestaurantServiceApplication.java`
3. Create or edit the Run Configuration so it uses this environment variable:

```text
DB_URL=jdbc:postgresql://localhost:5442/restaurant_db
```

4. Run `RestaurantServiceApplication`.
5. Wait until the Run window shows that Tomcat started on port `8081`.

#### Menu Service

1. Open `services/menu-service`.
2. Open the file:
   - `src/main/java/ee/ut/esi/quickbite/menu/MenuServiceApplication.java`
3. Create or edit the Run Configuration so it uses this environment variable:

```text
DB_URL=jdbc:postgresql://localhost:5433/menu_db
```

4. Run `MenuServiceApplication`.
5. Wait until the Run window shows that Tomcat started on port `8082`.

### D. Verify the Swagger UI pages in your browser

After both services are running:

1. Open `http://localhost:8081/swagger-ui.html`
2. Open `http://localhost:8082/swagger-ui.html`
3. Confirm both pages load instead of showing a browser error page
4. Confirm the Restaurant page lists the Restaurant endpoints
5. Confirm the Menu page lists the Menu endpoints

### E. Verify the Postman import

1. Open Postman.
2. Import this collection file:
   - `services/local-dev/postman/QuickBite.postman_collection.json`
3. Import this environment file:
   - `services/local-dev/postman/QuickBite.postman_environment.json`
4. In Postman, select the imported environment.
5. Confirm the collection appears and opens without errors.

### F. Stop everything when finished

1. Stop both Spring Boot apps from IntelliJ.
2. In PowerShell, return to `services/local-dev`.
3. Run:

```powershell
docker compose down
```

## Raw Evidence

Raw logs and captured artifacts from this rerun are stored under:

- `dev-docs/verification/.tmp-phase-2-to-6-golf-papa-tango/`

Most relevant files:

- `docker.compose.up.log`
- `docker.ps.log`
- `restaurant.mvn.test.log`
- `menu.mvn.test.log`
- `restaurant.mvn.package.log`
- `menu.mvn.package.log`
- `restaurant.stdout.log`
- `menu.stdout.log`
- `restaurant.restart.stdout.log`
- `menu.restart.stdout.log`
- `runtime-pre-restart.json`
- `runtime-post-restart.json`

## Bottom Line

The current commit `78ab0ca` passes the Phase 2-6 verification guide for all terminal-verifiable checks.

The prior short commit `acbeb5a` is now recorded in this report because it introduced the two changes that made this rerun possible on this machine:

1. parameterized DB host-port support
2. the Menu `price_currency` schema fix

Remaining user work is limited to the GUI-only confirmations for Postman import and visual Swagger rendering.
