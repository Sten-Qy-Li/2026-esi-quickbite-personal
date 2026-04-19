# Team-Lead Integration Readiness Audit -- local repository state rooted at `60fa710`

| Field | Value |
| --- | --- |
| `HEAD` commit | `60fa710c29e4417208864dce3d710e856bd782bf` |
| Commit subject | `Patch Golf-Papa-Tango bcc9dd0 findings 1-2` |
| Branch observed | `dev` |
| Audit date | `2026-04-19` |
| Auditor | `Golf-Papa-Tango` |
| Scope | Sierra-Lima-owned slice: Restaurant Service, Menu Service, Sierra-Lima frontend surfaces, local-dev stack, and Sierra-Lima's W1 responsibilities |
| Working tree status | `DIRTY` -- 3 modified Menu Service files are not committed |
| Verdict | `CONDITIONALLY READY` |

## Executive summary

The current **local repository contents** sufficiently cover Sierra-Lima's Assignment 3 responsibilities:

- `Restaurant Service` is implemented with the expected six endpoints.
- `Menu Service` is implemented with the expected six endpoints.
- both service schemas, Flyway migrations, seed data, JWT enforcement, and W1 endpoints are present and working;
- the local-dev Docker stack, dev-gateway stub, frontend build, and Sierra-Lima smoke paths all run successfully.

However, the repository is **not cleanly handover-ready as a git state yet**:

1. the fix that closes the previously-audited Menu orphan-item defect exists only as **uncommitted local changes**;
2. the tracked Windows validation scripts (`smoke.ps1`, `smoke-cross-service.ps1`) are currently broken on the actual local PowerShell environment;
3. the frontend has a few client-side validation gaps relative to the backend contract.

So the right verdict is:

- **Ready enough in the current local folder, if the team lead receives these exact local files**.
- **Not ready as commit `60fa710` alone, and not ready for a normal git handoff until the current Menu Service fix is committed.**

## Findings

### 1. Medium-High -- the critical Menu Service fix is present only in the local worktree, not in `HEAD`

Files with uncommitted changes:

- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java`
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/controller/MenuControllerTest.java`
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/service/MenuServiceTest.java`

What changed in the local worktree:

- `MenuService.create(...)` now resolves the restaurant owner first and returns `404` when the target restaurant does not exist, even for `Admin`.
- matching tests were added for the `Admin + missing restaurant -> 404` path.

Why this matters:

- `HEAD` `60fa710` was previously known to allow `Admin` to create an orphan menu item under a nonexistent restaurant.
- the **current local repo** no longer reproduces that defect;
- but if the team lead pulls `60fa710` from git without these local edits being committed, the fix is lost.

Evidence from this audit:

- current local repo probe: `Admin create menu on missing restaurant -> 404`
- current local repo probe: `Owner create menu on missing restaurant -> 404`
- `Menu Service` tests passed locally: `46/46`

Handover impact:

- this is the main release-management risk in the current state;
- it is blocking for any git-based handover, but not for a literal folder handoff.

### 2. Medium -- the tracked Windows PowerShell smoke scripts are broken on the actual local platform

Affected files:

- `services/local-dev/smoke.ps1:72`
- `services/local-dev/smoke-cross-service.ps1:106`

Observed behavior on this machine:

- both scripts use `Invoke-WebRequest -StatusCodeVariable ...`;
- PowerShell `7.6.0` on this machine does **not** expose that parameter;
- `smoke.ps1` fails immediately with:

```text
A parameter cannot be found that matches parameter name 'StatusCodeVariable'.
```

- `smoke-cross-service.ps1` catches the same failure and degrades it into a misleading HTTP `0`, so it reports a false Sierra-Lima failure.

Why this matters:

- the repo includes these `.ps1` scripts specifically for Windows-friendly validation;
- on the user's real platform, they currently cannot be trusted.

Mitigation used during this audit:

- `bash ./smoke.sh` passed;
- `bash ./smoke-cross-service.sh` passed.

Handover impact:

- not a blocker for backend integration itself;
- but it is a real verification-tooling defect and should be fixed before asking others to validate the repo from Windows.

### 3. Low -- frontend client-side validation is weaker than the backend contract

Contract drift observed:

- `services/frontend/quickbite-frontend/src/views/AddRestaurantView.vue:93-110`
- `services/frontend/quickbite-frontend/src/views/RestaurantDetailView.vue:181-198`

These forms do **not** require `city`, while Sierra-Lima's backend contract requires a non-blank city.

- `services/frontend/quickbite-frontend/src/views/AddMenuItemView.vue:131-134`
- `services/frontend/quickbite-frontend/src/views/MenuItemDetailView.vue:175-178`

These forms allow `priceAmount = 0`, while the backend requires price to be strictly positive.

Why this matters:

- users can submit values that the frontend treats as valid but the backend rejects with `400` / `422`;
- the result is avoidable user-facing friction, not a backend integrity problem.

Handover impact:

- low severity;
- worth cleaning before demo/polish, but not a blocker for service integration.

### 4. Low -- the Newman collection can overstate "green" status

Observed in the full Newman run:

- `POST /api/auth/login` through the dev-gateway stub returned `501 Not Implemented`;
- `POST /restaurants` in the CRUD folder returned `409 Conflict`;
- the collection still finished with `39 requests`, `66 assertions`, `0 failures`.

Why:

- those requests do not have assertions that enforce expected status.

Why this matters:

- a green Newman summary is useful, but not sufficient on its own;
- some requests in the collection are informational / future-facing rather than hard pass-fail checks.

Handover impact:

- not a code blocker;
- but the team lead should not treat the Newman green summary as a complete substitute for targeted probe review.

## Q1. Do the implemented functionalities sufficiently cover Sierra-Lima's Assignment 3 ownership?

**Answer: yes, with the current local worktree.**

Assignment 3 and the repo's scope freeze (`0001-scope-freeze.md`) make Sierra-Lima responsible for:

- `Restaurant Service`
- `Menu Service`
- participation in W1 via:
  - `GET /restaurants/{id}/availability`
  - `POST /menu-items/validate`

Coverage confirmed in the current local repository:

- `Restaurant Service`
  - `POST /restaurants`
  - `GET /restaurants/{id}`
  - `PUT /restaurants/{id}`
  - `PATCH /restaurants/{id}/status`
  - `GET /restaurants`
  - `GET /restaurants/{id}/availability`
- `Menu Service`
  - `POST /restaurants/{rid}/menu-items`
  - `GET /restaurants/{rid}/menu-items`
  - `GET /menu-items/{id}`
  - `PUT /menu-items/{id}`
  - `DELETE /menu-items/{id}`
  - `POST /menu-items/validate`
- per-service persistence and migrations are present
  - `V1__init.sql`
  - `V2__seed_demo_data.sql`
- JWT-based route protection and owner/admin authorization are present
- W1 hop 4 and hop 5 were exercised successfully in the rebuilt Docker stack
- the optional Phase 16 `menu-events` emit point also worked in the current local stack

Important boundary note:

- `Review Service` remains design-only, which matches the frozen scope;
- login/signup, real gateway auth, full order placement, payment, delivery, and notification remain teammate-owned and are **not** required to count Sierra-Lima's Assignment 3 subset as covered.

Conclusion for Q1:

- **Sierra-Lima's Assignment 3-owned subset is sufficiently covered in breadth and in core runtime behavior.**
- the remaining concerns are handover/process quality and validation-tooling quality, not a scope hole in the A3-owned backend slice.

## Q2. What validation was Golf-Papa-Tango able to complete?

### Completed validation

| Area | Result |
| --- | --- |
| Assignment 3 scope cross-check | Completed from `dev-docs/course-materials/Assignment_3_2026.pdf` and `dev-docs/decisions/0001-scope-freeze.md` |
| Git state review | Completed; `HEAD` at `60fa710`, worktree dirty with 3 modified Menu Service files |
| Restaurant backend tests | Passed: `32/32` |
| Menu backend tests | Passed: `46/46` |
| Frontend lint | Passed |
| Frontend production build | Passed; hash `b56fb68e13e1cf00` |
| Compose config render | Passed |
| Docker rebuild | Passed with `docker compose --profile dev-gateway --env-file .env.example up --build -d` |
| Container health | Passed: frontend, dev-gateway, both DBs, Restaurant Service, and Menu Service healthy |
| Bash smoke | Passed: `services/local-dev/smoke.sh` |
| Bash cross-service smoke | Passed: `services/local-dev/smoke-cross-service.sh` |
| Menu-events evidence | Captured `2` lines in `services/local-dev/evidence/menu-events_20260419T181617Z.log` |
| Gateway reachability | Passed: `http://localhost:8080/healthz` |
| Frontend reachability | Passed: `http://localhost:8090/` |
| Frontend same-origin API proxy | Passed: `http://localhost:8090/api/restaurants?page=0&size=1 -> 200` |
| Dev-gateway API proxy | Passed: `http://localhost:8080/api/restaurants?page=0&size=1 -> 200` |
| Newman collection | Passed: `39` requests, `66` assertions, `0` failures |
| Direct regression probe: admin create menu under missing restaurant | Passed locally: `404` |
| Direct regression probe: owner create menu under missing restaurant | Passed locally: `404` |
| Direct regression probe: duplicate same-owner restaurant rename | Passed locally: `409` |
| Direct regression probe: invalid `operatingHours=24:00-24:00` | Passed locally: `400` |
| Direct regression probe: `DELETE /restaurants/{id}` | Passed locally: `405` |
| Direct regression probe: `PATCH /menu-items/{id}` | Passed locally: `405` |
| Direct regression probe: mixed-currency validate | Passed locally: `400` after creating a temporary USD item |

