# 0002 -- Named Workflows W1 / W2 / W3

- **Status:** Accepted
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `7c5daba`
- **Source:** Assignment 3 §5.1-§5.3, and
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §6
  and Appendix F.

## Context

Assignment 3 requires QuickBite to demonstrate **both** synchronous REST
integration and asynchronous event-driven integration. The design baseline
in Assignment 3 defines three end-to-end workflows. They need stable,
short labels (`W1`, `W2`, `W3`) so every later phase, diagram, Postman
folder, and slide can refer to them without ambiguity.

## Decision

Three named workflows are **frozen** for the project.

### W1 -- Place Order (synchronous, REST)

Customer → API Gateway → Order Service. Order Service then calls
User Service (customer lookup), Restaurant Service (availability check),
Menu Service (batch validation), Payment Service (charge), and Delivery
Service (task creation) -- all as synchronous REST hops, all carrying a
bearer token. Sierra-Lima participates as a **callee** in steps 4 and 5:
`GET /restaurants/{id}/availability` and `POST /menu-items/validate`.
The full call chain and failure handling are pinned in the master plan
Appendix F.6.

### W2 -- Delivery Progress & Notifications (asynchronous, Kafka)

Delivery Service publishes `delivery.status-changed` events on the
`delivery-events` topic as a delivery moves through its lifecycle
(assigned, picked up, delivered). Order Service and Notification Service
consume these events -- Order updates its state, Notification messages the
customer. Sierra-Lima is **not** a producer or consumer here. The event
envelope and payload are pinned in the master plan Appendix F.7.

### W3 -- Payment Outcome Notification (asynchronous, Kafka)

Payment Service publishes `payment.completed` or `payment.failed` events
on the `payment-events` topic once a charge resolves. Notification Service
always consumes. Order Service consumes `payment.failed` so it can
transition the order to `Cancelled`. Sierra-Lima is **not** a producer or
consumer here. The event envelope and payload are pinned in the master
plan Appendix F.7.

## Sierra-Lima's concrete responsibility

| Workflow | Role | Action |
|----------|------|--------|
| W1 | Synchronous callee | Keep `GET /restaurants/{id}/availability` and `POST /menu-items/validate` fast, correct, and stable. Return the exact shapes in Appendix F.1 and F.2. |
| W2 | No direct role | Keep Restaurant and Menu services responsive while teammates run the flow. |
| W3 | No direct role | Same as W2. |

Optional stretch (Phase 16 only, time permitting): Menu Service publishes
`menu.item-availability-changed` on a new `menu-events` topic. Not in the
baseline A3 scope; only shipped if backend is stable.

## Consequences

- Postman collection folders are named `W1 Integration` and `Async Evidence`
  (W2 + W3) rather than per-service.
- The report and final slide deck use these exact labels.
- Diagrams in Figures 3 (W1) and 4 (W2 + W3) are the authoritative
  references; the master plan Appendix F summarises them.
- Any new inter-service communication that cannot be classified as one of
  W1, W2, or W3 is out of scope unless added by a superseding decision.

## Supersedes

None. This is the first workflow-freeze decision.
