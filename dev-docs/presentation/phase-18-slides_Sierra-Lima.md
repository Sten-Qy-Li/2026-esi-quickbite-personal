# CP#3 Presentation Deck -- Sierra-Lima slice

- **Owner.** Sierra-Lima (drives the slide build; sequences team
  speaking order with Alfa-Kilo, Elephant-Yankee, Mike-Alfa).
- **Source.** `Charlie-Lima-Alfa_a520963_project-phases-final.md`
  Phase 18 task 1 (eight bullets) + tasks 3-5.
- **Audience.** Course graders, ESI 2026 Checkpoint #3 (2026-05-19).
- **Budget.** 25 min talk + 10 min Q&A. Slides = ~14 content panes
  + cover + closing -> ~16 slides at ~90 s each.
- **Source of truth for content.** `dev-docs/report-draft-backend_Sierra-Lima.md`
  (the Phase 17 report). Slides quote from it; do not invent new
  numbers. Section cites in `(report §X.Y)` form on every slide so
  the speaker can flip to the report if challenged.
- **Format.** This file is a Markdown outline; a slide is one `## Sn`
  block with `Title`, `Visual`, `Talk track`, `Hand-off`. The team
  can render to PowerPoint / Keynote / pdfpc without rewriting any
  numbers -- every fact is checkable against the report or an ADR.

---

## Speaking-part assignments (Phase 18 task 3)

Mapping kept here so the deck and the speaking-order ledger stay
aligned. Re-confirm at the 2026-05-18 rehearsal.

| Block | Slides | Speaker | Backup speaker |
|-------|--------|---------|----------------|
| Cover + system overview | S1, S2 | Alfa-Kilo | Sierra-Lima |
| Service catalogue + DDD boundaries | S3 | Alfa-Kilo | Sierra-Lima |
| Sierra-Lima services (Restaurant + Menu) | S4, S5 | Sierra-Lima | Alfa-Kilo |
| API design (Swagger walk) | S6 | Sierra-Lima | Alfa-Kilo |
| Synchronous integration (W1, hops 4-5) | S7, S8 | Alfa-Kilo | Sierra-Lima |
| Asynchronous integration (W2 / W3) | S9, S10 | Mike-Alfa | Elephant-Yankee |
| Security (JWT + role + ownership) | S11, S12 | Sierra-Lima | Alfa-Kilo |
| Frontend walkthrough | S13 | Sierra-Lima | Alfa-Kilo |
| Resilience demo (optional) | S14 | Mike-Alfa | Sierra-Lima |
| Sacrifices + future work | S15 | Alfa-Kilo | Sierra-Lima |
| Closing + Q&A handover | S16 | rotating | -- |

Hand-off rule: the closing line of every slide names the next
speaker so the room never goes silent in transition. Hand-off lines
are recorded in each slide's `Hand-off:` field.

---

## S1 -- Cover

- **Title.** *QuickBite -- A Microservices Food-Delivery Platform.*
- **Subtitle.** Group 7 -- Alfa-Kilo, Sierra-Lima, Elephant-Yankee,
  Mike-Alfa. ESI 2026 Checkpoint #3.
- **Visual.** Group logo placeholder + cover photo of the SPA home
  view (capture: `dev-docs/verification/swagger-restaurant.png`-style
  placeholder, or just the four service tiles).
- **Talk track (Alfa-Kilo).** "We've built seven of the eight
  bounded contexts from Assignment 2 across one synchronous workflow
  and two asynchronous workflows. In the next 25 minutes you'll see
  the architecture, three live demos, and the trade-offs we made."
- **Hand-off.** "I'll start with the architecture overview -- next
  slide."

---

## S2 -- System overview and architecture

- **Visual.** ASCII reproduction of the deployment view from
  `dev-docs/report-draft-backend_Sierra-Lima.md` §3.1. Show the
  browser -> nginx -> API Gateway fan-out plus the Kafka backbone.
