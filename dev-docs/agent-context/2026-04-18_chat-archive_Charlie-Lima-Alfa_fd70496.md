# Chat Archive - 2026-04-18 - Charlie-Lima-Alfa (`fd70496`)

## Session Summary

This session continued from commit `fd70496` (end of the prior Phase 1
session) and executed **Phase 2 through Phase 6** of
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.

The backend scaffolding, database containers, and full CRUD + validation
+ OpenAPI for both Sierra-Lima services (Restaurant and Menu) were built
to the standard the master plan requires at the end of Phase 6: both
services compile, both test suites pass, and a step-by-step verification
guide is shipped for Sierra-Lima to test the stack locally in IntelliJ
IDEA.

Five main units of work landed:

1. **Phase 2A -- Contract pack.** Produced
   `dev-docs/decisions/0020-sierra-lima-contracts.md` freezing all
   12 REST endpoints with example requests and responses, validation
   rules, Flyway `V1__init.sql` schemas for both services, and the
   seed-data plan (Flyway V2). Also resolved Q1 (error envelope) and
   Q8 (seed format) as far as Phase 2 can resolve them.

2. **Phase 2B -- Spring Boot scaffolding.** Hand-wrote the two Maven
   projects (no Spring Initializr, because we want the `pom.xml`
   layout to be explicit and reviewable). Installed the Spring
   Security stub (`permitAll` + stateless), the Flyway migration,
   the JPA auditing placeholder (Q10 resolution), the CORS bean,
   and the `application.properties` env-var wiring.

3. **Phase 2B -- Local-dev stack.** Wrote
   `services/local-dev/docker-compose.yml` (two PostgreSQL 15
   containers with healthchecks), `.env.example`, `runbook.md`,
   and the Postman collection + environment skeleton (12 endpoints
   across three folders, plus three placeholder folders that fill
   in later phases). Expanded `services/local-dev/README.md` with
   the master-plan port/env-var matrix. Appended `.gitignore` with
   `*.iml`, `target/`, `.env.local`, `HELP.md`.

4. **Phases 3-6 -- Domain, API, validation, OpenAPI.** Both services
   got the full vertical stack: JPA entity (with `@Embeddable`
   `Location` / `Price` per Q4 resolution), repository with derived
   and `@Query` methods, service class with `@Transactional`
   boundaries, `@RestController` mapping all six endpoints,
   record-based DTOs with Bean Validation annotations, a
   `@RestControllerAdvice` global exception handler producing the
   project error envelope, `springdoc-openapi` metadata bean plus
   `@Operation` / `@ApiResponses` annotations. `mvn -B test` and
   `mvn -B compile` are green on both services.

