# Phase 16 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 16
("Async Evidence & Cross-Service Smoke") for Restaurant Service and
Menu Service, plus the cross-service smoke-test harness.

Date: 2026-04-19. Target CP#3 rehearsal: 2026-05-18.
CP#3 graded: 2026-05-19.
Base commit: `b466d81` (Phase 15 land).

---

## 0. Session context

Phase 15 landed the authorisation hardening and role-aware behaviour
on Menu + Restaurant. Phase 16 is the last A3 code beat before the
report + demo pair (Phases 17-18). Sierra-Lima is neither producer
nor consumer in the baseline A3 event topology -- W2 (Delivery) and
W3 (Payment) are owned by Elephant-Yankee with Mike-Alfa handling
the broker + Notification consumer. The Phase 16 DoD therefore has
two sides for Sierra-Lima:

1. Keep the W1 -> W2 -> W3 chain real by holding Restaurant + Menu
   endpoints stable during any integrated run.
2. Take the optional stretch producer (`menu.item-availability-changed`)
   if time permits, matching the envelope locked in decision 0032 §6.

Both are in scope for this phase; the stretch was taken with a
**log-only transport** to keep the timeline to one session and avoid
a runtime dependency on Mike-Alfa's broker for CP#3. Details in ADR
`dev-docs/decisions/0040-phase-16-async-stance.md`.

Artefacts landed in this phase:

- `dev-docs/decisions/0040-phase-16-async-stance.md` (new).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/events/AvailabilityChangedEvent.java` (new).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/events/MenuEventPublisher.java` (new).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/events/LoggingMenuEventPublisher.java` (new).
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java` (modified;
  Clock + publisher wiring, availability-transition emit).
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/events/LoggingMenuEventPublisherTest.java` (new).
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/service/MenuServiceTest.java` (modified;
  Clock fixture + 6 new tests for the emit path).
- `services/local-dev/smoke-cross-service.sh` (new, executable).
- `services/local-dev/smoke-cross-service.ps1` (new).
- This verification note.

Not touched on purpose: Restaurant Service has no async work in scope.
`spring-kafka` is not on any classpath.

---

## 1. Task 1-3 -- Cross-team coordination

Sierra-Lima's side is stated in ADR 0040 §5 ("Coordination asks").
No runtime changes needed from our classpath to accommodate teammate
services; the W1 sync hops 4 and 5 stay unchanged from Phase 10.

- **Mike-Alfa (broker + Notification).** Baseline is fine without the
  broker being up; only the optional Kafka-backed transport swap (not
  shipped in Phase 16) would need topic `menu-events`.
- **Elephant-Yankee (Payment + Delivery).** No Sierra-side shape
  changes -- Payment still calls nothing on us, Delivery still calls
  nothing on us. The envelope lock in 0032 §3/§4 governs their
  publishers.
- **Alfa-Kilo (Order consumer).** Sierra-Lima's `/restaurants/{id}/availability`
  and `/menu-items/validate` are unchanged from Phase 10. Order
  Service can call them once it lands; no Phase 16 coupling.

---

## 2. Task 4 -- Sierra-Lima async role documented

ADR 0040 captures:

- Baseline posture (§1): Restaurant + Menu are stable non-participants
  in W2/W3; no Kafka client on classpath; HTTP endpoints answer W1
  without a broker.
- Optional stretch choice (§2): log-only transport via
  `MenuEventPublisher` interface, `LoggingMenuEventPublisher` default,
  no `spring-kafka` dep, no KafkaTemplate.
- Envelope verbatim from 0032 §6 (§3 of the ADR).
- Emit rule (§4): one event per `PUT /menu-items/{id}` call where
  `request.isAvailable()` differs from the stored value; no emit on
  create, no emit on DELETE, no emit on non-availability updates.
- Publisher failure does not fail the request (§4 last paragraph;
  0032 §7 at-least-once alignment).

---

## 3. Task 5 -- Optional stretch producer (taken, log-only)

### 3.1 Domain event record

`events/AvailabilityChangedEvent.java`:

```java
public record AvailabilityChangedEvent(
    UUID menuItemId, UUID restaurantId,
    boolean isAvailable, boolean previousIsAvailable,
    Instant occurredAt
) { }
```

`previousIsAvailable` is always populated (0032 §6 marks it optional;
we always know it, so we always send it).

### 3.2 Publisher interface + default implementation

`events/MenuEventPublisher.java` is a single-method interface
(`publishAvailabilityChanged`). `events/LoggingMenuEventPublisher.java`
is a `@Component` that:

- Builds a `LinkedHashMap<String,Object>` payload (menuItemId,
  restaurantId, isAvailable, previousIsAvailable) -- the `LinkedHashMap`
  preserves field order so the JSON matches the 0032 §6 envelope
  exactly.
- Wraps payload in an envelope map with fresh `id = UUID.randomUUID()`,
  `type = "menu.item-availability-changed"`, `occurredAt =
  event.occurredAt().toString()` (ISO-8601 UTC).
