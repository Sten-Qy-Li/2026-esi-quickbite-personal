# Chat Archive - 2026-04-19 - Golf-Papa-Tango (`60fa710`)

## Session Summary

This session performed a fresh audit of commit `60fa710` in the
Sierra-Lima QuickBite personal repository, then wrote the requested
Markdown audit report under `dev-docs/audits`.

The session had three main phases:

1. map Sierra-Lima's Assignment 3 obligations from the repo decisions and
   the local Assignment 3 / prior-submission artefacts
2. inspect the `60fa710` patch against the earlier `bcc9dd0` audit chain
   and rerun the local validation matrix
3. identify and document the remaining runtime defect, then author the
   final Golf-Papa-Tango audit report for this commit

## User Context

- Repository:
  `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Agent call-sign for this session: `Golf-Papa-Tango`
- Commit under audit: `60fa710`
- Branch during the session: `dev`
- Workspace date: `2026-04-19`

The user asked for:

1. a thorough audit of commit `60fa710`
2. answers to:
   - whether the repo sufficiently covers Sierra-Lima's Assignment 3 slice
   - what validation Golf-Papa-Tango was able to complete
   - the final readiness verdict for sending the repo to the team lead
3. a Markdown audit report under `dev-docs/audits`
4. the report filename to include short hash `60fa710` and call-sign
   `Golf-Papa-Tango`

## Repo and Document Context Reviewed

The session grounded the audit in both code and repo-local design docs.

Key documents reviewed:

- `dev-docs/decisions/0001-scope-freeze.md`
- `dev-docs/decisions/0002-workflows.md`
- `dev-docs/decisions/0020-sierra-lima-contracts.md`
- `dev-docs/report-draft-backend_Sierra-Lima.md`
- `dev-docs/audits/audit-bcc9dd0_Golf-Papa-Tango_final-handover-readiness.md`
- `dev-docs/audits/audit-d23145f_Golf-Papa-Tango_integration-handover-readiness.md`
- `dev-docs/course-materials/Assignment_3_2026.pdf`
- `dev-docs/prior-submissions/Assignment-3-Submission.docx`

Key code paths reviewed:

- `services/restaurant-service/.../RestaurantService.java`
- `services/restaurant-service/.../RestaurantRepository.java`
- `services/restaurant-service/.../GlobalExceptionHandler.java`
- `services/menu-service/.../MenuService.java`
- `services/menu-service/.../RestaurantOwnershipClient.java`
- `services/menu-service/.../GlobalExceptionHandler.java`
- Restaurant/Menu controller and service tests
- frontend routing and browse/menu views

## Commit Delta Reviewed

The session first diffed `bcc9dd0..60fa710` to verify what this commit
claimed to fix.

Confirmed patch content:

- closes the earlier same-owner duplicate-name gap on
  `PUT /restaurants/{id}`
- adds explicit `HttpRequestMethodNotSupportedException -> 405`
  handlers in both services
- adds the corresponding regression tests
- adds the previously missing `bcc9dd0` audit artefacts and evidence logs

This meant the audit focus was:

1. verify those two earlier findings are actually fixed at runtime
2. check whether any Sierra-Lima defects still remain despite the green
   test matrix

## Validation Work Executed

### Assignment and ownership validation

The session confirmed directly from `Assignment_3_2026.pdf` that
Assignment 3 requires:

- service APIs
- service data models
- system workflows
- integration mechanisms
- final implementation responsibilities
- two microservices per student unless one is replaced by a shared
  integration/resilience component

The session also extracted `Assignment-3-Submission.docx` directly and
confirmed that Sierra-Lima owns:

- Restaurant Service
- Menu Service
- W1 participation through:
  - `GET /restaurants/{id}/availability`
  - `POST /menu-items/validate`

### Automated test and build work

Executed successfully:

- `mvn test` in `services/restaurant-service`
  - result: `32/32` passed
- `mvn test` in `services/menu-service`
  - result: `44/44` passed
- `npm run lint -- --no-fix` in `services/frontend/quickbite-frontend`
  - result: passed
- `npm run build` in `services/frontend/quickbite-frontend`
  - result: passed, hash `b56fb68e13e1cf00`

### Local stack and smoke validation

Executed successfully:

- `docker ps`
  - result: all 6 Sierra-Lima containers healthy
- `bash services/local-dev/smoke.sh`
  - result: passed
- `bash services/local-dev/smoke-cross-service.sh`
  - result: passed, Sierra-Lima failures `0`, teammate failures `0`
- `npx newman run services/local-dev/postman/QuickBite.postman_collection.json -e services/local-dev/postman/QuickBite.postman_environment.json`
  - result: `39` requests, `66` assertions, `0` failures

### Direct targeted HTTP probes

The session reran the prior known regression probes:

- duplicate same-owner restaurant rename:
  - `PUT /restaurants/d0000002-...` -> `409 Conflict`
- unsupported method on Restaurant Service:
  - `DELETE /restaurants/d0000001-...` -> `405 Method Not Allowed`
- unsupported method on Menu Service:
  - `PATCH /menu-items/e0000011-...` -> `405 Method Not Allowed`
- invalid operating hours:
  - `POST /restaurants` with `24:00-24:00` -> `400 Bad Request`
- mixed currency validate:
  - EUR + USD `POST /menu-items/validate` -> `400 Bad Request`
- paged restaurant browse:
  - `GET /restaurants?page=0&size=1` -> paged JSON with `content`

All of those checks confirmed that the earlier audit-chain defects
patched before or in `60fa710` still hold in the live stack.

## Remaining Runtime Defect Found

The session found one remaining defect not addressed by `60fa710`.

### Defect

`Admin` can still create a menu item under a nonexistent restaurant id.

### Root cause

In `MenuService.requireOwnerOrAdmin(...)`, `Admin` returns early before
`restaurantOwnership.findOwnerId(...)` is called. That means
`MenuService.create(...)` can persist a `restaurantId` that does not map
to a real Restaurant aggregate when the caller is `Admin`.

Relevant files:

- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java`
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/service/MenuServiceTest.java`

### Live reproduction

Executed successfully and cleaned up immediately:

1. owner token:
   - `POST /restaurants/ffffffff-ffff-ffff-ffff-ffffffffffff/menu-items`
   - result: `404 Not Found`
2. admin token:
   - same request
   - result: `201 Created`
3. validate orphan item:
   - `POST /menu-items/validate` with the orphan `menuItemId`
   - result: `200 OK`, `allValid=true`, `exists=true`,
     `isAvailable=true`
4. cleanup:
   - `DELETE /menu-items/{id}` on the temporary orphan row
   - result: `204 No Content`

### Why it mattered to the audit verdict

This was not just an admin-only CRUD nicety:

- it permits an orphan `MenuItem`
- that orphan item is then accepted by Sierra-Lima's own W1 validation
  endpoint

So the defect leaks into the contract surface used during order
placement.

## Audit Artifact Produced

The session authored:

- `dev-docs/audits/audit-60fa710_Golf-Papa-Tango_team-lead-integration-readiness.md`

Final verdict in that report:

- `NOT READY TO HAND OVER AS FINAL`

Reason:

- `60fa710` does fix the earlier duplicate-name and `405` findings
- the standard validation matrix is otherwise green
- but the remaining Menu orphan-item defect still exists and reaches the
  W1-facing validation path

## Files Created During This Session

Created by the agent:

- `dev-docs/audits/audit-60fa710_Golf-Papa-Tango_team-lead-integration-readiness.md`
- `dev-docs/agent-context/2026-04-19_chat-archive_Golf-Papa-Tango_60fa710.md`

No existing tracked source files were modified in this session.

## Runtime Cleanup Performed

The session created and then removed only temporary runtime data:

- deleted the temporary orphan menu-item row created during the admin
  missing-parent probe
- deleted the temporary USD menu-item row created during the mixed-currency
  regression probe
- removed the extra evidence logs generated by this session's
  `smoke-cross-service.sh` run:
  - `services/local-dev/evidence/cross-service-smoke_20260419T173238Z.log`
  - `services/local-dev/evidence/menu-events_20260419T173238Z.log`

## Workspace State At Archive Time

The worktree contained only the new Markdown artefacts created in this
session:

- `dev-docs/audits/audit-60fa710_Golf-Papa-Tango_team-lead-integration-readiness.md`
- `dev-docs/agent-context/2026-04-19_chat-archive_Golf-Papa-Tango_60fa710.md`

No source-code edits were left behind by this session.

## Recommended Next Steps For A Future Agent

1. Fix `MenuService.create(...)` so parent restaurant existence is always
   verified, even for `Admin`.
2. Add regression coverage for:
   - `Admin + missing restaurant -> 404`
   - no orphan `MenuItem` accepted by `POST /menu-items/validate`
3. Re-run the same validation matrix:
   - Restaurant `mvn test`
   - Menu `mvn test`
   - frontend lint/build
   - `smoke.sh`
   - `smoke-cross-service.sh`
   - Newman
4. If the user asks for a follow-up handover verdict, anchor the next
   audit on the post-fix commit rather than reusing `60fa710`.
