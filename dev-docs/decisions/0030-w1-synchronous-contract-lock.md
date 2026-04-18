# 0030 -- W1 Synchronous Call-Chain Contract Lock

- **Status:** Accepted
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `5bd6f45`
- **Source:**
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §9
  Phase 9 Tasks 1-2 and Appendix F.6; prior freezes in
  [`0010-auth-contract.md`](./0010-auth-contract.md) and
  [`0020-sierra-lima-contracts.md`](./0020-sierra-lima-contracts.md).

## Context

W1 is the Place-Order synchronous demo. Order Service drives a chain
of REST calls against four of the seven business services. Sierra-Lima
is a callee twice: availability on Restaurant, batch validate on Menu.

Phase 9 exists so every teammate integrates against written JSON and
status codes, not chat memory. The plan's Team-dependency note allows
Sierra-Lima to lock the contract unilaterally when the other callsigns
are unavailable; this document invokes that clause. If Alfa-Kilo,
Elephant-Yankee, or Mike-Alfa pushes back on a specific row later, a
superseding decision replaces only that row -- Sierra-Lima's DTOs stay
written against the shapes below.

The `0020` contract pack froze the shapes Order Service will call. This
document does three further things `0020` left open: it (a) lists the
whole W1 hop chain, not just Sierra-Lima's two endpoints, (b) states
which fields are authoritative vs. advisory for each hop, and (c)
confirms -- against the real committed code -- that the Sierra-Lima
shapes are no longer drafts.

## Decision

### 1. W1 hop chain (frozen order)

Lifted verbatim from Appendix F.6 of the master plan. Each hop carries
`Authorization: Bearer <token>` per [`0010`](./0010-auth-contract.md) §4
unless otherwise noted.

| # | From | To | Call | Purpose |
|---|------|------|------|---------|
| 1 | Client | API Gateway | `POST /api/orders` | Place-order entry |
| 2 | Gateway | Order Service | `POST /orders` (after `/api` strip) | Gateway validates token, forwards |
| 3 | Order | User Service | `GET /users/{customerId}` (or equivalent) | Customer lookup (verify exists + active) |
| 4 | Order | **Restaurant Service** | **`GET /restaurants/{id}/availability`** | Accept-orders check (§3 below) |
| 5 | Order | **Menu Service** | **`POST /menu-items/validate`** | Existence, availability, pricing (§4 below) |
| 6 | Order | (local) | Persist `Order` row with status `Placed` | -- |
| 7 | Order | Payment Service | `POST /payments` with `{orderId, amount}` | Synchronous charge |
| 8 | Order | Delivery Service | `POST /deliveries` with `{orderId, pickup, dropoff}` | On `Completed` payment |
| 9 | Order | Client | `201 Created` with `{orderId, status}` | Terminal response |

Sierra-Lima owns hops 4 and 5. Hops 3, 7, 8 are authoritative for the
owning teammate's service; Sierra-Lima writes against them as documented
here for planning and test stubbing only.

### 2. Order Service entry payload (hop 2, advisory for Sierra-Lima)

Sierra-Lima does not implement this; it is recorded so the team agrees
what the end-user sends.

**Request body:**

```json
{
  "customerId": "00000000-0000-0000-0000-000000000001",
  "restaurantId": "d0000001-0000-0000-0000-000000000000",
  "items": [
    { "menuItemId": "e0000011-0000-0000-0000-000000000000", "quantity": 2 },
    { "menuItemId": "e0000012-0000-0000-0000-000000000000", "quantity": 1 }
  ],
  "deliveryAddress": {
    "street": "Raekoja plats 10",
    "city": "Tartu",
    "postalCode": "51003"
  }
}
```

**Success response (`201 Created`):**

```json
{
  "orderId": "f0000000-0000-0000-0000-000000000001",
  "status": "Confirmed",
  "placedAt": "2026-05-05T12:34:56Z",
  "totalAmount": "25.00",
  "currency": "EUR"
}
```

Alfa-Kilo may extend the response with further Order-Service fields; the
five listed are the ones every other service consumes.

### 3. `GET /restaurants/{id}/availability` (hop 4, Sierra-Lima authoritative)

**Request:** no body.  
**Auth:** any valid bearer token (`Customer`, `RestaurantOwner`, `Driver`,
`Admin`, or `SERVICE`) per [`0010`](./0010-auth-contract.md) §8.

**Success response (`200 OK`):**

```json
{
  "restaurantId": "d0000001-0000-0000-0000-000000000000",
  "isOpen": true,
  "acceptsOrders": true,
  "operatingHours": "11:00-22:00",
  "checkedAt": "2026-05-05T12:34:56Z"
}
```

