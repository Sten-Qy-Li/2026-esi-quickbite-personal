# Golf-Papa-Tango Project Phases Plan (`aac68b0`)

**Pseudonym:** Golf-Papa-Tango  
**Base commit:** `aac68b0`  
**Supersedes:** `Golf-Papa-Tango_2ce188a_project-phases.md`  
**Student:** Sierra-Lima  
**Services owned:** Restaurant Service, Menu Service  
**Team:** Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa (Group 7)  
**Workspace type:** personal early-start repository  
**Date created:** 2026-04-17

---

## 0. How to Use This Document

This roadmap is intended to work as a standalone implementation guide for
Sierra-Lima's personal early-start workspace. The goal is that the critical
scope, contracts, workflows, and execution order needed to reach checkpoint
readiness remain recoverable from this single file even when day-to-day work
happens away from the assignment PDFs.

The primary design artifacts this roadmap is grounded in, all at commit
`aac68b0`, are:

- `dev-docs/prior-submissions/Assignment-1-Submission.pdf`
- `dev-docs/prior-submissions/Assignment-1_Feedback.txt`
- `dev-docs/prior-submissions/Assignment-2-Submission.pdf`
- `dev-docs/prior-submissions/Assignment-2-Submission.docx`
- `dev-docs/prior-submissions/Assignment-3-Submission.pdf`
- `dev-docs/prior-submissions/Assignment-3-Submission.docx`
- `dev-docs/prior-submissions/assignment-3_figure1_business-architecture.png`
- `dev-docs/prior-submissions/assignment-3_figure1b_implementation-architecture.png`
- `dev-docs/prior-submissions/assignment-3_figure2_service-er-diagrams.png`
- `dev-docs/prior-submissions/assignment-3_figure3_workflow-w1-sequence.png`
- `dev-docs/prior-submissions/assignment-3_figure4_workflow-w2-w3-events.png`

The canonical execution facts most likely to block implementation are pinned in
Appendix E so the roadmap remains self-contained. If a conflict is found
between this roadmap and the committed assignment artifacts, the assignment
artifacts are authoritative and this roadmap should be updated.

---

## 1. Review of the `2ce188a` Roadmap

### 1.1 What still holds

The earlier Golf roadmap was still right about several important things:

- Sierra-Lima should front-load `Restaurant Service` and `Menu Service`.
- service-local databases remain mandatory
- cross-service references should stay as IDs only
- `W1` should be the main backend integration target before checkpoint #1
- frontend work should follow backend stability, not precede it
- checkpoint dates and the overall course pacing were already correct

### 1.2 What changed materially

The `2ce188a` version was written before the updated `Assignment-3` submission
had been fully reviewed. The newer prior-submission materials change the plan
in several material ways:

1. **`User Service` is now implemented, not design-only.**  
   The implementation subset is now seven business services plus two shared
   integration components. Only `Review Service` remains design-only.

2. **Bearer-token authentication is part of the baseline, not a final-phase add-on.**  
   `User Service` issues tokens. Implemented services validate tokens locally.
   Public routes are only `POST /users` and `POST /auth/login` unless the team
   consciously changes that.

3. **Static service configuration is the default.**  
   The updated design does not require a separate service-discovery server.

4. **Shared-component ownership is now explicit.**  
   `API Gateway` is owned by Alfa-Kilo, and `Event Broker configuration` is
   owned by Mike-Alfa.

5. **Workflows, events, and contracts are more explicit.**  
   Endpoint paths, event topics, event names, and the envelope shape are now
   concrete enough to plan against directly.

6. **The repository has not yet caught up with the full official scope.**  
   At commit `aac68b0`, this personal repository still contains only
   documentation and service scaffolding for Sierra-Lima-owned work plus
   `local-dev`. The roadmap therefore has to stay aligned with the official
   seven-service implementation scope without pretending the personal workspace
   already contains it.

This revision keeps the parts of the earlier roadmap that were still sound,
then adds the stronger scope, contract, and operational discipline now visible
from the updated assignment material.

---

## 2. Overview

This roadmap maps the path from the current scaffolded personal workspace to a
checkpoint-ready and presentation-ready QuickBite implementation using focused
3-hour sessions.

### 2.1 Fixed Course Anchors

Use these dates as hard planning anchors:

| Date | Milestone | Focus |
| --- | --- | --- |
| `2026-04-21` | Project description practical | Official project specification |
| `2026-04-28` | Project consultation practical | Clarify scope and expectations |
| `2026-05-05` | Checkpoint #1 | Backend implementation |
| `2026-05-12` | Checkpoint #2 | Frontend + backend integration |
| `2026-05-19` | Checkpoint #3 | Final presentation, frontend + backend + security |

Note: the practicals page shows `28/04/2025` for project consultation. In the
2026 schedule context that is almost certainly a typo, so this roadmap treats
the consultation as `2026-04-28`.

### 2.2 Grading Context (informative)

- Assignments: `20` points total (`A1`: `4`, `A2`: `8`, `A3`: `8`)
- Project: `30` points total across the three checkpoints
- Exam: `50` points total, with at least `21` needed to pass
- Total: `100` points, with at least `51` needed to pass the course
- Assignment 1 score: `3.50 / 4.00`; the architecture and database penalties
  are addressed explicitly in Appendix C

### 2.3 Why this revision exists

The point of this revision is not just to restate the old roadmap with a new
commit id. It is to:

- correct the outdated six-service assumption
- treat auth as a baseline concern instead of a late addition
- keep the roadmap grounded in the real state of this personal workspace
- make the document more operational and self-contained

---

## 3. Updated Planning Baseline

### 3.1 Official implementation scope

The full QuickBite business architecture still contains eight business
services:

1. `User Service`
2. `Order Service`
3. `Menu Service`
4. `Restaurant Service`
5. `Delivery Service`
6. `Payment Service`
7. `Notification Service`
8. `Review Service`

The updated `Assignment-3` submission defines the implemented subset as:

| Category | Included |
| --- | --- |
| Implemented business services | `Order`, `User`, `Restaurant`, `Menu`, `Payment`, `Delivery`, `Notification` |
| Implemented shared components | `API Gateway`, `Event Broker configuration` |
| Design-only | `Review Service` |

### 3.2 Team ownership

