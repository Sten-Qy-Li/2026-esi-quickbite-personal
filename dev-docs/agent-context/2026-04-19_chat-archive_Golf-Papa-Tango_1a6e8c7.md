# Chat Archive - 2026-04-19 - Golf-Papa-Tango (`1a6e8c7`)

## Session Summary

This chat focused on a thorough pre-integration audit of commit `1a6e8c7` for the Sierra-Lima slice of the QuickBite personal repository.

The main outcomes were:

1. Audit commit `1a6e8c7` against Sierra-Lima's Assignment 3 ownership scope.
2. Cross-check the repository against the existing Charlie-Lima-Alfa pre-integration audit.
3. Re-run as much local validation as possible from the current machine instead of trusting prior documentation.
4. Identify any additional validation that Golf-Papa-Tango could complete beyond the Charlie-Lima-Alfa audit.
5. Produce a new audit report under `dev-docs/audits` with Golf-Papa-Tango's call-sign.
6. Conclude whether the repository is actually ready to hand over to the team lead for integration.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Agent call-sign for this session: `Golf-Papa-Tango`
- Requested commit under audit: `1a6e8c7`
- Current branch during the session: `dev`
- Current date in workspace context: `2026-04-19`

There was one important clarification early in the session:

- the user's earlier aborted prompt mentioned the wrong commit hash;
- the corrected audit target for this session was `1a6e8c7`.

Another important clarification discovered during the audit:

- commit `1a6e8c7` does **not** contain a file named `dev-docs/audits/audit-1a6e8c7_Charlie-Lima-Alfa_pre-integration-readiness.md`;
- it contains `dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md`, created by commit `1a6e8c7`.

That distinction mattered when answering the user's Q2.

## Audit Scope and Source Material Reviewed

The audit was anchored against both the codebase and the repo-local assignment/ADR material.

Documents and artifacts reviewed during this session included:

- `dev-docs/decisions/0001-scope-freeze.md`
- `dev-docs/decisions/0002-workflows.md`
- `dev-docs/decisions/0010-auth-contract.md`
- `dev-docs/decisions/0020-sierra-lima-contracts.md`
- `dev-docs/decisions/0030-w1-synchronous-contract-lock.md`
- `dev-docs/decisions/0040-phase-16-async-stance.md`
- `dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md`
- `dev-docs/course-materials/Assignment_3_2026.pdf` via text extraction tooling
- `dev-docs/prior-submissions/Assignment-3-Submission.docx`

The service and frontend code reviewed included:

- Restaurant controller, service, DTOs, repository, security config, tests
- Menu controller, service, DTOs, repository, ownership client, exception handling, tests
- Vue views for restaurant list/detail and menu list/detail
- local-dev scripts and Postman collection assets

## Validation Work Executed

The session did not stop at static review. Local validation was rerun from this environment.

### Automated test and build work

Executed successfully:

- `mvn test` in `services/restaurant-service`
  - result: `23/23` tests passed
- `mvn test` in `services/menu-service`
  - result: `42/42` tests passed
- `npm run build` in `services/frontend/quickbite-frontend`
  - result: production build succeeded
- `npm run lint -- --no-fix`
  - result: no lint errors

### Local stack and smoke validation

Executed successfully:

- `docker compose up -d` in `services/local-dev`
- `docker compose ps`
  - result: all 6 containers healthy
- `bash services/local-dev/smoke.sh`
  - result: passed
- `bash services/local-dev/smoke-cross-service.sh`
  - result: Sierra-Lima-owned steps passed; teammate probes skipped when unset
- health checks against:
  - `http://localhost:8081/actuator/health`
  - `http://localhost:8082/actuator/health`
  - `http://localhost:8090`

### Additional validation beyond the prior audit

This session also completed checks that the earlier Charlie-Lima-Alfa audit did not actually execute from this environment:

- `npx newman run services/local-dev/postman/QuickBite.postman_collection.json -e services/local-dev/postman/QuickBite.postman_environment.json`
- targeted live probe for mixed-currency `POST /menu-items/validate`
- targeted live probe for malformed `operatingHours`
- targeted live probe for `GET /restaurants?page=1&size=1`
- direct inspection of live `menu-events` log lines from the running menu container
- direct inspection of compose-time Flyway startup lines in container logs

