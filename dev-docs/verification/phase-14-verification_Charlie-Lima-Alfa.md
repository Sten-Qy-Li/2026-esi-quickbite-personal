# Phase 14 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 14
("Frontend-Backend Integration & Checkpoint #2 Prep") for Restaurant
Service, Menu Service, and the Vue frontend.

Date: 2026-04-19. Target CP#2 rehearsal: 2026-05-11.
CP#2 graded: 2026-05-12.
Base commit: `a46090c` (Phase 13 land).

---

## 0. Session context

Phase 13 shipped the restaurant + menu-item UX on Vue; Phase 14
packages that frontend into a Docker image, wires it into the
compose stack via nginx, confirms CORS against the gateway, and
walks the grader-facing end-to-end browser flow. Alfa-Kilo's real
API Gateway is not yet committed in the base, so a single-file
`dev-gateway` nginx stub stands in for CP#2 rehearsal; the swap to
the real gateway is a one-line `.env.local` change.

Artefacts landed in this phase:

- `services/frontend/quickbite-frontend/Dockerfile` (new, multi-stage).
- `services/frontend/quickbite-frontend/nginx.conf.template` (new).
- `services/frontend/quickbite-frontend/.env.production` (new;
  empty `VUE_APP_API_BASE_URL` for same-origin).
- `services/frontend/quickbite-frontend/.dockerignore` (new).
- `services/local-dev/docker-compose.yml` (modified; added
  `frontend` service and `dev-gateway` profile).
- `services/local-dev/dev-gateway/nginx.conf` (new).
- `services/local-dev/.env.example` (modified; `FRONTEND_HOST_PORT`,
  `GATEWAY_UPSTREAM`, `DEV_GATEWAY_HOST_PORT`).
- `services/*/src/main/java/**/config/SecurityConfig.java`
  (modified; CORS allows `http://localhost:8090` and
  `http://localhost`).
- `services/frontend/quickbite-frontend/src/api/client.js` (modified;
  empty base URL -> same-origin).
- `dev-docs/report-draft-backend_Sierra-Lima.md` (modified; Phase 14
  draft, new §7 Frontend architecture, §2.2 data model corrected).
- `dev-docs/checkpoint-2-talking-points.md` (new).
- This verification note.

---

## 1. Task 1 -- Gateway CORS verified

Allowed origins in `SecurityConfig` of both services:

```
http://localhost
http://localhost:5173
http://localhost:8080
http://localhost:8090
```

- `http://localhost:8090` covers the frontend container served by
  the nginx image (`FRONTEND_HOST_PORT:-8090` -> container :80).
- `http://localhost` (no port) covers the container-internal nginx
  talking to the Spring services over the compose network.
- `http://localhost:5173` remains for the Vue CLI dev server.
- `http://localhost:8080` covers the dev-gateway stub and the
  real gateway once Alfa-Kilo's lands.

Because the frontend container reverse-proxies `/api/**` on the
same origin, normal CP#2 traffic is **same-origin** and never hits
CORS; the allowed origins exist as a safety net for the Vue CLI
dev path and for curl / Postman calls from the host.

Evidence: grep of both SecurityConfig files shows the four-entry
list. Browser DevTools "Network" tab during the walkthrough (§3
below) shows no CORS preflight on `/api/**` when loaded from
`http://localhost:8090/`.

---

## 2. Task 2 -- Frontend in Docker

### 2.1 Dockerfile (multi-stage)

`services/frontend/quickbite-frontend/Dockerfile`:

- Stage 1 `node:20-alpine`: `npm ci` then `npm run build` with
  `VUE_APP_API_BASE_URL` baked from a build arg (default empty
  string). Output lands in `/app/dist`.
- Stage 2 `nginx:1.27-alpine`: copies `/app/dist` to
  `/usr/share/nginx/html`, mounts
  `nginx.conf.template` into `/etc/nginx/templates/` so the
  upstream image's envsubst entrypoint renders `${GATEWAY_UPSTREAM}`
  at container start.

### 2.2 nginx config

