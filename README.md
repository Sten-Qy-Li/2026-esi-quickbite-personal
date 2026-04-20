# 2026-esi-quickbite-personal

Sierra-Lima's personal workspace for the 2026 ESI QuickBite project
(MTAT.03.229, University of Tartu, Group 7). Contains:

- **Restaurant Service** (Java, Spring Boot) -- R19/R20.
- **Menu Service** (Java, Spring Boot) -- R21/R22.
- The Sierra-Lima slice of the Vue 3 frontend.
- A `docker-compose` stack, smoke scripts, Postman pack, and runbook
  for running the slice end-to-end on a developer laptop.
- Decisions, audits, gap analyses, chat archives, and per-phase
  verification reports for the full implementation history.

> This is Sierra-Lima's personal early-start fork, **not** the
> official Group 7 shared repository. The contents below are being
> handed to the Group 7 team lead for integration into the
> team-/group-wide repository.

---

## Team-lead integration guide

Hi team lead -- this section is for you. It walks through (a) what
is in this repo, (b) what to merge into the group repo, (c) what to
leave behind, (d) how to verify after merging, and (e) where the
known contracts and open issues are.

### 1. What you are getting

| Path | What it is | Merge verdict |
|---|---|---|
| `services/restaurant-service/` | Spring Boot service, Maven, JWT-auth, 6 endpoints, Flyway-backed Postgres, 33 tests green. | **Merge** into the group repo under the same path. |
| `services/menu-service/` | Spring Boot service, Maven, JWT-auth, 6 endpoints, Flyway-backed Postgres, 47 tests green, log-only `menu-events` publisher. | **Merge** into the group repo under the same path. |
| `services/frontend/quickbite-frontend/` | Vue 3 frontend (Sierra-Lima slice: restaurant + menu browse/edit/delete, login, signup placeholders). | **Merge** as a starting point; merge with other teammates' frontend slices. |
| `services/local-dev/` | Docker Compose stack, `.env.example`, Postman pack, smoke scripts, dev-gateway, evidence logs, runbook. | **Merge selectively** -- see §3 below. |
| `dev-docs/` | Sierra-Lima's design decisions, audits, gap analyses, roadmaps, verification logs, chat archives. | **Merge selectively** -- see §4 below. |
| `.gitignore`, `.github/` (none yet) | Standard. | **Merge** as a baseline. |

At the tip of branch `dev` (commit `6fcc447`), every automated check
is green:

```
restaurant-service:   mvn test       -> 33/33 pass
menu-service:         mvn test       -> 47/47 pass
frontend:             npm run lint   -> clean
                      npm run build  -> successful
local-dev:            docker compose --profile dev-gateway up -d
                                     -> 6 containers healthy
                      bash smoke.sh  -> OK
                      bash smoke-cross-service.sh
                                     -> 0/0 failures
                      newman run ... -> 39 requests, 68/68 assertions, 0 failures
```

The authoritative pre-team-integration audit is
[`dev-docs/audits/audit-50b8e1d_Charlie-Lima-Alfa_pre-team-integration-readiness.md`](dev-docs/audits/audit-50b8e1d_Charlie-Lima-Alfa_pre-team-integration-readiness.md).
It records each check above with the command, the result, and any
findings. **One open issue** -- a brittleness in the Postman
`PUT /restaurants/{id}` request body (see §5 below) -- is
documented but not blocking.

### 2. Suggested integration sequence

1. **Clone the group repo locally** and create an integration
   branch, e.g. `integrate-sierra-lima`.
2. **Copy `services/restaurant-service/`** and
   **`services/menu-service/`** into the group repo at the same
   paths. No rewrites needed; the Maven `groupId` is
   `ee.ut.esi.quickbite` and the package root is
   `ee.ut.esi.quickbite.<service>`, matching the group convention
   in [`dev-docs/decisions/0003-conventions.md`](dev-docs/decisions/0003-conventions.md).
3. **Port `services/local-dev/docker-compose.yml`** into the group
   compose file. The Sierra-Lima services expect
   `restaurant-db:5432` and `menu-db:5432` on a shared
   bridge network; adjust service names / networks to match the
   group convention if needed. Ports on the host: Restaurant
   Service `8081`, Menu Service `8082`, `restaurant-db` host `5432`,
   `menu-db` host `5433` -- pinned in the port matrix in
   [`services/local-dev/README.md`](services/local-dev/README.md).
4. **Merge the Postman pack** into the group's Postman workspace.
   The collection in `services/local-dev/postman/` covers:
   - JWT auto-mint (collection-level pre-request script).
   - Every Sierra-Lima endpoint (positive, negative-auth,
     wrong-role, bad-input).
   - The W1 Integration folder (hop 4 + hop 5) that Alfa-Kilo's
     Order Service has to drive.
   - The role-matrix folder that exercises the `0010` auth
     commitments end-to-end.
