# Services

Executable code for Sierra-Lima's slice of the QuickBite ESI project.
Everything under this directory builds, runs, and is under test.
Documentation-only artefacts live in [`../dev-docs/`](../dev-docs/).

## Layout

```
services/
  restaurant-service/    Maven + Spring Boot (owner: Sierra-Lima, R19/R20)
  menu-service/          Maven + Spring Boot (owner: Sierra-Lima, R21/R22)
  local-dev/             Docker Compose, runbook, .env.example, Postman pack, smoke scripts
  frontend/
    quickbite-frontend/  Vue.js 3 + Vue Router (Phase 12)
```

Only Sierra-Lima's owned services are in this repository. Teammates'
services (User, Order, Payment, Delivery, Notification) live in their
own repositories and are reached by static URL in Docker Compose, per
non-goal **N4** in
[`../dev-docs/decisions/0005-non-goals.md`](../dev-docs/decisions/0005-non-goals.md).

## Current state (handover-ready)

All four subdirectories are implemented and passing their automated
checks. The most recent audit -- `50b8e1d` `Charlie-Lima-Alfa`
pre-team-integration-readiness -- records the following green signal:

- `mvn test` -- 33/33 restaurant, 47/47 menu (80/80 backend tests).
- `npm run lint` + `npm run build` on the frontend -- clean.
- `docker compose --profile dev-gateway up --build` -- 6 containers
  healthy (both DBs, both services, frontend, dev-gateway).
- `bash services/local-dev/smoke.sh` -- Sierra-Lima smoke passes.
- `bash services/local-dev/smoke-cross-service.sh` -- 0/0 failures,
  `menu-events` captured.
- Newman over the Postman pack -- 39 requests, 68/68 assertions (see
  Finding 1 in the audit for one brittleness in the pack; it is a
  collection-level issue, not a service-level defect).

See [`../dev-docs/audits/`](../dev-docs/audits/) for the full list of
audits and the most recent verdict.

## Run commands

```bash
# Per-service unit + integration tests
cd services/restaurant-service && mvn clean test
cd services/menu-service        && mvn clean test

# Full stack (DBs + services + dev gateway + frontend) from the repo root
cd services/local-dev
docker compose --profile dev-gateway up -d --build

# Smoke tests (post stack-up)
bash services/local-dev/smoke.sh                  # Sierra-Lima only
bash services/local-dev/smoke-cross-service.sh    # cross-service probes
```

Detailed operational steps are in
[`local-dev/runbook.md`](local-dev/runbook.md). Per-service details
are in each service's own README.

## Conventions (pinned in `0003-conventions.md`)

- **Maven groupId:** `ee.ut.esi.quickbite`
- **Root package per service:** `ee.ut.esi.quickbite.<service>`
- **Docker image tag:** `quickbite-<service>:dev`
- **Compose service name:** `<service>` (no `quickbite-` prefix, but
  the generated container *name* is `quickbite-<service>`).
- **Env-var naming:** `DB_*`, `JWT_*`, `<SERVICE>_SERVICE_URL` --
  see [`local-dev/.env.example`](local-dev/.env.example).

## For AI coding agents

- **Before editing**, read the relevant decision file in
  [`../dev-docs/decisions/`](../dev-docs/decisions/). The
  authoritative API shape is `0020`; the authoritative auth matrix is
  `0010`; the authoritative status codes are `0031`. Do not invent
  endpoint paths, HTTP status codes, or env-var names -- look them
  up.
- **Scope freeze.** Sierra-Lima owns only `restaurant-service` and
  `menu-service` (plus his share of the frontend + local-dev stack).
  Do not modify files that presuppose teammate-owned services.
- **Tests run fast.** `mvn test` in each service takes <1 min on a
  warm cache. Run them after every non-trivial change; they are the
  first line of regression defence.
- **`docker compose`** changes have a slow feedback loop (rebuilds
  take 3-5 min on a cold cache). Prefer running services from
  IntelliJ against the DB containers for fast iteration (see
  `local-dev/runbook.md` §7).
- **Do not "fix" audits or chat archives** to reflect new reality.
  They are append-only evidence. Write a new audit instead.

## Phase history

The master plan
([`../dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`](../dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md))
walks every implementation phase. Per-phase verification reports live
in [`../dev-docs/verification/`](../dev-docs/verification/).
