# Gap Analysis -- Project Brief 2026 vs. commit `7b2fa61`

| Field | Value |
| --- | --- |
| Commit under analysis | `7b2fa61` (`Patch Golf-Papa-Tango 60fa710 audit_3 F1-F2`) |
| Branch | `dev` |
| Repository owner | Sten-Qy-Li (Sierra-Lima, QuickBite Group 7) |
| Author of analysis | Charlie-Lima-Alfa (Claude Opus 4.7, acting for Sierra-Lima) |
| Date | 2026-04-20 |
| Source document | `dev-docs/course-materials/Project2026.pdf` (Enterprise System Integration Project Guidelines 2026) |
| Scope anchor | Sierra-Lima's owned subset per `dev-docs/decisions/0001-scope-freeze.md`: Restaurant Service (R19/R20) + Menu Service (R21/R22). No integration-component swap. |

## 1. Purpose

The Project Brief reframes what the repository will be graded on: a
running microservices system, a frontend, basic security, Docker, and
an asynchronous interaction, measured across three dated checkpoints.
This document compares the current state of the repository at commit
`7b2fa61` against every requirement the brief lays out, and surfaces
the gaps that remain before each of the three checkpoints.

Two framing rules apply throughout:

- The brief is explicit that "you are not designing a new system.
  You are implementing" Assignments 1, 2, and 3. So the test is
  whether the code at `7b2fa61` delivers what Assignment 3 and the
  decision pack already committed to, **not** whether it satisfies
  some alternative design.
- The brief grades **individually**. Sierra-Lima's personal rubric is
  "2 microservices" per §2, not "1 microservice + 1 integration
  component". Teammate-owned services (User, Order, Payment,
  Delivery, Notification) and teammate-owned shared components
  (Spring Cloud Gateway, Kafka broker) are out of scope for
  Sierra-Lima's grade and therefore out of scope for this gap
  analysis except where Sierra-Lima depends on them for an
  integrated demo.

## 2. Evidence used

**Project Brief** (`dev-docs/course-materials/Project2026.pdf`, 4 pages):

- §1 Objective and implementation rules.
- §2 Individual Responsibilities (2 services OR 1 service + 1
  integration/resilience component; responsibilities fixed after A3).
- §3 Technical Requirements (system-level + per-service layering +
  at least one async workflow + ~5-8 endpoints per service).
- §4.1 Checkpoint #1 (Backend: Service 1 Complete, 12 pts, 05 May 2026).
- §4.2 Checkpoint #2 (Service 2 / Integration + Frontend, 8 pts, 12 May 2026).
- §4.3 Checkpoint #3 (Final System, 10 pts, 19 May 2026).

**Repository anchors** re-read at `7b2fa61`:

- Decisions `0001`, `0010`, `0020`, `0030`, `0031`, `0032`, `0033`, `0040`.
- Controllers `RestaurantController.java`, `MenuController.java`.
- Service-layer implementations `RestaurantService.java`, `MenuService.java`.
- Security config `config/SecurityConfig.java` + `security/JwtAuthFilter.java` on both services.
- OpenAPI config `config/OpenApiConfig.java` on both services.
- DB migrations `V1__init.sql`, `V2__seed_demo_data.sql` on both services.
- Test suites under `src/test/java/...` on both services.
- Frontend `services/frontend/quickbite-frontend/src/**`.
- Docker stack `services/local-dev/docker-compose.yml`.
- Smoke and Postman assets `services/local-dev/smoke*.{sh,ps1}`, `services/local-dev/postman/*`.
- Prior audits (same repo) for continuity of findings, especially
  `audit-bcc9dd0_Charlie-Lima-Alfa_final-handover-readiness.md` and
  `audit-7b2fa61_Golf-Papa-Tango_team-lead-integration-readiness_1.md`.

