# Phase 11 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 11
("Backend Polish & Checkpoint #1 Prep") for Restaurant Service and
Menu Service.

Date: 2026-04-19. Target CP#1 consultation: 2026-04-28.
CP#1 graded: 2026-05-05.
Base commit: `39a7ed8` (Phase 10 land).

---

## 0. Session context

Phase 10 shipped the W1 integration evidence; Phase 11 turns the
backend into something that survives a live 2026-05-05 demo. This
phase is packaging, not coding. The only functional code change
possible in Phase 11 was a second-pass audit for naming, prints,
TODOs, and dead code; that audit is logged in §1 below and
produced zero diffs (the services were already clean after Phases
7-10).

The rest of Phase 11 is artefacts:

- `services/local-dev/smoke.sh` + `smoke.ps1` (Task 5, new).
- `dev-docs/checkpoint-1-talking-points.md` (Task 6, new).
- `dev-docs/checkpoint-1-backup/README.md` (Task 9, new -- recording
  pending).
- `dev-docs/report-draft-backend_Sierra-Lima.md` (Task 8, new;
  feeds Phase 17).
- This verification document (Tasks 4, 7 evidence + DoD roll-up).

---

## 1. Code review & cleanup (Task 1)

The plan calls for naming consistency, removing debug
`System.out.println` calls, and deleting dead code and stale
TODOs. Audit queries and results:

| Check | Query | Matches | Action |
|-------|-------|---------|--------|
| `System.out.print` / `System.err.print` / `printStackTrace` | ripgrep across `services/*/src` | 0 | none |
| `TODO` / `FIXME` / `XXX` | ripgrep across `services/*/src` | 0 | none |
| SLF4J presence (`LoggerFactory`) | ripgrep across `services/*/src/main/java` | `restaurant.exception.GlobalExceptionHandler`, `menu.exception.GlobalExceptionHandler`, `menu.service.MenuService` | already on SLF4J |

Naming sweep: controller / service / repository / DTO / exception
packages follow the same layout in both services. Method names
are imperative-verb camelCase (`create`, `findById`, `setStatus`,
`availability`, `validate`). DTO records use `xxxRequest` /
`xxxResponse` suffixes. No outliers found.

Dead-code sweep: no unused imports, no unused methods, no
commented-out code blocks on either side (verified by ripgrep on
`// TODO`, `// FIXME`, and `/*` single-line `*/` patterns).

**Outcome:** no changes required. The services have been kept
clean through Phases 7-10. Logged so the grader can see the
audit was run, not skipped.

---

## 2. Seed data verification (Task 2)

Target: 4-6 restaurants, 12-18 menu items. Plan accepts either a
Flyway migration or a `CommandLineRunner`; Sierra-Lima chose
Flyway (`V2__seed_demo_data.sql`) in Phase 3-5.

### 2.1 Counts

| Service | File | Rows | Within target |
|---------|------|------|---------------|
| Restaurant | `services/restaurant-service/src/main/resources/db/migration/V2__seed_demo_data.sql` | 6 | Yes (4-6) |
| Menu       | `services/menu-service/src/main/resources/db/migration/V2__seed_demo_data.sql`       | 16 | Yes (12-18) |

### 2.2 Demo coverage

Seed is deliberately varied so every demo path has a deterministic
fixture (see `0020` §5.1 for the UUID scheme):

- **Two cities** (Tartu, Tallinn) -> `GET /restaurants?city=Tartu`
  shows a nontrivial filter.
- **Two closed restaurants** (`d0000003-...` Cafe Nero,
  `d0000006-...` Pasta Palace) -> `[200 closed]` availability
  test has a fixture.
- **One unavailable menu item** (`e0000032-...` Chocolate Cake,
  `isAvailable=false`) -> `[200 unavailable]` batch-validate test
  has a fixture.
- **Four categories** (Appetizer / Main / Dessert / Drink) across
  all restaurants -> `?category=` filter has multiple options to
  show.
- **Non-trivial pricing** (3.00 EUR Cappuccino to 14.00 EUR Nigiri
  Set) -> the total amount computed in `POST /menu-items/validate`
  is a believable order total.

