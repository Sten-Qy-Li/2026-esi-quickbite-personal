# Team-Lead Integration Readiness Audit -- commit `7b2fa61`

| Field | Value |
| --- | --- |
| Commit under audit | `7b2fa61` |
| Branch observed | `dev` |
| Auditor | Golf-Papa-Tango |
| Audit date | 2026-04-20 |
| Status | Complete |

## Scope reviewed

This audit is scoped to Sierra-Lima's Assignment 3 ownership and to the local validation surface that can be executed reliably from this repository.

Assignment 3 requires each student to define:

- the REST APIs of the services they implement
- the data models of those services
- the workflows those services participate in
- the final implementation responsibilities for the project

For Sierra-Lima, the authoritative ownership slice used in this audit is:

- `Restaurant Service` for R19 and R20
- `Menu Service` for R21 and R22
- participation in workflow `W1` as the synchronous callee for restaurant availability and menu validation
- no direct ownership in `W2` or `W3`, except that Sierra-Lima must remain compatible and non-disruptive during those teammate-owned flows

The frontend, local Docker/dev-gateway stack, Postman assets, and smoke scripts are treated as supporting implementation and validation assets rather than the primary Assignment 3 ownership boundary.

## Evidence used

Scope and assignment evidence reviewed so far:

- `dev-docs/course-materials/Assignment_3_2026.pdf`
- `dev-docs/prior-submissions/Assignment-3-Submission.pdf`
- `dev-docs/decisions/0001-scope-freeze.md`
- `dev-docs/decisions/0002-workflows.md`
- `dev-docs/decisions/0020-sierra-lima-contracts.md`
- `dev-docs/report-draft-backend_Sierra-Lima.md`
- `services/README.md`
- `services/local-dev/README.md`
- `services/local-dev/runbook.md`

Current implementation files reviewed so far:

- `services/restaurant-service/src/main/java/.../controller/RestaurantController.java`
- `services/restaurant-service/src/main/java/.../service/RestaurantService.java`
- `services/restaurant-service/src/main/java/.../config/SecurityConfig.java`
- `services/restaurant-service/src/main/java/.../security/JwtAuthFilter.java`
- `services/restaurant-service/src/main/java/.../dto/CreateRestaurantRequest.java`
- `services/menu-service/src/main/java/.../controller/MenuController.java`
- `services/menu-service/src/main/java/.../service/MenuService.java`
- `services/menu-service/src/main/java/.../config/SecurityConfig.java`
- `services/menu-service/src/main/java/.../dto/CreateMenuItemRequest.java`
- `services/frontend/quickbite-frontend/src/router/index.js`
- `services/frontend/quickbite-frontend/src/views/AddRestaurantView.vue`
- `services/frontend/quickbite-frontend/src/views/RestaurantDetailView.vue`
- `services/frontend/quickbite-frontend/src/views/AddMenuItemView.vue`
- `services/frontend/quickbite-frontend/src/views/MenuItemDetailView.vue`

Change focus narrowed by diff from `d23145f..7b2fa61`, especially:

- restaurant DTO validation hardening
- duplicate-name protection on restaurant update
- JWT issuer enforcement
- method-not-allowed handling
- menu create-path ownership lookup hardening
- frontend client-side validation alignment
- smoke-script and Postman updates

## Scope coverage assessment

Final assessment:

- **Yes**: the repository sufficiently covers Sierra-Lima's Assignment 3 ownership.
- `Restaurant Service` implements the six contracted endpoints from `0020-sierra-lima-contracts.md` and the A3 submission: create, get, list, update, status toggle, and availability.
- `Menu Service` implements the six contracted endpoints from `0020-sierra-lima-contracts.md` and the A3 submission: create, list, get, update, delete, and batch validate.
- Sierra-Lima's W1 responsibilities are present and working locally:
  - `GET /restaurants/{id}/availability`
  - `POST /menu-items/validate`
- Sierra-Lima remains correctly out of teammate-owned W2/W3 business logic. The optional `menu-events` publisher exists as a local stretch feature and does not conflict with the Assignment 3 boundary.
- Supporting local artifacts exceed the strict A3 minimum and improve handoff readiness:
  - Vue frontend for browse and owner CRUD flows
  - Docker/dev-gateway compose stack
  - PowerShell smoke scripts
  - Postman/Newman collection

Conclusion for Question 1: Sierra-Lima's owned subset is implemented and locally demonstrable. The remaining caveats are about validation reliability and a small frontend/backend validation mismatch, not missing Sierra-Lima scope.

## Validations completed

Repository and scope checks:

- confirmed current audited commit is `7b2fa61`
- confirmed the worktree is clean except for the new audit report and untracked local evidence logs under `services/local-dev/evidence/`
- reviewed Assignment 3 prompt, Assignment 3 submission, Sierra-Lima contract/workflow decisions, current service code, and recent diffs from `d23145f..7b2fa61`

Backend automated validation:

- `services/restaurant-service`: `mvn test` passed with `33/33` tests green
- `services/menu-service`: `mvn test` passed with `47/47` tests green

Frontend validation:

- `services/frontend/quickbite-frontend`: `npm run lint -- --no-fix` passed
- `services/frontend/quickbite-frontend`: `npm run build` passed

Local integration/runtime validation:

- rebuilt the local Docker stack with `docker compose --profile dev-gateway up -d --build`
- verified all six local compose services healthy after rebuild
- `pwsh -File services/local-dev/smoke.ps1` passed
- `pwsh -File services/local-dev/smoke-cross-service.ps1` passed
  - Sierra-Lima failures: `0`
  - teammate failures: `0`
  - teammate probes were skipped cleanly where URLs were unset
  - `menu-events` log evidence was captured locally
