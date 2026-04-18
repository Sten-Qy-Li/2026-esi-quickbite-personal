# 0004 -- Open Design Questions

- **Status:** Open
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `7c5daba`
- **Source:**
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §9
  Phase 0 Task 5 and Phase 2 Part A.

## Context

Some design details are not worth blocking Phase 0 on, but also must not
be forgotten. This document lists them with a current lean and a target
resolution phase. When each question is answered, record the resolution
inline (`Answer:` block) and, if the answer is non-trivial, move it into a
new numbered decision that supersedes this entry.

## Open Questions

### Q1 -- Error envelope shape across services

- **Question.** What exact JSON shape do Restaurant Service and Menu
  Service return for 4xx errors? Should the envelope include
  `traceId`?
- **Current lean.** Master plan Phase 4 specifies:
  ```json
  {
    "timestamp": "...",
    "status": 422,
    "error": "Unprocessable Entity",
    "message": "...",
    "path": "...",
    "validationErrors": [ { "field": "...", "message": "..." } ]
  }
  ```
  No `traceId` in the MVP. Add one only if the team wires Spring Cloud
  Sleuth or similar.
- **Target phase.** Phase 2 (freeze), Phase 4 (implement for Restaurant).
- **Answer.** _Pending._

### Q2 -- Category vocabulary for `MenuItem.category`

- **Question.** Is `category` a free-form String or an enum? If enum,
  which values?
- **Current lean.** Free-form String, with UI suggestions limited to
  `Appetizer`, `Main`, `Dessert`, `Drink`. Keeps the DB flexible; the
  UI can still constrain.
- **Target phase.** Phase 5 (data model).
- **Answer.** _Pending._

### Q3 -- HATEOAS links in responses

- **Question.** Should responses include HATEOAS links (`_links` block)?
- **Current lean.** No. Out of scope for the checkpoint demos and adds
  coupling with the frontend that Vue is not set up to consume.
- **Target phase.** Phase 4 / Phase 6.
- **Answer.** _Pending._

### Q4 -- `Location` and `Price` as JPA Embeddables

- **Question.** Do we actually embed the value objects from A2
  (`Location`: address/city/lat/lng; `Price`: amount/currency), or keep
  them as flat columns on the aggregate root?
- **Current lean.** Embed, because A2 DDD explicitly calls them out as
  value objects. Flyway schema is unchanged (columns stay flat) but the
  Java entity uses `@Embeddable`.
- **Target phase.** Phase 3 (Restaurant) and Phase 5 (Menu).
- **Answer.** _Pending._

### Q5 -- Restaurant-closed signalling: 200 or 409?

- **Question.** When `GET /restaurants/{id}/availability` is called on a
  closed restaurant, do we return `200` with `acceptsOrders:false` or
  `409 Conflict`?
- **Current lean.** `200` with `acceptsOrders:false`. A 200 lets the
  Order Service decide (maybe the client can queue a future order). A
  409 would force a hard rejection semantics that A3 does not require.
- **Target phase.** Phase 1 (agree with team) and Phase 9 (lock).
- **Answer.** _Pending._

### Q6 -- Browse-route protection (`GET` on restaurants and menu items)

- **Question.** Are `GET /restaurants`, `GET /restaurants/{id}`,
  `GET /restaurants/{rid}/menu-items`, and `GET /menu-items/{id}` public,
  or do they require at least a `Customer` token?
- **Current lean.** Public by default (per master plan §5.1 default 1).
  Tighten to customer-only only if the team decides so in Phase 1.
- **Target phase.** Phase 1.
- **Answer.** Resolved by [`0010-auth-contract.md`](./0010-auth-contract.md)
  §6 on 2026-04-18: public. Short rationale: W1 does not browse, the
  tightening is a security-filter change, and a login wall is not
  required for the CP#1 browse demo.

### Q7 -- Pagination strategy for `GET /restaurants`

- **Question.** Spring Data `Pageable` with `page`/`size`/`sort` query
  params, or a simpler offset/limit?
- **Current lean.** `Pageable` (Spring Data default). Matches how Spring
  Boot practicals work; gives sorting for free.
- **Target phase.** Phase 3 / Phase 4.
- **Answer.** _Pending._

### Q8 -- Seed data format: Flyway or `CommandLineRunner`?

- **Question.** Do we put demo restaurants and menu items in a Flyway
  `V2__seed_demo_data.sql` or in a Spring `CommandLineRunner` behind a
  `dev` profile?
- **Current lean.** Flyway migration, because it plays nicely with
  `ddl-auto=validate` and works identically in Docker and on the host.
  `CommandLineRunner` is the fallback if Flyway ordering becomes
  awkward.
- **Target phase.** Phase 2 (plan), Phase 7 (implement).
- **Answer.** _Pending._

### Q9 -- Test layer strategy

- **Question.** Do we use Testcontainers (real PostgreSQL in Docker
  during tests) or H2 with PostgreSQL dialect?
- **Current lean.** Testcontainers if setup is trouble-free on Windows;
  H2 otherwise. Tests should not depend on the host having a running
  PostgreSQL container pre-started.
- **Target phase.** Phase 7.
- **Answer.** _Pending._

### Q10 -- Audit auditor strategy before Phase 7

- **Question.** Between Phase 4 (when JPA auditing is enabled) and
  Phase 7 (when `JwtAuthFilter` populates `SecurityContext`), what does
  `AuditorAware<UUID>` return?
- **Current lean.** A fixed placeholder UUID (`00000000-0000-0000-0000-000000000000`)
  marked as the "system" user. Swap to `SecurityContextHolder`-sourced
  UUID once Phase 7 lands.
- **Target phase.** Phase 4 (introduce placeholder), Phase 7 (swap).
- **Answer.** _Pending._

## Resolution protocol

When a question is answered:

1. Replace `_Pending._` with `Resolved in phase <N>: <short answer>`.
2. If the answer is longer than one sentence or has wide impact, write a
   new `NNNN-<slug>.md` decision document and mark the question
   `Resolved by <NNNN>`.
3. Keep answered questions in this file as a running log.

## Supersedes

None.
