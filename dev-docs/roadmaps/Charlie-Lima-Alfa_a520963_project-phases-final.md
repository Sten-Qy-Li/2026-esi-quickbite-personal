# QuickBite ESI Project -- Final Master Plan for Sierra-Lima

**Author callsign:** Charlie-Lima-Alfa  
**Base commit:** `a520963`  
**Student callsign:** Sierra-Lima  
**Owned services:** Restaurant Service, Menu Service  
**Team (Group 7):** Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa  
**Date created:** 2026-04-18  
**Status:** Final. This file supersedes all earlier phased roadmaps in
`dev-docs/roadmaps/` for Sierra-Lima's personal plan.

---

## 0. How to Use This Document

This is a **master plan** for the part of the QuickBite ESI project that is
under Sierra-Lima's ownership. It is written so that any disciplined coding
agent (or Sierra-Lima directly) can pick it up and drive the work to
final-submission readiness without needing to re-read the original assignment
artifacts.

### 0.1 Reading order

Read in this order the first time:

1. Sections 1 to 7 -- context, scope, principles, strategy, stack, defaults.
2. Section 8 -- the phase map at a glance.
3. The current phase in Section 9 only.
4. Appendix F (canonical A3 reference) when you need the exact contract.
5. The rest of the appendices only when the current phase points to them.

### 0.2 Source artifacts

The authoritative inputs behind this plan are stored in this repository at
commit `a520963`:

- `dev-docs/prior-submissions/Assignment-1-Submission.pdf` -- original system
  definition (8 business services, early architecture).
- `dev-docs/prior-submissions/Assignment-1_Feedback.txt` -- 3.50/4.00, with
  two penalties: infrastructure mixed into the architecture diagram, shared
  database across microservices. Both penalties are addressed by this plan.
- `dev-docs/prior-submissions/Assignment-2-Submission.pdf` -- DDD domain
  model, aggregate roots, value objects, cross-service references as IDs.
- `dev-docs/prior-submissions/Assignment-3-Submission.pdf` (and `.docx`) --
  final design baseline. Defines REST endpoints, data models, workflows,
  integration style, and implementation responsibilities.
- `dev-docs/prior-submissions/assignment-3_figure1_business-architecture.png`
  -- business architecture (no infrastructure).
- `dev-docs/prior-submissions/assignment-3_figure1b_implementation-architecture.png`
  -- technical runtime view (gateway, Kafka, DBs).
- `dev-docs/prior-submissions/assignment-3_figure2_service-er-diagrams.png`
  -- ER diagrams for all services.
- `dev-docs/prior-submissions/assignment-3_figure3_workflow-w1-sequence.png`
  -- W1 sequence.
- `dev-docs/prior-submissions/assignment-3_figure4_workflow-w2-w3-events.png`
  -- W2 and W3 event flow.
- `dev-docs/course-materials/` -- official assignments, lecture slides, and
  course introduction, for reference if a phase needs a reminder of what the
  course actually asked for.

The critical contract content (endpoints, payloads, fields, events) is
pinned in **Appendix F**. If Appendix F and the A3 PDF disagree, the PDF is
authoritative -- update Appendix F first, then the relevant phase.

### 0.3 Writing style

The plan is written in plain, B1-level English. Short sentences are preferred
over long ones. Technical terms are used where they matter; everything else
is kept simple. Imperative tone ("add", "create", "verify") is used in task
lists.

### 0.4 How to treat time estimates

Each Phase is a **focused 3-hour working session**. Some Phases are marked
with `1 session` (plan for 3 hours). A few are marked `1-2 sessions` (plan
for 3 to 6 hours). These are working estimates for a student with
intermediate Spring Boot experience; add buffer if you are less experienced.

---

## 1. Ownership Scope and Key Dates

### 1.1 What Sierra-Lima must deliver

Sierra-Lima owns two business microservices of the QuickBite system:

| Service | Owned requirements | Summary |
|---------|-------------------|---------|
| **Restaurant Service** | R19 (register/manage restaurant), R20 (update open/closed and operating hours) | Stores restaurant profiles. Tells Order Service whether a restaurant can accept orders. |
| **Menu Service** | R21 (add/update/remove menu items), R22 (browse menu) | Stores menu items. Validates a cart during order placement. |

"Thoroughly completed" means the scope below is done, documented, and
demonstrable at the final presentation on **2026-05-19**:

- Both services run in Docker containers with their own PostgreSQL database.
- Every endpoint from Appendix F Tables F.1 and F.2 is implemented, validated,
  protected, and documented through Swagger.
- The **synchronous** integration that W1 relies on (availability check +
  batch menu validation) works reliably against Order Service.
- The services behave correctly when the rest of the system uses Kafka for
  W2 and W3 (see Section 6), even though Sierra-Lima is not a producer or
  consumer in the baseline A3 scope.
- Authentication (bearer token) and role-based authorisation work on both
  services.
- A Vue.js frontend allows a signed-in user to browse restaurants and menus,
  and -- for restaurant owners -- to create and update their own data.
- A final report covers all the sections listed in Phase 18, with diagrams
  that match the real code.
- A live demo can be performed end-to-end without manual database edits.

### 1.2 Key dates

| Date | Milestone | Focus |
|------|-----------|-------|
| 2026-04-18 (Sat) | Plan start | Today. Base commit `a520963`. |
| 2026-04-21 (Tue) | Project description practical | Official project spec released by instructor. |
| 2026-04-28 (Tue) | Project consultation practical | Clarify scope with instructor; the practicals page shows `28/04/2025` but this is treated as a typo. |
| **2026-05-05 (Tue)** | **Checkpoint #1** | **Backend demo (services, APIs, persistence, auth, W1).** |
| **2026-05-12 (Tue)** | **Checkpoint #2** | **Frontend + backend demo (end-to-end browser flow).** |
| **2026-05-19 (Tue)** | **Checkpoint #3** | **Final presentation (all workflows, security, report).** |
| 2026-05-25 (Mon) | Exam 1 | Written exam. |
| 2026-06-08 | Exam 2 | Retake window. |
| 2026-06-22 | Resit | Final resit. |

### 1.3 Grading context (informative)

- Assignments: 20 points (A1: 4, A2: 8, A3: 8).
- Project: 30 points (across the three checkpoints).
- Exam: 50 points (minimum 21 to pass).
- Total: 100 points (minimum 51 to pass).
- Assignment 1 score so far: 3.50/4.00 (feedback addressed in Appendix E).

---

## 2. Planning Principles

These are the rules every Phase follows. When a Phase seems to pull in a
different direction, one of these principles should win.

1. **Follow Assignments 1 to 3.** Do not reinvent the system. If a Phase feels
   tempted to add a new feature, check it against Appendix F first.
2. **Business and technical diagrams stay separate.** Do not mix
   infrastructure (API Gateway, Kafka, DB containers) into a business
   architecture diagram. This repeats the Assignment 1 penalty.
3. **One database per implemented microservice.** No shared DB. Cross-service
   references are stored as IDs only, without database-level foreign keys.
4. **Demonstrate both interaction styles.** Synchronous REST through W1.
   Asynchronous Kafka through W2 and W3. Both must be visible at the final
   presentation.
5. **Bake in auth from the first endpoint.** Do not build "open CRUD now, add
   security later". Build "JWT stub from day one, real role checks hardened
   later".
6. **Keep each service small.** ~5 to 8 endpoints per service, in line with
   the Assignment 3 guideline.
7. **Avoid infrastructure inflation.** Eureka service discovery, client-side
   load balancing, and similar extras are out of scope unless the instructor
   explicitly asks for them.
8. **Front-load solo-capable work.** Teammates may start late. Build contracts,
   schemas, Docker Compose, OpenAPI, and seed data first -- these are useful
   to the team even if Sierra-Lima is ahead of schedule.
9. **Reject shortcuts.** If someone suggests a shared DB during integration,
   or "skip auth for now, turn it on later", or "drop Swagger, we have
   Postman", **say no**. These shortcuts all have a real cost.
10. **Track decisions.** Every choice that is not obvious (auth shape, error
    envelope, port numbers, migration strategy) is logged in
    `dev-docs/decisions/` the same day it is made.

---

## 3. Delivery Strategy

The repository today is scaffolded but empty: three service folders
(`restaurant-service/`, `menu-service/`, `local-dev/`) each only contain a
`README.md`. Teammates may join the effort late. To turn that into a
final-submission-ready project, this plan uses a six-step strategy:

1. **Re-baseline.** Freeze scope, ownership, auth model, and workflows first
   (Phases 0-1).
2. **Contract & bootstrap.** Pin endpoints, payloads, data schemas, local-dev
   environment (Phase 2).
3. **Build Sierra-Lima services.** Restaurant then Menu, both protected
   from day one (Phases 3-8).
4. **Lock team contracts and assemble W1.** Prevent late integration drift
   (Phases 9-10).
5. **Polish and checkpoint cycle.** CP#1 backend, then frontend, then CP#2
   full-stack (Phases 11-14).
6. **Harden and finalise.** Authorisation, async evidence, report, rehearsal,
   buffer, CP#3 (Phases 15-19).

### 3.1 Cross-phase working assets

These assets stay **live** throughout the project. They are started in the
early phases and updated at every phase boundary, not created the week before
the presentation.

- **Postman collection** -- one shared collection rooted at `POST /auth/login`,
  then folders for Restaurant CRUD, Menu CRUD, W1 integration, and
  negative-auth cases. Environment variables hold the base URL and the
  current JWT.
- **Swagger / OpenAPI UI** -- live for both services at
  `http://localhost:8081/swagger-ui.html` and `http://localhost:8082/swagger-ui.html`.
- **Seed data** -- one Flyway migration (`V2__seed_demo_data.sql`) or a
  `CommandLineRunner` per service loads 4-6 demo restaurants and
  12-18 menu items with realistic names, cities, and categories.
- **Docker Compose runbook** -- `services/local-dev/README.md` documents
  `docker compose up --build`, environment variables, port mapping, and the
  expected success state.
- **Health / ping endpoints** -- `/actuator/health` exposed on every service,
  plus a simple `/ping` if needed for non-Spring callers.
- **Demo backups** -- at each checkpoint, record a 2-3 minute screen capture
  of the demo flow in case the live demo fails.
- **Decisions log** -- `dev-docs/decisions/NNNN-short-title.md` files.

---

## 4. Baseline Project Scope

### 4.1 Business system

QuickBite is a food-delivery platform with **eight** business services in
the overall business architecture:

1. `User Service`
2. `Order Service`
3. `Menu Service`
4. `Restaurant Service`
5. `Delivery Service`
6. `Payment Service`
7. `Notification Service`
8. `Review Service`

### 4.2 Implementation subset (from Assignment 3 §2.4)

| Category | Components |
|----------|-----------|
| Implemented business services | `Order`, `User`, `Restaurant`, `Menu`, `Payment`, `Delivery`, `Notification` |
| Implemented shared components | `API Gateway`, `Event Broker configuration` (Kafka) |
| Design-only (not coded) | `Review Service` |

### 4.3 Team ownership (from Assignment 3 §7)

| Team member | Owned components |
|-------------|------------------|
| Alfa-Kilo | `Order Service`, `User Service`, `API Gateway` |
| **Sierra-Lima** | **`Restaurant Service`, `Menu Service`** |
| Elephant-Yankee | `Payment Service`, `Delivery Service` |
| Mike-Alfa | `Notification Service`, `Event Broker configuration` |

Sierra-Lima owns two business services. Sierra-Lima does **not** replace
either with a shared integration or resilience component. This is a hard
constraint from Assignment 3.

### 4.4 Sierra-Lima domain recap

**Restaurant Service** (requirements R19 register/manage restaurant, R20
update status/operating hours)

- Aggregate root: `Restaurant` with fields `restaurantId`, `ownerId*`,
  `name`, `address`, `city`, `latitude`, `longitude`, `operatingHours`,
  `isOpen`.
- `ownerId*` is a cross-service reference to `User.userId`. Store it as a
  UUID; do **not** create a database foreign key.
- The address fields can be embedded as a `Location` value object
  (`@Embeddable`) to follow the DDD model from Assignment 2.
- Database: `restaurant_db` (its own PostgreSQL container).

**Menu Service** (requirements R21 add/update/remove items, R22 browse menu)

