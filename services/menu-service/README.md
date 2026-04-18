# Menu Service

Sierra-Lima's Spring Boot service that owns the Menu Item aggregate
(requirements **R21** add/update/remove menu items and **R22** browse
menu).

## Planned layout

Created as a Maven project in **Phase 2** of the master plan.

```
menu-service/
  pom.xml
  Dockerfile
  src/
    main/
      java/ee/ut/esi/quickbite/menu/
        MenuServiceApplication.java
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
      java/ee/ut/esi/quickbite/menu/
```

## Responsibilities

- Owns the `MenuItem` aggregate root (name, description, price,
  category, availability flag, `restaurantId` reference).
- Serves the W1 batch validation
  `POST /menu-items/validate` (see
  [`0002-workflows.md`](../../dev-docs/decisions/0002-workflows.md)).
- Serves browse routes `GET /restaurants/{rid}/menu-items` and
  `GET /menu-items/{id}`.
- Persists to a service-local PostgreSQL database (`menu-db` in
  Compose).
- Does **not** store restaurant metadata -- that belongs to
  `restaurant-service`. Cross-service references are by UUID only,
  no foreign keys (decision
  [`0001-scope-freeze.md`](../../dev-docs/decisions/0001-scope-freeze.md)).

## API surface

Canonical endpoints, error envelope, and batch-validate payload shape
are pinned in the master plan Appendix F.2 and F.6. The optional
`menu.item-availability-changed` Kafka producer is a **Phase 16**
stretch, not baseline scope.

## Current state

No code yet. The Maven skeleton and first Flyway migration are created
in **Phase 2**. The aggregate and repository land in **Phase 5**. REST
controllers land in **Phase 6**. Auth (JWT filter) is added in
**Phase 7**.

## Related decisions

- [`0001-scope-freeze.md`](../../dev-docs/decisions/0001-scope-freeze.md)
  -- Sierra-Lima owns this service.
- [`0002-workflows.md`](../../dev-docs/decisions/0002-workflows.md)
  -- W1 role as synchronous callee.
- [`0003-conventions.md`](../../dev-docs/decisions/0003-conventions.md)
  -- naming, package, Docker, env var.
- [`0004-open-questions.md`](../../dev-docs/decisions/0004-open-questions.md)
  -- Q1, Q2, Q4, Q6, Q7 still to resolve.
