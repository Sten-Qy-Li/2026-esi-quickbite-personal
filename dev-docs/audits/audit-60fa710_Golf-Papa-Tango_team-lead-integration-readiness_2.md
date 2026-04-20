# Team-Lead Integration Readiness Audit -- current local repository state

This file keeps the historical `60fa710` audit-chain name, but the conclusions below audit the **current local repository state at `HEAD d1cec1d`**.

| Field | Value |
| --- | --- |
| `HEAD` commit | `d1cec1d386c36e35af9f9220943da3930f49046a` |
| Commit subject | `Patch Golf-Papa-Tango 60fa710 audit_2 findings 2-4` |
| Branch observed | `dev` |
| Audit date | `2026-04-19` |
| Auditor | `Golf-Papa-Tango` |
| Scope | Sierra-Lima-owned slice: Restaurant Service, Menu Service, Sierra-Lima frontend surfaces, local-dev stack, and Sierra-Lima's W1 responsibilities |
| Working tree at audit start | `CLEAN` |
| Verdict | `CONDITIONALLY READY FOR TEAM-LEAD INTEGRATION` |

## Findings

### 1. Medium -- both services accept correctly signed JWTs even when the issuer claim is wrong

- Files:
  - `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/security/JwtAuthFilter.java:61`
  - `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/security/JwtProperties.java:12-30`
  - `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/security/JwtAuthFilter.java:61`
  - `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/security/JwtProperties.java:12-30`
- Code path:
  - both services load `jwt.issuer`;
  - both auth filters only parse the signed token, extract claims, and authenticate the request;
  - neither filter compares `claims.getIssuer()` to the configured issuer.
- Live reproduction on 2026-04-19:
  - I minted a valid HS256 token with repo-default secret and `iss="wrong-issuer"`.
  - `GET http://localhost:8081/restaurants/d0000001-0000-0000-0000-000000000001/availability` returned `200 OK`.
  - `POST http://localhost:8082/menu-items/validate` with the same token returned `200 OK` and `allValid=true`.
- Impact:
  - the intended trust boundary is weaker than the configuration suggests;
  - any actor holding the shared signing secret can mint tokens for an unexpected issuer and still access protected Sierra-Lima endpoints;
  - this is a real contract-hardening gap ahead of integration with the real User Service / gateway path.
- Recommendation before final sign-off:
  - enforce issuer equality in both `JwtAuthFilter` implementations;
  - add controller tests for `wrong issuer -> 401`.

### 2. Low -- service READMEs still describe the implementation as not yet created

- Files:
  - `services/README.md:23-24`
  - `services/restaurant-service/README.md:53`
  - `services/menu-service/README.md:58`
- Observed drift:
  - top-level `services/README.md` still says the Spring Boot skeletons are "not created yet";
  - both per-service READMEs still say "No code yet."
- Impact:
  - this does not break runtime behavior;
  - it does create avoidable confusion for a team lead receiving the repository for integration.
- Recommendation:
  - update these READMEs to reflect the actual implemented state and current run commands.

No additional reproducible functional defects were found in Sierra-Lima's owned runtime surface during this audit.

## Q1. Do the implemented functionalities sufficiently cover Sierra-Lima's Assignment 3 ownership?

**Answer: yes.**

Assignment 3 and the checked-in Assignment 3 submission assign Sierra-Lima to:

- `Restaurant Service`
- `Menu Service`
- W1 participation through:
  - `GET /restaurants/{id}/availability`
  - `POST /menu-items/validate`

Coverage confirmed in the current local repository:

- `Restaurant Service` exposes the expected six endpoints:
  - `POST /restaurants`
  - `GET /restaurants/{id}`
  - `PUT /restaurants/{id}`
  - `PATCH /restaurants/{id}/status`
  - `GET /restaurants`
  - `GET /restaurants/{id}/availability`
- `Menu Service` exposes the expected six endpoints:
  - `POST /restaurants/{rid}/menu-items`
  - `GET /restaurants/{rid}/menu-items`
  - `GET /menu-items/{id}`
  - `PUT /menu-items/{id}`
  - `DELETE /menu-items/{id}`
  - `POST /menu-items/validate`
- both services own their own schemas, Flyway migrations, and seed data;
- both W1 hops under Sierra-Lima ownership were exercised live and passed;
- owner/admin authorization is implemented for mutation endpoints;
- Sierra-Lima's local frontend slice exists as an extra convenience layer for browse/manage flows.

Boundary note:

- `Review Service` remains design-only, which matches the frozen A3 scope;
- teammate-owned end-to-end flows (`User`, `Order`, `Payment`, `Delivery`, `Notification`, real gateway auth orchestration) are outside Sierra-Lima's ownership and therefore outside the minimum "subset coverage" judgment here.

