# Phase 9 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 9
("Team Contract Lock for W1 / W2 / W3") for Restaurant Service and
Menu Service.

Date: 2026-04-18. Target CP#1 consultation: 2026-04-28.

---

## 0. Session context

Phase 9 is coordination-heavy and nominally requires all four
callsigns. Alfa-Kilo (User / Order / Gateway), Elephant-Yankee
(Payment / Delivery), and Mike-Alfa (Notification / Kafka) were
unavailable at the time of this session. The user (Sierra-Lima)
invoked the master plan's unilateral-fallback clause and asked
Charlie-Lima-Alfa to lock the contracts on everyone's behalf with
best-reading defaults from Assignment 3 Appendix F. Each other
callsign can supersede an advisory row later via a new numbered
decision; Sierra-Lima's own rows (§3, §4, §5 of `0030` and the
Sierra-Lima columns of `0031`, `0033`) are written against committed
code and are no longer drafts.

Four new decision documents are added in this session:

| Doc | Subject |
|-----|---------|
| [`0030-w1-synchronous-contract-lock.md`](../decisions/0030-w1-synchronous-contract-lock.md) | W1 hop chain, availability + validate response shapes, error enum codes |
| [`0031-cross-service-status-code-table.md`](../decisions/0031-cross-service-status-code-table.md) | HTTP status table for Restaurant + Menu failures; Order outward mapping (advisory) |
| [`0032-w2-w3-event-contract-lock.md`](../decisions/0032-w2-w3-event-contract-lock.md) | Topic list, envelope `{id,type,occurredAt,payload}`, DLQ naming, idempotency |
| [`0033-inter-service-token-propagation-lock.md`](../decisions/0033-inter-service-token-propagation-lock.md) | Token relay vs service token per hop, TTL ceiling, rejection posture |

---

## 1. W1 synchronous call chain locked (Task 1)

Frozen in [`0030`](../decisions/0030-w1-synchronous-contract-lock.md) §1,
lifted verbatim from Appendix F.6 of the master plan:

| # | From | To | Call | Owner |
|---|------|------|------|-------|
| 1-2 | Client / Gateway | Order | `POST /api/orders` -> `POST /orders` | Alfa-Kilo |
| 3 | Order | User | `GET /users/{customerId}` | Alfa-Kilo |
| 4 | Order | **Restaurant** | **`GET /restaurants/{id}/availability`** | **Sierra-Lima** |
| 5 | Order | **Menu** | **`POST /menu-items/validate`** | **Sierra-Lima** |
| 6 | Order | (local) | Persist `Order` row `Placed` | Alfa-Kilo |
| 7 | Order | Payment | `POST /payments` | Elephant-Yankee |
| 8 | Order | Delivery | `POST /deliveries` | Elephant-Yankee |
| 9 | Order | Client | `201 Created` | Alfa-Kilo |

Sierra-Lima is authoritative on hops 4 and 5. The other rows are
locked against `0030` as advisory best-readings; the owning callsign
supersedes via a later decision.

---

## 2. Sierra-Lima response shapes no longer drafts (Task 1, DoD bullet 2)

The master plan's Phase 9 DoD explicitly requires that Sierra-Lima's
availability and batch-validation response shapes be "the ones Order
Service will call -- not drafts". A divergence was discovered during
this session between `dev` at commit `5bd6f45` and the shapes frozen
in `0020-sierra-lima-contracts.md` §1.6 / §2.6. The session
remediates it in the same pass so the DoD is real, not aspirational.

### 2.1 `AvailabilityResponse` (Restaurant Service)