Field contract:

| Field | Type | Meaning |
|-------|------|---------|
| `restaurantId` | UUID | Echoes the path parameter. |
| `isOpen` | boolean | Stored `Restaurant.isOpen` flag. |
| `acceptsOrders` | boolean | `true` iff `isOpen` AND server local time is within `operatingHours`. |
| `operatingHours` | String | `HH:MM-HH:MM` window in `Europe/Tallinn` local time. |
| `checkedAt` | ISO-8601 UTC Instant | Instant the decision was made; lets Order log a monotonic integration time. |

Order Service MUST treat `acceptsOrders:false` as the rejection signal,
not `isOpen:false`. Hours can move the window even if the flag says
open -- see `0031` §2 for status-code treatment.

**Error response (`404 Not Found`):** standard error envelope per
`0020` §7 with `"error": "Not Found"`.

### 4. `POST /menu-items/validate` (hop 5, Sierra-Lima authoritative)

**Auth:** any valid bearer token per [`0010`](./0010-auth-contract.md) §8.

**Request body:**

```json
{
  "items": [
    { "menuItemId": "e0000011-0000-0000-0000-000000000000", "quantity": 2 },
    { "menuItemId": "e0000012-0000-0000-0000-000000000000", "quantity": 1 }
  ]
}
```

Validation rules (from `0020` §3): `items` non-empty, max 100 entries;
each `menuItemId` non-null; each `quantity >= 1` and `<= 100`. A
malformed request returns `400` with validation details, not `422`.

**Success response (`200 OK`):**

```json
{
  "allValid": false,
  "items": [
    {
      "menuItemId": "e0000011-0000-0000-0000-000000000000",
      "exists": true,
      "isAvailable": true,
      "unitPriceAmount": "8.50",
      "unitPriceCurrency": "EUR",
      "quantity": 2,
      "lineTotal": "17.00"
    },
    {
      "menuItemId": "e0000012-0000-0000-0000-000000000000",
      "exists": false,
      "quantity": 1,
      "error": "MENU_ITEM_NOT_FOUND"
    }
  ],
  "totalAmount": "17.00",
  "currency": "EUR"
}
```

Field contract:

| Field | Type | Meaning |
|-------|------|---------|
| `allValid` | boolean | `true` iff every line `exists && isAvailable`. |
| `items[]` | array | Per-request-line result, in the same order Order submitted. |
| `items[].menuItemId` | UUID | Echoes the request line's id. |
| `items[].quantity` | int | Echoes the request line's quantity. |
| `items[].exists` | boolean | Did a menu row match the id? |
| `items[].isAvailable` | boolean | `MenuItem.isAvailable` flag (only meaningful if `exists`). |
| `items[].unitPriceAmount` | BigDecimal (scale 2) | Unit price, present when `exists`. |
| `items[].unitPriceCurrency` | String (ISO-4217) | Unit-price currency, present when `exists`. |
| `items[].lineTotal` | BigDecimal (scale 2) | `unitPriceAmount * quantity`, present only when valid. |
| `items[].error` | enum String | Reason code when the line is not valid. See §5. |
| `totalAmount` | BigDecimal (scale 2) | Sum of `lineTotal` over valid lines. |
| `currency` | String (ISO-4217) | Currency for `totalAmount`. Always `"EUR"` in the A3 subset. |

Unused fields on a given line are omitted from the JSON
(`@JsonInclude(NON_NULL)`).

### 5. Per-line error codes (Menu validate)

Enum string values Menu returns in `items[].error`. Order Service keys
off these exact strings for its own error mapping.

| Code | Meaning |
|------|---------|
| `MENU_ITEM_NOT_FOUND` | No row exists for `menuItemId`. |
| `MENU_ITEM_NOT_AVAILABLE` | Row exists but `isAvailable == false`. |

Future codes (e.g. `QUANTITY_EXCEEDS_STOCK`) are out of scope for A3 and
will be added by a superseding decision, not by silent expansion.

### 6. Payment and Delivery hop shapes (advisory for Sierra-Lima)

Locked for integration-testing purposes so Order Service has a written
target even before Elephant-Yankee ships code. The owning teammate's
future decision document supersedes what is written here.

**`POST /payments`** request:

```json
{
  "orderId": "f0000000-0000-0000-0000-000000000001",
  "amount": "25.00",
  "currency": "EUR"
}
```

**`POST /payments`** success response (`201 Created`):

