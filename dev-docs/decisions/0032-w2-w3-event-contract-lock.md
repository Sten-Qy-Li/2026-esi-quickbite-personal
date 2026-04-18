# 0032 -- W2 / W3 Event Contract Lock

- **Status:** Accepted
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `5bd6f45`
- **Source:**
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §9
  Phase 9 Tasks 3-5, §6, and Appendix F.7.

## Context

W2 (delivery progress) and W3 (payment outcome) are the Assignment 3
asynchronous integrations. Neither Restaurant nor Menu is a producer or
consumer in the A3 baseline scope; Phase 16 carries an optional stretch
where Menu Service publishes `menu.item-availability-changed`, but
that is a later decision.

Phase 9 Tasks 3-5 still require the team to agree topic names, payload
shapes, envelope conventions, DLQ naming, and idempotency expectations
**now**, so that: (a) Sierra-Lima's services stay stable when other
callsigns bring up Kafka in Phase 16; (b) Order, Payment, Delivery, and
Notification can be coded against written contracts in the team repo
before infra is live; (c) the Phase 16 stretch producer, if taken,
slots into a pre-agreed topic rather than inventing one late.

The plan's Team-dependency note allows Sierra-Lima to lock the contract
unilaterally; this document invokes that clause. Later decisions from
Elephant-Yankee (producer of payment / delivery events) and Mike-Alfa
(broker owner) supersede the advisory rows.

## Decision

### 1. Topic list (frozen)

| Topic | Producer | Consumer(s) | Workflow | Required? |
|-------|----------|-------------|----------|-----------|
| `payment-events` | Payment Service (Elephant-Yankee) | Notification Service (Mike-Alfa); Order Service on `payment.failed` | W3 | **Required** |
| `delivery-events` | Delivery Service (Elephant-Yankee) | Order Service (Alfa-Kilo); Notification Service | W2 | **Required** |
| `order-events` | Order Service (Alfa-Kilo) | Payment Service (runs refund); Notification Service | W1 compensating | **Required** (needed for Appendix F.6 step 8 refund) |
| `menu-events` | Menu Service (Sierra-Lima) | Notification Service | optional Phase 16 stretch | **Optional** (A3 out-of-scope) |

Each topic has exactly one producer service. Consumers join a stable
consumer group per service (`<service-name>-consumer`, e.g.
`notification-service-consumer`, `order-service-consumer`).

Partition count, replication factor, and retention are Mike-Alfa's
broker-configuration decisions; not pinned here. For the CP#1 demo any
single-broker Kafka default (1 partition, retention 7 days) is
acceptable.

### 2. Event envelope (frozen)

Every message on every topic carries this envelope, regardless of
`type`. Payloads are nested under `payload`.

```json
{
  "id": "3e28a4c0-0000-0000-0000-00000000abcd",
  "type": "payment.completed",
  "occurredAt": "2026-05-05T12:34:58.123Z",
  "payload": { }
}
```

Field contract:

| Field | Type | Meaning |
|-------|------|---------|
| `id` | UUID | Globally-unique event id. **Also the idempotency key** consumers dedupe on. Producers MUST regenerate this only when the underlying domain event is itself new. |
| `type` | String | Dotted `domain.past-tense` form (e.g. `payment.completed`, `delivery.status-changed`). Consumers route by `type` exact match. |
| `occurredAt` | ISO-8601 UTC Instant | Wall-clock time the domain event happened. Producers SHOULD NOT use the publish time if it differs. |
| `payload` | Object | Per-topic, per-type body. Shapes below. |

No other top-level fields. Breaking addition requires a new decision.

Messages are JSON-serialised (`application/json`) with UTF-8, carried
as the Kafka record value. Record keys are **optional**; when used,
producers set the record key to the natural aggregate id (e.g. the
`orderId` or `deliveryId`) so that partitioning groups related events.
Record headers are not relied on by consumers.