No live execution was performed for this gap analysis; the most
recent live verification is the Golf-Papa-Tango audit at `7b2fa61`
dated 2026-04-20, which recorded `33/33` + `47/47` backend tests
green, frontend lint/build green, full compose rebuild healthy,
Newman `39/39` requests with `68` assertions and `0` failures, and
passing cross-service smoke with `0` Sierra-Lima failures.

## 3. Coverage matrix -- Brief requirement → repo evidence

Legend: **Met** = fully satisfied at `7b2fa61`; **Partial** =
satisfied for Sierra-Lima's owned slice but depends on teammate code
for the integrated picture; **Gap** = not satisfied.

### 3.1 Brief §3.1 System-level requirements

| # | Requirement | Status | Evidence |
|---|---|---|---|
| S1 | Multiple interacting services | Partial | Menu Service calls Restaurant Service synchronously via `RestaurantOwnershipClient` (`services/menu-service/src/main/java/.../security/RestaurantOwnershipClient.java`) -- wired through `GET /restaurants/{id}/availability` and the ownership lookup during menu writes. Teammate services (User, Order, Payment, Delivery, Notification) are not in this repo. |
| S2 | A frontend application | Met | Vue 3 SPA at `services/frontend/quickbite-frontend/` with `src/router/index.js` (12 routes), `src/api/client.js` wrapping `fetch` with JWT bearer injection, and per-endpoint views. |
| S3 | Basic security (authN + authZ) | Met | JWT HS256 filter chain in both services (`JwtAuthFilter.java`, `SecurityConfig.java`). Issuer pinned via `JWT_ISSUER`. Role matrix per `0010 §8` enforced by `hasAnyRole(...)` plus ownership checks in service layer. Public browse routes distinguished from protected mutations. |
| S4 | All services running together (Docker) | Met for Sierra-Lima's slice | `services/local-dev/docker-compose.yml` boots both services, both Postgres DBs, the frontend, and an opt-in `dev-gateway` profile. Healthchecks defined. Teammate services are not included because they are not in this repo. |

### 3.2 Brief §3.2 Per-service layering + endpoint + async + DB

Applied separately to each Sierra-Lima service.

#### Restaurant Service

| Layer / rule | Status | Evidence |
|---|---|---|
| Controller (API) only -- no business logic or DB access | Met | `controller/RestaurantController.java` is a thin 6-method delegate; no `@Transactional`, no repository calls. |
| DTO layer separates API contract from domain | Met | `dto/CreateRestaurantRequest.java`, `dto/UpdateRestaurantRequest.java`, `dto/UpdateRestaurantStatusRequest.java`, `dto/RestaurantResponse.java`, `dto/AvailabilityResponse.java`. Entity `Restaurant` is never returned directly. |
| Service (business logic) | Met | `service/RestaurantService.java` holds ownership checks, duplicate-name protection, operating-hours evaluation against `Europe/Tallinn`, status toggling. |
| Repository (persistence) | Met | `repository/RestaurantRepository.java` (Spring Data JPA; no business rules inside). |
| Domain model reflects A3 data model | Met | `domain/Restaurant.java` + `domain/Location.java` mirror `0020 §4.1` and A3 ER diagram. Flyway `V1__init.sql` matches the schema byte-for-byte. |
| Own database | Met | `quickbite-restaurant-db` (Postgres 15) with `restaurant_db_data` named volume. Separate from `menu-db`. |
| REST endpoints ~5-8 | Met | 6 endpoints (see `0020 §1`): `POST /restaurants`, `GET /restaurants/{id}`, `GET /restaurants`, `PUT /restaurants/{id}`, `PATCH /restaurants/{id}/status`, `GET /restaurants/{id}/availability`. |
| At least one async workflow | Not required here | Sierra-Lima's async is on the Menu side per `0040`. Restaurant Service is intentionally a non-participant in W2/W3. This matches the brief's wording "at least one" at the system level, not per-service. |

#### Menu Service

