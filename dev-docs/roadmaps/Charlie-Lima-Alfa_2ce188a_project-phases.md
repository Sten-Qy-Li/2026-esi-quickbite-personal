# QuickBite ESI Project -- Phased Implementation Plan

**Pseudonym:** Charlie-Lima-Alfa  
**Base commit:** `2ce188a`  
**Student:** Sierra-Lima  
**Services owned:** Menu Service, Restaurant Service  
**Team:** Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa (Group 7)  
**Date created:** 2026-04-17  

---

## 1. Overview

This document maps the journey from the current empty-scaffold repository to a
presentation-ready system across a series of focused 3-hour working sessions
("Phases"). Each Phase has a clear goal, concrete deliverables, and a
definition-of-done checklist.

It is grounded in:

- the 2025/26 ESI course page and the 2026 practicals sequence
- Assignments 1, 2, and 3 already stored in this repository
- the current repository state (documentation scaffolding plus empty `restaurant-service`, `menu-service`, and `local-dev` folders)
- the fact that Sierra-Lima is responsible for `Restaurant Service` and `Menu Service`

### 1.1 Key Deadlines

| Date | Milestone | Focus |
|------|-----------|-------|
| 2026-04-21 | Project description released | Official project spec from instructor |
| 2026-04-28 | Project consultation session | Clarify scope and expectations with instructor |
| **2026-05-05** | **Checkpoint #1** | **Backend implementation** |
| **2026-05-12** | **Checkpoint #2** | **Frontend + Backend integration** |
| **2026-05-19** | **Checkpoint #3** | **Final presentations (Frontend + Backend + Security)** |
| 2026-05-25 | Exam 1 | Written exam |

Note: the practicals page shows `28/04/2025` for project consultation, but in the 2026 schedule context that is almost certainly a typo; this plan treats the consultation as `2026-04-28`.

---

## 2. Baseline Project Scope

### 2.1 Business System

QuickBite is a food-delivery platform with eight business services in the overall architecture:

1. `User Service` (Alfa-Kilo)
2. `Order Service` (Alfa-Kilo)
3. `Menu Service` (Sierra-Lima)
4. `Restaurant Service` (Sierra-Lima)
5. `Delivery Service` (Elephant-Yankee)
6. `Payment Service` (Elephant-Yankee)
7. `Notification Service` (Mike-Alfa)
8. `Review Service` (Mike-Alfa)

### 2.2 Implementation Subset

Assignment 3 narrowed the implementation scope. Unless instructor feedback forces a change:

- **Implemented business services:** Order, Restaurant, Menu, Payment, Delivery, Notification
- **Implemented shared components:** API Gateway, Kafka/Event Broker configuration
- **Design-only services:** User, Review (represented in architecture diagrams but not coded)

### 2.3 Sierra-Lima Domain Recap

**Restaurant Service** (from Assignments 1-2)
- Aggregate root: `Restaurant` -- restaurantId, ownerId\*, name, location, operatingHours, isOpen
- Value Object: `Location` -- address, city, latitude, longitude
- Repository: `RestaurantRepository`
- Requirements: R19 (register/manage restaurant), R20 (update status/hours)

**Menu Service** (from Assignments 1-2)
- Aggregate root: `MenuItem` -- menuItemId, restaurantId\*, name, description, price, category, isAvailable
- Value Object: `Price` -- amount, currency
- Repository: `MenuRepository`
- Requirements: R21 (add/update/remove items), R22 (browse menu for a restaurant)

### 2.4 Design Decisions to Honour

- Each microservice owns its own database (no shared DB -- penalised in Assignment 1 feedback)
- Cross-service references use ID fields only (e.g. `restaurantId*`)
- At least one synchronous interaction (REST call) and one asynchronous interaction (Kafka event)
- Sierra-Lima may optionally replace one service with a shared integration/resilience component (API Gateway, Service Discovery, etc.)

### 2.5 Technology Stack (from course practicals)

| Layer | Technology | Notes |
|-------|-----------|-------|
| Backend framework | Spring Boot (Java) | Maven-friendly setup aligned with practicals |
| Database | PostgreSQL | One DB per service |
| Containerisation | Docker + Docker Compose | Local reproducibility is mandatory |
| Service discovery | Spring Cloud Netflix Eureka | Optional; only if the team wants a stronger infra demo |
| API gateway | Spring Cloud Gateway | Consistent with Assignment 3 design |
| Async messaging | Apache Kafka + Zookeeper | Use Zookeeper only if the chosen image still requires it |
| Resilience | Resilience4j | Optional; only after the core flows are stable |
| Frontend | Vue.js 3 (Vue CLI, Vue Router, Fetch API) | |
| Security | Spring Security + JWT (jjwt 0.11.5) | Required by final checkpoint |
| API documentation | OpenAPI / Swagger | Keep docs live from early service phases |
| API testing | Postman | Maintain one shared collection for demos and regression checks |

---

## 3. Named Workflows

These three workflow labels are used throughout the plan to avoid ambiguity:

| Label | Name | Style | Summary |
|-------|------|-------|---------|
| **W1** | Place Order | Synchronous | Client submits order -> Order Service checks restaurant availability (Restaurant Service) -> validates menu items (Menu Service) -> creates order -> triggers payment (Payment Service) -> creates delivery task (Delivery Service) |
| **W2** | Delivery Updates & Notifications | Asynchronous | Delivery Service publishes status events -> Notification Service consumes and notifies user -> Order Service updates order status |
| **W3** | Payment Outcome Notification | Asynchronous | Payment Service publishes payment.completed / payment.failed -> Order Service and Notification Service consume |

At minimum, **W1** demonstrates synchronous REST integration, and **W2** or **W3** demonstrates Kafka-based async integration. Together they satisfy the Assignment 3 requirement for both interaction styles.

---

## 4. Planning Principles

These principles govern every phase:

1. **Align with Assignments 1-3** rather than reinventing the system.
2. **Separate business architecture from technical architecture** in all diagrams and documentation.
3. **Give every implemented microservice its own database.** No shared database. Reject this shortcut immediately if it appears during integration.
4. **Prioritise the two required interaction styles:** synchronous REST (W1) and asynchronous Kafka (W2/W3).
5. **Keep each service small and demoable.** Assignment 3 suggests ~5-8 endpoints per service.
6. **Avoid infrastructure inflation.** Eureka, client-side load balancing, and other extras remain optional unless the instructor explicitly requests them.
7. **Front-load solo-capable work** so early phases produce artifacts useful even if teammates join late: API contracts, ER/data models, Docker Compose setup, seed data, service skeletons, OpenAPI docs.
8. **Treat security as mandatory** for the final phase, even if the Assignment 3 design left User Service as design-only.
9. **Keep auth assumptions abstract enough** early so JWT can be inserted later without rewriting every service.

