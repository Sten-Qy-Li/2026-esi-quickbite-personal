# 0001 -- Scope Freeze

- **Status:** Accepted
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `7c5daba`
- **Source:** Assignment 3 §2.4 and §7, and
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §4.

## Context

QuickBite, as described in Assignments 1 and 2, has **eight** business
services. Assignment 3 narrows the build to a smaller subset and pins team
ownership. Any drift on this subset makes every later phase harder: the wrong
services get stubbed, diagrams mislead, checkpoints misfire.

Phase 0 of the master plan requires a written scope freeze that no team
member can later dispute.

## Decision

The QuickBite implementation subset is **frozen** at the scope below for the
remainder of the 2026 ESI Project.

### Implemented business services (7)

1. `User Service` -- owned by Alfa-Kilo.
2. `Order Service` -- owned by Alfa-Kilo.
3. `Restaurant Service` -- owned by **Sierra-Lima**.
4. `Menu Service` -- owned by **Sierra-Lima**.
5. `Payment Service` -- owned by Elephant-Yankee.
6. `Delivery Service` -- owned by Elephant-Yankee.
7. `Notification Service` -- owned by Mike-Alfa.

### Implemented shared components (2)

1. `API Gateway` -- owned by Alfa-Kilo (Spring Cloud Gateway).
2. `Event Broker configuration` -- owned by Mike-Alfa (Apache Kafka).

### Design-only (not coded) (1)

1. `Review Service` -- documented in the report, covered in diagrams, but
   no code.

### Sierra-Lima scope

Sierra-Lima implements **two** business services and **does not** replace
either with a shared integration or resilience component.

| Service | Owned requirements |
|---------|-------------------|
| Restaurant Service | R19 (register/manage restaurant), R20 (update open/closed and operating hours) |
| Menu Service | R21 (add/update/remove menu items), R22 (browse menu) |

## Consequences

- Every phase in the master plan targets this subset. Scope changes require
  an updated decision document that supersedes this one.
- `Review Service` is out of the code base unless the instructor explicitly
  asks for it at the 2026-04-21 or 2026-04-28 sessions.
- No teammate should be stubbing a service outside this list. Anyone still
  working from the older "six-service" assumption is misaligned -- flag and
  correct.
- Cross-service references from Sierra-Lima's services:
  - `Restaurant.ownerId` references `User.userId` (Alfa-Kilo).
  - `MenuItem.restaurantId` references `Restaurant.restaurantId`
    (Sierra-Lima).
  References are stored as UUIDs only. No cross-DB foreign keys.

## Supersedes

None. This is the first scope-freeze decision in this workspace.
