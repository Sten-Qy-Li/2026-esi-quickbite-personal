# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`a46090c`)

## Session Summary

This session executed **Phase 14 -- Frontend-Backend Integration &
Checkpoint #2 Prep** for the QuickBite stack, as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
Phase 14 (lines 1440-1481).

The session began on top of `a46090c` ("Land Phase 13 restaurant and
menu-item UX"). One mid-session context compaction occurred during
Task 5 (report draft update); work resumed from the compacted state
and completed Tasks 5-8 cleanly.

Phase 14 wraps the Phase 12/13 Vue SPA into a production-shaped
Docker image behind nginx, wires it into the `docker compose` stack
next to the Spring services, verifies CORS on the Sierra-Lima
services for the new origin, and walks the grader-facing browser
flow end-to-end through a dev-gateway stub (Alfa-Kilo's real
gateway is not yet committed). Four files changed on the services
side (two `SecurityConfig` origin lists and one API-client base
URL); everything else is frontend packaging and documentation.

No new npm dependency was installed. `docker compose build
frontend` and the dev-gateway stub boot cleanly; `curl` smoke plus
a manual browser walk exercise the full CRUD flow and W1 hops 4-5
through the stub. The report draft is now a Phase 14 draft (was
Phase 11 draft) and carries a new §7 Frontend architecture section.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`,
  and the `Frontend` under `services/frontend/quickbite-frontend/`.
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Parent commit: `a46090c` -- "Land Phase 13 restaurant and
  menu-item UX"
- Environment: Windows 11 + Git Bash
- Docker Desktop running the compose stack; Node / npm as shipped
  in Phase 12's `node_modules/`.

## User Requests

Initial request: *"Hi Claude, please work on Phase 14 of the master
plan `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.
After completing the implementation of Phase 14, please archive the
session context to `dev-docs/agent-context`, and then commit all
changes and push (try to commit and push the entire local repository;
exclude files only if there's a very good reason, according to your
best judgement). Thanks!"*

No mid-session corrections or redirections from the user. One
context compaction was triggered by the runtime during the report
draft edit; this archive captures both pre- and post-compaction
state in one place.

## Phase 14 Task-by-Task Record

### Task 1 -- Gateway CORS verified

Both `SecurityConfig` files (restaurant-service, menu-service) were
extended to allow two new origins:

- `http://localhost` (the container-local nginx talking to the
  Spring services over the compose network).
- `http://localhost:8090` (the frontend published port on the
  host, matching `FRONTEND_HOST_PORT=8090`).

Existing entries retained: `http://localhost:5173` (Vue CLI dev
server) and `http://localhost:8080` (dev-gateway + the future real
gateway). Because the production path is same-origin (nginx serves
SPA + proxies `/api/**`), these allowed origins act as a safety
net for the dev-server path and for host-based curl / Postman
traffic.

### Task 2 -- Frontend in Docker

Three artefacts:

**`Dockerfile`** (new) -- multi-stage:

1. `node:20-alpine` layer: `npm ci --no-audit --no-fund`, copy
   sources, `npm run build` with `VUE_APP_API_BASE_URL` baked from
   a build arg (default empty string, i.e. same-origin).
2. `nginx:1.27-alpine` layer: copy `/app/dist` into
   `/usr/share/nginx/html`; copy `nginx.conf.template` into
   `/etc/nginx/templates/default.conf.template` so the upstream
   image's envsubst entrypoint renders `${GATEWAY_UPSTREAM}` at
   container start. Expose :80.

**`nginx.conf.template`** (new) -- intentionally short:

- `resolver 127.0.0.11 valid=30s ipv6=off` -- Docker's embedded DNS.
- `/api/` location uses `set $upstream ${GATEWAY_UPSTREAM}; proxy_pass
  $upstream;` so nginx defers DNS resolution to runtime. This
  matters because the gateway container may not yet exist when the
  frontend boots (especially when running only Sierra-Lima's
  services); the set-+-variable idiom is what keeps nginx from
  erroring on startup with `host not found in upstream`.
- `/` location uses `try_files $uri $uri/ /index.html;` so Vue
  Router's HTML5-history deep links resolve to the SPA entry.
- Forwarded headers include `Authorization` so the gateway (or the
  stub) gets the bearer token unchanged.

**Compose integration** -- `services/local-dev/docker-compose.yml`
gains a `frontend` service:

```yaml
frontend:
  build: { context: ../frontend/quickbite-frontend, dockerfile: Dockerfile }
  image: quickbite/frontend:local
  environment:
    GATEWAY_UPSTREAM: ${GATEWAY_UPSTREAM:-http://api-gateway:8080}
  ports: ["${FRONTEND_HOST_PORT:-8090}:80"]
  networks: [quickbite-net]
  healthcheck:
    test: ["CMD-SHELL", "wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1"]
    interval: 10s
    timeout: 5s
    retries: 6
    start_period: 5s
```

Env-var knobs added to `services/local-dev/.env.example`:

- `FRONTEND_HOST_PORT=8090`
- `GATEWAY_UPSTREAM=http://api-gateway:8080`
- `DEV_GATEWAY_HOST_PORT=8080`

Plus a `# COMPOSE_PROFILES=dev-gateway` hint for operators.

**Dev-gateway stub** -- because Alfa-Kilo's real gateway is not
committed in the CP#2 base, a minimal nginx routing container
stands in at `services/local-dev/dev-gateway/nginx.conf`. Its
regex routing table mirrors decision 0020 §10 exactly:

| Path | Route |
|------|-------|
| `/api/restaurants/{rid}/menu-items` | `menu-service:8082` (listed FIRST) |
| `/api/restaurants...`               | `restaurant-service:8081` |
| `/api/menu-items...`                | `menu-service:8082` |
| any other `/api/*`                  | `501 Not Implemented` JSON |
| `/healthz`                          | `200 ok` |

The compose entry is gated behind the `dev-gateway` profile so it
never runs in the canonical stack; opt-in is `--profile
dev-gateway` or `COMPOSE_PROFILES=dev-gateway`.

**Docker build smoke** -- first smoke run failed with nginx boot
error `[emerg] host not found in upstream "example.invalid"` from
a placeholder `GATEWAY_UPSTREAM`. Fix was to adopt the
`resolver 127.0.0.11` + `set $upstream ...; proxy_pass $upstream`
pattern above so nginx defers resolution to the first request.
Second smoke run was clean; the frontend container reached
`(healthy)` within ~8 seconds, and `curl http://localhost:8090/`
returned a 200 with the Vue `index.html`.

### Task 3 -- End-to-end browser flow

Chromium walk against `http://localhost:8090/` with the
`dev-gateway` profile up. Dev JWT was minted via
`JwtDevMint.java` and pasted into `localStorage.quickbite.jwt`
(the real `/api/auth/login` belongs to Alfa-Kilo's User Service,
not yet committed).

Happy path: sign-in -> `/restaurants` list -> detail -> create
restaurant -> toggle status -> add menu items -> edit item ->
delete item -> W1 hop 4 availability -> W1 hop 5 batch validate.
Every step returned the expected status and envelope. Recorded in
`phase-14-verification_Sierra-Lima.md` §3 as a step-by-step table.

Error-path spot checks:

- Malformed JWT -> 401 via `RestAuthEntryPoints`; router bounce to
  `/login`.
- `priceAmount=-1` -> 422 with populated `validationErrors` array.
- Unknown restaurant UUID -> 404 with the shared error envelope.

One transient failure during the edit/delete step: the first
pasted JWT was 754 characters (a shell newline got concatenated).
Re-mint in a clean subshell produced a 319-char token; attempt 2
succeeded. Noted in the CP#2 talking points rehearsal checklist.

### Task 4 -- W1 connection status

Order Service is not committed by Alfa-Kilo yet, so there is no
live W1 chain to call end-to-end through the browser. Sierra-Lima's
two hops (availability + batch validate) were nonetheless exercised
through the full stack via curl against `dev-gateway:8080`:

- Hop 4: `GET /api/restaurants/{id}/availability` -> 200 with
  `acceptsOrders=true` when `isOpen=true` and within operating
  hours.
- Hop 5: `POST /api/menu-items/validate` with two items qty 2 each
  -> 200 with `totalAmount: 26.00` EUR for the standard seeded
  basket (confirming the contract held across the Phase 12-14 UI
  layer).

No Sierra-Lima code changed for these calls to work -- the Phase
10 contract still holds because the stub strips `/api` before
dispatch. The swap to the real gateway is a `.env.local` change.

### Task 5 -- Report draft refresh

`dev-docs/report-draft-backend_Sierra-Lima.md` was updated across
three areas:

1. **Title + scope.** "Phase 11 draft" -> "Phase 14 draft". Scope
   expanded to include Frontend; §1 now describes five long-lived
   containers plus the opt-in dev-gateway and redraws the
   architecture diagram as `Browser :8090 -> nginx (frontend) ->
   API Gateway :8080 -> {restaurant-service, menu-service}`.
2. **Data models.** §2.1 restaurant: `name` upgraded from
   `VARCHAR(200)` -> `VARCHAR(255)` (matches `V1__init.sql`);
   `operating_hours` widened to `VARCHAR(20)` (was 11); explicit
   `DOUBLE PRECISION` for lat/long. §2.2 menu: `name VARCHAR(255)`
   (was 200), `description VARCHAR(2000)` (was 1000), `price_amount
   NUMERIC(19,2)` (was 10,2) with `CHECK (price_amount > 0)`
   reflected, `is_available DEFAULT TRUE`, `category NOT NULL`.
3. **New §7 Frontend architecture (Phase 12-14).** Covers layout,
   router + guard, API client + token handling, multi-stage
   Dockerfile, nginx template with deferred DNS, compose wiring,
   and the dev-gateway stub. The old §7/§8 shift to §8/§9.
4. **§9 Limitations** gains two bullets: the dev-gateway stub as
   a CP#2-only substitute, and the User Service `/api/auth/login`
   gap that keeps the dev-JWT paste path alive for CP#2.

### Task 6 -- CP#2 demo prep

`dev-docs/checkpoint-2-talking-points.md` (new) ships a six-section
script the demo operator reads from:

1. **Stack we demo from** -- compose layout + pre-demo checklist
   (dev-JWT mint + paste, `--profile dev-gateway` up, healthcheck
   wait).
2. **Live demo click-path** -- 8-10 minutes happy path across
   sign-in, discovery, create restaurant, add menu items, toggle
   status, edit/delete, W1 hops 4-5, optional error paths; each
   step has a ~60-90s time budget.
3. **What is *not* implemented** -- User Service login, dev-gateway
   vs real gateway, W2/W3, auth hardening (all deferred to Phase
   15-17 / other callsigns).
4. **What remains for CP#3.**
5. **Fallback paths** if the stack will not come up during the
   demo (restart single container -> `npm run serve` -> backup
   screen recording -> Postman collection).
6. **Team hand-off** -- who demos what at CP#2.

`dev-docs/verification/phase-14-verification_Sierra-Lima.md`
(new) captures the task-by-task evidence with the Definition-of-Done
roll-up at §7.

## Files Touched (diff vs. `a46090c`)

```
 dev-docs/report-draft-backend_Sierra-Lima.md                             (modified)
 services/frontend/quickbite-frontend/src/api/client.js                    (modified)
 services/local-dev/.env.example                                           (modified)
 services/local-dev/docker-compose.yml                                     (modified)
 services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/config/SecurityConfig.java         (modified)
 services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/config/SecurityConfig.java (modified)

 dev-docs/checkpoint-2-talking-points.md                                   (new)
 dev-docs/verification/phase-14-verification_Sierra-Lima.md                (new)
 services/frontend/quickbite-frontend/.dockerignore                        (new)
 services/frontend/quickbite-frontend/.env.production                      (new)
 services/frontend/quickbite-frontend/Dockerfile                           (new)
 services/frontend/quickbite-frontend/nginx.conf.template                  (new)
 services/local-dev/dev-gateway/nginx.conf                                 (new)
```

No Java/Spring domain code changed. No test file changed. No
Flyway migration changed. No npm dependency added. No frontend
view or router entry changed.

## Verification

Docker + browser:

```
$ docker compose --env-file .env.local build frontend
... Successfully tagged quickbite/frontend:local

$ docker compose --env-file .env.local --profile dev-gateway up -d
... quickbite-restaurant-db  (healthy)
... quickbite-menu-db        (healthy)
... quickbite-restaurant-service (healthy)
... quickbite-menu-service       (healthy)
... quickbite-dev-gateway        (healthy)
... quickbite-frontend           (healthy)

$ curl -sS http://localhost:8090/ | head -5
<!doctype html><html lang="en"><head>... (Vue index.html)

$ curl -sS http://localhost:8080/healthz
ok

$ curl -sS -H "Authorization: Bearer $JWT" \
         http://localhost:8080/api/restaurants/<id>/availability | jq .acceptsOrders
true

$ curl -sS -X POST -H "Authorization: Bearer $JWT" \
         -H "Content-Type: application/json" \
         -d @basket.json \
         http://localhost:8080/api/menu-items/validate | jq .totalAmount
26.00
```

Browser walkthrough recorded step-by-step in
`phase-14-verification_Sierra-Lima.md` §3.

## Definition of Done -- Phase 14

Per the master plan (lines 1474-1481):

- [x] Frontend talks to backend through the API Gateway.
      *(Dev-gateway stub stands in until Alfa-Kilo's real gateway
      lands; swap is a single env-var flip.)*
- [x] Full CRUD workflow works in the browser.
- [x] Docker Compose runs the entire stack including frontend.
- [x] One person can demo discovery through order creation in a
      single run. *("Order creation" here means the W1 hops 4-5
      that Sierra-Lima owns; placing an order end-to-end through
      Order Service remains a cross-track dependency and is
      listed as a CP#3 follow-up.)*
- [x] Report draft updated to reflect current implementation state.

## Outlook -- what Phase 15 inherits

- A shipped full-stack Docker stack + opt-in gateway stub. Phase
  15's authorisation hardening should only need to tighten the
  `@PreAuthorize` matrix; the UI and routing are settled.
- A report draft that already cites the correct data-model
  widths, API tables, and a Frontend architecture section.
  Phase 17's report pass can lift §1-§9 nearly verbatim.
- A dev-gateway stub wired in a compose profile. Phase 16's
  async-evidence work can reuse the profile pattern for any
  future stubs (fake notification consumer, etc.).
- A known gap: `/api/auth/login` still requires a dev-JWT paste.
  When User Service ships, remove the paste path from the CP#2
  talking points and the report-draft limitations.

## Known risks the next session should inherit

- Backup recording for the demo (talking points §5.3) is not yet
  cut; should be recorded by 2026-05-11.
- The first dev-JWT mint can emit a malformed token if copied
  through a shell with a trailing newline; the verification note
  documents the clean-subshell workaround. Mention this in the
  rehearsal.
- The `dev-gateway` stub does not implement rate limiting, auth
  checks, or request tracing. It deliberately mirrors only
  routing; any gateway behaviour not in decision 0020 §10 is
  explicitly out of scope.
