# Restaurant Service

Sierra-Lima's Spring Boot service that owns the `Restaurant` aggregate
(requirements **R19** register/manage restaurant and **R20** update
open/closed and operating hours). Implemented in **Phase 2** of the
master plan and hardened through Phases 5-19.

Owner: Sierra-Lima. Authoritative API shape:
[`../../dev-docs/decisions/0020-sierra-lima-contracts.md`](../../dev-docs/decisions/0020-sierra-lima-contracts.md).
Authoritative auth matrix:
[`../../dev-docs/decisions/0010-auth-contract.md`](../../dev-docs/decisions/0010-auth-contract.md).

## Layout

```
restaurant-service/
  pom.xml
  Dockerfile
  .dockerignore
  src/
    main/
      java/ee/ut/esi/quickbite/restaurant/
        RestaurantServiceApplication.java
        controller/                  HTTP entry points
        service/                     business logic + ownership checks
        repository/                  Spring Data JPA
        domain/                      JPA entities
        dto/                         request/response + bean-validation
        config/                      SecurityConfig, OpenApiConfig
        security/                    JwtAuthFilter
        exception/                   GlobalExceptionHandler + custom exceptions
      resources/
        application.yml              default profile
        application-docker.properties  overrides DB_URL to container hostname
        db/migration/                Flyway V1__init.sql, V2__seed_demo_data.sql
    test/
      java/ee/ut/esi/quickbite/restaurant/  33 tests at 50b8e1d
```

## Responsibilities

- Owns the `Restaurant` aggregate root (name, city, address,
  operating hours, open/closed flag, `ownerId` reference by UUID to a
  User).
- Serves the W1 availability check
  `GET /restaurants/{id}/availability` (see
  [`../../dev-docs/decisions/0030-w1-synchronous-contract-lock.md`](../../dev-docs/decisions/0030-w1-synchronous-contract-lock.md)).
- Persists to a service-local PostgreSQL database (`restaurant-db` in
  Compose).
- Does **not** own menu items -- those belong to `menu-service`.
  Cross-service references are by UUID only, no foreign keys
  (decision `0001-scope-freeze.md`).
- Does **not** provide a public `DELETE /restaurants/{id}` endpoint.
  See audit `50b8e1d` Finding 1 for the consequence (Postman pack
  accumulates rows across runs).

## API surface

| Method | Path | Who | Notes |
|---|---|---|---|
| `POST` | `/restaurants` | RestaurantOwner / Admin | Create. 201 on success. |
| `GET` | `/restaurants` | Public | Paged list with `city`, `isOpen` filters. |
| `GET` | `/restaurants/{id}` | Public | Fetch by id. |
| `PUT` | `/restaurants/{id}` | Owner of record / Admin | Full update. |
| `PATCH` | `/restaurants/{id}/status` | Owner / Admin | Toggle open/closed. |
| `GET` | `/restaurants/{id}/availability` | Any authenticated | W1 availability probe. |

Canonical endpoints, error envelope, and availability payload shape
are pinned in `0020 §Restaurant`, `0030 §hop-4`, and `0031` (status
codes). Browse routes are public by default, subject to Q6 in
[`0004-open-questions.md`](../../dev-docs/decisions/0004-open-questions.md).

Notable validation invariants (live-probed in the latest audit):

- `operatingHours` matches `^(?:[01][0-9]|2[0-3]):[0-5][0-9]-(?:[01][0-9]|2[0-3]):[0-5][0-9]$`
  (tightened at `15f5ab7`).
- Duplicate-name protection on rename returns `409` (added at
  `1a6e8c7`).
- Unsupported HTTP methods on valid paths return `405` (added at
  `7b2fa61`).

## Run locally

```bash
# Unit + integration tests (33 tests)
cd services/restaurant-service
mvn clean test

# From IntelliJ (fast iteration): run RestaurantServiceApplication
# with SPRING_PROFILES_ACTIVE=default and the DB container up.
# See ../local-dev/runbook.md §7.

# Full stack (DB + service + friends):
cd ../local-dev
docker compose --profile dev-gateway up -d --build
```

JWT auth (issuer-pinned HS256) is wired in
`security/JwtAuthFilter.java`. The shared dev secret and issuer live
in [`../local-dev/.env.example`](../local-dev/.env.example).

## For AI coding agents

- **Before editing a controller or DTO**, re-read `0020` (API
  contract) and `0031` (status codes). Do not invent a status code
  for a new failure mode -- add a row to `0031` first.
- **Ownership checks go in the service layer**, not the controller.
  See `service/RestaurantService.java` for the pattern: compare
  `record.ownerId` to the JWT subject, throw `ForbiddenException` on
  mismatch.
- **Errors flow through `GlobalExceptionHandler`.** Custom exceptions
  in `exception/` each map to one status code per `0031`. Do not
  `throw new ResponseStatusException` from controllers.
- **Flyway migrations are append-only.** Add `V3__<name>.sql`; do not
  edit `V1__init.sql` or `V2__seed_demo_data.sql`.
- **The `/availability` endpoint is W1 hop-4.** Any change to its
  response shape needs an amending decision to `0030`, not an inline
  tweak.

## Related decisions

- [`0001-scope-freeze.md`](../../dev-docs/decisions/0001-scope-freeze.md)
  -- Sierra-Lima owns this service.
- [`0002-workflows.md`](../../dev-docs/decisions/0002-workflows.md)
  -- W1 role as synchronous callee (hop 4).
- [`0003-conventions.md`](../../dev-docs/decisions/0003-conventions.md)
  -- naming, package, Docker, env var.
- [`0010-auth-contract.md`](../../dev-docs/decisions/0010-auth-contract.md)
  -- JWT shape, role matrix.
- [`0020-sierra-lima-contracts.md`](../../dev-docs/decisions/0020-sierra-lima-contracts.md)
  -- full API contract for this service.
- [`0030-w1-synchronous-contract-lock.md`](../../dev-docs/decisions/0030-w1-synchronous-contract-lock.md)
  -- W1 hop-4 request/response.
- [`0031-cross-service-status-code-table.md`](../../dev-docs/decisions/0031-cross-service-status-code-table.md)
  -- error envelope + status codes.
- [`0033-inter-service-token-propagation-lock.md`](../../dev-docs/decisions/0033-inter-service-token-propagation-lock.md)
  -- how callers propagate the bearer token.
