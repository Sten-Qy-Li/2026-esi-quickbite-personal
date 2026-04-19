# 0020 -- Sierra-Lima Service Contracts

- **Status:** Accepted
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `fd70496`
- **Source:**
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §9
  Phase 2 Part A, and Appendix F.1-F.4.

## Context

Phase 2 Part A of the master plan asks Sierra-Lima to freeze the exact
REST endpoints, request/response JSON payloads, validation rules, DB
schemas, and seed data **before** writing any business code. A single
canonical contract reference reduces rework in Phases 3-6 and in the
W1 integration (Phase 10). This document is that canonical reference.

## 1. Restaurant Service Endpoints (six)

Copied from master plan Appendix F.1.

| Method | Path | Purpose | Auth (per `0010`) |
|--------|------|---------|-------------------|
| `POST` | `/restaurants` | Register a new restaurant profile | `RestaurantOwner` / `Admin` |
| `GET` | `/restaurants/{id}` | Get a restaurant profile | Public |
| `PUT` | `/restaurants/{id}` | Update profile (hours, location, details) | Owner / `Admin` |
| `PATCH` | `/restaurants/{id}/status` | Toggle open/closed | Owner / `Admin` |
| `GET` | `/restaurants` | Search/list (filters: `city`, `isOpen`) | Public |
| `GET` | `/restaurants/{id}/availability` | Availability check for W1 | Any valid token / `SERVICE` |

### 1.1 `POST /restaurants`

**Request:**

```json
{
  "name": "Pizza Antonio",
  "address": "Rüütli 12",
  "city": "Tartu",
  "latitude": 58.3776,
  "longitude": 26.7290,
  "operatingHours": "11:00-22:00"
}
```

**Response (`201 Created`):**

```json
{
  "restaurantId": "4f4a2c9d-19b1-4d61-9b9b-7e6e3d4a1111",
  "ownerId": "00000000-0000-0000-0000-000000000001",
  "name": "Pizza Antonio",
  "address": "Rüütli 12",
  "city": "Tartu",
  "latitude": 58.3776,
  "longitude": 26.7290,
  "operatingHours": "11:00-22:00",
  "isOpen": false,
  "createdAt": "2026-04-20T10:00:00Z",
  "updatedAt": "2026-04-20T10:00:00Z"
}
```

### 1.2 `GET /restaurants/{id}`

**Response (`200 OK`):** same body as §1.1 response.

**Error (`404 Not Found`):** error envelope per §7 with `"error": "Not Found"`.

### 1.3 `PUT /restaurants/{id}`

**Request:** same shape as §1.1 request (all fields replaced).

**Response (`200 OK`):** same body as §1.1 response (the updated
resource). Returning the body avoids a second round-trip by the
frontend's edit view and keeps PUT and POST symmetric.

### 1.4 `PATCH /restaurants/{id}/status`

**Request:**

```json
{ "isOpen": true }
```

**Response (`200 OK`):** same body as §1.1 response (the updated
resource with the new `isOpen` value). Returning the body lets the
owner dashboard refresh its badge without a follow-up `GET`.

### 1.5 `GET /restaurants`

**Query params:** `city` (String, optional), `isOpen` (Boolean, optional),
`page` (int, default 0), `size` (int, default 20), `sort` (String,
default `"name,asc"`).

**Response (`200 OK`):**

```json
{
  "content": [
    {
      "restaurantId": "4f4a2c9d-19b1-4d61-9b9b-7e6e3d4a1111",
      "name": "Pizza Antonio",
      "city": "Tartu",
      "isOpen": true,
      "operatingHours": "11:00-22:00"
    }
  ],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20,
  "first": true,
  "last": true,
  "empty": false
}
```

### 1.6 `GET /restaurants/{id}/availability`

**Response (`200 OK`):**

```json
{
  "restaurantId": "4f4a2c9d-19b1-4d61-9b9b-7e6e3d4a1111",
  "isOpen": true,
  "acceptsOrders": true,
  "operatingHours": "11:00-22:00",
  "checkedAt": "2026-05-05T12:34:56Z"
}
```

`acceptsOrders` is `true` iff `isOpen` is true and the current local
time is within `operatingHours`.