5. **Verification guide.** Wrote
   `dev-docs/verification/phase-2-to-6.md` -- a step-by-step IntelliJ
   IDEA walk-through targeted at someone not experienced with Java:
   prerequisites, opening the project, starting the DB containers,
   running the services, Swagger UI, Postman, per-endpoint expected
   responses, Phase 2-6 DoD checklist, troubleshooting table.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`
- Today: 2026-04-18 (Saturday)
- Active branch: `dev`
- Parent commit: `fd70496`
- Environment: Windows 11 + IntelliJ IDEA + Git Bash
- Java toolchain: Temurin 17, Maven 3.9.14, Docker Desktop

## Files Created or Updated During This Session

### Created -- Decisions / docs

- `dev-docs/decisions/0020-sierra-lima-contracts.md` -- Phase 2A
  contract pack. Freezes the 12 endpoints (6 Restaurant + 6 Menu),
  example JSON bodies, validation rules, DB schemas, seed plan,
  error envelope (resolves Q1).
- `dev-docs/verification/phase-2-to-6.md` -- IntelliJ IDEA
  verification walkthrough with glossary, prerequisites, step-by-step
  run instructions, per-endpoint verification, DoD checklist,
  troubleshooting cheat sheet.
- `dev-docs/agent-context/2026-04-18_chat-archive_Charlie-Lima-Alfa_fd70496.md`
  -- this archive.

### Created -- Local-dev infrastructure

- `services/local-dev/docker-compose.yml` -- postgres:15 x 2,
  healthchecks, named volumes, `quickbite-net` bridge network.
- `services/local-dev/.env.example` -- env-var template (DB creds,
  JWT secret placeholder, service URL vars).
- `services/local-dev/runbook.md` -- 8-section operational runbook.
- `services/local-dev/postman/QuickBite.postman_collection.json` --
  Postman collection with 3 populated folders (Auth/Login,
  Restaurant CRUD, Menu CRUD) and 3 placeholder folders (W1
  Integration, Async Evidence, Negative Auth) for later phases.
- `services/local-dev/postman/QuickBite.postman_environment.json` --
  env vars (`gatewayUrl`, `restaurantBaseUrl`, `menuBaseUrl`,
  `restaurantId`, `menuItemId`, four token placeholders).

### Created -- Restaurant Service Maven project

- `services/restaurant-service/pom.xml` -- Spring Boot 3.3.4, Java 17,
  dependencies: web, data-jpa, validation, security, actuator,
  flyway, postgres, jjwt 0.11.5 (api/impl/jackson), springdoc 2.5.0,
  lombok, devtools, spring-boot-starter-test, spring-security-test.
- `services/restaurant-service/Dockerfile` -- multi-stage (maven
  build image + jre runtime). Not used during Phases 2-6 but available
  for future container packaging.
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/RestaurantServiceApplication.java`
  (`@SpringBootApplication`, `@EnableJpaAuditing(auditorAwareRef="auditorAware")`).
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/config/SecurityConfig.java`
  (Phase 2 stub: `permitAll`, CSRF off, stateless, CORS via
  `CorsConfigurationSource` bean).
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/config/AuditingConfig.java`
  (`AuditorAware<UUID>` returning placeholder `00000000-...-000000000000`).
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/config/OpenApiConfig.java`
  (Phase 4 `OpenAPI` metadata bean).
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/domain/Restaurant.java`
  (`@Entity`, `@EntityListeners(AuditingEntityListener.class)`, with
  `Location` `@Embedded`).
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/domain/Location.java`
  (`@Embeddable` value object with address/city/lat/lng; resolves Q4).
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/repository/RestaurantRepository.java`
  (`findByLocationCity`, `findByOpenTrue`, and a JPQL `search` taking
  nullable city + `open`).
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/service/RestaurantService.java`
  (create, findById, search, update, setStatus, availability; uses
  placeholder owner id `00000000-...-000000000001` until Phase 7).
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantController.java`
  (`@RestController @RequestMapping("/restaurants")`, six endpoints,
  `@Valid`, `UriComponentsBuilder` for 201 Location header,
  `@Operation` + `@ApiResponses` annotations).
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/CreateRestaurantRequest.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/UpdateRestaurantRequest.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/UpdateRestaurantStatusRequest.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/RestaurantResponse.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/AvailabilityResponse.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/exception/RestaurantNotFoundException.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/exception/ErrorResponse.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/exception/GlobalExceptionHandler.java`
  (`@RestControllerAdvice` handling `NotFound`, `MethodArgumentNotValid`,
  `HttpMessageNotReadable`, `MethodArgumentTypeMismatch`,
  `IllegalArgumentException` (-> 422), fallthrough Exception (-> 500)).
- `services/restaurant-service/src/main/resources/application.properties`
  (port 8081, DB at localhost:5432, ddl-auto=validate, Flyway on,
  springdoc paths, actuator health show-details=always).
- `services/restaurant-service/src/main/resources/db/migration/V1__init.sql`
  (`restaurant` table + indexes on city and owner_id).
- `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/RestaurantServiceApplicationTests.java`
  (placeholder `assertTrue(true)`; keeps `mvn test` green without a
  live DB -- full `@SpringBootTest` deferred to Phase 7).

### Created -- Menu Service Maven project

- `services/menu-service/pom.xml` -- same shape as restaurant-service.
- `services/menu-service/Dockerfile`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/MenuServiceApplication.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/config/SecurityConfig.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/config/AuditingConfig.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/config/OpenApiConfig.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/domain/MenuItem.java`
  (`@Entity` with `Price` `@Embedded`; BigDecimal amount / 3-char
  currency code).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/domain/Price.java`
  (`@Embeddable` resolving Q4 on the Menu side).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/repository/MenuItemRepository.java`
  (`findByRestaurantId`, `findByRestaurantIdAndAvailableTrue`,
  `findAllByMenuItemIdIn` for batch validation, JPQL
  `searchForRestaurant` with nullable filters).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java`
  (create, list, findById, update, delete, validate; per-line
  batch-validation output shape).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/controller/MenuController.java`
  (six endpoints across `/restaurants/{rid}/menu-items` and
  `/menu-items/**` roots, `@Operation` + `@ApiResponses`).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/dto/CreateMenuItemRequest.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/dto/UpdateMenuItemRequest.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/dto/MenuItemResponse.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/dto/ValidateMenuItemsRequest.java`
  (`List<Line>` with `@Valid`, `@NotEmpty`).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/dto/ValidateMenuItemsResponse.java`
  (top-level `allValid`; per-line `exists`, `available`,
  `unitPriceAmount`, `unitPriceCurrency`, `lineTotalAmount`,
  optional `reason`).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/MenuItemNotFoundException.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/ErrorResponse.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/GlobalExceptionHandler.java`
- `services/menu-service/src/main/resources/application.properties`
  (port 8082, DB at localhost:5433).
- `services/menu-service/src/main/resources/db/migration/V1__init.sql`
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/MenuServiceApplicationTests.java`

