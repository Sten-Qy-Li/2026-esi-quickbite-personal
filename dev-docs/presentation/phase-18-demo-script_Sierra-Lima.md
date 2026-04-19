# CP#3 Live Demo Script -- Sierra-Lima slice

- **Owner.** Sierra-Lima for browser + Sierra-Lima endpoints; team
  hand-offs called out at each beat.
- **Source.** `Charlie-Lima-Alfa_a520963_project-phases-final.md`
  Phase 18 task 2.
- **Companion.** `phase-18-slides_Sierra-Lima.md` (the deck) and
  `phase-18-fallbacks_Sierra-Lima.md` (recovery commands).
- **Date of dry-run.** 2026-05-18 (Phase 18 rehearsal slot).
- **Date of graded run.** 2026-05-19 (CP#3).
- **Total budget.** 12 minutes live + 3 min buffer; deck
  narration runs in parallel between beats.
- **Predecessor.** `dev-docs/checkpoint-2-talking-points.md` §2
  is the CP#2 click-path; this script supersedes it for CP#3
  (adds Phase 15 owner-vs-owner 403 demo + Phase 16 toggle ->
  log line beat).

---

## 0. Pre-demo checklist (run 30 minutes before the talk)

Performed by Sierra-Lima from `services/local-dev/`. Estimated
duration on a laptop with cached layers: **5 minutes**.

```
cd services/local-dev
cp .env.example .env.local                     # first time only
# Set teammate base URLs for the smoke run; no-op if not set
export USER_BASE=http://localhost:9081         # Alfa-Kilo
export ORDER_BASE=http://localhost:9082        # Alfa-Kilo
export PAYMENT_BASE=http://localhost:9083      # Elephant-Yankee
export DELIVERY_BASE=http://localhost:9084     # Elephant-Yankee
export NOTIFICATION_BASE=http://localhost:9085 # Mike-Alfa
docker compose --env-file .env.local --profile dev-gateway \
  up -d --build
docker compose ps                              # all (healthy)
```

When the team's combined compose file is in use (with all seven
services on one network), drop `--profile dev-gateway` and unset
`GATEWAY_UPSTREAM`; Alfa-Kilo's real gateway answers on `:8080`.

Mint dev tokens once (kept in two terminal panes for the demo):

```
java services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/JwtDevMint.java
# copy "owner" token   -> ENV var OWNER_JWT (terminal A) and into the
#                         browser via DevTools -> Application ->
#                         localStorage -> quickbite.jwt
# copy "customer" token -> ENV var CUSTOMER_JWT (terminal B)
```

Pre-open in tabs so the demo never alt-tabs cold:

| Pane | URL / command | Use |
|---|---|---|
| Browser tab 1 | `http://localhost:8090/restaurants` | Steps 1.x |
| Browser tab 2 | `http://localhost:8081/swagger-ui.html` | Step 2 (Restaurant Swagger) |
| Browser tab 3 | `http://localhost:8082/swagger-ui.html` | Step 2 (Menu Swagger) |
| Postman | `services/local-dev/postman/QuickBite.postman_collection.json` | Step 3 (W1) + Step 4 (Negative Auth) |
| Terminal A | `docker compose logs -f menu-service` | Step 5 (toggle -> log) |
| Terminal B | `docker compose logs -f restaurant-service` | Step 4 (denial WARN) |
| Terminal C | bash, in `services/local-dev/` | Step 6 (`smoke-cross-service.sh`) |

Verify before going live:

```
curl -s http://localhost:8081/actuator/health | grep '"UP"'
curl -s http://localhost:8082/actuator/health | grep '"UP"'
curl -s http://localhost:8090/                | head -1   # 200 + HTML
```

If any of these fails, jump straight to
`phase-18-fallbacks_Sierra-Lima.md` §1 *before* the audience sees
red text.

---

## 1. Browser walkthrough (3.5 min, Sierra-Lima)

Mirrors deck slide **S13**. Pre-conditions: signed-in as a
RestaurantOwner; tab 1 already open at `/restaurants`.

### 1.1 Sign-in moment (15 s)

Click **Sign out** in `AppNav`, then **Sign in** -> `/login`.
Either submit the live login form (User Service path) or paste
the dev token into `localStorage.quickbite.jwt` and refresh.

- **Expected.** AppNav shows "Sign out" + role label. Network
  tab shows the token attached to subsequent `/api/**` calls
  via `Authorization: Bearer ...`.
- **Narration.** "Token persists in `localStorage`; every API
  call attaches it; the gateway forwards verbatim and each
  service signature-verifies with the shared HS256 secret."

### 1.2 Browse and filter (30 s)

Stay on `/restaurants`. Type `Tallinn` into the city filter ->
list refreshes. Toggle the `isOpen=true` filter; "Cafe Nero"
disappears (closed in seed `V2`).

