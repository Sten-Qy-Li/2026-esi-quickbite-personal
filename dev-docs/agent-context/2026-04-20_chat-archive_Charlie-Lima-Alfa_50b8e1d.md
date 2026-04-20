# Chat Archive - 2026-04-20 - Charlie-Lima-Alfa (`50b8e1d`)

## Session Summary

Single-purpose audit session. The user asked for a
pre-team-integration audit (test / verify / validate) of the current
`dev` tip at `50b8e1d`, a written audit report under
`dev-docs/audits/`, resolution of any bugs found, and a final commit
+ push. The session found **one Medium-severity issue** that had been
flagged but not fixed at the `7b2fa61` Golf-Papa-Tango audit (the
Postman `PUT /restaurants/{id}` request silently returned `409` on
non-pristine volumes because the hard-coded `"Pizza Antonio
(updated)"` name collided with prior-run rows and the request had no
assertion). The audit report was written first; the Postman
collection was then patched; three confirmatory Newman runs were
executed to prove the fix is reliable across repeated runs on the
non-pristine volume; the audit report was appended with a Resolution
section.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Today: 2026-04-20 (Monday)
- Active branch: `dev`
- Upstream at session open: `origin/dev` at `50b8e1d` ("Close
  Charlie-Lima-Alfa 7b2fa61 gap-analysis F2 (DELETE menu-item UI)")
- Working tree at session open: clean (no modifications, no untracked
  files apart from `services/local-dev/evidence/*` prior-run logs
  listed as untracked by `git status`).
- Session model: Opus 4.7, max effort

## Requests In This Session

1. Audit (test/verify/validate) the current state of the local
   repository for any pre-team-integration bugs.
2. Write an audit report in `dev-docs/audits/`. Filename should
   include the callsign `Charlie-Lima-Alfa`.
3. If any bugs are found, after writing the audit report, resolve
   them. Then append to the audit report that they are resolved.
4. Archive the session context in `dev-docs/agent-context/`.
5. Commit the local repository and push to remote. Include every
   file in the local repository; exclude at best judgement only.

## Validations Performed

Local host on Windows 11 (Git Bash), Docker 29.4.0 / Compose 5.1.1.

1. `mvn test` in `services/restaurant-service`: **33/33 green**, BUILD SUCCESS.
2. `mvn test` in `services/menu-service`: **47/47 green**, BUILD SUCCESS.
3. `npm run lint -- --no-fix` in `services/frontend/quickbite-frontend`: `DONE  No lint errors found!`.
4. `npm run build` in the same dir: `DONE  Compiled successfully in 3338ms`; app 47.98 KiB; vendor 130.84 KiB; hash `e665785ceb70f60d`.
5. `docker compose --profile dev-gateway up -d --build` in `services/local-dev`: all six containers reached `healthy`.
6. `curl -sf http://localhost:8081/actuator/health` and `:8082/actuator/health`: both `UP` with DB `UP`. `curl -sf http://localhost:8080/healthz`: `ok`.
7. `bash services/local-dev/smoke.sh`: `OK -- Sierra-Lima smoke test passed.`
8. `bash services/local-dev/smoke-cross-service.sh`: `sierra-lima failures = 0, teammate failures = 0`; 2 `menu-events` log lines captured to `services/local-dev/evidence/menu-events_20260420T115714Z.log`.
9. `npx newman run services/local-dev/postman/QuickBite.postman_collection.json -e services/local-dev/postman/QuickBite.postman_environment.json` (pre-fix): 39 requests / 30 test-scripts / **68 assertions / 0 failures** -- BUT one request (`PUT /restaurants/{id}`) returned `409 Conflict` silently (no assertion to catch it).
10. Targeted live curl probes with freshly-minted HS256 tokens:
    - DELETE `/menu-items/{id}` (customer token) → **403**
    - DELETE `/menu-items/{id}` (owner token, happy path) → **204**
    - GET `/menu-items/{id}` after delete → **404**
    - DELETE `/menu-items/{id}` (non-existent uuid, owner token) → **404**
    - DELETE `/restaurants/{id}` (owner or admin token) → **405** (method-not-allowed)
    - PATCH `/menu-items/{id}` (owner or admin token) → **405**
    - POST `/restaurants` with `operatingHours="29:59-29:59"` → **400**
    - POST `/restaurants` with `operatingHours=""` → **400**
    - POST `/menu-items/validate` with mixed EUR+USD items → **400**
    - GET `/restaurants?page=0&size=1` → **200** (paged `Page<RestaurantResponse>`)

## Findings

### Finding 1 -- Medium, fixed this session.

- `services/local-dev/postman/QuickBite.postman_collection.json:226-260`: `PUT /restaurants/{id}` request was hard-coded to rename to `"Pizza Antonio (updated)"` and had no assertion block. Duplicate-name protection (landed at `1a6e8c7`) therefore returns `409 Conflict` on every non-pristine re-run, but Newman cannot see this because there is no test-script. Identical in substance to the `7b2fa61` Golf-Papa-Tango Finding 1; never patched until now.

### No other code-level findings.

- 80/80 backend tests remain green.
- Lint and build remain green.
- All 6 Docker containers healthy; both smoke scripts pass.
- New DELETE UI button in `MenuItemDetailView.vue` is correct: `canManage` gates on role, `window.confirm` prompt, `api.delete('/api/menu-items/{id}')`, navigation to `{ name: 'restaurant-menu', params: { id: restaurantId } }` on success, error banner on failure. Backed by existing tests (`MenuControllerTest.deleteMenuItem_*` five tests) + live curl confirmation.
- All patches from prior audits (`15f5ab7` `operatingHours` regex tightening; `1a6e8c7` rename-collision 409; `7b2fa61` 405 method-not-allowed via `HttpRequestMethodNotSupportedException` handler in both services; menu-create ownership hardening) still live on the wire.

## Files Changed This Session

### Modified

- `services/local-dev/postman/QuickBite.postman_collection.json`
  -- patched `PUT /restaurants/{id}` in the `Restaurant CRUD` folder:
  1. Added `{{$timestamp}}` suffix to the request-body `name` field so the rename target is unique per run.
  2. Added a collection-level `test` event asserting `status is 200` and `body.name` matches `/^Pizza Antonio \(updated\) \d+$/`. The Newman suite now fails loudly if the endpoint ever returns `409` again.

### Added

- `dev-docs/audits/audit-50b8e1d_Charlie-Lima-Alfa_pre-team-integration-readiness.md`
  -- this session's audit report. Scope, evidence, 17 validation
  checks, the one Medium finding, post-audit Resolution section
  (Finding 1 fixed + three Newman re-runs confirming 70/70 green and
  `200 OK` on repeat).
- `dev-docs/agent-context/2026-04-20_chat-archive_Charlie-Lima-Alfa_50b8e1d.md`
  -- this file.
- `services/local-dev/evidence/cross-service-smoke_20260420T115714Z.log`
  -- cross-service smoke trace captured during §2.7.
- `services/local-dev/evidence/menu-events_20260420T115714Z.log`
  -- paired `menu-events` evidence for the availability transitions
  the cross-service smoke exercised.

### Not committed

- `tmp_probe.sh` -- throwaway helper used to mint three HS256 tokens
  (owner / customer / admin) and run one-off live curl probes for
  DELETE, 405, regex, mixed-currency, and paged list. Deleted before
  commit.

## Post-Fix Verification

- Newman (run 1): `39 requests / 31 test-scripts / 70 assertions / 0 failures`. PUT restaurant line: `[200 OK, 776B, 15ms]`.
- Newman (run 2, same volume): `[200 OK, 776B, 19ms]` on the PUT, still `70/70`.
- Newman (run 3, same volume): `[200 OK, 776B, 13ms]` on the PUT, still `70/70`.
- `mvn test` on both services: re-confirmed green (no backend code change was involved, but re-running is cheap).
- `smoke.sh` + `smoke-cross-service.sh`: re-run not required for this fix, but prior-step traces for this session remain on disk and were committed as evidence.

## Memory / Context Notes For Next Session

- The final open item from the `7b2fa61` Golf-Papa-Tango audit
  (`Finding 1` Medium, Postman reliability) is now closed here at
  `50b8e1d`. The only remaining items tagged for Sierra-Lima across
  the full audit chain are the gap-analysis F1 (Kafka transport,
  conditional on the 2026-04-21 teammate sync) and F3 (teammate
  compose assembly), both cross-team and not Sierra-Lima-actionable
  unilaterally.
- The DELETE menu-item UI button added by commit `50b8e1d` was
  verified end-to-end live this session; no follow-up action
  required.
- Postman `PUT /restaurants/{id}` reliability regression test is now
  enforced by two Newman asserts; future edits must preserve the
  `{{$timestamp}}` suffix.

## Commit Plan

One commit, authored by Sierra-Lima and co-authored by Claude Opus
4.7, carrying:

- the audit report,
- the Postman patch resolving the audit's Finding 1,
- this chat archive,
- the new evidence logs.

Commit message will reference `audit-50b8e1d` and the `7b2fa61`
Golf-Papa-Tango F1 recurrence that is now closed here.