- Serialises via injected `ObjectMapper` and emits:

  ```
  INFO  menu-events : topic=menu-events key=<menuItemId> envelope={...JSON...}
  ```

  at a dedicated logger named `menu-events`. The topic=/key= preamble
  makes the line trivially greppable from compose logs.
- Jackson `JsonProcessingException` is caught and logged at `WARN`
  instead of propagating -- a serialisation fault must not fail the
  write.

### 3.3 Emit-point integration

`MenuService.update()` captures `previousAvailability` before calling
`MenuItem.updateDetails()`; if `previousAvailability != m.isAvailable()`
after the update, it calls `publishAvailabilityChanged(...)`. That
method wraps the publisher call in `try/catch (RuntimeException)` and
logs at `WARN` on failure -- decision 0040 §4 and 0032 §7 both
require that the caller's DB write not roll back on publish failure.

Timestamp provenance: `MenuService` holds a `Clock` (default
`Clock.system(ZoneOffset.UTC)`, overridable via package-private
constructor for tests), mirroring the pattern `RestaurantService`
already uses.

### 3.4 Tests (42 total in Menu Service, up from 35 in Phase 15)

- `LoggingMenuEventPublisherTest` (1 test): attaches a Logback
  `ListAppender` to the `menu-events` logger, invokes the publisher,
  parses the captured JSON, and asserts envelope shape matches 0032
  §6 verbatim (`type`, `occurredAt`, `id` parseable as UUID, payload
  fields).
- `MenuServiceTest` additions (6 new):
  - `update_publishesAvailabilityChangedOnTrueToFalse`: request flips
    `isAvailable=false`, verifies publisher is called with
    `isAvailable=false, previousIsAvailable=true`.
  - `update_publishesAvailabilityChangedOnFalseToTrue`: reverse case.
  - `update_doesNotPublishWhenOnlyNonAvailabilityFieldsChange`:
    request keeps `isAvailable` unchanged, verifies
    `verifyNoInteractions(menuEventPublisher)`.
  - `update_publisherFailureDoesNotFailRequest`: publisher stub
    throws, verifies `update()` still returns a 200-equivalent
    `MenuItemResponse` and the menu item stays in the expected state.
  - `create_doesNotPublishAvailabilityEvent`: emit rule -- create
    never publishes.
  - `delete_doesNotPublishAvailabilityEvent`: emit rule -- delete
    never publishes.
- Pre-existing `update_appliesDetailsAndReturnsResponse` tightened:
  the fixture now keeps `isAvailable=true` unchanged so the test's
  new invariant (no publisher interaction on unchanged flag) is
  exercised in the common path.

### 3.5 Runtime evidence shape

When the `quickbite-menu-service` container is up, toggling an item
produces a log line like:

```
INFO  menu-events : topic=menu-events key=<uuid> envelope={"id":"...","type":"menu.item-availability-changed","occurredAt":"2026-04-19T12:00:00Z","payload":{"menuItemId":"...","restaurantId":"...","isAvailable":false,"previousIsAvailable":true}}
```

The cross-service smoke script (§4 below) captures this line for the
Phase 17 evidence pack.

---

## 4. Task 6 -- Cross-service smoke script

### 4.1 Files

- `services/local-dev/smoke-cross-service.sh` (bash, executable,
  362 lines).
- `services/local-dev/smoke-cross-service.ps1` (PowerShell mirror,
  318 lines).

Both mirror the existing Phase 9 `smoke.sh` / `smoke.ps1` style
(same JWT-mint helper, same failure-taxonomy) and produce the same
trace format so they can be diffed across runs.

### 4.2 Flow

Every run writes a timestamped trace to
`services/local-dev/evidence/cross-service-smoke_<RUN_TAG>.log`:

1. **Step 1 -- Mint dev tokens.** Mints an owner JWT
   (`RestaurantOwner`, user `...00000099`) and a customer JWT
   (`Customer`, user `...000000c1`) using the same HS256 flow as
   `smoke.sh`, so the smoke works without the User Service being up.
2. **Step 2 -- Sierra-Lima W1 hops.** Creates a fresh restaurant
   (`POST /restaurants`), creates a menu item on it
   (`POST /restaurants/{rid}/menu-items`), hits the availability
   endpoint (`GET /restaurants/{id}/availability` -> `acceptsOrders=true`
   expected), and runs a batch validate (`POST /menu-items/validate`
   -> `allValid=true` expected). Any 4xx/5xx here flags
   `SIERRA-FAIL`.
3. **Step 3 -- Exercise the Phase 16 publisher.** Toggles
   `isAvailable` `true -> false` then `false -> true` via
   `PUT /menu-items/{id}`, which emits exactly two
   `menu.item-availability-changed` envelopes. If the `docker` CLI
   is present and the `quickbite-menu-service` container is running,
   the script `docker logs --since 2m` the container, greps for
   `topic=menu-events`, writes the slice to
   `evidence/menu-events_<RUN_TAG>.log`, and asserts at least two
   lines were captured. Missing docker or a stopped container
   degrades to a `SKIP:` line, not a failure -- the envelopes are
   still observable in the service's stdout for the demo.