| Team member | Owned components |
| --- | --- |
| Alfa-Kilo | `Order Service`, `User Service`, `API Gateway` |
| Sierra-Lima | `Restaurant Service`, `Menu Service` |
| Elephant-Yankee | `Payment Service`, `Delivery Service` |
| Mike-Alfa | `Notification Service`, `Event Broker configuration` |

Sierra-Lima still owns two business services and does not replace either one
with a shared integration component.

### 3.3 Current repository baseline

This repository is still a personal early-start workspace, not the official
shared team repository. At commit `aac68b0`, the code-side structure is still
mostly scaffolded, with placeholder folders for:

- `services/restaurant-service`
- `services/menu-service`
- `services/local-dev`

That makes the safest local scope:

- build Sierra-Lima-owned services fully enough to become stable integration targets
- build `local-dev` assets that help the whole team later
- document contracts and assumptions for teammate-owned services
- avoid silently taking over teammate-owned business logic unless the team explicitly decides so

### 3.4 Sierra-Lima service baseline

For Sierra-Lima's two services, the assignment-derived business core is already
known and should not be reinvented during implementation.

**Restaurant Service**

- aggregate root: `Restaurant`
- fields: `restaurantId`, `ownerId`, `name`, `address`, `city`, `latitude`, `longitude`, `operatingHours`, `isOpen`
- value object candidate: `Location`
- main requirements: `R19`, `R20`
- external dependency: `ownerId` references `User.userId`

Planned endpoints:

- `POST /restaurants`
- `GET /restaurants/{id}`
- `PUT /restaurants/{id}`
- `PATCH /restaurants/{id}/status`
- `GET /restaurants`
- `GET /restaurants/{id}/availability`

**Menu Service**

- aggregate root: `MenuItem`
- fields: `menuItemId`, `restaurantId`, `name`, `description`, `priceAmount`, `priceCurrency`, `category`, `isAvailable`
- value object candidate: `Price`
- main requirements: `R21`, `R22`
- external dependency: `restaurantId` references `Restaurant.restaurantId`

Planned endpoints:

- `POST /restaurants/{rid}/menu-items`
- `GET /restaurants/{rid}/menu-items`
- `GET /menu-items/{id}`
- `PUT /menu-items/{id}`
- `DELETE /menu-items/{id}`
- `POST /menu-items/validate`

### 3.5 Non-negotiable rules

These rules should be treated as fixed unless the instructor explicitly changes
them:

1. No shared database.
2. Business architecture and technical architecture stay separate.
3. Cross-service relationships use ID references only.
4. `User Service` is implemented; `Review Service` remains design-only.
5. The official Assignment 3 baseline treats only `POST /users` and
   `POST /auth/login` as public. If this personal workspace keeps browse reads
   public temporarily, that deviation must be logged explicitly and revisited
   in Phase 1.
6. Authentication is part of the baseline, not just a final demo feature.
7. Static service configuration is the default. Service discovery is optional,
   not assumed.
8. `W1` is the main synchronous workflow target, and at least one
   asynchronous workflow from `W2` or `W3` must be demonstrable by the final
   checkpoint.
9. This repository is a personal workspace, so local work should maximize
   reuse by the eventual shared team repo and avoid quietly absorbing
   teammate-owned business logic.

### 3.6 Working technology baseline

| Layer | Primary choice | Notes |
| --- | --- | --- |
| Backend | Spring Boot | Java + Maven setup aligned with course practicals |
| Java version | 17 recommended, 21 optional | lock one team-wide choice early |
| Database | PostgreSQL | one database per service |
| Schema migrations | Flyway | one migration history per service; after baseline, prefer `ddl-auto=validate` |
| Containerisation | Docker + Docker Compose | local reproducibility is mandatory |
| API gateway | Spring Cloud Gateway | owned by Alfa-Kilo |
| Async messaging | Apache Kafka | owned by Mike-Alfa |
| Frontend | Vue.js 3 | Vue Router plus a simple HTTP client is enough |
| Security | Spring Security + JWT (`jjwt` `0.11.5`) | baseline concern, not just a final-phase patch |
| API documentation | OpenAPI / Swagger (`springdoc-openapi`) | keep docs live from early service phases |
| API testing | Postman | maintain one shared collection with a login-first flow |
| Service discovery | none in baseline | add only if the team deliberately expands scope |
| Resilience | Resilience4j | optional, only after the core flows are stable |

---

### 3.7 Working implementation defaults for Sierra-Lima

To make the roadmap immediately actionable, Sierra-Lima should use these
working defaults unless the team explicitly changes them in Phase 1:

- `GET /restaurants`, `GET /restaurants/{id}`, `GET /restaurants/{rid}/menu-items`, and `GET /menu-items/{id}` are implemented as public browse routes in the personal workspace by default. If the team later changes them to customer-only, tighten security filters without changing DTOs or business logic.
- `GET /restaurants/{id}/availability` and `POST /menu-items/validate` accept either a customer token or a service token.
- Restaurant and menu write routes require `RestaurantOwner` or `Admin`.
- Ownership checks use the authenticated `userId` claim from the JWT and compare it against `Restaurant.ownerId`. Menu mutations are allowed only when the caller owns the target restaurant or has `Admin`.
- The minimum JWT claim set expected by Sierra-Lima services is: `sub`, `userId`, `role`, `tokenType`, and optional `serviceName` for internal service calls.
- The gateway may forward convenience headers such as `X-User-Id`, but JWT claims remain the source of truth inside the service.
- Each Sierra-Lima service owns its own Flyway migrations from the first persistent version onward. After the initial migration is in place, prefer schema validation over schema auto-creation in shared demos.

These defaults are intentionally concrete so implementation can start before
every team-level auth detail is perfect.

---

## 4. Named Workflows

These workflow labels are used throughout the plan:

| Label | Name | Style | Summary |
| --- | --- | --- | --- |
| `W1` | Place Order | Synchronous REST | Client -> Gateway -> Order -> User/Restaurant/Menu/Payment/Delivery |
| `W2` | Delivery Progress & Notifications | Asynchronous | Delivery publishes `delivery.status-changed`; Order and Notification consume |
| `W3` | Payment Outcome Notification | Asynchronous | Payment publishes `payment.completed` / `payment.failed`; Notification consumes, Order may react |

The more detailed contract facts are pinned in Appendix E.

---

## 5. Planning Principles

