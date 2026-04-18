# Chat Archive - 2026-04-18 - Charlie-Lima-Alfa (`5bd6f45`)

## Session Summary

This session executed **Phase 9 -- Team Contract Lock for W1 / W2 / W3**
end to end for Sierra-Lima's services, as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
§9 Phase 9 (lines 1156-1207).

The session began on top of `5bd6f45` ("Land Phase 8 dockerisation:
app services in compose, per-service DBs"). Sierra-Lima's three
teammate callsigns (Alfa-Kilo for User/Order/Gateway, Elephant-Yankee
for Payment/Delivery, Mike-Alfa for Notification/Kafka) were
unavailable. The user invoked the master plan's unilateral-fallback
clause and asked Charlie-Lima-Alfa to lock the contracts on the
team's behalf with best-reading defaults from Assignment 3 Appendix F.
A context-compaction auto-summary occurred mid-session after the
four decision documents were written and the DTO reshape was
drafted, but before the test suite went green; the archived context
below covers the full arc from initial survey to landed commit.

All six Phase 9 tasks reached the Definition of Done. Two test
failures surfaced during the reshape (Spring constructor ambiguity
and Mockito's greediest-constructor heuristic) and were fixed in-
session without widening the change. `mvn -B test` produces 46
passing tests across both services after the fix. The contracts are
ready for Phase 10 integration work.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`
- Today: 2026-04-18 (Saturday)
- Active branch: `dev`
- Parent commit: `5bd6f45` -- "Land Phase 8 dockerisation: app
  services in compose, per-service DBs"
- Landing commit: `a27a046` -- "Land Phase 9 team contract lock:
  W1/W2/W3 contracts, DTO reshape"
- Environment: Windows 11 + IntelliJ IDEA 2026.1 + Git Bash
- Java: 17.0.18 (Microsoft OpenJDK) via `ms-17` project SDK
- Test framework: JUnit 5, Mockito 5, Spring Boot Test, H2 in
  PostgreSQL mode

## Coordination posture

Per the user's explicit instruction at session start, decisions
normally requiring four-callsign agreement were taken unilaterally:

> "My team members would very likely be unavailable for communication
> and coordination, so please make preliminary decisions on their
> behalf first (optimal decisions according to prior work Assignment
> 3 and your discretion/opinions); if they disagree later, my opinion
> is that we can always make the necessary changes at that point of
> time."

This matches the master plan's own fallback clause ("locking
unilaterally is permitted; supersede via a later-numbered decision
if anyone pushes back"). Each new decision doc therefore:

- Marks **Status: Accepted** without hedging suffixes (per memory
  feedback `feedback_default_fallbacks.md`).
- Distinguishes Sierra-Lima-authoritative rows (written against
  committed code) from advisory rows (best-reading for teammate-
  owned services).
- Lists the supersession path explicitly so a pushback from
  Alfa-Kilo or Elephant-Yankee is a single-row swap, not a rewrite.

## Phase 9 Task-by-Task Record

### Task 1 -- W1 synchronous call chain locked

Frozen in
[`dev-docs/decisions/0030-w1-synchronous-contract-lock.md`](../decisions/0030-w1-synchronous-contract-lock.md)
§1. Hop chain lifted verbatim from Appendix F.6:

| # | From | To | Call |
|---|------|------|------|
| 1-2 | Client / Gateway | Order | `POST /api/orders` -> `POST /orders` |
| 3 | Order | User | `GET /users/{customerId}` |
| 4 | Order | **Restaurant** | **`GET /restaurants/{id}/availability`** |
| 5 | Order | **Menu** | **`POST /menu-items/validate`** |
| 6 | Order | (local) | Persist `Order` `Placed` |
| 7 | Order | Payment | `POST /payments` |
| 8 | Order | Delivery | `POST /deliveries` |
| 9 | Order | Client | `201 Created` |

§3 and §4 of the decision doc lock the exact JSON shapes Sierra-Lima
returns on hops 4 and 5; §5 locks the per-line error enum strings
(`MENU_ITEM_NOT_FOUND`, `MENU_ITEM_NOT_AVAILABLE`); §6 pins advisory
shapes for Payment and Delivery so Order Service has a written
target before Elephant-Yankee's service lands.

### Task 2 -- Status codes locked

Frozen in
[`dev-docs/decisions/0031-cross-service-status-code-table.md`](../decisions/0031-cross-service-status-code-table.md).

- §1 -- Restaurant availability: `200` on exists (regardless of
  open/hours; distinguish via `acceptsOrders`), `404` on not found,
  `401`/`403` on auth failures.
- §2 -- Menu validate: `200` whenever request is well-formed (even
  if every line is invalid). `400` on malformed body (missing
  fields, bad UUID, quantity out of range, >100 items). `422` for
  per-line domain failures **is made by Order Service**, not Menu.
- §3 -- Advisory Order-Service outward mapping: `422 RESTAURANT_CLOSED`,
  `422 MENU_VALIDATION_FAILED`, `402 PAYMENT_DECLINED`,
  `503 DELIVERY_UNAVAILABLE`.
- §5 -- Retry posture for Order Service (at most twice, 200ms/500ms
  backoff on 5xx/network failures; never retry 4xx).

Q5 of `dev-docs/decisions/0004-open-questions.md` was resolved at the
same time: restaurant-closed signalling stays `200` with
`acceptsOrders:false`, not `409`. Doc updated in the same commit.

### Task 3 -- Event contracts locked

Frozen in
[`dev-docs/decisions/0032-w2-w3-event-contract-lock.md`](../decisions/0032-w2-w3-event-contract-lock.md)
§1. Topic list:

| Topic | Producer | Required |
|-------|----------|----------|
| `payment-events` | Payment | Yes |
| `delivery-events` | Delivery | Yes |
| `order-events` | Order | Yes (compensating) |
| `menu-events` | Menu | Optional (Phase 16 stretch) |

Per-type payloads pinned in §3 (`payment.completed`, `payment.failed`),
§4 (`delivery.status-changed`), §5 (`order.cancelled`), §6
(`menu.item-availability-changed`, stretch only).

### Task 4 -- Event envelope shape locked

§2 of `0032`:

```json
{
  "id": "3e28a4c0-0000-0000-0000-00000000abcd",
  "type": "payment.completed",
  "occurredAt": "2026-05-05T12:34:58.123Z",
  "payload": { }
}
```

`id` is the idempotency key. `type` is dotted `domain.past-tense`.
`occurredAt` is ISO-8601 UTC. Kafka record key is optional; when
used, producers set it to the natural aggregate id.

### Task 5 -- Dead-letter and idempotency expectations

§7 and §8 of `0032`:

- At-least-once delivery. Consumers dedupe on envelope `id` (in-
  memory LRU fine for CP#1; persistent dedupe table is a Phase 16
  upgrade).
- Producers MUST set `id` once per domain event and re-use it on
  retries.
- DLQ naming: `<topic>.dlq`. Routing happens after Nth consecutive
  handler failure (recommended `N=3`). DLQ records preserve the
  original envelope plus `x-quickbite-dlq-reason` and
  `x-quickbite-dlq-original-topic` headers.

### Task 6 -- Token propagation confirmed

Frozen in
[`dev-docs/decisions/0033-inter-service-token-propagation-lock.md`](../decisions/0033-inter-service-token-propagation-lock.md).

Default posture for every user-triggered W1 hop: **token relay**.
Order Service forwards the inbound user token unchanged. Service
tokens are minted only for compensations (hop 8c refund) and scheduled
work. Key constraints:

- Service-token TTL `exp - iat <= 300s`, not cached across calls.
- `tokenType:SERVICE`, `serviceName` claim set to caller id, `role`
  omitted on service tokens.
- Sierra-Lima's `JwtAuthFilter` accepts service tokens on the two
  internal endpoints but **not** on mutation endpoints (§4 bold
  cell). A service wanting to mutate must carry a user token.
- ±30s clock skew on `exp`/`nbf`/`iat`.
- No full JWTs in logs.

### DoD remediation -- response shapes "not drafts"

The Phase 9 DoD bullet 2 reads: "Sierra-Lima's availability and
batch-validation response shapes are the ones Order Service will call
-- not drafts." During the Task 1 session survey, a divergence was
discovered between the code on `5bd6f45` and the shapes frozen in
`0020-sierra-lima-contracts.md` §1.6 / §2.6:

| Shape | Committed at 5bd6f45 | 0020 freeze | Resolution |
|-------|----------------------|-------------|-----------|
| `AvailabilityResponse` | missing `acceptsOrders`, missing `checkedAt` | both required | **Reshape** |
| `ValidateMenuItemsResponse` | `results[]` (not `items[]`), `available` (not `isAvailable`), `lineTotalAmount` (not `lineTotal`), `reason` (not `error`); missing `totalAmount`/`currency` at top level | matches 0030 §4 | **Reshape** |
| Request validator | `@Positive` quantity; no `@Size(max=100)` on items | `@Min(1)`/`@Max(100)`, `@Size(max=100)` | **Tighten** |

The DoD forbids leaving these as drafts, so the session went beyond
writing decision docs and also reshaped the code:

**`AvailabilityResponse` reshape** -- added `acceptsOrders` (boolean)
and `checkedAt` (Instant) to the record. Service-layer computes
`acceptsOrders = isOpen && isWithinOperatingHours()` where the
operating-hours parser honours overnight windows (`"22:00-02:00"`).
Clock is now injected:

```java
@Service
public class RestaurantService {
    private static final ZoneId OPERATING_HOURS_ZONE = ZoneId.of("Europe/Tallinn");
    private final Clock clock;

    @Autowired
    public RestaurantService(RestaurantRepository restaurants, CurrentUser currentUser) {
        this(restaurants, currentUser, Clock.system(OPERATING_HOURS_ZONE));
    }

    RestaurantService(RestaurantRepository restaurants, CurrentUser currentUser, Clock clock) {
        this.restaurants = restaurants;
        this.currentUser = currentUser;
        this.clock = clock;
    }
    // ...
}
```

The 2-arg public constructor carries `@Autowired` so Spring doesn't
need to guess; the 3-arg package-private constructor is for tests.

**`ValidateMenuItemsResponse` reshape** -- renamed `results` to
`items`, added `totalAmount` + `currency` at the top level, used
enum string constants for per-line `error`:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidateMenuItemsResponse(
    boolean allValid,
    List<Line> items,
    BigDecimal totalAmount,
    String currency
) {
    public static final String ERROR_NOT_FOUND = "MENU_ITEM_NOT_FOUND";
    public static final String ERROR_NOT_AVAILABLE = "MENU_ITEM_NOT_AVAILABLE";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Line(
        UUID menuItemId, int quantity, boolean exists,
        @JsonProperty("isAvailable") boolean isAvailable,
        BigDecimal unitPriceAmount, String unitPriceCurrency,
        BigDecimal lineTotal, String error
    ) {}
}
```

`@JsonProperty("isAvailable")` keeps the Jackson serialisation
explicit against the is-prefix primitive-boolean heuristic, since
`0020` locks the JSON field name.

**Request validator tightening** -- `ValidateMenuItemsRequest` now
matches `0020` §3: `items` is `@NotEmpty @Size(max=100)`; `quantity`
is `@NotNull @Min(1) @Max(100)`.

### Test-layer fix (constructor ambiguity + Mockito greediest)

The constructor refactor broke two tests:

1. `RestaurantServiceApplicationTests.contextLoads` failed with
   `BeanInstantiationException: No default constructor found`. Root
   cause: Spring saw two constructors on `RestaurantService` and
   refused to auto-select. Fix: `@Autowired` on the 2-arg public
   constructor.
2. `RestaurantServiceTest.availability_returnsOperatingHoursAndOpenFlag`
   failed with `NullPointerException: clock is null`. Root cause:
   Mockito's `@InjectMocks` picks the greediest constructor even
   when it can't satisfy all parameters (pass null for unmocked
   fields). Fix: drop `@InjectMocks`, construct `RestaurantService`
   manually in `@BeforeEach` with `Clock.fixed(Instant.parse("2026-05-05T12:34:56Z"), ZoneOffset.UTC)`.

The fixed instant maps to `2026-05-05T15:34:56 Europe/Tallinn`
(CEST = UTC+3 in May), which is inside the `11:00-22:00` test
window, so `acceptsOrders` is deterministically `true` in the
availability test.

### Test run

Post-reshape-and-fix:

```
restaurant-service:
  RestaurantServiceApplicationTests.contextLoads ........ 1/1
  service.RestaurantServiceTest ........................ 5/5
  controller.RestaurantControllerTest .................. 12/12
  Total: 18/18 green

menu-service:
  MenuServiceApplicationTests.contextLoads ............. 1/1
  service.MenuServiceTest .............................. 10/10
  controller.MenuControllerTest ........................ 17/17
  Total: 28/28 green

Combined: 46/46, 0 failures, 0 errors.
```

Same test count as Phase 7/8 -- coverage preserved, no regressions
from the shape reshape.

### Phase 9 verification doc

`dev-docs/verification/phase-9-verification_Sierra-Lima.md` written
in the Phase 7/8 style:

- §0 session context + authorisation posture
- §1 W1 chain (Task 1)
- §2 response-shape DoD remediation (the "not drafts" bullet)
- §3 status codes (Task 2)
- §4 event contracts (Tasks 3-5)
- §5 token propagation (Task 6)
- §6 Phase 9 Outputs checklist mapped to decision-doc sections
- §7 test run evidence
- §8 open questions touched (Q5)
- Final DoD ticked

## Key decisions within Phase 9

- **Invoke the unilateral-fallback clause.** The plan blesses it,
  and `feedback_default_fallbacks.md` recorded from a prior session
  that "Accepted" is the right status-line for plan-blessed
  unilateral work (no "pending ratification" hedging).
- **Reshape code, don't just write decision docs.** The DoD's "not
  drafts" bullet would be false if `AvailabilityResponse` still
  lacked `acceptsOrders` and `checkedAt`; the session completed the
  reshape in the same pass as the decision docs so the doc+code
  pair is internally consistent at the landing commit.
- **Dual-constructor pattern over single-ctor-with-default.** The
  cleanest Spring+Mockito pattern is a `@Autowired` primary
  constructor that delegates to a package-private "test" constructor
  accepting collaborators. Tests instantiate via the package-private
  path without fighting Mockito's heuristics.
- **Restaurant-closed stays `200` with `acceptsOrders:false`.** A
  `409` would force hard-reject semantics A3 does not ask for;
  keeping it `200` lets Order Service decide user-visible behaviour
  (e.g. "queue for later"). Resolves Q5.
- **Menu validate returns `200` for per-line failures.** Menu is a
  pure classifier; Order Service consolidates per-line detail into
  a single `422 MENU_VALIDATION_FAILED`. Keeps the endpoint stable
  and lets the client show per-row annotations.
- **Token relay default, service token for compensations only.**
  Simpler than always minting; matches `0010` §4 bullets 1-3; the
  compensation-only mint is still tested by Phase 10's WireMock
  stubs.
- **No Kafka code yet.** Phase 9 only locks contracts; actual
  producer/consumer code lands in Phase 16 (or stays unbuilt if the
  Menu-events stretch is skipped). `spring-kafka` is not added to
  either POM in Phase 9.

## Commands executed during the session

```bash
# Test runs (after the DTO reshape and test-layer fix)
cd /c/MSc-Computer-Science/Semester-2/esi/2026-esi-quickbite-personal/services/restaurant-service
mvn -B test                    # 18 green
cd /c/MSc-Computer-Science/Semester-2/esi/2026-esi-quickbite-personal/services/menu-service
mvn -B test                    # 28 green

# Commit
git add <specific files>
git commit                     # -> a27a046 "Land Phase 9 team contract lock..."
```

No Docker / Postman / curl runs in this phase -- Phase 9 is entirely
docs + DTO + unit tests.

## Files changed in this session

### New decision documents (4)

- `dev-docs/decisions/0030-w1-synchronous-contract-lock.md` -- W1
  hop chain, availability + validate shapes, per-line error codes.
- `dev-docs/decisions/0031-cross-service-status-code-table.md` --
  HTTP status codes for Restaurant + Menu failures; Order outward
  mapping (advisory); retry posture.
- `dev-docs/decisions/0032-w2-w3-event-contract-lock.md` -- Topic
  list, envelope, per-type payloads, idempotency, DLQ naming.
- `dev-docs/decisions/0033-inter-service-token-propagation-lock.md`
  -- Token relay vs service token, TTL ceiling, acceptance matrix,
  rejection posture, logging rules.

### New verification document (1)

- `dev-docs/verification/phase-9-verification_Sierra-Lima.md`

### Modified (10)

- `dev-docs/decisions/0004-open-questions.md` -- Q5 marked Resolved
  by 0031.
- `services/restaurant-service/src/main/java/.../dto/AvailabilityResponse.java`
  -- added `acceptsOrders`, `checkedAt`; `@JsonInclude(NON_NULL)`.
- `services/restaurant-service/src/main/java/.../service/RestaurantService.java`
  -- Clock injection via dual constructor; `isWithinOperatingHours`
  helper; `availability(...)` computes `acceptsOrders` + `checkedAt`.
- `services/restaurant-service/src/test/java/.../controller/RestaurantControllerTest.java`
  -- assertions on the three added availability fields.
- `services/restaurant-service/src/test/java/.../service/RestaurantServiceTest.java`
  -- dropped `@InjectMocks`; manual construction with fixed Clock.
- `services/menu-service/src/main/java/.../dto/ValidateMenuItemsRequest.java`
  -- `@Size(max=100)`, `@Min(1)`/`@Max(100)` on quantity.
- `services/menu-service/src/main/java/.../dto/ValidateMenuItemsResponse.java`
  -- reshape: `items` instead of `results`; added `totalAmount` +
  `currency`; enum constants; `@JsonProperty("isAvailable")`.
- `services/menu-service/src/main/java/.../service/MenuService.java`
  -- uses enum constants; computes `totalAmount` + `currency`;
  null-safe sum via `Objects::nonNull`.
- `services/menu-service/src/test/java/.../controller/MenuControllerTest.java`
  -- assertions updated to the new top-level shape.
- `services/menu-service/src/test/java/.../service/MenuServiceTest.java`
  -- `.items()` / `.isAvailable()` / `.lineTotal()` / `.error()`
  renames; total + currency expectations.

Plus this session archive (added in a follow-up commit).

## Pre-existing context reused

- **`0020-sierra-lima-contracts.md`** already froze the shapes and
  validators. Phase 9 surfaced that code had drifted; remediation
  brought code back in line with `0020`.
- **`0010-auth-contract.md` §4 and §8** defined token modes and
  route-protection already; Phase 9 extended §4 into per-hop default
  posture in `0033`.
- **Phase 7 auth filter** (`JwtAuthFilter`, `JwtDevMint`,
  `CurrentUser`, ±30s clock skew) was exactly the posture `0033`
  ratified; no code change was required there.
- **Phase 7 error envelope** (`GlobalExceptionHandler`,
  `ErrorResponse`) was the shape `0031` §4 referenced; no code
  change there either.
- **Phase 7/8 test scaffolding** (JUnit 5, `@SpringBootTest`,
  `@AutoConfigureMockMvc`, `MockMvc`, per-role `JwtDevMint` tokens)
  supported the shape assertions added in Phase 9 with no framework
  changes.

## State at session end

- Landing commit `a27a046` on `dev`, 1 commit ahead of `origin/dev`.
- 46/46 tests green across both services.
- Four new `Accepted` decision documents publicly pinning the W1 /
  W2 / W3 contracts from Sierra-Lima's perspective.
- Q5 of open-questions log closed.
- DTO shapes, service-layer behaviour, and unit tests all aligned
  against `0030` §3 / §4 / §5 -- "not drafts" in force.
- Phase 10 (W1 integration with WireMock stubs) is unblocked on the
  Sierra-Lima side.

## Phase 9 Definition of Done

- [x] Every teammate integrates against written contracts, not chat
      memory. Four decision docs committed on `dev`; each lists the
      supersession path for the callsign-advisory rows.
- [x] Sierra-Lima's availability and batch-validation response
      shapes are the ones Order Service will call -- not drafts.
      Committed code at `a27a046` matches `0030` §3 / §4 / §5
      verbatim; 46/46 tests assert the new fields.
