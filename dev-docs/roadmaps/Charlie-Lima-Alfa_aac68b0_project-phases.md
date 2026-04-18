# QuickBite ESI Project -- Phased Implementation Plan

**Pseudonym:** Charlie-Lima-Alfa  
**Base commit:** `aac68b0`  
**Supersedes:** `Charlie-Lima-Alfa_2ce188a_project-phases.md`  
**Student:** Sierra-Lima  
**Services owned:** Menu Service, Restaurant Service  
**Team:** Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa (Group 7)  
**Date created:** 2026-04-17

---

## 0. How to Use This Document

This roadmap is designed as a **standalone** reference. If the prior-submission
artifacts (`Assignment-1`, `Assignment-2`, `Assignment-3`) were unavailable, the
information needed to reach final-checkpoint readiness -- scope, contracts, data
models, workflows, events, and phase-by-phase execution -- would still be in
this file.

The canonical design artifacts it is grounded in (all stored in this repository
at commit `aac68b0`) are:

- `dev-docs/prior-submissions/Assignment-1-Submission.pdf` -- original system definition
- `dev-docs/prior-submissions/Assignment-1_Feedback.txt` -- penalties for infra in diagram and shared DB
- `dev-docs/prior-submissions/Assignment-2-Submission.pdf` -- DDD domain model
- `dev-docs/prior-submissions/Assignment-3-Submission.pdf` (and `.docx`) -- final design baseline
- `dev-docs/prior-submissions/assignment-3_figure1_business-architecture.png` -- business architecture
- `dev-docs/prior-submissions/assignment-3_figure1b_implementation-architecture.png` -- technical runtime view
- `dev-docs/prior-submissions/assignment-3_figure2_service-er-diagrams.png` -- ER diagrams across services
- `dev-docs/prior-submissions/assignment-3_figure3_workflow-w1-sequence.png` -- W1 sequence
- `dev-docs/prior-submissions/assignment-3_figure4_workflow-w2-w3-events.png` -- W2/W3 event flow

The critical contract-level content from those artifacts is inlined in
*Appendix E -- Canonical A3 Reference*, so day-to-day coding does not require
round-trips back to the PDFs.

---

## 1. What Changed Since `2ce188a`

The `2ce188a` version of this roadmap was written before the current
`Assignment-3` submission was readable. The new prior-submission materials
change the plan in five material ways:

1. **`User Service` is now implemented, not design-only.**  
   The implementation subset is now seven business services plus two shared
   integration components. Only `Review Service` remains design-only.

2. **Bearer-token authentication is part of the baseline, not a final-phase add-on.**  
   `User Service` issues tokens. Every implemented service validates tokens
   locally on entry. Public routes are only `POST /users` and
   `POST /auth/login`; everything else is protected unless the team explicitly
   decides otherwise.

3. **Static service configuration is the default.**  
   No Eureka/service-discovery server in the baseline runtime. Client-side load
   balancing is only added back if the team consciously expands scope.

4. **Shared-component ownership is now fixed.**  
   `API Gateway` is implemented by Alfa-Kilo. `Event Broker configuration`
   (Kafka) is implemented by Mike-Alfa. Sierra-Lima owns two business services
   (`Restaurant Service`, `Menu Service`) and does **not** replace either with
   a shared integration component.

5. **Workflows, events, and endpoint contracts are now explicit.**  
   Endpoint paths, event topics, event names, and the event-envelope shape are
   pinned down. The new phases are contract-aware rather than broad-guidance.

The previous roadmap's overall strategy (front-load Sierra-Lima services, build
`W1` before Checkpoint #1, etc.) still holds. The ordering below preserves that
and layers the changes in early.

---

## 2. Overview

This document maps the journey from the current early-scaffold repository to a
presentation-ready system across a series of focused 3-hour working sessions
("Phases"). Each Phase has a clear goal, concrete deliverables, and a
definition-of-done checklist.

### 2.1 Key Deadlines

| Date | Milestone | Focus |
|------|-----------|-------|
| 2026-04-21 | Project description released | Official project spec from instructor |
| 2026-04-28 | Project consultation session | Clarify scope and expectations with instructor |
| **2026-05-05** | **Checkpoint #1** | **Backend implementation** |
| **2026-05-12** | **Checkpoint #2** | **Frontend + Backend integration** |
| **2026-05-19** | **Checkpoint #3** | **Final presentations (Frontend + Backend + Security)** |
| 2026-05-25 | Exam 1 | Written exam |
| 2026-06-08 | Exam 2 | Retake window |
| 2026-06-22 | Resit | Final resit |

Note: the practicals page shows `28/04/2025` for the project consultation. In
the 2026 schedule context that is almost certainly a typo; this plan treats the
consultation as `2026-04-28`.

### 2.2 Grading Context (informative)

- Assignments: 20 points (A1: 4, A2: 8, A3: 8)
- Project: 30 points (across three checkpoints)
- Exam: 50 points (minimum 21 to pass)
- Total: 100 points (minimum 51 to pass)
- Assignment 1 score: 3.50/4.00 (see *Appendix D* for feedback to address)

---

## 3. Baseline Project Scope

### 3.1 Business System

QuickBite is a food-delivery platform with eight business services in the
overall business architecture:

1. `User Service`
2. `Order Service`
3. `Menu Service`
4. `Restaurant Service`
5. `Delivery Service`
6. `Payment Service`
7. `Notification Service`
8. `Review Service`

### 3.2 Implementation Subset (updated)

| Category | Included |
|----------|----------|
| Implemented business services | `Order`, `User`, `Restaurant`, `Menu`, `Payment`, `Delivery`, `Notification` |
| Implemented shared components | `API Gateway`, `Event Broker configuration` (Kafka) |
| Design-only (not coded) | `Review Service` |

### 3.3 Team Ownership

| Team member | Owned components |
|-------------|------------------|
| Alfa-Kilo | `Order Service`, `User Service`, `API Gateway` |
| Sierra-Lima | `Restaurant Service`, `Menu Service` |
| Elephant-Yankee | `Payment Service`, `Delivery Service` |
| Mike-Alfa | `Notification Service`, `Event Broker configuration` |

Sierra-Lima owns two business services and does **not** replace either with a
shared integration/resilience component. This is a constraint from the updated
Assignment 3 implementation-responsibility table.

### 3.4 Sierra-Lima Domain Recap

**Restaurant Service** (requirements R19 register/manage restaurant, R20 update status/hours)

- Aggregate root: `Restaurant` -- `restaurantId`, `ownerId*`, `name`, `address`, `city`, `latitude`, `longitude`, `operatingHours`, `isOpen`
- `ownerId*` references `User.userId` (cross-service ID only, no FK at DB level)
- Consider embedding a `Location` value object (`@Embeddable`: address, city, latitude, longitude)
- One database: `restaurant_db`

**Menu Service** (requirements R21 add/update/remove items, R22 browse menu)

- Aggregate root: `MenuItem` -- `menuItemId`, `restaurantId*`, `name`, `description`, `priceAmount`, `priceCurrency`, `category`, `isAvailable`
- `restaurantId*` references `Restaurant.restaurantId` (cross-service ID only)
- Consider embedding a `Price` value object (`@Embeddable`: amount, currency)
- One database: `menu_db`

### 3.5 Design Decisions to Honour

- **Service-local databases.** No shared DB across microservices. This was
  already penalised in Assignment 1 feedback; it is a hard rule.
- **Cross-service references use ID fields only.** No cross-service joins; no
  database-level FKs to another service's tables.
- **Auth is baseline.** Public routes are only `POST /users` and
  `POST /auth/login`. Everything else requires a valid bearer token,
  validated locally by each implemented service.
- **Static service configuration.** Downstream URLs are provided via env vars
  or config. No Eureka-based discovery in the baseline.
- **Both interaction styles must ship.** Synchronous REST is demonstrated by
  W1; asynchronous messaging by W2 and W3.
- **One Sierra-Lima business service can be cut if time collapses**, but both
  should ideally ship because both participate in W1.

### 3.6 Technology Stack (from course practicals)

| Layer | Technology | Notes |
|-------|-----------|-------|
| Backend framework | Spring Boot (Java) | Maven; aligns with PS0x1/PS0x2 practicals |
| Java version | **17 (recommended)** or 21 | Course practicals (PS0xx) use Java 17; choose 17 by default, raise to 21 only if the full team agrees |
| Database | PostgreSQL | One DB per service (separate containers) |
| Containerisation | Docker + Docker Compose | Local reproducibility mandatory |
| Service discovery | None in baseline | Eureka only if team consciously adds scope |
| API gateway | Spring Cloud Gateway | Owned by Alfa-Kilo; Sierra-Lima integrates through it |
| Async messaging | Apache Kafka | Owned by Mike-Alfa; Sierra-Lima is neither producer nor consumer per A3 scope (see §9) |
| Resilience | Resilience4j | Optional; add once core W1 is stable |
| Auth | Spring Security + JWT (jjwt 0.11.5) | Token issued by User Service; validated locally in each service |
| Frontend | Vue.js 3 (Vue CLI, Vue Router, Fetch API) | |
| API documentation | OpenAPI / Swagger (springdoc-openapi) | Keep live from early phases |
| API testing | Postman | One shared collection with a login-first flow |