These principles should govern every phase:

1. Keep the project aligned with Assignments 1 to 3 rather than reinventing the system.
2. Keep business architecture separate from technical architecture.
3. Give every implemented microservice its own database. No shared database.
4. Prioritize the two required interaction styles: synchronous REST and asynchronous event-driven integration.
5. Bake in auth from the start instead of treating it as a decorative final-phase add-on.
6. Keep each service small and demoable. Assignment 3 suggests roughly 5 to 8 endpoints per service.
7. Avoid infrastructure inflation. Extra components stay optional unless explicitly justified.
8. Bias the early phases toward artifacts that remain useful even if teammates join late.
9. Favor work that makes this personal repo easy to transfer into or compare against the shared team repo later.

---

## 6. Delivery Strategy

Because the official implementation scope has expanded but this repository is
still mostly empty, the safest strategy is:

1. re-baseline the project around the updated Assignment 3 scope
2. freeze auth and gateway assumptions early
3. build Sierra-Lima's two services as protected, integration-ready services
4. assemble backend workflow `W1` before checkpoint #1
5. add frontend before checkpoint #2
6. finish authorization hardening, async evidence, report, and demo readiness before checkpoint #3

This ordering keeps early solo work reusable and reduces late integration churn.

### 6.1 Cross-phase working assets

These assets should stay live throughout the project rather than appearing late:

- one shared Postman collection that starts with login and then exercises protected requests
- Swagger/OpenAPI exposure for Restaurant and Menu services
- a seeded demo dataset for restaurants, menu items, and `W1` participants
- one documented Docker Compose runbook for clean startup
- health-check or ping endpoints for quick verification
- example payloads for `Order -> Restaurant` and `Order -> Menu`
- screenshot or recording backups for each checkpoint demo

---

## 7. Phase Map at a Glance

| Phase | Title | Checkpoint Target | Est. Sessions |
| --- | --- | --- | --- |
| 0 | Scope Freeze & Repo Conventions | -- | 1 |
| 1 | Auth & Gateway Contract Alignment | -- | 1 |
| 2 | Contract Pack & Local-Dev Bootstrap | -- | 2 |
| 3 | Restaurant Service Foundation | CP#1 | 1 |
| 4 | Restaurant Service Full API, Validation, OpenAPI | CP#1 | 1 |
| 5 | Menu Service Foundation | CP#1 | 1 |
| 6 | Menu Service Full API, Validation, OpenAPI | CP#1 | 1 |
| 7 | Sierra-Lima Hardening Pass | CP#1 | 2 |
| 8 | Dockerise Both Services | CP#1 | 1 |
| 9 | Team Contract Lock for W1 / W2 / W3 | CP#1 | 1 |
| 10 | W1 Integration & Failure-Path Protection | CP#1 | 2 |
| 11 | Backend Polish & Checkpoint #1 Prep | CP#1 | 2 |
| 12 | Frontend Shell, Routing & Sign-In | CP#2 | 1 |
| 13 | Restaurant & Menu UX | CP#2 | 1 |
| 14 | Frontend-Backend Integration & Checkpoint #2 Prep | CP#2 | 1 |
| 15 | Authorization Hardening & Role-Aware Behaviour | CP#3 | 1 |
| 16 | Async Evidence & Cross-Service Smoke | CP#3 | 1 |
| 17 | Report & Evidence Pack | CP#3 | 1 |
| 18 | Final Presentation Rehearsal | CP#3 | 1 |
| 19 | Buffer & Final Freeze | CP#3 | 1 |

**Total:** roughly 24 focused 3-hour blocks, or about 72 hours of implementation and integration work.

A phase is a work package, not always a single 3-hour session. Any phase with
`Est. Sessions = 2` should be planned as two separate 3-hour blocks rather
than squeezed into one overloaded sitting.

---

## 8. Detailed Phase Plan

### Phase 0 -- Scope Freeze & Repo Conventions

**Goal**

Turn the updated assignment outputs into one implementation baseline so nobody
is still debating what is in or out.

**Tasks**

1. Reconfirm project scope from the updated Assignment 3:
   - implemented: `Order`, `User`, `Restaurant`, `Menu`, `Payment`, `Delivery`, `Notification`
   - shared: `API Gateway`, `Event Broker configuration`
   - design-only: `Review Service`
2. Freeze the canonical workflows `W1`, `W2`, and `W3`.
3. Decide folder layout under `services/`.
4. Define conventions:
   - branch strategy
   - commit message format
   - Java version
   - package naming
   - env-var naming
   - Docker image naming
5. Record open design questions in `dev-docs/decisions/`.
6. Produce a non-goals list for the first implementation pass.

**Definition of done**

- implementation subset confirmed in writing
- folder structure agreed
- conventions documented
- non-goals list exists
- nobody on the team should still be debating which services are in or out

---

### Phase 1 -- Auth & Gateway Contract Alignment

**Goal**

Remove the biggest new integration risk introduced by the updated scope:
unclear authentication and route-protection behaviour.

**Tasks**

1. Agree the public route list:
   - `POST /users`
   - `POST /auth/login`
2. Agree the default protected-route rule for all other implemented endpoints.
3. Confirm gateway path prefixes.
4. Confirm the token-propagation model:
   - client bearer token to gateway
   - gateway validates and forwards token
   - downstream services validate locally
   - service-to-service REST calls also use bearer tokens
5. Agree what identity context Sierra-Lima services need:
   - current authenticated user id
   - role
   - service identity for internal calls
6. Decide explicitly whether browse endpoints remain protected exactly as designed or whether the team will consciously deviate and document it.
7. Capture the expected JWT claims shape so Sierra-Lima can mock locally if needed.

**Outputs**

- auth contract sheet
- route-protection matrix
- gateway path map
- example JWT claims payload

**Definition of done**

- every Sierra-Lima endpoint has a documented auth posture
- Sierra-Lima can implement services against a documented JWT shape without blocking on User Service completion

---

### Phase 2 -- Contract Pack & Local-Dev Bootstrap

**Goal**

Make Restaurant Service and Menu Service precise enough for later integration
and define one shared local-dev target.

**Tasks**

1. Freeze final REST endpoints for both services.
2. Freeze request and response payloads.
3. Freeze validation rules:
   - restaurant required fields
   - operating-hours format
   - latitude/longitude ranges
   - price representation
   - item availability rules