- **Expected.** Two requests visible in DevTools:
  `GET /api/restaurants?city=Tallinn` and
  `GET /api/restaurants?city=Tallinn&isOpen=true`.
- **Narration.** "Both are anonymous reads -- `permitAll()` --
  per the route matrix on slide S12."

### 1.3 Open restaurant detail (15 s)

Click "Pizza Antonio". Land on `/restaurants/d0000099-...`.

- **Expected.** Request `GET /api/restaurants/d0000099-...`
  returns 200; the detail panel renders address, hours
  (`11:00-22:00`), `isOpen=true`.

### 1.4 Add restaurant (owner path, 45 s)

Click "Add restaurant" (visible because the role token is
`RestaurantOwner`). Fill the form: name `Demo Bistro`, city
`Tallinn`, lat `59.4`, long `24.7`, hours `08:00-22:00`,
`isOpen=true`. Submit.

- **Expected.** `POST /api/restaurants` -> 201 +
  `Location: /restaurants/<new-uuid>`; list view re-fetches and
  the new card appears at the top.
- **Narration (Phase 15).** "If a Customer-role token tried this,
  the server returns 403 even though the button is hidden -- the
  client-side guard is cosmetic; the server enforces."

### 1.5 Add menu items (60 s)

From the new restaurant, click **Menu** -> **Add menu item**.
Create two:

| Name | Category | Price | Available |
|---|---|---|---|
| Bruschetta | Appetizer | 4.50 EUR | true |
| Lasagna | Main | 12.90 EUR | true |

- **Expected.** Two `POST /api/restaurants/<rid>/menu-items` calls
  with status 201; list view shows both items.

### 1.6 Toggle availability (15 s)

Open Lasagna's row, switch the **Available** toggle off.

- **Expected.** `PUT /api/menu-items/<id>` 200 with `isAvailable=false`.
  In Terminal A (`docker compose logs -f menu-service`) the
  envelope appears (this is the cue for §5):

  ```
  INFO  menu-events : topic=menu-events key=<menuItemId>
    envelope={"id":"<uuid>","type":"menu.item-availability-changed",
    "occurredAt":"2026-05-18T13:22:01Z",
    "payload":{"menuItemId":"<id>","restaurantId":"<rid>",
    "isAvailable":false,"previousIsAvailable":true}}
  ```

  Pause for the audience to read it.

### 1.7 Hand-off

"That's the browser surface. Now we'll show the API design and
the W1 contract from outside the SPA."

---

## 2. Swagger walk (1.5 min, Sierra-Lima)

Mirrors deck slide **S6**. Pre-conditions: tabs 2 + 3 open.

### 2.1 Restaurant Swagger (45 s)

Switch to tab 2 (`localhost:8081/swagger-ui.html`).

- Expand the `Restaurants` tag.
- Expand `GET /restaurants/{id}/availability`. Highlight the
  response schema (`AvailabilityResponse`) and the `200` /
  `404` example payloads.
- Click **Try it out** -> paste `d0000099-0000-0000-0000-000000000099`
  -> Execute. Expected: `200` with
  `{"acceptsOrders": true, "isOpen": true, "operatingHours":
  "11:00-22:00", "checkedAt": "..."}`.

### 2.2 Menu Swagger (45 s)

Switch to tab 3 (`localhost:8082/swagger-ui.html`).

- Expand the `Menu items` tag.
- Expand `POST /menu-items/validate`; highlight the request
  body schema (`ValidateMenuItemsRequest`) and the response
  schema (`ValidateMenuItemsResponse`).
- Optional: paste an invalid item UUID, Execute, show the per-line
  `error: NOT_FOUND` and `allValid: false` while the response
  status stays `200`.

### 2.3 Hand-off

"The auth column on each endpoint matches the Spring
`@PreAuthorize` matrix; W1 hops 4 and 5 are visible end-to-end
in our Postman pack -- back to Alfa-Kilo for that."

---

## 3. Synchronous integration: W1 in Postman (2 min, Alfa-Kilo + Sierra-Lima)

Mirrors deck slide **S8**.

### 3.1 Postman folder run (60 s)

In Postman, open the `W1 Integration` folder. Click **Runner** ->
**Run W1 Integration** -> Run. The collection runner walks 9
requests with 40 assertions.

- **Expected.** Green check on every request; bottom counter
  reads `40 / 40 passed`.