---

## 4. Named Workflows

Three workflow labels are used throughout the plan. Together they satisfy the
Assignment 3 requirement for both synchronous and asynchronous integration
styles. W1 and W2 are the primary targets; W3 is the asynchronous complement
that rides on the same Kafka infrastructure as W2.

| Label | Name | Style | Summary |
|-------|------|-------|---------|
| **W1** | Place Order | Synchronous REST | Client -> Gateway -> Order -> {User lookup, Restaurant availability, Menu validation, Payment charge, Delivery task}. All protected by bearer tokens. |
| **W2** | Delivery Progress & Notifications | Asynchronous (Kafka) | Delivery publishes `delivery.status-changed` on `delivery-events`. Order and Notification consume. |
| **W3** | Payment Outcome Notification | Asynchronous (Kafka) | Payment publishes `payment.completed` / `payment.failed` on `payment-events`. Notification (and Order, for failures) consume. |

The detailed contract is pinned in *Appendix E*.

---

## 5. Planning Principles

1. **Align with Assignments 1-3** rather than reinventing the system.
2. **Separate business architecture from technical architecture** in every
   diagram and report section.
3. **Each implemented microservice owns its own database.** Reject any
   "shared DB" shortcut that appears during integration, even if convenient.
4. **Prioritise both required interaction styles:** synchronous REST (W1) and
   asynchronous Kafka (W2 + W3).
5. **Bake in auth from the first endpoint.** Do not build "open CRUD now, add
   security later"; do build "login flow stubbed, bearer validated, roles
   relaxed until Phase 16 if necessary".
6. **Keep each service small and demoable.** ~5-8 endpoints per service, per
   the Assignment 3 guideline.
7. **Avoid infrastructure inflation.** Eureka, client-side load balancing, and
   other extras remain optional unless the instructor explicitly requests them.
8. **Front-load solo-capable work** so early phases produce artifacts useful
   even if teammates join late: contracts, data models, Docker Compose, seed
   data, service skeletons, OpenAPI docs.
9. **Treat this repository as a personal early-start workspace.** Favor work
   that maximises reuse by the shared team repo; avoid absorbing
   teammate-owned service logic unless the team explicitly decides so.

---

## 6. Delivery Strategy

Because the repository is currently scaffolded but empty, and the team may
mobilise late, the safest strategy is:

1. **Re-baseline** scope, auth, and gateway assumptions
2. **Build** Sierra-Lima's two services as protected, integration-ready services
3. **Assemble** backend workflow W1 before Checkpoint #1
4. **Layer** frontend before Checkpoint #2
5. **Finish** authorisation, async polish, report, and presentation readiness before Checkpoint #3

### 6.1 Cross-Phase Working Assets

Keep these live throughout the project rather than creating them at the end:

- One shared Postman collection that starts with login and then exercises protected requests
- Swagger/OpenAPI exposure for every implemented backend service
- A seeded demo dataset for restaurants, menu items, and W1 participants
- One documented Docker Compose runbook for clean startup
- Health-check or ping endpoints for quick verification
- Screenshot or recording backups for each checkpoint demo
- Example payloads for Order -> Restaurant and Order -> Menu integration calls

---

## 7. Phase Map at a Glance

| Phase | Title | Checkpoint Target | Est. Sessions |
|-------|-------|-------------------|---------------|
| 0 | Scope Freeze & Repo Conventions | -- | 1 |
| 1 | Auth & Gateway Contract Alignment | -- | 1 |
| 2 | Contract Pack & Local-Dev Bootstrap | -- | 1 |
| 3 | Restaurant Service -- Foundation | CP#1 | 1 |
| 4 | Restaurant Service -- Full API, Validation, OpenAPI | CP#1 | 1 |
| 5 | Menu Service -- Foundation | CP#1 | 1 |
| 6 | Menu Service -- Full API, Validation, OpenAPI | CP#1 | 1 |
| 7 | Sierra-Lima Hardening Pass (auth, errors, seed, tests) | CP#1 | 1 |
| 8 | Dockerise Both Services | CP#1 | 1 |
| 9 | Team Contract Lock for W1 / W2 / W3 | CP#1 | 1 |
| 10 | W1 Integration & Resilience Protection | CP#1 | 1 |
| 11 | Backend Polish & Checkpoint #1 Prep | CP#1 | 1 |
| 12 | Vue.js Frontend -- Shell, Routing & Sign-In | CP#2 | 1 |
| 13 | Vue.js Frontend -- Restaurant & Menu UX | CP#2 | 1 |
| 14 | Frontend-Backend Integration & Checkpoint #2 Prep | CP#2 | 1 |
| 15 | Authorisation Hardening & Role-Aware Behaviour | CP#3 | 1 |
| 16 | Async Evidence & Cross-Service Smoke | CP#3 | 1 |
| 17 | Report & Evidence Pack | CP#3 | 1 |
| 18 | Final Presentation Rehearsal | CP#3 | 1 |
| 19 | Buffer & Final Freeze | CP#3 | 1 |

**Total: ~20 sessions x 3 hours = ~60 hours of focused work**

If time collapses, see *Appendix A -- Compression Guidance*.

---

## 8. Detailed Phase Plan

### Phase 0 -- Scope Freeze & Repo Conventions

**Goal.** Turn the updated Assignment 3 outputs into a single implementation
baseline so nobody on the team is still debating what's in or out.

#### Tasks

1. **Reconfirm project scope from the updated Assignment 3.**
   - Implemented: `Order`, `User`, `Restaurant`, `Menu`, `Payment`, `Delivery`, `Notification`.
   - Shared: `API Gateway` (Alfa-Kilo), `Event Broker configuration` (Mike-Alfa).
   - Design-only: `Review Service`.
2. **Freeze the canonical workflows** W1 / W2 / W3 (see §4 and Appendix E).
3. **Decide folder layout** under `services/`.
4. **Define conventions.** Git branch strategy (`dev` for daily, feature
   branches, `main` for releases), commit message format, Java version,
   package naming (`ee.ut.esi.quickbite.<service>`), env-var naming,
   Docker-image naming.
5. **Record open design questions** under `dev-docs/decisions/`.
6. **Produce a non-goals list** for the first implementation pass
   (e.g. "no frontend until after CP#1", "no real payment gateway",
   "no mobile app", "no Eureka").

#### Definition of Done

- [ ] Implementation subset confirmed in writing
- [ ] Folder structure agreed
- [ ] Conventions documented
- [ ] Non-goals list exists
- [ ] Nobody on the team should still be debating which services are in or out

---

### Phase 1 -- Auth & Gateway Contract Alignment

**Goal.** Remove the largest new risk introduced by the updated Assignment 3:
unclear authentication and route-protection behaviour. Sierra-Lima should be
able to implement Restaurant/Menu endpoints without guessing how auth arrives
or which requests must be rejected.

#### Tasks

1. **Agree the public route list.**
   - `POST /users` -- self-registration (User Service)
   - `POST /auth/login` -- token issuance (User Service)
2. **Agree the default protected-route rule.** Every other implemented
   endpoint requires a valid bearer token and at least the `Customer` role
   unless a stricter role is specified.