| Layer / rule | Status | Evidence |
|---|---|---|
| Controller (API) only -- no business logic or DB access | Met | `controller/MenuController.java` is a thin 6-method delegate. |
| DTO layer | Met | `dto/CreateMenuItemRequest.java`, `dto/UpdateMenuItemRequest.java`, `dto/MenuItemResponse.java`, `dto/ValidateMenuItemsRequest.java`, `dto/ValidateMenuItemsResponse.java`. |
| Service (business logic) | Met | `service/MenuService.java` holds ownership checks (via `RestaurantOwnershipClient`), price validation, category warnings, batch-validate mixed-currency rejection, availability-transition detection. |
| Repository (persistence) | Met | `repository/MenuItemRepository.java`. |
| Domain model reflects A3 data model | Met | `domain/MenuItem.java` + `domain/Price.java` (value object). Flyway `V1__init.sql` matches `0020 §4.2`. |
| Own database | Met | `quickbite-menu-db` (Postgres 15) with `menu_db_data` named volume. |
| REST endpoints ~5-8 | Met | 6 endpoints (see `0020 §2`): `POST /restaurants/{rid}/menu-items`, `GET /restaurants/{rid}/menu-items`, `GET /menu-items/{id}`, `PUT /menu-items/{id}`, `DELETE /menu-items/{id}`, `POST /menu-items/validate`. |
| At least one async workflow (system-level) | Partial -- see §5 finding F1 | `events/LoggingMenuEventPublisher.java` emits `menu.item-availability-changed` JSON at INFO on logger `menu-events` when `PUT /menu-items/{id}` flips `isAvailable`. Envelope matches `0032 §6` verbatim. Transport is log-only per `0040 §2`; no bytes cross the wire to a broker at `7b2fa61`. |

### 3.3 Brief §4.1 Checkpoint #1 (12 pts, 05 May 2026)

Applied individually -- each student must demonstrate ONE service.
Sierra-Lima can defend either Restaurant Service or Menu Service.

| Brief item | Status | Evidence |
|---|---|---|
| A. Running Service | Met | Both services start locally under `docker compose up` (see runbook `services/local-dev/runbook.md`). Actuator health probes configured. |
| B. API Implementation (all endpoints per A3) | Met | 6/6 Restaurant endpoints, 6/6 Menu endpoints -- see §3.2 above. Matches `0020 §1` and `0020 §2`. |
| C. OpenAPI / Swagger UI available + testable | Met | `config/OpenApiConfig.java` in both services. `/swagger-ui.html` and `/v3/api-docs` are `permitAll`ed in both `SecurityConfig.java`. Controllers carry `@Tag`, `@Operation`, `@ApiResponse` annotations. |
| D. Persistence (DB connected, data stored/retrieved) | Met | Flyway migrations `V1__init.sql` + `V2__seed_demo_data.sql` on both services. Dockerised Postgres with healthchecks. Seed: 6 restaurants + 16 menu items per `0020 §5`. |
| E. Testing -- `@WebMvcTest`-class (or equivalent); happy-path + error-case; endpoint involves a dependency on another component; mocks allowed | Met | `MenuControllerTest` (`@SpringBootTest + @AutoConfigureMockMvc`) mocks `RestaurantOwnershipClient` (dependency on Restaurant Service) -- 341 lines with both success paths and error paths (e.g. `listForRestaurant_isPublic`, `create_returns404_whenRestaurantMissing`). `MenuServiceTest` (397 lines) and `RestaurantServiceTest` (228 lines) and `RestaurantControllerTest` (311 lines) add unit and slice coverage. Golf-Papa-Tango `7b2fa61` audit recorded `33/33` + `47/47` green. |
| F. API Demonstration (Postman or Swagger) | Met | `services/local-dev/postman/QuickBite.postman_collection.json` + `.postman_environment.json`. Last Newman run: `39` requests, `68` assertions, `0` failures. Swagger UI available at each service on `/swagger-ui.html`. |

