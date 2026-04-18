# Local Dev

Docker Compose stack, environment-variable template, and Postman
workspace for running Sierra-Lima's services on a developer laptop.

Operational steps (start, reset, logs) live in
[`runbook.md`](runbook.md). This file is the reference index: what
exists, what ports each piece uses, and where to look next.

## Layout

```
local-dev/
  docker-compose.yml       restaurant-db + menu-db (Spring Boot services run from IntelliJ / mvn)
  .env.example             tracked template for all environment variables
  .env.local               git-ignored local overrides (created from .env.example on first run)
  runbook.md               how to bring the stack up, reset data, inspect logs, run services
  postman/
    QuickBite.postman_collection.json    12-endpoint collection (Phase 2 skeleton)
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
  host port 5432. Named volume `restaurant_db_data`.
- `quickbite-menu-db` (PostgreSQL 15): container port 5432 → host
  port 5433. Named volume `menu_db_data`.
- Shared bridge network `quickbite-net`.

Spring Boot services **do not** run in Docker during Phases 2-6 --
run them from IntelliJ or `mvn spring-boot:run`. See
[`runbook.md`](runbook.md) §7.

## Not included here

- Teammates' services (User, Order, Payment, Delivery, Notification).
  They run from their own repos during integration rehearsals.
- Eureka, Spring Cloud Config, or any discovery server -- explicit
  non-goal **N4** in
  [`0005-non-goals.md`](../../dev-docs/decisions/0005-non-goals.md).
- Kafka broker. Added to this Compose file in **Phase 10** (W2/W3
  async rehearsal support).

## Current state

Phase 2 scaffolding is in place:

- [x] Compose file for both PostgreSQL databases
- [x] `.env.example` template
- [x] Runbook for start/reset/logs/health
- [x] Postman collection skeleton (12 endpoints, all using
      `{{restaurantBaseUrl}}` / `{{menuBaseUrl}}`)
- [x] Postman environment with placeholders for `jwtToken`,
      `customerToken`, `ownerToken`, `adminToken` (populated in Phase 7)

Next expansions:

- Phase 10: add Kafka broker + topic config for W2/W3.
- Phase 16: add Async Evidence folder in Postman.
