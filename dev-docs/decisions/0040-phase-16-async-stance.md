# 0040 -- Phase 16 Async Stance for Sierra-Lima

- **Status:** Accepted
- **Date:** 2026-04-19
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `b466d81`
- **Source:**
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
  §9 Phase 16 (lines 1525-1570), §6 Named Workflows, Appendix F.7.
- **Supersedes:** none. Complements decisions
  [0032](0032-w2-w3-event-contract-lock.md) (event-contract lock) and
  [0033](0033-inter-service-token-propagation-lock.md) (token relay).

## Context

Phase 16 asks for at least one demonstrably-real asynchronous workflow
by Checkpoint #3. In the A3 baseline topology:

- **W2 (delivery progress)** producer = Delivery Service (Elephant-Yankee),
  consumer = Order Service (Alfa-Kilo) and Notification Service (Mike-Alfa).
- **W3 (payment outcome)** producer = Payment Service (Elephant-Yankee),
  consumer = Notification Service (always) + Order Service (on
  `payment.failed`).

Sierra-Lima owns **Restaurant Service** and **Menu Service**, neither
of which is a producer or consumer in that baseline. The plan calls
this out in §6 and in Phase 16 Task 4: "Restaurant and Menu services
do not produce or consume events in the baseline A3 scope. For CP#3,
the async demo is driven by teammate-owned services; Sierra-Lima keeps
Restaurant and Menu endpoints stable so the W1 -> W2 -> W3 chain stays
real."

Phase 16 also leaves an optional stretch for a Menu-side event --
`menu.item-availability-changed` -- whose envelope and emit-point were
pre-locked in decision [0032](0032-w2-w3-event-contract-lock.md) §6.

## Decision

### 1. Baseline posture (required by CP#3)

- Restaurant Service and Menu Service stay **stable non-participants**
  in W2 / W3 during the integrated demo:
  - No Kafka client on the Restaurant Service classpath.
  - No broker connection attempted on startup.
  - HTTP endpoints continue to answer W1 hops 4 (`GET /restaurants/{id}/availability`)
    and 5 (`POST /menu-items/validate`) without Kafka being up.
- Sierra-Lima's Phase 9-15 code already honours this (no Kafka
  dependencies committed). No further action needed on the baseline
  side for CP#3.

### 2. Optional stretch (taken, log-only transport)

We take the Phase 16 stretch (§6 of 0032) **with a log-only transport**
rather than a real Kafka producer:

- `MenuService.update()` detects when `isAvailable` transitions
  (old != new) on a `PUT /menu-items/{id}` and emits one
  `menu.item-availability-changed` envelope per transition.
- The emit goes through a `MenuEventPublisher` interface with a
  default `LoggingMenuEventPublisher` implementation that writes the
  decision-0032 §6 envelope at INFO to a dedicated logger
  (`menu-events`). The log line is structured JSON -- easy to grep,
  easy to lift into the Phase 17 evidence pack.
- No `spring-kafka` dependency added. No KafkaTemplate wired. No
  broker configuration needed for the demo.

Rationale: the stretch's value in the A3 rubric is proving that the
emit-point exists and the envelope matches the team-locked contract.
Transport (Kafka vs log) is an infrastructure choice that can swap
without reshaping the event. Going log-only:

- Keeps the Phase 16 timeline to one session (the Maven/test blast
  radius of adding `spring-kafka` is nontrivial this late).
- Removes a runtime dependency on Mike-Alfa's broker being up during
  CP#3 rehearsal -- the log evidence works standalone.
- Preserves the contract lock: when Mike-Alfa's broker is available,
  swapping in a `KafkaMenuEventPublisher` is a one-class change
  that reads the same `AvailabilityChangedEvent` record and emits the
  same envelope bytes. The interface is the seam.

### 3. Events produced: envelope shape (0032 §6 verbatim)

```json
{
  "id": "<UUID v4>",
  "type": "menu.item-availability-changed",
  "occurredAt": "<ISO-8601 UTC>",
  "payload": {
    "menuItemId": "<UUID>",
    "restaurantId": "<UUID>",
    "isAvailable": true,
    "previousIsAvailable": false
  }
}
```

- `id`: fresh UUID per transition. Doubles as the idempotency key
  consumers would dedupe on if the transport were Kafka.
- `occurredAt`: `Clock.instant()` (the shared `Clock` bean already
  used across MenuService for audit timestamps).
- `payload.previousIsAvailable`: required; callers reading only the
  new value can still reconstruct the transition direction. Decision
  0032 §6 marks it optional; we always populate it because we always
  know it.

### 4. Emit rule

Exactly one event is emitted per `PUT /menu-items/{id}` call in which
`request.isAvailable()` differs from the current
`MenuItem.isAvailable`. No event is emitted on:

- `POST /restaurants/{rid}/menu-items` (create) -- the transition from
  "nonexistent" to "available" is not a state flip on an existing
  item.
- `PUT` that changes name/description/price/category but keeps
  `isAvailable` identical.
- `DELETE` -- removal is not an availability transition; the caller
  can infer unavailability from a 404 or from the restaurant's menu
  listing.

The emit is best-effort: a publisher failure is caught and logged
at `WARN` but does not roll back the DB write. This mirrors 0032 §7
("at-least-once, consumers dedupe") and the master plan's general
"failure to produce must not fail the request" posture.

### 5. Coordination asks (Phase 16 Tasks 1-3)

Sierra-Lima's side of the team coordination:

- **Mike-Alfa (broker + Notification).** Once Kafka is up, confirm
  topics `payment-events` and `delivery-events` exist, and that
  Notification Service is running as a consumer on both. If the
  stretch producer (§2) is to be wired to a real broker for the
  final demo, Mike-Alfa also needs topic `menu-events` and the
  Sierra-Lima side needs a broker-URL env var -- not required for
  CP#3 baseline.
- **Elephant-Yankee (Payment + Delivery).** Confirm Payment Service
  produces `payment.completed` / `payment.failed` on `payment-events`
  with the envelope from 0032 §3, and Delivery Service produces
  `delivery.status-changed` on `delivery-events` with the envelope
  from 0032 §4.
- **Alfa-Kilo (Order consumer).** Confirm Order Service consumes
  `delivery.status-changed` and updates order state accordingly;
  confirm W1 still calls Sierra-Lima's `/restaurants/{id}/availability`
  and `/menu-items/validate` unchanged.

Sierra-Lima does **not** block on any of the above for CP#3 baseline.
The cross-service smoke script (§7 below) checks for the presence of
those services and gracefully skips segments that require teammate
infra, so the script runs standalone and escalates its trace when the
team stack is fully up.

### 6. What is emitted by Sierra-Lima at runtime

In the default deployment (log-only transport):

- A JSON envelope on logger `menu-events` at INFO, one line per
  availability transition.
- A preceding INFO audit line from `MenuService` noting the
  transition, caller, and menu-item id.
- No bytes leave the process on the wire.

In a future Kafka-backed deployment (not shipped in Phase 16, trivial
swap post-CP#3):

- The same JSON on Kafka topic `menu-events`, record key =
  `menuItemId.toString()`, at-least-once, 2 retries then log-and-move-on
  per 0032 §7.

### 7. Cross-service smoke script

Scope for Phase 16 Task 6: a bash + PowerShell smoke script that
exercises the full chain in whatever degree the teammate services
are available, and records a trace. See
`services/local-dev/smoke-cross-service.sh` and `.ps1`. The script:

1. Verifies Restaurant + Menu are up.
2. Runs the W1 portion on Sierra-Lima services (login, create restaurant,
   add menu item, toggle status, batch validate).
3. Triggers a menu-availability transition (ON -> OFF -> ON) on the
   freshly-created item to exercise the §2 publisher; greps the
   `menu-events` log lines out of the Menu Service container.
4. Optionally probes the teammate services (User / Order / Payment /
   Delivery / Notification) when their URLs are provided via env vars;
   skips with a clearly-labelled "SKIP: upstream not configured" when
   they are absent.
5. Writes a timestamped trace file under `services/local-dev/evidence/`
   for the Phase 17 evidence pack.

The script exits **non-zero only on failures of Sierra-Lima-owned steps**.
Missing teammate services produce SKIP lines, not failures -- the
script must stay usable solo during rehearsal.

## Consequences

- Phase 16 adds no runtime infra dependencies. `mvn test` and
  `docker compose up` behave identically.
- The new `MenuEventPublisher` interface is the injection seam for
  a future Kafka-backed publisher. Decision 0032 §6 already locked
  the envelope, so swapping transports requires exactly one new class.
- Phase 17 report references §3 (envelope) and §6 (emit evidence) of
  this decision plus a log excerpt showing the JSON line. W2 / W3
  evidence in the report is sourced from teammate services.
- Phase 18 demo script: if Mike-Alfa's broker is up by then and we
  wire the Kafka publisher, this decision is superseded piecewise
  (§2 moves from "log-only" to "log + Kafka"; all other clauses
  unchanged). If the broker is not up by then, the log-only posture
  is the CP#3 final.

## Non-goals

- **Consuming any team topics.** Sierra-Lima's services remain
  pure producers-or-silent.
- **Broker-side reliability.** Partition count, replication factor,
  retention, DLQ wiring remain Mike-Alfa's decisions per 0032 §1/§8.
- **Exactly-once semantics.** At-least-once per 0032 §7; the log-only
  transport is still "exactly once per transition detected" since
  there is no delivery retry layer.