## 2. Menu Service Endpoints (six)

Copied from master plan Appendix F.2.

| Method | Path | Purpose | Auth (per `0010`) |
|--------|------|---------|-------------------|
| `POST` | `/restaurants/{rid}/menu-items` | Add a new menu item | Owner of `rid` / `Admin` |
| `GET` | `/restaurants/{rid}/menu-items` | List items for a restaurant (filters: `category`, `available`) | Public |
| `GET` | `/menu-items/{id}` | Get a single item | Public |
| `PUT` | `/menu-items/{id}` | Update item | Owner / `Admin` |
| `DELETE` | `/menu-items/{id}` | Remove item | Owner / `Admin` |
| `POST` | `/menu-items/validate` | Batch validate items + prices (W1) | Any valid token / `SERVICE` |

### 2.1 `POST /restaurants/{rid}/menu-items`

**Request:**

```json
{
  "name": "Margherita",
  "description": "Tomato, mozzarella, basil.",
  "priceAmount": "8.50",
  "priceCurrency": "EUR",
  "category": "Main",
  "isAvailable": true
}
```

**Response (`201 Created`):**

```json
{
  "menuItemId": "11111111-2222-3333-4444-555555555555",
  "restaurantId": "4f4a2c9d-19b1-4d61-9b9b-7e6e3d4a1111",
  "name": "Margherita",
  "description": "Tomato, mozzarella, basil.",
  "priceAmount": "8.50",
  "priceCurrency": "EUR",
  "category": "Main",
  "isAvailable": true,
  "createdAt": "2026-04-20T10:05:00Z",
  "updatedAt": "2026-04-20T10:05:00Z"
}
```

### 2.2 `GET /restaurants/{rid}/menu-items`

**Query params:** `category` (String, optional), `available` (Boolean,
optional).

**Response (`200 OK`):**

```json
[
  {
    "menuItemId": "11111111-2222-3333-4444-555555555555",
    "restaurantId": "4f4a2c9d-19b1-4d61-9b9b-7e6e3d4a1111",
    "name": "Margherita",
    "priceAmount": "8.50",
    "priceCurrency": "EUR",
    "category": "Main",
    "isAvailable": true
  }
]
```

### 2.3 `GET /menu-items/{id}`

**Response (`200 OK`):** same shape as §2.1 response.

**Error (`404 Not Found`):** error envelope per §7.

### 2.4 `PUT /menu-items/{id}`

**Request:** same shape as §2.1 request.

**Response (`200 OK`):** same body as §2.1 response (the updated menu
item). Returning the body lets the menu-item detail view refresh the
price/availability badges in place.

### 2.5 `DELETE /menu-items/{id}`

**Response (`204 No Content`):** empty body.

### 2.6 `POST /menu-items/validate`

**Request:**

```json
{
  "items": [
    { "menuItemId": "11111111-2222-3333-4444-555555555555", "quantity": 2 },
    { "menuItemId": "99999999-aaaa-bbbb-cccc-dddddddddddd", "quantity": 1 }
  ]
}
```

**Response (`200 OK`):**

```json
{
  "allValid": false,
  "items": [
    {
      "menuItemId": "11111111-2222-3333-4444-555555555555",
      "exists": true,
      "isAvailable": true,
      "unitPriceAmount": "8.50",
      "unitPriceCurrency": "EUR",
      "quantity": 2,
      "lineTotal": "17.00"
    },
    {
      "menuItemId": "99999999-aaaa-bbbb-cccc-dddddddddddd",
      "exists": false,
      "error": "MENU_ITEM_NOT_FOUND"
    }
  ],
  "totalAmount": "17.00",
  "currency": "EUR"
}
```

Notes:

- `totalAmount` sums only the valid lines. Order Service treats
  `allValid: false` as a hard reject (step 5 failure in W1, returns
  `422`).
- If items have mixed currencies, that is treated as a validation error
  on the whole request (`400`) -- orders with mixed currencies are out
  of scope.

## 3. Validation Rules

**Restaurant Service (enforced in `CreateRestaurantRequest` /
`UpdateRestaurantRequest`):**