5. **Merge the frontend.** `services/frontend/quickbite-frontend/`
   is the Sierra-Lima slice (R19/R20 for Restaurant,
   R21/R22 for Menu). Teammates' frontends either merge into the
   placeholder views already present (`CartView.vue`,
   `OrderStatusView.vue`) or replace them entirely.
6. **Merge the decision pack.** See §4 below -- decisions 0001-0040
   are Sierra-Lima's contract commitments; they should land in the
   group repo wholesale so every teammate reads the same contracts.
7. **Run the verification commands in §6** on the integration
   branch. If any check fails, compare against the tip commit here
   (`6fcc447`) to see where drift was introduced.
8. **Merge the integration branch into the group `main`** (or
   equivalent) once green.

### 3. What to do with `services/local-dev/`

`local-dev/` was designed to run only Sierra-Lima's slice on a
developer laptop. For the group repo:

- **Merge** `docker-compose.yml` as a starting point for the
  group-wide compose file; add teammate services and the real
  Alfa-Kilo Spring Cloud Gateway. Drop the `dev-gateway` profile
  once the real gateway is in place.
- **Merge** the Postman pack -- the W1 Integration folder is the
  authoritative cross-service smoke for the "place order" flow.
- **Merge** `smoke.sh` / `smoke-cross-service.sh` as a starting
  point for the group-wide smoke suite.
- **Consider** keeping `evidence/` as the audit-trail convention in
  the group repo too. Past logs (named by timestamp) can stay as
  historical record; new runs should still land here.
- **Rename** `dev-gateway/` or drop it once the real API Gateway is
  present -- it is an nginx stub, not a replacement for Spring
  Cloud Gateway.

### 4. What to do with `dev-docs/`

| Subfolder | Action |
|---|---|
| `decisions/` | **Merge** 0001-0040 wholesale. These are Sierra-Lima's contract commitments; renumber within the group repo if its own ADR sequence requires it, but do not rewrite the contents. |
| `audits/` | Optional -- useful as handover evidence but not required in the group repo. Either mirror into `group-repo/dev-docs/audits/sierra-lima/` or archive off-repo. |
| `gap-analysis/` | Optional, same reasoning as `audits/`. |
| `agent-context/` | **Do not merge.** These are AI-coding-agent chat archives; useful for Sierra-Lima's own continuity, not for the group. |
| `verification/` | Optional -- per-phase evidence. Keep if the group wants per-phase DoD artefacts; otherwise archive. |
| `roadmaps/` | Optional -- Sierra-Lima's master plan. Typically superseded by the group's shared roadmap. |
| `presentation/` | **Merge** the Sierra-Lima phase-18 pack into the group's presentation folder if the group has one; otherwise keep here. |
| `prior-submissions/` | **Do not merge.** Submissions are already in the instructor's grading system; not useful in the group repo. |
| `course-materials/` | **Do not merge.** Keep one copy centrally (your own repo); there is no need to duplicate every PDF across every personal fork. |
| `checkpoint-1-talking-points.md` / `checkpoint-2-talking-points.md` | **Merge** into a group-wide talking-points folder; Sierra-Lima's sections are useful prep for the whole team. |
| `report-draft-backend_Sierra-Lima.md` | **Merge** as a starting draft for the Sierra-Lima half of the CP#1 backend report. |

### 5. Known issues (carried forward)

One item is documented but not fixed, recorded in audit `50b8e1d`
Finding 1:

- **Postman `PUT /restaurants/{id}` returns `409` on non-pristine
  volumes.** The update body hard-codes `"name": "Pizza Antonio
  (updated)"` with no uniqueness suffix. Combined with Sierra-Lima's
  duplicate-name protection on rename (added at `1a6e8c7`), the
  second run onwards gets a silent `409` because the request has
  no assertion block. Newman still reports "all green". Fix:
  (a) add `{{$timestamp}}` to the PUT body name, matching the POST
  pattern on line 204, and (b) add a `pm.test('status is 200')`
  assertion on the request.

Everything else is closed. The full open-findings list is §4 of the
audit linked above.

### 6. Post-merge verification

After you have merged into the integration branch, run these checks:

```bash
# From the group repo root, after merging Sierra-Lima's services:

# 1. Backend tests (Sierra-Lima's)
( cd services/restaurant-service && mvn clean test )
( cd services/menu-service        && mvn clean test )

# 2. Frontend (merge with teammates' first, then:)
( cd services/frontend/quickbite-frontend && npm ci && npm run lint -- --no-fix && npm run build )

# 3. Docker stack (Sierra-Lima's; extend for teammates' services):
( cd services/local-dev && docker compose --profile dev-gateway up -d --build )
docker ps --format "table {{.Names}}\t{{.Status}}"
# Expect: all containers "Up ... (healthy)"

# 4. Smoke
( cd services/local-dev && bash smoke.sh )
( cd services/local-dev && bash smoke-cross-service.sh )

# 5. Newman (Sierra-Lima's pack):
( cd services/local-dev && npx newman run postman/QuickBite.postman_collection.json \
                                      -e postman/QuickBite.postman_environment.json )
# Expect: 68/68 assertions, 0 failures
```

Any failure traceable to a Sierra-Lima file: compare `git diff`
against commit `6fcc447` of `https://github.com/Sten-Qy-Li/2026-esi-quickbite-personal.git`
(or reach out to Sierra-Lima).

### 7. Where the contracts live

Sierra-Lima's contract commitments to the group live in
[`dev-docs/decisions/`](dev-docs/decisions/). The ones you most
likely care about for integration:

- [`0001-scope-freeze.md`](dev-docs/decisions/0001-scope-freeze.md)
  -- the 7 services + 2 shared components + 1 design-only split.
- [`0003-conventions.md`](dev-docs/decisions/0003-conventions.md)
  -- naming, package, Docker, env vars.
- [`0010-auth-contract.md`](dev-docs/decisions/0010-auth-contract.md)
  -- JWT shape, issuer, secret, role matrix.
- [`0020-sierra-lima-contracts.md`](dev-docs/decisions/0020-sierra-lima-contracts.md)
  -- full HTTP API surface of Restaurant Service and Menu Service.
- [`0030-w1-synchronous-contract-lock.md`](dev-docs/decisions/0030-w1-synchronous-contract-lock.md)
  -- W1 hop-4 and hop-5 request/response shapes (Order → Restaurant
  availability → Menu batch validate).
- [`0031-cross-service-status-code-table.md`](dev-docs/decisions/0031-cross-service-status-code-table.md)
  -- canonical HTTP status codes for every documented failure mode.
- [`0032-w2-w3-event-contract-lock.md`](dev-docs/decisions/0032-w2-w3-event-contract-lock.md)
  -- Kafka event envelopes for W2/W3 (including Sierra-Lima's
  `menu-events` producer shape).
- [`0033-inter-service-token-propagation-lock.md`](dev-docs/decisions/0033-inter-service-token-propagation-lock.md)
  -- how the bearer token propagates cross-hop in W1.
- [`0040-phase-16-async-stance.md`](dev-docs/decisions/0040-phase-16-async-stance.md)
  -- Sierra-Lima's log-only `menu-events` stance and the
  one-class Kafka swap plan (triggers after the 2026-04-21 sync if
  teammate async is not on-track).

### 8. Contact

Open a GitHub issue at
<https://github.com/Sten-Qy-Li/2026-esi-quickbite-personal/issues>
or ping Sierra-Lima directly. The full commit history, all audits,
and all chat archives are in this repo for reference.

---

## For non-team-lead readers

Everything above is for the Group 7 team lead driving integration.
If you landed here for a different reason:

- **Contributor or reviewer?** Start at
  [`services/README.md`](services/README.md) for code layout and
  [`dev-docs/README.md`](dev-docs/README.md) for documentation layout.
- **AI coding agent?** Read the latest chat archive in
  [`dev-docs/agent-context/`](dev-docs/agent-context/) and the
  latest audit in [`dev-docs/audits/`](dev-docs/audits/) before
  touching code. Never modify decisions in place -- write a
  superseding decision instead.
- **Instructor / grader?** The graded submissions for A1-A3 are in
  [`dev-docs/prior-submissions/`](dev-docs/prior-submissions/), the
  design contracts are in [`dev-docs/decisions/`](dev-docs/decisions/),
  and the implementation evidence is in
  [`dev-docs/audits/`](dev-docs/audits/) and
  [`dev-docs/verification/`](dev-docs/verification/).

## License and attribution

This is coursework in progress. Authors:

- Human owner: Sten-Qy-Li, pseudonym **Sierra-Lima**,
  MSc Computer Science, University of Tartu, 2026.
- AI coding agents (Claude Code, acting under explicit user
  direction): **Charlie-Lima-Alfa** and **Golf-Papa-Tango** for
  most of the under-the-hood documentation, audits, and chat
  archives. Callsigns identify the author session, not a service
  owner.

Teammates in Group 7 (service owners, not authors of this repo):
Alfa-Kilo, Elephant-Yankee, Mike-Alfa.
