# Golf-Papa-Tango Project Phases Plan (`2ce188a`)

## 1. Purpose

This plan breaks the QuickBite ESI project into many 3-hour working sessions so the repository can move from the current empty scaffold to checkpoint-ready and final-presentation-ready status.

It is grounded in:

- the 2025/26 ESI course page and the 2026 practicals sequence
- Assignments 1, 2, and 3 already stored in this repository
- the current repository state, which contains only documentation scaffolding plus empty `restaurant-service`, `menu-service`, and `local-dev` folders
- the fact that Sierra-Lima is responsible for `Restaurant Service` and `Menu Service`

## 2. Fixed Course Anchors

Use these dates as non-negotiable milestones:

- `2026-04-21`: Project description practical
- `2026-04-28`: Project consultation practical
- `2026-05-05`: Project checkpoint #1, backend
- `2026-05-12`: Project checkpoint #2, frontend + backend
- `2026-05-19`: Project checkpoint #3, final presentation, frontend + backend + security

Note: the practicals page shows `28/04/2025` for project consultation, but in the 2026 schedule context that is almost certainly a typo; this plan treats the consultation as `2026-04-28`.

## 3. Baseline Project Scope

The assignments define the intended system clearly enough to avoid scope drift.

### 3.1 Business system

QuickBite is a food-delivery platform with these eight business services in the overall architecture:

1. `User Service`
2. `Restaurant Service`
3. `Menu Service`
4. `Order Service`
5. `Payment Service`
6. `Delivery Service`
7. `Notification Service`
8. `Review Service`

### 3.2 Planned implementation subset

Assignment 3 already narrowed the implementation scope to:

- implemented business services: `Order`, `Restaurant`, `Menu`, `Payment`, `Delivery`, `Notification`
- implemented shared components: `API Gateway`, `Kafka/Event Broker configuration`
- design-only services unless feedback forces a change: `User`, `Review`

### 3.3 Sierra-Lima ownership

Sierra-Lima owns:

- `Restaurant Service`
- `Menu Service`

That means Sierra-Lima can make meaningful early progress before the rest of the team becomes active.

### 3.4 Working technology baseline

The most likely implementation stack, based on the practical sessions and Assignment 3, is:

| Layer | Primary choice | Notes |
| --- | --- | --- |
| Backend | Spring Boot | Java + Maven-friendly setup aligned with practicals |
| Database | PostgreSQL | one database per service |
| Containerisation | Docker + Docker Compose | local reproducibility is mandatory |
| API gateway | Spring Cloud Gateway | already consistent with Assignment 3 |
| Async messaging | Apache Kafka | use Zookeeper only if the chosen image still requires it |
| Frontend | Vue.js 3 | router-enabled SPA |
| Security | Spring Security + JWT | required by final checkpoint expectations |
| API documentation | OpenAPI / Swagger | keep docs live from early service phases |
| API testing | Postman | maintain one shared collection for demos and regression checks |
| Service discovery | Eureka | optional, only if the team wants a stronger infra demo |
| Resilience | Resilience4j | optional, only after the core flows are stable |

### 3.5 Sierra-Lima domain recap

For Sierra-Lima's two services, the assignment-derived business core is already known and should not be reinvented during implementation.

**Restaurant Service**

- Aggregate root: `Restaurant`
- Key fields: `restaurantId`, `ownerId`, `name`, `location`, `operatingHours`, `isOpen`
- Value object: `Location`
- Repository: `RestaurantRepository`
- Main requirements: `R19`, `R20`

**Menu Service**

- Aggregate root: `MenuItem`
- Key fields: `menuItemId`, `restaurantId`, `name`, `description`, `price`, `category`, `isAvailable`
- Value object: `Price`
- Repository: `MenuRepository`
- Main requirements: `R21`, `R22`

## 4. Planning Principles

These principles should govern every phase:

1. Keep the project aligned with Assignments 1 to 3 rather than reinventing the system.
2. Keep business architecture separate from technical architecture.
3. Give every implemented microservice its own database. No shared database.
4. Prioritize the two required interaction styles:
   - synchronous REST workflow
   - asynchronous event-driven workflow
