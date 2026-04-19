# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`b466d81`)

## Session Summary

This session executed **Phase 16 -- Async Evidence & Cross-Service Smoke**
for the QuickBite stack, as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
Phase 16 (lines 1525-1570).

The session began on top of `b466d81` ("Land Phase 15 authorisation
hardening and role-aware behaviour"). One mid-session context
compaction occurred after the cross-service smoke scripts were
created; work resumed from the compacted state and completed the
verification note, archive, and commit cleanly.

Phase 16 is the last A3 code beat before the report + demo pair
(Phases 17-18). Sierra-Lima is neither producer nor consumer in the
baseline A3 event topology (W2 = Delivery -> Order + Notification,
W3 = Payment -> Order + Notification, both owned by Elephant-Yankee
/ Mike-Alfa). The Phase 16 DoD therefore has two sides for
Sierra-Lima:

1. Keep Restaurant + Menu endpoints stable so the W1 -> W2 -> W3
   chain stays real during any integrated run.
2. Take the optional stretch producer
   (`menu.item-availability-changed`, envelope locked in decision
   0032 §6) if time permits.

Both were taken. The stretch was implemented with a **log-only
transport** (no `spring-kafka` dep, no broker connection) to keep
the session to one sitting and remove any runtime dependency on
Mike-Alfa's broker being up at CP#3. The `MenuEventPublisher`
interface is the swap seam for a future `KafkaMenuEventPublisher`;
envelope bytes are pre-locked so the swap does not change consumer
code.

All tests pass: Restaurant Service **23/23** (unchanged), Menu
Service **42/42** (up from 35 -- +6 service tests, +1 events-package
test). `bash -n` + `pwsh [Parser]::ParseFile` confirm the smoke
scripts are syntactically clean.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`,
  and the `Frontend` under `services/frontend/quickbite-frontend/`.
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Parent commit: `b466d81` -- "Land Phase 15 authorisation hardening
  and role-aware behaviour"
- Environment: Windows 11 + Git Bash + Maven + PowerShell 7.6

## User Requests

Initial request: *"Hi Claude, please work on Phase 16 of the master
plan `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.
After completing the implementation of Phase 16, please archive the
session context to `dev-docs/agent-context`, and then commit all
changes and push (try to commit and push the entire local repository;
exclude files only if there's a very good reason, according to your
best judgement). Thanks!"*

Model pinned to `opus` and effort to `max` early in the session via
`/model opus` + `/effort max`. No mid-session corrections or
redirections. One context compaction occurred between the smoke
script creation and the verification note; the archive here captures
both pre- and post-compaction work.

## Phase 16 Task-by-Task Record

### Tasks 1-3 -- Cross-team coordination (documented, no code)

The Phase 16 plan asks Sierra-Lima to confirm teammate readiness
for Kafka, W2 producers, and W3 consumers. Since the coordination
asks are communicative and not shape-changing on our classpath, they
were captured in the new ADR instead of code:

- `dev-docs/decisions/0040-phase-16-async-stance.md` §5 lists the
  exact asks for Mike-Alfa (broker + Notification), Elephant-Yankee
  (Payment + Delivery producers), and Alfa-Kilo (Order consumer).
- ADR 0040 §1 records the baseline posture: Restaurant + Menu are
  "stable non-participants" -- no Kafka client on classpath, no
  broker connection attempted on startup, W1 hops 4/5 answer HTTP
  regardless of broker state.

### Task 4 -- Document Sierra-Lima's async role

`dev-docs/decisions/0040-phase-16-async-stance.md` (new, 219 lines):

- Header: Status = Accepted, Base commit = `b466d81`, cross-refs to
  ADR 0032 (envelope lock) and ADR 0033 (token relay).
- §1 Baseline posture: no Kafka in Restaurant Service; Menu Service
  already stable; HTTP endpoints unchanged since Phase 10.
- §2 Optional stretch choice: log-only transport. Rationale covers
  timeline (one session), teammate decoupling (no broker needed for
  CP#3), and future-proofing (interface seam lets a Kafka-backed
  impl drop in without envelope changes).
- §3 Envelope shape: verbatim from 0032 §6, explicitly noting we
  always populate `previousIsAvailable` even though 0032 marks it
  optional.
- §4 Emit rule: one event per `PUT /menu-items/{id}` where
  `request.isAvailable()` differs from stored value. No emit on
  create, no emit on DELETE, no emit on non-availability updates.
  Publisher failure does not fail the DB write (aligns with 0032
  §7 at-least-once posture).
- §5 Coordination asks (Tasks 1-3 above).
- §6 Runtime behaviour: what gets emitted in the log-only shape vs.
  the future Kafka-backed shape.
- §7 Cross-service smoke scope.
- Consequences + Non-goals.

### Task 5 -- Stretch producer (taken, log-only transport)

**New -- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/events/`:**

Three new files forming the events package:

1. `AvailabilityChangedEvent.java` -- record of
   `(UUID menuItemId, UUID restaurantId, boolean isAvailable,
   boolean previousIsAvailable, Instant occurredAt)`. No methods,
   no validation -- it is a plain domain event.

2. `MenuEventPublisher.java` -- single-method interface
   (`void publishAvailabilityChanged(AvailabilityChangedEvent event)`).
   This is the Spring-bean seam; injecting the interface instead of
   the concrete `LoggingMenuEventPublisher` keeps the future Kafka
   swap to one new `@Component` class.

3. `LoggingMenuEventPublisher.java` -- `@Component` default
   implementation. Takes an `ObjectMapper` in its constructor (Spring
   auto-wires the one Spring Boot already configures). Builds a
   `LinkedHashMap<String,Object>` payload (preserves insertion order
   in the JSON so the envelope field order matches 0032 §6 exactly)
   with `menuItemId`, `restaurantId`, `isAvailable`,
   `previousIsAvailable`. Wraps it in another `LinkedHashMap`
   envelope with fresh `id = UUID.randomUUID()`,
   `type = "menu.item-availability-changed"`, `occurredAt =
   event.occurredAt().toString()` (ISO-8601 UTC because the Clock
   runs on `ZoneOffset.UTC`). Serialises via the `ObjectMapper` and
   logs at INFO on a dedicated logger named `menu-events`:

   ```
   INFO  menu-events : topic=menu-events key=<menuItemId> envelope={...}
   ```

   Jackson `JsonProcessingException` is caught and logged at WARN
   instead of propagating -- a serialisation fault must not fail
   the MenuService write.

**Edit -- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java`:**

Changes:

- New fields: `MenuEventPublisher menuEventPublisher` and
  `Clock clock` (the Clock mirrors the pattern already in
  RestaurantService).
- Two constructors:
  - Public `@Autowired` constructor receiving the publisher,
    delegating to the package-private constructor with
    `Clock.system(ZoneOffset.UTC)`.
  - Package-private constructor taking an explicit `Clock` for tests.
  The explicit `@Autowired` annotation was needed because Spring
  can no longer pick a single constructor ambiguously (RestaurantService
  has the exact same shape).
- `update(...)` now captures `boolean previousAvailability =
  m.isAvailable();` *before* `m.updateDetails(...)` and, if the flag
  changed, calls a new private `publishAvailabilityChanged(...)`
  method.
- `publishAvailabilityChanged(...)` builds an
  `AvailabilityChangedEvent` using `clock.instant()` for
  `occurredAt`, logs an INFO audit line (actor-free,
  transition-only), and invokes `menuEventPublisher.publish...`
  inside `try { ... } catch (RuntimeException ex) { log.warn(...) }`.
  This is the "emit failure must not fail the request" requirement
  (0032 §7 / 0040 §4).
- No changes to `create(...)` or `delete(...)` -- the emit rule
  (0040 §4) only covers transitions on existing items.

**New -- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/events/LoggingMenuEventPublisherTest.java`:**

- Uses Logback `ListAppender<ILoggingEvent>` attached to the
  `menu-events` logger in `@BeforeEach`, detached in `@AfterEach`.
- One test: `publishesEnvelopeMatchingDecision0032Shape`. Invokes
  the publisher with a fixed event, asserts exactly one log line at
  INFO, extracts the JSON after `envelope=`, parses it with Jackson,
  and checks the envelope matches 0032 §6 field-by-field. Validates
  that `id` is a valid UUID, `type` is `menu.item-availability-changed`,
  `occurredAt` matches the injected `Instant`, and payload fields
  round-trip correctly.

**Edit -- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/service/MenuServiceTest.java`:**

- New imports: `AvailabilityChangedEvent`, `MenuEventPublisher`,
  `ArgumentCaptor`, `Clock`, `Instant`, `ZoneOffset`,
  `verifyNoInteractions`, `verify`.
- New fields: `@Mock MenuEventPublisher menuEventPublisher` and
  `FIXED_NOW = Instant.parse("2026-04-19T12:00:00Z")`.
- `setUp()` now builds a fixed Clock and passes both the publisher
  and the clock into the package-private MenuService constructor.
- Existing `update_appliesDetailsAndReturnsResponse` tightened: its
  request fixture now keeps `isAvailable=true` unchanged, and the
  test adds `verifyNoInteractions(menuEventPublisher)` so the
  no-emit invariant is exercised in the common case.
- Six new tests:
  - `update_publishesAvailabilityChangedOnTrueToFalse` -- asserts
    `publishAvailabilityChanged` called once with captured event
    where `isAvailable=false, previousIsAvailable=true, occurredAt =
    FIXED_NOW`.
  - `update_publishesAvailabilityChangedOnFalseToTrue` -- reverse
    transition.
  - `update_doesNotPublishWhenOnlyNonAvailabilityFieldsChange` --
    only the name/price change; publisher unused.
  - `update_publisherFailureDoesNotFailRequest` -- `doThrow(new
    RuntimeException("broker unreachable"))` on the publisher;
    verifies the `update()` call still returns the updated
    `MenuItemResponse` and the item state is applied.
  - `create_doesNotPublishAvailabilityEvent` -- no emit on create.
  - `delete_doesNotPublishAvailabilityEvent` -- no emit on delete.

### Task 6 -- Cross-service smoke script

**New -- `services/local-dev/smoke-cross-service.sh` (executable, 362 lines):**

Bash script mirroring the style of the Phase 9 `smoke.sh`:

- Env vars are all optional; sensible defaults for
  `RESTAURANT_BASE=http://localhost:8081`,
  `MENU_BASE=http://localhost:8082`, and the HS256 JWT mint using the
  same dev secret from `services/local-dev/.env.example`. Teammate
  URLs (`USER_BASE`, `ORDER_BASE`, `PAYMENT_BASE`, `DELIVERY_BASE`,
  `NOTIFICATION_BASE`) default unset and produce `SKIP:` lines.
- Step 1 mints an owner JWT (`RestaurantOwner`) and a customer JWT
  (`Customer`) -- both valid for 1 hour, signed with the shared dev
  secret.
- Step 2 runs the W1 Sierra-Lima portion: POST a unique restaurant,
  POST a menu item on it, GET availability, POST /menu-items/validate.
- Step 3 exercises the Phase 16 emit-point: PUT availability=false,
  PUT availability=true. Then, if `docker ps` shows the
  `quickbite-menu-service` container, does `docker logs --since 2m
  | grep topic=menu-events` and asserts >=2 captured lines. Graceful
  SKIP when docker is absent or the container is stopped.
- Step 4 probes each teammate service's `/actuator/health` with a
  2-second timeout.
- Step 5 summarises; exits 0/1/2 per the taxonomy in ADR 0040 §7:
  0 = all OK (incl. SKIPs), 1 = Sierra fail, 2 = teammate URL set
  but unreachable.
- All output is teed to `services/local-dev/evidence/cross-service-smoke_<RUN_TAG>.log`
  where `RUN_TAG` = `YYYYMMDDTHHMMSSZ` UTC.
- `bash -n` syntax check: OK.

**New -- `services/local-dev/smoke-cross-service.ps1` (318 lines):**

PowerShell port for Windows dev environments. Mirrors the bash flow
1:1:

- `Invoke-WebRequest` instead of `curl` (with `-SkipHttpErrorCheck`
  semantics emulated via `try { } catch { $_.Exception.Response }`).
- JWT mint uses `[System.Convert]::ToBase64String` plus manual
  URL-safe replacements, and `System.Security.Cryptography.HMACSHA256`
  for the signature.
- Same trace format so traces from either OS can be compared.
- `pwsh [Parser]::ParseFile` syntax check: OK.

### Task 7 -- Verification + archive + commit

- `mvn test` Menu Service: 42 / 42 passing (up from 35 in Phase 15;
  +1 `LoggingMenuEventPublisherTest`, +6 `MenuServiceTest`).
- `mvn test` Restaurant Service: 23 / 23 passing (unchanged; Phase
  16 does not touch Restaurant Service).
- `bash -n` syntax check on `smoke-cross-service.sh`: OK.
- `pwsh [Parser]::ParseFile` on `smoke-cross-service.ps1`: OK.
- Phase 16 verification note created at
  `dev-docs/verification/phase-16-verification_Sierra-Lima.md`
  following the format of the Phase 14 / 15 notes -- sections for
  session context, task roll-up, test + build verification, DoD
  roll-up, and known follow-ups.
- This archive.
- Commit pending as the final step.

## Files Changed (planned landing)

New files:

- `dev-docs/decisions/0040-phase-16-async-stance.md`
- `dev-docs/verification/phase-16-verification_Sierra-Lima.md`
- `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_b466d81.md` (this file)
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/events/AvailabilityChangedEvent.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/events/MenuEventPublisher.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/events/LoggingMenuEventPublisher.java`
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/events/LoggingMenuEventPublisherTest.java`
- `services/local-dev/smoke-cross-service.sh`
- `services/local-dev/smoke-cross-service.ps1`

Modified files:

- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java`
  (Clock + publisher wiring, availability-transition emit).
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/service/MenuServiceTest.java`
  (Clock fixture + no-emit tightening + 6 new emit-path tests).

Nothing else touched; Restaurant Service source tree is untouched
by Phase 16.

## Mid-session Compaction Notes

One compaction fired between the smoke script creation and the
verification note. The resumed half:

- Read back the freshly-created `MenuService.java`,
  `LoggingMenuEventPublisherTest.java`, ADR 0040, and both smoke
  scripts to rebuild context.
- Re-ran both service test suites -- 42 + 23 as noted above.
- Wrote the verification note, this archive, and proceeded to
  commit + push.

No code output changed across the compaction; all work was
additive write and verification.

## Decisions and Rationale

**Why log-only transport for the stretch producer?**

1. Timeline: adding `spring-kafka` late-phase has a nontrivial
   blast radius (Spring Boot's KafkaAutoConfiguration, bootstrap
   healthchecks, test harness). Phase 16 is scoped as a single
   session.
2. Teammate decoupling: Mike-Alfa's broker may not be committed /
   reachable at CP#3 rehearsal. Log-only transport gives Sierra-Lima
   a demoable stretch that does not depend on anyone else's infra.
3. Future-proofing via the interface seam: `MenuEventPublisher` is
   the Spring bean; `LoggingMenuEventPublisher` is just the default
   `@Component`. Swapping to a `KafkaMenuEventPublisher` post-CP#3
   is a one-class addition; envelope bytes are already correct per
   0032 §6.

**Why `previousIsAvailable` always populated?**

0032 §6 leaves `previousIsAvailable` optional. We always populate it
because:

- We always have it at emit-time (`update()` captures it before
  `updateDetails()`).
- Consumers can reconstruct the transition direction from either
  field alone, but having both costs nothing and makes the log line
  self-describing.

**Why the Clock injection pattern?**

RestaurantService already does this for its `@CreatedDate` / audit
behaviour -- see `RestaurantService.java:39`. MenuService copying
the pattern keeps the testing story uniform: one `FIXED_NOW`
`Instant` in each test class, drilled into the service constructor
as a fixed `Clock`.

**Why a dedicated `menu-events` logger name?**

Grep-friendliness in compose logs. `docker logs quickbite-menu-service
| grep topic=menu-events` lifts the envelope lines cleanly; the
smoke script uses exactly that idiom. A per-event-topic logger also
lets ops configure a separate log destination (e.g. a file sink
with daily rolling) if we wanted to archive the events without
Kafka.

**Why exit code 2 for teammate-only failures?**

ADR 0040 §7 locks the taxonomy: Sierra failures must be loud (exit
1), but teammate failures must not mask our own health when we run
the smoke solo during rehearsal. Distinct exit codes let CI / Make
targets decide per-phase what counts as a fail.

## Open Follow-ups for Phases 17-18

- **Re-run the cross-service smoke once the full team stack is up.**
  Commit the resulting trace + `menu-events_<RUN_TAG>.log` under
  `services/local-dev/evidence/` as the canonical CP#3 evidence.
- **Swap `LoggingMenuEventPublisher` for a `KafkaMenuEventPublisher`**
  once Mike-Alfa's broker is reachable; add one `@Component` class,
  inject `KafkaTemplate`, unchanged call site. ADR 0040 §6 describes
  this.
- **Phase 17 report §7 (Integration mechanisms)** should cite ADR
  0040 §3 (envelope) plus a short log excerpt from a real smoke
  trace. The topic table will list `menu-events` alongside
  `payment-events` / `delivery-events`.
- **Phase 18 demo script** should include a "toggle menu-item
  availability" beat -- it is the only Sierra-Lima-originated
  event in the rubric, and the log line is demonstrable live.