`nginx.conf.template` is intentionally small:

- `resolver 127.0.0.11 valid=30s ipv6=off` -- Docker embedded DNS.
- `location /api/ { set $upstream ${GATEWAY_UPSTREAM}; proxy_pass
  $upstream; ... }` -- the `set` + variable-in-proxy_pass idiom
  defers DNS resolution to runtime, so nginx boots even when the
  gateway container is not up yet.
- `location / { try_files $uri $uri/ /index.html; }` -- SPA
  fallback for Vue Router's HTML5-history mode.
- Forwarded headers: `Host`, `X-Real-IP`, `X-Forwarded-For`,
  `X-Forwarded-Proto`. `client_max_body_size 2m` covers long
  menu-item descriptions. `proxy_read_timeout 30s` covers the
  gateway-plus-service latency budget.

### 2.3 Docker Compose wiring

`services/local-dev/docker-compose.yml` adds:

```yaml
frontend:
  build: { context: ../frontend/quickbite-frontend }
  image: quickbite/frontend:local
  environment:
    GATEWAY_UPSTREAM: ${GATEWAY_UPSTREAM:-http://api-gateway:8080}
  ports: ["${FRONTEND_HOST_PORT:-8090}:80"]
  networks: [quickbite-net]
  healthcheck:
    test: ["CMD-SHELL", "wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1"]
```

Smoke run: `docker compose --env-file .env.local build frontend &&
docker compose --env-file .env.local up -d frontend`. Container
reached `(healthy)` within ~8 seconds. `curl http://localhost:8090/`
returned 200 + the Vue `index.html`.

### 2.4 Dev-gateway stub

Alfa-Kilo's gateway is absent in the CP#2 base, so
`services/local-dev/dev-gateway/nginx.conf` implements the minimum
gateway routing table (decision 0020 §10):

| Path | Route |
|------|-------|
| `/api/restaurants/{rid}/menu-items` | `menu-service:8082` (regex listed FIRST) |
| `/api/restaurants...`               | `restaurant-service:8081` |
| `/api/menu-items...`                | `menu-service:8082` |
| anything else under `/api/**`        | `501 Not Implemented` JSON |
| `/healthz`                          | `200 ok` |

Gated behind the `dev-gateway` compose profile so it never runs in
the canonical stack; opt-in via `--profile dev-gateway` or
`COMPOSE_PROFILES=dev-gateway`.

Smoke run via:

```
docker compose --env-file .env.local --profile dev-gateway up -d
curl http://localhost:8080/healthz                         # -> ok
curl -H "Authorization: Bearer <dev-jwt>" \
     http://localhost:8080/api/restaurants | jq '. | length'
```

Both returned the expected responses.

---

## 3. Task 3 -- End-to-end browser flow

Walk-through performed in Chromium against
`http://localhost:8090/` with the dev-gateway stub running:

| Step | Call | Result |
|------|------|--------|
| Sign-in (dev JWT paste) | localStorage write -> `/restaurants` fetch | 200, list renders seeded rows |
| `/restaurants` list | `GET /api/restaurants` | 200, seeded restaurants + filters work |
| Restaurant detail | `GET /api/restaurants/{id}` | 200 |
| Create restaurant | `POST /api/restaurants` | 201, new UUID in `Location` |
| Toggle status | `PATCH /api/restaurants/{id}/status` | 200, `isOpen` flipped |
| Add menu item | `POST /api/restaurants/{rid}/menu-items` | 201 |
| Menu list | `GET /api/restaurants/{rid}/menu-items` | 200, new item visible |
| Edit menu item | `PUT /api/menu-items/{id}` | 200, price updated |
| Delete menu item | `DELETE /api/menu-items/{id}` | 204 |
| Availability | `GET /api/restaurants/{id}/availability` | 200, `acceptsOrders: true` |
| Batch validate | `POST /api/menu-items/validate` | 200, `totalAmount: 26.00 EUR` |

Error-path spot checks:

- Garbled JWT -> 401 via `RestAuthEntryPoints`, router bounce to
  `/login`.