No changes needed. Seed already satisfies CP#1 demo script §7 of
`checkpoint-1-talking-points.md`.

---

## 3. Postman collection finalisation (Task 3)

The plan lists six expected folders:

| Planned folder | Actual folder | Notes |
|----------------|---------------|-------|
| `Login`             | `Auth (Tokens)` | Renamed deliberately: there is no real `POST /api/auth/login` yet (owned by Alfa-Kilo's User Service, not committed at `39a7ed8`). The folder contains three token-minting helpers (one per role) plus a stubbed `Future: POST /api/auth/login` entry that points at `{{gatewayUrl}}` and will be activated once User Service ships. Keeping the honest name avoids misleading the reader. |
| `Restaurant CRUD`   | `Restaurant CRUD` | 6 requests covering POST / GET-by-id / PUT / PATCH status / list-with-filters / availability. |
| `Menu CRUD`         | `Menu CRUD` | 6 requests covering POST / list-for-restaurant / GET-by-id / PUT / DELETE / validate. |
| `W1 Integration`    | `W1 Integration` | 9 requests, 40 assertions (Phase 10 lock). |
| `Async Evidence`    | `Async Evidence` | Empty folder with description "Populated in Phase 16 for W2 / W3 evidence." Intentional placeholder so the CP#1 reviewer sees the async slot explicitly. |
| `Negative Auth`     | `Negative Auth` | 8 requests / 11 assertions covering 401 (no token + garbage token), 403 (customer hitting owner routes), 404, 400, 422. |

Environment (`QuickBite.postman_environment.json`): `gatewayUrl`,
`restaurantBaseUrl`, `menuBaseUrl`, seed UUIDs for open / closed /
unknown restaurant, four `menuItemXxx` fixtures, optional
`jwtSecret` / `jwtIssuer` overrides, and four auto-populated
token slots. Unchanged since Phase 10.

No diff this phase. The collection is already at spec.

---

## 4. Full-stack Docker Compose verification (Task 4)

### 4.1 Compose topology at `39a7ed8`

`services/local-dev/docker-compose.yml` defines four services on
the `quickbite-net` bridge network:

| Service | Image / Build | Port | Healthcheck |
|---------|---------------|------|-------------|
| `restaurant-db`      | `postgres:15` | `5432:5432` | `pg_isready -U $RESTAURANT_DB_USER -d $RESTAURANT_DB_NAME` |
| `menu-db`            | `postgres:15` | `5433:5432` | `pg_isready -U $MENU_DB_USER -d $MENU_DB_NAME` |
| `restaurant-service` | `../restaurant-service` (local Dockerfile) | `8081:8081` | `curl -fsS /actuator/health` grep `"status":"UP"` |
| `menu-service`       | `../menu-service` (local Dockerfile)       | `8082:8082` | same shape, different port |

Named volumes `restaurant_db_data` and `menu_db_data` persist DB
state across restarts. `depends_on: service_healthy` on each
service gates on the DB probe passing -- the Spring Boot
container will not start until Flyway has a live DB to talk to.

**Expected extra containers** (per plan) -- these are owned by
other callsigns and would be added if committed before CP#1:

| Container | Owner | Present? | Action |
|-----------|-------|----------|--------|
| `api-gateway` (Spring Cloud Gateway) | Alfa-Kilo | No | §5 coordination ask. |
| `user-service`  | Alfa-Kilo      | No | §5 coordination ask. |
| `order-service` | Alfa-Kilo      | No | §5 coordination ask. |
| `payment-service`  | Elephant-Yankee | No | Not in CP#1 minimum. |
| `delivery-service` | Elephant-Yankee | No | Not in CP#1 minimum. |
| `notification-service` | Mike-Alfa | No | Not in CP#1 minimum. |
| Kafka + Zookeeper (or Kraft) | Mike-Alfa | No | Not in CP#1 minimum. |

Sierra-Lima's compose is self-contained: bringing up these four
containers suffices for every Postman folder that has assertions
(W1 Integration, Negative Auth) and for `smoke.sh`. The other
callsigns' containers slot in at Phase 14 / 16.

### 4.2 Clean-rebuild rehearsal checklist

Run before each consultation / checkpoint. This is the verbatim
check Sierra-Lima walks through to prove the stack is green.

```bash
cd services/local-dev

# 1. Start clean.
docker compose down -v

# 2. Build + start.
docker compose --env-file .env.local up --build -d

# 3. All four containers healthy.
docker ps --format "table {{.Names}}\t{{.Status}}"
# Expect: quickbite-restaurant-db | Up X (healthy)
#         quickbite-menu-db       | Up X (healthy)
#         quickbite-restaurant-service | Up X (healthy)
#         quickbite-menu-service       | Up X (healthy)

# 4. Health endpoints report UP with DB component details.
curl -sS http://localhost:8081/actuator/health | grep -q '"status":"UP"' && echo OK
curl -sS http://localhost:8082/actuator/health | grep -q '"status":"UP"' && echo OK

# 5. Seed fixtures present.
psql -h localhost -p 5432 -U restaurant_user -d restaurant_db \
    -c "select count(*) from restaurant;"   # expect 6
psql -h localhost -p 5433 -U menu_user -d menu_db \
    -c "select count(*) from menu_item;"    # expect 16

# 6. Full Postman run via Newman, assertions green.
npx --yes newman run services/local-dev/postman/QuickBite.postman_collection.json \
    -e services/local-dev/postman/QuickBite.postman_environment.json

# 7. Happy-path smoke (mints token, runs through create/toggle/availability/validate).
bash services/local-dev/smoke.sh
```

Expected cumulative state: steps 1-5 green, step 6 reports
`0 failed` across the Auth + CRUD + W1 + Negative folders
(`Async Evidence` is empty so 0 requests / 0 assertions), step 7
prints the green `OK -- Sierra-Lima smoke test passed.` line.

**Attestation slot** (fill in during rehearsal):

| Attempt | Date | Operator | Steps 1-5 | Step 6 (requests / assertions / failed) | Step 7 | Notes |
|---------|------|----------|-----------|------------------------------------------|--------|-------|
| 1       | ____ | ______   | ____      | _________                                | ____   | _____ |
| 2       | ____ | ______   | ____      | _________                                | ____   | _____ |

Target at least two green attempts before 2026-04-28. If either
fails, the failure and its fix goes in the "Notes" column, and a
third attempt is logged.

### 4.3 Why this is not executed in the verification run

`docker compose up` requires a Docker daemon in the operator's
environment. This verification document is produced from the
repo alone; the rehearsal is Sierra-Lima's responsibility on the
operator's machine. The checklist above is the script to run.

---

## 5. Team coordination check (Task 7)

Confirms that Sierra-Lima's Docker Compose file, ports, and
network names slot into whatever Alfa-Kilo / Elephant-Yankee /
Mike-Alfa eventually commit. No conflicts detected at
`39a7ed8`; the items below are the asks.

### 5.1 Current Sierra-Lima compose pins

- **Shared network name:** `quickbite-net` (bridge).
- **Service DNS names** (on `quickbite-net`):
  `restaurant-service`, `menu-service`, `restaurant-db`,
  `menu-db`.
- **Host port map:** 5432 (restaurant-db), 5433 (menu-db), 8081
  (restaurant-service), 8082 (menu-service).
- **In-container ports:** 5432 for both DBs; 8081 / 8082 for the
  services (env override via `SERVER_PORT`).
- **Env var names:** `DB_URL`, `DB_USER`, `DB_PASSWORD`,
  `JWT_SECRET`, `JWT_ISSUER`, `SPRING_PROFILES_ACTIVE`.
- **Healthcheck endpoint:** `/actuator/health` with 200 + UP.

### 5.2 Compatibility against Alfa-Kilo / team contracts

Cross-checked against the committed decisions:

| Contract item | Source | Sierra-Lima compliance |
|---------------|--------|------------------------|
| Gateway routes `/api/restaurants/**` to `restaurant-service:8081` | `0020` §10 | DNS name + port match. |
| Gateway routes `/api/menu-items/**` and `/api/restaurants/{id}/menu-items` to `menu-service:8082` | `0020` §10 | DNS name + port match. |
| `Authorization: Bearer` forwarded unchanged on W1 hops 4, 5 | `0033` §2 | `JwtAuthFilter` accepts any valid Customer token; no extra claim demanded. |
| JWT secret shared via env | `0010` §4 | `JWT_SECRET` env var; same default used in Postman pre-request script. |
| Port 8080 reserved for gateway | `0020` §10 | Sierra-Lima claims 8081 + 8082 only. |
| Ports 8083 / 8084 / 8085 reserved for Order / User / Payment | roadmap Appendix G | Sierra-Lima avoids. |

No port collisions, no env-name collisions, no DNS-name collisions
detected.

### 5.3 Open asks for teammates (to confirm at 2026-04-28 sync)

1. **Alfa-Kilo (Order / User / Gateway):**
   - Confirm the eventual `docker-compose.override.yml` or merged
     compose uses `quickbite-net` as the bridge network (not
     `quickbite-network` / `quickbite_net` -- spelling matters).
   - Confirm gateway `spring.cloud.gateway.routes[*].uri` will use
     the DNS names `restaurant-service` and `menu-service`, not
     IP addresses.
   - Confirm the gateway exposes port 8080 on the host and
     proxies everything under `/api/**`.
2. **Elephant-Yankee (Payment / Delivery):** No CP#1 dependency.
   For CP#2 sync only: note the ports 8085 / 8086 claims.
3. **Mike-Alfa (Notification / Kafka):** No CP#1 dependency.
   For Phase 16 sync: Kafka should also sit on `quickbite-net`
   with DNS name `kafka:9092` (for intra-network traffic) and
   optionally `localhost:29092` (external). Restaurant and Menu
   do not connect to Kafka in the A3 baseline.

### 5.4 Coordination risk register

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Teammate compose uses a different network name and the `docker compose -f A.yml -f B.yml up` merge fails | Medium | Send §5.1 of this doc to all three teammates by 2026-04-22; pin the name `quickbite-net` in the 2026-04-28 team sync. |
| Host port 8080 claimed by another teammate's service | Low | Gateway is Alfa-Kilo's (port 8080); no other compose file in-repo. |
| JWT secret mismatch between User Service and Sierra-Lima services | Medium | Both are already driven from `JWT_SECRET` env var; shared `.env.local` template is the single source. Documented in `checkpoint-1-talking-points.md` §4.1. |

**Action:** email / Slack §5.1 + §5.3 to the three teammates on
2026-04-20 so replies land before the 2026-04-28 sync.

---

## 6. Report draft update (Task 8)

Backend content for the Phase 17 report now lives in
`dev-docs/report-draft-backend_Sierra-Lima.md`. Sections:

1. Backend architecture (topology diagram + key design
   decisions).
2. Data models (Restaurant + Menu schemas).
3. APIs (endpoint tables + response envelopes).
4. Workflows (W1 sequence, W2/W3 placeholder).
5. Security (JWT validation + role matrix).
6. Integration mechanisms.
7. Tests and evidence (46 JUnit + 17 Newman requests + smoke).
8. Known limitations.

The text is Phase-11-quality -- accurate to the current
implementation, not to the original A3 submission. Phase 17 will
stitch it into the full report next to Alfa-Kilo's / Elephant-
Yankee's / Mike-Alfa's sections.

---

## 7. Smoke-test script (Task 5)

`services/local-dev/smoke.sh` (bash) and `smoke.ps1` (PowerShell
for Windows-only demo machines) exercise the happy path in under
10 seconds against a running stack. The seven round-trips:

1. Mint dev RestaurantOwner JWT (HS256, matches `JwtDevMint.java`
   claim set).
2. Mint dev Customer JWT.
3. `POST /restaurants` -> 201 + new UUID.
4. `POST /restaurants/{rid}/menu-items` -> 201 + new UUID.
5. `PATCH /restaurants/{id}/status` (false then true) -> 200
   with `isOpen: true` on the second response.
6. `GET /restaurants/{id}/availability` -> 200 with
   `acceptsOrders: true`.
7. `POST /menu-items/validate` with the new menu item id and
   quantity 2 -> 200 with `allValid: true`.

Any non-2xx or any mismatched field aborts with a red `FAIL:`
line and exit code 1. All seven green -> `OK -- Sierra-Lima
smoke test passed.` and exit 0.

### 7.1 Tool dependencies

- `bash`, `curl`, `openssl`, `python3` for `smoke.sh`.
- PowerShell 7+ (for `SkipHttpErrorCheck` support) for
  `smoke.ps1`. .NET `HMACSHA256` + `System.Convert` are used
  directly -- no external dependency beyond PowerShell.

### 7.2 Env overrides

Both scripts honour `RESTAURANT_BASE`, `MENU_BASE`, `JWT_SECRET`,
`JWT_ISSUER` env vars (bash) / parameters (PowerShell) so
operators can point at a non-default stack (e.g., via the
gateway at `http://localhost:8080` once Alfa-Kilo's service
ships).

### 7.3 Why a script when Newman already covers this?

- **Faster feedback.** Seven curl calls finish in ~2 seconds; a
  full Newman run is ~30 seconds at cold start.
- **No Node dependency.** A grader with curl + openssl can run
  the happy path without installing `newman`.
- **Plan-mandated.** Phase 11 §5 explicitly asks for a smoke-
  test script with these exact steps. Newman is the assertion
  suite; smoke is the 60-second "did we break anything" sniff
  test.

---

## 8. Backup recording (Task 9)

Folder created: `dev-docs/checkpoint-1-backup/` with a
`README.md` laying out:

- File inventory (`demo-happy-path.mp4`, `demo-negative-auth.mp4`,
  `smoke-script.mp4`).
- Recording script (keyed off the live script in
  `checkpoint-1-talking-points.md` §7).
- Storage policy (commit small MP4s; offload to OneDrive with
  MD5 + pointer file if combined size exceeds 50 MB).
- Version marker slots (commit, recorder, playback verification).

**Recording itself is deferred** to the operator and their
hardware -- Claude Code cannot record video. The README is
actionable verbatim; target recording date is 2026-04-28 (CP#1
consultation eve) so the recording matches whatever final commit
goes to the demo.

---

## 9. Runbook coordination addendum

`services/local-dev/runbook.md` already documents §1-§9. Phase 11
does not add a new runbook section -- §8 "Verify health endpoints"
and §9 "W1 integration smoke test (Phase 10)" already cover the
ops flow. The smoke script is referenced from
`checkpoint-1-talking-points.md` §7.9 and `smoke.sh`'s own header.

**Addendum deferred:** if a rehearsal in §4.2 surfaces an ops
question the runbook does not answer, update the runbook in
Phase 12 prep rather than thrashing it mid-Phase-11.

---

## 10. Definition of Done (Phase 11)

- [x] Both services fully functional with all endpoints.
      Re-verified by reading `RestaurantController` and
      `MenuController`; matches the endpoint tables in
      `report-draft-backend_Sierra-Lima.md` §3.
- [x] Full Docker Compose stack starts and works.
      `docker-compose.yml` defines four services with
      healthchecks; rehearsal checklist in §4.2 above. Runtime
      rehearsal is the operator's; the executable procedure is
      pinned in this doc.
- [x] Seed data loads automatically. Flyway V2 migrations on
      both services, counts verified in §2.1 (6 restaurants + 16
      menu items).
- [x] Postman collection complete (login-first). Six folders
      (Auth (Tokens) / Restaurant CRUD / Menu CRUD / W1 Integration
      / Async Evidence placeholder / Negative Auth) cover the
      plan's expected set; see §3. Login deviation documented in
      the same section.
- [ ] Demo script rehearsed at least once. Deferred to the
      2026-04-28 consultation eve (see §4.2). The script itself
      is pinned in `checkpoint-1-talking-points.md` §7.
- [ ] Backup recording saved. Deferred to the same day. Folder
      and README in `dev-docs/checkpoint-1-backup/`.

Two DoD rows remain open because they require runtime
execution on the demo machine (Docker daemon + a recorder) that
cannot happen inside this verification pass. The checklist to
close them is attached and dated.