5. Keep each service small and demoable. Assignment 3 suggests roughly 5 to 8 endpoints per service.
6. Avoid infrastructure inflation. `Eureka`, client-side load balancing, and other extras should remain optional unless the instructor explicitly requests them.
7. Bias the early phases toward artifacts that are useful even if teammates join late:
   - API contracts
   - ER/data models
   - Docker Compose setup
   - seed data
   - service skeletons
   - OpenAPI docs
8. Treat security as mandatory for the final phase, even if the Assignment 3 design left `User Service` as design-only.

## 5. Delivery Strategy

Because the repository is currently almost empty and the team may mobilize late, the safest strategy is:

- first, freeze the scope and interfaces
- second, build Sierra-Lima's two services completely enough to become stable integration targets
- third, assemble backend workflow `W1` before checkpoint #1
- fourth, add frontend before checkpoint #2
- fifth, add security and presentation readiness before checkpoint #3

This ordering keeps early solo work reusable and reduces merge chaos later.

### Cross-phase working assets

These assets should be kept live throughout the project rather than created at the end:

- one shared Postman collection with environment variables and smoke requests
- Swagger/OpenAPI links for every implemented backend service
- a seeded demo dataset for restaurants, menu items, and core workflow entities
- one documented Docker Compose runbook for clean startup
- health-check or ping endpoints for quick verification
- screenshot or recording backups for each checkpoint demo

Keeping these artifacts current reduces integration risk and makes late team coordination less damaging.

## 6. Phase Overview

Each phase is designed as one focused 3-hour session.

| Phase | Target window | Main focus | Primary outcome |
| --- | --- | --- | --- |
| 01 | 2026-04-17 to 2026-04-18 | Scope freeze and repo conventions | Shared project baseline |
| 02 | 2026-04-18 to 2026-04-19 | Sierra-Lima contract pack | Stable Restaurant/Menu specs |
| 03 | 2026-04-19 to 2026-04-20 | Local development bootstrap | Docker Compose and service skeleton plan |
| 04 | 2026-04-20 to 2026-04-22 | Restaurant Service foundation | Working restaurant backend |
| 05 | 2026-04-22 to 2026-04-24 | Menu Service foundation | Working menu backend |
| 06 | 2026-04-24 to 2026-04-26 | Sierra-Lima hardening pass | OpenAPI, tests, container readiness |
| 07 | 2026-04-26 to 2026-04-28 | Team integration contract lock | Agreed inter-service contracts |
| 08 | 2026-04-28 to 2026-05-01 | Backend assembly for workflow W1 | Happy-path place-order flow |
| 09 | 2026-05-01 to 2026-05-04 | Checkpoint #1 preparation | Backend checkpoint bundle |
| 10 | 2026-05-05 to 2026-05-07 | Frontend shell | Vue app shell and routing |
| 11 | 2026-05-07 to 2026-05-09 | Restaurant and menu UX | Browse and discovery features |
| 12 | 2026-05-09 to 2026-05-11 | Order/status UX and checkpoint #2 prep | Frontend + backend integrated demo |
| 13 | 2026-05-12 to 2026-05-14 | Security implementation | JWT-based protected flows |
| 14 | 2026-05-14 to 2026-05-15 | Async polish and notifications | Stronger W2/W3 demo |
| 15 | 2026-05-15 to 2026-05-17 | Report and evidence pack | Near-final documentation set |
| 16 | 2026-05-17 to 2026-05-18 | Final presentation rehearsal | Final demo, slides, fallback plan |
| 17 | 2026-05-18 to 2026-05-19 | Buffer and final freeze | Last defects removed |

## 7. Detailed Phase Plan

### Phase 01 - Scope Freeze and Repo Conventions

**Goal**

Turn the assignment outputs into a single implementation baseline.

**Work**

