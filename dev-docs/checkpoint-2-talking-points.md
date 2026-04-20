# Checkpoint #2 -- Sierra-Lima Demo Script & Talking Points

- **Checkpoint date:** 2026-05-12 (frontend + backend).
- **Target rehearsal:** 2026-05-11 (Phase 14 Monday dry run).
- **Owner:** Sierra-Lima (Restaurant + Menu services + Vue frontend).
- **Source:** `Charlie-Lima-Alfa_a520963_project-phases-final.md`
  Phase 14 §6 and Checkpoint #2 Gate (L1707+).
- **Demo stack base commit:** tracked in
  [`phase-14-verification_Sierra-Lima.md`](./verification/phase-14-verification_Sierra-Lima.md).

This file is the script Sierra-Lima reads from during CP#2. CP#1
talking points (`checkpoint-1-talking-points.md`) still hold -- the
graders asked us to *add* the full-stack browser walkthrough, not
replace the backend narrative. Keep this file short; details live in
the report draft (`report-draft-backend_Sierra-Lima.md` §7) and the
verification note.

---

## 1. Stack we demo from

One `docker compose` stack, five long-lived containers plus the
opt-in `dev-gateway` stub:

```
services/local-dev/docker-compose.yml
  restaurant-db      (postgres:15)      :${RESTAURANT_DB_HOST_PORT:-5432}
  menu-db            (postgres:15)      :${MENU_DB_HOST_PORT:-5433}
  restaurant-service (Spring Boot 3.3)  :8081
  menu-service       (Spring Boot 3.3)  :8082
  frontend           (nginx:1.27-alpine) :${FRONTEND_HOST_PORT:-8090}
  dev-gateway        (nginx:1.27-alpine) :${DEV_GATEWAY_HOST_PORT:-8080}
                                            (profile: dev-gateway)
```

**Pre-demo checklist (~3 minutes):**

1. `cd services/local-dev`
2. `cp .env.example .env.local` (first time only; tweak host ports
   if 5432/5433/8080/8090 collide).
3. Export `COMPOSE_PROFILES=dev-gateway` or append `--profile
   dev-gateway` to the up command so the stub joins the stack while
   Alfa-Kilo's real gateway is absent.
4. `docker compose --env-file .env.local --profile dev-gateway up -d --build`
5. Wait for health: `docker compose ps` -> all `(healthy)`.
6. Mint a dev JWT in a separate shell (until User Service lands):
   `java services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/JwtDevMint.java`
   and copy the token.
7. Open `http://localhost:8090/`, DevTools -> Application ->
   localStorage -> set `quickbite.jwt = <token>` (or use the
   sign-in flow once `POST /api/auth/login` is wired).

When Alfa-Kilo's real gateway lands, drop the `dev-gateway`
profile and flip `GATEWAY_UPSTREAM=http://api-gateway:8080` in
`.env.local` -- no other change on Sierra-Lima's side.

---

## 2. Live demo click-path

Rough budget: **8-10 minutes** for the happy path, 2-3 minutes for
error paths. Pause for grader questions between sections.

### 2.1 Sign in (60s)

- Navigate to `http://localhost:8090/`.
- Click "Sign in" -> `/login`.
- If User Service is live, type credentials; otherwise narrate:
  "User Service ships in Phase 15 / Alfa-Kilo's track -- for CP#2
  we paste a dev JWT that the Spring filters accept because the
  HS256 secret is shared." Show the localStorage key, then refresh.
- AppNav now shows "Sign out" + the signed-in role. Narrate: the
  token is forwarded to every `/api/**` call; each service
  signature-verifies only, never re-mints.

### 2.2 Discover restaurants (60s)

- `/restaurants` -- seeded rows from `V2__seed_demo_data.sql`.
- Point out the `?city=` and `?isOpen=` filters hitting
  `GET /api/restaurants`.
- Click a row -> `/restaurants/:id` (details + availability).

### 2.3 Create restaurant as owner (90s)

- `/restaurants/new` -- route-level guard redirected anonymous
  users to `/login` before we pasted the token.
- Fill the form (operating hours `08:00-22:00`, city `Tallinn`,
  valid lat/long). Submit.
- 201 returns the new UUID; the list view shows the row.
- Narrate: `name` is VARCHAR(255), operating hours are a
  server-parsed `HH:MM-HH:MM` string, duplicate-by-owner raises
  409 `DuplicateRestaurantException` in the service layer (not
  the DB) -- decision 0020 §5.

### 2.4 Add menu items (90s)

- From the new restaurant, click "Menu" -> "Add menu item".
- Create two items (one "Appetizer" at 3.50 EUR, one "Main" at
  12.90 EUR). Note NUMERIC(19,2); <= 2 decimal places is enforced
  at both the DTO and the DB CHECK (`price_amount > 0`).
