# Chat Archive - 2026-04-19 - Golf-Papa-Tango (`d23145f`)

## Session Summary

This session covered the full audit and remediation cycle for commit `d23145f` in the Sierra-Lima QuickBite personal repository.

The work happened in three phases:

1. audit commit `d23145f` for integration handover readiness
2. patch the remaining runtime defect found in that audit
3. rerun the full integration matrix and refresh the Golf-Papa-Tango audit with a post-fix verdict

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Agent call-sign for this session: `Golf-Papa-Tango`
- Commit under audit: `d23145f`
- Branch during the session: `dev`
- Workspace date: `2026-04-19`

The user asked for:

1. a thorough audit of `d23145f`
2. an audit report under `dev-docs/audits`
3. a patch for any problems found in the audit findings
4. a full post-fix rerun of the integration matrix
5. an updated Golf-Papa-Tango audit with the previous version archived as an appendix
6. a session archive under `dev-docs/agent-context`

## Repo and Document Context Reviewed

The session used both code inspection and repo-local planning/contract documents.

Key documents reviewed:

- `dev-docs/decisions/0001-scope-freeze.md`
- `dev-docs/decisions/0002-workflows.md`
- `dev-docs/decisions/0020-sierra-lima-contracts.md`
- `dev-docs/audits/audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md`

Key code paths reviewed:

- Restaurant DTO validation for `operatingHours`
- Restaurant controller and service behaviour
- Restaurant controller tests
- Menu mixed-currency validation path
- frontend list handling for paginated `GET /restaurants`
- local-dev Docker, smoke, and Newman assets

## Initial Audit Outcome

The first Golf-Papa-Tango audit for `d23145f` concluded:

- scope coverage for Sierra-Lima was present
- most earlier blockers from `1a6e8c7` were fixed
- one integration-blocking defect still remained

That remaining defect was:

- `POST /restaurants` still accepted impossible `operatingHours` values in the `24-29` hour range, such as `29:59-29:59`

This was reproduced live:

- `99:99-99:99` returned `400`
- `29:59-29:59` returned `201`

That meant the DTO regex had been tightened only partially. The availability fallback was safer than before, but invalid restaurant data could still enter the system.

The initial Golf audit artifact produced at that point was:

- `dev-docs/audits/audit-d23145f_Golf-Papa-Tango_integration-handover-readiness.md`

with verdict:

- `NOT READY TO HAND OVER AS-IS`

## Patch Work Completed

After the user asked to patch the findings, the session fixed the runtime defect directly in the restaurant-service boundary validation.

### Code changes made

- tightened `operatingHours` validation in:
  - `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/CreateRestaurantRequest.java`
  - `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/UpdateRestaurantRequest.java`
- updated controller test coverage in:
  - `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantControllerTest.java`
- updated the canonical contract note in:
  - `dev-docs/decisions/0020-sierra-lima-contracts.md`

### Validation pattern change

The old pattern allowed any hour in `[00-29]`:

- `^[0-2][0-9]:[0-5][0-9]-[0-2][0-9]:[0-5][0-9]$`

The patch changed it to a real `00-23` matcher:

- `^(?:[01][0-9]|2[0-3]):[0-5][0-9]-(?:[01][0-9]|2[0-3]):[0-5][0-9]$`

### New test coverage added

The controller test suite now explicitly rejects:

- `24:00-24:00`
- `29:59-29:59`

on restaurant create/update requests.

## Validation Work Executed After The Patch

The session reran the validation matrix rather than assuming the patch was correct.

### Automated test and build work

Executed successfully:

- `mvn test` in `services/restaurant-service`
  - result: `28/28` tests passed
- `mvn test` in `services/menu-service`
  - result: `43/43` tests passed
- `npm run lint -- --no-fix` in `services/frontend/quickbite-frontend`
  - result: passed
- `npm run build` in `services/frontend/quickbite-frontend`
  - result: passed

### Local stack and smoke validation

Executed successfully:

- rebuilt the running `restaurant-service` container
- `docker compose --profile dev-gateway ps`
  - result: all 6 services healthy