- Reconfirm the project scope from Assignment 3:
  - implement `Order`, `Restaurant`, `Menu`, `Payment`, `Delivery`, `Notification`
  - implement `API Gateway` and `Kafka`
  - keep `User` and `Review` as design-only unless later feedback requires a change
- Freeze the canonical workflows:
  - `W1`: place order, synchronous
  - `W2`: delivery updates and notifications, asynchronous
  - `W3`: payment completed or failed notification, asynchronous complement
- Decide folder layout for the codebase that will eventually hold all services.
- Define branch, commit, naming, and environment-variable conventions.
- Record any open design questions in `dev-docs/decisions`.

**Outputs**

- One written baseline decision set
- One agreed folder structure
- One list of non-goals for the first implementation pass

**Definition of done**

- Nobody on the team should still be debating which services are in or out for the project core.

### Phase 02 - Sierra-Lima Contract Pack

**Goal**

Make `Restaurant Service` and `Menu Service` precise enough that other teammates can integrate against them later without blocking Sierra-Lima now.

**Work**

- Define final REST endpoints from Assignment 3 for `Restaurant Service` and `Menu Service`.
- Freeze request and response payloads.
- Freeze validation rules:
  - restaurant required fields
  - operating hours format
  - menu item availability rules
  - price representation
- Define database schema for both services.
- Create initial seed-data plan for demo restaurants and menu items.
- Write cross-service assumptions explicitly:
  - `MenuItem.restaurantId` references `Restaurant.restaurantId`
  - no cross-service joins
  - only ID references across service boundaries

**Outputs**

- OpenAPI-ready endpoint list
- ER-style data model for both services
- Validation checklist
- Seed-data shortlist

**Definition of done**

- Sierra-Lima can begin implementation without waiting for Order, Payment, or Delivery teams.

### Phase 03 - Local Development Bootstrap

**Goal**

Create the local runtime design before code multiplies.

**Work**

- Verify the local toolchain:
  - Java 17+ or 21
  - Maven
  - Docker Desktop and Docker Compose
  - Node.js and NPM
  - Vue CLI if the chosen frontend flow still uses it
  - Postman
- If the service code still does not exist, scaffold `Restaurant Service` and `Menu Service` from Spring Initializr or an equivalent team-approved template.
- Design `docker-compose` layout for:
  - PostgreSQL instances or schemas per service
  - Kafka broker
  - optional Zookeeper if the chosen Kafka image still needs it
  - API Gateway
  - frontend app later
- Define service ports and environment variables.
- Decide how local configuration will be shared:
  - `.env`
  - per-service `application.properties` or YAML
  - README runbook
- Decide how health checks and startup order will work.
- Standardize minimal service verification endpoints:
  - `/actuator/health` if actuator is added
  - or a simple `/ping` endpoint if a lighter setup is preferred
- Create the initial QuickBite Postman workspace or collection with environment variables for service and gateway base URLs.
- Prepare a minimal local-dev checklist based on the practical sessions:
  - Spring Boot
  - Postgres
  - Docker Compose
  - Kafka

**Outputs**

- A concrete local-runtime map
- Standardized port list
- Standardized environment-variable list

**Definition of done**

- The team has one agreed local setup target instead of each person improvising their own environment.

### Phase 04 - Restaurant Service Foundation

**Goal**

Implement the first Sierra-Lima-owned business service as a clean integration target.

**Work**

- Scaffold Spring Boot project for `Restaurant Service`.
- Add core dependencies:
  - Spring Web
  - Spring Data JPA
  - PostgreSQL driver
  - validation
  - Lombok if the team accepts it
  - springdoc/OpenAPI
- Implement:
  - create restaurant
  - get restaurant
  - update restaurant
  - patch open or closed status
  - list/search restaurants
  - check availability endpoint
- Add persistence and migration strategy.
- Add controller-level and service-level tests.
- Seed at least 4 to 6 restaurants that make the demo interesting.

**Outputs**

- Running `Restaurant Service`
- Persistent restaurant data
- Test-backed CRUD and availability API

**Definition of done**