3. **Agree gateway path prefixes.** (See Appendix E Table E.5.)
4. **Agree the token-propagation model:**
   - Client -> Gateway carries `Authorization: Bearer <token>`.
   - Gateway validates token, routes request, forwards the token header
     downstream (and any derived context such as `X-User-Id`).
   - Each downstream service validates the token locally on entry
     (no shared session).
   - Service-to-service REST calls also carry a bearer token (either the
     original caller's, or a service token).
5. **Agree what identity context Sierra-Lima services need.**
   - Current authenticated user id
   - Role (`Customer` | `Driver` | `RestaurantOwner` | `Admin`)
   - Service identity for internal calls (if distinct)
6. **Decide explicitly whether `GET /restaurants` and `GET /menu-items` browse
   endpoints remain protected** or whether the team consciously deviates. Log
   the decision.
7. **Capture the JWT claims shape** used by User Service so Sierra-Lima can
   mock it locally before the real service is ready.

#### Outputs

- Auth contract sheet (written)
- Route-protection matrix (written)
- Gateway path map (written)
- Example JWT claims payload for local development

#### Definition of Done

- [ ] Every Sierra-Lima endpoint has a documented required auth posture
- [ ] Sierra-Lima can implement services against a documented JWT shape without blocking on Alfa-Kilo

---

### Phase 2 -- Contract Pack & Local-Dev Bootstrap

**Goal.** Make Restaurant Service and Menu Service precise enough that other
teammates can integrate against them later, and have a working local dev
environment.

#### Contract Pack

1. **Freeze final REST endpoints** for both services (see Appendix E Tables E.1 and E.2).
2. **Freeze request and response payloads** (JSON schemas).
3. **Freeze validation rules:**
   - Restaurant: required fields, `operatingHours` format, latitude/longitude ranges.
   - Menu: `priceAmount` as `BigDecimal` with scale 2, `priceCurrency` ISO-4217, availability rules.
4. **Freeze database schemas** (see Appendix E Tables E.3 and E.4).
5. **Create seed-data plan** for demo restaurants and menu items.
6. **Write cross-service assumptions explicitly:**
   - `MenuItem.restaurantId` references `Restaurant.restaurantId`
   - `Restaurant.ownerId` references `User.userId`
   - No cross-service joins, only ID references

#### Environment Bootstrap

7. **Install / verify prerequisites.** Java 17+ (or 21) JDK; Maven; Docker
   Desktop (Windows WSL2 backend); Docker Compose v2; Node.js 18+ and NPM
   (for later); Vue CLI (`npm install -g @vue/cli`); Postman; IDE with Spring
   Boot support.

8. **Initialise Spring Boot projects** via [Spring Initializr](https://start.spring.io/):
   - `restaurant-service`: Spring Web, Spring Data JPA, PostgreSQL Driver, Validation, Lombok, DevTools, Spring Security (for token validation)
   - `menu-service`: Spring Web, Spring Data JPA, PostgreSQL Driver, Validation, Lombok, DevTools, Spring Security
   - Place generated sources under `services/restaurant-service/` and `services/menu-service/`
   - After generation, manually add `jjwt` to each service's `pom.xml` (Spring Initializr does not include it):
     ```xml
     <dependency>
       <groupId>io.jsonwebtoken</groupId>
       <artifactId>jjwt-api</artifactId>
       <version>0.11.5</version>
     </dependency>
     <dependency>
       <groupId>io.jsonwebtoken</groupId>
       <artifactId>jjwt-impl</artifactId>
       <version>0.11.5</version>
       <scope>runtime</scope>
     </dependency>
     <dependency>
       <groupId>io.jsonwebtoken</groupId>
       <artifactId>jjwt-jackson</artifactId>
       <version>0.11.5</version>
       <scope>runtime</scope>
     </dependency>
     ```

   > **Spring Security bootstrap caveat (important):** Adding `spring-boot-starter-security`
   > locks every endpoint behind a generated form-login page by default. The Phase 2 DoD
   > requires `/actuator/health` to return 200 — it will return 401/302 unless a permissive
   > `SecurityConfig` is added immediately. Create the following stub in each service and
   > leave it in place until Phase 7 replaces it with the real JWT filter:
   > ```java
   > @Configuration
   > @EnableWebSecurity
   > public class SecurityConfig {
   >     @Bean
   >     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
   >         http.csrf(AbstractHttpConfigurer::disable)
   >             .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
   >             .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
   >         return http.build();
   >     }
   > }
   > ```
   > Phase 7 will replace `anyRequest().permitAll()` with the real route-protection matrix.

9. **Local PostgreSQL databases via Docker Compose.**  
   `services/local-dev/docker-compose.yml` with two PostgreSQL containers:
   - `restaurant-db` (container port 5432, host port 5432)
   - `menu-db` (container port 5432, host port 5433)  
   Each with its own named volume.

10. **Service ports and env-var matrix.**

    | Service | Port | DB Port | Notes |
    |---------|------|---------|-------|
    | API Gateway | 8080 | -- | Alfa-Kilo |
    | User Service | 8085 | 5435 | Alfa-Kilo; issues tokens |
    | Order Service | 8086 | 5436 | Alfa-Kilo |
    | Restaurant Service | 8081 | 5432 | Sierra-Lima |
    | Menu Service | 8082 | 5433 | Sierra-Lima |
    | Payment Service | 8083 | 5437 | Elephant-Yankee |
    | Delivery Service | 8084 | 5438 | Elephant-Yankee |
    | Notification Service | 8087 | 5439 | Mike-Alfa |
    | Kafka broker | 9092 | -- | Mike-Alfa |

    Env vars every Sierra-Lima service needs: `DB_URL`, `DB_USER`, `DB_PASSWORD`,
    `JWT_ISSUER`, `JWT_PUBLIC_KEY` (or HS256 shared secret for development),
    `RESTAURANT_SERVICE_URL` / `MENU_SERVICE_URL` (only if a service calls another).

11. **Verify both services start** (`mvn spring-boot:run`) with a `/actuator/health` or `/ping` endpoint.

12. **Set up Postman workspace** with one QuickBite collection rooted at login,
    then Restaurant CRUD and Menu CRUD folders, plus a shared environment file.

#### Definition of Done

- [ ] OpenAPI-ready endpoint list exists for both services
- [ ] Database schemas defined
- [ ] Validation rules documented
- [ ] Both Spring Boot apps start without errors
- [ ] Both PostgreSQL databases running via Docker Compose
- [ ] `/ping` or `/actuator/health` returns 200 on both services
- [ ] Postman collection exists with a login placeholder
- [ ] Sierra-Lima can begin implementation without waiting for Order, Payment, or Delivery teams

---

### Phase 3 -- Restaurant Service: Foundation

**Goal.** The `Restaurant` entity is persisted in its own PostgreSQL database.
Basic lifecycle (create, retrieve, list, update, status toggle) works end-to-end
via Postman.

#### Tasks

1. **Domain model.**
   - `Restaurant` JPA `@Entity`:
     - `restaurantId` (UUID, `@Id`, `@GeneratedValue`)
     - `ownerId` (UUID, cross-service reference to `User`)
     - `name` (String, not blank)
     - `address`, `city` (String)
     - `latitude`, `longitude` (Double; validate ranges)
     - `operatingHours` (String, e.g. "09:00-22:00")
     - `isOpen` (Boolean, default `false`)
     - `createdAt`, `updatedAt` (LocalDateTime, JPA auditing)
   - Consider embedding `Location` as a `@Embeddable` value object.

2. **Repository.** `RestaurantRepository extends JpaRepository<Restaurant, UUID>`,
   with derived queries `findByCity(String city)` and `findByIsOpenTrue()`.

3. **Service layer.** `RestaurantService` with methods matching Appendix E Table E.1.

4. **Controller.** `RestaurantController` with `@RestController` and
   `@RequestMapping("/restaurants")` (gateway rewrites `/api/restaurants/**`).

5. **DTOs.** `CreateRestaurantRequest`, `UpdateRestaurantRequest`,
   `RestaurantResponse`, `AvailabilityResponse`.

6. **Test with Postman.** POST, GET (all), GET (by id), PUT, PATCH status,
   GET availability.

#### Definition of Done

- [ ] Restaurant table auto-created in `restaurant_db` on startup
- [ ] All six endpoints from Appendix E Table E.1 reachable and returning correct data via Postman (basic working — full validation and OpenAPI added in Phase 4)
- [ ] Data persists across service restarts
- [ ] Postman collection updated

> **Note:** Restaurant Service has no `DELETE /restaurants/{id}` endpoint — this is intentional.
> Restaurants are toggled open/closed via `PATCH /{id}/status`; hard deletion would orphan
> historical order data. If the team later needs soft-delete, add a `status: INACTIVE` field.

---

### Phase 4 -- Restaurant Service: Full API, Validation, OpenAPI

**Goal.** Complete the Restaurant Service API with validation, OpenAPI docs,
consistent error responses, and CORS ready for the frontend.

#### Tasks

1. **Complete endpoint set per Appendix E Table E.1.**
   - `POST /restaurants`
   - `GET /restaurants/{id}`
   - `PUT /restaurants/{id}`
   - `PATCH /restaurants/{id}/status`
   - `GET /restaurants` (filters: `city`, `isOpen`)
   - `GET /restaurants/{id}/availability` (lightweight open/closed check for Order Service)

2. **Validation.** Bean Validation annotations (`@NotBlank`, `@NotNull`,
   `@DecimalMin`/`@DecimalMax` for lat/lng, `@Pattern` for operating hours).
   `@ControllerAdvice` global exception handler producing a consistent error
   JSON envelope.

3. **HTTP status codes.** 201 Created, 204 No Content, 400 Bad Request,
   404 Not Found, 409 Conflict, 422 Unprocessable Entity.

4. **OpenAPI / Swagger.** Add `springdoc-openapi-starter-webmvc-ui`. Annotate
   with `@Operation`, `@ApiResponses`. Verify UI at
   `http://localhost:8081/swagger-ui.html`.

5. **Auditing.** `@EnableJpaAuditing`, `@CreatedDate`, `@LastModifiedDate`.

6. **CORS configuration.** Use a global `CorsConfigurationSource` bean wired through
   `SecurityFilterChain` (via `http.cors(cors -> cors.configurationSource(...))`).
   `@CrossOrigin` on controllers is overridden by Spring Security's filter chain before
   Spring MVC processes the request, so the global bean approach is required.

#### Definition of Done

- [ ] All six endpoints fully functional, with clear validation errors
- [ ] Swagger UI renders every endpoint and schema
- [ ] Consistent error envelope across the service
- [ ] CORS headers present
- [ ] Postman collection complete

---

### Phase 5 -- Menu Service: Foundation

**Goal.** The `MenuItem` entity is persisted in its own PostgreSQL database.
Order Service will be able to ask for item validity and pricing without
direct DB access.

#### Tasks

1. **Domain model.**
   - `MenuItem` JPA `@Entity`:
     - `menuItemId` (UUID, `@Id`, `@GeneratedValue`)
     - `restaurantId` (UUID, cross-service reference to `Restaurant`)
     - `name`, `description` (String)
     - `priceAmount` (BigDecimal, scale 2)
     - `priceCurrency` (String, default `"EUR"`)
     - `category` (String, e.g. "Appetizer", "Main", "Dessert", "Drink")
     - `isAvailable` (Boolean, default `true`)
     - `createdAt`, `updatedAt`
   - Consider embedding `Price` as a `@Embeddable` value object.

2. **Repository.** `MenuItemRepository extends JpaRepository<MenuItem, UUID>`
   with derived queries `findByRestaurantId(UUID)`,
   `findByRestaurantIdAndIsAvailableTrue(UUID)`, `findAllByMenuItemIdIn(Set)`.

3. **Service layer.** `MenuService` with methods matching Appendix E Table E.2,
   including a **batch validation** method used by Order Service during order
   placement. Given a list of `{menuItemId, quantity}`, it returns per-item
   existence, availability, and unit price.

4. **Controller.** `MenuController`, a mixture of paths rooted under
   `/restaurants/{rid}/menu-items` and `/menu-items/**`.

5. **DTOs.** `CreateMenuItemRequest`, `UpdateMenuItemRequest`,
   `MenuItemResponse`, `ValidateMenuItemsRequest`, `ValidateMenuItemsResponse`.

6. **Test with Postman.** All CRUD + restaurant-scoped list + batch validation.

#### Definition of Done

- [ ] MenuItem table auto-created in `menu_db`
- [ ] All six endpoints from Appendix E Table E.2 work via Postman
- [ ] Batch validation returns prices and availability per item
- [ ] Data persists across restarts

---

### Phase 6 -- Menu Service: Full API, Validation, OpenAPI

**Goal.** Complete the Menu Service API with validation, OpenAPI, error
handling, and CORS.

#### Tasks

1. **Finalise endpoint set per Appendix E Table E.2.**
   - `POST /restaurants/{rid}/menu-items`
   - `GET /restaurants/{rid}/menu-items` (filters: `category`, `available`)
   - `GET /menu-items/{id}`
   - `PUT /menu-items/{id}`
   - `DELETE /menu-items/{id}`
   - `POST /menu-items/validate` (batch validation for Order Service)

2. **Validation.** `@Positive` for `priceAmount`, `@NotBlank` for `name`,
   `@Size` for description.

3. **Global exception handler.** Extract the shared pattern from
   Restaurant Service into a reusable module or duplicate the shape.

4. **OpenAPI / Swagger.** Verify at `http://localhost:8082/swagger-ui.html`.

5. **CORS configuration.**

6. **Batch validation response shape** -- lock it down so Order Service can
   rely on a stable payload (see Appendix E Table E.2, validate endpoint).

#### Definition of Done

- [ ] All six endpoints fully functional
- [ ] Swagger UI renders every endpoint
- [ ] Batch validation responds with stable JSON
- [ ] Postman collection covers all endpoints

---

### Phase 7 -- Sierra-Lima Hardening Pass

**Goal.** Make both services demo-grade rather than just coded. Both must be
protected, testable, and safe to show independently at the project consultation
session on 2026-04-28 if the rest of the team is not ready.

#### Tasks

1. **Create a dev JWT generator utility** before wiring the filter, so tokens
   can actually be tested. Add a small `DevTokenGenerator` class (or a
   `@SpringBootTest` that prints a token) using:
   ```java
   String token = Jwts.builder()
       .setSubject("dev-user-001")
       .claim("role", "RestaurantOwner")
       .setIssuedAt(new Date())
       .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
       .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(DEV_SECRET)))
       .compact();
   ```
   Use a fixed `DEV_SECRET` (a Base64-encoded 256-bit key) stored in
   `application.properties` as `jwt.secret`. The real User Service key will
   replace it once Alfa-Kilo's service exists; the env var name stays the same.
   Add one token per role (`Customer`, `RestaurantOwner`, `Admin`) to the
   Postman environment so all auth cases can be tested immediately.

2. **Wire local token validation.** Add a lightweight `JwtAuthFilter` that
   accepts tokens signed with the agreed dev secret/public key, populates
   `SecurityContext` with the authenticated user id and role, and rejects
   missing or invalid tokens with 401.
   Replace the Phase 2 permissive `SecurityConfig` stub with the real filter chain.

3. **Route-protection matrix applied.**
   - Public (if team agreed): `GET /restaurants`, `GET /restaurants/{id}`,
     `GET /restaurants/{id}/availability`, `GET /restaurants/{rid}/menu-items`,
     `GET /menu-items/{id}`.
   - Authenticated: `POST`, `PUT`, `PATCH`, `DELETE` on all paths.
   - Role-gated (light): restaurant owner or admin for mutations. Precise role
     checks may be left until Phase 15 if the backend is only showing smoke
     integration at CP#1.

4. **Standardise error responses** across both services (consistent JSON error
   envelope: `timestamp`, `status`, `error`, `message`, `path`,
   `validationErrors[]`).

5. **Request validation tightened.** Edge cases: duplicate restaurant, empty
   menu, invalid price, unknown category.

6. **Auditing verified** (`createdAt`, `updatedAt` populated on create/update).

7. **Add seed data** -- `data.sql` or `CommandLineRunner` per service:
   - 4-6 demo restaurants with realistic names, cities, coordinates
   - 12-18 menu items spread across those restaurants, with 3-4 categories

8. **Controller-level and service-level tests** for critical paths (happy path
   + validation errors + 404).

9. **Refresh the Postman collection** with a login placeholder, environment
   token, and all Restaurant + Menu requests.

10. **Assignment 1 feedback guarded:** infrastructure is not mixed into business
    diagrams; databases are strictly service-local.

#### Definition of Done

- [ ] Missing / invalid tokens produce 401
- [ ] Valid tokens unlock mutation endpoints
- [ ] Consistent, structured error responses
- [ ] Seed data loads automatically on startup
- [ ] Tests pass for critical CRUD and validation paths
- [ ] Postman collection complete and shareable
- [ ] Both services demonstrable independently through a login-gated Postman flow

---

### Phase 8 -- Dockerise Both Services

**Goal.** Both services and their databases run entirely inside Docker
containers, orchestrated by Docker Compose.

#### Tasks

1. **Dockerfiles (multi-stage build per service):**
   ```
   Stage 1: maven:3.9-eclipse-temurin-17  (or 21) -- build the JAR
   Stage 2: eclipse-temurin:17-jre               -- run the JAR
   ```

2. **Extend Docker Compose.**
   - `restaurant-db`, `menu-db` (PostgreSQL 15)
   - `restaurant-service` (depends on `restaurant-db`)
   - `menu-service` (depends on `menu-db`)
   - Docker network for service-to-service communication
   - Env vars injected for DB connection strings and JWT settings
   - (Optional now, required later) `api-gateway`, `user-service` once those
     exist from Alfa-Kilo

3. **Spring profiles.**
   - `application.properties` -- local-machine defaults
   - `application-docker.properties` -- Docker-internal hostnames
   - Activated via `SPRING_PROFILES_ACTIVE=docker`

4. **Test the stack.**
   - `docker compose up --build`
   - All endpoints via Postman
   - Data persists in volumes
   - `docker compose down -v` resets state

5. **`.dockerignore` per service** (exclude `target/`, `.idea/`, `.claude/`, `.git/`).

#### Definition of Done

- [ ] `docker compose up --build` starts everything from scratch
- [ ] Both services reachable and functional
- [ ] Databases have persistent volumes
- [ ] No shared database (each service has its own container)
- [ ] `docker compose down` stops cleanly

---

### Phase 9 -- Team Contract Lock for W1 / W2 / W3

**Goal.** Prevent late integration drift. Agree, in writing, the exact calls
and events each service produces and consumes. This phase is coordination-heavy
and may happen across messaging even if the team cannot sit together.

#### Tasks

1. **W1 synchronous call chain locked** (Appendix E Section E.6):
   - `Order -> User` (customer lookup)
   - `Order -> Restaurant` (availability check)
   - `Order -> Menu` (batch validate)
   - `Order -> Payment` (charge)
   - `Order -> Delivery` (create task)
2. **Status codes locked** for validation failures from Restaurant and Menu
   so Order does not have to infer meaning from ad hoc responses.
3. **Event contracts locked** (Appendix E Section E.7):
   - Topics: `payment-events`, `delivery-events`, (optionally `order-events`)
   - Events: `payment.completed`, `payment.failed`, `delivery.status-changed`,
     (optionally `order.cancelled`)
4. **Event envelope shape locked** (id, type, occurredAt, payload).
5. **Dead-letter and idempotency expectations agreed.** Consumers are
   idempotent; event id is the dedup key; failed handlers route to a DLQ.
6. **Token propagation on inter-service calls** confirmed: the original
   caller's token (or a service token) flows on REST hops.

#### Outputs

- Contract sheet for W1
- Event contract sheet for W2 and W3
- Shared event-envelope example
- Route-status code table

#### Definition of Done

- [ ] Every teammate can integrate against written contracts, not chat memory
- [ ] Sierra-Lima's Restaurant and Menu endpoints have agreed response shapes
  for availability and batch validation that Order will actually call

---

### Phase 10 -- W1 Integration & Resilience Protection

**Goal.** Make Sierra-Lima's services practical participants in W1 end-to-end,
with basic resilience patterns protecting the Order -> Restaurant and
Order -> Menu calls (which land on *our* services, so failure modes must be
well-behaved).

Sierra-Lima does not produce or consume Kafka events per A3 scope. This phase
focuses on the synchronous integration Sierra-Lima *is* on the hook for.

#### Tasks -- W1 Integration

1. **End-to-end test the availability check** that Order Service will use:
   `GET /restaurants/{id}/availability` with bearer token, returns
   `{restaurantId, isOpen, acceptsOrders}`.
2. **End-to-end test the batch validation** that Order Service will use:
   `POST /menu-items/validate` with a list of `{menuItemId, quantity}` and
   bearer token, returns per-item existence, availability, and unit price.
3. **Confirm failure behaviour:**
   - Restaurant not found -> 404
   - Restaurant closed -> 200 with `acceptsOrders: false` (or 409 if team agrees)
   - Unknown menu item -> 422 with per-item error breakdown
   - Unavailable menu item -> 422 with per-item error breakdown
   - Unauthorised -> 401 (never a leaky 500)
4. **Coordinate a smoke test with Alfa-Kilo's Order Service** once it exists;
   share seed IDs and demo payloads.

#### Tasks -- Resilience

> **Scope note:** Per A3, Sierra-Lima makes no outbound REST calls to other
> services and is neither a Kafka producer nor consumer. Tasks 5-7 below are
> **only relevant if the team agreement in Phase 9 adds an outbound call from
> Restaurant or Menu to another service.** If no outbound call exists, skip to
> Task 6 (resilient callee hardening) and mark Task 5 and 7 as N/A.

5. **Add Resilience4j** (only if Sierra-Lima makes an outbound call):
   - `spring-cloud-starter-circuitbreaker-resilience4j`
   - `spring-boot-starter-actuator`
   - `spring-boot-starter-aop`
6. **Ensure Sierra-Lima services are *resilient callees*.** Timeouts are
   reasonable; 5xx only happens on genuinely unexpected failures; slow paths
   are logged. This task applies regardless of whether outbound calls exist.
7. **Test with the services stopped** (only if outbound calls exist): confirm
   that when Order's upstream gateway cannot reach Restaurant or Menu, the
   response to the client is a clear, mapped error -- not a stack trace.

#### Definition of Done

- [ ] `Order -> Restaurant` availability check demonstrably works
- [ ] `Order -> Menu` batch validation demonstrably works
- [ ] Known failure paths documented with status codes and payloads
- [ ] Actuator health endpoint exposed (optional circuit-breaker state)

---

### Phase 11 -- Backend Polish & Checkpoint #1 Prep

**Goal.** Package the backend into something that survives a live Checkpoint #1
demonstration on 2026-05-05.

#### Tasks

1. **Code review & cleanup.** Consistent naming, remove debug code, proper
   SLF4J logging in service and client layers.
2. **Verify seed data.** 4-6 restaurants, 12-18 menu items, demo-ready.
3. **Finalise Postman collection.** Folders: Login, Restaurant CRUD, Menu CRUD,
   W1 Integration (availability + batch validate), Async Evidence.
4. **Docker Compose full-stack verification.**
   - `docker compose up --build` from scratch
   - Run through the entire Postman collection
   - Verify: PostgreSQL (x2), (gateway + user when available), Restaurant,
     Menu, (Kafka when available)
5. **Add a smoke-test script** for the main backend flow.
6. **Prepare Checkpoint #1 talking points:**
   - Which seven business services are implemented; why `Review` is design-only
   - Why the team chose static configuration (no Eureka)
   - Why each service has its own DB (and how it answers Assignment 1 feedback)
   - How auth is enforced at gateway and service level
   - Where W1 crosses Sierra-Lima (availability + batch validate)
   - How async (W2/W3) appears in the architecture (even if it's being wired
     by teammates)
   - Live demo script: login -> create restaurant -> add menu items -> toggle
     status -> hit availability -> hit batch validate -> show error paths
7. **Coordinate with team.** Ensure other services at least have stubs;
   agree on the shared Docker Compose file; test cross-team API calls if
   possible.
8. **Update report draft** with backend architecture and workflow diagrams.
9. **Backup screenshots / short screen recording** of the demo path, in case
   of live demo failure.

#### Definition of Done

- [ ] Both services fully functional with all endpoints
- [ ] Full Docker Compose stack starts and works
- [ ] Seed data loads automatically
- [ ] Postman collection complete (login-first)
- [ ] Demo script rehearsed
- [ ] Backup demo materials exist

---

### Phase 12 -- Vue.js Frontend: Shell, Routing & Sign-In

**Goal.** Create the minimum viable Vue.js frontend with navigation, routing,
a login-first flow, and API client utilities that carry bearer tokens.

#### Tasks

1. **Scaffold Vue.js project.**
   - `vue create quickbite-frontend` (Vue 3, Router, Babel)
   - Place under `services/frontend/`
   - Verify `npm run serve` works

2. **Configure API base URL via env.** `VUE_APP_API_BASE_URL=http://localhost:8080`
   (the gateway).

3. **Route map and layout.**
   - Navigation: Home, Restaurants, Menu, Orders, Login / Logout
   - Placeholder views: restaurant list, restaurant detail/menu, cart/order,
     order status, login, signup

4. **Shared API client utility.**
   - `fetch()` wrapper with base URL, content-type, error handling
   - Attaches `Authorization: Bearer <token>` from local storage if present
   - Handles 401 by redirecting to `/login` and clearing token

5. **Login flow.**
   - `Login.vue` -> `POST /auth/login` -> store JWT in `localStorage`
   - `Signup.vue` -> `POST /users` -> redirect to login
   - Logout button clears token and redirects

6. **Route guards.**
   - `beforeEach` navigation guard: redirect to `/login` if protected route
     is accessed without a token.

#### Definition of Done

- [ ] Frontend starts and navigates between pages
- [ ] Login / logout flow functional against a mock or real User Service
- [ ] API client attaches tokens
- [ ] Protected routes redirect when no token present

---

### Phase 13 -- Vue.js Frontend: Restaurant & Menu UX

**Goal.** Expose Sierra-Lima's backend work visibly in the user-facing app.
A signed-in demo user can discover a restaurant, open it, and view orderable
menu items.

#### Tasks

1. **Restaurant views.**
   - `RestaurantList.vue` -- `GET /api/restaurants`, card/table with name,
     city, status (open/closed), link to detail. "Add Restaurant" for owners.
   - `AddRestaurant.vue` -- POST form with validation.
   - `RestaurantDetail.vue` -- GET by id; edit form (PUT); PATCH status
     toggle; link to "View Menu".

2. **Menu item views.**
   - `MenuItemList.vue` -- `GET /api/restaurants/{rid}/menu-items`; filterable
     by category / availability.
   - `AddMenuItem.vue` -- POST with restaurant dropdown populated from backend.
   - `MenuItemDetail.vue` -- GET by id; edit; toggle availability.

3. **Router additions.**
   - `/restaurants`, `/restaurants/new`, `/restaurants/:id`
   - `/restaurants/:id/menu`
   - `/menu-items/new`, `/menu-items/:id`

4. **UI polish.** Loading, empty, and error states. Consistent styling. Role
   visibility: hide owner-side actions for customers.

#### Definition of Done

- [ ] Restaurant CRUD works from the browser
- [ ] Menu item CRUD works from the browser
- [ ] Browse flow: list -> detail -> menu -> item works end-to-end
- [ ] Error states handled gracefully

---

### Phase 14 -- Frontend-Backend Integration & Checkpoint #2 Prep

**Goal.** Full-stack app works end-to-end through the API Gateway.
Demonstrable for Checkpoint #2 (2026-05-12).

#### Tasks

1. **Gateway CORS verified** (frontend -> gateway -> services).

2. **Frontend via Docker.**
   - Dockerfile: multi-stage (node build -> nginx serve)
   - Add to Docker Compose
   - nginx proxies API requests to gateway

3. **End-to-end browser flow:**
   1. Sign in
   2. View restaurant list
   3. Create a new restaurant (as owner)
   4. Add menu items
   5. Toggle restaurant status
   6. Edit and delete operations
   7. (If Order Service is ready) place an order; view status

4. **Connect to W1** if Order Service path is ready.

5. **Update report draft.** Add or refresh the sections covering: data models
   (match actual code), API endpoint tables, and frontend architecture. Diagrams
   that were accurate at CP#1 may have drifted; fix them now rather than at Phase 17.

6. **Prepare Checkpoint #2 demo.** Demo script covering frontend + backend,
   screenshots/recording as backup, list of what remains for CP#3.

#### Definition of Done

- [ ] Frontend communicates with backend through API Gateway
- [ ] Full CRUD workflow works in the browser
- [ ] Docker Compose runs entire stack including frontend
- [ ] One person can demo discovery through order creation in a single run
- [ ] Report draft updated to reflect current implementation state

---

### Phase 15 -- Authorisation Hardening & Role-Aware Behaviour

**Goal.** Turn the placeholder "authenticated" checks from Phase 7 into real
role-aware authorisation. Remove any temporary bypasses. Demonstrate at
CP#3 that unauthorised access is visibly rejected.

#### Tasks

1. **Role-gated mutations on Restaurant Service.**
   - Only the `ownerId` user or an admin can `PUT /restaurants/{id}` or
     `PATCH /restaurants/{id}/status`.
   - Public GETs remain as decided in Phase 1.

2. **Role-gated mutations on Menu Service.**
   - Only the restaurant's owner or an admin can `POST`, `PUT`, or `DELETE`
     on menu items for that restaurant.
   - `POST /menu-items/validate` can be locked to service tokens or remain
     customer-callable, whichever the team agreed.

3. **Remove any `permitAll()` or debug shortcuts** added during Phases 3-7.

4. **Audit-test.** With Postman, verify:
   - Customer token on owner-only endpoint -> 403
   - Owner A token on Restaurant owned by Owner B -> 403
   - Admin token -> 200
   - Missing token -> 401

5. **Log security denials** at `WARN` so the demo can optionally show them.

#### Definition of Done

- [ ] Protected endpoints return 401 without valid JWT
- [ ] Role-based access works (Customer vs Driver vs RestaurantOwner vs Admin)
- [ ] Ownership check enforced on restaurant and menu mutations
- [ ] Postman collection includes negative-auth cases

---

### Phase 16 -- Async Evidence & Cross-Service Smoke

**Goal.** Ensure at least one asynchronous workflow is *demonstrably real* by
Checkpoint #3, even though Sierra-Lima is neither producer nor consumer in the
A3 event topology.

#### Tasks

1. **Coordinate with Mike-Alfa.** Confirm Kafka is up, topics are created,
   Notification Service is consuming events correctly.

2. **Coordinate with Elephant-Yankee.** Confirm Delivery Service publishes
   `delivery.status-changed` and Payment Service publishes `payment.completed`
   and `payment.failed`.

3. **Coordinate with Alfa-Kilo.** Confirm Order Service consumes
   `delivery.status-changed` and updates order state.

4. **Document Sierra-Lima's role.** Restaurant/Menu services do not produce
   or consume events in the baseline A3 scope. For CP#3, the async demo is
   driven by teammate-owned services; Sierra-Lima's role is to keep Restaurant
   and Menu endpoints stable so the overall W1 -> W2 -> W3 chain stays real.

5. **Optional (time permitting).** Add a `MenuItemAvailabilityChanged` event
   producer to Menu Service. This is not in A3 scope but makes the final demo
   stronger. Publish `menu.item-availability-changed` to a new
   `menu-events` topic. Mike-Alfa's broker configuration should already
   accept new topics.

6. **Cross-service smoke test.** Run one full trace from login through
   W1 synchronous path into W2 async path, confirming Sierra-Lima's services
   stay responsive under realistic load (single demo user).

#### Definition of Done

- [ ] At least one async workflow visibly works end-to-end
- [ ] Sierra-Lima's services stay stable under the integrated flow
- [ ] Smoke-test script captures the full trace for replay during the demo

---

### Phase 17 -- Report & Evidence Pack

**Goal.** Finish the written deliverable before the final presentation crunch.
If the code froze tomorrow, the report would still be presentation-quality.

#### Tasks

1. **Assemble report sections.**
   - Business architecture (from Assignments 1-2, figure 1)
   - Technical architecture (actual implementation, figure 1b)
   - Implemented vs design-only services; justification for `Review` design-only
   - Data models (ER diagrams matching actual code, figure 2)
   - APIs (endpoint tables, Swagger screenshots)
   - Workflows: W1 synchronous (figure 3); W2 and W3 asynchronous (figure 4)
   - Integration mechanisms: REST, gateway routing, Kafka topics and envelope
   - Security approach: JWT issuance, validation, role-gating
   - Team responsibilities (see §3.3)
   - Limitations and future work (e.g. `Review Service` to be built later)

2. **Refresh diagrams to match the actual code.** Do not just reuse the
   `assignment-3` figures verbatim if the implementation diverged.

3. **Add screenshots, endpoint tables, topic tables, and demo notes.**

4. **Proofread and format for submission.**

#### Definition of Done

- [ ] All report sections drafted
- [ ] Diagrams match the implemented system
- [ ] Evidence (screenshots, tables) included
- [ ] Report is near-final quality

---

### Phase 18 -- Final Presentation Rehearsal

**Goal.** Turn the working system into a convincing presentation.

#### Tasks

1. **Slide deck** covering:
   - System overview and architecture
   - Sierra-Lima's services: Restaurant + Menu
   - API design (Swagger screenshots)
   - Synchronous integration: Menu batch validation in W1
   - Asynchronous integration: W2 / W3 end-to-end
   - Security: JWT issuance and validation, role-gating
   - Frontend walkthrough
   - (Optional) Resilience demo

2. **Live demo script** with exact click-path and expected outputs.

3. **Assign speaking parts** -- architecture overview, Sierra-Lima services,
   synchronous flow, asynchronous flow, security.

4. **Fallback materials** -- screenshots, pre-recorded flow, backup seed data,
   recovery commands.

5. **Rehearse timeboxed answers** to likely questions:
   - Why were these seven services implemented?
   - Why is `Review` design-only?
   - How were service boundaries chosen?
   - How does async integration work? (topics, envelope, idempotency)
   - How was security enforced at gateway vs service level?
   - Why no Eureka?

#### Definition of Done

- [ ] Slides complete
- [ ] Live demo rehearsed at least once
- [ ] Every presenter knows what to say, click, and do if the demo misbehaves
- [ ] Backup materials ready

---

### Phase 19 -- Buffer & Final Freeze

**Goal.** Use remaining time for stabilisation only, not feature invention.

#### Tasks

1. **Fix highest-risk bugs only.** No new features.
2. **Re-run smoke tests.**
3. **Verify seeded demo users, restaurants, menu items, and order flow.**
4. **Verify clean startup** (`docker compose down -v && docker compose up --build`).
5. **Freeze the branch** for presentation.

#### Definition of Done

- [ ] Full system starts and runs in Docker Compose
- [ ] All workflows demonstrable end-to-end
- [ ] No unresolved defect remains that could break the main demo narrative
- [ ] Code is clean and committed

---

## 9. Checkpoint Readiness Gates

Use these as go/no-go criteria before each checkpoint.

### Checkpoint #1 Gate (2026-05-05 -- Backend)

The project is not ready unless **all** of these are true:

- [ ] At least the main implemented backend services compile and run
- [ ] Restaurant Service and Menu Service are fully operational, **protected by bearer-token validation**
- [ ] Login or token issuance demonstrable (even if via mocked User Service)
- [ ] W1 happy path works through the gateway or through documented service calls
- [ ] Each service owns its own database
- [ ] Backend startable from a documented local process
- [ ] Architecture and workflow diagrams match reality

### Checkpoint #2 Gate (2026-05-12 -- Frontend + Backend)

All CP#1 criteria still hold, plus:

- [ ] Frontend signs in and uses real protected APIs (not hardcoded mocks)
- [ ] A user can browse restaurants, inspect menus, and (ideally) place an order
- [ ] UI handles at least basic loading and error states
- [ ] Demo does not rely on hidden manual DB manipulation

### Checkpoint #3 Gate (2026-05-19 -- Final Presentation)

All CP#2 criteria still hold, plus:

- [ ] Role-aware authorisation visibly enforced (customer vs owner vs admin)
- [ ] At least one asynchronous workflow (W2 or W3) demonstrably working
- [ ] Report is presentation-ready and matches the implemented system
- [ ] Team has a demo fallback plan

---

## 10. What Sierra-Lima Can Safely Do Early, Even Alone

These are the highest-value early-start tasks that don't require teammates:

1. Complete Phases 0-8 without waiting for anyone.
2. Produce canonical Restaurant / Menu API contracts and OpenAPI docs.
3. Seed compelling restaurant and menu demo data.
4. Make `POST /menu-items/validate` especially strong -- Order Service
   depends on it directly in W1.
5. Keep both services documented through OpenAPI and Postman.
6. Prepare stable example IDs, payloads, and failure responses for the Order
   team.
7. Set up Docker Compose and the local-dev runbook for the whole team.
8. Mock the User Service locally for token issuance so login + protected
   routes can be demonstrated even before Alfa-Kilo's real service exists.

**Stubbing strategy while solo:**

- Issue dev JWTs from a tiny local signer with a shared secret; swap to
  User Service's real keys once it exists.
- Use hardcoded UUIDs for `ownerId` until User Service exists.
- Skip Order Service calls; use Postman to call `availability` and
  `validate` endpoints directly.
- Test async flow using `kafka-console-consumer` against teammate-produced
  events once the broker is up.

---

## 11. Final Demo Success Criteria

By final-presentation-ready status, the team should be able to demonstrate:

1. A customer signs in and browses restaurants and menus.
2. An order is created through the frontend.
3. Backend coordination across multiple services (W1), including Sierra-Lima's
   availability check and batch validation.
4. At least one event-driven update or notification (W2 or W3).
5. At least one role-gated authorisation rejection (403 for wrong role).
6. Clear service boundaries and separate data ownership.
7. A report and diagrams that match the implemented system.

---

## 12. Bottom Line

The safest route from this repository's current scaffolded state to a credible
final project is:

1. **Re-baseline** scope and auth assumptions now that User Service is
   implemented and tokens are mandatory.
2. **Build Sierra-Lima's two services** properly, protected from day one.
3. **Use them as stable anchors** for the rest of the team, especially for
   the W1 availability check and batch validation.
4. **Reach backend integration** by 2026-05-05.
5. **Layer in frontend** by 2026-05-12.
6. **Finish authorisation hardening, async evidence, report, and presentation
   readiness** by 2026-05-19.

If the team follows the phase order above, early solo work is not wasted, and
the project stays aligned with the course assignments, practicals, and
checkpoint structure.

---

## Appendix A -- Suggested Session Calendar

Assuming ~4 sessions per week starting 2026-04-18:

| Session | Date (approx.) | Phase | Notes |
|---------|---------------|-------|-------|
| 1 | Apr 18 (Sat) | Phase 0 | Scope freeze and conventions |
| 2 | Apr 19 (Sun) | Phase 1 | Auth + gateway contract alignment |
| 3 | Apr 20 (Mon) | Phase 2 | Contract pack + local-dev bootstrap |
| -- | Apr 21 (Tue) | -- | *Project description practical* |
| 4 | Apr 22 (Wed) | Phase 3 | Restaurant foundation |
| 5 | Apr 23 (Thu) | Phase 4 | Restaurant full API + Swagger |
| 6 | Apr 25 (Sat) | Phase 5 | Menu foundation |
| 7 | Apr 26 (Sun) | Phase 6 | Menu full API + Swagger |
| 8 | Apr 27 (Mon) | Phase 7 | Hardening pass |
| -- | Apr 28 (Tue) | -- | *Project consultation practical* |
| 9 | Apr 29 (Wed) | Phase 8 | Dockerise both services |
| 10 | Apr 30 (Thu) | Phase 9 | Team contract lock |
| 11 | May 02 (Sat) | Phase 10 | W1 integration + resilience |
| 12 | May 03 (Sun) | Phase 11 | Backend polish + CP#1 prep |
| **--** | **May 05 (Tue)** | **--** | ***Checkpoint #1 (Backend)*** |
| 13 | May 06 (Wed) | Phase 12 | Frontend shell + sign-in |
| 14 | May 08 (Fri) | Phase 13 | Restaurant + Menu UX |
| 15 | May 10 (Sun) | Phase 14 | Frontend-backend integration + CP#2 prep |
| **--** | **May 12 (Tue)** | **--** | ***Checkpoint #2 (Frontend + Backend)*** |
| 16 | May 13 (Wed) | Phase 15 | Authorisation hardening |
| 17 | May 14 (Thu) | Phase 16 | Async evidence + cross-service smoke |
| 18 | May 16 (Sat) | Phase 17 | Report + evidence pack |
| 19 | May 17 (Sun) | Phase 18 | Final rehearsal |
| 20 | May 18 (Mon) | Phase 19 | Buffer + freeze |
| **--** | **May 19 (Tue)** | **--** | ***Checkpoint #3 (Final Presentation)*** |

### Compression Guidance

If any phase slips, compress in this priority order (sacrifice optional extras first):

1. **Drop first:** optional Menu-as-Kafka-producer in Phase 16; Resilience4j
   instrumentation in Phase 10 if core W1 is stable without it.
2. **Merge second:** Phases 3+4 (Restaurant foundation + full API in one
   session if confident); Phases 5+6 (same for Menu); Phases 12+13 (frontend
   shell + UX in one session if Vue experience exists).
3. **Never compress:** Phase 1 (auth contract), Phase 9 (team contract lock),
   Phase 15 (authorisation hardening), Phase 11 / 14 / 18 (checkpoint prep),
   Phase 19 (buffer).

---

## Appendix B -- Team Coordination Points

| When | What must be agreed | Who |
|------|---------------------|-----|
| Before Phase 1 | Java version, build tool, package structure, naming rules | All |
| During Phase 1 | Public vs protected routes, JWT claims, token propagation | All |
| Before Phase 2 | Gateway path prefixes, port assignments, env-var matrix | All |
| Before Phase 8 | Docker Compose service names, network, shared volumes | Backend owners |
| Before Phase 9 | Exact REST payloads for W1 hops, event topics and payloads | Backend owners |
| Before Phase 10 | Status-code contract for validation failures | Backend owners |
| Before Phase 15 | Role-gating matrix and ownership-check approach | All |
| Before Phase 18 | Slide structure, demo path, fallback plan, speaking order | All |

When teammates are late, Sierra-Lima can still move by using stable IDs,
seeded demo data, locally issued dev tokens, documented assumptions, and
contract-first stubs -- then replacing those stubs once real services arrive.

---

## Appendix C -- Risk Register

| Risk | Impact | Mitigation |
|------|--------|------------|
| Teammates still work from the old six-service assumption | Wrong integration and wrong scope at CP#1 | Use this roadmap as the corrected local baseline; share at next team meeting |
| Auth is implemented inconsistently across gateway and services | Broken logins, leaky endpoints | Front-load Phase 1; agree JWT claims and token propagation before hardening |
| Sierra-Lima builds good local services that do not fit the shared auth path | Rework after CP#1 | Implement against the agreed auth contract, not a solo-only shape |
| Static URLs drift across services | Run-time errors, late debugging | Maintain one central local-dev env matrix (§Phase 2 step 10) |
| Event names or payloads drift across teams | Consumers break silently | Lock contracts in Phase 9; keep one shared envelope example |
| Protected-read behaviour hurts UX and causes late debate | CP#2 risk | Decide explicitly in Phase 1 whether browse endpoints stay protected |
| **Shared database shortcut during integration** | **Repeat of Assignment 1 penalty** | **Reject immediately. Cross-service references are IDs only.** |
| Docker / Kafka setup issues on Windows | Blocks multiple phases | Allocate extra time in Phase 8; WSL2 as fallback |
| Assignment 3 design changes after feedback | Rework APIs/models | Keep services small; design for easy change; log decisions |
| Scope expands back to all eight business services | Time crunch | Hold the A3 implementation subset unless the instructor explicitly asks for more |
| Diagrams and report fall behind code | Last-minute scramble | Phase 17 dedicated; update diagrams at each checkpoint, not only at the end |
| Checkpoint demo fails live | Lost marks | Screenshots / recording as backup; rehearse with Docker; Phase 19 buffer |

---

## Appendix D -- Assignment Feedback to Address

From Assignment 1 feedback (3.50/4.00):

> **-0.25**: Infrastructure elements mixed into architecture diagram.  
> **-0.25**: Shared database across microservices.

**Actions taken in this project:**

- Architecture diagrams show business services only (no API Gateway, DB, Kafka
  in logical diagrams). Figure 1 (business) vs figure 1b (technical) are kept
  separate.
- Each microservice has its **own PostgreSQL database** -- enforced in Docker
  Compose with separate containers, volumes, and ports.
- Technical infrastructure (gateway, discovery, messaging) is treated as an
  implementation detail, not as part of the business architecture.

---

## Appendix E -- Canonical A3 Reference (pinned so this file is self-contained)

This appendix pins the Assignment 3 design facts that drive the phase plan, so
the file can be used without round-tripping back to the PDFs. If a conflict is
found between this appendix and the committed `Assignment-3-Submission.pdf`
at `aac68b0`, the PDF is authoritative -- update this appendix, then the rest
of the plan.

### E.1 Restaurant Service Endpoints (Sierra-Lima)

| Method | Path | Purpose | Auth |
|--------|------|---------|------|
| POST | `/restaurants` | Register a new restaurant profile | RestaurantOwner / Admin |
| GET | `/restaurants/{id}` | Get a restaurant profile | Public (or Customer, per team decision) |
| PUT | `/restaurants/{id}` | Update profile (hours, location, details) | Owner of this restaurant / Admin |
| PATCH | `/restaurants/{id}/status` | Change open/closed | Owner / Admin |
| GET | `/restaurants` | Search/list (filters: `city`, `isOpen`) | Public (or Customer) |
| GET | `/restaurants/{id}/availability` | Lightweight availability check; called by Order Service in W1 | Service or Customer |

**Availability response shape:**

```json
{
  "restaurantId": "uuid",
  "isOpen": true,
  "acceptsOrders": true,
  "operatingHours": "09:00-22:00",
  "checkedAt": "2026-05-05T12:34:56Z"
}
```

### E.2 Menu Service Endpoints (Sierra-Lima)

| Method | Path | Purpose | Auth |
|--------|------|---------|------|
| POST | `/restaurants/{rid}/menu-items` | Add a new menu item | Owner of `rid` / Admin |
| GET | `/restaurants/{rid}/menu-items` | List items for a restaurant (filters: `category`, `available`) | Public (or Customer) |
| GET | `/menu-items/{id}` | Get a single item | Public (or Customer) |
| PUT | `/menu-items/{id}` | Update item | Owner / Admin |
| DELETE | `/menu-items/{id}` | Remove item | Owner / Admin |
| POST | `/menu-items/validate` | Batch validate a list of `{menuItemId, quantity}`; return unit prices; called by Order Service in W1 | Service or Customer |

**Batch validation request shape:**

```json
{
  "items": [
    { "menuItemId": "uuid", "quantity": 2 },
    { "menuItemId": "uuid", "quantity": 1 }
  ]
}
```

**Batch validation response shape:**

```json
{
  "allValid": false,
  "items": [
    {
      "menuItemId": "uuid",
      "exists": true,
      "isAvailable": true,
      "unitPriceAmount": "12.50",
      "unitPriceCurrency": "EUR",
      "quantity": 2,
      "lineTotal": "25.00"
    },
    {
      "menuItemId": "uuid",
      "exists": false,
      "error": "MENU_ITEM_NOT_FOUND"
    }
  ],
  "totalAmount": "25.00",
  "currency": "EUR"
}
```

### E.3 Restaurant Service Data Model

`Restaurant` (aggregate root, one per DB row):

| Field | Type | Notes |
|-------|------|-------|
| `restaurantId` | UUID | PK |
| `ownerId` | UUID | Cross-service ref to `User.userId` |
| `name` | String | not blank |
| `address` | String | |
| `city` | String | |
| `latitude` | Double | -90..90 |
| `longitude` | Double | -180..180 |
| `operatingHours` | String | e.g. "09:00-22:00" |
| `isOpen` | Boolean | default `false` |
| `createdAt` | LocalDateTime | audited |
| `updatedAt` | LocalDateTime | audited |

### E.4 Menu Service Data Model

`MenuItem` (aggregate root):

| Field | Type | Notes |
|-------|------|-------|
| `menuItemId` | UUID | PK |
| `restaurantId` | UUID | Cross-service ref to `Restaurant.restaurantId` |
| `name` | String | not blank |
| `description` | String | optional |
| `priceAmount` | BigDecimal | scale 2, positive |
| `priceCurrency` | String | ISO-4217, default `"EUR"` |
| `category` | String | e.g. `Appetizer`, `Main`, `Dessert`, `Drink` |
| `isAvailable` | Boolean | default `true` |
| `createdAt` | LocalDateTime | audited |
| `updatedAt` | LocalDateTime | audited |

### E.5 Gateway Path Map

| Gateway prefix | Target service | Owner |
|----------------|----------------|-------|
| `/api/auth/**`, `/api/users/**` | User Service | Alfa-Kilo |
| `/api/orders/**` | Order Service | Alfa-Kilo |
| `/api/restaurants/**` | Restaurant Service | Sierra-Lima |
| `/api/menu-items/**`, `/api/restaurants/*/menu-items/**` | Menu Service | Sierra-Lima |
| `/api/payments/**` | Payment Service | Elephant-Yankee |
| `/api/deliveries/**`, `/api/drivers/**` | Delivery Service | Elephant-Yankee |
| `/api/notifications/**` | Notification Service | Mike-Alfa |

### E.6 W1 Synchronous Call Chain (Place Order)

1. `Client -> API Gateway`: `POST /api/orders` with
   `Authorization: Bearer <token>` and body `{ customerId, restaurantId, items[], deliveryAddress }`.
2. `API Gateway -> Order Service`: gateway validates token, forwards request.
3. `Order -> User Service`: customer lookup via bearer token (verify the user exists and is active).
4. `Order -> Restaurant Service`: `GET /restaurants/{id}/availability` -- confirm restaurant is open and accepting orders.
5. `Order -> Menu Service`: `POST /menu-items/validate` with the items list -- verify existence, availability, and unit prices.
6. `Order Service` persists the order with status `Placed`.
7. `Order -> Payment Service`: `POST /payments` with `{ orderId, amount }`. Synchronous charge.
8. On `Completed`: `Order -> Delivery Service`: `POST /deliveries` with pickup/dropoff. Order status transitions `Paid -> Confirmed`.
9. `Order Service` returns `201 Created` with the order id and first status.

**Failure handling.**
- Step 3 failure (customer invalid): `401 Unauthorized`.
- Step 4 failure (restaurant closed): `409 Conflict` (or `200` with `acceptsOrders:false` per team agreement).
- Step 5 failure (menu items invalid/unavailable): `422 Unprocessable Entity` with per-item error breakdown.
- Step 7 failure (payment declined): mark order `Cancelled`; no delivery task created.
- Step 8 failure (delivery creation fails): compensating `POST /payments/{id}/refund` to cancel the charge; order `Cancelled`.

### E.7 W2 / W3 Event Contracts

**Topic:** `payment-events`

| Event | Producer | Consumer(s) | Payload |
|-------|----------|-------------|---------|
| `payment.completed` | Payment Service | Notification Service | `{ paymentId, orderId, amount, occurredAt }` |
| `payment.failed` | Payment Service | Notification Service, Order Service | `{ paymentId, orderId, reason, occurredAt }` |

**Topic:** `delivery-events`

| Event | Producer | Consumer(s) | Payload |
|-------|----------|-------------|---------|
| `delivery.status-changed` | Delivery Service | Order Service, Notification Service | `{ deliveryId, orderId, status, occurredAt }` |

**Topic (optional):** `order-events`

| Event | Producer | Consumer(s) | Payload |
|-------|----------|-------------|---------|
| `order.cancelled` | Order Service | Payment Service (runs refund), Notification Service | `{ orderId, reason, occurredAt }` |

**Envelope shape:** `{ id, type, occurredAt, payload }`. `id` is the idempotency key.

**Delivery guarantees:**

- At-least-once.
- Consumers are idempotent (dedup by envelope `id`).
- Failed handlers route to a dead-letter queue.

### E.8 Data Model Quick Reference (Other Services)

Sierra-Lima does not write these, but the ER diagram committed at `aac68b0`
(figure 2) provides the full picture. Summaries relevant to W1:

- **User Service:** `User(userId, email, passwordHash, fullName, phoneNumber, role, status)`; `Address(addressId, userId, ...)`; `DriverProfile(userId, licenceNumber, vehicleType, isAvailable)`.
- **Order Service:** `Order(orderId, customerId, restaurantId, status, placedAt, totalAmount, deliveryStreet, deliveryCity, deliveryPostalCode)`; `OrderItem(orderItemId, orderId, menuItemId, name, unitPrice, quantity)`.
- **Payment Service:** `Payment(paymentId, orderId, amount, currency, status, processedAt)`; `Transaction(transactionId, paymentId, type, amount, occurredAt)`.
- **Delivery Service:** `DeliveryTask(deliveryId, orderId, driverId, pickupStreet, pickupCity, dropoffStreet, dropoffCity, status, assignedAt)`.
- **Notification Service:** `Notification(notificationId, recipientId, channel, message, sentAt, status)`.

All cross-service references are ID-only; no foreign keys cross DB boundaries.

---

*End of roadmap. Next step: execute Phase 0 in the next working session.*