Conclusion:

- **Sierra-Lima's A3-owned subset is sufficiently covered in both breadth and live behavior.**

## Q2. What validation was Golf-Papa-Tango able to complete?

### Completed validation

| Area | Result |
| --- | --- |
| Assignment 3 scope cross-check | Completed from `dev-docs/course-materials/Assignment_3_2026.pdf` and `dev-docs/prior-submissions/Assignment-3-Submission.pdf` |
| Code review | Completed across Sierra-Lima backend, frontend, Docker stack, scripts, and docs |
| Restaurant backend tests | Passed: `32/32` via `mvn clean test` |
| Menu backend tests | Passed: `46/46` via `mvn clean test` |
| Frontend lint | Passed via `npm run lint` |
| Frontend production build | Passed via `npm run build` |
| Compose config render | Passed via `docker compose --profile dev-gateway ... config` |
| Docker rebuild and boot | Passed via `docker compose --profile dev-gateway ... up -d --build` |
| Container health | Passed: `frontend`, `dev-gateway`, both DBs, `restaurant-service`, `menu-service` all healthy |
| Sierra-Lima smoke | Passed via `services/local-dev/smoke.ps1` in `pwsh` |
| Cross-service smoke | Passed via `services/local-dev/smoke-cross-service.ps1` in `pwsh` |
| Menu-events evidence | Captured `2` log lines in `services/local-dev/evidence/menu-events_20260419T190125Z.log` |
| Frontend reachability | Passed: `http://localhost:8090 -> 200` |
| Dev-gateway reachability | Passed: `http://localhost:8080/healthz -> 200` |
| Newman `Restaurant CRUD` | Passed: `6` requests, `1` assertion, `0` failures |
| Newman `Menu CRUD` | Passed: `6` requests, `2` assertions, `0` failures |
| Newman `W1 Integration` | Passed: `9` requests, `40` assertions, `0` failures |
| Newman `Negative Auth` | Passed: `14` requests, `21` assertions, `0` failures |
| Targeted security probe | Reproduced issuer-enforcement defect on both services with a wrong-issuer token |
| Source-hygiene scan | No `TODO` / `FIXME` / `XXX` markers found in `services/` source files |

### Validation limitations and notes

- Real teammate-owned services were not present from this repository alone, so I could **not** complete a true end-to-end W1 through `User`, `Order`, `Payment`, `Delivery`, and `Notification`.
- I did not run a manual browser walkthrough; frontend validation here was static review, lint/build, container boot, and HTTP reachability.
- The PowerShell smoke scripts are **PowerShell 7** scripts in practice because they rely on `Invoke-WebRequest -SkipHttpErrorCheck`; they pass in `pwsh`, but invoking them through legacy `powershell.exe` will fail.

## Q3. Final verdict on readiness to send the repository to the team lead for integration

**Final verdict: `CONDITIONALLY READY FOR TEAM-LEAD INTEGRATION`.**

Why it is ready enough to hand over for integration work:

- Sierra-Lima's Assignment 3-owned services are implemented and live;
- the full local build/test/runtime matrix is green:
  - backend tests,
  - frontend lint/build,
  - Docker health,
  - Sierra-Lima smoke,
  - cross-service smoke,
  - and the curated Newman folders;
- no owned-scope functional gap comparable to the earlier orphan-menu defect was reproduced.

Why the verdict is still conditional instead of unqualified:

- Finding 1 is real and live: issuer pinning is configured but not enforced in either service;
- Finding 2 means the handoff documentation is stale enough to misrepresent repository maturity.

My recommendation:

1. Fix issuer enforcement in both JWT filters and add the negative tests.
2. Refresh the stale READMEs under `services/`.
3. Then treat the repository as fully ready for handoff.

If the team lead needs the repository **now** for functional integration, I would send it with an explicit note that:

- Sierra-Lima's owned runtime surface is green;
- the remaining open issue is JWT issuer hardening, not missing W1 functionality.

## Evidence summary

- Backend tests passed: `78/78`
- Frontend lint/build passed
- Docker stack healthy under dev-gateway profile
- PowerShell smokes passed in `pwsh`
- Newman folders passed: `35` requests, `64` assertions, `0` failures
- Evidence logs produced:
  - `services/local-dev/evidence/cross-service-smoke_20260419T190125Z.log`
  - `services/local-dev/evidence/menu-events_20260419T190125Z.log`

## Bottom line

Sierra-Lima's subset is implemented, runnable, and integration-capable. The repository is close to handoff quality, but not yet at the "as bug-free as possible" bar because both services still trust a wrong-issuer JWT as long as it is signed with the shared secret.