- Another service can reliably ask, "Is restaurant X open?" and get a correct answer.

### Phase 05 - Menu Service Foundation

**Goal**

Implement the second Sierra-Lima-owned business service with enough realism for order placement.

**Work**

- Scaffold Spring Boot project for `Menu Service`.
- Implement:
  - add menu item
  - list menu items for restaurant
  - get menu item
  - update menu item
  - delete menu item
  - validate menu items for order placement
- Enforce `restaurantId` ownership rules.
- Add categories, price fields, availability flag, and realistic demo data.
- Add tests for menu validation, because `Order Service` will depend on this path directly.

**Outputs**

- Running `Menu Service`
- Demo menu catalogues for seeded restaurants
- A stable validation endpoint for the order workflow

**Definition of done**

- `Order Service` can ask for item validity and pricing without needing direct DB access.

### Phase 06 - Sierra-Lima Hardening Pass

**Goal**

Make both Sierra-Lima services demo-grade rather than just coded.

**Work**

- Add Swagger/OpenAPI exposure.
- Standardize error responses.
- Add request validation and clear bad-input handling.
- Add timestamps and auditing conventions such as `createdAt` and `updatedAt`.
- Add CORS configuration so the later Vue frontend does not inherit avoidable backend friction.
- Add Dockerfiles and local run instructions.
- Add `.dockerignore` files where needed so container builds stay clean.
- Add seed loading or migration scripts.
- Add smoke tests and sample requests.
- Publish or refresh the shared Postman collection with Restaurant and Menu requests.
- Ensure both services follow the Assignment 1 feedback rule:
  - infrastructure is not mixed into business diagrams
  - databases stay service-local

**Outputs**

- Demo-ready `Restaurant Service`
- Demo-ready `Menu Service`
- Better docs and lower integration risk

**Definition of done**

- Sierra-Lima services can be shown independently during consultation if the rest of the project is not ready yet.

### Phase 07 - Team Integration Contract Lock

**Goal**

Stop future arguments about how services should talk to each other.

**Work**

- Review every inter-service call from Assignment 3.
- Confirm request and response shapes for:
  - `Order -> Restaurant`
  - `Order -> Menu`
  - `Order -> Payment`
  - `Order -> Delivery`
- Confirm asynchronous events:
  - `payment.completed`
  - `payment.failed`
  - `delivery.status-changed`
  - optionally `order.cancelled`
- Confirm topic names, event envelope, and idempotency strategy.
- Define temporary stubbing rules for services that may still be missing when integration starts:
  - seeded `ownerId` values
  - static auth assumptions or demo headers
  - log-based or console-consumer verification for Kafka flows
- Decide whether the team will keep static service configuration or add discovery later.

**Outputs**

- Versioned API and event contract sheet
- Confirmed workflow sequence for `W1`, `W2`, and `W3`

**Definition of done**

- A teammate can implement against contracts rather than chat messages and guesswork.

### Phase 08 - Backend Assembly for Workflow W1

**Goal**

Get the core synchronous business flow working before checkpoint #1.

**Work**

- Integrate `API Gateway` with backend routes.
- Wire the happy path for `W1`:
  1. client submits order
  2. `Order Service` checks restaurant availability
  3. `Order Service` validates menu items
  4. `Order Service` creates order
  5. `Payment Service` processes payment
  6. `Delivery Service` creates delivery task
- Confirm compensating behavior if downstream calls fail.
- Keep logs and demo data readable for checkpoint explanation.

**Outputs**

- One end-to-end backend happy path
- Known failure paths documented

**Definition of done**

- The team can place one order through the gateway and see data created in the right services.

### Phase 09 - Checkpoint #1 Preparation

**Goal**

Package the backend into something that survives a live checkpoint.

**Work**

- Add smoke-test script for the main backend flow.
- Prepare seeded demo scenario.
- Ensure every service can start cleanly from scratch.
- Fix bad error messages, missing env vars, and container startup issues.
- Verify Swagger links, Postman requests, and health checks so the backend can be explained, not just executed.
- Prepare checkpoint talking points:
  - service boundaries
  - why no shared DB
  - why W1 is synchronous
  - where async messaging will appear next