```json
{
  "paymentId": "a0000000-0000-0000-0000-000000000001",
  "orderId": "f0000000-0000-0000-0000-000000000001",
  "status": "Completed",
  "amount": "25.00",
  "currency": "EUR",
  "processedAt": "2026-05-05T12:34:57Z"
}
```

`status` enum: `Completed` | `Failed`. A `Failed` payment still returns
`201` with the record -- the declined-payment rejection is signalled
via `status`, not HTTP (so that Order can persist the attempt before
deciding). Declined means hop 8 is skipped and the order becomes
`Cancelled`; the compensating refund path in Appendix F.6 step 8 uses
`POST /payments/{id}/refund` with an empty body.

**`POST /deliveries`** request:

```json
{
  "orderId": "f0000000-0000-0000-0000-000000000001",
  "pickup": {
    "restaurantId": "d0000001-0000-0000-0000-000000000000",
    "street": "Rüütli 12",
    "city": "Tartu"
  },
  "dropoff": {
    "street": "Raekoja plats 10",
    "city": "Tartu",
    "postalCode": "51003"
  }
}
```

**`POST /deliveries`** success response (`201 Created`):

```json
{
  "deliveryId": "b0000000-0000-0000-0000-000000000001",
  "orderId": "f0000000-0000-0000-0000-000000000001",
  "status": "Assigned",
  "assignedAt": "2026-05-05T12:34:58Z"
}
```

`status` enum: `Pending` | `Assigned` | `PickedUp` | `Delivered` |
`Failed`. Only `Assigned` or `Pending` is expected at response time;
downstream transitions are reported via the `delivery.status-changed`
event (see [`0032`](./0032-w2-w3-event-contract-lock.md)).

### 7. Ordering and idempotency

- Order Service MUST call hop 4 before hop 5. Menu may reject a valid
  item if the restaurant is later patched closed, but sequence is
  predictable for the failure mapping in `0031`.
- Menu's `POST /menu-items/validate` is a **pure read** -- no row is
  mutated. Order may re-call it without an idempotency key.
- Restaurant's `GET /restaurants/{id}/availability` is a pure read.
- Payment's `POST /payments` MUST be idempotent on `orderId` per
  Elephant-Yankee's future decision. Pending that, Order Service treats
  duplicate `orderId` as a client bug and does not retry at this layer.

### 8. Inter-service authorisation posture

Every hop in §1 carries a bearer token. Both modes from
[`0010`](./0010-auth-contract.md) §4 are allowed:

- **Token relay** -- Order forwards the end-user token. Default.
- **Service token** -- Order mints a `tokenType:SERVICE` token when no
  end-user token is available (compensations, scheduled retries).

The exact posture Order Service takes by default is locked in
[`0033`](./0033-inter-service-token-propagation-lock.md).

## Team alignment note

Sierra-Lima authoritative: §3, §4, §5. These are written against the
code currently on `dev` at `5bd6f45` as of the Phase 9 session.

Advisory (Sierra-Lima will not implement, other callsigns own the
final word): §2, §6. The rows are frozen only as a best reading of
A3 Appendix F.6 so Order Service can be coded against something
concrete; a later decision document from Alfa-Kilo, Elephant-Yankee,
or Mike-Alfa supersedes the advisory rows piece-by-piece.

## Consequences

- Resolves Phase 9 Tasks 1 and 2 insofar as Sierra-Lima can resolve
  them unilaterally. Full sign-off from Alfa-Kilo and Elephant-Yankee
  remains useful but is no longer a blocker on Sierra-Lima's Phase
  10 integration work.
- The divergence between code at `5bd6f45` and `0020` §1.6 / §2.6 that
  was discovered during this Phase 9 session (availability missing
  `acceptsOrders`/`checkedAt`; validate using `results`/`available`/
  `lineTotalAmount`/`reason` and missing top-level `totalAmount`/
  `currency`) is remediated in the same Phase 9 session by reshaping
  `AvailabilityResponse` and `ValidateMenuItemsResponse` plus their
  service layers and tests to the shapes in §3, §4, and §5 here.
  Phase 9 verification records the test run.
- Status-code mapping for the §3 and §4 failure modes is locked in
  [`0031`](./0031-cross-service-status-code-table.md).
- Event-layer obligations (what Order Service publishes or consumes
  after W1 terminates) are locked in
  [`0032`](./0032-w2-w3-event-contract-lock.md).

## Supersedes

None directly. Overlaps with `0020-sierra-lima-contracts.md` §1.6 and
§2.6; where this document adds or renames a field, the locked shape
here is the one Order Service will call.
