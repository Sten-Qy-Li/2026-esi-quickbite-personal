# Local Dev

Docker Compose stack, environment-variable template, smoke scripts,
and Postman workspace for running Sten's services on a
developer laptop.

**Operational steps** (start, reset, logs, IntelliJ mode, W1 smoke)
live in [`runbook.md`](runbook.md). This file is the reference index:
what exists, what ports each piece uses, and where to look next.

## Layout

```
local-dev/
  docker-compose.yml                   both DBs + both services + frontend + dev-gateway (profile)
  .env.example                         tracked template for all environment variables
  .env.local                           git-ignored local overrides (created from .env.example on first run)
  runbook.md                           operational steps (stack up, IntelliJ mode, smokes, CP#1 dry run)
  smoke.sh / smoke.ps1                 Sten-only happy-path probe (expect: exit 0)
  smoke-cross-service.sh / .ps1        cross-service probes + captures `menu-events` log
  dev-gateway/
    nginx.conf                         dev-only reverse proxy for the frontend (profile: dev-gateway)
  evidence/
    cross-service-smoke_*.log          per-run smoke output (tracked; audit trail for Phase 16+ DoD)
    menu-events_*.log                  per-run `menu-events` log lines (tracked, same reason)
  postman/
    QuickBite.postman_collection.json  Phase 7+ collection: JWT auto-mint, role matrix, W1 folder, negative-auth
    QuickBite.postman_environment.json shared variables (base URLs, ids, tokens)
```

`evidence/*.log` files are deliberately tracked despite the global
`*.log` ignore rule, per the exception in the root `.gitignore` --
they are the point-in-time evidence audits cite.

## Port and env-var matrix

| Service              | Service Port | DB Port | Owner           | Scaffolded here? |
|----------------------|--------------|---------|-----------------|------------------|
| API Gateway          | 8080         | --      | Alfa-Kilo       | No (external)    |
| User Service         | 8085         | 5435    | Alfa-Kilo       | No (external)    |
| Order Service        | 8086         | 5436    | Alfa-Kilo       | No (external)    |
| Restaurant Service   | 8081         | 5432    | Sten     | **Yes**          |
| Menu Service         | 8082         | 5433    | Sten     | **Yes**          |
| Payment Service      | 8083         | 5437    | Elephant-Yankee | No (external)    |
| Delivery Service     | 8084         | 5438    | Elephant-Yankee | No (external)    |
| Notification Service | 8087         | 5439    | Mike-Alfa       | No (external)    |
| Kafka broker         | 9092         | --      | Mike-Alfa       | No (Phase 10+)   |

Sten services read the following environment variables (values
in `.env.example`):

| Variable                  | Used by                         | Notes                                               |
|---------------------------|---------------------------------|-----------------------------------------------------|
| `DB_URL`                  | restaurant-service, menu-service| JDBC URL, defaults match the port matrix above      |
| `DB_USER`, `DB_PASSWORD`  | restaurant-service, menu-service| From `.env.local` via IntelliJ Run Configuration    |
| `JWT_SECRET`              | Phase 7+                        | Base64 HS256 key (dev only; placeholder committed)  |
| `JWT_ISSUER`              | Phase 7+                        | `quickbite-user-service`                            |
| `RESTAURANT_SERVICE_URL`  | (future callers)                | `http://localhost:8081` by default                  |
| `MENU_SERVICE_URL`        | (future callers)                | `http://localhost:8082` by default                  |

## Compose topology

- `quickbite-restaurant-db` (PostgreSQL 15): container port 5432 →
  host port `${RESTAURANT_DB_HOST_PORT:-5432}`. Named volume
  `restaurant_db_data`. Healthcheck: `pg_isready`.
- `quickbite-menu-db` (PostgreSQL 15): container port 5432 → host
  port `${MENU_DB_HOST_PORT:-5433}`. Named volume `menu_db_data`.
  Healthcheck: `pg_isready`.
- `quickbite-restaurant-service` (Spring Boot): host port 8081.
  `depends_on: restaurant-db (service_healthy)`,
  `SPRING_PROFILES_ACTIVE=docker`. Healthcheck: `curl /actuator/health`.
- `quickbite-menu-service` (Spring Boot): host port 8082.
  `depends_on: menu-db (service_healthy)`,
  `SPRING_PROFILES_ACTIVE=docker`. Healthcheck: `curl /actuator/health`.
- Shared bridge network `quickbite-net` -- services reach their DBs
  via the container hostnames `restaurant-db` / `menu-db`.

From Phase 8 onward the full stack runs in Docker Compose. The
earlier "services from IntelliJ, DBs from Compose" mode still works
for fast iteration; see [`runbook.md`](runbook.md) §7.

## Not included here

- Teammates' services (User, Order, Payment, Delivery, Notification).
  They run from their own repos during integration rehearsals. The
  real API Gateway (Alfa-Kilo, Spring Cloud Gateway) is also
  teammate-owned; the `dev-gateway` nginx profile here is a thin
  local substitute that only routes the frontend-served paths.
- Eureka, Spring Cloud Config, or any discovery server -- explicit
  non-goal for this checkpoint.
- Kafka broker. Sten remains a non-participant; the `menu-events`
  publisher logs to stdout. A Kafka swap is a one-class drop-in when
  the group broker is ready.

## For AI coding agents

- **Before editing `docker-compose.yml`**, do not introduce new
  `<SERVICE>_SERVICE_URL` variants -- they are enumerated in the
  env-var matrix above.
- **Smoke scripts are the contract.** `smoke.sh` exits 0 only when
  Sten's happy path works end-to-end. If you change a DTO
  shape, update the smoke probe in the same commit.
- **`evidence/` is append-only.** Do not delete past smoke logs; they
  are cited by audits. Each run appends a new timestamped file.
- **Postman pack is a demo asset.** Any change to the collection
  should keep every positive POST using `{{$timestamp}}` for
  uniqueness, to avoid cross-run id collisions.

## Current state

Phase 8 dockerisation is complete:

- [x] Compose file boots both PostgreSQL databases and both Spring
      Boot services end-to-end
- [x] `application-docker.properties` per service overrides the DB
      URL to the container hostname (`restaurant-db` / `menu-db`)
- [x] `.dockerignore` per service keeps `target/`, `.idea/`,
      `.claude/`, `.git/` out of the build context
- [x] DB healthchecks (`pg_isready`) + app healthchecks
      (`curl /actuator/health`) + `depends_on: service_healthy`
- [x] `.env.example` template and runbook
- [x] Postman collection with JWT auto-mint (Phase 7)

Handover state (at commit `50b8e1d`):

- 6-container compose all healthy (`dev-gateway` profile on).
- Both smoke scripts exit 0, cross-service log captured to
  `evidence/cross-service-smoke_20260420T115714Z.log`.
- Newman over the full Postman pack: 39 requests, 68/68 assertions,
  0 failures. One known brittleness in `PUT /restaurants/{id}` body
  is documented but not blocking.

Conditional expansions:

- Kafka broker + topic config for the `menu-events` producer --
  only if Sten elects to participate in W2/W3 post-CP#3.
- An "Async Evidence" folder in Postman, if the swap lands.