4. Freeze database schemas.
5. Create seed-data plans for demo restaurants and menu items.
6. Write cross-service assumptions explicitly:
   - `Restaurant.ownerId` references `User.userId`
   - `MenuItem.restaurantId` references `Restaurant.restaurantId`
   - no cross-service joins
7. Verify the local toolchain:
   - Java
   - Maven
   - Docker Desktop
   - Docker Compose
   - Node.js
   - Postman
8. Scaffold Spring Boot projects for `restaurant-service` and `menu-service`
   with at least:
   - Spring Web
   - Spring Data JPA
   - PostgreSQL Driver
   - Validation
   - Lombok
   - DevTools
   - Spring Security
   - manually added `jjwt` dependencies after generation, because Spring
     Initializr will not include them
9. Design `local-dev` assets:
   - `docker-compose.yml`
   - `.env` or equivalent
   - README runbook
10. Standardize local ports, URLs, and environment variables.
11. Decide the initial migration approach and create the first Flyway plan for:
   - `restaurant_db`
   - `menu_db`

**Outputs**

- contract pack
- seed-data shortlist
- local runtime map
- env-var matrix

**Definition of done**

- Sierra-Lima can begin implementation without waiting for the rest of the team
- the repo has one documented local-dev target instead of ad hoc personal setup

**Recommended split across two 3-hour blocks**

- Block A: contracts, payloads, validation rules, schema decisions
- Block B: toolchain verification, scaffolding, compose skeleton, env matrix, migration setup

**Minimum viable outcome if only one block is available**

- finish Block A first and leave scaffolding/bootstrap mechanics for the next session

---

### Phase 3 -- Restaurant Service Foundation

**Goal**

Implement a persistent Restaurant Service that already fits the updated scope.

**Tasks**

- scaffold the service
- add core dependencies:
  - Spring Web
  - Spring Data JPA
  - PostgreSQL driver
  - validation
  - OpenAPI support
- implement the Restaurant data model
- implement:
  - create restaurant
  - get restaurant
  - update restaurant
  - patch open/closed status
  - list/search restaurants
  - availability endpoint
- add persistence and seed at least a few demo restaurants
- add initial Flyway migration for the first Restaurant schema
- use migration-driven schema creation rather than relying on long-term auto-DDL

**Definition of done**

- `Restaurant Service` runs locally
- restaurant data persists
- another service could ask whether a restaurant is open without a database shortcut

---

### Phase 4 -- Restaurant Service Full API, Validation, OpenAPI

**Goal**

Raise Restaurant Service from foundation to documented integration target.

**Tasks**

- add validation and structured error responses
- add timestamps and auditing
- add OpenAPI / Swagger exposure
- add tests for:
  - happy-path CRUD
  - invalid input
  - availability responses
- keep the service shape clean for later gateway integration
- switch shared/demo configuration toward migration validation instead of schema auto-creation

**Definition of done**

- Restaurant Service has a stable, documented, test-backed API

---

### Phase 5 -- Menu Service Foundation

**Goal**

Implement a persistent Menu Service that is realistic enough for order
validation.

**Tasks**

- scaffold the service
- implement the MenuItem data model
- implement:
  - add menu item
  - list menu items for a restaurant
  - get menu item
  - update menu item
  - delete menu item
  - validate menu items for order placement
- add seed data linked to demo restaurants
- add initial Flyway migration for the first Menu schema
- use migration-driven schema creation rather than relying on long-term auto-DDL

**Definition of done**

- `Menu Service` runs locally
- menu data persists
- `Order Service` could validate items and price inputs without direct DB access

---

### Phase 6 -- Menu Service Full API, Validation, OpenAPI

**Goal**

Raise Menu Service from foundation to stable integration contract.

**Tasks**

- add validation and structured error responses
- add timestamps and auditing
- add OpenAPI / Swagger exposure
- add tests for:
  - CRUD behaviour
  - restaurant ownership rules
  - batch validation path for `W1`
- make `POST /menu-items/validate` return the exact information needed for price calculation and per-item failures
- switch shared/demo configuration toward migration validation instead of schema auto-creation

**Definition of done**

- Menu Service has a stable, documented, test-backed API
- the validation endpoint is strong enough for direct use by `Order Service`

---

### Phase 7 -- Sierra-Lima Hardening Pass

**Goal**

Raise Restaurant/Menu from "works locally" to "usable under the intended auth
and demo conditions".

**Tasks**

- add token-validation hook or agreed auth integration
- standardize error response shape across both services
- pin the 401/403/404/409/422 behavior for the Sierra-Lima routes
- add CORS and gateway-friendly configuration
- add Dockerfiles and build hygiene
- publish example requests in Postman
- verify owner/admin checks against `ownerId` using the agreed JWT claims
- keep both services aligned with Assignment 1 feedback:
  - infrastructure stays out of business architecture
  - databases stay service-local

**Definition of done**

- Restaurant and Menu are no longer just local CRUD demos; they are usable integration targets

**Recommended split across two 3-hour blocks**

- Block A: auth integration, claim mapping, owner checks, 401/403/error model
- Block B: CORS, Dockerfiles, Postman refresh, docs, demo polish

**Minimum viable outcome if only one block is available**

- finish Block A first; auth and authorization correctness matter more than packaging polish

---

### Phase 8 -- Dockerise Both Services

**Goal**

Package Sierra-Lima's two services into the shared local runtime story.

**Tasks**

- add `docker-compose` support for:
  - restaurant database
  - menu database
  - restaurant service
  - menu service
  - Kafka broker if already needed for local-dev
- verify clean startup order
- document startup and reset commands

**Definition of done**

- both services can start from the documented local-dev runbook

---

### Phase 9 -- Team Contract Lock for `W1` / `W2` / `W3`

**Goal**

Stop late integration drift.

**Tasks**

- confirm the synchronous call chain for `W1`
- confirm request and response shapes for:
  - `Order -> User`
  - `Order -> Restaurant`
  - `Order -> Menu`
  - `Order -> Payment`
  - `Order -> Delivery`
- confirm event topics and event names:
  - `payment-events`
  - `delivery-events`
  - `order-events`
  - `payment.completed`
  - `payment.failed`
  - `delivery.status-changed`
  - `order.cancelled`
