# Team-Lead Integration Readiness Audit -- commit `60fa710`

| Field | Value |
| --- | --- |
| Commit under audit | `60fa710c29e4417208864dce3d710e856bd782bf` (short `60fa710`) |
| Commit subject | `Patch Golf-Papa-Tango bcc9dd0 findings 1-2` |
| Branch observed | `dev` |
| Auditor | Golf-Papa-Tango |
| Audit date | 2026-04-19 |
| Scope | Sierra-Lima-owned slice: Restaurant Service, Menu Service, Sierra-Lima frontend surfaces, local-dev stack, and Sierra-Lima's W1 responsibilities |
| Verdict | `NOT READY TO HAND OVER AS FINAL` |

## Findings

### 1. Medium -- `Admin` can still create orphan menu items under a nonexistent restaurant id

- Files:
  - `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java:76-85`
  - `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java:139-147`
  - `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/service/MenuServiceTest.java:273-293`
- Contract context:
  - Assignment 3 §3.4 defines `POST /restaurants/{rid}/menu-items` as "Add a new menu item to a restaurant."
  - Assignment 3 §5.1 assumes Menu Service validates real restaurant-backed order items during W1.
  - The repo's own prior submission (`Assignment-3-Submission.docx` §§3.4, 4.4, 5.1) models `MenuItem.restaurantId` as a reference to a real Restaurant aggregate.
- Code path:
  - `create()` persists the incoming `restaurantId` immediately after `requireOwnerOrAdmin(...)`.
  - `requireOwnerOrAdmin(...)` returns early for `Admin` at line 141, before `restaurantOwnership.findOwnerId(...)` is called.
  - The current service tests explicitly encode the admin bypass, but there is no missing-parent regression test.
- Live reproduction on 2026-04-19:
  - Owner token: `POST /restaurants/ffffffff-ffff-ffff-ffff-ffffffffffff/menu-items` returned `404 Not Found`.
  - Admin token: the same request returned `201 Created`.
  - I then called `POST /menu-items/validate` with that orphan `menuItemId`; it returned `200 OK` with `allValid=true`, `exists=true`, and `isAvailable=true`.
  - I deleted the temporary orphan row immediately with `DELETE /menu-items/{id}` -> `204 No Content`.
- Impact:
  - Menu Service can persist a menu item whose parent restaurant does not exist.
  - That orphan row is then accepted by Sierra-Lima's own W1 validation endpoint, so the defect is not isolated to an admin-only CRUD path; it leaks into the contract surface used for order placement.
  - `60fa710` correctly fixes the earlier duplicate-name and `405` issues, but this remaining integrity gap is still a real runtime defect.
- Recommended fix before handover:
  - Split parent existence from authorization in `MenuService.create(...)`.
  - Always resolve restaurant existence first for `POST /restaurants/{rid}/menu-items`, then apply owner/admin authorization.
  - Add service and controller coverage for `Admin + missing restaurant -> 404`.

No additional runtime defects were reproduced in this audit beyond Finding 1.

## Q1 -- Do the implemented functionalities sufficiently cover Sierra-Lima's Assignment 3 ownership?

**Answer: yes in scope coverage, no as a final-quality handover.**

What Assignment 3 requires:

- `Assignment_3_2026.pdf` requires each team to define and implement:
  - service APIs,
  - service data models,
  - system workflows,
  - integration mechanisms,
  - and final implementation responsibilities.
- The same assignment also states that each student is initially responsible for **two microservices** and that each implemented service must participate in at least one implemented workflow.

What Sierra-Lima is responsible for in the assignment/submission pack:

- The checked-in Assignment 3 submission (`Assignment-3-Submission.docx`) assigns Sierra-Lima to:
  - `Restaurant Service` (`§3.3`, `§4.3`),
  - `Menu Service` (`§3.4`, `§4.4`),
  - W1 participation through `GET /restaurants/{id}/availability` and `POST /menu-items/validate` (`§5.1`),
  - no baseline producer/consumer role in W2/W3.
- The repo decision records `0001-scope-freeze.md` and `0002-workflows.md` match that same assignment split.

What the repository at `60fa710` covers:

- Restaurant Service exposes the expected six endpoints.
- Menu Service exposes the expected six endpoints.
- Restaurant and Menu database migrations and DTO validation are present and runnable.
- W1 hop 4 and hop 5 are implemented and live.
- The optional `menu.item-availability-changed` producer is present behind the log-only seam.
- Sierra-Lima's frontend slice includes browse/manage routes for restaurants and menus.

So the Sierra-Lima subset is covered in breadth. The negative overall verdict comes from the remaining Menu create-path defect, not from missing scope.

