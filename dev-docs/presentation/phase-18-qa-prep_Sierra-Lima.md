# CP#3 Q&A Drill -- Sierra-Lima slice

- **Owner.** Sierra-Lima drives the drill; every callsign rehearses
  their assigned answer aloud at the 2026-05-18 rehearsal.
- **Source.** `Charlie-Lima-Alfa_a520963_project-phases-final.md`
  Phase 18 task 5 (seven anticipated questions).
- **Budget.** 10 min total Q&A. Answers below are timeboxed at
  **<= 45 s aloud** (~120 words) so we can fit 10-12 questions
  without stalling.
- **Rule.** First sentence is always the headline answer; second
  sentence is the *why*; third is the citation
  (`report §X`, `ADR XX`, `phase-XX-verification`). If the grader
  presses, we go deeper from the cite.

The seven anticipated questions from Phase 18 task 5 are below;
each is annotated with the assigned answerer plus a backup. We
also append three "likely follow-ups" the grader is statistically
most likely to ask given the slides.

---

## Q1 -- "Why these seven services and not all eight?"

- **Answerer.** Alfa-Kilo. **Backup.** Sierra-Lima.
- **Cites.** Report §11.2; CP#1 talking-points §1; ADR 0001 §3.

**Headline.** "We implemented seven of the eight bounded contexts
and kept `Review` design-only. Assignment 3 §2 sets the bar at
*at least five end-to-end with at least one async workflow* --
seven gives us comfortable margin without thinning anyone's
ownership slot."

**Why.** "Four owners over ~10 weeks can't ship eight CRUD
surfaces, two demo workflows, a frontend, and an integration
test pack. Dropping `Review` freed exactly one slot, which we
poured into Phase 15 ownership hardening and Phase 16 async
evidence."

**If pressed -- "what would Review have looked like?"**
"It owns post-order feedback: a `Review` aggregate with an
`OrderRef` value object, keyed on `(orderId, customerId)`,
consuming `order-events` to gate eligibility on
`order.delivered`. Migration path is in report §11.2; no
Sierra-Lima schema change required."

---

## Q2 -- "Why is `Review` design-only?"

- **Answerer.** Alfa-Kilo. **Backup.** Sierra-Lima.
- **Cites.** Report §11.2; CP#1 talking-points §1.

**Headline.** "`Review` doesn't appear in any of W1, W2, or W3
-- the three workflows the rubric requires us to demo. Adding
it would mean inventing a fourth workflow that lives entirely
inside `Review`, and there are none in the A3 baseline."

**Why.** "It's still in the business architecture (Figure 1)
so the grader sees it was modelled. The implementation
deferral is captured in §13.3 of the report under limitations,
not hidden."

**If pressed -- "is it on the future-work list?"**
"Yes -- report §14, third bullet. The migration path is a
single Spring Boot service plus a `review_db`; no upstream
service changes."

---

## Q3 -- "How were service boundaries chosen?"

- **Answerer.** Alfa-Kilo. **Backup.** Mike-Alfa.
- **Cites.** Assignment 2 (DDD analysis); report §2; CP#1 §1.

**Headline.** "Domain-Driven Design from Assignment 2: one
bounded context per service. We identified eight contexts via
event storming and ubiquitous-language workshops; each became
its own service when we moved to Assignment 3."