Committed shape now matches `0030` §3 verbatim:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityResponse(
    UUID restaurantId,
    boolean isOpen,
    boolean acceptsOrders,   // <-- added in Phase 9
    String operatingHours,
    Instant checkedAt        // <-- added in Phase 9
) {}
```

File: `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/AvailabilityResponse.java`.

Service-layer change in `RestaurantService.availability(...)`:

- Clock is now injected (`java.time.Clock`, defaults to
  `Clock.system(ZoneId.of("Europe/Tallinn"))`) so `checkedAt` is
  produced deterministically in tests.
- `acceptsOrders` is computed as `isOpen && isWithinOperatingHours()`
  with a tolerant parser for `"HH:MM-HH:MM"` (returns `true` on
  malformed or absent hours rather than silently blocking orders).
- `isWithinOperatingHours` honours overnight windows where `end <
  start` (e.g. `"22:00-02:00"`).

Two constructors exist: a Spring-injected 2-arg public constructor
(`@Autowired`), and a package-private 3-arg test constructor accepting
a `Clock`. `RestaurantServiceTest` uses the latter with a
`Clock.fixed(Instant.parse("2026-05-05T12:34:56Z"), ZoneOffset.UTC)`.

### 2.2 `ValidateMenuItemsResponse` (Menu Service)

Committed shape now matches `0030` §4 verbatim:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidateMenuItemsResponse(
    boolean allValid,
    List<Line> items,          // <-- renamed from results
    BigDecimal totalAmount,    // <-- added in Phase 9
    String currency            // <-- added in Phase 9
) {
    public static final String ERROR_NOT_FOUND = "MENU_ITEM_NOT_FOUND";
    public static final String ERROR_NOT_AVAILABLE = "MENU_ITEM_NOT_AVAILABLE";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Line(
        UUID menuItemId,
        int quantity,
        boolean exists,
        @JsonProperty("isAvailable") boolean isAvailable,
        BigDecimal unitPriceAmount,
        String unitPriceCurrency,
        BigDecimal lineTotal,   // <-- renamed from lineTotalAmount
        String error            // <-- renamed from reason, enum values per 0030 §5
    ) {}
}
```

File: `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/dto/ValidateMenuItemsResponse.java`.

Corresponding changes in `MenuService.validate(...)`:

- Per-line missing -> `error = MENU_ITEM_NOT_FOUND` (from enum
  constant).
- Per-line row exists but `!isAvailable` -> `error =
  MENU_ITEM_NOT_AVAILABLE`.
- `totalAmount` is the sum of `lineTotal` values from valid lines
  (null-safe via `Objects::nonNull`).
- `currency` defaults to `"EUR"` when no valid line is present.
- `@JsonInclude(NON_NULL)` keeps the JSON clean: `lineTotal` / `error`
  / `unitPriceAmount` only appear when applicable.

### 2.3 Request validator tightening

`ValidateMenuItemsRequest` now enforces `0020` §3 exactly:

- `items`: `@NotEmpty`, `@Size(max = 100)`.
- `menuItemId`: `@NotNull`.
- `quantity`: `@NotNull`, `@Min(1)`, `@Max(100)`.

A request with >100 items now returns `400` with a
`validationErrors[].field = "items"` block rather than slipping through
to the service layer.

---

## 3. Status codes locked (Task 2)

Frozen in [`0031`](../decisions/0031-cross-service-status-code-table.md).
Sierra-Lima's rows (authoritative):

| Condition | HTTP | Body summary |
|-----------|------|--------------|
| Restaurant exists, accepting | `200` | `acceptsOrders:true` |
| Restaurant exists, `isOpen=false` | `200` | `acceptsOrders:false` |
| Restaurant exists, outside hours | `200` | `acceptsOrders:false` |
| Restaurant not found | `404` | Error envelope |
| Validate: all lines valid | `200` | `allValid:true` |
| Validate: some lines invalid | `200` | `allValid:false`, per-line `error` |
| Validate: malformed request (`400` validation) | `400` | `validationErrors[]` |
| Validate: >100 items | `400` | `validationErrors[]` |
| Any endpoint: no / bad token | `401` | Error envelope |
| Any endpoint: wrong role | `403` | Error envelope |

Restaurant-closed signal is `200` + `acceptsOrders:false`, not `409`.
This resolves Q5 of `0004-open-questions.md` and was set as the
current lean there; it is now formally locked.

Order Service's outward end-user mapping (advisory) is recorded in
`0031` §3 so Phase 10 failure-path tests can be written today.

---

## 4. Event contracts locked (Tasks 3-5)

Frozen in [`0032`](../decisions/0032-w2-w3-event-contract-lock.md).

Topics (§1):

| Topic | Producer | Required |
|-------|----------|----------|
| `payment-events` | Payment | **Yes** |
| `delivery-events` | Delivery | **Yes** |
| `order-events` | Order | **Yes** (compensating) |
| `menu-events` | Menu | Optional (Phase 16 stretch) |

Envelope (§2) is the shared example requested by the master plan's
Phase 9 Outputs list:

```json
{
  "id": "3e28a4c0-0000-0000-0000-00000000abcd",
  "type": "payment.completed",
  "occurredAt": "2026-05-05T12:34:58.123Z",
  "payload": { "orderId": "f0000000-...", "amount": "25.00", "currency": "EUR" }
}
```