- `name`: `@NotBlank`, `@Size(max=255)`.
- `address`: `@Size(max=255)` (nullable).
- `city`: `@NotBlank`, `@Size(max=120)`.
- `latitude`: `@DecimalMin("-90.0")`, `@DecimalMax("90.0")`.
- `longitude`: `@DecimalMin("-180.0")`, `@DecimalMax("180.0")`.
- `operatingHours`: `@Pattern("^(?:[01][0-9]|2[0-3]):[0-5][0-9]-(?:[01][0-9]|2[0-3]):[0-5][0-9]$")`.
  Hours are strictly `00-23`; values such as `24:00` and `29:59` are invalid.

**Menu Service (enforced in `CreateMenuItemRequest` /
`UpdateMenuItemRequest`):**

- `name`: `@NotBlank`, `@Size(max=255)`.
- `description`: `@Size(max=2000)` (nullable).
- `priceAmount`: `@NotNull`, `@Positive`, `@Digits(integer=17,fraction=2)`
  (`BigDecimal`). The `integer=17` cap mirrors the `NUMERIC(19,2)`
  column in §4.2.
- `priceCurrency`: `@NotBlank`, `@Pattern("^[A-Z]{3}$")` (ISO-4217).
- `category`: `@NotBlank`, `@Size(max=100)`.
- `isAvailable`: optional on `POST` -- when omitted the service defaults
  it to `true` (the common case for a newly listed item). `@NotNull` on
  `PUT` -- an update is expected to be explicit about the flag so the
  menu-events availability transition is unambiguous.

**`POST /menu-items/validate` request:**

- `items`: `@NotEmpty`, `@Size(max=100)`.
- `items[].menuItemId`: `@NotNull`.
- `items[].quantity`: `@Min(1)`, `@Max(100)`.

## 4. Database Schemas

### 4.1 Restaurant Service (`restaurant_db`)

Committed at
`services/restaurant-service/src/main/resources/db/migration/V1__init.sql`
(Phase 2B).

```sql
CREATE TABLE restaurant (
    restaurant_id     UUID            PRIMARY KEY,
    owner_id          UUID            NOT NULL,
    name              VARCHAR(255)    NOT NULL,
    address           VARCHAR(255),
    city              VARCHAR(120),
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    operating_hours   VARCHAR(20),
    is_open           BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP       NOT NULL
);
CREATE INDEX idx_restaurant_city  ON restaurant(city);
CREATE INDEX idx_restaurant_owner ON restaurant(owner_id);
```

### 4.2 Menu Service (`menu_db`)

Committed at
`services/menu-service/src/main/resources/db/migration/V1__init.sql`
(Phase 2B).

```sql
CREATE TABLE menu_item (
    menu_item_id      UUID            PRIMARY KEY,
    restaurant_id     UUID            NOT NULL,
    name              VARCHAR(255)    NOT NULL,
    description       VARCHAR(2000),
    price_amount      NUMERIC(19,2)   NOT NULL CHECK (price_amount > 0),
    price_currency    VARCHAR(3)      NOT NULL DEFAULT 'EUR',
    category          VARCHAR(100)    NOT NULL,
    is_available      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP       NOT NULL
);
CREATE INDEX idx_menu_item_restaurant ON menu_item(restaurant_id);
CREATE INDEX idx_menu_item_category   ON menu_item(category);
```

## 5. Seed Data Plan

Seed data is loaded via **Flyway `V2__seed_demo_data.sql`** per Q8 of
`0004-open-questions.md` (resolved here: Flyway is the chosen route
because it plays nicely with `ddl-auto=validate` and works identically
inside Docker and on the host). Six demo restaurants and 16 menu items.

### 5.1 Restaurants

| restaurant_id | owner_id | name | city | is_open | operating_hours |
|---------------|----------|------|------|---------|-----------------|
| `d0000001-...` | `00000000-...-01` | Pizza Antonio | Tartu | true | 11:00-22:00 |
| `d0000002-...` | `00000000-...-01` | Sushi Lumi | Tartu | true | 12:00-23:00 |
| `d0000003-...` | `00000000-...-02` | Cafe Nero | Tartu | false | 08:00-20:00 |
| `d0000004-...` | `00000000-...-02` | Burger Bros | Tallinn | true | 10:00-22:00 |
| `d0000005-...` | `00000000-...-03` | Vegan Vibes | Tallinn | true | 11:00-21:00 |
| `d0000006-...` | `00000000-...-03` | Pasta Palace | Tallinn | false | 11:30-22:30 |