- `curl http://localhost:8081/actuator/health`
  - result: `UP`
- `curl http://localhost:8082/actuator/health`
  - result: `UP`
- `curl http://localhost:8080/healthz`
  - result: `ok`
- `curl http://localhost:8090`
  - result: frontend served successfully
- `bash services/local-dev/smoke.sh`
  - result: passed
- `bash services/local-dev/smoke-cross-service.sh`
  - result: passed, Sierra-Lima failures `0`, teammate failures `0`

### Newman validation

Executed successfully:

- `npx newman run services/local-dev/postman/QuickBite.postman_collection.json -e services/local-dev/postman/QuickBite.postman_environment.json`

Result:

- `39` requests
- `66` assertions
- `0` failures

### Direct targeted HTTP probes

Executed successfully:

- `POST /restaurants` with `operatingHours="24:00-24:00"` -> `400`
- `POST /restaurants` with `operatingHours="29:59-29:59"` -> `400`
- `GET /restaurants?page=0&size=2&city=Tartu&isOpen=true` -> paginated JSON object
- mixed EUR/USD `POST /menu-items/validate` -> `400`

These direct probes were used to prove that the exact former blocker was gone and that the other previously fixed paths stayed correct.

## Runtime Cleanup Performed

The session also cleaned up runtime-side residue introduced by earlier audit probes.

### Database cleanup

Removed the earlier invalid `Audit Invalid%` restaurant rows from the local dev Postgres volume:

- result: `DELETE 2`

### Probe cleanup

- temporary USD menu item created for the mixed-currency probe was deleted successfully
- extra rerun evidence logs created by the post-fix `smoke-cross-service.sh` invocation were removed

Pre-existing untracked files were left untouched:

- `dev-docs/audits/audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md`
- `services/local-dev/evidence/cross-service-smoke_20260419T144233Z.log`
- `services/local-dev/evidence/menu-events_20260419T144233Z.log`

## Final Audit Outcome

After the patch and full rerun, the Golf-Papa-Tango audit was rewritten with a fresh verdict:

- `READY TO HAND OVER`

The refreshed audit artifact is:

- `dev-docs/audits/audit-d23145f_Golf-Papa-Tango_integration-handover-readiness.md`

That file now contains:

- the fresh post-fix verdict and evidence
- the archived pre-fix Golf-Papa-Tango audit preserved verbatim in Appendix A

## Files Created or Updated During This Session

### Created by the agent

- `dev-docs/audits/audit-d23145f_Golf-Papa-Tango_integration-handover-readiness.md`
- `dev-docs/agent-context/2026-04-19_chat-archive_Golf-Papa-Tango_d23145f.md`

### Updated by the agent

- `dev-docs/decisions/0020-sierra-lima-contracts.md`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/CreateRestaurantRequest.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/UpdateRestaurantRequest.java`
- `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantControllerTest.java`

## Workspace State At Archive Time

Tracked modifications present:

- `dev-docs/decisions/0020-sierra-lima-contracts.md`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/CreateRestaurantRequest.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/UpdateRestaurantRequest.java`
- `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantControllerTest.java`

Untracked files present:

- `dev-docs/audits/audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md`
- `dev-docs/audits/audit-d23145f_Golf-Papa-Tango_integration-handover-readiness.md`
- `services/local-dev/evidence/cross-service-smoke_20260419T144233Z.log`
- `services/local-dev/evidence/menu-events_20260419T144233Z.log`

## Recommended Next Steps For A Future Agent

1. If the user wants a clean handoff commit, stage the restaurant-service validation fix, the controller test additions, the contract-doc update, and the refreshed Golf audit together.
2. Do not touch the Charlie-Lima-Alfa untracked audit unless the user explicitly asks for it, because it was treated as read-only reference material in this session.
3. If requested, prepare a commit message that explains both the runtime validation fix and the post-fix audit rerun.
4. If the user wants the workspace fully documented, archive this updated session alongside any later commit or handover message.
