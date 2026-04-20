# Chat Archive - 2026-04-18 - Charlie-Lima-Alfa (`213d26c`)

## Session Summary

This session executed **Phase 7 -- Sierra-Lima Hardening Pass** end to
end for the Restaurant Service and Menu Service, as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
§9 Phase 7 (lines 1001-1087).

The session began on top of `213d26c` ("Archive Phase 2-6 verification
sign-off evidence and session chats"). A context-compaction
auto-summary occurred mid-session after tasks 1-7 were already in
place but before task 8's menu-service tests, task 9's Postman
refresh, and task 10's Assignment 1 guard were written. The archived
context below covers the entire phase from fresh start to full green
build, including the work that existed before compaction.

All ten Phase 7 tasks reached the Definition of Done. `mvn -B test`
produces 46 passing tests across both services. The two services are
demo-grade for the CP#1 consultation on 2026-04-28.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`
- Today: 2026-04-18 (Saturday)
- Active branch: `dev`
- Parent commit: `213d26c` -- "Archive Phase 2-6 verification sign-off
  evidence and session chats"
- Environment: Windows 11 + IntelliJ IDEA 2026.1 + Git Bash
- Java: 17.0.18 (Microsoft OpenJDK) via `ms-17` project SDK
- Test framework: JUnit 5, Mockito, Spring Boot Test, H2 in
  PostgreSQL mode

## Phase 7 Task-by-Task Record

### Task 1 -- Dev JWT generator utility

New package `security/` added to each service with these classes:

- `JwtProperties` -- `@Configuration` that reads `jwt.secret`,
  `jwt.issuer`, `jwt.ttl` (default `PT1H`) via `@Value`.
- `JwtDevMint` -- static `mint(...)` builder using `jjwt 0.11.5`.
  Exposes three default UUIDs used throughout the test suite and
  Postman collection:

  | Role              | UUID                                       |
  |-------------------|--------------------------------------------|
  | `Customer`        | `00000000-0000-0000-0000-0000000000c1`     |
  | `RestaurantOwner` | `00000000-0000-0000-0000-000000000001`     |
  | `Admin`           | `00000000-0000-0000-0000-0000000000a1`     |

  The token payload is `{ iss, sub, userId, role, tokenType, iat,
  exp }` with an optional `serviceName` claim; HS256 over a base64
  secret decoded via `Keys.hmacShaKeyFor(Decoders.BASE64.decode(...))`.
- `AuthenticatedUser` -- `record AuthenticatedUser(UUID userId, String
  role, String tokenType, String serviceName)`.
- `SecurityRoles` -- constants `CUSTOMER`, `RESTAURANT_OWNER`,
  `DRIVER`, `ADMIN`, `SERVICE_TOKEN_TYPE`.
- `CurrentUser` -- `@Component` with `current()` Optional and
  `require()` helper that throws `AccessDeniedException` if the
  context is unauthenticated.

### Task 2 -- JwtAuthFilter + real SecurityConfig

- `JwtAuthFilter extends OncePerRequestFilter` -- reads the
  `Authorization` header, parses the JWS, pulls `userId`, `role`,
  `tokenType`, `serviceName`, and installs a
  `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`
  with authority `ROLE_<role>`. On `JwtException`, writes a 401
  JSON error envelope via `RestAuthEntryPoints` and halts the
  chain.
- `RestAuthEntryPoints` -- bundles `AuthenticationEntryPoint` (401)
  and `AccessDeniedHandler` (403) that both produce the canonical
  `ErrorResponse` JSON, so unauthenticated and forbidden requests
  look the same shape as domain errors.
- `SecurityConfig` in both services rewritten from the Phase 2 stub:
  - CSRF disabled, CORS whitelists `http://localhost:5173` and
    `http://localhost:8080`.
  - Session policy `STATELESS`.
  - Actuator health/info, SpringDoc paths, and `OPTIONS /**` are
    `permitAll`.
  - Public GETs for `/restaurants`, `/restaurants/{id:[0-9a-fA-F-]+}`
    (Restaurant) and `/restaurants/*/menu-items`,
    `/menu-items/{id:[0-9a-fA-F-]+}` (Menu).
  - `/restaurants/*/availability` and `/menu-items/validate` require
    any authenticated user.
  - All mutations require `.hasAnyRole("RESTAURANT_OWNER", "ADMIN")`.
  - `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`.
  - `@EnableMethodSecurity` turned on for future per-method locks.

### Task 3 -- Route-protection matrix

See the table in §1 of
`dev-docs/verification/phase-7-verification_Sierra-Lima.md`. First
match wins, so the availability matcher precedes the generic
`/restaurants/{id}` matcher to avoid shadowing.

### Task 4 -- Error envelope standardisation

Both services' `GlobalExceptionHandler` produce:

```json
{
  "timestamp": "<OffsetDateTime>",
  "status":    <int>,
  "error":     "<reason phrase>",
  "message":   "<human message>",
  "path":      "<request URI>",
  "validationErrors": [ { "field": "...", "message": "..." } ]
}
```

`validationErrors` is only populated on 400 from
`MethodArgumentNotValidException`. 401 and 403 use the same envelope
via `RestAuthEntryPoints`.

### Task 5 -- Validation tightening

- **Restaurant** -- `DuplicateRestaurantException` (new) handled as
  409. `RestaurantService.create` first checks
  `restaurants.existsByOwnerIdAndNameIgnoreCase(ownerId, req.name())`
  using the current authenticated user's `userId` as owner.
- **Menu** -- `InvalidPriceException` (new) handled as 422.
  `MenuService.validatePrice(BigDecimal)` rejects `<= 0` and
  `stripTrailingZeros().scale() > 2`. Moved out of DTO-level
  `@DecimalMin` / `@Digits` so the error emits as semantic 422
  rather than syntactic 400.
- **Menu** -- empty `ValidateMenuItemsRequest.items` returns 400 via
  bean validation (`@NotEmpty`).
- **Menu** -- unknown `category` (not in `{Appetizer, Main, Dessert,
  Drink}`) is accepted but logged at DEBUG by
  `warnIfUnknownCategory`.

### Task 6 -- Auditing

`AuditingConfig` in each service exposes an `AuditorAware<UUID>` that
reads the authenticated principal's `userId` from
`SecurityContextHolder`. When the context is empty (tests, Flyway
migrations, pre-auth requests), it falls back to
`SYSTEM_USER = 00000000-0000-0000-0000-000000000000`. `@CreatedDate`
/ `@LastModifiedDate` are already on the entities from Phase 2-6.
`@EnableJpaAuditing(auditorAwareRef = "auditorAware")` is on each
`SpringBootApplication` class.

### Task 7 -- V2 Flyway seed migrations

Files created:

- `services/restaurant-service/src/main/resources/db/migration/V2__seed_demo_data.sql`
  -- 6 restaurants (Pizza Antonio, Sushi Lumi, Cafe Nero [closed],
  Burger Bros, Vegan Vibes, Pasta Palace [closed]) with stable UUIDs
  `d0000001-...` through `d0000006-...` under three owners
  (`...0001`, `...0002`, `...0003`). Mix of `is_open = true/false`.
- `services/menu-service/src/main/resources/db/migration/V2__seed_demo_data.sql`
  -- 16 menu items with stable UUIDs that encode their restaurant:
  `e0000011-...0011` is a menu item on `d0000001-...0001`, and so on.
  Mix of available / unavailable across `Main`, `Appetizer`,
  `Dessert`, `Drink`.

Both use `ON CONFLICT DO NOTHING` for idempotency.

### Task 8 -- Controller + service tests

Added `com.h2database:h2` (test scope) to both `pom.xml` files.

`src/test/resources/application-test.properties` in each service:
H2 in PostgreSQL mode (`MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE`),
`ddl-auto=create-drop`, Flyway disabled, H2Dialect, and the
convention test JWT secret
(`dGVzdC1zZWNyZXQtZm9yLWRldi1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZC0xMjM0NTY=`).

Test classes:

| File                                   | Kind       | Tests |
|----------------------------------------|------------|-------|
| `RestaurantServiceApplicationTests`    | context    | 1     |
| `service/RestaurantServiceTest`        | Mockito    | 5     |
| `controller/RestaurantControllerTest`  | MockMvc    | 12    |
| `MenuServiceApplicationTests`          | context    | 1     |
| `service/MenuServiceTest`              | Mockito    | 10    |
| `controller/MenuControllerTest`        | MockMvc    | 17    |
| **Total**                              |            | **46** |

Restaurant controller tests assert the full matrix: public list/get,
404 not-found envelope, availability 401 without token / 200 with
customer token, create 401 / 403 (customer) / 201 (owner) / 201
(admin), 400 on missing `name`, 401 on garbage bearer, 403 on PATCH
`/status` from a customer token.

Menu controller tests likewise cover public list/get, 404,
create 401 / 403 / 201 / 201, 422 on `priceAmount = 0`, 400 on
missing `name`, 401 and 200 on `/menu-items/validate`, 400 on
empty `items`, 401 / 403 / 204 / 404 on DELETE, and 401 on garbage
bearer.

Menu service-layer tests cover happy-path create, zero price,
negative price, 3-decimal price, missing `findById`, `update`
applies details, `update` missing, `delete` missing, `validate`
mixing found / missing / unavailable, and `validate` all-valid.

`mvn -B test` ran green at 2026-04-18 16:23 local:
`Tests run: 46, Failures: 0, Errors: 0, Skipped: 0`.

### Task 9 -- Postman refresh

- `services/local-dev/postman/QuickBite.postman_collection.json`
  gained a **collection-level pre-request script** that mints fresh
  HS256 JWTs on every request via CryptoJS using the same base64
  secret as the dev default in `application.properties`. It sets
  `customerToken`, `ownerToken`, `adminToken`, and the back-compat
  alias `jwtToken` in the environment.
- **Auth (Tokens)** folder replaces the old "Login (placeholder)"
  with three stub requests -- one per role -- that hit `/actuator/health`
  and print the minted token to the Postman console with a `pm.test`
  assertion on the JWT shape. A fourth stub preserves the eventual
  `POST /api/auth/login` call once Alfa-Kilo's User Service ships.
- **Negative Auth** folder (previously empty) now has 8 scenarios
  with `pm.test` assertions on status code and envelope shape:

  | Scenario                                                                 | Expected |
  |--------------------------------------------------------------------------|----------|
  | `GET /restaurants/{id}/availability` with no token                       | 401      |
  | `GET /availability` with garbage Bearer                                  | 401      |
  | `POST /restaurants` with customer token                                  | 403      |
  | `PATCH /restaurants/{id}/status` with customer token                     | 403      |
  | `DELETE /menu-items/{id}` with customer token                            | 403      |
  | `GET /restaurants/{missing-uuid}`                                        | 404      |
  | `POST /menu-items/validate` with empty `items` array                     | 400      |
  | `POST /restaurants/{rid}/menu-items` with `priceAmount = 0`              | 422      |

- `QuickBite.postman_environment.json` gained two override keys,
  `jwtSecret` and `jwtIssuer`, for the case where Sierra-Lima runs
  the services with a non-default `JWT_SECRET`. Empty values make
  the pre-request script use the bundled dev defaults.

Both JSON files validate against `python -m json.tool`.

### Task 10 -- Assignment 1 guard

Verified (no file changes needed):

- `dev-docs/prior-submissions/assignment-3_figure1_business-architecture.png`
  -- shows 8 business services only. No API Gateway, no Kafka, no DB
  cylinders. Marker legend distinguishes implemented vs. design-only.
- `dev-docs/prior-submissions/assignment-3_figure1b_implementation-architecture.png`
  -- the infrastructure layer (API Gateway, Kafka, per-service DB
  cylinders) lives here and nowhere else.
- `services/local-dev/docker-compose.yml` -- two independent
  PostgreSQL 15 containers (`quickbite-restaurant-db`,
  `quickbite-menu-db`) with separate volumes
  (`restaurant_db_data`, `menu_db_data`) and separate host ports
  parameterised via `.env.local`. No cross-container volume sharing.

Both points from
`dev-docs/prior-submissions/Assignment-1_Feedback.txt`
("Infrastructure elements mixed into architecture diagram" and
"Shared database across microservices") are prevented by
construction and guarded by Figure 1 plus the compose file.

## Files Created or Updated

### New source files

- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/security/`
  - `JwtProperties.java`
  - `JwtDevMint.java`
  - `AuthenticatedUser.java`
  - `SecurityRoles.java`
  - `JwtAuthFilter.java`
  - `RestAuthEntryPoints.java`
  - `CurrentUser.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/exception/DuplicateRestaurantException.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/security/` (mirror of restaurant security)
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/InvalidPriceException.java`

### New Flyway migrations

- `services/restaurant-service/src/main/resources/db/migration/V2__seed_demo_data.sql`
- `services/menu-service/src/main/resources/db/migration/V2__seed_demo_data.sql`

### New test files

- `services/restaurant-service/src/test/resources/application-test.properties`
- `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/service/RestaurantServiceTest.java`
- `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantControllerTest.java`
- `services/menu-service/src/test/resources/application-test.properties`
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/service/MenuServiceTest.java`
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/controller/MenuControllerTest.java`

### Modified

- Both `pom.xml` files: added `com.h2database:h2` test dependency.
- Both `config/SecurityConfig.java`: rewritten from Phase 2 stub.
- Both `config/AuditingConfig.java`: `AuditorAware<UUID>` reads
  authenticated principal's `userId`.
- Both `exception/GlobalExceptionHandler.java`: added handlers for
  `DuplicateRestaurantException` (409) and `InvalidPriceException`
  (422); kept the existing 400 / 404 / 500 branches.
- `RestaurantService.java`: constructor takes `CurrentUser`;
  `create(...)` uses the current user as owner and checks duplicate
  name.
- `RestaurantRepository.java`: added
  `existsByOwnerIdAndNameIgnoreCase(UUID, String)`.
- `MenuService.java`: added `validatePrice` (throwing
  `InvalidPriceException`) and `warnIfUnknownCategory` (DEBUG log).
- `CreateMenuItemRequest.java` / `UpdateMenuItemRequest.java`:
  removed `@DecimalMin` / `@Digits` on `priceAmount` so the rule
  surfaces as 422 via the service, not 400 via the DTO.
- Both `src/test/java/.../*ApplicationTests.java`: replaced
  placeholder with `@SpringBootTest @ActiveProfiles("test") void
  contextLoads()`.
- `services/local-dev/postman/QuickBite.postman_collection.json`:
  auto-mint pre-request script, Auth (Tokens) folder, populated
  Negative Auth folder.
- `services/local-dev/postman/QuickBite.postman_environment.json`:
  added `jwtSecret` and `jwtIssuer` overrides.

### New documentation

- `dev-docs/verification/phase-7-verification_Sierra-Lima.md` -- the
  end-of-phase sign-off doc. Content-compatible with the
  `phase-2-to-6-verification_Sierra-Lima.md` predecessor.
- `dev-docs/agent-context/2026-04-18_chat-archive_Charlie-Lima-Alfa_213d26c.md`
  -- this archive.

## Open Questions Movement

No Open Questions changed status in this session. Phase 7 consumed
the security-related unknowns (`Q3` auth contract, `Q7` JWT
expectations), but the master plan already recorded those as
"answered" before this session started.

## Suggested Next Steps

1. Push to `origin/dev` and share the branch with the team (Alfa-Kilo
   will especially need the canonical token-shape expectations for
   the User Service login endpoint; the pre-request script shows
   exactly what it needs to emit).
2. Dry-run the Postman **Negative Auth** folder once both services
   are running locally, to confirm the CryptoJS JWT minter and the
   real `JwtAuthFilter` agree on HS256 signature bytes.
3. Move to Phase 8 -- Dockerise both services. Prerequisites are now
   met.

## Repository State at End of Session

- Branch: `dev`
- Parent HEAD at start of session: `213d26c`
- Working tree before commit: 17 modified files + 5 untracked
  directory trees (security packages, test packages, test
  resources, V2 migrations) + `phase-7-verification_Sierra-Lima.md`
  + this archive file.
- `mvn -B test` in both services: 46 tests, 0 failures, 0 errors.
- Remote `origin/dev`: in sync with `213d26c` until the follow-up
  push.
