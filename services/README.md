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

The Spring Boot project skeletons under `restaurant-service/` and
`menu-service/` are not created yet; they are scaffolded in **Phase 2**
of the master plan
(`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`).
The `local-dev/` Docker Compose stack is also wired up in Phase 2. The
`frontend/` folder is created in **Phase 12**.

## Conventions

Naming, package layout, image tags, and env-var keys are pinned in
[`0003-conventions.md`](../dev-docs/decisions/0003-conventions.md).
Summary:

- **Maven groupId:** `ee.ut.esi.quickbite`
- **Root package per service:** `ee.ut.esi.quickbite.<service>`
- **Docker image tag:** `quickbite-<service>:dev`
- **Compose service name:** `<service>` (no `quickbite-` prefix)
