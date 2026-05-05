# Menu Service in a Food Delivery System

## Repository owner

Student name: Sten (Qun-yan Li)

Student group: 7

## About

Individual-level (not team-level) repository for the course project of Enterprise System Integration, a course taught at the University of Tartu in the spring of 2026.

The repository is solely for grading at Project Checkpoint 1 only. For Project Checkpoints 2 and 3, kindly see the team-level repository: [https://github.com/anup28kmr/esi](https://github.com/anup28kmr/esi).

The repository and its contents were produced with the assistance of AI agents.

## Instructions for grader

### Service under examination

The **Menu Service** (`menu-service/`) is the service submitted for Project Checkpoint 1 grading. The Restaurant Service (`restaurant-service/`) is also fully implemented and is included so that the Menu Service's cross-service test (item E below) can exercise a real collaborator interaction; per the "one service per student" rule for CP1, the second service is not itself in scope at this checkpoint.

### Mapping to Checkpoint 1 deliverables

| Item | Where to look |
|---|---|
| **A. Running service** | Menu Service runs on `http://localhost:8082` after the local stack is up (see Quick start below). Health probe: `GET /actuator/health`. Spring Boot entry point: [`src/main/java/ee/ut/esi/quickbite/menu/MenuServiceApplication.java`](src/main/java/ee/ut/esi/quickbite/menu/MenuServiceApplication.java). |
| **B. API endpoints** | [`src/main/java/ee/ut/esi/quickbite/menu/controller/MenuController.java`](src/main/java/ee/ut/esi/quickbite/menu/controller/MenuController.java) -- 6 endpoints (create, list-by-restaurant, get-by-id, replace, delete, batch-validate). |
| **C. OpenAPI / Swagger UI** | <http://localhost:8082/swagger-ui.html> (UI). <http://localhost:8082/v3/api-docs> (JSON). Every endpoint carries `@Operation` and `@ApiResponse` annotations. |
| **D. Persistence** | Postgres 15 (`menu_db`, host port 5433). Flyway migrations: [`V1__init.sql`](src/main/resources/db/migration/V1__init.sql) and [`V2__seed_demo_data.sql`](src/main/resources/db/migration/V2__seed_demo_data.sql). JPA entities under `src/main/java/.../domain/`. |
| **E. Test with mocked cross-service dependency, happy + error case** | [`src/test/java/ee/ut/esi/quickbite/menu/controller/MenuControllerTest.java`](src/test/java/ee/ut/esi/quickbite/menu/controller/MenuControllerTest.java) declares `@MockBean RestaurantOwnershipClient` -- the HTTP collaborator that Menu Service calls into Restaurant Service. The 23 cases include happy paths (`createMenuItem_ownerTokenReturns201`, `validate_succeedsWithCustomerToken`) and error cases (`getMenuItemById_returns404WhenMissing`, `createMenuItem_adminUnknownRestaurantReturns404`). The class uses `@SpringBootTest + @AutoConfigureMockMvc + @MockBean` rather than `@WebMvcTest`, which CP1 §4.1.E permits ("or any other testing framework you prefer"). |
| **F. API demonstration** | Postman pack: [`../local-dev/postman/QuickBite.postman_collection.json`](../local-dev/postman/QuickBite.postman_collection.json) and the matching environment file. Run via Newman (command below), or use Swagger UI directly. |

### Quick start

Prerequisites: Docker Desktop, Java 17+ (for `mvn test`), and a shell that can run either `.sh` or `.ps1` scripts.

```bash
# Bring up the Sten stack (Postgres x2 + both services + frontend)
cd local-dev
docker compose --profile dev-gateway up -d --build
docker ps                                     # expect 6 healthy containers

# Sanity-check the Menu Service
curl -fsS http://localhost:8082/actuator/health
```

### Reproduction commands

```bash
# Backend unit + integration tests
cd menu-service        && mvn clean test          # 47/47
cd ../restaurant-service && mvn clean test        # 33/33

# Postman / Newman (after stack is up)
cd ../local-dev
npx newman run postman/QuickBite.postman_collection.json \
              -e postman/QuickBite.postman_environment.json

# Smoke probes (after stack is up)
bash smoke.sh                                     # POSIX shell
./smoke.ps1                                       # PowerShell equivalent
```

### Authentication during demo

The local stack uses dev HS256 JWTs minted by `JwtDevMint`; smoke scripts and Postman pre-request scripts mint them automatically. Three roles are exercised:

- `dev-customer` -- Customer (read-only on public endpoints, batch-validate)
- `dev-owner` -- RestaurantOwner (full CRUD on items they own)
- `dev-admin` -- Admin (bypasses ownership checks)

Token-minting commands and the per-endpoint auth matrix are documented in [`../local-dev/runbook.md`](../local-dev/runbook.md) §4 and §9.

## Service overview

Sten's Spring Boot service that owns the `MenuItem` aggregate
(requirements **R21** add/update/remove menu items and **R22** browse
menu).

## Layout

```
menu-service/
  pom.xml
  Dockerfile
  .dockerignore
  src/
    main/
      java/ee/ut/esi/quickbite/menu/
        MenuServiceApplication.java
        controller/                  HTTP entry points
        service/                     business logic + ownership checks
        repository/                  Spring Data JPA
        domain/                      JPA entities
        dto/                         request/response + validation
        config/                      SecurityConfig, OpenApiConfig
        security/                    JwtAuthFilter, RestaurantOwnershipClient
        event/                       MenuEventPublisher (log-only by default)
        exception/                   GlobalExceptionHandler + custom exceptions
      resources/
        application.yml              default profile
        application-docker.properties  overrides DB_URL to container hostname
        db/migration/                Flyway V1__init.sql, V2__seed_demo_data.sql
    test/
      java/ee/ut/esi/quickbite/menu/  47 tests at 50b8e1d
```

## Responsibilities

- Owns the `MenuItem` aggregate root (name, description, price,
  category, availability flag, `restaurantId` reference by UUID).
- Serves the W1 batch validation `POST /menu-items/validate`.
- Serves browse routes (R22) and owner-gated CRUD (R21).
- Persists to a service-local PostgreSQL database (`menu-db` in
  Compose).
- Does **not** store restaurant metadata -- that belongs to
  `restaurant-service`. Cross-service references are by UUID only,
  no foreign keys.
- Optional: publishes `menu-events` on availability change. Default
  implementation is log-only; the Kafka swap is a one-class drop-in.

## API surface

| Method | Path | Who | Notes |
|---|---|---|---|
| `POST` | `/restaurants/{rid}/menu-items` | Owner of `{rid}` / Admin | Create. 201 on success. |
| `GET` | `/restaurants/{rid}/menu-items` | Public | Browse (R22). |
| `GET` | `/menu-items/{id}` | Public | Fetch by id. |
| `PUT` | `/menu-items/{id}` | Owner of parent restaurant / Admin | Update. |
| `DELETE` | `/menu-items/{id}` | Owner of parent restaurant / Admin | Remove. 204 on success. |
| `POST` | `/menu-items/validate` | Any authenticated | W1 batch validation. |

## Run locally

```bash
# Unit + integration tests (47 tests)
cd menu-service
mvn clean test

# From IntelliJ (fast iteration): run MenuServiceApplication with
# SPRING_PROFILES_ACTIVE=default and the DB container up. See
# ../local-dev/runbook.md §7.

# Full stack (DB + service + friends):
cd ../local-dev
docker compose --profile dev-gateway up -d --build
```

JWT auth (issuer-pinned HS256) is wired in
`security/JwtAuthFilter.java`. The shared dev secret and issuer live
in [`../local-dev/.env.example`](../local-dev/.env.example).
Restaurant ownership is checked via
`security/RestaurantOwnershipClient.java`, which calls the **public**
`GET /restaurants/{id}` on `restaurant-service` -- no token required
for that call.

## For AI coding agents

- **Ownership checks go in the service layer**, not the controller.
  See `service/MenuService.java` for the pattern: fetch the parent
  restaurant id, delegate to `RestaurantOwnershipClient`, throw
  `ForbiddenException` on mismatch.
- **Errors flow through `GlobalExceptionHandler`.** Custom exceptions
  in `exception/` each map to one status code. Do not
  `throw new ResponseStatusException` from controllers.
- **Flyway migrations are append-only.** Add `V3__<name>.sql`; do not
  edit `V1__init.sql` or `V2__seed_demo_data.sql`.
- **The log-only publisher is intentional.** Do not swap in a Kafka
  implementation without team alignment first -- the stance is
  "log-only unless teammate async is late".