`id` is the idempotency key; consumers dedupe on it (Task 5). At-least-
once delivery. DLQ naming is `<topic>.dlq`, documented in §8 of `0032`.

Sierra-Lima produces no Kafka traffic in the A3 baseline; the
`menu-events` row is opt-in for Phase 16. No `spring-kafka` is added
to either POM in Phase 9.

---

## 5. Token propagation locked (Task 6)

Frozen in [`0033`](../decisions/0033-inter-service-token-propagation-lock.md).

Default is **token relay**: Order forwards the inbound user token
unchanged on every W1 hop. Service tokens are used only for
compensations (8c refund) and future scheduled work.

Sierra-Lima's acceptance matrix (§4):

- All bearer roles (`Customer`, `RestaurantOwner`, `Admin`, `Driver`,
  `SERVICE`) accepted on the two internal endpoints (§3 of `0030`).
- `SERVICE` tokens **never** unlock mutation endpoints (§4 bold cell).
- TTL ≤ 300 s on service tokens, ±30 s clock skew, full-JWT logging
  forbidden.
- `401` on missing/malformed/expired token; `403` only after
  successful parse when role check fails.

These rules match Sierra-Lima's `JwtAuthFilter` at `5bd6f45`: no code
change was required in Phase 9 beyond the DTO reshape tracked by §2
above.

---

## 6. Contract-sheet completeness (Phase 9 Outputs checklist)

The master plan lists four output artefacts. Where each is recorded:

| Phase 9 output | Location |
|----------------|----------|
| Contract sheet for W1 | `0030` §1 (hop table), §3-§4 (payloads), §6 (advisory Payment/Delivery) |
| Event contract sheet for W2 and W3 | `0032` §1 (topics), §3-§5 (payloads) |
| Shared event-envelope example | `0032` §2 + verbatim JSON in §3 |
| Status-code table for cross-service errors | `0031` |

The Sierra-Lima columns in each table reflect committed code at this
session's base commit (HEAD post-reshape) and are verified by the
test run in §7.

---

## 7. Test run (DoD check)

### 7.1 Restaurant Service

```
$ mvn -B test
...
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  -- RestaurantServiceApplicationTests.contextLoads
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
  -- service.RestaurantServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
  -- controller.RestaurantControllerTest
[INFO] Results:
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Test updates in this phase:

- `RestaurantServiceTest` drops `@InjectMocks`, instantiates
  `RestaurantService` directly with a fixed `Clock`, removes the
  dependency on Mockito's greediest-constructor heuristic.
- `RestaurantControllerTest.availability_succeedsWithCustomerToken`
  asserts the three added fields (`acceptsOrders`, `operatingHours`,
  `checkedAt`).

### 7.2 Menu Service

```
$ mvn -B test
...
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  -- MenuServiceApplicationTests.contextLoads
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
  -- service.MenuServiceTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
  -- controller.MenuControllerTest
[INFO] Results:
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Test updates:

- `MenuServiceTest.validate_*` uses the new enum constants
  (`MENU_ITEM_NOT_FOUND`, `MENU_ITEM_NOT_AVAILABLE`), asserts
  `items()` (renamed from `results()`) and adds `totalAmount` /
  `currency` expectations (`13.00 EUR` and `17.00 EUR` on the two
  validate scenarios).
- `MenuControllerTest.validate_succeedsWithCustomerToken` and related
  tests assert the new top-level `items` array and `currency` field.

### 7.3 Combined total

46 tests across both services (unchanged count from Phase 7/8; the
shape reshape was absorbed without losing coverage).

---

## 8. Open questions touched

- **Q5 (`0004-open-questions.md`)** -- Resolved by `0031`. Restaurant-
  closed signalling is `200` with `acceptsOrders:false`. The
  `_Pending._` line in `0004` is replaced in the same commit as this
  verification doc.

Other open questions (Q3 HATEOAS, Q7 pagination, Q9 Testcontainers)
are unrelated to Phase 9 and remain pending at their original target
phases.

---

## Definition of Done (Phase 9)

- [x] Every teammate integrates against written contracts, not chat
      memory. Contracts live in `0030`, `0031`, `0032`, `0033`;
      committed on the `dev` branch in this session.
- [x] Sierra-Lima's availability and batch-validation response shapes
      are the ones Order Service will call -- not drafts. Committed
      code at HEAD matches `0030` §3 / §4 / §5; tests assert the new
      fields; 46/46 tests green across both services.
