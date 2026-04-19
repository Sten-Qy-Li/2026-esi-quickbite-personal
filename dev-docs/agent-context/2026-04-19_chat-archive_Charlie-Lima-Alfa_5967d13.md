# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`5967d13`)

## Session Summary

This session executed **Phase 12 -- Vue.js Frontend: Shell, Routing
& Sign-In** for the QuickBite frontend, as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
Phase 12 (lines 1350-1396).

The session began on top of `5967d13` ("Land Phase 11 backend polish
and Checkpoint #1 prep"). No mid-session compaction occurred; the run
is a single autonomous execution of the Phase 12 task list plus the
archive-and-commit at the end.

Phase 12 is a frontend-only phase. The QuickBite repo did not contain
a `services/frontend/` tree before this session. After this session
it contains a Vue 3 + Vue Router 4 + Babel scaffold under
`services/frontend/quickbite-frontend/` whose:

1. `npm install` succeeds (905 packages, no errors).
2. `npm run lint` is clean.
3. `npm run build` produces a 47 KiB gzipped vendor bundle and a
   5 KiB gzipped app bundle.
4. `npm run serve` listens on `http://localhost:8090/` and returns
   the SPA shell HTML for `/`, `/login`, `/restaurants`, and `/cart`
   (history fallback works).
5. `src/api/client.js` shared `fetch()` wrapper attaches the JWT
   bearer from `localStorage`, prepends the gateway base URL, sets
   JSON content type, and bounces 401s back to `/login?next=<path>`.
6. `src/router/index.js` `beforeEach` guard redirects anonymous
   visitors away from `meta.requiresAuth` routes (`/cart`,
   `/orders/:id?`).
7. `LoginView`, `SignupView`, and the `AppNav` logout button cover
   the full session lifecycle.

User Service / API Gateway (Alfa-Kilo) are still not committed in
`5967d13`, so the login flow is verified contract-correct against the
locked DTO shape (`POST /api/auth/login` -> `{ token | accessToken |
jwt }`) but not against a live backend. The plan's DoD allows "real
or mocked" for Phase 12; the live wire-up lands in Phase 14.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`,
  and starting this phase, the `Frontend` (lives at
  `services/frontend/quickbite-frontend/`).
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Parent commit: `5967d13` -- "Land Phase 11 backend polish and
  Checkpoint #1 prep"
- Environment: Windows 11 + IntelliJ IDEA 2026.1 + Git Bash
- Node.js: v24.13.1
- npm: 11.8.0
- Browser target: Vue CLI 5 default (`> 1%`, `last 2 versions`, `not
  dead`, `not ie 11`)

## User Requests

1. Initial request: *"Hi Claude, please work on Phase 12 of the master
   plan `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.
   After completing the implementation of Phase 12, please archive
   the session context to `dev-docs/agent-context`, and then commit
   all changes and push (try to commit and push the entire local
   repository; exclude files only if there's a very good reason,
   according to your best judgement). Thanks!"*

No mid-session corrections or redirections. The chat was a single
autonomous run.

## Phase 12 Task-by-Task Record

### Task 1 -- Scaffold the Vue project

The plan calls for `vue create quickbite-frontend` with Vue 3,
Router, and Babel selected, placed under `services/frontend/`. The
interactive `@vue/cli` wizard cannot be driven non-interactively in
this session, so the scaffold was written by hand to be byte-equivalent
to what the wizard's `--default` preset produces:

- `package.json` with `vue` ^3.4, `vue-router` ^4.3, and the four
  Vue CLI 5 plugins (`babel`, `eslint`, `router`, plus `cli-service`
  itself).
- `babel.config.js` extending `@vue/cli-plugin-babel/preset`.
- `vue.config.js` setting `devServer.port = 8090` (so `npm run serve`
  satisfies the plan's "verify `npm run serve` works at
  `http://localhost:8090`" sub-task without an extra `--port` flag).
- `.eslintrc.js` -- `vue3-essential` + `eslint:recommended`.
- `public/index.html` -- the standard Vue CLI shell, with
  `<title>QuickBite</title>`.
- `src/main.js` -- `createApp(App).use(router).mount('#app')`.
- `src/App.vue` -- root layout with the global stylesheet (CSS
  variables for the brand palette, used by every view).

Initial build attempt failed with
`BrowserslistError: contains both .browserslistrc and package.json
with browsers`. The fix was to delete `.browserslistrc` -- the
wizard puts the browser list in `package.json` (under the
`browserslist` key), not in a sibling file. Single source of truth
restored, build passed.

Build evidence:

```
$ npm install --no-audit --no-fund
added 905 packages in 1m

$ npm run lint -- --no-fix
DONE  No lint errors found!

$ npm run build
DONE  Compiled successfully in 5322ms
  dist\js\chunk-vendors.6dcee5ab.js    129.39 KiB    46.37 KiB gzipped
  dist\js\app.6f19b293.js               14.91 KiB     4.90 KiB gzipped
  dist\css\app.e1d16d67.css              2.67 KiB     0.99 KiB gzipped

$ npm run serve   (background)
INFO  Starting development server...
DONE  Compiled successfully in 4078ms
App running at:
- Local:   http://localhost:8090/
- Network: http://172.31.144.21:8090/

$ curl -fsS http://localhost:8090/      ->  200, SPA shell
$ curl -fsS http://localhost:8090/login ->  200, SPA shell (history fallback)
$ curl -fsS http://localhost:8090/restaurants -> 200
$ curl -fsS http://localhost:8090/cart  ->  200 (client-side guard then redirects)
```

### Task 2 -- API base URL via env

`VUE_APP_API_BASE_URL=http://localhost:8080` -- locked in
`0010-auth-contract.md` §3 as the gateway address.

Two artefacts:

- `services/frontend/quickbite-frontend/.env.development` -- tracked
  default, loaded automatically by `vue-cli-service serve`.
- `services/frontend/quickbite-frontend/.env.example` -- copy-and-
  rename template for per-machine overrides; instructs the operator
  to copy to `.env.local`, which the local `.gitignore` excludes.

`src/api/client.js` reads `process.env.VUE_APP_API_BASE_URL` (Vue
CLI 5 inlines that at build time via webpack DefinePlugin) with a
hard-coded `http://localhost:8080` fallback so a missing env file
doesn't blank the bundle.

`src/views/HomeView.vue` echoes the base URL on the home card -- a
sanity check the operator can read at a glance during the demo.

### Task 3 -- Route map and layout

`src/router/index.js`:

| Path | Name | Component | Auth |
|------|------|-----------|------|
| `/` | `home` | `HomeView` | -- |
| `/login` | `login` | `LoginView` | hide when authed |
| `/signup` | `signup` | `SignupView` | hide when authed |
| `/restaurants` | `restaurants` | `RestaurantListView` | -- |
| `/restaurants/:id` | `restaurant-detail` | `RestaurantDetailView` | -- |
| `/restaurants/:id/menu` | `restaurant-menu` | `MenuView` | -- |
| `/cart` | `cart` | `CartView` | requires |
| `/orders/:id?` | `orders` | `OrderStatusView` | requires |
| `/:pathMatch(.*)*` | `not-found` | `NotFoundView` | -- |

Browse is open per `0010` §6 (backend `GET /restaurants` and
`GET /menu-items` are public). Cart and Orders gate on a token
because they call into Order Service, which is `Customer`-or-
stronger.

`src/components/AppNav.vue` exposes the top-level surface:

- Always: brand link, Home, Restaurants.
- Authed only: Cart, Orders, "Signed in as <sub>", Logout button.
- Anonymous only: Login, Sign up.

The nav re-renders after each navigation (via `watch: $route`) and
across browser tabs (via the `storage` event listener) so a logout
in one tab updates the others.

Placeholder views (`RestaurantListView`, `RestaurantDetailView`,
`MenuView`, `CartView`, `OrderStatusView`, `NotFoundView`) are
deliberately thin; Phase 13 replaces their bodies, but Phase 12
needs the route shells for the navigation DoD ("Frontend starts and
navigates between pages").

### Task 4 -- Shared API client utility

`src/api/client.js` is a single-purpose module exporting:

- `apiFetch(path, options)` -- the workhorse.
- `api` -- thin convenience surface (`get/post/put/patch/delete`).
- `ApiError` -- typed error with `status` and `body` so views can
  present the server's error message verbatim.
- `baseUrl` -- the resolved env value, exported for the home card.

Behaviour:

| Requirement | Where |
|-------------|-------|
| Prepend the base URL | `buildUrl()` -- absolute URLs pass through unchanged. |
| `Content-Type: application/json` for bodies | Auto-stringifies non-FormData / non-string bodies, sets the header if missing. |
| `Authorization: Bearer <token>` from `localStorage` | `headers.set('Authorization', 'Bearer ' + getToken())` when present. |
| 401 -> redirect to `/login` and clear token | `handleUnauthenticated()` calls `clearToken()` and sets `window.location.href = '/login?next=<original>'` (skips the redirect when already on `/login`). |
| Wrap network errors | `try { fetch(...) } catch (err) { throw new ApiError('Network error: could not reach the server.', { cause: err }); }` |
| Wrap non-2xx | Parses response body, picks `body.message` / `body.error` if present, raises `ApiError(status, body)`. |

`src/auth/token.js` is the only place that touches `localStorage`
for the JWT. It exposes `getToken`, `setToken`, `clearToken`,
`isAuthenticated`, and `readClaims` (Base64-URL safe payload decoder
used only for the "Signed in as <sub>" label -- never for trust).

### Task 5 -- Login flow

`LoginView.vue`:

1. Email + password fields with HTML5 validation (`required`,
   `type=email`, `autocomplete=current-password`).
2. `onSubmit()` -> `api.post('/api/auth/login', { email, password })`.
3. Accepts `{ token | accessToken | jwt }` -- covers the three names
   that have appeared in Alfa-Kilo's draft DTOs.
4. On success: `setToken(<jwt>)`, then
   `router.push(query.next || '/')` (so the post-auth bounce lands
   the user on whatever route originally triggered the redirect).
5. On failure: shows `ApiError.message` in an inline error banner;
   form re-enables.

`SignupView.vue`:

1. Email, password (min 8), role selector.
2. `onSubmit()` -> `api.post('/api/users', { email, password,
   role })`.
3. On success: `router.push({ name: 'login', query: {
   registered: 1 } })`.
4. On failure: same error-banner pattern.

Logout in `AppNav.vue`'s `onLogout()`:

1. `clearToken()`.
2. Bumps the local `authVersion` so the nav re-renders without a
   page reload.
3. `router.push({ name: 'login' })`.

### Task 6 -- Route guards

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
   redirected to `/login?next=<original-path>`. After successful
   login, `LoginView` reads `query.next` and forwards.
2. **Authed bounce.** A signed-in user opening `/login` or
   `/signup` is sent home -- the form is dead weight for them.

The 401 handler in `api/client.js` is the second-line defence: a
mid-session token expiry triggers the same redirect even if the user
never navigated.

## Verification

Phase 12 verification document written at
`dev-docs/verification/phase-12-verification_Charlie-Lima-Alfa.md`.
Sections:

- §0 Session context -- why the wizard was bypassed and what backend
  state the phase ships against.
- §1 Project scaffold -- file layout + the four `npm` commands as
  evidence.
- §2 API base URL configuration.
- §3 Routing and layout -- full route table + nav rules.
- §4 Shared API client -- behavioural contract table.
- §5 Login / signup / logout flow.
- §6 Route guard.
- §7 Definition of Done -- four-row roll-up; all four "Met".
- §8 Files changed -- 25 tracked files, `node_modules/` and
  `dist/` ignored.
- §9 Outlook -- what Phase 13 inherits.

## Files Created / Modified

Tracked additions (25 files):

```
services/frontend/quickbite-frontend/
  .env.development
  .env.example
  .eslintrc.js
  .gitignore
  README.md
  babel.config.js
  package.json
  package-lock.json
  public/index.html
  vue.config.js
  src/App.vue
  src/main.js
  src/api/client.js
  src/auth/token.js
  src/components/AppNav.vue
  src/router/index.js
  src/views/CartView.vue
  src/views/HomeView.vue
  src/views/LoginView.vue
  src/views/MenuView.vue
  src/views/NotFoundView.vue
  src/views/OrderStatusView.vue
  src/views/RestaurantDetailView.vue
  src/views/RestaurantListView.vue
  src/views/SignupView.vue

dev-docs/verification/phase-12-verification_Charlie-Lima-Alfa.md
dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_5967d13.md   (this file)
```

Ignored by git (correct): `services/frontend/quickbite-frontend/{node_modules,dist}/`.

No backend file changed in Phase 12.

## Tools / Commands Used

- `Bash`: `npm install` (1m), `npm run build` (5s), `npm run lint`
  (sub-second), `npm run serve` (background; killed before commit),
  `curl` probes against `:8090`, `git status / ls-files`.
- `Monitor`: polled `:8090` until the dev server replied 200 (one
  event: "READY: dev server responding on :8090").
- `Read` / `Write` / `Edit`: scaffold files, verification doc,
  archive.
- `Grep`: located the Phase 12 section in the master plan.
- `TaskCreate` / `TaskUpdate`: nine-task tracker for the phase
  (read-spec, scaffold, configure-env, api-client, login-flow,
  route-guard, verify-DoD, archive, commit-and-push).

## Open Items for Future Sessions

1. **Phase 13 -- Vue.js Frontend: Restaurant & Menu UX.** Replace
   each placeholder view with the real CRUD surface against the
   Sierra-Lima backend. The shells are named exactly the way the
   plan references them, so the diff is per-view body changes plus a
   small router additions list (`/restaurants/new`, `/menu-items/new`,
   `/menu-items/:id`).
2. **Phase 14 -- Frontend-Backend Integration & Checkpoint #2 Prep.**
   First wire-up against a live API Gateway. The login flow's
   "mocked" status closes here, and the gateway CORS contract gets
   verified.
3. **Per-machine env override.** The `.env.example` template names
   `.env.local` for operator overrides; gitignored. Document this in
   the team runbook if the team picks up the frontend before Phase
   14.
4. **Favicon / branding asset.** The `public/index.html` shell does
   not reference a favicon; Vue CLI's wizard ships one but bypassing
   the wizard skipped it. Add when the team picks a brand visual; no
   functional impact on Phase 12 DoD.

## Notes for Future Claude

- Frontend lives at `services/frontend/quickbite-frontend/`. The
  parent dir `services/frontend/` exists only as a namespace -- if a
  second SPA appears later, it slots in alongside.
- `package-lock.json` is committed (npm convention). Re-running
  `npm install` on a different machine should produce an identical
  tree.
- The frontend's own `.gitignore` excludes `node_modules/` and
  `dist/`. The repo-level `.gitignore` does not need to change.
- Vue CLI 5 reads env files: `.env`, `.env.local`, `.env.<mode>`,
  `.env.<mode>.local`. Only `.env.development` (mode=`development`,
  used by `serve`) and `.env.example` (template) are tracked. The
  `.local` variants are gitignored.
- The dev server log writes ANSI escapes to stdout (the `[2K[1A`
  sequences in the captured output). That is webpack's progress
  reporter, not corruption.
- Browserslist refuses two configs simultaneously. The wizard
  produces the `browserslist` key in `package.json`; do not add a
  sibling `.browserslistrc` -- the build will fail with the same
  error as Task 1's first attempt.