- `npx --yes newman run services/local-dev/postman/QuickBite.postman_collection.json -e services/local-dev/postman/QuickBite.postman_environment.json` passed
  - `39` requests
  - `68` assertions
  - `0` failures

Direct live probes against the rebuilt stack:

- confirmed invalid restaurant hours are rejected:
  - `POST /restaurants` with `24:00-24:00` -> `400`
  - `PUT /restaurants/{id}` with `29:59-29:59` -> `400`
- confirmed duplicate restaurant rename is rejected:
  - `PUT /restaurants/{id}` to existing same-owner name -> `409`
- confirmed unsupported methods return explicit `405` JSON envelopes:
  - `DELETE /restaurants/{id}` -> `405`
  - `PATCH /menu-items/{id}` -> `405`
- confirmed wrong-issuer JWTs are rejected:
  - restaurant availability with wrong issuer -> `401`
  - menu validate with wrong issuer -> `401`
- confirmed admin create against an unknown restaurant is rejected:
  - `POST /restaurants/{missing}/menu-items` -> `404`
- confirmed positive restaurant update still succeeds live despite the Newman blind spot:
  - direct `POST /restaurants` -> `201`
  - direct follow-up `PUT /restaurants/{id}` with a unique updated name -> `200`

## Findings and risks

### 1. Medium -- the checked-in Newman collection overstates reliable coverage of positive restaurant update on non-pristine local volumes

- File: `services/local-dev/postman/QuickBite.postman_collection.json:226`
- Evidence:
  - the request body at `services/local-dev/postman/QuickBite.postman_collection.json:235` hard-codes the update name to `Pizza Antonio (updated)`
  - that request has no test/assertion block around it in the collection segment at `:226-242`
  - during the 2026-04-20 audit run, the full Newman collection still passed overall, but this request itself returned `409 Conflict`
- Why this matters:
  - the collection is not fully reliable as a positive CRUD acceptance gate once the local Postgres volume already contains an earlier restaurant updated to the same static name
  - because Sierra-Lima has no `DELETE /restaurants/{id}` endpoint, those earlier audit/demo rows accumulate across local runs
  - the collection therefore gives a greener signal than it should for this specific path unless run against pristine data or patched to use a unique update name plus a `200` assertion
- Impact on readiness:
  - not a runtime blocker for the actual service, because direct live probing and backend tests still confirmed successful `PUT /restaurants/{id}`
  - it is a real limitation on what Golf-Papa-Tango can claim the Postman/Newman pack validates locally and reliably

### 2. Low -- frontend restaurant-hours validation is still looser than backend validation

- Files:
  - `services/frontend/quickbite-frontend/src/views/AddRestaurantView.vue:55`
  - `services/frontend/quickbite-frontend/src/views/AddRestaurantView.vue:74`
  - `services/frontend/quickbite-frontend/src/views/RestaurantDetailView.vue:90`
  - `services/frontend/quickbite-frontend/src/views/RestaurantDetailView.vue:124`
- Evidence:
  - the frontend regex only checks `HH:MM-HH:MM` shape, not the backend's stricter `00-23` hour range
  - by inspection, values like `29:59-29:59` match the frontend pattern and are only rejected by the backend after submission
- Why this matters:
  - this is a UX-validation mismatch, not a data-integrity bug
  - users can still submit impossible hours and only see the failure after the round-trip
- Impact on readiness:
  - non-blocking for team-lead integration because backend validation now correctly protects persistence and W1 logic

## Explicit gaps or unverified assumptions

- I did **not** validate full end-to-end W1 with teammate-owned real services (`User`, `Order`, `Payment`, `Delivery`, `Notification`). Local validation here covers Sierra-Lima's owned endpoints and the dev-gateway stub.
- I did **not** validate broker-backed W2 or W3 in a real teammate integration environment. Sierra-Lima is intentionally a non-owner there.
- I did **not** validate interoperability against Alfa-Kilo's real JWT issuer; local checks use the shared dev HS256 secret and also include wrong-issuer rejection probes.
- The cross-service smoke script only probes teammate services when their URLs are configured. In this audit run those URLs were unset, so those probes were skipped rather than exercised.
- I did **not** reset Docker volumes to a pristine state. The local Restaurant database therefore still contains pre-existing demo/audit rows, plus additional audit-created restaurant rows from this run. That persistence is part of why the Postman `PUT /restaurants/{id}` request is not fully reliable across repeated runs.
- I did **not** execute browser-level frontend interaction tests. Frontend validation here is lint/build plus code inspection of the changed forms and routes.

## Final verdict

**READY TO HAND OVER, with two caveats that should be documented for the team lead.**

Why:

- Sierra-Lima's Assignment 3 scope is present and locally demonstrable.
- All critical automated checks passed on commit `7b2fa61`:
  - backend tests `80/80`
  - frontend lint/build passed
  - compose rebuild and health passed
  - Sierra-Lima smoke scripts passed
  - Newman passed with `39` requests and `68` assertions
- direct live probes confirmed the recent hardening fixes behave correctly on the rebuilt stack

Caveats:

- the service code itself looks integration-ready, but the checked-in Newman collection should **not** be treated as a fully reliable positive-update gate for restaurants on non-pristine local volumes until its `PUT /restaurants/{id}` request is made unique and asserted
- the frontend's restaurant-hours validation is slightly looser than the backend's, which is a user-experience issue rather than a handoff blocker