- confirm the shared event envelope
- confirm dead-letter and idempotency expectations
- lock down status codes for Restaurant/Menu validation failures

**Definition of done**

- teammates can implement against written contracts instead of chat memory

---

### Phase 10 -- W1 Integration & Failure-Path Protection

**Goal**

Make Sierra-Lima's services practical parts of the main synchronous backend
workflow before checkpoint #1.

**Tasks**

- test Restaurant availability checks from the actual Order-facing path
- test Menu validation from the actual Order-facing path
- provide stable seed IDs and demo payloads for integration
- help debug gateway or auth issues that affect Restaurant/Menu reachability
- verify meaningful failure paths:
  - restaurant closed
  - missing menu item
  - unavailable menu item
  - malformed payload
- keep logs and demo data readable for checkpoint explanation
- verify both direct service calls and gateway-routed calls if the gateway path is available

**Definition of done**

- the team can hit `W1` and see Restaurant/Menu participate through documented interfaces rather than manual workarounds

**Recommended split across two 3-hour blocks**

- Block A: direct contract smoke for availability and validation, seed IDs, payload stabilization
- Block B: gateway/auth path, failure-path verification, log cleanup, demo narrative

**Minimum viable outcome if only one block is available**

- complete Block A and record the unresolved gateway/auth integration items explicitly for follow-up

---

### Phase 11 -- Backend Polish & Checkpoint #1 Prep

**Goal**

Package the backend for a live backend checkpoint.

**Tasks**

- rehearse startup from a clean local-dev path
- verify login or token issuance plus protected-service access
- verify Swagger docs still match reality
- verify Postman collection includes:
  - registration or login
  - protected Restaurant calls
  - protected Menu calls
  - one `W1` path
- prepare checkpoint talking points:
  - seven implemented business services
  - why `Review Service` stayed design-only
  - why static configuration was enough
  - how auth is enforced
  - how separate databases address Assignment 1 feedback

**Definition of done**

- the backend demo survives a cold start and can show login, protected API access, and the main `W1` path

**Recommended split across two 3-hour blocks**

- Block A: startup rehearsal, Postman verification, smoke checks, missing-env cleanup
- Block B: checkpoint talk track, evidence pack, fallback notes, diagram alignment

**Minimum viable outcome if only one block is available**

- complete Block A first; a reproducible backend is more important than polished checkpoint narration

---

### Phase 12 -- Frontend Shell, Routing & Sign-In

**Goal**

Start the frontend in a way that respects the protected-route model.

**Tasks**

- scaffold the Vue 3 shell
- add routing, layout, and API client utilities
- implement sign-in flow against `POST /auth/login`
- store token state in the frontend
- send bearer tokens through gateway-based API calls
- define pages for:
  - login
  - restaurant list
  - restaurant detail/menu
  - cart/order flow
  - notifications

**Definition of done**

- the frontend can sign in and call at least one protected backend route through the gateway

---

### Phase 13 -- Restaurant & Menu UX

**Goal**

Expose Sierra-Lima's services clearly in the user-facing application.

**Tasks**

- implement restaurant browse/list view
- implement restaurant detail view
- implement menu display for a selected restaurant
- add loading, error, and empty states
- support owner-side flows if the team wants them in the demo:
  - create restaurant
  - update restaurant
  - add or edit menu items
- keep the UI connected to real protected APIs, not hardcoded mocks

**Definition of done**

- a demo user can sign in, browse restaurants, open one, and inspect orderable menu items

---

### Phase 14 -- Frontend-Backend Integration & Checkpoint #2 Prep

**Goal**

Reach the `frontend + backend` expectation for checkpoint #2.

**Tasks**

- connect the frontend to the main `W1` order-placement path
- add order-status views if the Order path is ready
- surface notification or delivery-state feedback if available
- remove obvious UI placeholders and broken routes
- prepare checkpoint #2 story:
  - sign-in
  - browse
  - select menu items
  - submit order
  - inspect status

**Definition of done**

- one person can demonstrate sign-in through order placement without hidden manual DB edits

---

### Phase 15 -- Authorization Hardening & Role-Aware Behaviour

**Goal**

Finish the parts most likely to be left weak after checkpoint #2.

**Tasks**

- enforce real authorization behaviour on protected Restaurant/Menu writes
- confirm role-aware behaviour where relevant:
  - customer
  - restaurant owner
  - driver
  - admin
- remove temporary auth bypasses or debug shortcuts
- keep the frontend token-aware and role-aware

**Definition of done**

- unauthorized access is visibly rejected
- at least one role-gated rejection can be demonstrated

---

### Phase 16 -- Async Evidence & Cross-Service Smoke

**Goal**

Strengthen the event-driven story so the final demo is not just a chain of REST
calls.

**Tasks**

- confirm the event contracts still match actual producer and consumer behaviour
- verify async evidence for the final demo:
  - delivery progress updates
  - payment result notification
  - optional order-cancelled notification
- show event payloads clearly in logs or documentation
- add lightweight dead-letter or retry support if feasible

**Definition of done**

- at least one asynchronous workflow is demonstrably real and explainable

---

### Phase 17 -- Report & Evidence Pack

**Goal**

Finish the written deliverable before the final presentation crunch.

**Tasks**

- assemble report sections:
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
- refresh diagrams so they match the actual implementation
- add screenshots, endpoint tables, topic tables, and demo notes
- explain clearly where `Review Service` remains design-only

**Definition of done**

- if coding froze tomorrow, the written deliverable would still be mostly ready

---

### Phase 18 -- Final Presentation Rehearsal

**Goal**

Turn the implemented system into a stable, explainable presentation.

**Tasks**

- define the final demo script with exact click path
- assign speaking responsibilities
- prepare screenshots and fallback recordings
- rehearse explanations of:
  - service ownership
  - `W1`
  - `W2` and `W3`
  - auth and token flow
  - separate data ownership
- keep Sierra-Lima's contribution explicit:
  - Restaurant endpoints
  - Menu endpoints
  - Menu validation role in `W1`
- rehearse short answers to likely questions:
  - why seven business services were implemented
  - why `Review Service` stayed design-only
  - how async integration works in practice
  - how auth is enforced at gateway versus service level
  - why static configuration and no Eureka were acceptable

**Definition of done**

- every presenter knows what to say, what to click, and how to recover if part of the stack misbehaves