- Aggregate root: `MenuItem` with fields `menuItemId`, `restaurantId*`,
  `name`, `description`, `priceAmount`, `priceCurrency`, `category`,
  `isAvailable`.
- `restaurantId*` is a cross-service reference to `Restaurant.restaurantId`.
  Again, stored as a UUID; no FK.
- The price fields can be embedded as a `Price` value object
  (`@Embeddable`).
- Database: `menu_db` (its own PostgreSQL container).

### 4.5 Design decisions to honour

- **Service-local databases.** Each implemented service owns one PostgreSQL
  database in its own container. Assignment 1 feedback already penalised a
  shared-DB pattern; do not repeat that mistake.
- **Cross-service references are ID-only.** No cross-DB joins. No FK to a
  table in another service.
- **Auth is baseline, not optional.** Public routes are only `POST /users`
  and `POST /auth/login` (owned by Alfa-Kilo). Every other endpoint requires
  a valid bearer token validated locally by each service.
- **Static service configuration.** Downstream URLs live in environment
  variables. No Eureka.
- **Both interaction styles must ship.** W1 is the synchronous proof.
  W2 or W3 (ideally both) is the asynchronous proof.
- **If time collapses, one Sierra-Lima service can be reduced to read-only
  + seed data, but both must still ship** because both participate in W1.

---

## 5. Technology Stack

| Layer | Technology | Notes |
|-------|-----------|-------|
| Backend framework | Spring Boot 3.x (Java) | Maven; aligns with course practicals PS0x1/PS0x2. |
| Java version | **17 (default)** or 21 | Course practicals (PS0xx) use Java 17; raise to 21 only if the full team agrees. |
| Database | PostgreSQL 15 | One database per service in its own container. |
| Schema migrations | **Flyway** | One migration history per service. After the first migration is in place, prefer `spring.jpa.hibernate.ddl-auto=validate`. |
| Containerisation | Docker + Docker Compose | Local reproducibility is mandatory. |
| Service discovery | None in baseline | Eureka only if the team consciously adds scope. |
| API gateway | Spring Cloud Gateway | Owned by Alfa-Kilo; Sierra-Lima integrates through it. |
| Async messaging | Apache Kafka | Owned by Mike-Alfa; Sierra-Lima is neither producer nor consumer in A3 scope. See Section 6 and Phase 16. |
| Resilience | Resilience4j | **Optional**. Only add after core W1 is stable. |
| Auth | Spring Security + JWT (`jjwt` 0.11.5) | Token issued by User Service; validated locally in each service. |
| Frontend | Vue.js 3 (Vue CLI, Vue Router, Fetch API) | Matches course practicals PS091-PS121. |
| API documentation | OpenAPI / Swagger (`springdoc-openapi-starter-webmvc-ui`) | Live from Phase 4 onward. |
| API testing | Postman | One shared collection. |

### 5.1 Working implementation defaults for Sierra-Lima

These defaults are used **unless the team explicitly changes them** during
Phase 1. They exist so Sierra-Lima can start coding before every team-level
auth detail is finalised.

1. **Public browse routes.**  
   `GET /restaurants`, `GET /restaurants/{id}`,
   `GET /restaurants/{rid}/menu-items`, and `GET /menu-items/{id}` are public
   by default. If the team later changes them to customer-only, tighten the
   security filter -- do not change DTOs or business logic.
2. **Service-callable verification routes.**  
   `GET /restaurants/{id}/availability` and `POST /menu-items/validate`
   accept a customer token or a service token.
3. **Write routes require role.**  
   All `POST`, `PUT`, `PATCH`, and `DELETE` routes require `RestaurantOwner`
   or `Admin`.
4. **Ownership check.**  
   Ownership is enforced by comparing the authenticated `userId` claim
   against `Restaurant.ownerId`. For menu mutations, ownership is checked
   against the owning restaurant.
5. **Expected JWT claims.**  
   Minimum claims: `sub`, `userId`, `role`. Optional: `tokenType`,
   `serviceName` (for internal service calls). The gateway may forward
   convenience headers such as `X-User-Id`, but JWT claims are the source
   of truth inside the service.
6. **Flyway-first schema management.**  
   The very first persistent version of each service ships with a Flyway
   migration (`V1__init.sql`). After that, prefer `ddl-auto=validate` in
   shared demos so schema drift is caught fast.

These defaults are deliberately concrete.

---

## 6. Named Workflows (W1, W2, W3)

The plan uses three workflow labels throughout. Together they demonstrate
the Assignment 3 requirement of both synchronous and asynchronous
integration. The exact contract is pinned in Appendix F.

| Label | Name | Style | Summary |
|-------|------|-------|---------|
| **W1** | Place Order | Synchronous REST | Client → Gateway → Order → {User, Restaurant, Menu, Payment, Delivery}. All calls use bearer tokens. Sierra-Lima participates in steps 4 and 5. |
| **W2** | Delivery Progress & Notifications | Asynchronous Kafka | Delivery Service publishes `delivery.status-changed` on `delivery-events`. Order Service and Notification Service consume. |
| **W3** | Payment Outcome Notification | Asynchronous Kafka | Payment Service publishes `payment.completed` and `payment.failed` on `payment-events`. Notification Service always consumes. Order Service consumes failure events. |

**Sierra-Lima's direct role:**

- W1: **callee only.** Restaurant Service exposes
  `GET /restaurants/{id}/availability`. Menu Service exposes
  `POST /menu-items/validate`. Both must respond correctly under normal and
  failure conditions.
- W2 / W3: **not a producer or consumer** in the baseline A3 scope.
  Sierra-Lima's services must stay stable while teammates run these flows.
  Optional stretch: Menu Service publishing `menu.item-availability-changed`
  on a new `menu-events` topic (Phase 16).

---

## 7. Explicit Assumptions

These assumptions are stated here so any agent resuming the plan can check
them before proceeding.

1. **Today is 2026-04-18 (Saturday).** The calendar in Appendix A is anchored
   to this date.
2. **Base commit is `a520963`** on branch `dev`. All referenced paths are
   relative to the repository root at that commit.
3. **Sierra-Lima uses Windows 11** with bash (Git for Windows) as the primary
   shell, Docker Desktop with WSL2 backend, and Windows-style absolute paths
   where necessary. Unix shell syntax is used inside scripts.
4. **Each Phase is a 3-hour working session.** Two-session phases are marked.
5. **The team repository is separate** from this personal repo. Work here is
   designed to transfer cleanly into the shared repo without requiring
   destructive changes.
6. **Teammates may join late.** Every Phase up to Phase 8 is designed to make
   progress without any other teammate's code.
7. **Review Service stays design-only** unless the instructor explicitly asks
   for it at the 2026-04-21 or 2026-04-28 sessions.
8. **The 2025/26 assignment design is final.** If Assignment 3 is revised
   after 2026-04-21, update Appendix F first, then any phase that references
   the changed contract.
9. **HS256 shared secret is acceptable for dev tokens.** The User Service is
   expected to ship a production-ready key in a later sprint; the env-var
   names (`JWT_SECRET`, `JWT_ISSUER`) remain the same.
10. **Spring Initializr output is the starting point** for each service.
    Generated code is committed as-is, then evolved Phase by Phase.

If any of these assumptions becomes false, flag it at the top of the
current phase in `dev-docs/decisions/` and re-plan.

---

## 8. Phase Map at a Glance

The plan is 20 Phases (Phase 0 to Phase 19). Total effort: about 22-24
three-hour sessions (66-72 hours of focused work). The calendar in
Appendix A fits this comfortably between 2026-04-18 and 2026-05-18.

| Phase | Title | Checkpoint Target | Est. Effort |
|-------|-------|-------------------|-------------|
| 0 | Scope Freeze & Repo Conventions | -- | 1 session |
| 1 | Auth & Gateway Contract Alignment | -- | 1 session |
| 2 | Contract Pack & Local-Dev Bootstrap | -- | 1-2 sessions |
| 3 | Restaurant Service -- Foundation | CP#1 | 1 session |
| 4 | Restaurant Service -- Full API, Validation, OpenAPI | CP#1 | 1 session |
| 5 | Menu Service -- Foundation | CP#1 | 1 session |
| 6 | Menu Service -- Full API, Validation, OpenAPI | CP#1 | 1 session |
| 7 | Sierra-Lima Hardening Pass | CP#1 | 1-2 sessions |
| 8 | Dockerise Both Services | CP#1 | 1 session |
| 9 | Team Contract Lock for W1 / W2 / W3 | CP#1 | 1 session |
| 10 | W1 Integration & Failure-Path Protection | CP#1 | 1-2 sessions |
| 11 | Backend Polish & Checkpoint #1 Prep | CP#1 | 1 session |
| 12 | Vue.js Frontend -- Shell, Routing & Sign-In | CP#2 | 1 session |
| 13 | Vue.js Frontend -- Restaurant & Menu UX | CP#2 | 1 session |
| 14 | Frontend-Backend Integration & Checkpoint #2 Prep | CP#2 | 1 session |
| 15 | Authorisation Hardening & Role-Aware Behaviour | CP#3 | 1 session |
| 16 | Async Evidence & Cross-Service Smoke | CP#3 | 1 session |
| 17 | Report & Evidence Pack | CP#3 | 1 session |
| 18 | Final Presentation Rehearsal | CP#3 | 1 session |
| 19 | Buffer & Final Freeze | CP#3 | 1 session |

If a phase slips, see *Appendix B -- Compression Guidance*.

---

## 9. Detailed Phase Plan

Each Phase below has a single **Goal**, a list of **Prerequisites** (previous
phases and teammate dependencies), **Tasks** with concrete acceptance
criteria, and a **Definition of Done** checklist.

---

### Phase 0 -- Scope Freeze & Repo Conventions

- **Goal.** Turn the updated Assignment 3 outputs into one implementation
  baseline that no team member can later dispute. Produce a short set of
  conventions so every file created from Phase 2 onward has a consistent
  shape.
- **Prerequisites.** None. This is the first phase.
- **Estimated effort.** 1 session.

#### Tasks

1. **Reconfirm the implementation subset** (see §4.2). Record in
   `dev-docs/decisions/0001-scope-freeze.md`.
   - Implemented: `Order`, `User`, `Restaurant`, `Menu`, `Payment`,
     `Delivery`, `Notification`.
   - Shared: `API Gateway` (Alfa-Kilo), `Event Broker` (Mike-Alfa).
   - Design-only: `Review Service`.
   - *Acceptance:* a written statement exists and is committed.
2. **Freeze the named workflows W1 / W2 / W3** (see §6 and Appendix F).
   - *Acceptance:* one paragraph per workflow in
     `dev-docs/decisions/0002-workflows.md`.
3. **Define the services folder layout.** Standard layout:
   ```
   services/
     restaurant-service/    <- Maven project root
     menu-service/          <- Maven project root
     local-dev/             <- docker-compose.yml, .env.example, runbook
   ```
   - *Acceptance:* README comments reflect this layout.
4. **Decide conventions** and record them in
   `dev-docs/decisions/0003-conventions.md`:
   - Git: `dev` for daily work, feature branches off `dev`, `main` for
     release milestones.
   - Commit messages: `<type>: <subject>` where type is `feat`, `fix`,
     `chore`, `docs`, `refactor`, `test`.
   - Java version: 17.
   - Maven groupId: `ee.ut.esi.quickbite`. Package base:
     `ee.ut.esi.quickbite.<service>`.
   - Docker image naming: `quickbite-<service>:dev` for local images.
   - Env-var naming: SCREAMING_SNAKE_CASE, grouped by prefix (`DB_`, `JWT_`,
     `RESTAURANT_SERVICE_`, `MENU_SERVICE_`).
   - *Acceptance:* doc exists and is referenced from Phase 2.
5. **Record open design questions.** Topics likely to be raised: error
   envelope shape, categorisation vocabulary for menu items, whether to
   use HATEOAS, whether to use `Location` / `Price` as Embeddables.
   - *Acceptance:* a list in `dev-docs/decisions/0004-open-questions.md`.
6. **Produce a non-goals list** for the first implementation pass:
   - No frontend before CP#1 prep.
   - No real payment gateway integration.
   - No mobile app.
   - No Eureka.
   - No second data store per service.
   - *Acceptance:* a list in `dev-docs/decisions/0005-non-goals.md`.

#### Definition of Done