### Validation limitations and caveats

- `smoke.ps1` and `smoke-cross-service.ps1` could **not** be used as reliable validation artifacts on the local Windows shell because of the broken `Invoke-WebRequest -StatusCodeVariable` usage.
- the full Newman collection is helpful, but two requests observed in the same run were not actually green:
  - `POST /api/auth/login` via dev-gateway stub -> `501`
  - `POST /restaurants` in the CRUD folder -> `409`
- real teammate-owned services were not available from this repo alone, so the following were not end-to-end validated:
  - User Service
  - Order Service
  - Payment Service
  - Delivery Service
  - Notification Service
- I did not perform a manual browser walkthrough; frontend validation here was buildability, route/proxy reachability, and static code review.

## Q3. Final verdict on readiness to send the repository to the team lead for integration

**Final verdict: `CONDITIONALLY READY`.**

### Ready, if all of the following are true

- the team lead will receive the **current local repository contents**, not just commit `60fa710`;
- or the 3 modified Menu Service files are committed before handover;
- the team lead understands that the Windows PowerShell smoke scripts are currently broken and should use the bash scripts or raw HTTP probes instead.

### Not ready, if handover means "push current branch as-is"

If the team lead is expected to pull `HEAD` `60fa710` from git, my verdict becomes:

- **`NOT READY YET`**

Reason:

- the local worktree contains a meaningful Menu Service fix that is not part of the commit history yet.

### Recommendation before handover

1. Commit the 3 local Menu Service changes so the missing-restaurant fix is part of the actual handoff state.
2. Fix `smoke.ps1` and `smoke-cross-service.ps1` by removing the unsupported `-StatusCodeVariable` usage.
3. Tighten frontend validation so it matches backend rules for required `city` and strictly positive menu prices.
4. Optionally tighten the Newman collection so the known `409` and `501` requests are either asserted intentionally or moved out of the "green means good" path.

## Evidence produced during this audit

- `services/local-dev/evidence/cross-service-smoke_20260419T181617Z.log`
- `services/local-dev/evidence/menu-events_20260419T181617Z.log`

## Bottom line

For Sierra-Lima's actual Assignment 3 ownership, the current local repo is in good shape and the backend/runtime path is green. The two things preventing an unqualified sign-off are:

- the fix-bearing worktree is still uncommitted;
- Windows-native verification scripts are currently broken.

Once the Menu Service fix is committed, I would consider this repository fit to send to the team lead for integration, with the PowerShell-script issue tracked as a follow-up.