### 3. `payment-events` payloads (W3)

Producer: Payment Service. Consumers: Notification Service (always),
Order Service (only for `payment.failed`).

**Event `payment.completed`:**

```json
{
  "id": "3e28a4c0-...",
  "type": "payment.completed",
  "occurredAt": "2026-05-05T12:34:58.123Z",
  "payload": {
    "paymentId": "a0000000-0000-0000-0000-000000000001",
    "orderId": "f0000000-0000-0000-0000-000000000001",
    "amount": "25.00",
    "currency": "EUR"
  }
}
```

**Event `payment.failed`:**

```json
{
  "id": "3e28a4c0-...",
  "type": "payment.failed",
  "occurredAt": "2026-05-05T12:34:58.123Z",
  "payload": {
    "paymentId": "a0000000-0000-0000-0000-000000000001",
    "orderId": "f0000000-0000-0000-0000-000000000001",
    "reason": "PAYMENT_DECLINED"
  }
}
```

`payload.reason` enum: `PAYMENT_DECLINED` | `PAYMENT_CARD_EXPIRED` |
`PAYMENT_INSUFFICIENT_FUNDS` | `PAYMENT_UPSTREAM_ERROR`. Unknown reasons
are treated by consumers as `PAYMENT_DECLINED` for user messaging.

### 4. `delivery-events` payloads (W2)

Producer: Delivery Service. Consumers: Order Service, Notification
Service.

**Event `delivery.status-changed`:**

```json
{
  "id": "3e28a4c0-...",
  "type": "delivery.status-changed",
  "occurredAt": "2026-05-05T12:45:10.000Z",
  "payload": {
    "deliveryId": "b0000000-0000-0000-0000-000000000001",
    "orderId": "f0000000-0000-0000-0000-000000000001",
    "status": "PickedUp",
    "previousStatus": "Assigned"
  }
}
```

`payload.status` enum matches `0030` §6: `Pending` | `Assigned` |
`PickedUp` | `Delivered` | `Failed`. `previousStatus` is optional (may
be omitted for the very first event on a delivery) but when present
MUST be the value the producer observed before this transition -- so
consumers can ignore out-of-order events by comparing.

Producers emit one event per state transition. Duplicate emissions for
the same transition (same `previousStatus -> status`) are allowed and
expected (at-least-once delivery); consumers dedupe via envelope `id`.

### 5. `order-events` payloads (compensating, W1 tail)

Producer: Order Service. Consumers: Payment Service (refund), Notification
Service.

**Event `order.cancelled`:**

```json
{
  "id": "3e28a4c0-...",
  "type": "order.cancelled",
  "occurredAt": "2026-05-05T12:34:59.500Z",
  "payload": {
    "orderId": "f0000000-0000-0000-0000-000000000001",
    "reason": "DELIVERY_UNAVAILABLE",
    "paymentId": "a0000000-0000-0000-0000-000000000001"
  }
}
```

`payload.reason` enum: `CUSTOMER_CANCELLED` | `DELIVERY_UNAVAILABLE` |
`RESTAURANT_REJECTED` | `PAYMENT_FAILED_LATE` | `SYSTEM_TIMEOUT`.
`payload.paymentId` is included only when a charge exists and must be
refunded; Payment Service's consumer treats its absence as "nothing to
refund".

### 6. `menu-events` payloads (optional, Phase 16 stretch)

Producer (if built): Menu Service. Consumer: Notification Service.

**Event `menu.item-availability-changed`:**

```json
{
  "id": "3e28a4c0-...",
  "type": "menu.item-availability-changed",
  "occurredAt": "2026-05-05T13:00:00.000Z",
  "payload": {
    "menuItemId": "e0000011-0000-0000-0000-000000000000",
    "restaurantId": "d0000001-0000-0000-0000-000000000000",
    "isAvailable": false,
    "previousIsAvailable": true
  }
}
```