**Why.** "The `Restaurant` aggregate owns `Restaurant`,
embedded `Location`, and the `isOpen` policy. The `Menu`
aggregate owns `MenuItem` and embedded `Price`. They share no
data; cross-context references are UUIDs only. That gives us
deploy independence -- each service has its own Postgres,
its own Flyway migrations, its own credentials (CP#1 §3)."

**If pressed -- "why isn't `Menu` a sub-aggregate of
`Restaurant`?"** "Different change cadences. Operating hours
and `isOpen` change daily; menu items and prices change
weekly. Splitting them keeps the write path on each side
focused, and Phase 15's ownership lookup (Menu -> Restaurant)
is cheap and easy to reason about."

---

## Q4 -- "How does async integration work? Topics, envelope, idempotency."

- **Answerer.** Mike-Alfa. **Backup.** Elephant-Yankee.
- **Cites.** ADR 0032 (envelope lock); report §6.2; ADR 0040
  (Sierra-Lima posture).

**Headline.** "Five Kafka topics: `order-events`,
`payment-events`, `delivery-events`, `notification-events`,
plus Sierra-Lima's log-only `menu-events`. Every envelope
has the same shape: `id` (UUID v4), `type`, `occurredAt`
(ISO-8601 UTC), `payload`."

**Why.** "At-least-once delivery; consumers dedupe by
`envelope.id`. Producers use the aggregate ID as the Kafka
record key so messages for the same aggregate stay
ordered on the partition. Notification consumes everything;
Order consumes payment + delivery."

**If pressed -- "show us one envelope."**
Toggle a menu item in the running browser; the `menu-events`
log line appears in the compose pane within a second. Walk
through the JSON fields against the slide S9 envelope.

---

## Q5 -- "How was security enforced at gateway vs service level?"

- **Answerer.** Sierra-Lima. **Backup.** Alfa-Kilo.
- **Cites.** ADR 0010 (auth contract); ADR 0033 (token relay);
  report §7.

**Headline.** "Two layers, both run on every protected request.
Gateway shape-checks the bearer header and forwards verbatim
with `/api` stripped. Each service signature-verifies via a
`JwtAuthFilter`, extracts `userId` and `role`, and applies a
`@PreAuthorize` matrix."

**Why.** "Defence in depth: the gateway can be misconfigured
without leaking past the service filter, and the service
filter can be bypassed in dev (compose calls direct to
`:8081`) without losing the gateway's filtering for production.
Phase 15 added an *ownership* check on top: an actor with role
`RestaurantOwner` can only mutate restaurants and menu items
they own."

**If pressed -- "show us a 403."**
Postman -> `Negative Auth` -> `[403] PUT /restaurants/{ownerB-rest}
as ownerA`. Status 403; body is the `ErrorResponse` envelope; in
the service log a `WARN ownership denial actor=...` line appears
matching the demo script §4.2.

**If pressed -- "what stops the gateway from re-signing?"**
"It doesn't have the secret. The `JWT_SECRET` is loaded only by
services that *verify*; gateway only forwards. ADR 0010 §4.2."

---

## Q6 -- "Why no Eureka?"

- **Answerer.** Alfa-Kilo. **Backup.** Sierra-Lima.
- **Cites.** ADR 0005 §3; CP#1 §2; report §13.3 (limitations).

**Headline.** "Out of scope. Assignment 3 §3 lists the shared
infrastructure as *API Gateway* and *Event Broker
configuration* -- not a discovery server. Adding Eureka would
be scope creep."

**Why.** "Four services on one laptop is the entire universe
of the demo. Discovery earns its complexity at tens of
instances, not at four. Static DNS-name routing on the
compose network is deterministic, fast, and lets the grader
`curl localhost:8081` without bootstrapping a registry."

**If pressed -- "would you add it for production?"**
"Yes, with Consul or Spring Cloud Discovery. It's listed in
the report as a limitation we accept consciously, not an
oversight."

---

## Q7 -- "What did you sacrifice and why?"

- **Answerer.** Alfa-Kilo. **Backup.** Sierra-Lima.
- **Cites.** Report §13 (full limitations list); ADR 0040 §2
  (Sierra-Lima async stance).

**Headline.** "Five conscious sacrifices, each tied to a budget
trade-off rather than an oversight."

**Why.**

1. `Review Service` -- Q1/Q2 above.
2. Eureka -- Q6 above.
3. CI runner -- local `mvn test` + `smoke-cross-service.sh`
   covers the test surface; CI is excluded by the master plan
   §8 non-goals.
4. Resilience4j on Sierra-Lima's outbound Menu -> Restaurant
   call -- the call is on the *mutation* path, not the hot
   read path; Restaurant outage blocking Menu mutations is the
   acceptable failure mode (report §13.3).
5. Kafka client on Sierra-Lima's classpath -- `menu-events` is
   log-only; the `KafkaMenuEventPublisher` is a one-class
   drop-in for after CP#3 (ADR 0040 §6).

**If pressed -- "what would you do first if you had another
week?"** "Two things: (a) stand up Mike-Alfa's broker against
the swap-in `KafkaMenuEventPublisher` so `menu-events` is on
real Kafka end-to-end; (b) add a `restaurant.deleted` event
that Menu consumes to soft-delete orphan items. Both are in
report §14."

---

## Likely follow-ups

These didn't appear in Phase 18 task 5 verbatim but are the
statistically-most-likely Q&A based on the deck content. We
keep one-line answers ready.

### F1 -- "How do you know your tests actually pass?"

- **Answerer.** Sierra-Lima.
- **Headline.** "23 / 23 Restaurant + 42 / 42 Menu JUnit + 9
  Postman requests / 40 assertions for W1 + 14 / 17 for
  Negative Auth -- numbers in report §10. The
  `smoke-cross-service.sh` evidence trace under
  `services/local-dev/evidence/` is dated."

### F2 -- "Walk us through the `menu-events` envelope."

- **Answerer.** Sierra-Lima.
- **Headline.** Toggle an item live; read the log line
  field-by-field (`id` -- per-emit UUID; `type` --
  `menu.item-availability-changed`; `occurredAt` -- UTC ISO
  string from an injected `Clock`; payload includes both
  `isAvailable` and `previousIsAvailable`). "Envelope matches
  ADR 0032 §6 verbatim; ADR 0040 §3 records that we're emitting
  on a logger named `menu-events`, not on Kafka."

### F3 -- "How does the frontend know when a token expires?"

- **Answerer.** Sierra-Lima.
- **Headline.** "Reactively. Any `/api/**` call that returns
  `401` triggers `apiFetch`'s 401 handler in
  `src/api/client.js`: it clears `localStorage.quickbite.jwt`
  and redirects to `/login?next=<currentPath>`. Tokens have a
  baked-in expiry claim but we don't pre-check; the server is
  the authority. Report §8.2."

---

## Drill protocol for the rehearsal

At the 2026-05-18 rehearsal:

1. Sierra-Lima reads the question aloud.
2. The named answerer responds **without** looking at this file.
3. The room times the answer; if it crosses 60 s, the answerer
   tightens the talk track on the spot.
4. Repeat each question once more after a short break to check
   the tightening stuck.
5. Each callsign confirms aloud: "I have my four cites memorised."
   (Each answer here cites at most four artefacts.)

If a question feels under-rehearsed, add a second pass before
moving to the next question. Q&A is the half of the talk we
don't control; rehearsing answers down to muscle memory is
where the marks come from.