Checkpoint-1 verdict for Sierra-Lima's slice: **all deliverables are
present, testable, and documented**, for either service. No gap
blocks the 12-point defence.

### 3.4 Brief §4.2 Checkpoint #2 (8 pts, 12 May 2026)

Per §2 Sierra-Lima owns 2 services (no integration-component swap),
so the §4.2 "Second Service" path applies.

| Brief item | Status | Evidence |
|---|---|---|
| A. Second Service runs (A + B + D from CP#1, without tests/docs requirement) | Met | Both services are fully implemented at CP#1 quality, not CP#2-reduced quality. `MenuService`/`RestaurantService` share equivalent depth of layering, DTOs, validation, and auditing. |
| B. Basic Integration -- at least one working interaction between two implemented services (real call, not mocked) | Met | `MenuService.create()` and `MenuService.requireOwnerOrAdmin()` call `RestaurantOwnershipClient.findOwnerId()` which performs a live `GET /restaurants/{id}` against Restaurant Service over the compose network. Reproducible via `services/local-dev/smoke-cross-service.{sh,ps1}`; see `services/local-dev/evidence/cross-service-smoke_20260420T054823Z.log` (untracked). |
| C. Frontend calls at least one backend endpoint per student and displays real data | Met | Vue views call 8 of Sierra-Lima's 12 endpoints with real data: `GET /restaurants` (`RestaurantListView.vue:91`), `GET /restaurants/{id}` (`RestaurantDetailView.vue:158`, `MenuView.vue:106`), `POST /restaurants` (`AddRestaurantView.vue:127`), `PUT /restaurants/{id}` (`RestaurantDetailView.vue:216`), `PATCH /restaurants/{id}/status` (`RestaurantDetailView.vue:230`), `GET /restaurants/{rid}/menu-items` (`MenuView.vue:120`), `GET /menu-items/{id}` (`MenuItemDetailView.vue:148`), `POST /restaurants/{rid}/menu-items` (`AddMenuItemView.vue:157`), `PUT /menu-items/{id}` (`MenuItemDetailView.vue:201`). |

Checkpoint-2 verdict for Sierra-Lima's slice: **all 8 points are
defensible from the current repo**. Gap items affecting CP#3 are
listed in §5.

### 3.5 Brief §4.3 Checkpoint #3 (10 pts, 19 May 2026)

| Brief item | Status | Evidence / gap |
|---|---|---|
| A. All Responsibilities Completed (2 services or 1 service + 1 integration) | Met for Sierra-Lima | Menu + Restaurant -- both shipped at CP#1 quality. |
| B. Full System Running in Docker Compose | Partial | `docker-compose.yml` boots Sierra-Lima's slice plus the `dev-gateway` nginx stub. Teammate services (User, Order, Payment, Delivery, Notification) and the real Spring Cloud Gateway + Kafka broker are not in this repo -- by scope design (`0001`). The "full system" claim is only achievable when the team's combined compose assembly is available. **This is a cross-team dependency, not a Sierra-Lima gap.** |
| C. Frontend Fully Functional (all endpoints triggerable + error handling) | Partial -- see finding F2 | 9/12 Sierra-Lima endpoints have UI triggers. 3 endpoints have no UI path: `DELETE /menu-items/{id}`, `POST /menu-items/validate`, `GET /restaurants/{id}/availability`. The last two are B2B endpoints consumed by teammate Order Service in W1; the brief's wording invites scrutiny of whether they must be reachable from the UI. Error handling is in place: `src/api/client.js` normalises `401` into a login redirect and parses JSON error bodies into `ApiError.message`. |
| D. Security (authN + authZ applied to endpoints) | Met | JWT HS256 end-to-end: login page issues token via `POST /api/auth/login` (User Service), stored in `localStorage`, attached by `src/api/client.js`. Each Sierra-Lima service re-validates locally. Role-based `hasAnyRole` plus service-level ownership checks. Direct probes in the Golf-Papa-Tango audit confirmed wrong-issuer JWTs return `401` and non-owner actors receive `403`. |
| E. Asynchronous Communication -- at least one event/message-based interaction between services | Gap -- see finding F1 | At `7b2fa61` the only Sierra-Lima async surface is the log-only `LoggingMenuEventPublisher`. The envelope is contract-correct (`0032 §6`), but no bytes cross a process boundary to a consumer -- "between services" as the brief phrases it is not satisfied by Sierra-Lima standalone. The baseline async demo in `0040 §1` relies on teammate-owned W2 (Delivery→Order/Notification) and W3 (Payment→Order/Notification) over Kafka; those producers and consumers are not in this repo. |

## 4. Deadline position

Today is **2026-04-20**. The brief deadlines relative to today:

- CP#1: **+15 days** (05 May 2026). Sierra-Lima is **code-complete**
  for this checkpoint and has been since Phase 11 (commit `5967d13`).
  Remaining CP#1 work is rehearsal: walking the demo script,
  capturing fresh Swagger screenshots, re-running the Postman
  collection against a pristine DB, and practising the 15-minute
  discussion slot.
- CP#2: **+22 days** (12 May 2026). Sierra-Lima is code-complete
  for this checkpoint since Phase 14 (commit `a5fc5f9`). Remaining
  CP#2 work is integration rehearsal with teammate services once
  their APIs are reachable.
- CP#3: **+29 days** (19 May 2026). Sierra-Lima's own CP#3 surface
  is code-complete since Phase 19 (commit `50774fe`). The single
  non-closed item in Sierra-Lima's scope at `7b2fa61` is the async
  transport choice (§5 F1). Everything else in CP#3 hinges on the
  team's joint integration, not on Sierra-Lima-local code.

Implication: Sierra-Lima has **buffer** against every checkpoint.
Time between now and 05 May should go to integration rehearsal with
teammates, not to new feature work on Sierra-Lima's services.

## 5. Findings and gaps

Findings are numbered `F{n}` for easy reference in later audits.
Severity is relative to the Project Brief's rubric, not to overall
code quality.

### F1. Medium -- Async transport is log-only; no bytes cross services

- **Brief reference:** §4.3 E (CP#3, 3 points shared with A).
- **Location:** `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/events/LoggingMenuEventPublisher.java:33-54`.
- **Decision pointer:** `dev-docs/decisions/0040-phase-16-async-stance.md §2`.
- **Claim:** Sierra-Lima publishes `menu.item-availability-changed`
  to a named logger, not to a broker. Envelope and emit-rule are
  correct (`0032 §6`, `0040 §4`). The receiver is `slf4j`, not
  another service.
- **Why the brief flags it:** §4.3 E literally says "event/message-based
  interaction **between services**". A log line, no matter how
  structured, does not cross a service boundary in transport terms.
- **What keeps the risk Medium rather than High:**
  1. The *interface* seam (`MenuEventPublisher`) is correct, so a
     drop-in Kafka implementation is a one-class change -- `0040 §2`
     pre-commits to this.
  2. The brief says "at least one event/message-based interaction
     between services" at the *system* level, not per-student. If
     the team's W2 (Delivery → Notification/Order) or W3 (Payment →
     Notification/Order) demo lands on the broker by 19 May, the
     system-level rubric is already satisfied by teammate-owned
     producers/consumers.
  3. Decision `0040 §1` intentionally keeps Sierra-Lima a
     non-participant in W2/W3 baseline so the CP#1/CP#2 demo surface
     stays stable; this trade-off is documented and ratified.
- **Residual risk:** if the teammate async demo slips on 19 May (no
  broker, or producers/consumers not wired), Sierra-Lima is the
  only async surface the discussion panel can interrogate, and the
  rubric language "between services" may be held against the
  log-only transport.
- **Recommendation (ordered):**
  1. Confirm on the 2026-04-21 session (or Slack) that Mike-Alfa's
     Kafka broker and teammate producers/consumers are on track for
     19 May. Record confirmation or gap in a new decision file.
  2. If teammate async is at risk, implement
     `KafkaMenuEventPublisher implements MenuEventPublisher` plus
     the `spring-kafka` dep in `services/menu-service/pom.xml`, wired
     behind an env flag (`MENU_EVENTS_TRANSPORT=kafka`) so the
     log-only default stays the demo-safe path. `0040 §2` rationale
     pre-approves this swap.
  3. If neither happens, scope the CP#3 E answer explicitly around
     the contract-locked envelope + the publisher seam + the log
     line, with the "the transport is a single-file swap" narrative
     from `0040 §2`. Prepare but do not rely on it.

### F2. Low -- Three Sierra-Lima endpoints have no UI trigger

- **Brief reference:** §4.3 C (CP#3, 2 points).
- **Endpoints without a UI trigger at `7b2fa61`:**
  - `DELETE /menu-items/{id}` -- no button in `MenuItemDetailView.vue`
    or `MenuView.vue`; `src/api/client.js` supports `api.delete` but
    no view calls it.
  - `POST /menu-items/validate` -- B2B-only in `0010 §8` and
    `0020 §2.6`. Called by Order Service during W1.
  - `GET /restaurants/{id}/availability` -- B2B-only in `0010 §8` and
    `0020 §1.6`. Called by Order Service during W1.
- **Brief wording reading:**
  - §4.3 C says "Fully functional, i.e., all the endpoints of the
    implemented microservices can be triggered from the frontend".
    Taken literally, this includes `validate` and `availability`
    even though they were designed as service-to-service.
  - §4.3 C also says "It handles errors in responses from the
    backend" -- error handling is already centralised in
    `src/api/client.js`.
- **Why the risk is Low not Medium:**
  1. `validate` and `availability` have a credible "this is consumed
     by Order Service during W1, not by the UI" defence rooted in
     `0010 §8` and `0020`. The examiner may accept the B2B framing.
  2. `DELETE /menu-items` is the only genuinely UI-missing path.
     The owner workflow can still be demonstrated via curl /
     Postman and the endpoint itself is covered by tests.
- **Recommendation (ordered):**
  1. Before CP#3 (19 May), add a delete button to
     `MenuItemDetailView.vue` for owners/admins, wired to
     `api.delete`. Blast radius: one view + router refresh.
     Closes the single "can be triggered from the frontend" gap
     that has no B2B defence.
  2. Consider adding a small "diagnostic" screen (gated to
     `Admin`) that lets the examiner exercise `validate` and
     `availability` from the browser during the discussion. This
     is optional polish, not required.

### F3. Low -- "Full system running" depends on teammate compose that is not in this repo

- **Brief reference:** §4.3 B (CP#3, 2 points).
- **Claim:** Sierra-Lima's `docker-compose.yml` boots her two services,
  two DBs, her frontend build, and an nginx `dev-gateway` stub. It
  does **not** boot User, Order, Payment, Delivery, Notification, the
  real Spring Cloud Gateway, or Kafka.
- **Why this is Low not Medium:** the brief §2 places the API
  Gateway and Messaging/Event infrastructure in the "integration
  components" list -- owned here by Alfa-Kilo (gateway) and Mike-Alfa
  (broker) per `0001`. Sierra-Lima's rubric entry B is met **by the
  team's joint compose**, not by her personal repo. Decision `0001`
  and `0040` both pre-commit to this split.
- **Residual risk:** if the team has not produced a joint
  `docker-compose.yml` that includes Sierra-Lima's services by 19
  May, CP#3 B cannot be demonstrated end-to-end even though
  Sierra-Lima's slice is demo-ready.
- **Recommendation:** during the next team lead sync, confirm who
  owns the combined compose file and request an early mock-up.
  Sierra-Lima's services already accept all their configuration
  via environment variables so no code change is expected on her
  side.

### F4. Informational -- Untracked evidence artefacts

- **Claim:** `git status` at `7b2fa61` shows eight untracked files in
  `services/local-dev/evidence/` (three `cross-service-smoke_*.log`,
  three `menu-events_*.log`) plus the Project Brief PDF and a Golf-Papa-Tango
  audit from 2026-04-20. None of them are required by the Brief or
  by the scope freeze.
- **Why this is Informational:** prior audits already expect
  `evidence/` to be untracked working scratch; the Golf-Papa-Tango
  audit was produced within a minute of HEAD and its commit is part
  of normal cadence. The Project Brief file is now checked in here
  -- the correct gitignored home is `dev-docs/course-materials/`.
- **Recommendation:** leave as-is unless the team lead asks for the
  brief to be tracked; Sierra-Lima's pattern to date is to keep
  evidence runs local.

## 6. Explicit non-gaps

These are items a naive reading of the brief might flag. Each is
explicitly a non-gap once scope freeze (`0001`) and prior audits are
applied:

- **No API Gateway code in this repo.** Spring Cloud Gateway is
  Alfa-Kilo's component. Sierra-Lima's `dev-gateway` nginx is a
  local stand-in for demos; it does not need to be real per `0001`.
- **No Kafka dependency in `services/menu-service/pom.xml`.** Decision
  `0040 §2` keeps it out deliberately. Adding it is contingent on
  finding F1.
- **`Review Service` is absent.** `0001 §3` flags it as
  design-only.
- **Restaurant Service emits no events.** `0040 §1` keeps it a
  non-participant in W2/W3; the brief's "at least one" is a
  system-level floor.
- **Newman's `PUT /restaurants/{id}` has no assertion block.**
  Flagged as Medium by Golf-Papa-Tango at `7b2fa61`; it is a
  Postman-pack coverage limitation, not a Brief gap. Tracked in the
  Golf-Papa-Tango audit, not duplicated here.
- **Frontend operating-hours regex is looser than backend.**
  Flagged as Low by Golf-Papa-Tango at `7b2fa61`; backend validation
  still protects persistence. Brief rubric is about whether the
  endpoint can be triggered, not about client-side UX symmetry.

## 7. Verdict and next actions

At commit `7b2fa61`, Sierra-Lima's owned subset **meets or
exceeds** every rubric item the Project Brief sets for Checkpoints
#1 and #2, and meets every Checkpoint #3 item **except** §4.3 E
(one soft gap, F1) and the UI-delete thread in §4.3 C (one soft
gap, F2). Neither requires a large intervention.

Highest-leverage actions between today (2026-04-20) and the first
deadline (2026-05-05):

1. **Lock cross-team expectations for the async demo.** Confirm
   with Mike-Alfa whether the Kafka broker + teammate
   producers/consumers will exist by 2026-05-19. Record the answer
   in a new decision (`0041-cp3-async-demo-commitment.md` or
   similar). Drives F1's branch: log-only (stay) or Kafka (swap).
2. **Add the `DELETE /menu-items` UI control.** Single-file
   change. Closes F2's only non-B2B residue.
3. **Rehearse the CP#1 15-minute defence.** Walk Swagger →
   Postman → one @WebMvcTest → the live endpoint. Sierra-Lima has
   two services to choose from; pick the one with the richer test
   suite (`menu-service`, 47 tests) unless the examiner prefers
   simpler domain.
4. **Re-run `docker compose down -v && docker compose up`** at
   least once before CP#1 against pristine volumes, so the
   Postman `PUT /restaurants` path is green on a clean run (the
   Golf-Papa-Tango F1 at `7b2fa61` is only triggered by repeat
   runs against a stale volume).

Sierra-Lima does **not** need to add new features to Restaurant or
Menu Service between now and any checkpoint.