Emitted when `MenuItem.isAvailable` toggles on a `PUT /menu-items/{id}`
that changes the flag (not on unrelated edits). Notification Service
uses this to push "your favourite dish is back" or similar to customers
who previously ordered the item. Not required for CP#1, CP#2, or CP#3
unless the team takes the Phase 16 stretch.

### 7. Delivery guarantees and idempotency

- **At-least-once** delivery. No exactly-once semantics. Consumers
  MUST be idempotent on envelope `id`.
- Producers MUST set `id` once per domain event and re-use it on
  retries (do not regenerate the UUID on resend). A domain event here
  means one state transition on the producing aggregate.
- Consumers track seen `id`s for at least 24 hours (in-memory LRU is
  fine for the CP#1 demo; persistent dedupe table is the Phase 16
  upgrade). A seen id MUST be acknowledged without re-applying the
  side-effect.
- `occurredAt` is informational only. Consumers MUST NOT use it as the
  basis for ordering guarantees beyond "newest wins on the same
  aggregate" -- the producer guarantees monotonic `occurredAt` per
  aggregate id.

### 8. Dead-letter topics

For every topic `T` in §1, a DLQ topic exists with name `T.dlq`. Rules:

- Consumer routes a message to `<topic>.dlq` after the Nth consecutive
  handler failure on the same envelope `id`. `N` is consumer-defined;
  `N=3` is the recommended default.
- DLQ records preserve the full original envelope plus two extra
  headers: `x-quickbite-dlq-reason` (exception class + short message)
  and `x-quickbite-dlq-original-topic` (the source topic).
- No consumer is wired to DLQ topics by default. Operations replay from
  DLQ is manual (a `kcat` or similar one-off). The Phase 17 report
  mentions DLQs as evidence of the resilience posture; live recovery
  tooling is out of scope.

### 9. Token / auth on Kafka

Kafka messages do **not** carry a JWT. Producer-side authentication is
out of scope for the demo (Kafka in the compose file is unauthenticated,
binds only to the internal network). If the broker is later moved to an
authenticated profile (SASL/SCRAM), Mike-Alfa supersedes this clause.

Consumer-side authorisation is enforced when the consumer subsequently
performs work that mutates another aggregate -- e.g. Payment Service's
refund call back to itself is internal; Notification Service's outbound
message send is its own concern. Consumers SHOULD log the envelope
`id` alongside every handled event for post-hoc audit.

## Team alignment note

Sierra-Lima participates in the event layer only if the Phase 16
stretch is taken (§6). The contract below for §3, §4, §5 is Sierra-Lima's
best reading of Appendix F.7 and is recorded here so the broker and
consumer teams can start coding; a future decision document from
Elephant-Yankee (payment / delivery) or Alfa-Kilo (order) supersedes
the affected rows piece-by-piece.

Authoritative for Sierra-Lima today: §1 (topic list, modulo whether
`menu-events` is built), §2 (envelope), §6 (shape if the stretch is
built), §7 (guarantees), §8 (DLQ naming).

Advisory only: §3, §4, §5 payloads.

## Consequences

- Sierra-Lima's Phase 9 and Phase 16 stretch code writes event
  envelopes that match §2 verbatim.
- If the Phase 16 stretch producer ships, the emit point is
  `MenuService.update()` when `isAvailable` transitions, using
  `KafkaTemplate<String, String>` bound to topic `menu-events`, key
  `menuItemId.toString()`, value the JSON envelope. Failure to produce
  MUST NOT roll back the DB write; the producer retries at most twice
  then logs and moves on (A3 does not require eventual-delivery
  semantics here).
- No runtime Kafka configuration is added to Restaurant Service or
  Menu Service in Phase 9. Phase 16 adds `spring-kafka` to Menu's
  POM only if the stretch is taken.
- Phase 17 report lists this document as the event-contract source.

## Supersedes

None directly. Pins Appendix F.7 of the master plan into a team-visible
decision artefact.