- Update report draft with backend architecture and workflow diagrams.

**Outputs**

- Backend checkpoint demo pack
- First checkpoint-grade report content

**Definition of done**

- The team can demonstrate backend-only integration with minimal manual setup.

### Phase 10 - Frontend Shell

**Goal**

Create the minimum viable Vue frontend after backend fundamentals are stable.

**Work**

- Scaffold the Vue 3 application.
- Add router, page layout, shared navigation, and service client utilities.
- Define UI pages for:
  - restaurant list
  - restaurant detail/menu
  - cart/order creation
  - order status/history
  - notifications
- Add environment config for gateway-based API access.
- Decide basic state management approach.

**Outputs**

- Running frontend shell
- Route map and API client layer

**Definition of done**

- The frontend can navigate between pages even before all real data flows are complete.

### Phase 11 - Restaurant and Menu UX

**Goal**

Expose Sierra-Lima's backend work visibly in the user-facing app.

**Work**

- Implement restaurant browse/search screen.
- Implement restaurant detail page.
- Implement menu-item listing, availability display, and item selection.
- Add loading, empty, and error states.
- Ensure the UI consumes live `Restaurant Service` and `Menu Service` APIs, not hardcoded mocks.

**Outputs**

- Working browse-and-select experience
- Clear demo value from Sierra-Lima-owned services

**Definition of done**

- A demo user can discover a restaurant, open it, and view orderable menu items.

### Phase 12 - Order/Status UX and Checkpoint #2 Prep

**Goal**

Reach the `frontend + backend` expectation for checkpoint #2.

**Work**

- Implement cart submission flow.
- Connect order placement form to `W1`.
- Add order details/status page.
- Show eventual updates from payment or delivery if possible.
- Remove obvious UI breakages and placeholder screens.
- Prepare a coherent checkpoint #2 story:
  - frontend path
  - backend path
  - what is still pending for final security phase

**Outputs**

- Integrated frontend/backend demo
- Checkpoint #2 readiness bundle

**Definition of done**

- One person can demo restaurant discovery through order creation in a single run.

### Phase 13 - Security Implementation

**Goal**

Cover the `security` expectation before the final checkpoint.

**Recommended direction**

Implement a thin JWT-based authentication layer rather than a full-blown user-management product.

**Work**

- Decide the smallest acceptable final security scope:
  - thin auth service issuing JWTs, recommended
  - or gateway-level JWT validation plus seeded demo users
- Protect the most important routes.
- Add role-aware authorization where useful:
  - customer actions
  - restaurant-owner actions
  - driver updates
- Update the frontend to store token state and send auth headers.
- Remove any checkpoint-era shortcuts that would look weak in the final demo.

**Outputs**

- Protected API routes
- Token-aware frontend
- Security narrative tied to the course practicals

**Definition of done**

- The team can demonstrate that unauthorized access is blocked and authenticated flows work.

### Phase 14 - Async Polish and Notifications

**Goal**

Strengthen the event-driven story so the final system is not just a chain of REST calls.

**Work**

- Finalize Kafka topic setup.
- Verify producers and consumers for `W2` and `W3`.
- Ensure `Delivery Service` publishes delivery updates.
- Ensure `Notification Service` consumes relevant events and stores user-facing notifications.
- Show event payloads clearly in logs or documentation.
- Add dead-letter or retry strategy if lightweight support is feasible.
- If the core flow is already stable, optionally add a minimal Resilience4j circuit breaker or retry around the highest-risk synchronous call to strengthen the technical story without expanding scope recklessly.

**Outputs**

- Reliable asynchronous demo path
- Stronger justification for using microservices

**Definition of done**

- The team can point to a real event-driven behavior that updates another service without a direct synchronous call.

### Phase 15 - Report and Evidence Pack

**Goal**

Finish the written deliverable before the final presentation crunch.

**Work**

