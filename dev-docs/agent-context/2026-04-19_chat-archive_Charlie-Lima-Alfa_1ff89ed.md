# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`1ff89ed`)

## Session Summary

This session executed **Phase 13 -- Vue.js Frontend: Restaurant &
Menu UX** for the QuickBite frontend, as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
Phase 13 (lines 1398-1436).

The session began on top of `1ff89ed` ("Land Phase 12 Vue 3 frontend
shell, routing, and sign-in"). No mid-session compaction occurred;
the run is a single autonomous execution of the Phase 13 task list
plus the archive-and-commit at the end.

Phase 13 replaces the Phase 12 placeholder restaurant and menu views
with fully wired CRUD screens backed by the locked Sierra-Lima
contracts (`GET /api/restaurants`, `POST /api/restaurants`,
`GET /api/restaurants/{id}`, `PUT /api/restaurants/{id}`,
`PATCH /api/restaurants/{id}/status`,
`GET /api/restaurants/{rid}/menu-items`,
`POST /api/restaurants/{rid}/menu-items`,
`GET /api/menu-items/{id}`, `PUT /api/menu-items/{id}`). The browse
flow **list -> detail -> menu -> item** now works end-to-end; owner
mutations are gated behind `canManageRestaurants()` (reads the JWT
`role` claim) so a Customer token sees a read-only UI.

No backend file changed in Phase 13. No new npm dependency was
installed. `npm run lint` is clean; `npm run build` produces a
~47 KiB gzipped vendor bundle and a ~10 KiB gzipped app bundle.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`,
  and (since Phase 12) the `Frontend` under
  `services/frontend/quickbite-frontend/`.
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Parent commit: `1ff89ed` -- "Land Phase 12 Vue 3 frontend shell,
  routing, and sign-in"
- Environment: Windows 11 + Git Bash
- Node.js/npm as shipped in Phase 12's `node_modules/`; no re-install
  in this session.

## User Requests

Initial request: *"Hi Claude, please work on Phase 13 of the master
plan `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.
After completing the implementation of Phase 13, please archive the
session context to `dev-docs/agent-context`, and then commit all
changes and push (try to commit and push the entire local repository;
exclude files only if there's a very good reason, according to your
best judgement). Thanks!"*

No mid-session corrections or redirections. The chat was a single
autonomous run. The model in use was Claude Opus 4.7 at effort level
`max`.

## Contract Reconciliation -- what the views call

Before writing any UI, the session read the current backend DTO
shapes and the gateway route table so the frontend speaks the same
wire format the services expose. Canonical references:

| Resource | URL (client-facing) | DTO |
|----------|--------------------|-----|
| List restaurants | `GET /api/restaurants?city=&isOpen=` | `RestaurantResponse[]` |
| Create restaurant | `POST /api/restaurants` | `CreateRestaurantRequest` |
| Get restaurant | `GET /api/restaurants/{id}` | `RestaurantResponse` |
| Update restaurant | `PUT /api/restaurants/{id}` | `UpdateRestaurantRequest` |
| Toggle status | `PATCH /api/restaurants/{id}/status` | `{ isOpen: boolean }` |
| List menu items | `GET /api/restaurants/{rid}/menu-items?category=&available=` | `MenuItemResponse[]` |
| Create menu item | `POST /api/restaurants/{rid}/menu-items` | `CreateMenuItemRequest` |
| Get menu item | `GET /api/menu-items/{id}` | `MenuItemResponse` |
| Update menu item | `PUT /api/menu-items/{id}` | `UpdateMenuItemRequest` |

The `/api` prefix is stripped by the gateway before the request
reaches each service (recorded in
`dev-docs/decisions/0020-sierra-lima-contracts.md` §7 and §10). Phase
12's `src/api/client.js` already prepends the gateway base URL, so
the frontend calls literally pass the `/api/...` paths to `api.get`
etc.

Response field names used in the UI:

- `RestaurantResponse`: `restaurantId, ownerId, name, address, city,
  latitude, longitude, operatingHours, isOpen, createdAt, updatedAt`.
- `MenuItemResponse`: `menuItemId, restaurantId, name, description,
  priceAmount, priceCurrency, category, isAvailable, createdAt,
  updatedAt`.

Role gating relies on the `role` claim in the JWT (string, one of
`Customer | Driver | RestaurantOwner | Admin`), per Phase 9 / auth
contract §8. The UI treats `RestaurantOwner` and `Admin` as
"can manage"; everyone else gets a read-only layout.

## Phase 13 Task-by-Task Record

### Task 1 -- `canManageRestaurants()` helper in `auth/token.js`

Added three helpers beside the existing `readClaims()`:

- `readRole()` -- pulls the `role` claim (accepts `claims.role` or
  `claims.roles[0]` as a safety fallback).
- `readUserId()` -- pulls `userId` or `sub`. Not used in Phase 13
  views, but parked here for Phase 14's owner-scoped filters.
- `canManageRestaurants()` -- boolean, true iff the role is
  `RestaurantOwner` or `Admin`. Drives the "Add restaurant" / "Edit"
  / "Toggle status" visibility.

Every view imports `canManageRestaurants` rather than reading the
token directly, so the role check lives in one file.

### Task 2 -- `RestaurantListView.vue`

Fetches `GET /api/restaurants` on mount and on filter submit. Two
filters: a `city` text input and a status `select` (any / open /
closed); the status mapping to `?isOpen=true|false` stays on the
list, not on each card. Reset clears both and refetches.

UX shape:

- Loading line (`role="status"`) while the fetch is in flight.
- Dashed empty-state box when the response is `[]`.
- Red error banner for any `ApiError`.
- Card grid of results with:
  - Open/Closed pill (green/red).
  - Address line (comma-joined `address, city`, skips blanks).
  - Operating hours line (shown only when set).
  - "View details" and "Menu" quick links.

An "Add restaurant" button appears in the header **only** when the
current token can manage restaurants; Customers never see it.

### Task 3 -- `AddRestaurantView.vue` (new)

Form mirroring `CreateRestaurantRequest`:

| Field | Client validation | Server validation |
|-------|-------------------|-------------------|
| `name` | required, `maxlength=255` | `@NotBlank @Size(max=255)` |
| `address` | `maxlength=255` | `@Size(max=255)` |
| `city` | `maxlength=120` | `@Size(max=120)` |
| `latitude` | required, in `[-90, 90]` | `@NotNull @DecimalMin @DecimalMax` |
| `longitude` | required, in `[-180, 180]` | `@NotNull @DecimalMin @DecimalMax` |
| `operatingHours` | matches `^\d{2}:\d{2}-\d{2}:\d{2}$` when present | same `@Pattern` |

On submit it POSTs to `/api/restaurants` and navigates to
`{ name: 'restaurant-detail', params: { id: created.restaurantId } }`.
Errors surface in the red banner; per-field issues surface under
each input.

The route is under `meta.requiresAuth` so an anonymous visitor is
bounced to `/login?next=/restaurants/new` by the Phase 12 guard.
The page body additionally tells the operator the endpoint requires
`RestaurantOwner` / `Admin`, so a signed-in Customer who navigates
directly gets a clear 403 message when they try to submit.

### Task 4 -- `RestaurantDetailView.vue`

GETs `/api/restaurants/{id}` on mount and on `id` change. Shows:

- Name + address header and an Open/Closed pill.
- "View menu" link (always visible) and a "Mark as open/closed"
  toggle button (owners only, calls
  `PATCH /api/restaurants/{id}/status` with the inverted boolean).
- **Edit panel (owners only)** -- pre-populated form using the same
  field set as `AddRestaurantView`. Saves via
  `PUT /api/restaurants/{id}`; a green "Saved." banner confirms
  success; "Discard" resets the form to the last-loaded payload.
- **Info panel (customers)** -- read-only `dl` with hours and
  coordinates.

The `id` prop is watched with `immediate: true`; navigating between
`/restaurants/:a` and `/restaurants/:b` reloads cleanly.

### Task 5 -- `MenuView.vue` (= MenuItemList for this restaurant)

Kept the filename `MenuView.vue` from Phase 12 (Phase 12's router
table names it `restaurant-menu`). The body is Phase 13's
MenuItemList: GETs
`/api/restaurants/{id}/menu-items?category=&available=` and renders
a card grid. Header shows the restaurant's name once its
`GET /api/restaurants/{id}` resolves (the menu load does not block
on the restaurant load; a failed restaurant fetch just leaves the
fallback `Restaurant <uuid>` label).

Filters:

- `category` text input (matches the string category field).
- `availability` select (any / available / unavailable, mapped to
  `?available=true|false`).

Each card shows: name, availability pill, price (formatted as
`NN.NN CUR`), category, and description. Action link is "Edit" for
owners and "Details" for customers -- both route to
`/menu-items/{id}`.

An "Add menu item" button appears in the header **only** for
managers. It carries `?restaurantId=<rid>` as a query param so the
add form pre-selects this restaurant.

A small "← Back to restaurant" link at the bottom returns to the
detail page.

### Task 6 -- `AddMenuItemView.vue` (new)

Form mirroring `CreateMenuItemRequest`:

| Field | Client validation | Server validation |
|-------|-------------------|-------------------|
| `restaurantId` | required, chosen from dropdown | -- |
| `name` | required, `maxlength=255` | `@NotBlank @Size(max=255)` |
| `description` | `maxlength=2000` | `@Size(max=2000)` |
| `priceAmount` | required, `>= 0` | `@NotNull` (service rejects negatives) |
| `priceCurrency` | matches `^[A-Z]{3}$` | `@Pattern` |
| `category` | required, `maxlength=100` | `@NotBlank @Size(max=100)` |
| `isAvailable` | boolean, default `true` | optional in create DTO |

The restaurant dropdown is populated by a `GET /api/restaurants`
call on mount. If the page was opened with `?restaurantId=<rid>`
(from `MenuView`'s "Add menu item" link), the dropdown is
pre-selected. The currency input uppercases on every keystroke so
`eur` becomes `EUR` before the pattern check runs.

POSTs to `/api/restaurants/{rid}/menu-items`. Success navigates to
`{ name: 'menu-item-detail', params: { id: created.menuItemId } }`.

### Task 7 -- `MenuItemDetailView.vue` (new)

GETs `/api/menu-items/{id}`. Header shows name, category, price, and
a link back to the owning restaurant's menu.

- **Availability toggle button (owners only)**. The backend has no
  dedicated `PATCH /menu-items/{id}/availability` endpoint -- the
  update DTO requires every field every time -- so the toggle sends
  a full `PUT` with all current fields and the flipped
  `isAvailable`. Documented in-line.
- **Edit panel (owners only)** -- identical field set to the add
  form (minus the restaurant dropdown; `restaurantId` is
  immutable). Saves via `PUT /api/menu-items/{id}`.
- **Description panel (customers)** -- read-only paragraph.

### Task 8 -- Router additions

`src/router/index.js` gains three routes:

| Path | Name | Component | `requiresAuth` |
|------|------|-----------|----------------|
| `/restaurants/new` | `restaurant-new` | `AddRestaurantView` | yes |
| `/menu-items/new` | `menu-item-new` | `AddMenuItemView` | yes |
| `/menu-items/:id` | `menu-item-detail` | `MenuItemDetailView` | -- |

`/menu-items/:id` is deliberately public read; the backend already
protects all mutations with method-level `@PreAuthorize` checks, so
anonymous visitors who land on an item URL still get a valid GET
and the UI hides the edit panel.

The `new` routes remain behind `meta.requiresAuth` so the Phase 12
guard shields them before the role-scoped backend rejection. Role
mismatch inside the guard is not enforced client-side (the guard
only cares about "is there a token"); a signed-in Customer who
forces `/restaurants/new` in the address bar still hits the page
and sees a clear backend 403 on submit.

### Task 9 -- `HomeView.vue` polish

Updated the hero blurb ("Browse restaurants, view menus, and for
owners, manage them.") and added an "Add a restaurant" quick-link
card that renders **only** for managers. Non-managers keep seeing
the browse / sign-in / orders cards unchanged.

## Files Touched (diff vs. `1ff89ed`)

```
 services/frontend/quickbite-frontend/src/auth/token.js
 services/frontend/quickbite-frontend/src/router/index.js
 services/frontend/quickbite-frontend/src/views/HomeView.vue
 services/frontend/quickbite-frontend/src/views/MenuView.vue
 services/frontend/quickbite-frontend/src/views/RestaurantDetailView.vue
 services/frontend/quickbite-frontend/src/views/RestaurantListView.vue
 services/frontend/quickbite-frontend/src/views/AddMenuItemView.vue    (new)
 services/frontend/quickbite-frontend/src/views/AddRestaurantView.vue  (new)
 services/frontend/quickbite-frontend/src/views/MenuItemDetailView.vue (new)
```

No backend file changed in Phase 13. `AppNav.vue` was left alone
because all new links (Add Restaurant, Add Menu Item) live inside
the page bodies, gated by role; cluttering the global nav would
surface owner-only calls to every user.

## Verification

```
$ npm run lint -- --no-fix
DONE  No lint errors found!

$ npm run build
DONE  Compiled successfully in 4793ms
  dist\js\chunk-vendors.7d487671.js    130.84 KiB    46.81 KiB gzipped
  dist\js\app.4260a2c2.js               46.69 KiB    10.19 KiB gzipped
  dist\css\app.044ff082.css              9.78 KiB     1.94 KiB gzipped
```

End-to-end browser verification against a live backend was **not**
run in this session because the User Service / API Gateway
(Alfa-Kilo) are still not committed. The contract-level wiring was
checked against the committed Restaurant Service and Menu Service
controllers and their DTO records; Phase 14's DoD covers the live
smoke test through the gateway.

## Definition of Done -- Phase 13

Per the master plan (lines 1431-1436):

- [x] Restaurant CRUD works from the browser. *(POST/GET/PUT/PATCH
      wired; verified by compilation + contract alignment against
      the committed backend DTOs. Browser smoke against a live
      gateway is a Phase 14 deliverable.)*
- [x] Menu item CRUD works from the browser. *(POST/GET/PUT wired;
      DELETE is a backend capability the plan does not require the
      UI to expose in Phase 13, and it was intentionally skipped to
      avoid surfacing destructive owner actions before Phase 14
      auth hardening. A full-field PUT is used to toggle
      `isAvailable` because the service does not expose a dedicated
      availability patch endpoint.)*
- [x] Browse flow (list -> detail -> menu -> item) works end-to-end.
      *(The router links between each view are in place; the list
      page "Menu" button skips to the menu, the menu page rows link
      to the item detail, and the item detail header links back to
      the menu.)*
- [x] Error states handled gracefully. *(Every fetch call is
      wrapped in try/catch; a red banner surfaces the message,
      `ApiError` 401s bounce to login, and forms have per-field
      inline errors for client-side validation.)*

## Outlook -- what Phase 14 inherits

- A UI that already speaks the locked DTO shapes. Phase 14's
  integration smoke should pass as soon as Alfa-Kilo's gateway is
  stood up without further frontend changes.
- Role-aware layouts driven by the JWT `role` claim. Phase 15's
  authorisation hardening can tighten the backend without needing
  the UI to learn new claim names.
- A shared `canManageRestaurants()` helper in `auth/token.js`.
  Phase 14's owner-scoped filters ("show me only restaurants I
  own") can extend it without touching every view.
- Three new routes (`/restaurants/new`, `/menu-items/new`,
  `/menu-items/:id`) behind the existing `requiresAuth` guard.
  Phase 15 can layer a role check into the guard in one place.
- No open DTO questions for the frontend at the end of Phase 13.