---

### Phase 19 -- Buffer & Final Freeze

**Goal**

Use the last session for stabilization only.

**Tasks**

- fix only the highest-risk bugs
- re-run smoke tests
- verify demo seed data still exists
- verify clean startup from the agreed runbook
- freeze the presentation build

**Definition of done**

- no unresolved known issue remains that is likely to break the main demo path

---

## 9. Checkpoint Readiness Gates

### Sierra-Lima-Owned Checkpoint #1 Gate (`2026-05-05`)

Sierra-Lima's part should be considered ready for checkpoint #1 if all of the
following are true, even if teammate-owned services are still catching up:

- `Restaurant Service` and `Menu Service` compile and run cleanly
- both services expose OpenAPI docs and seed-backed demo data
- Restaurant availability and Menu batch validation are working from documented requests
- write routes enforce the agreed auth and ownership rules, or the documented dev-token fallback
- both services can start from the documented local-dev process, including migrations
- Postman requests and fallback payloads exist for the Order team

### Project Checkpoint #1 Gate (`2026-05-05`)

The project should not be considered ready for backend checkpoint #1 unless all
of the following are true:

- the main backend services can start cleanly
- `Restaurant Service` and `Menu Service` are fully operational and protected according to the agreed auth model
- login or token issuance is demonstrable, even if locally stubbed
- `W1` works at least on the happy path
- databases are separated per service
- the backend can be started from a documented local process
- architecture and workflow diagrams match reality

### Sierra-Lima-Owned Checkpoint #2 Gate (`2026-05-12`)

Sierra-Lima's part should be considered ready for checkpoint #2 if all of the
following are true:

- the Sierra-Lima browse flows work through the gateway from the frontend
- restaurant list/detail and menu display use real backend data
- at least one owner-side mutation flow is demonstrable, or explicitly deferred with a documented reason
- loading, empty, and error states exist for the Restaurant/Menu UI
- the frontend no longer depends on hidden manual DB edits for Sierra-Lima flows

### Project Checkpoint #2 Gate (`2026-05-12`)

The project should not be considered ready for frontend/backend checkpoint #2 unless:

- checkpoint #1 criteria still hold
- the frontend signs in and uses real protected APIs
- a user can browse restaurants, inspect menus, and place an order
- the UI handles at least basic loading and error states
- the demo does not rely on hidden manual database manipulation

### Sierra-Lima-Owned Checkpoint #3 Gate (`2026-05-19`)

Sierra-Lima's part should be considered ready for the final presentation if all
of the following are true:

- Restaurant and Menu routes demonstrate the agreed role and ownership checks
- Sierra-Lima can explain the contract role of `availability` and `menu-items/validate` in `W1`
- OpenAPI, example payloads, and demo seed data still match the implementation
- Sierra-Lima has screenshots or fallback evidence for both service flows

### Project Checkpoint #3 Gate (`2026-05-19`)

The project should not be considered ready for the final presentation unless:

- checkpoint #2 criteria still hold
- a role-aware or otherwise visible authorization story is demonstrable
- at least one asynchronous workflow is demonstrably working
- the report is presentation-ready and matches the implementation
- the team has a demo fallback plan

---

## 10. What Sierra-Lima Can Safely Do Early, Even Alone

These are the highest-value early-start tasks for Sierra-Lima in this personal
workspace:

1. complete Phases `0` to `8` without waiting for anyone
2. produce canonical Restaurant and Menu contracts
3. seed compelling restaurant and menu demo data
4. make `POST /menu-items/validate` especially strong because `Order Service` depends on it directly
5. keep both services documented through OpenAPI and Postman
6. prepare stable example IDs, payloads, and failure responses for the Order team
7. set up Docker Compose and the local-dev runbook for the whole team
8. mock token issuance locally if the real User Service is late

**Stubbing strategy while solo**

- issue dev JWTs from a tiny local signer with a shared secret, then swap to the real User Service flow later
- use hardcoded UUIDs for `ownerId` until User Service exists
- skip missing teammate-owned services and call `availability` / `validate` directly from Postman
- test async flow using console consumers once the broker exists

If the rest of the team arrives late, Sierra-Lima's work still remains useful
because it becomes the most stable part of the system.

---

## 11. Risks and Mitigations

### Risk 1 - teammates still work from the old six-service assumption

**Mitigation**

- use this roadmap as the corrected local baseline
- repeat in team coordination that `User Service` is implemented

### Risk 2 - auth is implemented inconsistently across gateway and services

**Mitigation**

- front-load Phase `1`
- agree route protection and token propagation before service hardening

### Risk 3 - Sierra-Lima builds good local services that do not fit the shared auth path

**Mitigation**

- implement Restaurant/Menu against the agreed auth contract, not a solo-only temporary shape

### Risk 4 - static configuration drifts across services

**Mitigation**

- maintain one central local-dev env matrix
- avoid ad hoc per-service URL naming

### Risk 5 - event names or payloads drift across teams

**Mitigation**

- lock contracts in Phase `9`
- keep one shared event-envelope example

### Risk 6 - protected-read behaviour causes late UX debate

**Mitigation**

- decide explicitly in Phase `1` whether browse endpoints remain protected or are intentionally relaxed

### Risk 7 - a shared database shortcut appears during integration

**Mitigation**

- reject it immediately
- keep Restaurant and Menu integrations strictly ID-based

### Risk 8 - diagrams and report fall behind code

**Mitigation**

- update evidence at each checkpoint phase instead of leaving everything to the end

---

## 12. Recommended Success Criteria for the Final Demo

By the time the project reaches final-presentation-ready status, the team
should be able to demonstrate:

1. a customer signs in and browses restaurants and menus
2. creation of an order through the frontend
3. backend coordination across multiple services
4. at least one event-driven update or notification
5. at least one secured and role-aware route or flow
6. clear service boundaries and separate data ownership
7. a report and diagrams that match the implemented system

---

## 13. Bottom Line

The safest route from this repository's current scaffolded state to a credible
final project is:

1. **re-baseline** scope and auth assumptions now that `User Service` is implemented
2. **build Sierra-Lima's two services** properly and protect them from day one
3. **use them as stable anchors** for the rest of the team, especially for the `W1` availability check and batch validation
4. **reach backend integration** by `2026-05-05`
5. **layer in frontend** by `2026-05-12`
6. **finish authorization hardening, async evidence, report, and presentation readiness** by `2026-05-19`