At least one closed restaurant (row 3 and 6) for failure-path demos.

### 5.2 Menu items (16)

| menu_item_id | restaurant | name | category | price | is_available |
|--------------|------------|------|----------|-------|--------------|
| `e0000011-...` | Pizza Antonio | Margherita | Main | 8.50 | true |
| `e0000012-...` | Pizza Antonio | Quattro Formaggi | Main | 10.50 | true |
| `e0000013-...` | Pizza Antonio | Tiramisu | Dessert | 5.00 | true |
| `e0000014-...` | Pizza Antonio | Garlic Bread | Appetizer | 3.50 | true |
| `e0000021-...` | Sushi Lumi | Nigiri Set | Main | 14.00 | true |
| `e0000022-...` | Sushi Lumi | Miso Soup | Appetizer | 4.00 | true |
| `e0000023-...` | Sushi Lumi | Mochi | Dessert | 4.50 | true |
| `e0000031-...` | Cafe Nero | Cappuccino | Drink | 3.00 | true |
| `e0000032-...` | Cafe Nero | Chocolate Cake | Dessert | 5.50 | false |
| `e0000041-...` | Burger Bros | Double Cheese | Main | 9.50 | true |
| `e0000042-...` | Burger Bros | Fries | Appetizer | 3.00 | true |
| `e0000043-...` | Burger Bros | Vanilla Shake | Drink | 4.00 | true |
| `e0000051-...` | Vegan Vibes | Quinoa Bowl | Main | 11.00 | true |
| `e0000052-...` | Vegan Vibes | Green Smoothie | Drink | 5.00 | true |
| `e0000053-...` | Vegan Vibes | Vegan Brownie | Dessert | 4.00 | true |
| `e0000061-...` | Pasta Palace | Carbonara | Main | 10.00 | true |

Full UUIDs live in the `V2` migration.

## 6. Cross-Service Assumptions

- `MenuItem.restaurantId` is a **cross-service** reference to a
  restaurant owned by Restaurant Service. Stored as `UUID`. **No
  foreign-key constraint.** No cross-DB join.
- `Restaurant.ownerId` is a **cross-service** reference to a user
  owned by User Service (Alfa-Kilo). Stored as `UUID`. **No foreign-key
  constraint.** No cross-DB join.
- Sierra-Lima's services do **not** validate that `ownerId` or
  `restaurantId` exist in the other database at write time. W1 integrity
  is handled by Order Service during the call chain (it already looks up
  the user and the restaurant before placing an order).

## 7. Error Envelope

Canonical error envelope for both services (resolves Q1 of
`0004-open-questions.md`: no `traceId` in MVP).

```json
{
  "timestamp": "2026-04-20T10:15:00Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Validation failed",
  "path": "/restaurants",
  "validationErrors": [
    { "field": "name", "message": "must not be blank" }
  ]
}
```

- `timestamp` is ISO-8601 UTC.
- `validationErrors` is present only for `400` / `422` responses.
- `path` is the request path **before** the gateway's `/api` strip
  (controllers see `/restaurants`, clients see `/api/restaurants`).

## Consequences

- Phases 3-6 implement against this document without reopening the
  master plan for JSON shapes.
- Phase 10 (W1 integration) consumes §1.6 and §2.6 directly. Do not
  change those shapes after Phase 6 without a superseding decision.
- Resolves Q1 (error envelope) and Q8 (Flyway seed) of
  `0004-open-questions.md`.
- Leaves Q2 (category vocabulary -- kept as free-form String for now,
  UI constraint deferred), Q3 (no HATEOAS), Q4 (embeddables in the
  Java entity), Q5 (availability 200-vs-409 -- stays 200), Q7
  (Spring Data `Pageable`), Q9 (Testcontainers-vs-H2), and Q10
  (auditor strategy) as open -- resolved in later phases.

## Supersedes

None. This is the first contract pack.