- Assemble the final report sections:
  - business architecture
  - technical architecture
  - implemented services
  - data models
  - APIs
  - workflows
  - integration mechanisms
  - security approach
  - team responsibilities
  - limitations and future work
- Refresh diagrams to match the actual code, not the old design only.
- Add screenshots, endpoint tables, topic tables, and demo notes.
- Make sure the report explains where design-only services still exist.

**Outputs**

- Near-final project report
- Supporting visual evidence

**Definition of done**

- If the code froze tomorrow, the written deliverable would still be mostly ready.

### Phase 16 - Final Presentation Rehearsal

**Goal**

Turn a working system into a convincing presentation.

**Work**

- Create slide deck.
- Define live demo script with exact click-path and expected outputs.
- Assign who speaks about:
  - architecture
  - Sierra-Lima services
  - synchronous flow
  - asynchronous flow
  - security
- Prepare fallback materials:
  - screenshots
  - prerecorded flow
  - backup seed data
  - recovery commands
- Rehearse timeboxed answers to likely questions:
  - why these services were implemented
  - why some services stayed design-only
  - how service boundaries were chosen
  - how asynchronous integration works
  - how security was added

**Outputs**

- Rehearsed presentation
- Fail-safe demo plan

**Definition of done**

- Every presenter knows what to say, what to click, and what to do if the demo misbehaves.

### Phase 17 - Buffer and Final Freeze

**Goal**

Use the last session for stabilization only, not feature invention.

**Work**

- Fix the highest-risk bugs only.
- Re-run smoke tests.
- Verify seeded demo users, restaurants, menu items, and order flow.
- Verify startup from a clean environment.
- Freeze the branch for presentation.

**Outputs**

- Final demo build
- Lower presentation-day risk

**Definition of done**

- No unresolved defect remains that could break the main demo narrative.

## 8. What Sierra-Lima Can Safely Do Early, Even Alone

These are the highest-value early-start tasks for Sierra-Lima:

1. Finish Phases `01` to `06` without waiting for anyone.
2. Produce the canonical `Restaurant Service` and `Menu Service` contracts.
3. Seed compelling restaurant and menu demo data.
4. Make `Menu Service` validation solid, because `Order Service` depends on it directly.
5. Keep both services documented through OpenAPI and example requests.
6. Prepare lightweight integration stubs or sample payloads for the Order team.

If the rest of the team arrives late, Sierra-Lima's work still remains useful because it becomes the most stable part of the system.

## 9. Checkpoint Readiness Gates

### Checkpoint #1 Gate (`2026-05-05`)

The project should not be considered ready for backend checkpoint #1 unless all of the following are true:

- at least the main implemented backend services compile and run
- `Restaurant Service` and `Menu Service` are fully operational
- `W1` happy path works through the gateway or through clearly documented service calls
- databases are separated per service
- the backend can be started from a documented local process
- architecture and workflow diagrams match reality

### Checkpoint #2 Gate (`2026-05-12`)

The project should not be considered ready for frontend/backend checkpoint #2 unless:

- checkpoint #1 criteria still hold
- the frontend uses live backend APIs
- a user can browse restaurants, inspect menus, and place an order
- the UI handles at least basic loading and error states
- the demo does not rely on hidden manual DB manipulation

### Checkpoint #3 Gate (`2026-05-19`)

The project should not be considered ready for the final presentation unless:

- checkpoint #2 criteria still hold
- a security mechanism is visible in the real system
- at least one asynchronous workflow is demonstrably working
- the report is presentation-ready
- the team has a demo fallback plan

## 10. Risks and Mitigations

### Risk 1 - Team mobilizes late

**Mitigation**

- Front-load Sierra-Lima-owned services and interface definitions.
- Use contract-first coordination.
- Treat Phases `01` to `06` as solo-capable.

### Risk 2 - Scope expands back to all eight services

**Mitigation**

- Hold the Assignment 3 implementation subset unless the instructor explicitly asks for more.
- Prefer quality and integration over service count inflation.

### Risk 3 - Security is postponed too long

**Mitigation**