- **Pause** on the `[200 closed] GET /availability` request --
  show `acceptsOrders=false`, `isOpen=false`, restaurant
  `Cafe Nero`. Narrate that closure is a `200` payload, not a
  `409` (per ADR 0030 -- "Order Service aggregates the cannot-
  accept signal in the same shape").
- **Pause** on `[200 missing] POST /menu-items/validate` --
  show one `MENU_ITEM_NOT_FOUND` line + `allValid:false`.

### 3.2 Smoke script (30 s)

In Terminal C:

```
bash services/local-dev/smoke.sh
```

Expected last line: `Sierra-Lima smoke test passed.` Time:
~10 s on a warm stack. This is the same flow Postman just
walked, executed end-to-end without the GUI.

### 3.3 Hand-off

"Async next -- Mike-Alfa drives the Order -> Notification chain."

---

## 4. Negative auth (Phase 15) (1.5 min, Sierra-Lima)

Mirrors deck slide **S12**. Two beats.

### 4.1 401 -- missing token (30 s)

Postman -> `Negative Auth` folder -> request
`[401] POST /restaurants (no Authorization)` -> Send.

- **Expected.** Status 401. Body is the `ErrorResponse` envelope
  (`status: 401`, `error: Unauthorized`, no leaky stack trace).

### 4.2 403 -- ownership denial (45 s)

Postman -> `Negative Auth` folder -> request
`[403] PUT /restaurants/{ownerB-restaurant-id} as ownerA` ->
Send.

- **Expected.** Status 403. Body is the `ErrorResponse` envelope.
  In Terminal B (`docker compose logs -f restaurant-service`)
  the WARN line appears immediately:

  ```
  WARN ... e.u.e.q.r.service.RestaurantService :
    ownership denial actor=00000000-0000-0000-0000-000000000099
    role=RestaurantOwner endpoint=PUT /restaurants/d0000098-...
    restaurantId=d0000098-... ownerId=00000000-0000-0000-0000-000000000098
  ```

  Read the line aloud; the audience now sees the matching server
  signal.

### 4.3 Hand-off

"Frontend, then async -- back to Sierra-Lima for the
front-end recap, then Mike-Alfa for the Kafka demo."

---

## 5. Phase 16 toggle -> menu-events log (already covered in 1.6)

This beat is folded into §1.6. Keep it visible by *not closing
Terminal A* -- when Mike-Alfa shows the Order / Notification
trace in §6, the same compose-logs pane is still tailing
Sierra-Lima's `menu-events` logger.

---

## 6. Cross-service smoke (2 min, Mike-Alfa)

Mirrors deck slide **S10**.

### 6.1 Trigger (30 s)

Terminal C:

```
cd services/local-dev
bash smoke-cross-service.sh
```

The script writes a timestamped trace to
`services/local-dev/evidence/cross-service-smoke_<RUN_TAG>.log`
and (if `docker` is reachable) a grepped envelope slice to
`services/local-dev/evidence/menu-events_<RUN_TAG>.log`.

### 6.2 Tail the trace live (60 s)

In Terminal C:

```
tail -n 80 services/local-dev/evidence/cross-service-smoke_<RUN_TAG>.log
```

Walk the audience through:

- Step 1 -- token mints (owner + customer).
- Step 2 -- W1 hops 4-5 (`acceptsOrders=true`, `allValid=true`,
  `totalAmount=...`).
- Step 3 -- two `menu.item-availability-changed` envelopes
  from the toggle pair.
- Step 4 -- teammate probes (User / Order / Payment / Delivery
  / Notification) green if the URLs are set.
- Step 5 -- summary line: `OK` (exit 0) or `TEAMMATE-FAIL` /
  `SIERRA-FAIL` (exit 2 / 1).

### 6.3 Hand-off

"That's the full trace. Q&A is open."

---

## 7. Talk-track timing summary

Total = 12 min live demo, narrated alongside the deck.

| § | Beat | Duration |
|---|---|---|
| 0 | Pre-demo (off-stage) | 5 min |
| 1 | Browser walkthrough (S13) | 3.5 min |
| 2 | Swagger (S6) | 1.5 min |
| 3 | W1 Postman + smoke (S8) | 2 min |
| 4 | Negative auth (S12) | 1.5 min |
| 5 | (folded into §1.6) | -- |
| 6 | Cross-service smoke (S10) | 2 min |
| Buffer | If we run hot | 1.5 min |

A 25-min talk pairs ~12 min of slide narration with ~12 min of
this demo.

---

## 8. What can go wrong (and what to do)

Detailed recovery flows live in
`dev-docs/presentation/phase-18-fallbacks_Sierra-Lima.md`.
Single-line summary:

- Stack won't come up -> §1 of the fallbacks doc.
- A single service is unhealthy -> §2 (`docker compose restart
  <name>`).
- Browser flow breaks live -> §3.1 (Postman path covers the same
  beats).
- Swagger fails to load -> §3.2 (use the screenshot pack).
- Demo shows wrong data -> §3.3 (`docker compose down -v` then
  re-seed).
- Cross-service smoke exits non-zero -> §3.4 (read the exit code
  table; teammate fail is presentable, Sierra-Lima fail isn't).
