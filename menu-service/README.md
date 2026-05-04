# Menu Service

Sten's Spring Boot service that owns the `MenuItem` aggregate
(requirements **R21** add/update/remove menu items and **R22** browse
menu). Implemented in **Phase 2** of the master plan and hardened
through Phases 5-19.

Owner: Sten. Authoritative API shape:
[`../../dev-docs/decisions/0020-sten-contracts.md`](../../dev-docs/decisions/0020-sten-contracts.md).
Authoritative auth matrix:
[`../../dev-docs/decisions/0010-auth-contract.md`](../../dev-docs/decisions/0010-auth-contract.md).

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
- Serves the W1 batch validation
  `POST /menu-items/validate` (see
  [`../../dev-docs/decisions/0030-w1-synchronous-contract-lock.md`](../../dev-docs/decisions/0030-w1-synchronous-contract-lock.md)).
- Serves browse routes (R22) and owner-gated CRUD (R21).
- Persists to a service-local PostgreSQL database (`menu-db` in
  Compose).
- Does **not** store restaurant metadata -- that belongs to
  `restaurant-service`. Cross-service references are by UUID only,
  no foreign keys (decision `0001-scope-freeze.md`).
- Optional: publishes `menu-events` on availability change. Default
  implementation is log-only (Phase 16 stance per
  [`0040`](../../dev-docs/decisions/0040-phase-16-async-stance.md));
  the Kafka swap is a one-class drop-in.

## API surface

| Method | Path | Who | Notes |
|---|---|---|---|
| `POST` | `/restaurants/{rid}/menu-items` | Owner of `{rid}` / Admin | Create. 201 on success. |
| `GET` | `/restaurants/{rid}/menu-items` | Public | Browse (R22). |
| `GET` | `/menu-items/{id}` | Public | Fetch by id. |
| `PUT` | `/menu-items/{id}` | Owner of parent restaurant / Admin | Update. |
| `DELETE` | `/menu-items/{id}` | Owner of parent restaurant / Admin | Remove. 204 on success. |
| `POST` | `/menu-items/validate` | Any authenticated | W1 batch validation. |

Canonical endpoints, error envelope, and batch-validate payload shape
are pinned in `0020 §Menu`, `0030 §hop-5`, and `0031` (status codes).
The optional `menu.item-availability-changed` Kafka producer is a
Phase 16 stretch, not baseline scope (`0040`).

## Run locally

```bash
# Unit + integration tests (47 tests)
cd services/menu-service
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

- **Before editing a controller or DTO**, re-read `0020` (API
  contract) and `0031` (status codes). Do not invent a status code
  for a new failure mode -- add a row to `0031` first.
- **Ownership checks go in the service layer**, not the controller.
  See `service/MenuService.java` for the pattern: fetch the parent
  restaurant id, delegate to `RestaurantOwnershipClient`, throw
  `ForbiddenException` on mismatch.
- **Errors flow through `GlobalExceptionHandler`.** Custom exceptions
  in `exception/` each map to one status code per `0031`. Do not
  `throw new ResponseStatusException` from controllers.
- **Flyway migrations are append-only.** Add `V3__<name>.sql`; do not
  edit `V1__init.sql` or `V2__seed_demo_data.sql`.
- **The log-only publisher is intentional.** Do not swap in a Kafka
  implementation without reading `0040` first -- the stance is
  "log-only unless teammate async is late".

## Related decisions

- [`0001-scope-freeze.md`](../../dev-docs/decisions/0001-scope-freeze.md)
  -- Sten owns this service.
- [`0002-workflows.md`](../../dev-docs/decisions/0002-workflows.md)
  -- W1 role as synchronous callee (hop 5).
- [`0003-conventions.md`](../../dev-docs/decisions/0003-conventions.md)
  -- naming, package, Docker, env var.
- [`0010-auth-contract.md`](../../dev-docs/decisions/0010-auth-contract.md)
  -- JWT shape, role matrix.
- [`0020-sten-contracts.md`](../../dev-docs/decisions/0020-sten-contracts.md)
  -- full API contract for this service.
- [`0030-w1-synchronous-contract-lock.md`](../../dev-docs/decisions/0030-w1-synchronous-contract-lock.md)
  -- W1 hop-5 request/response.
- [`0031-cross-service-status-code-table.md`](../../dev-docs/decisions/0031-cross-service-status-code-table.md)
  -- error envelope + status codes.
- [`0032-w2-w3-event-contract-lock.md`](../../dev-docs/decisions/0032-w2-w3-event-contract-lock.md)
  -- `menu-events` envelope (log-only by default).
- [`0040-phase-16-async-stance.md`](../../dev-docs/decisions/0040-phase-16-async-stance.md)
  -- async non-participation + Kafka swap trigger.