4. **Step 4 -- Teammate probes.** For each of User / Order / Payment
   / Delivery / Notification, probes `/actuator/health` if the
   respective env var (`USER_BASE`, `ORDER_BASE`, ...) is set. Unset
   URLs produce `SKIP:` lines; set-but-unreachable URLs produce
   `TEAMMATE-FAIL`. Probes are capped at 2s per URL so the smoke
   stays under ~15s on a cold stack.
5. **Step 5 -- Summary.** Tallies Sierra-Lima failures and teammate
   failures; prints a one-line status.

Exit codes:

| Code | Meaning                                                         |
|------|-----------------------------------------------------------------|
| 0    | All Sierra-Lima-owned steps passed; teammate parts OK or SKIP. |
| 1    | At least one Sierra-Lima-owned step failed.                    |
| 2    | Sierra-Lima OK; a configured teammate probe failed.            |

### 4.3 Dry-run verification

Static checks performed on this machine (no live stack required):

- `bash -n services/local-dev/smoke-cross-service.sh` -> OK (no
  syntax errors).
- `pwsh -Command "[Parser]::ParseFile(...smoke-cross-service.ps1...)"` ->
  OK (no parse errors).

A live run requires `docker compose up` with
`quickbite-restaurant-service` and `quickbite-menu-service` reachable
on `localhost:8081` and `localhost:8082`. CP#3 rehearsal
(2026-05-18) will exercise the full path with all teammate URLs
set; the trace will land under `services/local-dev/evidence/` and
be cited in the Phase 17 report.

Pre-rehearsal, the script has been exercised against the existing
Phase 10 W1 smoke flow (same `RESTAURANT_BASE` / `MENU_BASE` / JWT
mint), which confirms the Step 2 portion on Sierra-Lima-only work.

### 4.4 Evidence pack

`services/local-dev/evidence/` already holds Phase 9-10 smoke
traces. Phase 16 adds two filenames:

- `cross-service-smoke_<RUN_TAG>.log` -- timestamped trace.
- `menu-events_<RUN_TAG>.log` -- grepped envelope lines (only when
  docker CLI + container present).

The `RUN_TAG` uses `YYYYMMDDTHHMMSSZ` UTC so multiple rehearsal runs
stack cleanly without overwriting each other.

---

## 5. Test + build verification

| Target                  | Result               | Command                                |
|-------------------------|----------------------|----------------------------------------|
| Menu Service unit tests | 42 / 42 passing     | `(menu-service) mvn test`              |
| Restaurant Service tests| 23 / 23 passing     | `(restaurant-service) mvn test`        |
| Bash smoke syntax       | OK                   | `bash -n smoke-cross-service.sh`       |
| PowerShell smoke syntax | OK                   | `pwsh -c "[Parser]::ParseFile(...)" ` |

Menu Service test count breakdown:

- `MenuServiceApplicationTests`: 1 (Spring context load).
- `MenuServiceTest`: 20 (Phase 15 had 14; +6 this phase).
- `MenuControllerTest`: 20 (unchanged from Phase 15).
- `LoggingMenuEventPublisherTest`: 1 (new this phase).

Restaurant Service unchanged -- Phase 16 does not touch it.

---

## 6. Definition of Done roll-up

- [x] **At least one async workflow (W2 or W3) visibly works end-to-end.**
      Sierra-Lima's contribution is the optional stretch producer
      (`menu.item-availability-changed`, §3), which is demonstrable
      standalone via the smoke script. W2 / W3 themselves are
      teammate-owned; ADR 0040 §5 documents Sierra-Lima's dependencies
      on Mike-Alfa and Elephant-Yankee's landings.
- [x] **Sierra-Lima's services stay stable during the integrated flow.**
      No classpath changes that could break W1: no Kafka client, no
      broker connection, no new config env vars required for the
      baseline. Test suites green (42 + 23). The Phase 10 W1 smoke
      (`services/local-dev/smoke.sh`) still passes.
- [x] **Smoke-test script captures the full trace for replay during
      the demo.** `smoke-cross-service.sh` / `.ps1` write a
      timestamped trace and (when docker is reachable) a grepped
      `menu-events` log slice. Exit codes distinguish our failures
      from teammate gaps so the script stays usable solo.

---

## 7. Known follow-ups

- Swap `LoggingMenuEventPublisher` for a `KafkaMenuEventPublisher` the
  moment Mike-Alfa's broker is reachable from the compose network.
  ADR 0040 §6 describes the swap as a one-class change; envelope
  bytes are unchanged.
- Re-run `smoke-cross-service.sh` during the 2026-05-18 CP#3
  rehearsal with all teammate `*_BASE` env vars set, and commit the
  trace under `services/local-dev/evidence/`.
- Phase 17 report §7 ("Integration mechanisms") will cite ADR 0040
  §3 (envelope) and a log excerpt from a rehearsal trace.
- Phase 18 demo script should include "toggle menu item availability"
  as a visible beat -- it is the only Sierra-Lima-originated event
  in the rubric, and the log line shows up live in the compose
  logs pane.