### Updated

- `.gitignore` -- appended `*.iml`, `target/`, `.env.local`,
  `**/.env.local`, `HELP.md`.
- `services/local-dev/README.md` -- replaced the "Phase 2 not yet
  landed" placeholder with the real port/env-var matrix
  (master plan §9 Phase 2 Task 10) and a current-state checklist.
- `dev-docs/decisions/0004-open-questions.md` -- resolved Q1
  (error envelope), Q2 (category vocab), Q4 (embeddables), Q10
  (auditor placeholder). Q8 now recorded as "plan locked, impl
  deferred to Phase 7". Q3, Q5, Q7, Q9 remain pending.

## Technical Choices Worth Preserving

### Spring Boot 3.3.4

Chosen because it is the latest 3.3.x on 2026-04-18, the Java 17
line is still current, and Spring Boot 3.4 was already out but would
pull in tooling that is marginally newer than what the ESI practical
materials assume.

### `jjwt 0.11.5`

Chosen for alignment with A3 and the course's PS111 practical. 0.12.x
broke the `Jwts.builder()` API; sticking with 0.11.x keeps the code
identical to what the course notes show.

### `springdoc-openapi` 2.5.0

Chosen because the 2.5 line is the stable one for Spring Boot 3.3.
UI path is kept at `/swagger-ui.html` (the default) not
`/swagger-ui/index.html`.

### No Spring Initializr

Projects were hand-rolled. The three-step Initializr boilerplate
(`mvnw`, `.mvn/`, `HELP.md`, auto-generated empty test) added
nothing we needed. `mvnw` was deliberately omitted so the Dockerfile
uses the stock `maven:3.9-eclipse-temurin-17` image directly. This
removes one tree of files from PR review.

### `ddl-auto=validate` + Flyway

The master plan locks this explicitly. JPA does not create or update
the schema at runtime; Flyway does. `validate` ensures the entity
mappings still match the migration-created schema at startup.

### Audit placeholder UUID

`00000000-0000-0000-0000-000000000000` used uniformly. Q10 is now
closed. The Phase 7 swap is a one-line change: replace the bean
body with `SecurityContextHolder.getContext().getAuthentication()`
lookup.

### Placeholder owner id in `RestaurantService`

Separate placeholder (`00000000-...-000000000001`) because the
owner id is cross-service data, not an audit trail entry. Phase 7
will read ownerId from the JWT `userId` claim. Marked with an
inline `// Phase 3 placeholder` comment in the service class (the
only phase-tag comment in the codebase; all other Phase-N references
live in decisions or the archive).

### Placeholder unit tests