- **Talk track (Alfa-Kilo).** Three layers:
  1. **Edge.** Vue 3 SPA on nginx (port 8090) -> API Gateway
     (port 8080). One origin from the browser's perspective; the
     gateway strips `/api` and routes to the right service.
  2. **Services.** Seven Spring Boot 3.3 services on Java 21, each
     with its own Postgres database. No service shares a schema
     (CP#1 talking-points §3).
  3. **Async backbone.** Kafka broker; four event topics (`order-`,
     `payment-`, `delivery-`, `notification-events`) plus one
     log-only stretch topic (`menu-events`) emitted by Sierra-Lima
     (report §6.2).
- **Numbers to call out.** "Seven services + one gateway + one
  broker + one frontend + seven databases. Eight bounded contexts
  from Assignment 2; `Review` stays design-only -- we'll justify
  that on slide 15."
- **Hand-off.** "Next slide is the service catalogue."

---

## S3 -- Service catalogue and ownership

- **Visual.** Table; one row per service, columns: Service, Port,
  Owner callsign, Workflow role.

  | Service | Port | Owner | Role in W1 / W2 / W3 |
  |---|---|---|---|
  | User | 808x | Alfa-Kilo | mints JWTs (W1 hop 1) |
  | Order | 808x | Alfa-Kilo | orchestrates W1; consumes W2/W3 |
  | Restaurant | 8081 | Sierra-Lima | W1 hop 4 (callee) |
  | Menu | 8082 | Sierra-Lima | W1 hop 5 (callee); emits `menu-events` |
  | Payment | 808x | Elephant-Yankee | W3 producer |
  | Delivery | 808x | Elephant-Yankee | W2 producer |
  | Notification | 808x | Mike-Alfa | terminal consumer |
  | Review | -- | (design-only) | not implemented (S15 / report §11.2) |

- **Talk track (Alfa-Kilo).** Seven services, four owners, two
  workflows. Each service has its own Postgres; cross-service
  references are UUIDs only -- no foreign keys across boundaries
  (Assignment 1 feedback addressed -- CP#1 talking-points §3).
- **Hand-off.** "Sierra-Lima owns the next two slides on
  Restaurant + Menu."

---

## S4 -- Sierra-Lima: Restaurant Service

- **Visual.** Two-column layout. Left: ER table for `restaurant`
  (six business columns + audit) from report §4.1. Right: endpoint
  list from §5.1.
- **Talk track (Sierra-Lima).** Restaurant Service is the
  authoritative source for *who owns what*, *what hours we keep*,
  and *can we accept orders right now*. Six endpoints:
  - Public browse (`GET /restaurants`, `GET /restaurants/{id}`)
    -- `permitAll()`.
  - Owner CRUD (`POST`, `PUT`, `PATCH .../status`) -- role
    `RestaurantOwner` or `Admin`, **and** the actor must be the
    owner of that restaurant (Phase 15 ownership check).
  - W1 hop 4 (`GET /restaurants/{id}/availability`) -- any
    authenticated caller; returns `acceptsOrders` derived from
    `isOpen && within(operatingHours, now())`.
- **Highlights.** 23 / 23 JUnit green; `restaurant_db` on port
  5432 in compose; embedded `Location` value object validated for
  lat/long range (report §4.1).
- **Hand-off.** "Same speaker, next slide -- Menu."

---

## S5 -- Sierra-Lima: Menu Service

- **Visual.** ER table for `menu_item` (report §4.2) on the left;
  endpoint list (§5.2) on the right. Highlight the W1 hop 5 row.
- **Talk track (Sierra-Lima).** Menu Service owns items, prices,
  and per-item availability under a parent restaurant. Six endpoints
  mirror Restaurant's pattern:
  - Public browse + read.
  - Owner CRUD with the same Phase 15 ownership check, but Menu
    doesn't store `ownerId` -- it resolves it via a synchronous
    `GET /restaurants/{id}` call to Restaurant Service
    (`RestaurantOwnershipClient`, report §7.3).
  - W1 hop 5 (`POST /menu-items/validate`) -- batch existence +
    availability + line totals + grand total in one call.
  - The *only* event Sierra-Lima publishes:
    `menu.item-availability-changed`, log-only transport
    (`menu-events` logger), envelope verbatim from ADR 0032 §6.
- **Highlights.** 42 / 42 JUnit green (20 service + 20 controller
  + 1 events + 1 context-load). `menu_db` on port 5433. Currency is
  ISO-4217 `EUR` throughout the seed.
- **Hand-off.** "Now the API design through Swagger -- next slide."

---

## S6 -- API design (Swagger walk)

- **Visual.** Two screenshots side by side:
  `dev-docs/verification/swagger-restaurant.png`,
  `dev-docs/verification/swagger-menu.png`.
  Captured during the 2026-05-18 rehearsal against the live
  compose stack.
- **Talk track (Sierra-Lima).** Both services expose OpenAPI at
  `/v3/api-docs` and Swagger UI at `/swagger-ui.html`. Each endpoint
  is `@Operation` + `@ApiResponses` annotated; the tag groups
  (`Restaurants`, `Menu items`) match the service boundary. The
  Phase 15 authorisation column in our endpoint tables (report
  §5.1, §5.2) maps 1-to-1 to the Spring `@PreAuthorize` matrix in
  `SecurityConfig`.
- **Live moment.** Open `http://localhost:8081/swagger-ui.html`
  briefly; click `GET /restaurants/{id}/availability`; show the
  `200 / 404` response schemas. Pre-emptive screenshot in the deck
  in case the live stack fails.
- **Hand-off.** "Synchronous integration is next -- back to
  Alfa-Kilo."

---

## S7 -- Synchronous integration: W1 hops 4 and 5

- **Visual.** Sequence diagram (text rendition; the canonical
  asset is `dev-docs/prior-submissions/assignment-3_figure3_workflow-w1-sequence.png`).
  Highlight Sierra-Lima rows.

  ```
  Client -> Gateway -> Order
                        |-- hop 4 --> Restaurant.GET /restaurants/{id}/availability
                        |             <- AvailabilityResponse {acceptsOrders, ...}
                        |-- hop 5 --> Menu.POST /menu-items/validate
                        |             <- ValidateMenuItemsResponse {allValid, totalAmount, ...}
                        |-- hops 6-8 -> Payment / Delivery
  ```

- **Talk track (Alfa-Kilo).** Order Service drives W1; on each
  cart submission it asks Restaurant "are you open?" and Menu
  "are these items still available, what's the total?" before
  hitting Payment. The contract is locked in ADR 0030.
- **Failure semantics.** A closed restaurant returns `200` with
  `acceptsOrders=false` (not 409); unknown items return `200` with
  `allValid=false` and per-line errors. Order propagates these as
  client-visible 422s. Resilience4j circuit breaker on the Order
  side (Alfa-Kilo) trips after 3 consecutive 5xx (report §6.1).
- **Hand-off.** "Next slide shows the live demo of those two
  hops."

---

## S8 -- Synchronous integration: live demo cue

- **Visual.** Terminal screenshot of `services/local-dev/smoke.sh`
  green output (the "Sierra-Lima smoke test passed" line). Demo
  command call-out:

  ```
  bash services/local-dev/smoke.sh
  ```

- **Talk track (Alfa-Kilo + Sierra-Lima).** Three live beats from
  `dev-docs/presentation/phase-18-demo-script_Sierra-Lima.md` §2:
  1. Browser: Sign in -> create restaurant -> add menu items.
  2. Postman `W1 Integration` folder: 9 requests, 40 assertions,
     all green. Highlight `acceptsOrders=true` then `false` (Cafe
     Nero is closed by seed -- CP#1 talking-points §5).
  3. Smoke script: end-to-end in ~10 s.
- **Why this slide is short.** The demo *is* the content; the
  slide is just a navigational beacon for the room.
- **Hand-off.** "Async integration is next -- Mike-Alfa."

---

## S9 -- Asynchronous integration: topology

- **Visual.** Topic table from report §6.2. Highlight the producer
  / consumer columns.

  | Topic | Producer | Consumers |
  |---|---|---|
  | `order-events` | Order (A-K) | Notification (M-A), Delivery (E-Y) |
  | `payment-events` | Payment (E-Y) | Order (A-K), Notification (M-A) |
  | `delivery-events` | Delivery (E-Y) | Order (A-K), Notification (M-A) |
  | `notification-events` | Notification (M-A) | (terminal) |
  | `menu-events` | Menu (S-L) | (none in A3; **log-only** transport) |

- **Talk track (Mike-Alfa).** Two A3 async workflows:
  - **W2 -- Delivery progress.** Delivery emits
    `delivery.status-changed`; Order updates state; Notification
    emails the customer.
  - **W3 -- Payment outcome.** Payment emits `payment.completed`
    or `.failed`; Order moves the saga forward; Notification fires.
  - Envelope is locked in ADR 0032: `id` (UUID), `type`,
    `occurredAt` (ISO-8601 UTC), `payload`. At-least-once delivery;
    consumers dedupe by `envelope.id`.
- **Sierra-Lima's slot.** "Menu emits an availability-changed
  envelope on a logger called `menu-events` -- not on Kafka. The
  `MenuEventPublisher` interface is the swap seam; ADR 0040 §2
  documents the trade-off."
- **Hand-off.** "Next slide is the live W2/W3 trace."

---

## S10 -- Async integration: live trace

- **Visual.** Excerpt of `services/local-dev/evidence/cross-service-smoke_<RUN_TAG>.log`
  from the 2026-05-18 rehearsal run (one envelope per workflow).
  Sample envelope (report §15.5):

  ```
  topic=menu-events key=e0000010-... envelope={
    "id":"<uuid>", "type":"menu.item-availability-changed",
    "occurredAt":"2026-05-18T13:22:01Z",
    "payload":{"menuItemId":"e0000010-...",
               "restaurantId":"d0000099-...",
               "isAvailable":false,
               "previousIsAvailable":true} }
  ```

- **Talk track (Mike-Alfa + Sierra-Lima).** Live demo:
  1. Toggle a menu item's availability in the browser ->
     Sierra-Lima's `menu-events` line appears in `docker compose
     logs -f menu-service`.
  2. Place an order via the SPA; in a second tail Mike-Alfa's
     Notification consumer prints the matching `order-events`,
     `payment-events`, `delivery-events` envelopes.
  3. Run `bash services/local-dev/smoke-cross-service.sh` -- it
     captures the same trace into the evidence directory in
     ~15 s.
- **Hand-off.** "Security next -- back to Sierra-Lima."

---

## S11 -- Security: JWT issuance and validation

- **Visual.** Two-box diagram. Left: User Service mints HS256 JWT
  with claims (`sub`, `userId`, `role`, `iss=quickbite-user-service`).
  Right: every other service signature-verifies via `JwtAuthFilter`
  (report §7.1). Arrow labelled "Bearer relay -- never re-signed."
- **Talk track (Sierra-Lima).** Two enforcement layers:
  1. **Gateway** (Alfa-Kilo) -- shape-checks the bearer header,
     forwards verbatim with the path stripped of `/api`.
  2. **Service** (every Spring Boot service) -- `JwtAuthFilter
     extends OncePerRequestFilter`, registered before
     `UsernamePasswordAuthenticationFilter`. Missing / invalid /
     expired -> 401 via `RestAuthEntryPoints`. Successful parse
     installs a `ROLE_<role>` authority into the
     `SecurityContextHolder`.
  - Token contract is ADR 0010; relay is ADR 0033 §2.
- **Hand-off.** "Same speaker, next slide -- role + ownership."

---

## S12 -- Security: role gating + Phase 15 ownership

- **Visual.** Two stacked tables. Top: route matrix (subset of
  report §7.2). Bottom: 401 / 403 evidence cards from
  `dev-docs/verification/negative-auth-401.png`,
  `negative-auth-403.png`.
- **Talk track (Sierra-Lima).** Role gating via `@PreAuthorize`
  isn't enough by itself -- two `RestaurantOwner` accounts could
  step on each other. Phase 15 closes that gap:
  - **Restaurant Service** -- `requireOwnerOrAdmin(...)` checks
    `actor.userId == restaurant.ownerId`; `Admin` bypasses.
  - **Menu Service** -- doesn't store `ownerId`; calls
    `GET /restaurants/{id}` on Restaurant via
    `RestaurantOwnershipClient` (report §7.3) and applies the
    same predicate.
  - **Live moment.** Postman `Negative Auth` folder ->
    `[403] PUT /restaurants/{ownerB-rest} as ownerA` -> 403 +
    `ErrorResponse` envelope. Tail the service log; the
    `ownership denial actor=...` WARN line shows up immediately
    (report §7.3 + §15.4).
- **Hand-off.** "Frontend next -- same speaker."

---

## S13 -- Frontend walkthrough

- **Visual.** SPA storyboard, four panes:
  1. `LoginView` -- token mint + persist (`localStorage.quickbite.jwt`).
  2. `RestaurantListView` + filters (`?city=`, `?isOpen=`).
  3. `MenuView` + add/edit/toggle controls (visible only when
     `getRole() === 'RestaurantOwner' || 'Admin'`; report §8.1).
  4. `CartView` -> Order Service (W1 entry).
- **Talk track (Sierra-Lima).** Vue 3 + Vue Router 4 SPA, served
  by nginx; the same nginx reverse-proxies `/api/**` to the
  gateway (or to `dev-gateway` during solo rehearsal). Auth state
  is a `localStorage` key + a `router.beforeEach` guard. Owner
  controls are *cosmetically* hidden client-side; the server
  re-checks every mutation (defence in depth).
- **Live moment.** End-to-end flow from §2.1-§2.7 of CP#2
  talking-points -- sign in, browse, add, toggle, validate.
- **Hand-off.** "Resilience demo is the optional next slide --
  Mike-Alfa decides whether we run it."

---

## S14 -- Resilience demo (optional, time-permitting)

- **Visual.** Side-by-side: command + expected output. Two beats:
  1. `docker compose stop restaurant-service` -> Order Service
     trips its Resilience4j circuit breaker after 3 consecutive
     5xx. Show the response payload (`reason: RESTAURANT_TIMEOUT`).
  2. `docker compose start restaurant-service` -> after the
     half-open probe, Order recovers. Show a green W1 retry.
- **Talk track (Mike-Alfa).** "Resilience4j lives on Alfa-Kilo's
  Order Service -- Sierra-Lima's outbound call to Restaurant for
  the ownership lookup is intentionally uncached and uncircuited
  (report §13.3) because it sits on a *mutation* path, not the
  hot read path. We accept that trade-off and document it as a
  limitation."
- **If we're tight on time.** Skip live; use the screenshot
  fallback from `dev-docs/presentation/phase-18-fallbacks_Sierra-Lima.md`
  §3.4.
- **Hand-off.** "Sacrifices and future work next -- Alfa-Kilo."

---

## S15 -- Sacrifices and future work

- **Visual.** Two-column list.
- **Talk track (Alfa-Kilo).**
  - **Sacrificed.**
    - `Review Service` -- design-only (report §11.2; CP#1
      §1). Frees one ownership slot for hardening the other
      seven services.
    - Service discovery (Eureka / Consul) -- static compose
      networking instead (CP#1 §2 + ADR 0005).
    - CI runner -- local `mvn test` + `smoke-cross-service.sh`
      + manual browser walk; report §10.5.
    - Resilience4j on Sierra-Lima's outbound Menu -> Restaurant
      call -- accepted limitation (report §13.3).
    - Kafka client on Sierra-Lima's classpath -- log-only
      `menu-events` (ADR 0040 §2); KafkaMenuEventPublisher is a
      one-class drop-in for after CP#3.
  - **Future work** (report §14).
    - Kafka transport for `menu-events`.
    - `restaurant.deleted` event so Menu can soft-delete orphans.
    - Review Service implementation.
    - Short-TTL cache on the ownership lookup.
- **Hand-off.** "That's the talk. Q&A next."

---

## S16 -- Closing + Q&A

- **Visual.** Single-line title: *Q&A.* Underneath, three
  pre-rehearsed prompts the team is ready for (so silence in the
  room becomes a chance for us to volunteer):
  - "Why these seven services and not the eighth?"
  - "Walk us through how Menu validates a batch."
  - "How do you stop one owner editing another's menu?"
- **Talk track (rotating).** "We're happy to take questions in
  any order; full Q&A drill is in
  `dev-docs/presentation/phase-18-qa-prep_Sierra-Lima.md`."
- **Hand-off.** End of deck.

---

## Build notes

- **Render target.** Markdown -> PowerPoint via Marp / pandoc, or
  copy-paste into Google Slides. Either works; the demo script and
  fallbacks live alongside this file so the renderer choice does
  not gate rehearsal.
- **Screenshots.** Targets named in S6 (Swagger), S12 (negative
  auth), S14 (resilience). Capture all four during the 2026-05-18
  rehearsal; same folder as `dev-docs/verification/`. The deck
  references them by relative path; once the screenshots land the
  deck renders unchanged.
- **Numbers to refresh before CP#3 itself.** None. JUnit counts
  (23 / 42), Postman counts (9 / 40 + 14 / 17), service count (7 /
  8) are stable since Phase 16. If anything drifts before the
  graded run, edit the report first; this deck quotes from it.