## Main Findings Reached During The Session

The audit concluded that the repository is **not ready to hand over as-is**.

The key findings were:

### 1. Mixed-currency validation bug

`POST /menu-items/validate` currently accepts mixed-currency baskets and returns:

- `200 OK`
- `allValid: true`
- a summed `totalAmount`
- the first seen currency

This contradicts the frozen contract, which says mixed currencies must be rejected with `400`.

### 2. Operating-hours validation bug

Restaurant create/update accepts impossible values such as:

- `99:99-99:99`

Then, when the restaurant is open, `GET /restaurants/{id}/availability` can still return:

- `acceptsOrders: true`

because malformed or unparsable hours fall back to `true` in the service logic.

### 3. Restaurant list contract drift

`GET /restaurants` is documented in `0020` as a paginated response with:

- `page`
- `size`
- `sort`
- paging metadata

But the live controller returns a plain JSON array and only handles:

- `city`
- `isOpen`

The frontend is also coupled to that array shape.

### 4. Newman collection is runnable, but not green

Unlike the earlier Charlie-Lima-Alfa audit, this environment could run Newman.

The actual result was:

- `39` requests executed
- `63` assertions executed
- `5` assertion failures

The failures came from two causes:

1. stale expectation that zero-price menu-item creation returns `422` rather than the now-correct `400`
2. fixture reuse after the collection deletes a seed menu item and later tries to reuse it in negative-auth/admin-bypass requests

### 5. Prior audit accuracy issue

The Charlie-Lima-Alfa audit claims `DELETE /restaurants/{id}` exists, but the current contract and controller do not define that endpoint.

This mattered because the user asked what validation from that audit Golf-Papa-Tango could complete; the answer had to account for the fact that one part of the earlier audit was itself inaccurate.

## Final Audit Artifact Produced

The main deliverable created during this session was:

- `dev-docs/audits/audit-1a6e8c7_Golf-Papa-Tango_pre-integration-readiness.md`

That report answered the user's requested questions:

1. whether Sierra-Lima's responsibilities are sufficiently covered
2. what parts of the Charlie-Lima-Alfa audit Golf-Papa-Tango could complete
3. what extra validation Golf-Papa-Tango could complete
4. what validation Golf-Papa-Tango could not complete
5. the final readiness verdict

The verdict recorded in that report was:

- `NOT READY TO HAND OVER AS-IS`

## Files Created or Updated During This Session

### Created by the agent

- `dev-docs/audits/audit-1a6e8c7_Golf-Papa-Tango_pre-integration-readiness.md`
- `dev-docs/agent-context/2026-04-19_chat-archive_Golf-Papa-Tango_1a6e8c7.md`

### Temporary evidence generated during the session

The cross-service smoke script generated fresh evidence files during execution, but those fresh untracked logs were removed before close-out so the worktree stayed clean except for the requested documentation artifact.

Previously committed evidence under `services/local-dev/evidence/` was left untouched.

## What Could Not Be Completed From This Repository Alone

The session established that the following still require follow-up outside the Sierra-Lima personal repo:

- full W1 end-to-end against teammate-owned services
- real W2 / W3 async validation with teammate producers and consumers
- JWT interoperability against Alfa-Kilo's real issuer
- merged-branch integration validation on the team lead's environment

These limitations were documented explicitly in the final audit report.

## Workspace Notes At Archive Time

At the time this archive was written:

- the only worktree addition from this session was:
  - `dev-docs/audits/audit-1a6e8c7_Golf-Papa-Tango_pre-integration-readiness.md`
- no source-code files were modified
- no destructive git operations were used

## Recommended Next Steps For A Future Agent

1. If the user wants the repository upgraded to integration-ready, fix the mixed-currency validation bug first because it directly affects W1 correctness.
2. Tighten `operatingHours` validation to reject impossible times and remove the permissive `acceptsOrders=true` fallback on parse failure.
3. Resolve the `GET /restaurants` contract drift either by implementing pagination or by updating the contract and any dependent validation assets.
4. Repair the Postman/Newman collection so it runs green again on a clean seeded stack.
5. If requested, follow this archive and the new audit report to produce code fixes plus a rerun of the same validation set.
