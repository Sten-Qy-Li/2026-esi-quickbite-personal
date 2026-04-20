# 0031 -- Cross-Service Status-Code Table for W1 Validation Failures

- **Status:** Accepted
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `5bd6f45`
- **Source:**
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §9
  Phase 9 Task 2 and Phase 10 DoD.

## Context

Order Service fans out to Restaurant, Menu, Payment, and Delivery
during W1. Each callee must return predictable status codes so Order
can map them to a single end-user outcome without guessing. Phase 9
Task 2 asks the team to lock this table in writing. Phase 10 treats it
as the contract that Order's failure-path tests assert against.

This document covers **the Sierra-Lima half of the table authoritatively**
(hops 4 and 5 of [`0030`](./0030-w1-synchronous-contract-lock.md) §1) and
records a best-reading advisory row for the teammate-owned services.
The plan's unilateral-fallback clause is invoked; Alfa-Kilo's or
Elephant-Yankee's later decision supersedes only the advisory rows.

It also resolves open question
[Q5 of `0004-open-questions.md`](./0004-open-questions.md): restaurant-closed
signalling stays `200` with `acceptsOrders:false`, not `409`.

## Decision

### 1. Restaurant Service status codes (hop 4, Sierra-Lima authoritative)

Endpoint: `GET /restaurants/{id}/availability`.

| Condition | HTTP | Body | Order Service reaction |
|-----------|------|------|------------------------|
| Restaurant exists, `isOpen=true`, within `operatingHours` | `200` | `acceptsOrders:true` payload per `0030` §3 | Proceed to hop 5. |
| Restaurant exists, `isOpen=false` | `200` | `acceptsOrders:false` payload | Reject order with `422 Unprocessable Entity`, reason `RESTAURANT_CLOSED`. |
| Restaurant exists, `isOpen=true`, outside `operatingHours` | `200` | `acceptsOrders:false` payload | Reject order with `422 Unprocessable Entity`, reason `RESTAURANT_NOT_ACCEPTING_ORDERS`. |
| Restaurant does not exist | `404` | Error envelope per `0020` §7 | Reject order with `422 Unprocessable Entity`, reason `RESTAURANT_NOT_FOUND`. |
| Unauthenticated (no / bad token) | `401` | Error envelope | Propagate `401` to the client (hop 2 normally catches this; if not, Order returns `401`). |
| Authenticated but role not permitted | `403` | Error envelope | Propagate `403`. Any Customer token is permitted per `0010` §8, so this fires only on malformed tokens. |

`acceptsOrders:false` wins over `isOpen:true` by design: hours can close
the window even while the flag is open. Order Service **must not** decide
on `isOpen` alone (see also `0030` §3).

### 2. Menu Service status codes (hop 5, Sierra-Lima authoritative)

Endpoint: `POST /menu-items/validate`.

Menu returns `200` whenever the request itself is well-formed, even if
every line is invalid. The per-line state lives inside `items[].error`
and the top-level `allValid` flag. Only the request-envelope problems
below fire non-200.

| Condition | HTTP | Body | Order Service reaction |
|-----------|------|------|------------------------|
| Request well-formed, all lines valid | `200` | `allValid:true` payload per `0030` §4 | Proceed to hop 6 (persist order). |
| Request well-formed, some lines invalid | `200` | `allValid:false`, per-line `error` codes per `0030` §5 | Reject order with `422`, reason `MENU_VALIDATION_FAILED`, pass the per-item `items[]` block through verbatim in the error response envelope so the client can show per-line detail. |
| Request body malformed (missing fields, non-UUID, negative quantity, empty `items`) | `400` | Error envelope with `validationErrors[]` per `0020` §7 | Return `500 Internal Server Error` to the client -- this is an Order-Service bug, not an end-user error. Alert-worthy in production. |
| Request body exceeds 100 items | `400` | Error envelope | Same as malformed. |
| Unauthenticated | `401` | Error envelope | As per §1. |
| Authenticated but role not permitted | `403` | Error envelope | As per §1. |

Menu **does not** return `422` for unknown or unavailable items. The
`422` decision is made by Order Service after reading the `200`
response body. This keeps Menu's endpoint a pure per-line classifier and
lets Order consolidate several per-line failures into one user-facing
error.