---

## 5. Delivery Strategy

Because the repository is currently almost empty and the team may mobilise late, the safest strategy is:

1. **Freeze** the scope and interfaces
2. **Build** Sierra-Lima's two services completely enough to become stable integration targets
3. **Assemble** backend workflow W1 before Checkpoint #1
4. **Layer** frontend before Checkpoint #2
5. **Finish** security, async polish, report, and presentation readiness before Checkpoint #3

This ordering keeps early solo work reusable and reduces merge chaos later.

### 5.1 Cross-Phase Working Assets

These assets should be kept live throughout the project rather than created at the end:

- One shared Postman collection with environment variables and smoke requests
- Swagger/OpenAPI links for every implemented backend service
- A seeded demo dataset for restaurants, menu items, and core workflow entities
- One documented Docker Compose runbook for clean startup
- Health-check or ping endpoints for quick verification
- Screenshot or recording backups for each checkpoint demo

Keeping these artifacts current reduces integration risk and makes late team coordination less damaging.

---

## 6. Phase Map at a Glance

| Phase | Title | Checkpoint Target | Est. Sessions |
|-------|-------|-------------------|---------------|
| 0 | Scope Freeze & Repo Conventions | -- | 1 |
| 1 | Contract Pack & Environment Bootstrap | -- | 1 |
| 2 | Restaurant Service -- Foundation | CP#1 | 1 |
| 3 | Restaurant Service -- Full API & Swagger | CP#1 | 1 |
| 4 | Menu Service -- Foundation | CP#1 | 1 |
| 5 | Menu Service -- Full API & Swagger | CP#1 | 1 |
| 6 | Sierra-Lima Hardening Pass | CP#1 | 1 |
| 7 | Dockerise Both Services | CP#1 | 1 |
| 8 | Inter-Service Communication (Sync REST) | CP#1 | 1 |
| 9 | Service Discovery & API Gateway | CP#1 | 1 |
| 10 | Event-Driven Integration with Kafka | CP#1 | 1 |
| 11 | Backend Workflow W1 Assembly & Resilience | CP#1 | 1 |
| 12 | Backend Polish & Checkpoint #1 Prep | CP#1 | 1 |
| 13 | Vue.js Frontend -- Shell & Routing | CP#2 | 1 |
| 14 | Vue.js Frontend -- Restaurant & Menu UX | CP#2 | 1 |
| 15 | Frontend-Backend Integration & Checkpoint #2 Prep | CP#2 | 1 |
| 16 | Security -- Spring Security & JWT | CP#3 | 1 |
| 17 | Secure Full-Stack Integration | CP#3 | 1 |
| 18 | Report & Evidence Pack | CP#3 | 1 |
| 19 | Final Presentation Rehearsal & Buffer | CP#3 | 1 |

**Total: ~20 sessions x 3 hours = ~60 hours of focused work**

If time is tight, see *Appendix A -- Compression Guidance* for which phases can be merged or trimmed.

---

## 7. Detailed Phase Plan

### Phase 0 -- Scope Freeze & Repo Conventions

**Goal:** Turn the assignment outputs into a single implementation baseline so nobody
on the team is still debating what's in or out.

#### Tasks

1. **Reconfirm project scope from Assignment 3**
   - Implemented services: Order, Restaurant, Menu, Payment, Delivery, Notification
   - Shared components: API Gateway, Kafka
   - Design-only: User, Review (unless feedback forces a change)

2. **Freeze the canonical workflows**
   - W1: place order (synchronous)
   - W2: delivery updates and notifications (asynchronous)
   - W3: payment outcome notification (asynchronous complement)

3. **Decide folder layout** for the codebase that will hold all services

4. **Define conventions**
   - Branch strategy (e.g. `dev` for daily work, feature branches, `main` for releases)
   - Commit message format
   - Naming conventions (packages, endpoints, env vars)
   - Java version (17 or 21)

5. **Record open design questions** in `dev-docs/decisions/`

6. **Produce a non-goals list** for the first implementation pass (e.g., "no frontend until after CP#1", "no real payment gateway", "no mobile app")

#### Definition of Done
- [ ] Implementation subset confirmed in writing
- [ ] Folder structure agreed
- [ ] Conventions documented
- [ ] Non-goals list exists
- [ ] Nobody on the team should still be debating which services are in or out

---

### Phase 1 -- Contract Pack & Environment Bootstrap

**Goal:** Make Restaurant Service and Menu Service precise enough that other
teammates can integrate against them later, and have a working local dev
environment.

#### Tasks -- Contract Pack

1. **Define final REST endpoints** for both services (from Assignment 3)
2. **Freeze request and response payloads** (JSON schemas)
3. **Freeze validation rules:**
   - Restaurant: required fields, operating hours format
   - Menu: price representation (BigDecimal + currency), availability rules
4. **Define database schemas** for both services (ER-style)
5. **Create seed-data plan** for demo restaurants and menu items
6. **Write cross-service assumptions explicitly:**
   - `MenuItem.restaurantId` references `Restaurant.restaurantId`
   - No cross-service joins, only ID references

#### Tasks -- Environment Bootstrap

7. **Install / verify prerequisites**
   - Java 17+ (or 21) JDK
   - Maven (matches course examples)
   - Docker Desktop (Windows with WSL2 backend)
   - Docker Compose v2
   - Node.js 18+ and NPM (for Vue.js later)
   - Vue CLI (`npm install -g @vue/cli`)
   - Postman
   - IDE: IntelliJ IDEA or VS Code with Spring Boot extensions