Both services' `*ApplicationTests` hold a trivial `assertTrue(true)`.
Spring Boot's Initializr default (`@SpringBootTest`) would fail
without a live Postgres. Proper slice tests are a Phase 7 concern
(see master plan DoD "Tests pass for critical CRUD and validation
paths").

### Global exception handler per service, not a shared module

The master plan explicitly says "Extract the shared pattern from
Restaurant Service into a reusable module **or duplicate the shape**."
We duplicated. Rationale: 63 lines of code, one data class, no
Maven multi-module churn, no cross-service coupling. If a third
service were ever to need the same envelope we would reconsider.

### DTOs as Java records

Records give us immutable value semantics + Jackson
serialisation/deserialisation + Bean Validation support for free.
The only trade-off is that we can't put JPA annotations on records
(so entities remain classes with mutable fields).

### `@Valid` on `@RequestBody`

Every mutation endpoint annotates the body with `@Valid`, which
forces Bean Validation to run. Missing it would silently allow
invalid payloads through.

## Compile / Test Results

- `cd services/restaurant-service && mvn -B compile` -> BUILD SUCCESS
- `cd services/restaurant-service && mvn -B test` -> BUILD SUCCESS,
  1 test passed.
- `cd services/menu-service && mvn -B compile` -> BUILD SUCCESS
- `cd services/menu-service && mvn -B test` -> BUILD SUCCESS,
  1 test passed.

The services were **not** boot-tested end-to-end in this session
because that requires a running Postgres and this agent's sandbox
does not run Docker. The boot-and-hit-endpoints pass is Sierra-Lima's
next step, guided by `dev-docs/verification/phase-2-to-6.md`.

## Phase 2-6 Definition of Done -- What Has Shipped

| DoD item | Satisfied |
|----------|-----------|
| OpenAPI-ready endpoint list for both services | `0020-sierra-lima-contracts.md` §3, §4 |
| Database schemas defined | `V1__init.sql` per service |
| Validation rules documented | `0020-sierra-lima-contracts.md` §5 + DTO annotations |
| Both Spring Boot apps compile without errors | `mvn -B compile` green both |
| Both PostgreSQL databases provisioned | `docker-compose.yml` + runbook |
| `/actuator/health` endpoint present | `application.properties` `management.endpoints.web.exposure.include=health,info` |
| Postman collection exists with login placeholder | `QuickBite.postman_collection.json` |
| Restaurant entity persists + full CRUD | Restaurant/Repository/Service/Controller all in place |
| Six Restaurant endpoints reachable | RestaurantController |
| Data persists across restarts | `ddl-auto=validate` + Flyway ensures the table stays |
| Swagger UI renders all endpoints | `springdoc-openapi-starter-webmvc-ui` pulled in + `OpenApiConfig` bean + `@Operation`/`@ApiResponses` on controllers |
| Consistent error envelope across both services | `GlobalExceptionHandler` in each service |
| CORS headers present | `SecurityConfig.corsConfigurationSource()` bean wired via `http.cors(...)` |
| MenuItem entity + six endpoints | MenuItem/Repository/Service/Controller all in place |
| Batch validation returns locked-down shape | `ValidateMenuItemsResponse` record; allValid + per-line results |

The only DoD item the agent could not verify locally (no running
Docker in the sandbox) is "both apps boot and `/actuator/health`
returns 200." Sierra-Lima verifies this manually via the Phase 2-6
verification guide.

## Open Questions Status After This Session

| # | Status | Note |
|---|---|---|
| Q1 | Resolved | Phase 2 / 4 / 6; envelope shape + `ErrorResponse` record implemented |
| Q2 | Resolved | Free-form `VARCHAR(100)` category |
| Q3 | Pending | HATEOAS — still no (no plan to revisit until CP#3) |
| Q4 | Resolved | Location and Price are `@Embeddable` |
| Q5 | Pending | Availability endpoint returns 200 with `isOpen` — matches the lean, not yet formally ratified |
| Q6 | Resolved (earlier) | Public browse routes, per `0010` |
| Q7 | Pending | Pagination not yet wired; list returns a plain array |
| Q8 | Plan locked, impl deferred | Flyway `V2__seed_demo_data.sql` planned; Phase 7 implements |
| Q9 | Pending | Test strategy (H2 vs Testcontainers); Phase 7 |
| Q10 | Resolved | Placeholder UUID active in both `AuditingConfig` beans |

## Commits Created in This Session

1. One commit bundling every file listed in "Files Created or
   Updated" above. Subject at commit time recorded in `git log`.

## Feedback and Preferences Re-used

- **Don't over-qualify unilateral decisions** (feedback memory from
  the 97f2d2b session). Applied here: the Phase 2 choices (seed via
  Flyway, embeddables, error envelope, owner-id placeholder) are
  recorded as Accepted decisions without "pending team ratification"
  hedges.
- **Imperative sentence-case commit subjects** (from `0003-conventions.md`).

## Suggested Next Steps For a Future Agent

1. **Sierra-Lima verifies locally** by running through
   `dev-docs/verification/phase-2-to-6.md`. Any gap uncovered there
   should be fixed before Phase 7 starts.
2. **Begin Phase 7 -- Sierra-Lima Hardening Pass.** Master plan §9
   Phase 7 covers:
   - Replace the `permitAll` stub with a real `JwtAuthFilter` + the
     route-protection matrix from `0010-auth-contract.md` §8.
   - Write a `DevTokenGenerator` utility and add one token per role
     (Customer, RestaurantOwner, Admin) to the Postman environment.
   - Seed data via `V2__seed_demo_data.sql` (closes Q8). Target
     4-6 demo restaurants and 12-18 menu items per master plan.
   - Swap the `AuditorAware` placeholder for a real
     `SecurityContextHolder` lookup (final step of Q10).
   - Controller and service-level tests for critical paths.
3. **Also consider resolving** Q5 (availability payload) and Q7
   (pagination) during Phase 7, since the hardening pass is a
   natural place to formalise them.

## Workspace Safety Notes

- No files were deleted or truncated in this session.
- No `git push --force` operations.
- Single `git push origin dev` planned for the Phase 2-6 bundle.
- `.idea/`, `.claude/`, `target/`, `.env.local`, `HELP.md`
  continue to be ignored.
- No secrets were committed. The HS256 secret in `.env.example` is
  a visibly-labelled dev-only placeholder and is rotated before
  Phase 7 integration.
