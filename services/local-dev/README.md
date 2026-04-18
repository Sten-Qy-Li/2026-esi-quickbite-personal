# Local Dev

Docker Compose stack, environment-variable template, and Postman
workspace for running Sierra-Lima's services on a developer laptop.

Operational steps (start, reset, logs) live in
[`runbook.md`](runbook.md). This file is the reference index: what
exists, what ports each piece uses, and where to look next.

## Layout

```
local-dev/
  docker-compose.yml       restaurant-db + menu-db + restaurant-service + menu-service
  .env.example             tracked template for all environment variables
  .env.local               git-ignored local overrides (created from .env.example on first run)
  runbook.md               how to bring the stack up, reset data, inspect logs, run services
  postman/
    QuickBite.postman_collection.json    Phase 7 collection with JWT auto-mint and negative-auth scenarios
    QuickBite.postman_environment.json   shared variables (base URLs, ids, tokens)
```

## Port and env-var matrix (master plan §9 Phase 2 Task 10)

| Service              | Service Port | DB Port | Owner           | Scaffolded here? |
|----------------------|--------------|---------|-----------------|------------------|
| API Gateway          | 8080         | --      | Alfa-Kilo       | No (external)    |
| User Service         | 8085         | 5435    | Alfa-Kilo       | No (external)    |
| Order Service        | 8086         | 5436    | Alfa-Kilo       | No (external)    |
| Restaurant Service   | 8081         | 5432    | Sierra-Lima     | **Yes**          |
| Menu Service         | 8082         | 5433    | Sierra-Lima     | **Yes**          |
| Payment Service      | 8083         | 5437    | Elephant-Yankee | No (external)    |
| Delivery Service     | 8084         | 5438    | Elephant-Yankee | No (external)    |
| Notification Service | 8087         | 5439    | Mike-Alfa       | No (external)    |
| Kafka broker         | 9092         | --      | Mike-Alfa       | No (Phase 10+)   |

Sierra-Lima services read the following environment variables (names
fixed by the master plan; values in `.env.example`):

| Variable                  | Used by                         | Notes                                               |
|---------------------------|---------------------------------|-----------------------------------------------------|
| `DB_URL`                  | restaurant-service, menu-service| JDBC URL, defaults match the port matrix above      |
| `DB_USER`, `DB_PASSWORD`  | restaurant-service, menu-service| From `.env.local` via IntelliJ Run Configuration    |
| `JWT_SECRET`              | Phase 7+                        | Base64 HS256 key (dev only; placeholder committed)  |
| `JWT_ISSUER`              | Phase 7+                        | `quickbite-user-service`                            |
| `RESTAURANT_SERVICE_URL`  | (future callers)                | `http://localhost:8081` by default                  |
| `MENU_SERVICE_URL`        | (future callers)                | `http://localhost:8082` by default                  |

Rationale for keeping the HS256 JWT secret in `.env.example` (not
Vault/KMS) is non-goal **N8** in
[`0005-non-goals.md`](../../dev-docs/decisions/0005-non-goals.md).

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
  They run from their own repos during integration rehearsals.
- Eureka, Spring Cloud Config, or any discovery server -- explicit
  non-goal **N4** in
  [`0005-non-goals.md`](../../dev-docs/decisions/0005-non-goals.md).
- Kafka broker. Added to this Compose file in **Phase 10** (W2/W3
  async rehearsal support).

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

Next expansions:

- Phase 10: add Kafka broker + topic config for W2/W3.
- Phase 16: add Async Evidence folder in Postman.