- Reserve Phase `13` specifically for security.
- Keep auth assumptions abstract enough early so JWT can be inserted later without rewriting every service.

### Risk 4 - Shared database shortcut appears during integration

**Mitigation**

- Reject it immediately.
- Keep cross-service references as IDs only.
- Treat separate persistence as a project constraint, not a suggestion.

### Risk 5 - Demo environment becomes fragile

**Mitigation**

- Standardize ports, env vars, seed data, and startup order in early phases.
- Rehearse clean-start runs before each checkpoint.

### Risk 6 - Diagrams and report fall behind code

**Mitigation**

- Update report material at each checkpoint phase instead of leaving everything to the end.

## 11. Recommended Success Criteria for the Final Demo

By the time the project reaches final-presentation-ready status, the team should be able to demonstrate:

1. a customer browsing restaurants and menus
2. creation of an order through the frontend
3. backend coordination across multiple services
4. at least one event-driven update or notification
5. at least one secured route or authenticated flow
6. clear service boundaries and separate data ownership
7. a report and diagrams that match the implemented system

## 12. Bottom Line

The safest route from this repository's current scaffolded state to a credible final project is:

- build Sierra-Lima's two services first and build them properly
- use them as stable anchors for the rest of the team
- reach backend integration by `2026-05-05`
- layer in frontend by `2026-05-12`
- finish security, async polish, report, and presentation readiness by `2026-05-19`

If the team follows the phase order above, early solo work will not be wasted, and the project will remain aligned with the course assignments, practicals, and checkpoint structure.

## Appendix A - Suggested Session Calendar

One workable cadence is roughly three sessions per week, with the most Sierra-Lima-heavy work front-loaded before team-wide integration becomes unavoidable.

| Session | Approx. date | Phase | Notes |
| --- | --- | --- | --- |
| 1 | 2026-04-18 | 01 | Scope freeze and conventions |
| 2 | 2026-04-19 | 02 | Sierra-Lima contract pack |
| 3 | 2026-04-20 | 03 | Local bootstrap |
| 4 | 2026-04-21 | 04 | Restaurant Service foundation |
| 5 | 2026-04-23 | 05 | Menu Service foundation |
| 6 | 2026-04-25 | 06 | Sierra-Lima hardening |
| 7 | 2026-04-27 | 07 | Team integration contract lock |
| 8 | 2026-04-29 | 08 | Backend assembly for `W1` |
| 9 | 2026-05-02 | 09 | Checkpoint #1 prep |
| 10 | 2026-05-06 | 10 | Frontend shell |
| 11 | 2026-05-08 | 11 | Restaurant and menu UX |
| 12 | 2026-05-10 | 12 | Order/status UX and checkpoint #2 prep |
| 13 | 2026-05-13 | 13 | Security |
| 14 | 2026-05-15 | 14 | Async polish |
| 15 | 2026-05-16 | 15 | Report and evidence pack |
| 16 | 2026-05-18 | 16 | Final rehearsal |
| 17 | 2026-05-19 | 17 | Buffer and final freeze |

If any phase slips, compress optional infrastructure before compressing the core Sierra-Lima service work, the main `W1` flow, or the final security pass.

## Appendix B - Coordination Triggers

These are the moments where proactive team coordination matters most:

| When | What must be agreed | Who needs to be involved |
| --- | --- | --- |
| Before Phase 04 | package structure, Java version, build tool, naming rules | all |
| Before Phase 07 | endpoint paths, request/response schemas, ownership boundaries | all |
| Before Phase 08 | gateway routing assumptions and workflow `W1` sequence | backend owners |
| Before Phase 14 | topic names, event envelope, and consumer expectations | backend owners |
| Before Phase 13 | JWT approach, protected routes, and demo user strategy | all |
| Before Phase 16 | slide structure, demo path, fallback plan, speaking order | all |

When teammates are late, Sierra-Lima can still move by using stable IDs, seeded demo data, documented assumptions, and contract-first stubs, then replacing those stubs once the real services arrive.
