# quickbite-frontend

Vue.js 3 frontend for the QuickBite food-delivery platform.
Created in **Phase 12** of the Sten master plan
(`../../../dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`).
Owned by Sten. Covers the Sten slice of the UI
(restaurant and menu flows); teammates' flows (user signup, order
placement, driver, payment) integrate during the group-wide
frontend merge.

## What the frontend ships

- Vue 3 + Vue Router 4 + Babel scaffold (Vue CLI 5).
- Top-level navigation: Home, Restaurants, Cart, Orders, Login /
  Sign up.
- Restaurant browse + detail + add (R19), status toggle (R20).
- Menu browse + item detail + add + update + delete (R21), with
  role-gated visibility of owner-only controls.
- Shared `api/` client that prepends the gateway base URL, attaches
  the JWT bearer from `localStorage`, and bounces 401s back to
  `/login`.
- `auth/token.js` -- the single source of truth for the bearer token
  and role decoding.
- Route guard that redirects anonymous visitors away from
  `meta.requiresAuth` routes.

## Prerequisites

- Node.js 18+ (the project was scaffolded against Node 24 / npm 11).
- The QuickBite API Gateway listening on `http://localhost:8080`
  (Alfa-Kilo). The frontend still starts and renders without it; only
  the login / signup network calls fail.

## Running it

```bash
cd services/frontend/quickbite-frontend
npm install
npm run serve     # http://localhost:8090
npm run build     # production bundle in ./dist
```

## Configuration

Environment variables are read by Vue CLI from `.env*` files. Only
`VUE_APP_*` variables are exposed to the browser.

| Variable | Default | Used for |
|----------|---------|----------|
| `VUE_APP_API_BASE_URL` | `http://localhost:8080` | Gateway base URL prepended by `src/api/client.js`. |

The committed `.env.development` provides the default. Copy
`.env.example` to `.env.local` to override without touching tracked
files.

## Layout

```
src/
  api/client.js            shared fetch() wrapper + ApiError
  auth/token.js            JWT storage + claim decoding helpers
  components/AppNav.vue    top navigation bar
  router/index.js          routes + global beforeEach auth guard
  views/
    HomeView.vue           landing
    LoginView.vue          /api/auth/login (posted to teammate User Service via gateway)
    SignupView.vue         /api/auth/signup
    RestaurantListView.vue paged restaurant list (R19 read)
    RestaurantDetailView.vue single restaurant + status toggle (R20)
    AddRestaurantView.vue  owner-only create (R19)
    MenuView.vue           menu browse for a restaurant (R22)
    MenuItemDetailView.vue owner-gated edit + delete button (R21)
    AddMenuItemView.vue    owner-only create (R21)
    CartView.vue           placeholder for teammate-owned order flow
    OrderStatusView.vue    placeholder for teammate-owned order flow
    NotFoundView.vue       404
  App.vue                  root layout
  main.js                  bootstrap
```

## Auth flow

1. `LoginView` posts credentials to `/api/auth/login`.
2. The returned token (`token` / `accessToken` / `jwt`) is stored in
   `localStorage` under `quickbite.jwt`.
3. Every subsequent `apiFetch` call attaches
   `Authorization: Bearer <token>` automatically.
4. A 401 anywhere clears the token and redirects to
   `/login?next=<original-path>`.
5. The router guard mirrors that protection at navigation time for
   `meta.requiresAuth` routes.

The JWT is the same HS256 token minted by the User Service (or the
Postman pre-request mock during local testing); the frontend never
inspects the signature, only the unverified payload for role
decoding. See
[`../../../dev-docs/decisions/0010-auth-contract.md`](../../../dev-docs/decisions/0010-auth-contract.md)
for the token shape.

## For AI coding agents

- **Before adding a new endpoint call**, check that it is in
  [`../../../dev-docs/decisions/0020-sten-contracts.md`](../../../dev-docs/decisions/0020-sten-contracts.md)
  (for Sten's services) or owned by a teammate (in which
  case do not add it here without a teammate commitment on the
  shared contract).
- **Role gating goes through `auth/token.js`.** Do not duplicate
  decoding logic in views -- import `hasRole()`.
- **All API calls go through `api/client.js`.** A raw `fetch()` in a
  view bypasses the 401 interceptor and will misbehave on logout.
- **Placeholder views** (Cart, OrderStatus) are owned by teammates
  after integration. Do not wire them to Sten's services.