- [ ] Implementation subset confirmed in writing.
- [ ] Workflow labels W1/W2/W3 frozen.
- [ ] Folder layout agreed.
- [ ] Conventions documented (Git, naming, env vars).
- [ ] Non-goals list exists.
- [ ] No open scope debate blocking Phase 1.

---

### Phase 1 -- Auth & Gateway Contract Alignment

- **Goal.** Remove the largest risk from the updated Assignment 3: unclear
  auth and route-protection behaviour. After this phase, Sierra-Lima can
  implement Restaurant and Menu endpoints without guessing how auth arrives
  or which routes are public.
- **Prerequisites.** Phase 0.
- **Estimated effort.** 1 session.
- **Team dependency.** Decisions here benefit from input from Alfa-Kilo
  (who owns User Service + Gateway). If Alfa-Kilo is not available, use
  the defaults in §5.1 and note any unilateral choices in the decisions
  log.

#### Tasks

1. **Confirm the public route list.**
   - `POST /users` -- self-registration, owned by User Service.
   - `POST /auth/login` -- token issuance, owned by User Service.
   - *Acceptance:* both routes listed in
     `dev-docs/decisions/0010-auth-contract.md`.
2. **Confirm the default protected-route rule.** Every other implemented
   endpoint requires a valid bearer token and at least the `Customer` role,
   unless a stricter role is specified.
   - *Acceptance:* rule recorded.
3. **Agree gateway path prefixes.** (See Appendix F Table F.5.) Each
   service gets one or two prefixes; Sierra-Lima has:
   - `/api/restaurants/**` → Restaurant Service.
   - `/api/menu-items/**` and `/api/restaurants/*/menu-items/**` → Menu Service.
   - *Acceptance:* all prefixes documented.
4. **Agree the token-propagation model.**
   - Client → Gateway carries `Authorization: Bearer <token>`.
   - Gateway validates the token, routes the request, and forwards the
     `Authorization` header (and optional `X-User-Id`) downstream.
   - Each downstream service validates the token locally (no shared
     session).
   - Service-to-service REST calls also carry a bearer token -- either the
     original caller's token, or a dedicated service token (signed the same
     way, with `tokenType=SERVICE` claim).
   - *Acceptance:* decision recorded; diagram sketch in the decisions log.
5. **Document the identity context Sierra-Lima services need.**
   - `userId` (UUID) -- identity of the caller.
   - `role` (`Customer` | `Driver` | `RestaurantOwner` | `Admin`).
   - Optional `tokenType` and `serviceName` for internal calls.
   - *Acceptance:* these fields are listed and locked.
6. **Decide browse-route protection.** Confirm whether `GET /restaurants`,
   `GET /restaurants/{id}`, `GET /restaurants/{rid}/menu-items`, and
   `GET /menu-items/{id}` stay public (per §5.1 default) or require
   `Customer`. Record the decision.
   - *Acceptance:* one sentence decision.
7. **Record the JWT claims shape** so Sierra-Lima can mock it locally before
   the real User Service exists:
   ```json
   {
     "sub": "dev-user-001",
     "userId": "00000000-0000-0000-0000-000000000001",
     "role": "RestaurantOwner",
     "tokenType": "USER",
     "iat": 1746432000,
     "exp": 1746435600
   }
   ```
   - *Acceptance:* example payload committed.

#### Outputs

- Auth contract sheet (written).
- Route-protection matrix (written).
- Gateway path map (written).
- Example JWT claims payload.

#### Definition of Done

- [ ] Every Sierra-Lima endpoint has a documented required auth posture.
- [ ] Sierra-Lima can implement services against the documented JWT shape
      without waiting for Alfa-Kilo.

---

### Phase 2 -- Contract Pack & Local-Dev Bootstrap

- **Goal.** Pin the exact REST contracts, validation rules, DB schemas, and
  local environment so Phases 3-8 can focus on implementation only.
- **Prerequisites.** Phase 0, Phase 1.
- **Estimated effort.** 1-2 sessions. If you split it: session A is the
  contract pack, session B is the local-dev bootstrap.

#### Part A -- Contract Pack

1. **Freeze final REST endpoints** for both services (Appendix F Tables
   F.1 and F.2). Six endpoints per service.
   - *Acceptance:* endpoint tables copied into
     `dev-docs/decisions/0020-sierra-lima-contracts.md`.
2. **Freeze request and response payloads** (JSON schemas). Draft one
   example request and one example response per endpoint.
   - *Acceptance:* all 12 endpoints have example JSON.
3. **Freeze validation rules.**
   - Restaurant: `name` not blank; `latitude` in `[-90, 90]`; `longitude`
     in `[-180, 180]`; `operatingHours` matches `HH:MM-HH:MM`;
     `city` not blank.
   - Menu: `priceAmount` is `BigDecimal`, positive, scale 2;
     `priceCurrency` is ISO-4217 (default `"EUR"`); `name` not blank;
     `category` not blank.
   - *Acceptance:* rules listed and referenced by Phase 4 and Phase 6.
4. **Freeze database schemas** (Appendix F Tables F.3 and F.4). Draft as
   SQL:
   ```sql
   -- V1__init.sql for restaurant_db
   CREATE TABLE restaurant (
     restaurant_id UUID PRIMARY KEY,
     owner_id UUID NOT NULL,
     name VARCHAR(255) NOT NULL,
     address VARCHAR(255),
     city VARCHAR(120),
     latitude DOUBLE PRECISION,
     longitude DOUBLE PRECISION,
     operating_hours VARCHAR(20),
     is_open BOOLEAN NOT NULL DEFAULT FALSE,
     created_at TIMESTAMP NOT NULL,
     updated_at TIMESTAMP NOT NULL
   );
   CREATE INDEX idx_restaurant_city ON restaurant(city);
   CREATE INDEX idx_restaurant_owner ON restaurant(owner_id);
   ```
   ```sql
   -- V1__init.sql for menu_db
   CREATE TABLE menu_item (
     menu_item_id UUID PRIMARY KEY,
     restaurant_id UUID NOT NULL,
     name VARCHAR(255) NOT NULL,
     description VARCHAR(2000),
     price_amount NUMERIC(19,2) NOT NULL CHECK (price_amount > 0),
     price_currency CHAR(3) NOT NULL DEFAULT 'EUR',
     category VARCHAR(100) NOT NULL,
     is_available BOOLEAN NOT NULL DEFAULT TRUE,
     created_at TIMESTAMP NOT NULL,
     updated_at TIMESTAMP NOT NULL
   );
   CREATE INDEX idx_menu_item_restaurant ON menu_item(restaurant_id);
   CREATE INDEX idx_menu_item_category ON menu_item(category);
   ```
   - *Acceptance:* both SQL migrations live at
     `services/restaurant-service/src/main/resources/db/migration/V1__init.sql`
     and
     `services/menu-service/src/main/resources/db/migration/V1__init.sql`.
5. **Create a seed-data plan.** 4-6 demo restaurants, 12-18 menu items.
   Include:
   - At least two cities (e.g. `Tartu`, `Tallinn`).
   - At least three categories (`Appetizer`, `Main`, `Dessert`).
   - At least one closed restaurant for failure-path demos.
   - *Acceptance:* one table per service in the decisions log.
6. **Write cross-service assumptions explicitly.**
   - `MenuItem.restaurantId` is a cross-service reference; stored as UUID;
     no FK.
   - `Restaurant.ownerId` is a cross-service reference; stored as UUID;
     no FK.
   - *Acceptance:* one sentence in the decisions log.

#### Part B -- Environment Bootstrap

7. **Install / verify prerequisites.**
   - Java 17 JDK (Temurin).
   - Maven 3.9+.
   - Docker Desktop with WSL2 backend, Docker Compose v2.
   - Node.js 18+ and npm (for Phase 12+).
   - Vue CLI (`npm install -g @vue/cli`) (for Phase 12+).
   - Postman.
   - IDE with Spring Boot support (IntelliJ IDEA recommended).
   - *Acceptance:* `java -version`, `mvn -version`, `docker --version`,
     `docker compose version`, `node -v`, `npm -v` all succeed.
