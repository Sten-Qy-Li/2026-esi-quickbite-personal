# Team-Lead Integration Readiness Audit -- current local repository state

This file keeps the historical `60fa710` audit-chain name, but the conclusions below audit the current local repository state at `HEAD d1cec1d386c36e35af9f9220943da3930f49046a` on `2026-04-20`.

| Field | Value |
| --- | --- |
| `HEAD` commit | `d1cec1d386c36e35af9f9220943da3930f49046a` |
| Commit subject | `Patch Golf-Papa-Tango 60fa710 audit_2 findings 2-4` |
| Branch observed | `dev` |
| Audit date | `2026-04-20` |
| Auditor | `Golf-Papa-Tango` |
| Scope | Sierra-Lima-owned Assignment 3 slice: `Restaurant Service`, `Menu Service`, Sierra-Lima local-dev stack, and supporting frontend/runtime surfaces relevant to integration handoff |
| Working tree at audit start | `DIRTY` -- modified `dev-docs/audits/audit-60fa710_Golf-Papa-Tango_team-lead-integration-readiness_2.md` plus untracked evidence logs under `services/local-dev/evidence/` |
| Verdict | `CONDITIONALLY READY FOR TEAM-LEAD INTEGRATION` |

## Scope reviewed

- Assignment 3 baseline: `dev-docs/course-materials/Assignment_3_2026.pdf`
- Team-specific Assignment 3 ownership split: `dev-docs/prior-submissions/Assignment-3-Submission.pdf`
- Frozen Sierra-Lima service contract: `dev-docs/decisions/0020-sierra-lima-contracts.md`
- Sierra-Lima implementation:
  - `services/restaurant-service`
  - `services/menu-service`
  - `services/local-dev`
  - `services/frontend/quickbite-frontend`

## Evidence used

- Contract and scope documents:
  - `dev-docs/course-materials/Assignment_3_2026.pdf`
  - `dev-docs/prior-submissions/Assignment-3-Submission.pdf`
  - `dev-docs/decisions/0020-sierra-lima-contracts.md`
- Source review:
  - `services/restaurant-service/src/main/java/...`
  - `services/menu-service/src/main/java/...`
  - `services/restaurant-service/src/test/java/...`
  - `services/menu-service/src/test/java/...`
  - `services/local-dev/docker-compose.yml`
  - `services/local-dev/smoke.ps1`
  - `services/local-dev/smoke-cross-service.ps1`
  - `services/frontend/quickbite-frontend/package.json`
- Runtime evidence produced during this audit:
  - `services/local-dev/evidence/cross-service-smoke_20260420T051833Z.log`
  - `services/local-dev/evidence/menu-events_20260420T051833Z.log`

## Findings

### 1. Medium -- both services still accept correctly signed JWTs even when the issuer claim is wrong

- Files:
  - `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/security/JwtAuthFilter.java:58-76`
  - `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/security/JwtProperties.java:11-30`
  - `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/security/JwtAuthFilter.java:58-76`
  - `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/security/JwtProperties.java:11-30`
- Source evidence:
  - both filters parse the signed JWT and extract claims;
  - neither filter compares `claims.getIssuer()` with the configured `jwt.issuer`;
  - both services still load `jwt.issuer`, which makes the missing check especially misleading.
- Live reproduction on `2026-04-20`:
  - I minted a valid HS256 token using the repo-default secret and `iss="wrong-issuer"`;
  - `GET http://localhost:8081/restaurants/4eaaf550-89d1-4f23-9456-3d97f05d5a6f/availability` returned `200`;
  - `POST http://localhost:8082/menu-items/validate` with the same token returned `200` and `allValid=true`.
- Impact:
  - the configured issuer boundary is not actually enforced;
  - any actor holding the shared signing secret can mint tokens for an unexpected issuer and still reach protected Sierra-Lima endpoints;
  - this remains a real hardening gap ahead of team-level auth and gateway integration.
- Recommendation:
  - enforce issuer equality in both `JwtAuthFilter` implementations;
  - add negative controller tests for `wrong issuer -> 401` in both services.

### 2. Low -- core service READMEs still describe the codebase as not yet implemented

- Files:
  - `services/README.md:21-28`
  - `services/restaurant-service/README.md:51-55`
  - `services/menu-service/README.md:56-61`
- Evidence:
  - the top-level services README still says the Spring Boot project skeletons are "not created yet";
  - both service READMEs still say "No code yet."
- Impact:
  - runtime behavior is unaffected;
  - handoff clarity is degraded because the checked-in documentation understates repository maturity and can mislead the team lead during integration.
- Recommendation:
  - update these READMEs to reflect the implemented services, current run commands, and the actual local-dev workflow.

No additional reproducible Sierra-Lima-owned functional defects were found during this audit.

## Q1. Do the current functionalities sufficiently cover Sierra-Lima's Assignment 3 ownership?

**Answer: yes.**

The team Assignment 3 submission assigns Sierra-Lima to:

- `Restaurant Service`
- `Menu Service`
- W1 participation through:
  - `GET /restaurants/{id}/availability`
  - `POST /menu-items/validate`

Coverage confirmed in the current local repository:

- `Restaurant Service` implements the expected six endpoints:
  - `POST /restaurants`
  - `GET /restaurants/{id}`
  - `PUT /restaurants/{id}`
  - `PATCH /restaurants/{id}/status`
  - `GET /restaurants`
  - `GET /restaurants/{id}/availability`
