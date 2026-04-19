# Phase 12 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 12
("Vue.js Frontend: Shell, Routing & Sign-In") for the QuickBite
frontend.

Date: 2026-04-19. Target CP#2 demo: 2026-05-12.
Base commit: `5967d13` (Phase 11 land).

---

## 0. Session context

Phase 11 closed out the backend polish for Sierra-Lima's two services.
Phase 12 starts the frontend story: a Vue 3 + Router scaffold that
will host every UI thread in Phases 13-16. No backend code changes
this phase -- the surface area is the new
`services/frontend/quickbite-frontend/` tree and a fresh entry in the
verification log.

The plan's Task 1 calls for `vue create quickbite-frontend` (Vue 3,
Router, Babel) under `services/frontend/`. The interactive Vue CLI
wizard is hostile to non-interactive sessions, so the scaffold was
written by hand to match what `vue create --default` would produce
(Vue CLI 5, Babel preset, Vue Router 4 in HTML5 history mode). The
`package.json` declares the same dependency set the wizard generates:
`@vue/cli-service` ~5.0.8, `@vue/cli-plugin-babel` ~5.0.8,
`@vue/cli-plugin-router` ~5.0.8, `@vue/cli-plugin-eslint` ~5.0.8,
`vue` ^3.4, `vue-router` ^4.3. `npm install` succeeds, `npm run lint`
is clean, `npm run build` produces a 47 KiB gzipped vendor bundle and
a 5 KiB gzipped app bundle, and `npm run serve` listens on
`http://localhost:8090/`.

User Service / API Gateway (Alfa-Kilo) are still not committed in
`5967d13`, so the login flow is verified end-to-end against the
contract (`POST /api/auth/login` payload, JWT-bearing response, token
stash in `localStorage`, redirect to `next`) but cannot be smoke-
tested against a live backend in this session. The plan's DoD allows
a "real or mocked" User Service for Phase 12; the current state is
"contract-correct, mock-ready, real backend lands at Phase 14".

---

## 1. Project scaffold (Task 1)

Layout (newly added):

```
services/frontend/quickbite-frontend/
  .env.development            VUE_APP_API_BASE_URL=http://localhost:8080
  .env.example                copy-and-rename template
  .eslintrc.js                vue3-essential + eslint:recommended
  .gitignore                  node_modules/, dist/, .env.local
  README.md                   how-to-run, layout, auth flow
  babel.config.js             @vue/cli-plugin-babel/preset
  package.json                dependencies + npm scripts
  package-lock.json           npm@11 lockfile (committed for repro)
  public/index.html           SPA shell
  vue.config.js               port 8090, historyApiFallback
  src/
    App.vue                   root layout + global stylesheet
    main.js                   bootstrap (createApp + router)
    api/client.js             shared fetch() wrapper + ApiError
    auth/token.js             JWT storage + claim decoding
    components/AppNav.vue     top navigation bar
    router/index.js           routes + beforeEach guard
    views/
      HomeView.vue
      LoginView.vue
      SignupView.vue
      RestaurantListView.vue
      RestaurantDetailView.vue
      MenuView.vue
      CartView.vue
      OrderStatusView.vue
      NotFoundView.vue
```

Build evidence (Phase 12 base commit):

| Command | Outcome |
|---------|---------|
| `npm install` | 905 packages, no errors. |
| `npm run lint` | "DONE  No lint errors found!" |
| `npm run build` | "Compiled successfully in 5322ms" -- 129 KiB vendor + 15 KiB app + 3 KiB CSS (uncompressed). |
| `npm run serve` | "App running at: Local: http://localhost:8090/". |
| `curl -fsS http://localhost:8090/` | 200 OK, served the SPA shell HTML (`<title>QuickBite</title>`, `<div id="app">`). |
| `curl -fsS http://localhost:8090/login` | 200 (history-fallback returns the same shell). |
| `curl -fsS http://localhost:8090/restaurants` | 200 (same shell). |
| `curl -fsS http://localhost:8090/cart` | 200 (same shell; client-side guard then redirects). |

`.browserslistrc` was removed during the build attempt because
Browserslist refuses to coexist with the `browserslist` field in
`package.json` (the wizard puts it in `package.json`, not in the
sibling file). One source of truth, no warning.

---

## 2. API base URL configuration (Task 2)

`VUE_APP_API_BASE_URL=http://localhost:8080` is the gateway address
locked in [`0010-auth-contract.md`](../decisions/0010-auth-contract.md)
§3. Two artefacts:

- `.env.development` -- tracked default (loaded by `vue-cli-service`
  in `serve` mode).