8. **Initialise Spring Boot projects** via
   [Spring Initializr](https://start.spring.io/).
   - Both projects: Spring Web, Spring Data JPA, PostgreSQL Driver,
     Validation, Lombok, DevTools, Spring Security, Flyway Migration.
   - Place generated sources under `services/restaurant-service/` and
     `services/menu-service/`.
   - After generation, **manually add** `jjwt` to each `pom.xml`:
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
   - *Acceptance:* both `mvn clean package` builds succeed.

   > **Spring Security bootstrap caveat (important):** adding
   > `spring-boot-starter-security` locks every endpoint behind the
   > generated form-login page by default. The Phase 2 DoD requires
   > `/actuator/health` to return 200 -- it will return 401/302 unless a
   > permissive `SecurityConfig` is added immediately. Add the following
   > stub in each service and leave it in place until Phase 7 replaces it
   > with the real JWT filter:
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
   > Phase 7 replaces `anyRequest().permitAll()` with the real
   > route-protection matrix.

9. **Set up local PostgreSQL via Docker Compose.** Create
   `services/local-dev/docker-compose.yml`:
   - `restaurant-db`: `postgres:15`, container port 5432, host port 5432,
     database `restaurant_db`, user `restaurant_user`, password from
     `.env.local`, named volume `restaurant_db_data`.
   - `menu-db`: `postgres:15`, container port 5432, host port 5433,
     database `menu_db`, user `menu_user`, password from `.env.local`,
     named volume `menu_db_data`.
   - *Acceptance:* `docker compose up -d` brings both up; `docker ps`
     shows both containers healthy.
10. **Standardise the service ports and env-var matrix.**

    | Service | Port | DB host port | Notes |
    |---------|------|--------------|-------|
    | API Gateway | 8080 | -- | Alfa-Kilo |
    | Restaurant Service | 8081 | 5432 | Sierra-Lima |
    | Menu Service | 8082 | 5433 | Sierra-Lima |
    | Payment Service | 8083 | 5437 | Elephant-Yankee |
    | Delivery Service | 8084 | 5438 | Elephant-Yankee |
    | User Service | 8085 | 5435 | Alfa-Kilo; issues tokens |
    | Order Service | 8086 | 5436 | Alfa-Kilo |
    | Notification Service | 8087 | 5439 | Mike-Alfa |
    | Kafka broker | 9092 | -- | Mike-Alfa |

    Env vars every Sierra-Lima service needs:
    - `DB_URL`, `DB_USER`, `DB_PASSWORD`.
    - `JWT_SECRET` (Base64 256-bit key for HS256 dev), `JWT_ISSUER`.
    - `RESTAURANT_SERVICE_URL`, `MENU_SERVICE_URL` only if one calls the
      other.
    - *Acceptance:* matrix lives in `services/local-dev/README.md`.
11. **Add Flyway configuration** (`application.properties`):
    ```properties
    spring.flyway.enabled=true
    spring.flyway.locations=classpath:db/migration
    spring.jpa.hibernate.ddl-auto=validate
    ```
    - For the very first run, a Flyway migration `V1__init.sql` must exist
      (from step 4). Otherwise `validate` will fail.
    - *Acceptance:* both services start with Flyway `V1` applied.
12. **Verify both services start** (`mvn spring-boot:run` in each project).
    Hit `/actuator/health` and confirm `{"status":"UP"}`.
13. **Set up the Postman workspace.** One collection named `QuickBite`,
    folders: `Auth (Login)`, `Restaurant CRUD`, `Menu CRUD`, `W1
    Integration`, `Async Evidence`, `Negative Auth`. Environment file with
    `baseUrl`, `gatewayUrl`, `jwtToken`, `customerToken`, `ownerToken`,
    `adminToken`.
    - *Acceptance:* collection exported to `dev-docs/` or shared via
      Postman Cloud and linked from the decisions log.

#### Definition of Done

- [ ] All 12 endpoints have frozen JSON schemas.
- [ ] Both services' DB schemas exist as Flyway `V1__init.sql`.
- [ ] Validation rules documented.
- [ ] Both Spring Boot apps start without errors.
- [ ] Both PostgreSQL databases run via Docker Compose.
- [ ] `/actuator/health` returns 200 on both services.
- [ ] Postman collection exists with a login placeholder.
- [ ] Sierra-Lima can begin implementation without waiting for any teammate.

---

### Phase 3 -- Restaurant Service: Foundation

- **Goal.** Persist the `Restaurant` entity in `restaurant_db`. Make the
  basic lifecycle (create, retrieve, list, update, toggle status) work
  end-to-end via Postman.
- **Prerequisites.** Phase 2.
- **Estimated effort.** 1 session.

#### Tasks

1. **Domain model.** Create JPA `@Entity` for `Restaurant`:
   - `restaurantId` (UUID, `@Id`, `@GeneratedValue`).
   - `ownerId` (UUID, not null -- cross-service ref).
   - `name` (String, `@NotBlank`).
   - `address`, `city` (String).
   - `latitude`, `longitude` (Double; validate ranges in DTO).
   - `operatingHours` (String).
   - `isOpen` (Boolean, default `false`).
   - `createdAt`, `updatedAt` (`LocalDateTime`, JPA auditing).
   - Consider embedding `Location` as `@Embeddable` (address, city,
     latitude, longitude).
   - *Acceptance:* entity compiles; maps to the Flyway schema.
2. **Repository.** `RestaurantRepository extends JpaRepository<Restaurant, UUID>`
   with derived queries `findByCity(String city)` and
   `findByIsOpenTrue()`.
3. **Service layer.** `RestaurantService` with these methods (one per
   endpoint in Appendix F Table F.1):
   - `UUID create(CreateRestaurantRequest)`
   - `RestaurantResponse findById(UUID)`
   - `void update(UUID, UpdateRestaurantRequest)`
   - `void setStatus(UUID, boolean)`
   - `Page<RestaurantResponse> search(String city, Boolean isOpen, Pageable)`
   - `AvailabilityResponse checkAvailability(UUID)`
4. **Controller.** `RestaurantController` with `@RestController` and
   `@RequestMapping("/restaurants")`. The gateway rewrites
   `/api/restaurants/**` to this path.
5. **DTOs.** `CreateRestaurantRequest`, `UpdateRestaurantRequest`,
   `RestaurantResponse`, `AvailabilityResponse`.
6. **Postman tests.** Walk all six endpoints:
   - `POST /restaurants` → 201, returns `restaurantId`.
   - `GET /restaurants/{id}` → 200.
   - `PUT /restaurants/{id}` → 204.
   - `PATCH /restaurants/{id}/status` with `{"isOpen": true}` → 204.
   - `GET /restaurants?city=Tartu&isOpen=true` → 200 with list.
   - `GET /restaurants/{id}/availability` → 200.
   - *Acceptance:* all six return the documented status codes.

#### Definition of Done

- [ ] `restaurant` table exists in `restaurant_db` (via Flyway `V1`).
- [ ] All six endpoints from Appendix F Table F.1 reachable.
- [ ] Data persists across service restarts.
- [ ] Postman collection updated.

> **Note.** Restaurant Service has **no** `DELETE /restaurants/{id}`
> endpoint. This is intentional. Restaurants are toggled open/closed via
> `PATCH /{id}/status`; hard deletion would orphan historical order data.
> If the team later needs soft-delete, add a `status: INACTIVE` field
> rather than a DELETE.

---

### Phase 4 -- Restaurant Service: Full API, Validation, OpenAPI

- **Goal.** Turn Phase 3's working CRUD into a polished API: validation,
  consistent errors, Swagger, and CORS ready for the frontend.
- **Prerequisites.** Phase 3.
- **Estimated effort.** 1 session.

#### Tasks

1. **Complete the endpoint set** per Appendix F Table F.1 (six endpoints).
2. **Bean Validation annotations.**
   - `@NotBlank` on `name`, `address`, `city`.
   - `@DecimalMin("-90")` / `@DecimalMax("90")` on `latitude`;
     `@DecimalMin("-180")` / `@DecimalMax("180")` on `longitude`.
   - `@Pattern(regexp = "^[0-2][0-9]:[0-5][0-9]-[0-2][0-9]:[0-5][0-9]$")`
     on `operatingHours`.
3. **Global exception handler.** `@ControllerAdvice` returning a consistent
   error envelope:
   ```json
   {
     "timestamp": "2026-05-05T12:34:56Z",
     "status": 422,
     "error": "Unprocessable Entity",
     "message": "Validation failed",
     "path": "/restaurants",
     "validationErrors": [
       {"field": "name", "message": "must not be blank"}
     ]
   }
   ```
4. **HTTP status codes.** 201 Created on `POST`; 204 No Content on `PUT`
   and `PATCH`; 400 Bad Request for malformed JSON; 404 Not Found for
   unknown id; 409 Conflict for business rule conflicts (e.g. duplicate
   name within the same owner); 422 Unprocessable Entity for validation.
5. **OpenAPI / Swagger.**
   - Add `springdoc-openapi-starter-webmvc-ui` dependency.
   - Annotate controllers with `@Operation`, `@ApiResponses`.
   - Verify Swagger UI at `http://localhost:8081/swagger-ui.html`.
6. **Auditing.** Add `@EnableJpaAuditing`, `@CreatedDate`,
   `@LastModifiedDate`. Provide an `AuditorAware<UUID>` that reads the
   authenticated `userId` from `SecurityContext` (or returns a default UUID
   during the pre-Phase-7 bootstrap).
7. **CORS configuration.** Define a global `CorsConfigurationSource` bean
   and wire it through `SecurityFilterChain`:
   ```java
   http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
   ```
   Do **not** rely on `@CrossOrigin` on controllers; Spring Security's
   filter chain runs before Spring MVC, so controller-level CORS is
   effectively overridden.

#### Definition of Done

- [ ] All six endpoints fully functional with clear validation errors.
- [ ] Swagger UI renders every endpoint and schema.
- [ ] Consistent error envelope across the service.
- [ ] CORS headers present on all responses (preflight `OPTIONS` returns 200).
- [ ] Postman collection complete for Restaurant Service.

---

### Phase 5 -- Menu Service: Foundation

- **Goal.** Persist `MenuItem` in `menu_db`. Order Service can ask for item
  validity and pricing without direct DB access.
- **Prerequisites.** Phase 2.
- **Estimated effort.** 1 session.

#### Tasks

1. **Domain model.** `MenuItem` JPA `@Entity`:
   - `menuItemId` (UUID, PK, `@GeneratedValue`).
   - `restaurantId` (UUID, not null -- cross-service ref).
   - `name` (String, `@NotBlank`), `description` (String, nullable).
   - `priceAmount` (`BigDecimal`, scale 2, `@Positive`).
   - `priceCurrency` (String, default `"EUR"`).
   - `category` (String, `@NotBlank`).
   - `isAvailable` (Boolean, default `true`).
   - `createdAt`, `updatedAt`.
   - Consider embedding `Price` as `@Embeddable` (amount, currency).
2. **Repository.** `MenuItemRepository extends JpaRepository<MenuItem, UUID>`
   with:
   - `List<MenuItem> findByRestaurantId(UUID)`.
   - `List<MenuItem> findByRestaurantIdAndIsAvailableTrue(UUID)`.
   - `List<MenuItem> findAllByMenuItemIdIn(Set<UUID>)`.
3. **Service layer.** `MenuService` with methods matching Appendix F
   Table F.2. Includes a **batch validation** method:
   - Input: `List<{menuItemId, quantity}>`.
   - Output: per-item existence, availability, unit price, and line total;
     plus aggregate `allValid` and `totalAmount`.
4. **Controller.** `MenuController`. Path mix:
   - `POST /restaurants/{rid}/menu-items`, `GET /restaurants/{rid}/menu-items`.
   - `GET /menu-items/{id}`, `PUT /menu-items/{id}`, `DELETE /menu-items/{id}`.
   - `POST /menu-items/validate`.
5. **DTOs.** `CreateMenuItemRequest`, `UpdateMenuItemRequest`,
   `MenuItemResponse`, `ValidateMenuItemsRequest`,
   `ValidateMenuItemsResponse` (with the shape pinned in Appendix F).
6. **Postman tests.** All CRUD + restaurant-scoped list + batch validation.

#### Definition of Done

- [ ] `menu_item` table exists in `menu_db` (via Flyway `V1`).
- [ ] All six endpoints from Appendix F Table F.2 work via Postman.
- [ ] Batch validation returns correct prices and availability per item.
- [ ] Data persists across restarts.

---

### Phase 6 -- Menu Service: Full API, Validation, OpenAPI

- **Goal.** Complete the Menu Service API with validation, consistent
  errors, Swagger, CORS, and a locked batch-validation response shape.
- **Prerequisites.** Phase 5.
- **Estimated effort.** 1 session.

#### Tasks

1. **Finalise endpoint set** per Appendix F Table F.2 (six endpoints).
2. **Validation.** `@Positive` on `priceAmount`; `@NotBlank` on `name` and
   `category`; `@Size(max=2000)` on `description`; `@Pattern("^[A-Z]{3}$")`
   on `priceCurrency`.
3. **Global exception handler.** Reuse the envelope from Phase 4
   (duplicate the class across services -- or extract a small shared jar;
   duplication is acceptable at this scope).
4. **OpenAPI / Swagger.** Verify UI at
   `http://localhost:8082/swagger-ui.html`.
5. **CORS configuration.** Same pattern as Phase 4.
6. **Batch validation response shape** locked to the Appendix F
   template. Order Service will consume this directly in W1 Phase 10;
   changing the shape after Phase 10 is painful.
7. **Filters on list endpoint.** `GET /restaurants/{rid}/menu-items`
   accepts optional query params `category` and `available` (Boolean).

#### Definition of Done

- [ ] All six endpoints fully functional.
- [ ] Swagger UI renders every endpoint.
- [ ] Batch validation returns stable JSON matching Appendix F.
- [ ] Postman collection complete for Menu Service.

---

### Phase 7 -- Sierra-Lima Hardening Pass

- **Goal.** Make both services **demo-grade** rather than just coded.
  They should be protected, testable, and safe to show independently at
  the project consultation session on 2026-04-28 if the rest of the team
  is behind.
- **Prerequisites.** Phase 4, Phase 6.
- **Estimated effort.** 1-2 sessions.

#### Tasks

1. **Create a dev JWT generator utility.** Before wiring the filter, make
   sure we can actually produce tokens. Add a small `DevTokenGenerator`
   class (or a `@SpringBootTest` that prints a token on demand):
   ```java
   String token = Jwts.builder()
       .setSubject("dev-user-001")
       .claim("userId", "00000000-0000-0000-0000-000000000001")
       .claim("role", "RestaurantOwner")
       .claim("tokenType", "USER")
       .setIssuedAt(new Date())
       .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
       .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(DEV_SECRET)))
       .compact();
   ```
   `DEV_SECRET` is a Base64-encoded 256-bit key stored in
   `application.properties` as `jwt.secret`. The real User Service key
   will replace it later -- env var name stays `JWT_SECRET`.
   Produce one token per role (`Customer`, `RestaurantOwner`, `Admin`)
   and add them to the Postman environment.
2. **Wire local token validation.** Add a lightweight `JwtAuthFilter` that:
   - Accepts tokens signed with the shared dev secret.
   - Populates `SecurityContext` with the authenticated `userId` and
     `role` (use a simple `Authentication` principal, e.g.
     `UsernamePasswordAuthenticationToken`).
   - Rejects missing or invalid tokens with HTTP 401.
   - Skips public routes (per decision in Phase 1).
   Replace the Phase 2 permissive `SecurityConfig` stub with this real
   filter chain.
3. **Apply the route-protection matrix.**
   - Public (per §5.1 default, unless Phase 1 changed it):
     - `GET /restaurants`.
     - `GET /restaurants/{id}`.
     - `GET /restaurants/{id}/availability` (or token-protected, per §5.1).
     - `GET /restaurants/{rid}/menu-items`.
     - `GET /menu-items/{id}`.
   - Authenticated: all other `POST`, `PUT`, `PATCH`, `DELETE`.
   - Role-gated: restaurant owner or admin for mutations. Precise
     ownership checks are acceptable but may be light at this phase;
     Phase 15 hardens them.
4. **Standardise error responses** across both services. Same envelope,
   same field names, same status codes.
5. **Tighten request validation.** Edge cases:
   - Duplicate restaurant name by the same owner -> 409.
   - Empty menu item list in batch validate -> 400.
   - Invalid price (<= 0, scale > 2) -> 422.
   - Unknown category -> allowed (free-form), but log at DEBUG.
6. **Verify auditing** (`createdAt`, `updatedAt` are populated on create
   and update).
7. **Add seed data** via a second Flyway migration (`V2__seed_demo_data.sql`)
   or a `CommandLineRunner` behind a dev profile:
   - 4-6 demo restaurants (Tartu and Tallinn), including one closed.
   - 12-18 menu items across at least 3 categories.
   - Stable UUIDs (so Postman collection can reference them).
8. **Controller-level and service-level tests.** JUnit 5 + Spring Boot
   Test. Cover: happy path, validation errors, 404. Use Testcontainers
   for an in-Docker PostgreSQL if feasible; otherwise H2 in-memory with
   PostgreSQL dialect.
9. **Refresh the Postman collection.**
   - Login folder issues a token for each role (stubbed against the
     local dev generator until User Service exists).
   - Environment variables auto-populate from login response.
   - All Restaurant and Menu requests reference environment variables.
10. **Guard Assignment 1 feedback.**
    - Confirm the business-architecture diagrams (Figure 1) show no
      infrastructure.
    - Confirm each service has its own DB container in the Docker Compose.

#### Definition of Done

- [ ] Missing or invalid tokens produce 401.
- [ ] Valid tokens unlock mutation endpoints.
- [ ] Consistent, structured error responses across both services.
- [ ] Seed data loads automatically on startup.
- [ ] Tests pass for critical CRUD and validation paths.
- [ ] Postman collection complete, login-first, shareable.
- [ ] Both services demonstrable independently through a login-gated flow.

---

### Phase 8 -- Dockerise Both Services

- **Goal.** Both services and their databases run entirely inside Docker
  containers, orchestrated by one Docker Compose file.
- **Prerequisites.** Phase 7.
- **Estimated effort.** 1 session.

#### Tasks

1. **Dockerfile per service (multi-stage).**
   ```dockerfile
   # Stage 1: build
   FROM maven:3.9-eclipse-temurin-17 AS build
   WORKDIR /app
   COPY pom.xml .
   RUN mvn -B -DskipTests dependency:go-offline
   COPY src ./src
   RUN mvn -B -DskipTests package

   # Stage 2: run
   FROM eclipse-temurin:17-jre
   WORKDIR /app
   COPY --from=build /app/target/*.jar app.jar
   EXPOSE 8081
   ENTRYPOINT ["java","-jar","/app/app.jar"]
   ```
   Adjust the exposed port per service.
2. **Extend Docker Compose.** `services/local-dev/docker-compose.yml`
   should now contain:
   - `restaurant-db`, `menu-db` (PostgreSQL 15 with named volumes).
   - `restaurant-service` (depends_on `restaurant-db` with `condition:
     service_healthy`).
   - `menu-service` (depends_on `menu-db`).
   - Network: one bridge network for inter-service communication.
   - (Optional now, required later) `api-gateway`, `user-service`,
     `order-service`, `payment-service`, `delivery-service`,
     `notification-service`, `kafka`, `zookeeper` -- placeholders ready
     to uncomment once teammates' images exist.
3. **Spring profiles.**
   - `application.properties` -- defaults for running via `mvn` locally
     (DB at `localhost:5432`/`5433`).
   - `application-docker.properties` -- DB at `restaurant-db:5432` /
     `menu-db:5432` (internal container hostnames).
   - Activate the docker profile via `SPRING_PROFILES_ACTIVE=docker`
     env var in Docker Compose.
4. **Health checks.** Add `healthcheck:` blocks to both DB services
   (`pg_isready`) and both app services (`curl /actuator/health`).
5. **Run the stack.**
   - `docker compose up --build` starts from scratch.
   - Run the entire Postman collection against it.
   - Restart to confirm data persists in volumes.
   - `docker compose down -v` resets state.
6. **`.dockerignore` per service** (`target/`, `.idea/`, `.claude/`,
   `.git/`, `*.iml`).

#### Definition of Done

- [ ] `docker compose up --build` starts everything from scratch.
- [ ] Both services reachable and functional.
- [ ] Databases have persistent volumes.
- [ ] No shared database (each service has its own container).
- [ ] `docker compose down` stops cleanly.

---

### Phase 9 -- Team Contract Lock for W1 / W2 / W3

- **Goal.** Prevent late integration drift. Agree, in writing, the exact
  calls and events each service produces and consumes. This phase is
  coordination-heavy; most of it can happen over chat if the team cannot
  meet.
- **Prerequisites.** Phase 6 (so Sierra-Lima knows the real response
  shapes).
- **Estimated effort.** 1 session.
- **Team dependency.** All four members.

#### Tasks

1. **W1 synchronous call chain locked** (Appendix F Section F.6):
   - `Order -> User` (customer lookup).
   - `Order -> Restaurant` (`GET /restaurants/{id}/availability`).
   - `Order -> Menu` (`POST /menu-items/validate`).
   - `Order -> Payment` (`POST /payments`).
   - `Order -> Delivery` (`POST /deliveries`).
2. **Status codes locked** for validation failures from Restaurant and
   Menu so Order Service does not guess:
   - Restaurant not found -> 404.
   - Restaurant closed -> 200 with `acceptsOrders:false` (or 409, per
     team decision in Phase 1/7).
   - Unknown menu item -> 422 with per-item detail.
   - Unavailable menu item -> 422 with per-item detail.
3. **Event contracts locked** (Appendix F Section F.7):
   - Topic `payment-events`: `payment.completed`, `payment.failed`.
   - Topic `delivery-events`: `delivery.status-changed`.
   - Optional `order-events`: `order.cancelled`.
4. **Event envelope shape locked:** `{ id, type, occurredAt, payload }`.
   `id` is the idempotency key.
5. **Dead-letter and idempotency expectations agreed.**
   - Consumers are idempotent (dedup by envelope `id`).
   - Failed handlers route to a DLQ topic (`<topic>.dlq`).
6. **Token propagation on inter-service calls** confirmed. The original
   caller's token (or a service token) flows over REST hops.

#### Outputs

- Contract sheet for W1.
- Event contract sheet for W2 and W3.
- Shared event-envelope example.
- Status-code table for cross-service errors.

#### Definition of Done

- [ ] Every teammate integrates against written contracts, not chat
      memory.
- [ ] Sierra-Lima's availability and batch-validation response shapes are
      the ones Order Service will call -- not drafts.

---

### Phase 10 -- W1 Integration & Failure-Path Protection

- **Goal.** Make Sierra-Lima's services real participants in W1, with
  well-behaved failure responses so Order Service never has to guess what
  a 4xx means.
- **Prerequisites.** Phase 8, Phase 9.
- **Estimated effort.** 1-2 sessions.
- **Team dependency.** Alfa-Kilo's Order Service needs at least a smoke
  version for the end-to-end test; if not available, mock Order with a
  Postman pre-request script.

Sierra-Lima is **not** a Kafka producer or consumer in the baseline A3
scope. This phase focuses on the synchronous integration Sierra-Lima is
on the hook for.

#### Tasks -- W1 Integration

1. **End-to-end test the availability check.**
   - `GET /restaurants/{id}/availability` with bearer token.
   - Returns `{ restaurantId, isOpen, acceptsOrders, operatingHours, checkedAt }`.
   - *Acceptance:* Postman test case per scenario (open / closed / unknown).
2. **End-to-end test the batch validation.**
   - `POST /menu-items/validate` with a list of `{ menuItemId, quantity }`.
   - Returns per-item existence, availability, unit price, and line total.
   - Plus aggregate `allValid` and `totalAmount`.
   - *Acceptance:* Postman test cases for: all valid, one item missing,
     one item unavailable, quantity zero, empty list.
3. **Confirm failure behaviour.**
   - Restaurant not found -> 404.
   - Restaurant closed -> 200 with `acceptsOrders:false`, unless the team
     agreed 409.
   - Unknown menu item -> 422 with per-item error breakdown.
   - Unavailable menu item -> 422 with per-item error breakdown.
   - Unauthorised call -> 401 (never a leaky 500).
4. **Coordinate a smoke test with Alfa-Kilo's Order Service.** Share seed
   IDs and demo payloads. Confirm the Order flow successfully calls
   Restaurant and Menu in sequence.

#### Tasks -- Resilience (Optional, scope-dependent)

> **Scope note.** Per A3, Sierra-Lima makes no outbound REST calls to
> other services. Resilience tasks 5-7 below are **only relevant if** the
> team's Phase 9 agreement adds an outbound call from Restaurant or Menu
> to another service. If no outbound call exists, skip to Task 6 and mark
> Tasks 5 and 7 as N/A.

5. **Add Resilience4j** (only if an outbound call exists):
   - `spring-cloud-starter-circuitbreaker-resilience4j`.
   - `spring-boot-starter-actuator`.
   - `spring-boot-starter-aop`.
   - Configure a `TimeLimiter` (1 s) and `CircuitBreaker` (sliding
     window 10 requests, 50% failure threshold).
6. **Ensure Sierra-Lima services are resilient callees** regardless of
   outbound calls.
   - Controller timeouts are reasonable (via Tomcat thread pool; default
     is fine).
   - 5xx only happens on genuinely unexpected failures.
   - Slow paths are logged at WARN.
7. **Test with dependent services stopped** (only if outbound calls
   exist): confirm the returned error is clear and mapped, not a stack
   trace.

#### Definition of Done

- [ ] `Order -> Restaurant` availability check demonstrably works.
- [ ] `Order -> Menu` batch validation demonstrably works.
- [ ] Known failure paths documented with status codes and payloads.
- [ ] Actuator health endpoint exposed.
- [ ] (If applicable) circuit-breaker state observable via actuator.

---

### Phase 11 -- Backend Polish & Checkpoint #1 Prep

- **Goal.** Package the backend into something that survives a **live
  Checkpoint #1 demo** on 2026-05-05.
- **Prerequisites.** Phase 10.
- **Estimated effort.** 1 session.

#### Tasks

1. **Code review & cleanup.**
   - Consistent naming of methods and variables.
   - Remove debug `System.out.println` calls; replace with SLF4J logging.
   - Delete dead code and TODOs that are no longer relevant.
2. **Verify seed data.** 4-6 restaurants and 12-18 menu items load from
   the Flyway seed migration or the dev `CommandLineRunner`. Demo-ready.
3. **Finalise the Postman collection.** Folders:
   - `Login`.
   - `Restaurant CRUD`.
   - `Menu CRUD`.
   - `W1 Integration` (availability + batch validate).
   - `Async Evidence` (placeholder for Phase 16).
   - `Negative Auth` (401, 403 test cases).
4. **Full-stack Docker Compose verification.**
   - `docker compose up --build` from scratch.
   - Run through the entire Postman collection.
   - Expected containers up: `restaurant-db`, `menu-db`, plus
     `api-gateway`, `user-service`, `order-service` if the team has
     committed them.
5. **Smoke-test script.** `services/local-dev/smoke.sh` (or `.ps1`):
   curl commands that:
   - Issue a dev token.
   - Create a restaurant.
   - Add a menu item.
   - Toggle status.
   - Call availability.
   - Call batch validate.
   - Expected exit code 0 if all OK.
6. **Checkpoint #1 talking points.** Draft in
   `dev-docs/checkpoint-1-talking-points.md`:
   - Which seven business services are implemented; why `Review` is
     design-only.
   - Why static configuration (no Eureka).
   - Why each service has its own DB, answering Assignment 1 feedback.
   - How auth is enforced at gateway and service level.
   - Where W1 crosses Sierra-Lima (availability + batch validate).
   - How async (W2/W3) appears in the architecture, even if wired by
     teammates.
   - Live demo script: login -> create restaurant -> add menu items ->
     toggle status -> hit availability -> hit batch validate -> show
     error paths.
7. **Team coordination check.** Confirm the shared Docker Compose file,
   ports, and network names match across teammates' services.
8. **Update report draft.** Add backend architecture and workflow diagrams
   at their current state.
9. **Backup materials.** 2-3 minute screen recording of the demo path,
   stored in `dev-docs/checkpoint-1-backup/`.

#### Definition of Done

- [ ] Both services fully functional with all endpoints.
- [ ] Full Docker Compose stack starts and works.
- [ ] Seed data loads automatically.
- [ ] Postman collection complete (login-first).
- [ ] Demo script rehearsed at least once.
- [ ] Backup recording saved.

---

### Phase 12 -- Vue.js Frontend: Shell, Routing & Sign-In

- **Goal.** Create the minimum viable Vue.js frontend with navigation,
  routing, a login-first flow, and API client utilities that carry bearer
  tokens.
- **Prerequisites.** Phase 11.
- **Estimated effort.** 1 session.

#### Tasks

1. **Scaffold the Vue project.**
   - `vue create quickbite-frontend` (Vue 3, Router, Babel).
   - Place under `services/frontend/`.
   - Verify `npm run serve` works at `http://localhost:8090`.
2. **Configure API base URL via env.**
   ```
   VUE_APP_API_BASE_URL=http://localhost:8080
   ```
   (the gateway).
3. **Route map and layout.**
   - Top-level navigation: Home, Restaurants, Menu, Orders, Login /
     Logout.
   - Placeholder views: restaurant list, restaurant detail + menu,
     cart / order, order status, login, signup.
4. **Shared API client utility.** A `fetch()` wrapper that:
   - Prepends the base URL.
   - Sets `Content-Type: application/json`.
   - Attaches `Authorization: Bearer <token>` from `localStorage`.
   - Translates 401 responses into a redirect to `/login` and clears the
     token.
   - Wraps network errors so components get consistent messages.
5. **Login flow.**
   - `Login.vue` posts to `/api/auth/login`, stores the JWT in
     `localStorage`, redirects to the home page.
   - `Signup.vue` posts to `/api/users`, then redirects to login.
   - A "Logout" button clears the token and redirects.
6. **Route guards.** Use `beforeEach` on the router: if the target route
   is protected and no token is present, redirect to `/login`.

#### Definition of Done

- [ ] Frontend starts and navigates between pages.
- [ ] Login / logout flow works against a real or mocked User Service.
- [ ] API client attaches tokens automatically.
- [ ] Protected routes redirect when no token is present.

---

### Phase 13 -- Vue.js Frontend: Restaurant & Menu UX

- **Goal.** Put Sierra-Lima's backend on screen. A signed-in demo user can
  discover a restaurant, open it, and view orderable menu items.
- **Prerequisites.** Phase 12.
- **Estimated effort.** 1 session.

#### Tasks

1. **Restaurant views.**
   - `RestaurantList.vue` -- calls `GET /api/restaurants`, shows a
     card/table with name, city, status (open/closed), and a link to the
     detail page. Shows an "Add Restaurant" button for owners.
   - `AddRestaurant.vue` -- POST form with client-side validation
     mirroring the backend rules.
   - `RestaurantDetail.vue` -- `GET /api/restaurants/{id}`. Shows edit
     form (PUT), status toggle (PATCH), and a link to the menu.
2. **Menu item views.**
   - `MenuItemList.vue` -- `GET /api/restaurants/{rid}/menu-items`.
     Filterable by category and availability.
   - `AddMenuItem.vue` -- POST form with a restaurant dropdown populated
     from the backend.
   - `MenuItemDetail.vue` -- GET by id; edit; toggle availability.
3. **Router additions.**
   - `/restaurants`, `/restaurants/new`, `/restaurants/:id`.
   - `/restaurants/:id/menu`.
   - `/menu-items/new`, `/menu-items/:id`.
4. **UI polish.**
   - Loading spinner, empty state, error banner per list page.
   - Consistent styling (CSS scoped, or one shared stylesheet).
   - Role visibility: hide owner actions when the current token has role
     `Customer`.

#### Definition of Done

- [ ] Restaurant CRUD works from the browser.
- [ ] Menu item CRUD works from the browser.
- [ ] Browse flow (list -> detail -> menu -> item) works end-to-end.
- [ ] Error states handled gracefully.

---

### Phase 14 -- Frontend-Backend Integration & Checkpoint #2 Prep

- **Goal.** Full-stack app works end-to-end through the API Gateway.
  Demonstrable at Checkpoint #2 on 2026-05-12.
- **Prerequisites.** Phase 13.
- **Estimated effort.** 1 session.

#### Tasks

1. **Gateway CORS verified.** Frontend at `http://localhost:8090` can
   call gateway at `http://localhost:8080` without preflight errors.
2. **Frontend in Docker.**
   - Dockerfile: multi-stage (node build, then nginx serve).
   - Add to Docker Compose as `frontend` service (port 80 or 8090).
   - nginx proxies `/api/**` to the gateway.
3. **End-to-end browser flow.**
   1. Sign in.
   2. View restaurant list.
   3. Create a new restaurant (as owner).
   4. Add menu items.
   5. Toggle restaurant status.
   6. Edit and delete items.
   7. (If Order Service is ready) place an order and view status.
4. **Connect to W1** if the Order Service happy path is available.
5. **Update the report draft.** Refresh:
   - Data-model section (must match current code).
   - API endpoint tables (must match current Swagger).
   - Frontend architecture section.
   - Diagrams that may have drifted since CP#1.
6. **Prepare CP#2 demo.**
   - Demo script covering frontend + backend.
   - Backup screenshots / recording.
   - List of what remains for CP#3.

#### Definition of Done

- [ ] Frontend talks to backend through the API Gateway.
- [ ] Full CRUD workflow works in the browser.
- [ ] Docker Compose runs the entire stack including frontend.
- [ ] One person can demo discovery through order creation in a single
      run.
- [ ] Report draft updated to reflect current implementation state.

---

### Phase 15 -- Authorisation Hardening & Role-Aware Behaviour

- **Goal.** Turn the light "authenticated" checks from Phase 7 into real
  role-aware authorisation. Remove any temporary bypasses. Show unauthorised
  access being visibly rejected at CP#3.
- **Prerequisites.** Phase 14.
- **Estimated effort.** 1 session.

#### Tasks

1. **Role-gated mutations on Restaurant Service.**
   - Only the `ownerId` user or an `Admin` can `PUT /restaurants/{id}` or
     `PATCH /restaurants/{id}/status`.
   - Public `GET` routes remain as decided in Phase 1.
2. **Role-gated mutations on Menu Service.**
   - Only the owning restaurant's owner or an `Admin` can
     `POST /restaurants/{rid}/menu-items`, `PUT /menu-items/{id}`, or
     `DELETE /menu-items/{id}`.
   - `POST /menu-items/validate` stays open to customer + service tokens
     (or locked to service tokens -- whichever Phase 9 agreed).
3. **Remove any `permitAll()` or debug shortcuts** added during Phases
   3-7.
4. **Audit-test with Postman.**
   - Customer token on an owner-only endpoint -> 403.
   - Owner A token on a restaurant owned by Owner B -> 403.
   - Admin token -> 200.
   - Missing token -> 401.
5. **Log security denials** at `WARN`. The demo can optionally show a
   tail of the log at a denial moment.

#### Definition of Done

- [ ] Protected endpoints return 401 without a valid JWT.
- [ ] Role-based access works (Customer vs Driver vs RestaurantOwner vs
      Admin).
- [ ] Ownership check is enforced on restaurant and menu mutations.
- [ ] Postman collection includes negative-auth cases.

---

### Phase 16 -- Async Evidence & Cross-Service Smoke

- **Goal.** Ensure at least one asynchronous workflow is demonstrably real
  by Checkpoint #3, even though Sierra-Lima is neither producer nor
  consumer in the A3 event topology.
- **Prerequisites.** Phase 15.
- **Estimated effort.** 1 session.
- **Team dependency.** Mike-Alfa (Kafka + Notification), Elephant-Yankee
  (Payment + Delivery events), Alfa-Kilo (Order consumer).

#### Tasks

1. **Coordinate with Mike-Alfa.** Confirm Kafka is up, topics are created
   (`payment-events`, `delivery-events`, optional `order-events`),
   Notification Service consumes correctly.
2. **Coordinate with Elephant-Yankee.** Confirm Delivery Service publishes
   `delivery.status-changed` and Payment Service publishes
   `payment.completed` / `payment.failed`.
3. **Coordinate with Alfa-Kilo.** Confirm Order Service consumes
   `delivery.status-changed` and updates order state.
4. **Document Sierra-Lima's role.** Restaurant and Menu services do not
   produce or consume events in the baseline A3 scope. For CP#3, the
   async demo is driven by teammate-owned services; Sierra-Lima keeps
   Restaurant and Menu endpoints stable so the W1 -> W2 -> W3 chain stays
   real.
5. **Optional stretch (time permitting).** Add a
   `MenuItemAvailabilityChanged` event producer to Menu Service.
   - Publish `menu.item-availability-changed` on a new `menu-events`
     topic.
   - Envelope: `{ id, type, occurredAt, payload: { menuItemId,
     restaurantId, isAvailable } }`.
   - Only ship this if backend is stable and time allows.
6. **Cross-service smoke test.** Run one full trace:
   1. Login.
   2. Place an order (W1) -- hits Sierra-Lima's services.
   3. Payment outcome (W3) flows through Kafka.
   4. Delivery status (W2) flows through Kafka.
   - Confirm no exception in Sierra-Lima logs during the whole trace.
   - Record logs and timestamps for the report.

#### Definition of Done

- [ ] At least one async workflow (W2 or W3) visibly works end-to-end.
- [ ] Sierra-Lima's services stay stable during the integrated flow.
- [ ] Smoke-test script captures the full trace for replay during the
      demo.

---

### Phase 17 -- Report & Evidence Pack

- **Goal.** Finish the written deliverable before the final presentation
  crunch. If code froze today, the report would still be
  presentation-quality.
- **Prerequisites.** Phase 16.
- **Estimated effort.** 1 session.

#### Tasks

1. **Assemble report sections.**
   - Business architecture (from Assignments 1-2, Figure 1, no
     infrastructure).
   - Technical architecture (actual implementation, Figure 1b).
   - Implemented services vs design-only; justification for `Review`
     being design-only.
   - Data models (ER diagrams matching actual code, Figure 2).
   - APIs (endpoint tables, Swagger screenshots).
   - Workflows: W1 synchronous (Figure 3); W2/W3 asynchronous
     (Figure 4).
   - Integration mechanisms: REST, gateway routing, Kafka topics and
     envelope.
   - Security approach: JWT issuance, validation, role-gating.
   - Team responsibilities (see §4.3).
   - Limitations and future work (`Review Service` to be built later).
2. **Refresh diagrams to match the code.** Do not simply reuse
   `assignment-3` figures if the implementation diverged. Mark any
   divergences in the limitations section.
3. **Add evidence.**
   - Screenshots of Swagger UI for both services.
   - Endpoint tables.
   - Topic tables.
   - One screenshot of a 401 and a 403 (security working).
   - A short log excerpt showing an event being consumed.
4. **Proofread and format.** Target a clean PDF (or DOCX) export. Avoid
   broken layout, missing images, or stale diagrams.

#### Definition of Done

- [ ] All report sections drafted.
- [ ] Diagrams match the implemented system.
- [ ] Evidence (screenshots, tables) included.
- [ ] Report is near-final quality -- only minor polish left for Phase 18.

---

### Phase 18 -- Final Presentation Rehearsal

- **Goal.** Turn the working system into a convincing presentation.
- **Prerequisites.** Phase 17.
- **Estimated effort.** 1 session.
- **Team dependency.** All four members; at least for speaking-order
  agreement.

#### Tasks

1. **Slide deck** covering:
   - System overview and architecture.
   - Sierra-Lima's services: Restaurant + Menu.
   - API design (Swagger screenshots).
   - Synchronous integration: Menu batch validation in W1.
   - Asynchronous integration: W2 / W3 end-to-end.
   - Security: JWT issuance and validation, role-gating.
   - Frontend walkthrough.
   - (Optional) Resilience demo.
2. **Live demo script** with exact click-path and expected outputs.
3. **Assign speaking parts.** Architecture overview, Sierra-Lima
   services, synchronous flow, asynchronous flow, security, frontend.
4. **Fallback materials.** Screenshots, pre-recorded flow, backup seed
   data, recovery commands (`docker compose restart` etc.).
5. **Rehearse timeboxed answers** to likely questions:
   - Why were these seven services implemented?
   - Why is `Review` design-only?
   - How were service boundaries chosen? (DDD, Assignment 2.)
   - How does async integration work? Topics, envelope, idempotency.
   - How was security enforced at gateway vs service level?
   - Why no Eureka?
   - What did you sacrifice and why?

#### Definition of Done

- [ ] Slides complete.
- [ ] Live demo rehearsed at least once.
- [ ] Every presenter knows what to say and click.
- [ ] Backup materials ready.

---

### Phase 19 -- Buffer & Final Freeze

- **Goal.** Use remaining time for stabilisation only. No feature
  invention.
- **Prerequisites.** Phase 18.
- **Estimated effort.** 1 session.

#### Tasks

1. **Fix highest-risk bugs only.** No new features.
2. **Re-run smoke tests.** Both the Sierra-Lima `smoke.sh` and the full
   cross-service trace from Phase 16.
3. **Verify seeded demo users, restaurants, menu items, and order flow.**
4. **Verify clean startup** (`docker compose down -v && docker compose
   up --build`).
5. **Freeze the branch** for the presentation. Tag it, e.g.
   `v1.0.0-cp3`.

#### Definition of Done

- [ ] Full system starts and runs in Docker Compose.
- [ ] All workflows demonstrable end-to-end.
- [ ] No unresolved defect remains that could break the main demo
      narrative.
- [ ] Code is clean and committed.
- [ ] Branch is tagged for the presentation.

---

## 10. Checkpoint Readiness Gates

Use these as go/no-go criteria before each checkpoint. If any item is
unchecked, do **not** declare the phase block complete.

### Checkpoint #1 Gate (2026-05-05 -- Backend)

- [ ] At least the main implemented backend services compile and run.
- [ ] Restaurant Service and Menu Service are fully operational and
      **protected by bearer-token validation**.
- [ ] Login / token issuance is demonstrable, even if via a mocked User
      Service.
- [ ] W1 happy path works through the gateway or through documented
      service calls.
- [ ] Each service owns its own database.
- [ ] Backend starts from a documented local process.
- [ ] Architecture and workflow diagrams match reality.

### Checkpoint #2 Gate (2026-05-12 -- Frontend + Backend)

All CP#1 items still hold, plus:

- [ ] Frontend signs in and uses real protected APIs (not hardcoded
      mocks).
- [ ] A user can browse restaurants, inspect menus, and ideally place an
      order.
- [ ] UI handles loading and error states.
- [ ] Demo does not rely on hidden manual DB edits.

### Checkpoint #3 Gate (2026-05-19 -- Final Presentation)

All CP#2 items still hold, plus:

- [ ] Role-aware authorisation visibly enforced (customer vs owner vs
      admin).
- [ ] At least one asynchronous workflow (W2 or W3) demonstrably works.
- [ ] Report is presentation-ready and matches the implemented system.
- [ ] Team has a demo fallback plan.

---

## 11. What Sierra-Lima Can Safely Do Solo

These are the highest-value early-start tasks that do not require any
teammate:

1. Complete Phases 0-8 without waiting for anyone.
2. Produce canonical Restaurant and Menu API contracts and OpenAPI docs.
3. Seed compelling restaurant and menu demo data.
4. Make `POST /menu-items/validate` especially strong -- Order Service
   depends on it directly in W1.
5. Keep both services documented through OpenAPI and Postman.
6. Prepare stable example IDs, payloads, and failure responses for the
   Order team.
7. Set up Docker Compose and the local-dev runbook for the whole team.
8. Mock the User Service locally for token issuance so login and
   protected routes can be demonstrated before Alfa-Kilo's real service
   exists.

### 11.1 Stubbing strategy while solo

- Issue dev JWTs from `DevTokenGenerator` with a shared HS256 secret.
  Swap to User Service's real keys later -- keep the env var name
  `JWT_SECRET` stable.
- Use fixed UUIDs for `ownerId` until User Service exists. Record them
  in the decisions log.
- Skip Order Service calls. Use Postman to hit `availability` and
  `validate` directly.
- Test async evidence with `kafka-console-consumer` against teammate
  events once the broker is up (does not require teammate help).

---

## 12. Final Demo Success Criteria

By final-presentation readiness, the team should be able to demonstrate
all of the following:

1. A customer signs in and browses restaurants and menus.
2. An order is created through the frontend.
3. Backend coordination across multiple services (W1), including
   Sierra-Lima's availability check and batch validation.
4. At least one event-driven update or notification (W2 or W3).
5. At least one role-gated rejection (403 for wrong role).
6. Clear service boundaries and separate data ownership.
7. A report and diagrams that match the implemented system.

---

## 13. Bottom Line

The safest route from `a520963` (current scaffolded state) to a credible
final submission is:

1. **Re-baseline** scope and auth assumptions now that User Service is
   implemented and tokens are mandatory (Phases 0-1).
2. **Build** Sierra-Lima's two services properly, protected from day one
   (Phases 2-8).
3. **Use them as stable anchors** for the rest of the team, especially
   for the W1 availability check and batch validation (Phases 9-10).
4. **Reach backend integration** by 2026-05-05 (Phase 11).
5. **Layer in frontend** by 2026-05-12 (Phases 12-14).
6. **Harden authorisation, gather async evidence, finish report, and
   rehearse** by 2026-05-19 (Phases 15-19).

If the team follows the phase order above, early solo work is not wasted,
and the project stays aligned with the course assignments, practicals,
and checkpoint structure.

---

## Appendix A -- Suggested Session Calendar

Anchored to 2026-04-18 (Saturday). Assumes ~4 sessions per week on
average, with course practical days reserved.

| Session | Date | Day | Phase | Notes |
|---------|------|-----|-------|-------|
| 1 | 2026-04-18 | Sat | Phase 0 | Scope freeze and conventions |
| 2 | 2026-04-19 | Sun | Phase 1 | Auth + gateway contract alignment |
| 3 | 2026-04-20 | Mon | Phase 2A | Contract pack |
| -- | 2026-04-21 | Tue | -- | *Project description practical* |
| 4 | 2026-04-22 | Wed | Phase 2B | Local-dev bootstrap (scaffold, env, Flyway) |
| 5 | 2026-04-23 | Thu | Phase 3 | Restaurant foundation |
| 6 | 2026-04-25 | Sat | Phase 4 | Restaurant full API + Swagger |
| 7 | 2026-04-26 | Sun | Phase 5 | Menu foundation |
| 8 | 2026-04-27 | Mon | Phase 6 | Menu full API + Swagger |
| -- | 2026-04-28 | Tue | -- | *Project consultation practical* |
| 9 | 2026-04-29 | Wed | Phase 7A | Hardening: JWT filter + route matrix |
| 10 | 2026-04-30 | Thu | Phase 7B | Hardening: seed, tests, errors |
| 11 | 2026-05-02 | Sat | Phase 8 | Dockerise both services |
| 12 | 2026-05-03 | Sun | Phase 9 | Team contract lock |
| 13 | 2026-05-04 | Mon | Phase 10 | W1 integration + failure paths |
| **--** | **2026-05-05** | **Tue** | **--** | **Checkpoint #1 (Backend)** |
| 14 | 2026-05-06 | Wed | Phase 11 | Backend polish + CP#1 recap |
| 15 | 2026-05-07 | Thu | Phase 12 | Frontend shell + sign-in |
| 16 | 2026-05-09 | Sat | Phase 13 | Restaurant + Menu UX |
| 17 | 2026-05-10 | Sun | Phase 14 | Frontend-backend integration + CP#2 prep |
| 18 | 2026-05-11 | Mon | Phase 14 | Full-stack Docker dry run |
| **--** | **2026-05-12** | **Tue** | **--** | **Checkpoint #2 (Frontend + Backend)** |
| 19 | 2026-05-13 | Wed | Phase 15 | Authorisation hardening |
| 20 | 2026-05-14 | Thu | Phase 16 | Async evidence + cross-service smoke |
| 21 | 2026-05-16 | Sat | Phase 17 | Report + evidence pack |
| 22 | 2026-05-17 | Sun | Phase 18 | Final rehearsal |
| 23 | 2026-05-18 | Mon | Phase 19 | Buffer + final freeze |
| **--** | **2026-05-19** | **Tue** | **--** | **Checkpoint #3 (Final Presentation)** |

This calendar gives ~23 focused 3-hour sessions, which is ahead of the
~20-session core plan. Use the extra slots as buffer or for stretch
goals (e.g. Phase 16 optional Menu event producer).

---

## Appendix B -- Compression Guidance

If any phase slips, compress in this priority order. Sacrifice optional
extras **first**; never compress the items on the "never compress" list.

### Drop first

- Optional Menu-as-Kafka-producer in Phase 16.
- Resilience4j instrumentation in Phase 10, if core W1 is stable without
  it.
- Role visibility in the frontend (Phase 13 Task 4) -- owners can see a
  few extra buttons; customers will not suffer.
- Backup screen recordings for CP#1 or CP#2 (keep CP#3 recording).

### Merge second

- Phases 3 + 4 (Restaurant foundation + full API) in one session, if
  confident in Spring Boot.
- Phases 5 + 6 (same for Menu).
- Phases 12 + 13 (frontend shell + UX) in one session, if Vue experience
  exists.

### Never compress

- Phase 1 (auth contract).
- Phase 9 (team contract lock).
- Phase 15 (authorisation hardening).
- Phase 11 / 14 / 18 (checkpoint preparation).
- Phase 19 (buffer).

### When to re-plan vs compress

- If you are **one session** behind: compress inside the current
  week-block.
- If you are **two or more sessions** behind: re-plan the remaining
  phases explicitly in `dev-docs/decisions/` before continuing.

---

## Appendix C -- Team Coordination Points

These are the moments where at least one written agreement between team
members is required.

| When | What must be agreed | Who |
|------|---------------------|-----|
| Before Phase 1 | Java version, Maven, package structure, naming rules | All |
| During Phase 1 | Public vs protected routes, JWT claims, token propagation | All |
| Before Phase 2 | Gateway path prefixes, port assignments, env-var matrix | All |
| Before Phase 8 | Docker Compose service names, network, shared volumes | Backend owners |
| Before Phase 9 | Exact REST payloads for W1 hops, event topics and payloads | Backend owners |
| Before Phase 10 | Status-code contract for validation failures | Backend owners |
| Before Phase 14 | Gateway CORS origins for the frontend | Alfa-Kilo + Sierra-Lima |
| Before Phase 15 | Role-gating matrix and ownership-check approach | All |
| Before Phase 18 | Slide structure, demo path, fallback plan, speaking order | All |

When teammates are late, Sierra-Lima can still move by using stable IDs,
seeded demo data, locally issued dev tokens, documented assumptions, and
contract-first stubs. Then replace stubs once the real services arrive.

---

## Appendix D -- Risk Register

| # | Risk | Impact | Mitigation |
|---|------|--------|------------|
| 1 | Teammates still work from the old six-service assumption | Wrong integration and wrong scope at CP#1 | Use this roadmap as the corrected local baseline; share at next team meeting |
| 2 | Auth is implemented inconsistently across gateway and services | Broken logins, leaky endpoints | Front-load Phase 1; agree JWT claims and token propagation before hardening |
| 3 | Sierra-Lima builds good local services that do not fit the shared auth path | Rework after CP#1 | Implement against the agreed auth contract, not a solo-only shape |
| 4 | Static URLs drift across services | Run-time errors, late debugging | Maintain one central local-dev env matrix (§Phase 2 step 10) |
| 5 | Event names or payloads drift across teams | Consumers break silently | Lock contracts in Phase 9; keep one shared envelope example |
| 6 | Protected-read behaviour hurts UX and causes late debate | CP#2 risk | Decide explicitly in Phase 1 whether browse endpoints stay protected |
| 7 | **Shared-database shortcut during integration** | **Repeat of Assignment 1 penalty** | **Reject immediately. Cross-service references are IDs only.** |
| 8 | Docker or Kafka setup issues on Windows | Blocks multiple phases | Allocate extra time in Phase 8; WSL2 as fallback; document the exact version tags |
| 9 | Assignment 3 design changes after 2026-04-21 feedback | Rework APIs or models | Keep services small; design for easy change; log decisions |
| 10 | Scope expands back to all eight business services | Time crunch | Hold the A3 implementation subset unless the instructor explicitly asks for more |
| 11 | Diagrams and report fall behind code | Last-minute scramble | Phase 17 dedicated; refresh diagrams at each checkpoint, not only at the end |
| 12 | Checkpoint demo fails live | Lost marks | Screenshots / recording as backup; rehearse with Docker; Phase 19 buffer |
| 13 | Security postponed "to the final phase" | CP#3 surprise | Security is baked in from Phase 2 (stub) and Phase 7 (real filter) |
| 14 | `Review Service` pulled back into scope late | Time crunch near CP#3 | Only if the instructor explicitly asks; otherwise document as future work |

---

## Appendix E -- Assignment 1 Feedback to Address

From `dev-docs/prior-submissions/Assignment-1_Feedback.txt` (3.50/4.00):

> **-0.25**: Infrastructure elements mixed into architecture diagram.  
> **-0.25**: Shared database across microservices.

**Actions taken in this plan:**

- Architecture diagrams in the final report show business services only
  (no API Gateway, no DB containers, no Kafka in the logical diagram).
  Figure 1 (business) and Figure 1b (technical) are kept separate.
- Each implemented microservice has its **own PostgreSQL database** --
  enforced in Docker Compose with separate containers, volumes, and
  ports.
- Technical infrastructure (gateway, discovery, messaging) is treated as
  an implementation detail, not as part of the business architecture.

---

## Appendix F -- Canonical A3 Reference

This appendix pins the Assignment 3 design facts so the rest of this
file can be read without opening the PDF. If a conflict appears between
this appendix and `dev-docs/prior-submissions/Assignment-3-Submission.pdf`
at `a520963`, the **PDF is authoritative** -- update this appendix first,
then any phase that references the changed contract.

### F.1 Restaurant Service Endpoints (Sierra-Lima)

| Method | Path | Purpose | Auth |
|--------|------|---------|------|
| `POST` | `/restaurants` | Register a new restaurant profile | `RestaurantOwner` / `Admin` |
| `GET` | `/restaurants/{id}` | Get a restaurant profile | Public (or `Customer`, per team decision) |
| `PUT` | `/restaurants/{id}` | Update profile (hours, location, details) | Owner of this restaurant / `Admin` |
| `PATCH` | `/restaurants/{id}/status` | Toggle open/closed | Owner / `Admin` |
| `GET` | `/restaurants` | Search/list (filters: `city`, `isOpen`) | Public (or `Customer`) |
| `GET` | `/restaurants/{id}/availability` | Lightweight availability check; called by Order Service in W1 | Service or `Customer` |

**Availability response shape:**

```json
{
  "restaurantId": "00000000-0000-0000-0000-000000000000",
  "isOpen": true,
  "acceptsOrders": true,
  "operatingHours": "09:00-22:00",
  "checkedAt": "2026-05-05T12:34:56Z"
}
```

### F.2 Menu Service Endpoints (Sierra-Lima)

| Method | Path | Purpose | Auth |
|--------|------|---------|------|
| `POST` | `/restaurants/{rid}/menu-items` | Add a new menu item | Owner of `rid` / `Admin` |
| `GET` | `/restaurants/{rid}/menu-items` | List items for a restaurant (filters: `category`, `available`) | Public (or `Customer`) |
| `GET` | `/menu-items/{id}` | Get a single item | Public (or `Customer`) |
| `PUT` | `/menu-items/{id}` | Update item | Owner / `Admin` |
| `DELETE` | `/menu-items/{id}` | Remove item | Owner / `Admin` |
| `POST` | `/menu-items/validate` | Batch validate `{menuItemId, quantity}` list; return unit prices; called by Order Service in W1 | Service or `Customer` |

**Batch validation request shape:**

```json
{
  "items": [
    { "menuItemId": "00000000-0000-0000-0000-000000000001", "quantity": 2 },
    { "menuItemId": "00000000-0000-0000-0000-000000000002", "quantity": 1 }
  ]
}
```

**Batch validation response shape:**

```json
{
  "allValid": false,
  "items": [
    {
      "menuItemId": "00000000-0000-0000-0000-000000000001",
      "exists": true,
      "isAvailable": true,
      "unitPriceAmount": "12.50",
      "unitPriceCurrency": "EUR",
      "quantity": 2,
      "lineTotal": "25.00"
    },
    {
      "menuItemId": "00000000-0000-0000-0000-000000000002",
      "exists": false,
      "error": "MENU_ITEM_NOT_FOUND"
    }
  ],
  "totalAmount": "25.00",
  "currency": "EUR"
}
```

### F.3 Restaurant Service Data Model

`Restaurant` (aggregate root, one row per restaurant):

| Field | Type | Notes |
|-------|------|-------|
| `restaurantId` | UUID | PK |
| `ownerId` | UUID | Cross-service reference to `User.userId` |
| `name` | String | not blank |
| `address` | String | optional |
| `city` | String | indexed |
| `latitude` | Double | -90..90 |
| `longitude` | Double | -180..180 |
| `operatingHours` | String | e.g. `"09:00-22:00"` |
| `isOpen` | Boolean | default `false` |
| `createdAt` | LocalDateTime | audited |
| `updatedAt` | LocalDateTime | audited |

### F.4 Menu Service Data Model

`MenuItem` (aggregate root):

| Field | Type | Notes |
|-------|------|-------|
| `menuItemId` | UUID | PK |
| `restaurantId` | UUID | Cross-service reference to `Restaurant.restaurantId`; indexed |
| `name` | String | not blank |
| `description` | String | optional, max 2000 |
| `priceAmount` | BigDecimal | scale 2, positive |
| `priceCurrency` | String | ISO-4217, default `"EUR"` |
| `category` | String | e.g. `Appetizer`, `Main`, `Dessert`, `Drink` |
| `isAvailable` | Boolean | default `true` |
| `createdAt` | LocalDateTime | audited |
| `updatedAt` | LocalDateTime | audited |

### F.5 Gateway Path Map

| Gateway prefix | Target service | Owner |
|----------------|----------------|-------|
| `/api/auth/**`, `/api/users/**` | User Service | Alfa-Kilo |
| `/api/orders/**` | Order Service | Alfa-Kilo |
| `/api/restaurants/**` | Restaurant Service | Sierra-Lima |
| `/api/menu-items/**`, `/api/restaurants/*/menu-items/**` | Menu Service | Sierra-Lima |
| `/api/payments/**` | Payment Service | Elephant-Yankee |
| `/api/deliveries/**`, `/api/drivers/**` | Delivery Service | Elephant-Yankee |
| `/api/notifications/**` | Notification Service | Mike-Alfa |

### F.6 W1 Synchronous Call Chain (Place Order)

1. `Client -> API Gateway`: `POST /api/orders` with
   `Authorization: Bearer <token>` and body
   `{ customerId, restaurantId, items[], deliveryAddress }`.
2. `API Gateway -> Order Service`: gateway validates the token and
   forwards the request.
3. `Order -> User Service`: customer lookup via bearer token (verify the
   user exists and is active).
4. `Order -> Restaurant Service`:
   `GET /restaurants/{id}/availability` -- confirm the restaurant is
   open and accepting orders.
5. `Order -> Menu Service`: `POST /menu-items/validate` with the items
   list -- verify existence, availability, and unit prices.
6. `Order Service` persists the order with status `Placed`.
7. `Order -> Payment Service`: `POST /payments` with
   `{ orderId, amount }`. Synchronous charge.
8. On `Completed`: `Order -> Delivery Service`: `POST /deliveries` with
   pickup / dropoff. Order status transitions `Paid -> Confirmed`.
9. `Order Service` returns `201 Created` with the order id and first
   status.

**Failure handling.**

- Step 3 failure (customer invalid): `401 Unauthorized`.
- Step 4 failure (restaurant closed): `409 Conflict` (or `200` with
  `acceptsOrders:false`, per team agreement).
- Step 5 failure (menu items invalid or unavailable):
  `422 Unprocessable Entity` with per-item error breakdown.
- Step 7 failure (payment declined): mark order `Cancelled`; no delivery
  task created.
- Step 8 failure (delivery creation fails): compensating
  `POST /payments/{id}/refund` to cancel the charge; order `Cancelled`.

### F.7 W2 / W3 Event Contracts

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

**Optional Sierra-Lima stretch topic:** `menu-events`

| Event | Producer | Consumer(s) | Payload |
|-------|----------|-------------|---------|
| `menu.item-availability-changed` | Menu Service | Notification Service | `{ menuItemId, restaurantId, isAvailable, occurredAt }` |

**Envelope shape:** `{ id, type, occurredAt, payload }`. `id` is the
idempotency key.

**Delivery guarantees:**

- At-least-once.
- Consumers are idempotent (dedup by envelope `id`).
- Failed handlers route to a dead-letter topic named `<topic>.dlq`.

### F.8 Data Model Quick Reference (Other Services)

Sierra-Lima does not implement these, but the ER diagram committed at
`a520963` (Figure 2) shows the full picture. Summaries relevant to W1:

- **User Service:** `User(userId, email, passwordHash, fullName,
  phoneNumber, role, status)`; `Address(addressId, userId, ...)`;
  `DriverProfile(userId, licenceNumber, vehicleType, isAvailable)`.
- **Order Service:** `Order(orderId, customerId, restaurantId, status,
  placedAt, totalAmount, deliveryStreet, deliveryCity,
  deliveryPostalCode)`; `OrderItem(orderItemId, orderId, menuItemId,
  name, unitPrice, quantity)`.
- **Payment Service:** `Payment(paymentId, orderId, amount, currency,
  status, processedAt)`; `Transaction(transactionId, paymentId, type,
  amount, occurredAt)`.
- **Delivery Service:** `DeliveryTask(deliveryId, orderId, driverId,
  pickupStreet, pickupCity, dropoffStreet, dropoffCity, status,
  assignedAt)`.
- **Notification Service:** `Notification(notificationId, recipientId,
  channel, message, sentAt, status)`.

All cross-service references are ID-only. No foreign keys cross database
boundaries.

---

## Appendix G -- Directory Conventions

When Phase 0 is complete, the repository should look like this:

```
2026-esi-quickbite-personal/
  dev-docs/
    agent-context/           <- chat archives by date and callsign
    audits/                  <- roadmap audits
    course-materials/        <- lecture PDFs, assignment PDFs, README
    decisions/               <- 0001-... 0002-... per decision
    gap-analysis/
    instructor-feedback/
    prior-submissions/       <- A1, A2, A3 submissions + feedback
    roadmaps/                <- this file and earlier variants
  services/
    restaurant-service/      <- Maven + Spring Boot project
      src/main/java/ee/ut/esi/quickbite/restaurant/
      src/main/resources/application.properties
      src/main/resources/application-docker.properties
      src/main/resources/db/migration/V1__init.sql
      src/main/resources/db/migration/V2__seed_demo_data.sql
      src/test/java/...
      Dockerfile
      .dockerignore
      pom.xml
    menu-service/            <- Maven + Spring Boot project
      (same layout as restaurant-service)
    local-dev/
      docker-compose.yml
      .env.example
      README.md               <- runbook
      smoke.sh                <- optional smoke-test script
    frontend/                 <- Vue.js project (from Phase 12)
  .gitignore
  README.md
```

---

## Appendix H -- Memory for Future Agents

If a future coding agent resumes this plan, the most important facts to
keep in mind are:

1. This is **Sierra-Lima's personal early-start workspace**, not the
   official team repository.
2. Sierra-Lima owns **Restaurant Service and Menu Service**, not the
   gateway, not Kafka, not User Service.
3. **No shared DB, no Eureka, no infrastructure in business diagrams** --
   these are the expensive lessons from Assignment 1.
4. The **implementation subset is seven business services plus API
   Gateway plus Kafka**. Only `Review Service` is design-only.
5. **Bearer-token auth is baseline, not a final-phase add-on.** Stub it
   from Phase 2, harden it in Phase 7, role-lock it in Phase 15.
6. **W1 is the synchronous demo (Sierra-Lima is a callee).** W2 and W3
   are the asynchronous demos (Sierra-Lima is **not** a producer or
   consumer in A3 scope; this is intentional).
7. **Flyway migrations, not `ddl-auto=create`**, once the first `V1`
   migration is in place.
8. The A3 PDF is authoritative over Appendix F here.
9. When in doubt, re-read Section 2 (Principles) and Section 11 (Solo
   work) before acting.
10. Record every non-obvious decision in `dev-docs/decisions/`.

---

*End of master plan. The next step is to execute Phase 0 in the next
working session.*