8. **Initialise Spring Boot projects** via [Spring Initializr](https://start.spring.io/):
   - `restaurant-service`: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, DevTools
   - `menu-service`: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, DevTools
   - Place generated sources under `services/restaurant-service/` and `services/menu-service/`

9. **Set up local PostgreSQL databases via Docker Compose**
   - `services/local-dev/docker-compose.yml` with two PostgreSQL containers:
     - `quickbite-restaurant-db` (port 5432)
     - `quickbite-menu-db` (port 5433)
   - Each with its own volume for persistence

10. **Define service ports and env vars**

    | Service | Port | DB Port |
    |---------|------|---------|
    | Restaurant Service | 8081 | 5432 |
    | Menu Service | 8082 | 5433 |
    | Discovery Server | 8761 | -- |
    | API Gateway | 8080 | -- |
    | Kafka Broker | 9092 | -- |

11. **Verify both services start** (`mvn spring-boot:run`) with `/ping` endpoints

12. **Set up Postman workspace** with QuickBite collection and environment variables

#### Definition of Done
- [ ] OpenAPI-ready endpoint list exists for both services
- [ ] Database schemas defined
- [ ] Validation rules documented
- [ ] Both Spring Boot apps start without errors
- [ ] Both PostgreSQL databases running via Docker Compose
- [ ] `/ping` returns 200 on both services
- [ ] Postman collection exists
- [ ] Sierra-Lima can begin implementation without waiting for Order, Payment, or Delivery teams

---

### Phase 2 -- Restaurant Service: Foundation

**Goal:** The Restaurant entity is persisted in PostgreSQL with basic CRUD operations
working end-to-end. Another service can reliably ask "Is restaurant X open?" and
get a correct answer.

#### Tasks

1. **Define the domain model**
   - `Restaurant` entity (JPA `@Entity`):
     - `restaurantId` (UUID, `@Id`, `@GeneratedValue`)
     - `ownerId` (UUID -- cross-service reference to User Service)
     - `name` (String, not blank)
     - `address` (String)
     - `city` (String)
     - `latitude` (Double)
     - `longitude` (Double)
     - `operatingHours` (String -- e.g. "09:00-22:00")
     - `isOpen` (Boolean, default false)
     - `createdAt` (LocalDateTime)
     - `updatedAt` (LocalDateTime)
   - Consider embedding `Location` as a `@Embeddable` value object

2. **Repository layer**
   - `RestaurantRepository extends JpaRepository<Restaurant, UUID>`
   - Derived queries: `findByCity(String city)`, `findByIsOpenTrue()`

3. **Service layer**
   - `RestaurantService` class with methods:
     - `createRestaurant(dto)` -- validates and saves
     - `getRestaurantById(id)` -- returns or throws 404
     - `getAllRestaurants()` -- returns list
     - `updateRestaurant(id, dto)` -- partial update
     - `deleteRestaurant(id)` -- soft or hard delete

4. **Controller layer**
   - `RestaurantController` with `@RestController` and `@RequestMapping("/api/restaurants")`
   - Implement basic CRUD endpoints

5. **DTOs**
   - `CreateRestaurantRequest`
   - `UpdateRestaurantRequest`
   - `RestaurantResponse`

6. **Test with Postman** -- POST, GET (all), GET (by id), PUT, DELETE

#### Definition of Done
- [ ] Restaurant table auto-created in PostgreSQL on startup
- [ ] All 5 CRUD operations work via Postman
- [ ] Data persists across service restarts
- [ ] Postman collection updated with all requests

---

### Phase 3 -- Restaurant Service: Full API & Swagger

**Goal:** Complete the Restaurant Service API with all required endpoints and
OpenAPI documentation.

#### Tasks

1. **Additional endpoints**
   - `PATCH /api/restaurants/{id}/status` -- toggle open/closed
   - `GET /api/restaurants/{id}/status` -- check if restaurant is open
   - `GET /api/restaurants?city={city}` -- filter by city
   - `GET /api/restaurants?isOpen=true` -- filter open restaurants

2. **Validation**
   - Bean Validation annotations (`@NotBlank`, `@NotNull`, `@Min`, `@Max`)
   - Global exception handler (`@ControllerAdvice`) for validation errors and 404s
   - Proper HTTP status codes (201 Created, 204 No Content, 400, 404)

3. **OpenAPI / Swagger**
   - Add `springdoc-openapi-starter-webmvc-ui` dependency
   - Annotate controller with `@Operation`, `@ApiResponses`
   - Verify Swagger UI at `http://localhost:8081/swagger-ui.html`

4. **Timestamps & auditing**
   - `@PrePersist` / `@PreUpdate` for `createdAt` / `updatedAt`
   - Or Spring Data JPA Auditing (`@EnableJpaAuditing`, `@CreatedDate`, `@LastModifiedDate`)

5. **CORS configuration** (`@CrossOrigin(origins = "*")` or global config bean)

#### Definition of Done
- [ ] 6-8 endpoints fully functional
- [ ] Validation errors return structured JSON responses
- [ ] Swagger UI renders all endpoints with schemas
- [ ] CORS headers present in responses
- [ ] Postman collection updated and all tests pass

---

### Phase 4 -- Menu Service: Foundation

**Goal:** The MenuItem entity is persisted in its own PostgreSQL database with basic
CRUD operations working end-to-end. Order Service can ask for item validity and
pricing without needing direct DB access.

#### Tasks

1. **Define the domain model**
   - `MenuItem` entity (JPA `@Entity`):
     - `menuItemId` (UUID, `@Id`, `@GeneratedValue`)
     - `restaurantId` (UUID -- cross-service reference)
     - `name` (String, not blank)
     - `description` (String)
     - `amount` (BigDecimal -- price amount)
     - `currency` (String, default "EUR" -- part of Price value object)
     - `category` (String -- e.g. "Appetizer", "Main", "Dessert", "Drink")
     - `isAvailable` (Boolean, default true)
     - `createdAt` (LocalDateTime)
     - `updatedAt` (LocalDateTime)
   - Consider embedding `Price` as `@Embeddable`

2. **Repository layer**
   - `MenuItemRepository extends JpaRepository<MenuItem, UUID>`
   - Derived queries: `findByRestaurantId(UUID)`, `findByRestaurantIdAndIsAvailableTrue(UUID)`, `findByCategory(String)`

3. **Service layer**
   - `MenuService` class with methods:
     - `addMenuItem(dto)` -- create new item
     - `getMenuItemById(id)` -- return or 404
     - `getMenuByRestaurantId(restaurantId)` -- list all items for a restaurant
     - `updateMenuItem(id, dto)` -- update item details
     - `deleteMenuItem(id)` -- remove item
   - Add a validation-oriented method for order placement (batch validate item IDs and prices)

4. **Controller layer** -- `MenuController` with `@RequestMapping("/api/menu-items")`

5. **DTOs** -- `CreateMenuItemRequest`, `UpdateMenuItemRequest`, `MenuItemResponse`

6. **Test with Postman** -- all CRUD + filter by restaurantId

#### Definition of Done
- [ ] MenuItem table auto-created in its own PostgreSQL database
- [ ] All 5 CRUD operations work via Postman
- [ ] Menu items correctly filtered by restaurantId
- [ ] Data persists across restarts

---

### Phase 5 -- Menu Service: Full API & Swagger

**Goal:** Complete the Menu Service API with all required endpoints, validation,
and OpenAPI documentation.

#### Tasks

1. **Additional endpoints**
   - `GET /api/menu-items?restaurantId={id}&available=true` -- browse available items
   - `PATCH /api/menu-items/{id}/availability` -- toggle availability
   - `GET /api/menu-items/categories?restaurantId={id}` -- list categories for a restaurant

2. **Validation & error handling**
   - Bean Validation (`@Positive` for price, `@NotBlank` for name)
   - Global exception handler (extract shared pattern from Restaurant Service)
   - Proper HTTP status codes

3. **OpenAPI / Swagger** -- verify at `http://localhost:8082/swagger-ui.html`

4. **CORS configuration**

#### Definition of Done
- [ ] 6-8 endpoints fully functional
- [ ] Swagger UI renders all Menu Service endpoints
- [ ] Validation works correctly
- [ ] Postman collection covers all endpoints

---

### Phase 6 -- Sierra-Lima Hardening Pass

**Goal:** Make both Sierra-Lima services demo-grade rather than just coded. After
this phase, the services can be shown independently during the project
consultation even if the rest of the team is not ready yet.

#### Tasks

1. **Standardise error responses** across both services (consistent JSON error envelope)
2. **Review and tighten request validation** and bad-input handling
3. **Verify timestamps/auditing** (`createdAt`, `updatedAt`) work correctly
4. **Add seed data** -- `data.sql` or `CommandLineRunner` per service:
   - 4-6 demo restaurants with realistic names and locations
   - 12-18 menu items spread across those restaurants
5. **Add controller-level and service-level tests** for critical paths
6. **Refresh the shared Postman collection** with all Restaurant and Menu requests
7. **Ensure both services respect Assignment 1 feedback:**
   - Infrastructure not mixed into business diagrams
   - Databases are service-local

#### Definition of Done
- [ ] Both services return consistent, structured error responses
- [ ] Seed data loads automatically on startup
- [ ] Tests pass for critical CRUD and validation paths
- [ ] Postman collection is complete and shareable
- [ ] Sierra-Lima services can be demonstrated independently

---

### Phase 7 -- Dockerise Both Services

**Goal:** Both services and their databases run entirely inside Docker containers,
orchestrated by Docker Compose.

#### Tasks

1. **Write Dockerfiles** (multi-stage build per service):
   ```
   Stage 1: maven:3.9-eclipse-temurin-17 -- build the JAR
   Stage 2: eclipse-temurin:17-jre -- run the JAR
   ```

2. **Extend Docker Compose** (`services/local-dev/docker-compose.yml` or top-level):
   - `restaurant-db` (PostgreSQL)
   - `menu-db` (PostgreSQL)
   - `restaurant-service` (depends on restaurant-db)
   - `menu-service` (depends on menu-db)
   - Docker network for service-to-service communication
   - Environment variables for DB connection strings

3. **Spring profiles**
   - `application-docker.properties` for each service with Docker-internal hostnames
   - Activate via `SPRING_PROFILES_ACTIVE=docker`

4. **Test the full stack**
   - `docker compose up --build`
   - Verify all endpoints via Postman
   - Verify data persists in volumes

5. **Add `.dockerignore`** per service (exclude `target/`, `.idea/`, etc.)

#### Definition of Done
- [ ] `docker compose up --build` starts everything from scratch
- [ ] Both services are reachable and functional
- [ ] Databases have persistent volumes
- [ ] No shared database -- each service has its own container
- [ ] `docker compose down` stops everything cleanly

---

### Phase 8 -- Inter-Service Communication (Synchronous REST)

**Goal:** Menu Service calls Restaurant Service via REST (WebClient) to validate
that a restaurant exists before creating/returning menu items. This is the
synchronous interaction needed for W1.

#### Tasks

1. **Add WebClient dependency** -- `spring-boot-starter-webflux` in Menu Service

2. **Create a `RestaurantClient` in Menu Service**
   - Calls `GET /api/restaurants/{id}` on Restaurant Service
   - Returns simplified DTO (`RestaurantBasicInfo`: id, name, isOpen)
   - Handles errors: 404 from Restaurant Service -> meaningful error to caller

3. **Integrate into Menu Service business logic**
   - When creating a menu item: validate `restaurantId` exists
   - When browsing a menu: optionally enrich response with restaurant name/status
   - Return 400/404 if restaurant doesn't exist

4. **Configuration**
   - Externalize base URL: `restaurant-service.url=http://localhost:8081`
   - Docker override: `restaurant-service.url=http://restaurant-service:8081`

5. **Test the integration** -- both locally and in Docker Compose

#### Definition of Done
- [ ] Menu Service calls Restaurant Service synchronously
- [ ] Invalid restaurantId returns proper error
- [ ] Works both locally and in Docker Compose
- [ ] Integration documented in Postman collection

---

### Phase 9 -- Service Discovery & API Gateway

**Goal:** Set up Eureka for service discovery and Spring Cloud Gateway as a single
entry point. This may count as Sierra-Lima's optional integration component.

#### Tasks

1. **Eureka Server**
   - New project: `services/discovery-server/`
   - Dependencies: `spring-cloud-starter-netflix-eureka-server`
   - `@EnableEurekaServer`, port 8761

2. **Register services as Eureka clients**
   - Add `spring-cloud-starter-netflix-eureka-client` to both services
   - Set `spring.application.name` for each
   - Verify both appear in Eureka dashboard

3. **API Gateway**
   - New project: `services/api-gateway/`
   - Dependencies: `spring-cloud-starter-gateway`, `spring-cloud-starter-netflix-eureka-client`
   - Route config:
     ```yaml
     spring.cloud.gateway.routes:
       - id: restaurant-service
         uri: lb://restaurant-service
         predicates: Path=/api/restaurants/**
       - id: menu-service
         uri: lb://menu-service
         predicates: Path=/api/menu-items/**
     ```

4. **Update WebClient for discovery**
   - `@LoadBalanced` WebClient builder in Menu Service
   - URL becomes `http://restaurant-service/api/restaurants/{id}`

5. **Add to Docker Compose** -- discovery-server and api-gateway containers

6. **Test through gateway** -- all requests via `http://localhost:8080/api/...`

#### Definition of Done
- [ ] Eureka dashboard shows all registered services
- [ ] API Gateway routes requests to correct services
- [ ] `@LoadBalanced` WebClient resolves service names
- [ ] Works in Docker Compose
- [ ] Postman collection updated with gateway-routed requests

---

### Phase 10 -- Event-Driven Integration with Kafka

**Goal:** Implement asynchronous communication using Apache Kafka. This provides
the async interaction style required by Assignment 3 and underpins W2/W3.

#### Tasks

1. **Add Kafka to Docker Compose**
   - Zookeeper container (confluentinc/cp-zookeeper:7.3.2)
   - Kafka broker container (confluentinc/cp-kafka:7.3.2)
   - Configure broker listeners for Docker network + external localhost

2. **Define Kafka topics**
   - `restaurant-status-events` -- published when a restaurant opens/closes
   - `menu-item-events` -- published when menu items are added/updated/removed
   - Create topics via `KafkaTopicConfiguration` beans

3. **Restaurant Service as producer**
   - Add `spring-kafka` dependency
   - When restaurant status changes (open/closed), publish event:
     ```json
     {
       "eventType": "RESTAURANT_STATUS_CHANGED",
       "restaurantId": "uuid",
       "isOpen": true,
       "timestamp": "2026-..."
     }
     ```
   - `KafkaTemplate<String, RestaurantEvent>` with JSON serializer

4. **Menu Service as consumer**
   - `@KafkaListener` on `restaurant-status-events` topic
   - When a restaurant closes, optionally mark all its menu items as unavailable
   - Log received events for demonstration

5. **Menu Service as producer (optional)**
   - Publish events when menu items change (useful for Order Service)

6. **Test the flow**
   - Change restaurant status via REST
   - Verify event via `kafka-console-consumer`
   - Verify Menu Service receives and processes the event

#### Definition of Done
- [ ] Kafka + Zookeeper running in Docker Compose
- [ ] Restaurant Service publishes status events to Kafka
- [ ] Menu Service consumes events and reacts
- [ ] Events visible via kafka-console-consumer
- [ ] End-to-end async flow demonstrated

---

### Phase 11 -- Backend Workflow W1 Assembly & Resilience

**Goal:** Get the core synchronous business flow (W1) working end-to-end, with
resilience patterns protecting inter-service calls.

#### Tasks -- W1 Assembly

1. **Integrate API Gateway** with routes for all backend services
2. **Wire the W1 happy path** (requires coordination with teammates):
   - Client submits order
   - Order Service checks restaurant availability (Restaurant Service)
   - Order Service validates menu items (Menu Service)
   - Order Service creates order
   - Payment Service processes payment
   - Delivery Service creates delivery task
3. **Confirm compensating behaviour** if downstream calls fail
4. **Keep logs and demo data readable** for checkpoint explanation

#### Tasks -- Resilience

5. **Add Resilience4j to Menu Service**
   - `spring-cloud-starter-circuitbreaker-resilience4j`
   - `spring-boot-starter-actuator`
   - `spring-boot-starter-aop`

6. **Circuit Breaker** on `RestaurantClient`:
   - `@CircuitBreaker(name = "restaurantService", fallbackMethod = "...")`
   - Fallback returns default/cached response or clear error
   - Monitor via `/actuator/health`

7. **Retry** -- `@Retry(name = "restaurantService")` with configured attempts/wait

8. **Time Limiter** -- `@TimeLimiter` to prevent hanging calls

9. **Test resilience** -- stop Restaurant Service, verify graceful degradation

#### Definition of Done
- [ ] W1 happy path works through the gateway (or documented service calls)
- [ ] Circuit breaker protects inter-service calls
- [ ] Fallback responses work when Restaurant Service is down
- [ ] Actuator health endpoint shows circuit breaker state
- [ ] Known failure paths documented

---

### Phase 12 -- Backend Polish & Checkpoint #1 Prep

**Goal:** Package the backend into something that survives a live Checkpoint #1
demonstration (2026-05-05).

#### Tasks

1. **Code review & cleanup**
   - Consistent naming conventions
   - Remove debug code, TODOs, commented-out blocks
   - Proper logging (SLF4J) in service and client layers

2. **Verify seed data** -- 4-6 restaurants, 12-18 menu items, demo-ready

3. **Finalise Postman collection**
   - Folders: Restaurant CRUD, Menu CRUD, Integration, Kafka, Resilience
   - Include example requests and expected responses

4. **Docker Compose full stack verification**
   - `docker compose up --build` from scratch
   - Run through entire Postman collection
   - Verify: PostgreSQL (x2), Eureka, API Gateway, Kafka, Restaurant Service, Menu Service

5. **Add a smoke-test script** for the main backend flow

6. **Prepare Checkpoint #1 talking points:**
   - Which services are implemented and which are design-only
   - Architecture diagram (business services only, no infra mixed in)
   - Why no shared DB
   - Why W1 is synchronous; where async messaging appears
   - API overview (Swagger links)
   - Demo script: create restaurant -> add menu items -> change status -> observe Kafka event -> resilience demo

7. **Coordinate with team**
   - Other services at least have stubs
   - Agree on shared Docker Compose for the full system
   - Test cross-team API calls if possible

8. **Update report draft** with backend architecture and workflow diagrams

#### Definition of Done
- [ ] Both services fully functional with all endpoints
- [ ] Full Docker Compose stack starts and works
- [ ] Seed data loads automatically
- [ ] Postman collection complete
- [ ] Demo script rehearsed
- [ ] The team can demonstrate backend-only integration with minimal manual setup

---

### Phase 13 -- Vue.js Frontend: Shell & Routing

**Goal:** Create the minimum viable Vue.js frontend with navigation, routing,
and API client utilities ready for feature views.

#### Tasks

1. **Scaffold Vue.js project**
   - `vue create quickbite-frontend` (Vue 3, Router, Babel)
   - Place under `services/frontend/`
   - `npm install` and verify `npm run serve` opens `http://localhost:8080`

2. **Configure API base URL**
   - Config/constants file pointing to API Gateway
   - Environment variable: `VUE_APP_API_BASE_URL=http://localhost:8080`

3. **Define route map and page layout**
   - Navigation bar: Home, Restaurants, Menu Items
   - Placeholder views for: restaurant list, restaurant detail/menu, cart/order, order status
   - Shared layout components (header, footer, loading spinner)

4. **Create shared API client utility**
   - Reusable `fetch()` wrapper with base URL, content-type headers, error handling
   - Will later add JWT token attachment (Phase 17)

5. **Decide basic state approach** (local component state is fine; Vuex/Pinia only if needed)

#### Definition of Done
- [ ] Frontend starts and navigates between pages
- [ ] Shared API client utility works
- [ ] Route map and placeholder views in place
- [ ] Environment config for gateway-based API access

---

### Phase 14 -- Vue.js Frontend: Restaurant & Menu UX

**Goal:** Expose Sierra-Lima's backend work visibly in the user-facing app. A demo
user can discover a restaurant, open it, and view orderable menu items.

#### Tasks

1. **Restaurant views**
   - `RestaurantList.vue` -- list all restaurants (GET `/api/restaurants`)
     - Table/card layout: name, city, status (open/closed)
     - Link to individual restaurant page
     - "Add Restaurant" button
   - `AddRestaurant.vue` -- form (POST)
     - Fields: name, address, city, latitude, longitude, operating hours
     - `v-model` bindings, form validation
     - Submit -> redirect to list
   - `RestaurantDetail.vue` -- view/edit/delete
     - Fetch by ID (`this.$route.params.id`)
     - Edit form (PUT)
     - Toggle open/closed (PATCH)
     - Delete with confirmation
     - Link to "View Menu" for this restaurant

2. **Menu item views**
   - `MenuItemList.vue` -- list items (filterable by restaurant)
     - Dropdown or route param to select restaurant
     - Table: name, category, price, availability
   - `AddMenuItem.vue` -- form (POST)
     - Restaurant dropdown populated from backend
   - `MenuItemDetail.vue` -- view/edit/delete
     - Toggle availability (PATCH)

3. **Router additions**
   - `/restaurants`, `/restaurants/new`, `/restaurants/:id`
   - `/menu-items`, `/menu-items/new`, `/menu-items/:id`
   - `/restaurants/:id/menu` -- filtered menu view

4. **UI polish**
   - Loading, empty, and error states
   - Consistent styling
   - Ensure the UI consumes live APIs, not hardcoded mocks

#### Definition of Done
- [ ] Restaurant CRUD works from the browser
- [ ] Menu item CRUD works from the browser
- [ ] Restaurant dropdown populated dynamically
- [ ] Linked navigation between restaurant and menu views
- [ ] Error states handled gracefully

---

### Phase 15 -- Frontend-Backend Integration & Checkpoint #2 Prep

**Goal:** Full-stack app works end-to-end through the API Gateway. Demonstrable
for Checkpoint #2 (2026-05-12).

#### Tasks

1. **API Gateway CORS** -- verify frontend -> gateway -> services flow

2. **Frontend via Docker (recommended)**
   - Dockerfile: multi-stage (node build -> nginx serve)
   - Add to Docker Compose
   - Configure nginx to proxy API requests to gateway

3. **End-to-end testing** (full user flow in browser):
   1. View restaurant list
   2. Create a new restaurant
   3. Add menu items to it
   4. Toggle restaurant status
   5. See menu items update (via Kafka event)
   6. Edit and delete operations

4. **Connect order placement flow** (if Order Service from teammate is ready):
   - Cart submission -> W1 path
   - Order status page

5. **Prepare Checkpoint #2 demo**
   - Demo script covering frontend + backend
   - Screenshots/recording as backup
   - What is still pending for final security phase

#### Definition of Done
- [ ] Frontend communicates with backend through API Gateway
- [ ] Full CRUD workflow works in the browser
- [ ] Docker Compose runs entire stack including frontend
- [ ] One person can demo restaurant discovery through order creation in a single run

---

### Phase 16 -- Security: Spring Security & JWT

**Goal:** Secure backend services with Spring Security and JWT-based authentication.
Cover the "security" expectation before the final checkpoint.

#### Tasks

1. **Auth Service (if owned by Sierra-Lima) or integration with team's Auth Service**
   - If Sierra-Lima takes this as the integration component:
     - New project `services/auth-service/`
     - User registration (`POST /api/auth/signup`) with role assignment (CUSTOMER, RESTAURANT_OWNER, ADMIN)
     - Login (`POST /api/auth/login`) returning JWT token
     - Token validation (`GET /api/auth/authenticate`)
     - JPA-based user storage in its own PostgreSQL database
     - JWT components: `JwtService`, `JwtAuthFilter`, `SecurityConfig`, `MyUserDetailsService`
   - If Auth Service is owned by another team member:
     - Integrate with their JWT validation logic

2. **Secure Restaurant Service endpoints**
   - Add `spring-boot-starter-security`
   - `SecurityConfig` with `SecurityFilterChain`:
     - Public: `GET /api/restaurants/**` (browsing)
     - Authenticated: `POST`, `PUT`, `PATCH`, `DELETE` on `/api/restaurants/**`
     - Role-based: Only restaurant owners (or ADMIN) can modify
   - `JwtAuthFilter` -- extract and validate tokens from `Authorization: Bearer <token>`
   - Stateless session management (`SessionCreationPolicy.STATELESS`)

3. **Secure Menu Service endpoints** -- same pattern (public GETs, authenticated mutations)

4. **CSRF/CORS adjustments** -- disable CSRF (stateless REST API), maintain CORS

5. **Test with Postman**
   - Login -> get token -> use token -> access protected endpoints
   - 401 for unauthenticated, 403 for wrong role

#### Definition of Done
- [ ] Protected endpoints return 401 without valid JWT
- [ ] Valid JWT grants access to protected endpoints
- [ ] Role-based access works (ADMIN vs USER vs RESTAURANT_OWNER)
- [ ] Postman collection updated with auth flow

---

### Phase 17 -- Secure Full-Stack Integration

**Goal:** JWT authentication works in the Vue.js frontend. Users log in before
accessing protected functionality.

#### Tasks

1. **Login and Signup views**
   - `Login.vue` -- POST `/api/auth/login`, store JWT in localStorage, decode role, redirect
   - `Signup.vue` -- POST `/api/auth/signup`, redirect to login

2. **Auth utility (`auth.js`)**
   - `isAuthenticated()` -- check JWT in localStorage
   - `getToken()` -- retrieve token
   - `getRole()` -- extract role from JWT
   - `logout()` -- remove token, redirect to login

3. **Protect Vue routes**
   - Router `beforeEnter` guard: redirect to `/login` if unauthenticated
   - Show/hide UI elements based on role

4. **Attach JWT to API calls**
   - Update shared fetch wrapper to include `Authorization: Bearer <token>` header

5. **Logout** -- button in nav bar, clears token

6. **Remove checkpoint-era security shortcuts** that would look weak in the final demo

#### Definition of Done
- [ ] Login/signup flow works in browser
- [ ] JWT stored and sent with API requests
- [ ] Protected routes require authentication
- [ ] Role-based UI rendering works
- [ ] Logout clears session
- [ ] Unauthorized access is visibly blocked

---

### Phase 18 -- Report & Evidence Pack

**Goal:** Finish the written deliverable before the final presentation crunch. If
the code froze tomorrow, the report would still be mostly ready.

#### Tasks

1. **Assemble report sections:**
   - Business architecture (from Assignments 1-2)
   - Technical architecture (actual implementation)
   - Implemented services vs design-only services
   - Data models (ER diagrams matching actual code)
   - APIs (endpoint tables, Swagger screenshots)
   - Workflows (W1 synchronous, W2/W3 asynchronous)
   - Integration mechanisms (REST, Kafka, API Gateway)
   - Security approach (JWT flow)
   - Team responsibilities
   - Limitations and future work

2. **Refresh diagrams** to match the actual code, not just the old design

3. **Add screenshots, endpoint tables, topic tables, and demo notes**

4. **Verify the report explains** where design-only services still exist

5. **Proofread and format** for submission

#### Definition of Done
- [ ] All report sections drafted
- [ ] Diagrams match implemented system
- [ ] Evidence (screenshots, tables) included
- [ ] Report is near-final quality

---

### Phase 19 -- Final Presentation Rehearsal & Buffer

**Goal:** Turn a working system into a convincing presentation. Use remaining time
for stabilisation only, not feature invention.

#### Tasks -- Presentation Prep

1. **Create slide deck** covering:
   - System overview and architecture diagram
   - Services implemented by Sierra-Lima (Restaurant + Menu)
   - API design (Swagger screenshots)
   - Sync communication (Menu -> Restaurant via REST, W1)
   - Async communication (Kafka events, W2/W3)
   - Resilience (circuit breaker demo)
   - Frontend demo
   - Security (JWT flow)

2. **Define live demo script** with exact click-path and expected outputs

3. **Assign speaking parts** with team:
   - Architecture overview
   - Sierra-Lima services
   - Synchronous flow
   - Asynchronous flow
   - Security

4. **Prepare fallback materials:**
   - Screenshots
   - Pre-recorded flow
   - Backup seed data
   - Recovery commands if demo breaks

5. **Rehearse timeboxed answers** to likely questions:
   - Why were these services implemented?
   - Why are some services design-only?
   - How were service boundaries chosen?
   - How does async integration work?
   - How was security added?

#### Tasks -- Buffer (Stabilisation)

6. **Fix highest-risk bugs only** -- no new features
7. **Re-run smoke tests**
8. **Verify seeded demo users, restaurants, menu items, and order flow**
9. **Verify startup from clean environment** (`docker compose up --build` from scratch)
10. **Freeze the branch** for presentation

#### Definition of Done
- [ ] Full system starts and runs in Docker Compose
- [ ] All workflows demonstrable end-to-end
- [ ] Presentation slides complete
- [ ] Live demo rehearsed at least once
- [ ] Every presenter knows what to say, click, and do if the demo misbehaves
- [ ] No unresolved defect remains that could break the main demo narrative
- [ ] Code is clean and committed

---

## 8. Checkpoint Readiness Gates

Use these as go/no-go criteria before each checkpoint.

### Checkpoint #1 Gate (2026-05-05 -- Backend)

The project is not ready unless ALL of these are true:

- [ ] At least the main implemented backend services compile and run
- [ ] Restaurant Service and Menu Service are fully operational
- [ ] W1 happy path works through the gateway or through documented service calls
- [ ] Databases are separated per service
- [ ] Backend can be started from a documented local process
- [ ] Architecture and workflow diagrams match reality

### Checkpoint #2 Gate (2026-05-12 -- Frontend + Backend)

All CP#1 criteria still hold, plus:

- [ ] Frontend uses live backend APIs (not hardcoded mocks)
- [ ] A user can browse restaurants, inspect menus, and (ideally) place an order
- [ ] UI handles at least basic loading and error states
- [ ] Demo does not rely on hidden manual DB manipulation

### Checkpoint #3 Gate (2026-05-19 -- Final Presentation)

All CP#2 criteria still hold, plus:

- [ ] A security mechanism is visible in the real system
- [ ] At least one asynchronous workflow is demonstrably working
- [ ] The report is presentation-ready
- [ ] The team has a demo fallback plan

---

## 9. What Sierra-Lima Can Safely Do Early, Even Alone

These are the highest-value early-start tasks that don't require teammates:

1. Complete Phases 0 through 7 without waiting for anyone
2. Produce the canonical Restaurant Service and Menu Service API contracts
3. Seed compelling restaurant and menu demo data
4. Make Menu Service validation solid (Order Service will depend on it directly)
5. Keep both services documented through OpenAPI and Postman
6. Prepare lightweight integration stubs and sample payloads for the Order team
7. Set up Docker Compose and Kafka infrastructure for the whole team

If the rest of the team arrives late, Sierra-Lima's work still remains useful because it becomes the most stable part of the system.

**Stubbing strategy while solo:**
- Use hardcoded UUIDs for `ownerId` (User Service not yet available)
- Skip Order Service validation calls (Order Service not yet available)
- Test Kafka events using `kafka-console-consumer` as a stand-in consumer
- Use seeded demo data and documented assumptions; replace stubs once real services arrive

---

## 10. Final Demo Success Criteria

By final-presentation-ready status, the team should be able to demonstrate:

1. A customer browsing restaurants and menus
2. Creation of an order through the frontend
3. Backend coordination across multiple services (W1)
4. At least one event-driven update or notification (W2 or W3)
5. At least one secured route or authenticated flow
6. Clear service boundaries and separate data ownership
7. A report and diagrams that match the implemented system

---

## 11. Bottom Line

The safest route from this repository's current scaffolded state to a credible final project is:

1. **Build Sierra-Lima's two services first** and build them properly
2. **Use them as stable anchors** for the rest of the team
3. **Reach backend integration** by 2026-05-05
4. **Layer in frontend** by 2026-05-12
5. **Finish security, async polish, report, and presentation readiness** by 2026-05-19

If the team follows the phase order above, early solo work will not be wasted, and the project will remain aligned with the course assignments, practicals, and checkpoint structure.

---

## Appendix A -- Suggested Session Calendar

Assuming ~4 sessions per week starting 2026-04-18:

| Session | Date (approx.) | Phase | Notes |
|---------|---------------|-------|-------|
| 1 | Apr 18 (Fri) | Phase 0 | Scope freeze and conventions |
| 2 | Apr 19 (Sat) | Phase 1 | Contract pack & environment bootstrap |
| 3 | Apr 21 (Mon) | Phase 2 | Restaurant Service foundation |
| 4 | Apr 22 (Tue) | Phase 3 | Restaurant full API & Swagger |
| 5 | Apr 24 (Thu) | Phase 4 | Menu Service foundation |
| 6 | Apr 25 (Fri) | Phase 5 | Menu full API & Swagger |
| 7 | Apr 26 (Sat) | Phase 6 | Sierra-Lima hardening pass |
| 8 | Apr 27 (Sun) | Phase 7 | Dockerise both services |
| -- | Apr 28 (Tue) | -- | *Project consultation session* |
| 9 | Apr 29 (Wed) | Phase 8 | Sync inter-service communication |
| 10 | Apr 30 (Thu) | Phase 9 | Discovery & API Gateway |
| 11 | May 01 (Fri) | Phase 10 | Kafka integration |
| 12 | May 02 (Sat) | Phase 11 | W1 assembly & resilience |
| 13 | May 04 (Sun) | Phase 12 | Backend polish & CP#1 prep |
| **--** | **May 05 (Mon)** | **--** | ***Checkpoint #1 (Backend)*** |
| 14 | May 07 (Wed) | Phase 13 | Vue.js frontend shell |
| 15 | May 09 (Fri) | Phase 14 | Restaurant & menu UX |
| 16 | May 11 (Sun) | Phase 15 | Frontend integration & CP#2 prep |
| **--** | **May 12 (Mon)** | **--** | ***Checkpoint #2 (Frontend + Backend)*** |
| 17 | May 13 (Tue) | Phase 16 | Security & JWT |
| 18 | May 14 (Wed) | Phase 17 | Secure full-stack |
| 19 | May 16 (Fri) | Phase 18 | Report & evidence pack |
| 20 | May 18 (Sun) | Phase 19 | Final rehearsal & buffer |
| **--** | **May 19 (Mon)** | **--** | ***Checkpoint #3 (Final Presentation)*** |

### Compression Guidance

If any phase slips, compress in this priority order (sacrifice optional extras first):

1. **Drop first:** Rate Limiter (Phase 11), Menu-as-Kafka-producer (Phase 10), Eureka/discovery if static config works
2. **Merge second:** Phases 13+14 (frontend shell + UX in one session if Vue experience exists), Phases 16+17 (security backend + frontend if scope is thin JWT)
3. **Never compress:** W1 assembly, core Sierra-Lima service work, Checkpoint prep phases, security pass

---

## Appendix B -- Team Coordination Points

| When | What must be agreed | Who |
|------|---------------------|-----|
| Before Phase 1 | Package structure, Java version, build tool, naming rules | All |
| Before Phase 8 | Endpoint paths, request/response schemas, ownership boundaries | All |
| Before Phase 9 | Gateway routing assumptions and W1 sequence | Backend owners |
| Before Phase 10 | Kafka topic names, event envelope, consumer expectations | Backend owners |
| Before Phase 11 | W1 end-to-end wiring order | Backend owners |
| Before Phase 16 | JWT approach, protected routes, demo user strategy | All |
| Before Phase 19 | Slide structure, demo path, fallback plan, speaking order | All |

When teammates are late, Sierra-Lima can still move by using stable IDs, seeded demo data, documented assumptions, and contract-first stubs -- then replacing those stubs once real services arrive.

---

## Appendix C -- Risk Register

| Risk | Impact | Mitigation |
|------|--------|------------|
| Teammates start late | Integration delays at checkpoints | Front-load Sierra-Lima services and interface definitions; use contract-first coordination; Phases 0-7 are solo-capable |
| Assignment 3 design changes after feedback | Rework APIs/models | Keep services small and focused; design for easy change |
| Scope expands back to all 8 services | Time crunch | Hold the Assignment 3 implementation subset unless instructor explicitly asks for more; prefer quality over service count |
| Docker/Kafka setup issues on Windows | Blocks multiple phases | Allocate extra time in Phases 7/10; WSL2 as fallback |
| Scope creep in domain model | Over-engineering | Stick to 1-2 entities per service, 5-8 endpoints |
| Spring Cloud version conflicts | Build errors | Pin Spring Cloud BOM version; use compatible Spring Boot version |
| **Shared database shortcut during integration** | **Violates architecture; repeat of A1 penalty** | **Reject immediately. Keep cross-service references as IDs only. Treat separate persistence as a constraint, not a suggestion.** |
| Security postponed too long | Rushed for CP#3 | Reserve Phase 16 specifically; keep auth assumptions abstract early so JWT inserts cleanly |
| Diagrams and report fall behind code | Last-minute scramble | Phase 18 is dedicated to report; update diagrams at each checkpoint phase, not just at the end |
| Checkpoint demo fails live | Lost marks | Prepare screenshots/recording as backup; rehearse with Docker; Phase 19 buffer session |

---

## Appendix D -- Assignment Feedback to Address

From Assignment 1 feedback (3.50/4.00):

> **-0.25**: Infrastructure elements mixed into architecture diagram.  
> **-0.25**: Shared database across microservices.

**Actions taken in this project:**
- Architecture diagrams will show business services only (no API Gateway, DB, Kafka in logical diagrams)
- Each microservice has its **own PostgreSQL database** -- enforced in Docker Compose with separate containers
- Technical infrastructure (gateway, discovery, messaging) treated as implementation details, not business architecture