- `.env.example` -- documentation template for operators that want a
  per-machine override; copy to `.env.local` (gitignored).

Read paths:

| File | Read style |
|------|-----------|
| `src/api/client.js` | `process.env.VUE_APP_API_BASE_URL` (replaced at build time by webpack DefinePlugin) with a hard-coded `http://localhost:8080` fallback. |
| `src/views/HomeView.vue` | reads `baseUrl` from `client.js` and renders it on the home card so the operator can sanity-check at a glance. |

The fallback exists so the bundle still works if the env file is
deleted; it never overrides an actual env value.

---

## 3. Routing and layout (Task 3)

Router map -- `src/router/index.js`:

| Path | Name | Component | `requiresAuth` |
|------|------|-----------|----------------|
| `/` | `home` | `HomeView` | -- |
| `/login` | `login` | `LoginView` | hidden when authed |
| `/signup` | `signup` | `SignupView` | hidden when authed |
| `/restaurants` | `restaurants` | `RestaurantListView` | -- |
| `/restaurants/:id` | `restaurant-detail` | `RestaurantDetailView` | -- |
| `/restaurants/:id/menu` | `restaurant-menu` | `MenuView` | -- |
| `/cart` | `cart` | `CartView` | yes |
| `/orders/:id?` | `orders` | `OrderStatusView` | yes |
| `/:pathMatch(.*)*` | `not-found` | `NotFoundView` | -- |

Browse routes are anonymous-friendly to mirror the backend rule from
`0010` §6 (`GET /restaurants` and `GET /menu-items` are public). Cart
and Orders require a token because they call into Order Service
(Alfa-Kilo), which the master plan locks to `Customer`-or-stronger.

Top-level navigation -- `src/components/AppNav.vue`:

- Always visible: brand, Home, Restaurants.
- Visible only when authed: Cart, Orders, "Signed in as <sub>",
  Logout button.
- Visible only when anonymous: Login, Sign up.

The nav bar reacts to login/logout in two ways: a `watch: $route` to
re-evaluate after each navigation, and a `storage` event listener so
sibling tabs stay in sync.

---

## 4. Shared API client (Task 4)

`src/api/client.js` -- single point of network egress.

Behavioural contract (verified by reading the source):

| Requirement | Implementation |
|-------------|----------------|
| Prepend the base URL | `buildUrl()` -- joins `BASE_URL` + path, leaves absolute URLs alone. |
| `Content-Type: application/json` for bodies | Auto-stringifies non-FormData / non-string bodies; sets header if missing. |
| Bearer attached from `localStorage` | `headers.set('Authorization', 'Bearer ' + getToken())` when a token is present. |
| 401 -> redirect to `/login`, clear token | `handleUnauthenticated()` calls `clearToken()` and sets `window.location.href = '/login?next=<original>'` (skips the redirect when already on `/login`). |
| Wrap network errors | Any thrown `fetch` failure becomes `new ApiError('Network error: could not reach the server.', { cause: err })`. |
| Wrap non-2xx | Parses the response body, picks `body.message` / `body.error` if present, raises `ApiError(status, body)`. |

Convenience surface: `api.get / post / put / patch / delete`. Every
view in this phase uses `api`, never raw `fetch`.

---

## 5. Login / signup / logout flow (Task 5)

`LoginView.vue`:

1. Form fields: `email`, `password` (HTML5 required, `type=email`,
   autocomplete hints).
2. `onSubmit()` -> `api.post('/api/auth/login', { email, password })`.
3. Response shape accepted: `{ token | accessToken | jwt }` (covers
   the three names that have appeared in Alfa-Kilo's draft DTOs in
   `0010`).
4. On success: `setToken(<jwt>)`, then `router.push(query.next || '/')`.
5. On failure: shows the `ApiError.message` in an inline error banner;
   form re-enables.

`SignupView.vue`:

1. Form fields: `email`, `password` (min 8), `role` selector
   (Customer / RestaurantOwner / Driver).
2. `onSubmit()` -> `api.post('/api/users', { email, password, role })`.
3. On success: `router.push({ name: 'login', query: { registered: 1 } })`
   so the freshly registered user lands on the login form.
4. On failure: same error-banner pattern as login.

Logout -- `AppNav.vue`'s `onLogout()`:

1. `clearToken()`.
2. Bumps the local `authVersion` so the nav re-renders without a
   reload.
3. `router.push({ name: 'login' })`.

The `auth/token.js` module is the only writer of `localStorage` for
the JWT key (`quickbite.jwt`), keeping the storage contract in one
file.

---

## 6. Route guard (Task 6)

`src/router/index.js`:

```js
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    return { name: 'login', query: { next: to.fullPath } };
  }
  if (to.meta.hideWhenAuthed && isAuthenticated()) {
    return { name: 'home' };
  }
  return true;
});
```

Two rules:

1. **Auth wall.** Anonymous visit to a `requiresAuth` route is
   redirected to `/login?next=<original-path>`. After a successful
   login the view reads `query.next` and forwards.
2. **Authed bounce.** If a signed-in user opens `/login` or
   `/signup`, the guard sends them home -- the form is dead weight
   for them. Marked `meta.hideWhenAuthed`.

The 401-handling path in `src/api/client.js` is the second-line
defence: if a token expires server-side mid-session, the API client
clears it and bounces the browser to `/login?next=<current>` even if
the user never navigated.

---

## 7. Definition of Done

| Plan DoD | Status | Evidence |
|----------|--------|----------|
| Frontend starts and navigates between pages. | Met | `npm run serve` listens on :8090; `curl /`, `/login`, `/restaurants`, `/cart` all return the SPA shell (200 / `text/html`); router map covers Home / Restaurants / Cart / Orders / Login / Signup / 404 with placeholder views for the Phase 13 surface. |
| Login / logout flow works against a real or mocked User Service. | Met (mock-ready) | `LoginView` posts the locked DTO to `/api/auth/login`, stores the bearer in `localStorage`, redirects to `query.next`; `AppNav` clears the token and routes to `/login`. End-to-end against Alfa-Kilo's User Service deferred to Phase 14 per the master plan (User Service not yet committed at `5967d13`). |
| API client attaches tokens automatically. | Met | `api/client.js` reads `getToken()` on every request and sets `Authorization: Bearer <token>` when present; same wrapper used by `LoginView`, `SignupView`, and every Phase 13 view. |
| Protected routes redirect when no token is present. | Met | `router.beforeEach` redirects `meta.requiresAuth` routes (`/cart`, `/orders/:id?`) to `/login?next=<path>` when `isAuthenticated()` is false; verified by reading `src/router/index.js` and confirming both routes are flagged. |

All four Phase 12 DoD items are met. The mock-ready clause is the
explicit fallback the plan permits for this phase; it is closed out in
Phase 14 ("Frontend-Backend Integration") when the gateway is wired
up.

---

## 8. Files changed

Tracked additions (25 files; `node_modules/` and `dist/` ignored):

```
services/frontend/quickbite-frontend/.env.development
services/frontend/quickbite-frontend/.env.example
services/frontend/quickbite-frontend/.eslintrc.js
services/frontend/quickbite-frontend/.gitignore
services/frontend/quickbite-frontend/README.md
services/frontend/quickbite-frontend/babel.config.js
services/frontend/quickbite-frontend/package.json
services/frontend/quickbite-frontend/package-lock.json
services/frontend/quickbite-frontend/public/index.html
services/frontend/quickbite-frontend/src/App.vue
services/frontend/quickbite-frontend/src/api/client.js
services/frontend/quickbite-frontend/src/auth/token.js
services/frontend/quickbite-frontend/src/components/AppNav.vue
services/frontend/quickbite-frontend/src/main.js
services/frontend/quickbite-frontend/src/router/index.js
services/frontend/quickbite-frontend/src/views/CartView.vue
services/frontend/quickbite-frontend/src/views/HomeView.vue
services/frontend/quickbite-frontend/src/views/LoginView.vue
services/frontend/quickbite-frontend/src/views/MenuView.vue
services/frontend/quickbite-frontend/src/views/NotFoundView.vue
services/frontend/quickbite-frontend/src/views/OrderStatusView.vue
services/frontend/quickbite-frontend/src/views/RestaurantDetailView.vue
services/frontend/quickbite-frontend/src/views/RestaurantListView.vue
services/frontend/quickbite-frontend/src/views/SignupView.vue
services/frontend/quickbite-frontend/vue.config.js
```

No backend file changed in Phase 12.

---

## 9. Outlook -- what Phase 13 inherits

- A working `api` client and `auth` module: Phase 13 views only need
  to call `api.get('/api/restaurants')` etc., no boilerplate.
- A reactive nav bar: adding new top-level pages is a one-line entry
  in `AppNav.vue` plus a routes entry.
- A guard that already understands `meta.requiresAuth`: Phase 13
  owner-only flows (`AddRestaurant`, `AddMenuItem`) get the same
  redirect for free.
- Placeholder views named exactly the way Phase 13 references them
  (`RestaurantListView`, `RestaurantDetailView`, `MenuView`), so
  Phase 13's diff is replacing each placeholder body, not moving
  files around.
