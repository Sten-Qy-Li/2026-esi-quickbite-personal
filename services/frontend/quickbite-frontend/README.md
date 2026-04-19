# quickbite-frontend

Vue.js 3 frontend for the QuickBite food-delivery platform.
Created in **Phase 12** of the Sierra-Lima master plan
(`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`).
Owned by Sierra-Lima.

## What this phase ships

- Vue 3 + Vue Router 4 + Babel scaffold (Vue CLI 5).
- Top-level navigation: Home, Restaurants, Cart, Orders, Login / Sign up.
- Placeholder views for everything that lands in Phase 13+.
- Shared `api/` client that prepends the gateway base URL, attaches
  the JWT bearer from `localStorage`, and bounces 401s back to
  `/login`.
- `auth/token.js` -- the single source of truth for the bearer token.
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
  views/                   page-level components (one per route)
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