- `Menu Service` implements the Sierra-Lima contract surface:
  - `POST /restaurants/{rid}/menu-items`
  - `GET /restaurants/{rid}/menu-items`
  - `GET /menu-items/{id}`
  - `PUT /menu-items/{id}`
  - `DELETE /menu-items/{id}`
  - `POST /menu-items/validate`
- both services own their own persistence, Flyway-backed schema, and service-local runtime configuration;
- Sierra-Lima's W1-owned hops passed in live smoke:
  - restaurant availability check;
  - menu batch validation;
- owner/admin authorization is implemented for mutating endpoints;
- the local-dev stack, frontend, and gateway stub provide a usable integration surface for team-level handoff even though they are not the core A3 ownership unit.

Boundary note:

- `Review Service` remains design-only, which matches the team Assignment 3 submission;
- teammate-owned true end-to-end behavior across `User`, `Order`, `Payment`, `Delivery`, `Notification`, and the real gateway remains outside Sierra-Lima's minimum ownership obligation.

Conclusion:

- **Sierra-Lima's Assignment 3-owned subset is functionally covered.**

## Q2. What validation was Golf-Papa-Tango able to complete locally and reliably?

| Area | Result |
| --- | --- |
| Assignment 3 scope cross-check | Completed from `Assignment_3_2026.pdf`, `Assignment-3-Submission.pdf`, and `0020-sierra-lima-contracts.md` |
| Code review | Completed across Sierra-Lima backend, tests, local-dev stack, and frontend support surface |
| Restaurant backend tests | Passed: `32/32` via `mvn clean test` |
| Menu backend tests | Passed: `46/46` via `mvn clean test` |
| Frontend lint | Passed via `npm run lint` |
| Frontend production build | Passed via `npm run build` |
| Compose render | Passed via `docker compose --profile dev-gateway config` |
| Docker rebuild and boot | Passed via `docker compose --profile dev-gateway up -d --build` |
| Container health | Passed: `frontend`, `dev-gateway`, `restaurant-db`, `menu-db`, `restaurant-service`, and `menu-service` all healthy |
| Sierra-Lima smoke | Passed via `pwsh -File services/local-dev/smoke.ps1` |
| Cross-service smoke | Passed via `pwsh -File services/local-dev/smoke-cross-service.ps1`; teammate probes were skipped when unset |
| Frontend reachability | Passed: `http://localhost:8090 -> 200` |
| Dev-gateway reachability | Passed: `http://localhost:8080/healthz -> 200` |
| Menu-events evidence | Captured `2` log lines in `services/local-dev/evidence/menu-events_20260420T051833Z.log` |
| Wrong-issuer security probe | Reproduced issuer-enforcement defect live on both services |
| Source-hygiene scan | No `TODO` / `FIXME` / `XXX` markers found in authored backend, local-dev, or frontend source files scanned during this audit |

## Q3. Final verdict on readiness for handoff to the team lead for integration

**Final verdict: `CONDITIONALLY READY FOR TEAM-LEAD INTEGRATION`.**

Why it is ready enough to hand over for integration work:

- Sierra-Lima's Assignment 3-owned backend slice is implemented and live;
- the current local validation matrix is green for:
  - backend unit/integration-style tests,
  - frontend lint/build,
  - compose rendering,
  - Docker boot and health,
  - Sierra-Lima smoke,
  - cross-service smoke within the limits of this repo;
- the live runtime checks confirmed the key owned workflow edges used in W1.

Why the verdict is still conditional rather than fully clean:

- Finding 1 is still live and security-relevant: issuer pinning is configured but not enforced;
- Finding 2 means handoff documentation is stale enough to misrepresent actual repository maturity.

Recommended pre-handoff fixes:

1. Enforce `iss` validation in both JWT filters and add the negative tests.
2. Refresh the stale README files under `services/`.
3. Re-cut the handoff from a clean working tree after deciding which evidence logs should remain versioned.

If the repository must be handed to the team lead immediately, I would send it with an explicit note that:

- Sierra-Lima's owned runtime surface is green;
- the remaining open issue is JWT issuer hardening, not missing Restaurant/Menu functionality.

## Explicit gaps or unverified assumptions

- Teammate-owned services were not configured in this personal repository during the audit, so I could not complete a true end-to-end W1 through `User`, `Order`, `Payment`, `Delivery`, and `Notification`.
- I did not rerun the Postman collection via Newman during this audit because a local `newman` CLI was not present. The audit instead relied on source review, unit tests, frontend checks, compose validation, and both PowerShell smoke scripts.
- Frontend validation here was static plus deployability-focused:
  - lint;
  - production build;
  - container health;
  - HTTP reachability.
  I did not perform a full manual browser click-through.
- `GET /restaurants` currently returns raw `Page<RestaurantResponse>` from `RestaurantController` (`services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantController.java:71-80`). During `mvn clean test`, Spring Data logged a warning that raw `PageImpl` JSON is not a stable contract. This is not a reproduced failure at the current version, but it is a residual contract-stability risk.
- The working tree was already dirty at audit start, and this audit added fresh evidence logs under `services/local-dev/evidence/`. Repository cleanliness therefore still needs an explicit handoff decision.

## Bottom line

Sierra-Lima's owned slice is implemented, testable, and integration-capable. The repository is close to handoff quality, but it is not yet at the "as bug-free as possible" bar because both services still trust a wrong-issuer JWT as long as it is signed with the shared secret, and the core handoff READMEs still describe the implementation as not yet built.