## Q2 -- What validation was Golf-Papa-Tango able to complete?

### Completed validation

| Area | Result |
| --- | --- |
| Assignment 3 scope cross-check | Completed via `pdftotext` on `dev-docs/course-materials/Assignment_3_2026.pdf` |
| Prior submission scope cross-check | Completed via OOXML extraction from `dev-docs/prior-submissions/Assignment-3-Submission.docx` |
| Commit-diff review | Completed for `bcc9dd0..60fa710`; confirmed the commit fixes the prior duplicate-update and `405` findings |
| Restaurant backend tests | Passed: `32/32` |
| Menu backend tests | Passed: `44/44` |
| Frontend lint | Passed |
| Frontend build | Passed; hash `b56fb68e13e1cf00` |
| Docker stack health | Passed; all 6 Sierra-Lima containers healthy |
| `smoke.sh` | Passed |
| `smoke-cross-service.sh` | Passed; Sierra-Lima failures `0`, teammate probes skipped where URLs were unset |
| Newman / Postman suite | Passed: `39` requests, `66` assertions, `0` failures |
| Live regression probe: duplicate restaurant rename | Passed as fixed: same-owner duplicate rename now returns `409 Conflict` |
| Live regression probe: unsupported `DELETE /restaurants/{id}` | Passed as fixed: now returns `405 Method Not Allowed` |
| Live regression probe: unsupported `PATCH /menu-items/{id}` | Passed as fixed: now returns `405 Method Not Allowed` |
| Live regression probe: invalid `operatingHours=24:00-24:00` | Passed as fixed: returns `400 Bad Request` |
| Live regression probe: mixed EUR/USD validate request | Passed as fixed: returns `400 Bad Request` |
| Live regression probe: paged `GET /restaurants?page=0&size=1` | Passed as fixed: response contains `content` and paging metadata |
| Live edge probe: owner creates menu item under missing restaurant | Correctly returned `404 Not Found` |
| Live edge probe: admin creates menu item under missing restaurant | Reproduced defect: returned `201 Created` |
| Live edge probe: validate orphan menu item | Reproduced defect: returned `200 OK` with `allValid=true` |

### Validation not completed

These remain outside what I could complete in this personal repository audit:

- True end-to-end W1 through teammate-owned `User`, `Order`, `Payment`, `Delivery`, and `Notification` services.
- JWT interoperability against Alfa-Kilo's real issuer instead of the local dev minting path.
- Full W2/W3 broker-backed verification with teammate-owned producers and consumers.
- Manual browser walkthrough of the frontend slice. I verified routing/buildability and backend reachability, but not a human-driven UI session.

## Q3 -- Final verdict on readiness to send the repository to the team lead for integration

**Final verdict: `NOT READY TO HAND OVER AS FINAL`.**

Why:

- `60fa710` successfully fixes the two defects previously raised against `bcc9dd0`:
  - duplicate same-owner restaurant rename now returns `409`,
  - unsupported methods now return `405` instead of `500`.
- The standard local validation matrix is otherwise green:
  - backend tests,
  - frontend lint/build,
  - Docker health,
  - smoke scripts,
  - Newman suite,
  - and prior regression probes.
- But Finding 1 is still a real runtime defect on Sierra-Lima's owned contract surface:
  - Menu Service allows `Admin` to create a menu item under a nonexistent restaurant,
  - and that orphan row is then accepted by `POST /menu-items/validate`.

My recommendation:

1. Fix the missing-parent check on `POST /restaurants/{rid}/menu-items` for `Admin` callers.
2. Add regression tests for `Admin + missing restaurant -> 404`.
3. Re-run the same local matrix.

If the team lead urgently needs the branch for early integration work, the repository is close and most of the matrix is green. But for the requested "as bug-free as possible" handover, I would not sign off `60fa710` until this defect is closed.

## Evidence summary

- Assignment 3 contract categories: confirmed locally from `Assignment_3_2026.pdf`
- Sierra-Lima ownership mapping: confirmed locally from `Assignment-3-Submission.docx`, `0001`, and `0002`
- Backend tests: `76/76` passed total
- Frontend lint/build: passed
- Docker stack: healthy
- Smoke scripts: both passed
- Newman: `39` requests, `66` assertions, `0` failures
- Live probes:
  - duplicate rename -> `409`
  - unsupported restaurant `DELETE` -> `405`
  - unsupported menu-item `PATCH` -> `405`
  - invalid `operatingHours` -> `400`
  - mixed currency validate -> `400`
  - paged `GET /restaurants` shape preserved
  - owner create under missing restaurant -> `404`
  - admin create under missing restaurant -> `201`
  - orphan item validate -> `200 allValid=true`
