# Services

Implementation code for Sierra-Lima's part of the QuickBite ESI project.

## Planned layout

```
services/
  restaurant-service/    Maven + Spring Boot project (owner: Sierra-Lima)
  menu-service/          Maven + Spring Boot project (owner: Sierra-Lima)
  local-dev/             docker-compose.yml, .env.example, runbook
  frontend/              Vue.js 3 project (created in Phase 12)
```

Only Sierra-Lima's services live in this repository. Teammates' services
(User, Order, Payment, Delivery, Notification, API Gateway,
Notification/Kafka config) are in their own repos and are reached by
static URLs in Docker Compose, as agreed in decision
[`0005-non-goals.md`](../dev-docs/decisions/0005-non-goals.md) N4.

## Current state

All four subdirectories are implemented:

- `restaurant-service/` and `menu-service/` -- Maven + Spring Boot
  services with Flyway-backed schema, JWT auth, controller/service/
  repository layers, and unit/integration tests.
- `local-dev/` -- Docker Compose stack (both databases, both services,
  dev gateway, and frontend), PowerShell/bash smoke scripts, and the
  Postman workspace. See [`local-dev/README.md`](local-dev/README.md)
  and [`local-dev/runbook.md`](local-dev/runbook.md).
- `frontend/` -- Vue.js 3 project consumed by the dev gateway.

Run commands:

- Unit tests per service: `mvn clean test` from
  `services/restaurant-service/` or `services/menu-service/`.
- Full stack (DBs + services + gateway + frontend):
  `docker compose --profile dev-gateway up -d --build` from
  `services/local-dev/`.
- Smoke tests: `pwsh -File services/local-dev/smoke.ps1` (Sierra-Lima
  owned) and `pwsh -File services/local-dev/smoke-cross-service.ps1`
  (cross-service probes).

Phase history lives in the master plan
(`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`).

## Conventions

Naming, package layout, image tags, and env-var keys are pinned in
[`0003-conventions.md`](../dev-docs/decisions/0003-conventions.md).
Summary:

- **Maven groupId:** `ee.ut.esi.quickbite`
- **Root package per service:** `ee.ut.esi.quickbite.<service>`
- **Docker image tag:** `quickbite-<service>:dev`
- **Compose service name:** `<service>` (no `quickbite-` prefix)