### 3. Order Service outward mapping (advisory for Sierra-Lima)

The end-user response from `POST /api/orders`. Recorded here so that the
Phase 10 test plan can be written today; Alfa-Kilo's service decision
supersedes.

| Upstream state | End-user HTTP | End-user reason |
|----------------|---------------|-----------------|
| Hop 3 customer missing / inactive | `401` | `CUSTOMER_NOT_AUTHENTICATED` |
| Hop 4 restaurant not found | `422` | `RESTAURANT_NOT_FOUND` |
| Hop 4 `acceptsOrders:false` (closed) | `422` | `RESTAURANT_CLOSED` |
| Hop 4 `acceptsOrders:false` (outside hours) | `422` | `RESTAURANT_NOT_ACCEPTING_ORDERS` |
| Hop 5 any line `MENU_ITEM_NOT_FOUND` | `422` | `MENU_VALIDATION_FAILED` + per-line detail |
| Hop 5 any line `MENU_ITEM_NOT_AVAILABLE` | `422` | `MENU_VALIDATION_FAILED` + per-line detail |
| Hop 7 payment `status:Failed` | `402` | `PAYMENT_DECLINED` |
| Hop 8 delivery creation fails | `503` | `DELIVERY_UNAVAILABLE` (after compensating refund) |
| Any hop 5xx other than above | `502` | `UPSTREAM_SERVICE_ERROR` |
| Any hop times out | `504` | `UPSTREAM_TIMEOUT` |

Order Service MUST surface per-line detail on `MENU_VALIDATION_FAILED`
so the frontend can highlight the bad rows. Payment and Delivery
failures roll up to the order as a whole.

### 4. Error-envelope shape (reminder)

Every non-200 response from Sierra-Lima, whether 4xx or 5xx, uses the
canonical envelope locked in `0020` §7. This holds for both services
and all HTTP methods.

```json
{
  "timestamp": "2026-05-05T12:34:56Z",
  "status": 404,
  "error": "Not Found",
  "message": "Restaurant d0000001-... not found",
  "path": "/restaurants/d0000001-.../availability",
  "validationErrors": [ /* only on 400/422 */ ]
}
```

`path` is the internal controller path (post gateway `/api` strip).
`validationErrors[]` is present only for request-body validation
failures (`400`). A `422` from Sierra-Lima uses the same envelope
without `validationErrors[]`; the cause lives in `message`.

### 5. Retry posture

Order Service treats Sierra-Lima responses as:

- `2xx` / `4xx` -- authoritative, do not retry.
- `5xx` / network failure -- retry **at most twice** with a 200 ms / 500
  ms backoff before giving up and mapping to the §3 `502` / `504` row.

Sierra-Lima's endpoints are safe to retry (`GET` for availability,
idempotent read for validate). Order Service should not retry
`POST /payments` without its own idempotency key, per `0030` §7.

## Team alignment note

Authoritative: §1, §2, §4, §5 on the Sierra-Lima side. Sierra-Lima's
services already return these codes today (verified against
`GlobalExceptionHandler` on `5bd6f45`); §5's retry discipline is an
advisory to Order Service implementers, not something Sierra-Lima
enforces.

Advisory only: §3. Alfa-Kilo's first Order-Service decision document
supersedes any row.

## Consequences

- Resolves Q5 of [`0004-open-questions.md`](./0004-open-questions.md):
  restaurant-closed signalling is `200` with `acceptsOrders:false`.
  Q5 is marked `Resolved by 0031` in that file.
- Sierra-Lima's Phase 10 integration tests assert the §1 and §2 rows
  via WireMock stubs; no code change is required in Sierra-Lima's
  services to satisfy this table beyond the DTO reshape tracked by
  `0030` and executed in the same Phase 9 session.
- The `@PreAuthorize` wiring on `/restaurants/{id}/availability` and
  `/menu-items/validate` is already `Any valid token / SERVICE` per
  `0010` §8. No further route-matrix work needed for Phase 9.

## Supersedes

None directly. Finalises Q5 from `0004-open-questions.md` and extends
`0020-sierra-lima-contracts.md` with explicit failure-row status codes.
