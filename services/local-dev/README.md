# Local Dev

Docker Compose stack, environment-variable template, seed data, and
helper scripts for running Sierra-Lima's services on a developer
laptop.

## Planned layout

Populated in **Phase 2** of the master plan.

```
local-dev/
  docker-compose.yml       restaurant-service, menu-service, their DBs, (later) gateway, kafka, consumers
  .env.example             tracked template for all environment variables
  .env.local               git-ignored local overrides (real passwords, JWT secrets)
  runbook.md               how to bring the stack up, reset data, inspect logs
  postman/                 Postman collection and environment (created in Phase 8)
  seed/                    optional SQL dumps if Flyway seed grows large
```

## Standard usage (once Phase 2 lands)

```
cd services/local-dev
cp .env.example .env.local
docker compose up --build
```

All ports, image names, and service names follow the matrix in the
master plan §9 Phase 2 Task 10 and the conventions in
[`0003-conventions.md`](../../dev-docs/decisions/0003-conventions.md):

- `restaurant-service` exposes the configured HTTP port and depends on
  `restaurant-db` (PostgreSQL 15).
- `menu-service` exposes the configured HTTP port and depends on
  `menu-db` (PostgreSQL 15).
- Single shared bridge network `quickbite-net`.
- Volumes `restaurant_db_data` and `menu_db_data`.

## Environment variables

Prefixes: `DB_`, `JWT_`, `SPRING_`, `<SERVICE>_SERVICE_URL`. The
authoritative list lives in `.env.example` once Phase 2 lands. The
dev JWT signing secret is an HS256 shared key per
[`0005-non-goals.md`](../../dev-docs/decisions/0005-non-goals.md) N8
-- no Vault, no KMS.

## Not included here

- Teammates' services (User, Order, Payment, Delivery, Notification).
  They run from their own repos during integration rehearsals.
- Eureka, Spring Cloud Config, or any discovery server -- explicitly
  non-goal N4 in
  [`0005-non-goals.md`](../../dev-docs/decisions/0005-non-goals.md).
- Kafka broker. Added to this Compose file in **Phase 10** (W2/W3
  async rehearsal support); until then Sierra-Lima's services run
  without it.

## Current state

No Compose file yet. Scaffolded in **Phase 2**. Postman collection
arrives in **Phase 8**. Kafka is wired in **Phase 10**.