- Show the list refreshes, toggle availability on one item.

### 2.5 Toggle restaurant status (30s)

- Back to the restaurant detail, `PATCH
  /api/restaurants/:id/status`. `isOpen` flips. Narrate: W1 hop 4
  inspects this field + operating hours when Order Service asks
  whether the restaurant can take the order.

### 2.6 Edit / delete menu item (60s)

- Edit the Main item's price from 12.90 -> 13.50, save. PUT
  returns the updated representation.
- Delete the Appetizer. DELETE returns 204; list view refreshes.
- Narrate: menu items are hard-deleted; the Order domain is
  expected to carry item snapshots on orders so deletion never
  breaks order history (§9 limitations).

### 2.7 W1 through the gateway (90s)

- In a side terminal: `curl` `GET
  /api/restaurants/{id}/availability` through the `dev-gateway` on
  :8080 with the Bearer token. Response: `{acceptsOrders: true,
  ...}` -- hop 4 of W1.
- `curl` `POST /api/menu-items/validate` with an existing item ID
  and quantity 2. Response carries `totalAmount: 27.00 EUR` (hop
  5).
- Narrate: Order Service calls these two endpoints from the same
  compose network when W1 is live. For CP#2 the `dev-gateway` stub
  stands in; the real gateway in Phase 15 swaps transparently.

### 2.8 Error paths (optional, 60-90s)

Pick two quick ones:

- Paste a garbled JWT -> 401 via `RestAuthEntryPoints`.
- Submit a menu item with `priceAmount=-1` -> 422 with populated
  `validationErrors` array.
- `GET /api/restaurants/{unknown-uuid}/availability` -> 404 with
  the shared error envelope.

---

## 3. What we say about what is *not* implemented

- **Real `/api/auth/login` is Alfa-Kilo / User Service.** Dev
  token mint is the bridge; will be removed once User Service
  ships (Phase 15 or Alfa-Kilo's Phase equivalent).
- **Dev-gateway stub** stands in for Alfa-Kilo's real gateway for
  CP#2. Listed as a CP#2 limitation; the swap is a one-line
  `.env.local` change.
- **W2 / W3 events.** Sierra-Lima is neither producer nor consumer
  (decision 0032 §2). If the grader asks about async, point at
  `order-events`, `payment-events`, `delivery-events`,
  `notification-events` in the envelope contract and note that
  Restaurant / Menu do not appear on either side in the A3
  baseline.
- **Authorisation hardening** happens in Phase 15. Today the
  `@PreAuthorize` matrix exists; visible 403-vs-401 distinction
  and the unauthorised-access demo lands at CP#3.
- **Resilience4j / retries.** N/A for Sierra-Lima -- no
  service-to-service calls out. See
  `phase-10-verification_Charlie-Lima-Alfa.md` §6.

---

## 4. What remains for CP#3

- Role-aware authorisation visibly enforced (customer vs owner vs
  admin) -- Phase 15.
- W2 / W3 asynchronous evidence (Mike-Alfa's broker, Order ->
  Notification flow) -- Phase 16.
- Final report assembly, rehearsal, exam-ready limitations
  section -- Phase 17.
- Real gateway + real User Service swap (cross-track dependency).

---

## 5. If the stack will not come up during the demo

Backup path (ordered by how disruptive the swap is):

1. Restart a single container: `docker compose --env-file
   .env.local restart frontend` (or the failing one).
2. Fall back to `npm run serve` (Vue dev server at :8081) with
   `VUE_APP_API_BASE_URL=http://localhost:8080` -- dev-gateway
   still provides the API.
3. Show the **Phase 14 backup screen recording** at
   `dev-docs/verification/phase-14-backup-recording.mp4` (to land
   with the verification note). Narrate over it.
4. Absolute fallback: Postman collection from CP#1 -- `W1
   Integration` + `Negative Auth` folders in
   `services/local-dev/postman/QuickBite.postman_collection.json`
   prove the backend half still works end-to-end.

---

## 6. Team hand-off for CP#2

- **Sierra-Lima** (this script): full-stack browser walkthrough,
  restaurants + menu, W1 hops 4 and 5.
- **Alfa-Kilo**: gateway routing, User Service sign-in (if
  shipped), Order Service happy path into W1.
- **Elephant-Yankee**: Payment hop 7 if live.
- **Mike-Alfa**: Delivery hop 8 + broker infrastructure preview
  (async evidence lands at CP#3).

If Alfa-Kilo's gateway is live by rehearsal, we drop the
`dev-gateway` profile and re-record §2.7 against
`api-gateway:8080`; if not, we keep the stub and flag it in the
spoken limitations.
