# Restaurant Service

Sierra-Lima's Spring Boot service that owns the Restaurant aggregate
(requirements **R19** register/manage restaurant and **R20** update
open/closed and operating hours).

## Planned layout

Created as a Maven project in **Phase 2** of the master plan.

```
restaurant-service/
  pom.xml
  Dockerfile
  src/
    main/
      java/ee/ut/esi/quickbite/restaurant/
        RestaurantServiceApplication.java
        controller/
        service/
        repository/
        domain/
        dto/
        config/
        security/
      resources/
        application.yml
        db/migration/               Flyway V*__*.sql
    test/
      java/ee/ut/esi/quickbite/restaurant/
```

## Responsibilities

- Owns the `Restaurant` aggregate root (restaurant metadata, operating
  hours, open/closed flag, `ownerId` reference to a User).
- Serves the W1 availability check
  `GET /restaurants/{id}/availability` (see
  [`0002-workflows.md`](../../dev-docs/decisions/0002-workflows.md)).
- Persists to a service-local PostgreSQL database (`restaurant-db` in
  Compose).
- Does **not** own menu items -- those belong to `menu-service`.

## API surface

Canonical endpoints, error envelope, and availability payload shape are
pinned in the master plan Appendix F.1 and F.6. Browse routes are
public by default, subject to the Phase 1 team decision recorded as
Q6 in [`0004-open-questions.md`](../../dev-docs/decisions/0004-open-questions.md).

## Current state

Implemented. The service boots from `RestaurantServiceApplication`,
exposes the six endpoints below, persists to a Flyway-backed PostgreSQL
schema (`V1__init.sql` + `V2__seed_demo_data.sql`), and ships with
controller, service, and repository tests.

Endpoints:

- `POST /restaurants` -- create (owner/admin)
- `GET /restaurants` -- paged list with `city` / `isOpen` filters (public)
- `GET /restaurants/{id}` -- fetch by id (public)
- `PUT /restaurants/{id}` -- update (owner of record or admin)
- `PATCH /restaurants/{id}/status` -- toggle open/closed (owner or admin)
- `GET /restaurants/{id}/availability` -- W1 availability probe (any authenticated role)

Run locally:

- Tests: `mvn clean test` from this directory.
- Full stack (DB + service + friends): see
  [`../local-dev/README.md`](../local-dev/README.md).

JWT auth (issuer-pinned HS256) is wired in `security/JwtAuthFilter`;
the shared dev secret and issuer live in `.env.example`.

## Related decisions

- [`0001-scope-freeze.md`](../../dev-docs/decisions/0001-scope-freeze.md)
  -- Sierra-Lima owns this service.
- [`0002-workflows.md`](../../dev-docs/decisions/0002-workflows.md)
  -- W1 role as synchronous callee.
- [`0003-conventions.md`](../../dev-docs/decisions/0003-conventions.md)
  -- naming, package, Docker, env var.
- [`0004-open-questions.md`](../../dev-docs/decisions/0004-open-questions.md)
  -- Q1, Q4, Q5, Q6, Q7 still to resolve.