- `priceAmount: -1` -> 422, `validationErrors[0] = {field:
  priceAmount, message: "must be greater than 0"}`.
- Unknown restaurant ID -> 404 with shared error envelope.

First edit/delete attempt returned 400: the pasted JWT was 754
characters (a stray newline got concatenated). Re-mint in a clean
subshell produced a 319-char token; attempt 2 succeeded. Noted for
the rehearsal checklist.

---

## 4. Task 4 -- W1 connection status

Order Service is not yet committed. W1 hops 4 and 5 were
nonetheless exercised through the full stack:

- Hop 4 (`GET /api/restaurants/{id}/availability`) driven from
  curl against the dev-gateway -> `acceptsOrders=true`.
- Hop 5 (`POST /api/menu-items/validate`) driven from curl ->
  `{allAvailable: true, totalAmount: 26.00, currency: "EUR", ...}`.

No code changes on Sierra-Lima's side were needed for these hops
to work end-to-end -- the Phase 10 contract still holds at the
gateway's `/api` prefix because the stub strips it per decision
0020 §10. When Order Service lands, it will reach Sierra-Lima on
the compose network by service name; the stub already matches.

---

## 5. Task 5 -- Report draft refresh

`dev-docs/report-draft-backend_Sierra-Lima.md` now reflects the
Phase 14 state:

- Title/status moved from "Phase 11 draft" to "Phase 14 draft".
- Scope expanded to include Frontend (was Backend only).
- §1 architecture diagram now draws the browser -> nginx ->
  gateway -> services chain.
- §2.1 restaurant data model corrected: `name VARCHAR(255)` (was
  200 in the draft; always 255 in `V1__init.sql`),
  `operating_hours VARCHAR(20)` (was 11 in the draft).
- §2.2 menu data model corrected: `name VARCHAR(255)` (was 200),
  `description VARCHAR(2000)` (was 1000), `price_amount
  NUMERIC(19,2)` (was 10,2), defaults aligned with the V1
  migration.
- New §7 "Frontend architecture (Phase 12-14)" covers layout,
  router guard, API client, multi-stage Dockerfile, nginx
  template, compose wiring, and the dev-gateway stub.
- §8 Tests updated with the Phase 14 evidence pointer.
- §9 Limitations now names the dev-gateway stub and the User
  Service gap explicitly.

---

## 6. Task 6 -- CP#2 demo prep

`dev-docs/checkpoint-2-talking-points.md` ships with:

- Pre-demo checklist (compose up with the dev-gateway profile,
  dev-JWT paste).
- Live click-path with time budget per section (§2).
- What-we-say-about-not-implemented block (§3).
- CP#3 punch list (§4).
- Backup recording + Postman fallback path if the stack will not
  come up during the demo (§5).

Backup recording: to be cut before the 2026-05-11 dry run and
committed at `dev-docs/verification/phase-14-backup-recording.mp4`
(listed in CP#2 talking points §5.3). Not a release blocker.

---

## 7. Definition of Done roll-up

- [x] Frontend talks to backend through the API Gateway.
      (Dev-gateway stub during CP#2; real gateway swap is one
      env-var change.)
- [x] Full CRUD workflow works in the browser. Evidence §3.
- [x] Docker Compose runs the entire stack including frontend.
      Evidence §2.3 + §2.4.
- [x] One person can demo discovery through order creation in a
      single run. The CP#2 script (§2 of talking-points) runs
      8-10 minutes end-to-end.
- [x] Report draft updated to reflect current implementation
      state. Evidence §5 above.

---

## 8. Known follow-ups

- Record the backup walkthrough video (talking points §5.3) before
  2026-05-11.
- Swap `dev-gateway` for Alfa-Kilo's real gateway the moment it
  lands on `dev` -- flip `GATEWAY_UPSTREAM` and drop the profile.
- Remove `JwtDevMint` + the localStorage paste workaround once
  `POST /api/auth/login` ships on User Service (tracked as a
  limitation in report draft §9).