If the team follows the phase order above, early solo work will not be wasted,
and the project will remain aligned with the course assignments, practicals,
and checkpoint structure.

---

## Appendix A -- Suggested Session Calendar

One workable cadence is roughly four to five sessions per week, with the most
Sierra-Lima-heavy work front-loaded before team-wide integration becomes
unavoidable.

| Block | Approx. date | Phase | Notes |
| --- | --- | --- | --- |
| 1 | 2026-04-18 | 0 | Scope freeze and conventions |
| 2 | 2026-04-19 | 1 | Auth + gateway contract alignment |
| 3 | 2026-04-20 | 2A | Contract pack: endpoints, DTOs, validation, schemas |
| 4 | 2026-04-21 | 2B | Local-dev bootstrap, scaffold, env matrix, migrations |
| 5 | 2026-04-22 | 3 | Restaurant foundation |
| 6 | 2026-04-23 | 4 | Restaurant full API + Swagger |
| 7 | 2026-04-24 | 5 | Menu foundation |
| 8 | 2026-04-25 | 6 | Menu full API + Swagger |
| 9 | 2026-04-26 | 7A | Auth integration, owner checks, error model |
| 10 | 2026-04-27 | 7B | Packaging, Postman refresh, docs |
| 11 | 2026-04-28 | 8 | Dockerise both services; consultation day |
| 12 | 2026-04-29 | 9 | Team contract lock |
| 13 | 2026-04-30 | 10A | Direct `W1` contract smoke for availability and validation |
| 14 | 2026-05-01 | 10B | Gateway path, failure-path verification, demo cleanup |
| 15 | 2026-05-03 | 11A | Startup rehearsal, smoke checks, env cleanup |
| 16 | 2026-05-04 | 11B | Checkpoint story, evidence, fallback notes |
| -- | 2026-05-05 | -- | Checkpoint #1 (backend) |
| 17 | 2026-05-06 | 12 | Frontend shell + sign-in |
| 18 | 2026-05-07 | 13 | Restaurant + Menu UX |
| 19 | 2026-05-10 | 14 | Frontend-backend integration + checkpoint #2 prep |
| -- | 2026-05-12 | -- | Checkpoint #2 (frontend + backend) |
| 20 | 2026-05-13 | 15 | Authorization hardening |
| 21 | 2026-05-14 | 16 | Async evidence + smoke |
| 22 | 2026-05-16 | 17 | Report + evidence pack |
| 23 | 2026-05-17 | 18 | Final rehearsal |
| 24 | 2026-05-18 | 19 | Buffer + freeze |
| -- | 2026-05-19 | -- | Checkpoint #3 (final presentation) |

### Compression guidance

If any phase slips, compress in this priority order:

1. drop optional extras before dropping core contracts or hardening
2. merge paired implementation phases only if confidence is already high
3. never compress auth-contract alignment, team contract lock, checkpoint prep, or final buffer

---

## Appendix B -- Team Coordination Points

| When | What must be agreed | Who |
| --- | --- | --- |
| Before Phase 1 | Java version, build tool, package structure, naming rules | all |
| During Phase 1 | public vs protected routes, JWT claims, token propagation | all |
| Before Phase 2 | gateway path prefixes, port assignments, env-var matrix | all |
| Before Phase 8 | Docker Compose service names, network, shared volumes | backend owners |
| Before Phase 9 | exact REST payloads for W1 hops, event topics and payloads | backend owners |
| Before Phase 10 | status-code contract for validation failures | backend owners |
| Before Phase 15 | role-gating matrix and ownership-check approach | all |
| Before Phase 18 | slide structure, demo path, fallback plan, speaking order | all |

When teammates are late, Sierra-Lima can still move by using stable IDs,
seeded demo data, locally issued dev tokens, documented assumptions, and
contract-first stubs, then replacing those stubs once the real services arrive.

---

## Appendix C -- Assignment Feedback to Address

From Assignment 1 feedback (`3.50 / 4.00`):

> `-0.25`: infrastructure elements were mixed into the architecture diagram.
>
> `-0.25`: the database was shared across microservices.

**Actions taken in this roadmap**

- business architecture and technical architecture remain explicitly separate
- each implemented microservice keeps its own database
- gateway, broker, and other infrastructure belong to the technical runtime view, not the business architecture view

---

## Appendix D -- Risk Register

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Team mobilizes late | integration delays | front-load Sierra-Lima services and contract-first work |
| Teammates still work from the old six-service assumption | wrong scope and wrong integration decisions | use this roadmap as the corrected baseline in team coordination |
| Auth scope remains fuzzy | rework and broken demos | front-load Phase 1 and document route protection, token flow, and claims shape |
| Sierra-Lima services drift away from the shared auth model | rework after checkpoint #1 | implement against the agreed auth contract, not a solo-only shortcut |
| Static URLs drift | runtime breakage | maintain one central env matrix |
| Event contracts drift | consumer failures | lock contracts in Phase 9 and keep a shared envelope example |
| Protected-read behaviour causes late UX debate | frontend churn close to checkpoint #2 | decide it explicitly in Phase 1 and log the decision |
| Shared DB shortcut appears | repeat of Assignment 1 penalty | reject immediately and keep integrations ID-based only |
| Docker / Kafka issues on Windows | blocks multiple phases | allocate time in local-dev and keep WSL2 fallback in mind |
| Assignment 3 facts change after feedback or consultation | endpoint or model rework | keep services small, document decisions, and update Appendix E first |
| Scope expands back toward all eight services | time pressure and diluted ownership | hold the seven-service implementation subset unless explicitly told otherwise |
| Report falls behind code | last-minute scramble | build evidence at each checkpoint phase |
| Checkpoint demo fails live | lost marks despite working code | keep screenshots, recordings, and fallback commands ready |

---

## Appendix E -- Canonical A3 Reference

This appendix pins the Assignment 3 design facts that drive the phase plan, so
the file can be used without round-tripping back to the PDFs. If a conflict is
found between this appendix and the committed `Assignment-3-Submission` files
at `aac68b0`, the assignment files are authoritative and this appendix should
be updated.

### E.1 Restaurant Service endpoints

Working default for the personal workspace:

- browse reads are public
- availability accepts `Customer` or `Service`
- writes require `RestaurantOwner` or `Admin`
- ownership checks compare JWT `userId` against `Restaurant.ownerId`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/restaurants` | Register a new restaurant profile | RestaurantOwner / Admin |
| GET | `/restaurants/{id}` | Get a restaurant profile | Public |
| PUT | `/restaurants/{id}` | Update restaurant profile | RestaurantOwner / Admin |
| PATCH | `/restaurants/{id}/status` | Change open/closed status | RestaurantOwner / Admin |
| GET | `/restaurants` | Search/list restaurants | Public |
| GET | `/restaurants/{id}/availability` | Lightweight availability check for `W1` | Customer / Service |

**Availability response shape**

```json
{
  "restaurantId": "uuid",
  "isOpen": true,
  "acceptsOrders": true,
  "operatingHours": "09:00-22:00",
  "checkedAt": "2026-05-05T12:34:56Z"
}
```

### E.2 Menu Service endpoints

Working default for the personal workspace:

- browse reads are public
- batch validation accepts `Customer` or `Service`
- writes require `RestaurantOwner` or `Admin`
- ownership checks compare JWT `userId` against the owner of the referenced restaurant

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/restaurants/{rid}/menu-items` | Add a new menu item | RestaurantOwner / Admin |
| GET | `/restaurants/{rid}/menu-items` | List menu items for a restaurant | Public |
| GET | `/menu-items/{id}` | Get a single menu item | Public |
| PUT | `/menu-items/{id}` | Update a menu item | RestaurantOwner / Admin |
| DELETE | `/menu-items/{id}` | Remove a menu item | RestaurantOwner / Admin |
| POST | `/menu-items/validate` | Batch validate items for `W1` | Customer / Service |

**Batch validation request shape**

```json
{
  "items": [
    { "menuItemId": "uuid", "quantity": 2 },
    { "menuItemId": "uuid", "quantity": 1 }
  ]
}
```

**Batch validation response shape**

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

### E.3 Restaurant Service data model

| Field | Type | Notes |
| --- | --- | --- |
| `restaurantId` | UUID | PK |
| `ownerId` | UUID | cross-service ref to `User.userId` |
| `name` | String | not blank |
| `address` | String | |
| `city` | String | |
| `latitude` | Double | -90..90 |
| `longitude` | Double | -180..180 |
| `operatingHours` | String | e.g. `09:00-22:00` |
| `isOpen` | Boolean | default `false` |
| `createdAt` | LocalDateTime | audited |
| `updatedAt` | LocalDateTime | audited |

### E.4 Menu Service data model

| Field | Type | Notes |
| --- | --- | --- |
| `menuItemId` | UUID | PK |
| `restaurantId` | UUID | cross-service ref to `Restaurant.restaurantId` |
| `name` | String | not blank |
| `description` | String | optional |
| `priceAmount` | BigDecimal | scale 2, positive |
| `priceCurrency` | String | ISO-4217, default `EUR` |
| `category` | String | e.g. `Appetizer`, `Main`, `Dessert`, `Drink` |
| `isAvailable` | Boolean | default `true` |
| `createdAt` | LocalDateTime | audited |
| `updatedAt` | LocalDateTime | audited |

### E.5 Gateway path map

| Gateway prefix | Target service | Owner |
| --- | --- | --- |
| `/api/auth/**`, `/api/users/**` | User Service | Alfa-Kilo |
| `/api/orders/**` | Order Service | Alfa-Kilo |
| `/api/restaurants/**` | Restaurant Service | Sierra-Lima |
| `/api/menu-items/**`, `/api/restaurants/*/menu-items/**` | Menu Service | Sierra-Lima |
| `/api/payments/**` | Payment Service | Elephant-Yankee |
| `/api/deliveries/**`, `/api/drivers/**` | Delivery Service | Elephant-Yankee |
| `/api/notifications/**` | Notification Service | Mike-Alfa |

### E.6 W1 synchronous call chain

1. `Client -> API Gateway`: `POST /api/orders` with `Authorization: Bearer <token>`.
2. `Gateway -> Order Service`: request is forwarded after token checks.
3. `Order -> User Service`: verify customer exists and is active.
4. `Order -> Restaurant Service`: `GET /restaurants/{id}/availability`.
5. `Order -> Menu Service`: `POST /menu-items/validate`.
6. `Order Service` persists the order with status `Placed`.
7. `Order -> Payment Service`: synchronous charge.
8. On success, `Order -> Delivery Service`: create delivery task.
9. `Order Service` returns `201 Created`.

**Failure handling**

- invalid customer: `401` or equivalent auth failure
- closed restaurant: `409` or explicit non-accepting availability result
- invalid or unavailable menu items: `422`
- payment failure: order cancelled, no delivery created
- delivery creation failure: compensation or refund path is triggered

### E.7 W2 / W3 event contracts

**Topic:** `payment-events`

| Event | Producer | Consumer(s) | Payload |
| --- | --- | --- | --- |
| `payment.completed` | Payment Service | Notification Service | `{ paymentId, orderId, amount, occurredAt }` |
| `payment.failed` | Payment Service | Notification Service, Order Service | `{ paymentId, orderId, reason, occurredAt }` |

**Topic:** `delivery-events`

| Event | Producer | Consumer(s) | Payload |
| --- | --- | --- | --- |
| `delivery.status-changed` | Delivery Service | Order Service, Notification Service | `{ deliveryId, orderId, status, occurredAt }` |

**Topic:** `order-events` (optional)

| Event | Producer | Consumer(s) | Payload |
| --- | --- | --- | --- |
| `order.cancelled` | Order Service | Notification Service and optionally other listeners | `{ orderId, reason, occurredAt }` |

**Envelope shape:** `{ id, type, occurredAt, payload }`

**Delivery guarantees**

- at-least-once delivery
- idempotent consumers
- failed handlers routed to a dead-letter path where feasible

### E.8 Other data-model quick references

Relevant to `W1`:

- `User Service`: users, addresses, driver profiles
- `Order Service`: orders and order items
- `Payment Service`: payments and transactions
- `Delivery Service`: delivery tasks
- `Notification Service`: notifications

All cross-service references remain ID-only. No foreign key should cross
database boundaries.

---

*End of roadmap. Next step: execute Phase 0 in the next working session.*
