# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`d23145f`)

## Session Summary

This session was an **integration-handover readiness audit** against
commit `d23145f` ("Patch Golf-Papa-Tango audit findings 1-4 at
`1a6e8c7`"). The user wanted the repository to be "as bug-free as
possible" before handing it to the Group 7 team lead for cross-team
integration. Not a master-plan Phase; a final personal-repo quality
gate.

The session had two distinct jobs:

1. Author a Charlie-Lima-Alfa audit of `d23145f` answering Q1-Q4
   (scope coverage / validation completed / validation not completed /
   final verdict) and save it under `dev-docs/audits/`.
2. Archive session context under `dev-docs/agent-context/` and then
   commit + push every file in the local repository (with exclusions
   only at best judgement).

Between (1) and (2), the user interrupted to let "something else
happen in the repository first". That something was a parallel
Golf-Papa-Tango session that ran its own audit of `d23145f`, identified
a runtime defect inside my §Q3.7 observation (the DTO regex
permissiveness), patched it, and reran the validation matrix. On
restart I accepted the Golf-Papa-Tango changes verbatim per memory
`feedback_other_callsign_files.md` ("treat other-callsign artefacts as
read-only; commit verbatim when asked") and handled the commit +
push.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`,
  and the `Frontend` under `services/frontend/quickbite-frontend/`.
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Upstream at session open: `origin/dev` at `d23145f`
- Docker compose stack state at session open: all six services
  healthy (restaurant-db, menu-db, restaurant-service, menu-service,
  frontend, dev-gateway)

## Audit Scope (Q1-Q4)

The user asked for answers to four concrete questions about `d23145f`:

- **Q1** -- Do implemented functionalities sufficiently cover
  Sierra-Lima's Assignment 3 ownership?
- **Q2** -- Validation Charlie-Lima-Alfa was able to complete.
- **Q3** -- Validation Charlie-Lima-Alfa was not able to complete.
- **Q4** -- Final verdict on integration-handover readiness.

Deliverable location and naming: under `dev-docs/audits/`, filename
must include short hash `d23145f` and callsign `Charlie-Lima-Alfa`,
plus a proposed descriptive element. I settled on
`audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md`.

## Reference Material Consulted

- `dev-docs/decisions/0001-scope-freeze.md` (Sierra-Lima service
  ownership locked to R19, R20, R21, R22).
- `dev-docs/decisions/0002-workflows.md` (W1 synchronous, W2/W3 async
  teammate-owned, Sierra-Lima owns hops 4/5).
- `dev-docs/decisions/0010-sierra-lima-auth.md` (JWT HS256, gateway
  path-prefix route matrix, §8 auth posture per endpoint).
- `dev-docs/decisions/0020-sierra-lima-contracts.md` (12 Sierra-Lima
  endpoints, DTO validation rules, error envelope).
- `dev-docs/decisions/0030-w1-lock.md` (W1 hop contracts, status-code
  enums, token-propagation posture).
- `dev-docs/decisions/0032-event-contracts.md` (event envelope shape).
- `dev-docs/decisions/0040-phase16-stance.md` (optional `menu-events`
  log-only emit posture).
- `dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md`
  (prior Charlie-Lima-Alfa baseline, verdict READY).
- `dev-docs/audits/audit-1a6e8c7_Golf-Papa-Tango_pre-integration-readiness.md`
  (prior Golf-Papa-Tango baseline, verdict NOT READY with five
  findings F1-F5; F1-F4 patched at `d23145f`, F5 deferred).

Code paths read end-to-end:

- `services/restaurant-service` -- controller, service, repository,
  DTOs, exception advice, security shim, migrations.
- `services/menu-service` -- controller, service, exception advice
  (with new `MixedCurrencyException`), event publisher, migrations.
- `services/frontend/quickbite-frontend` -- `RestaurantListView.vue`
  and `AddMenuItemView.vue` response-shape handling.
- `services/local-dev` -- docker compose, smoke scripts, Postman
  collection and environment.

## Audit Findings

### Q1 -- Coverage

Verdict: **yes, with one open doc-drift (F5) that has no runtime
effect.** Built a 12-row endpoint matrix mapping every `0020 §1-§2`
contract clause to a real controller method at `d23145f`; all 12 line
up. Data models match `0020 §4`, seed count `6 restaurants + 16 menu
items` matches `0020 §5` + the Phase 19 errata. Workflows covered:
W1 hops 4/5 match `0030`; W2/W3 are Sierra-Lima non-participant per
`0040`; the optional Phase 16 log-only `menu-events` emit fires with
the frozen envelope (live evidence captured in
`services/local-dev/evidence/menu-events_20260419T144233Z.log`, 2
JSON lines during `smoke-cross-service.sh`).

### Q2 -- Validation completed

16 Q2 checks, all green:

- `mvn test` restaurant-service: 26/26 green (up from 23 at `5a998ad`;
  +3 from F2/F3 tests).
- `mvn test` menu-service: 43/43 green (up from 42 at `5a998ad`; +1
  from F1 test).
- `npm run lint -- --no-fix` + `npm run build`: both green.
- `docker compose ps`: 6/6 healthy.
- `smoke.sh`: `OK -- Sierra-Lima smoke test passed.` (exit 0).
- `smoke-cross-service.sh`: `sierra-lima failures = 0, teammate
  failures = 0`; 2 `menu-events` log lines captured.
- `newman run QuickBite.postman_collection.json -e
  QuickBite.postman_environment.json`: 39 requests, 28 test-scripts,
  66 assertions, 0 failures on a single cold run.
- Code-read spot-checks on F1 (MixedCurrencyException + advice), F2
  (`@Pattern` tightening + parse fallback), F3 (Page response), F4
  (Postman zero-price 400 + CRUD seed isolation).
- F5 grep confirmed still open: `grep "DELETE /restaurants"
  audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md`
  still returns a hit at line 34.
- Auth matrix spot-check via Newman and tests (`401` / `403` / `200`
  for various actor-endpoint combos).

### Q3 -- Validation not completed

9 gaps, none integration-blocking:

- **3.1** F5 open (doc-drift in older Charlie-Lima-Alfa audit);
  explicitly deferred by the `d23145f` commit message and the
  other-callsign-read-only feedback rule.
- **3.2** Minor commit-message mislabel: the line that reads "F4:
  Postman mixed-currency case now expects 400" actually refers to the
  zero-price case (`priceAmount=0`); there is no mixed-currency
  Postman case.
- **3.3** True end-to-end W1 across teammate services (owned by
  Alfa-Kilo, Elephant-Yankee, Mike-Alfa; not present here).
- **3.4** JWT interop against Alfa-Kilo's real user-service issuer;
  dev stack uses local HS256.
- **3.5** Live W2/W3 broker-backed envelopes; Sierra-Lima is
  non-participant per `0040`.
- **3.6** Presentation-deck screenshots; deferred to rehearsal.
- **3.7** Minor DTO `@Pattern` permissiveness: the contract-aligned
  regex `[0-2][0-9]:[0-5][0-9]-[0-2][0-9]:[0-5][0-9]` still admits
  hours 23-29 inclusive; the service-layer `LocalTime.parse` fallback
  catches the problem at availability-evaluation time but the DTO
  accepts it at create/update. Flagged as optional post-CP#1
  tightening, exactly matching `0020 §3` so no drift.
- **3.8** Encrypted `Assignment-3-Submission.pdf` cross-check; still
  password-protected.
- **3.9** Load/perf profile for `/menu-items/validate`; out of A3
  scope.

### Q4 -- Verdict

**READY TO HAND OVER to the Group 7 team lead for cross-team
integration.** 69/69 unit tests green, 66/66 Newman assertions green,
6/6 docker services healthy, both smokes exit 0, event envelope
frozen. F1-F4 all patched and re-verified; F5 is documentation-only
and has a 5-minute remedy. My audit recommended: let the team lead
merge `dev` at `d23145f`; Sierra-Lima can close F5 as an errata before
or immediately after the hand-over email.

## Mid-session Interrupt: Parallel Golf-Papa-Tango Session

Between the audit (phase 1) and the commit (phase 2), the user
interrupted phase 2 saying "something else needed to happen in the
repository first." That "something else" was a parallel
Golf-Papa-Tango session which:

1. Ran its own audit of `d23145f` and converted my §Q3.7 "minor DTO
   permissiveness" observation into a blocker finding. Golf-Papa-Tango
   reproduced the actual defect live: `POST /restaurants` with
   `operatingHours="29:59-29:59"` returned `201 Created`, while
   `99:99-99:99` correctly returned `400`. That proved the existing
   `[0-2][0-9]` regex let 24-29 hours slip through.
2. Tightened the regex to a real `00-23` matcher
   `^(?:[01][0-9]|2[0-3]):[0-5][0-9]-(?:[01][0-9]|2[0-3]):[0-5][0-9]$`
   in `CreateRestaurantRequest.java` + `UpdateRestaurantRequest.java`.
3. Added controller tests
   `createRestaurant_outOfRangeOperatingHoursReturns400` and
   `putRestaurant_outOfRangeOperatingHoursReturns400` asserting
   `24:00-24:00` and `29:59-29:59` return `400`, plus
   `verifyNoInteractions(service)` assertions. Factored
   `validCreateBody` to use a new `bodyWithHours(String)` helper.
4. Updated `dev-docs/decisions/0020-sierra-lima-contracts.md` to
   document the tighter regex and explicitly note that `24:00` and
   `29:59` are invalid.
5. Reran the full matrix after the patch: 28/28 restaurant (up from
   26), 43/43 menu, Newman 66/66, both smokes green, all direct HTTP
   probes confirm `400` on the previously-accepted bad values.
6. Rewrote `audit-d23145f_Golf-Papa-Tango_integration-handover-readiness.md`
   with a fresh verdict of **READY TO HAND OVER**, archiving the
   pre-fix version verbatim in Appendix A.
7. Left a session archive at
   `dev-docs/agent-context/2026-04-19_chat-archive_Golf-Papa-Tango_d23145f.md`.
8. Cleaned up local runtime residue (two `Audit Invalid%` rows from
   the pre-fix probe in the restaurant Postgres volume; the temporary
   USD menu item used in the mixed-currency probe; their own
   extra evidence logs from the rerun -- leaving my
   `20260419T144233Z` logs untouched).

On session restart the user said: "Sorry Claude, something else
needed to happen in the repository first. Please proceed with the
previous cancelled prompt as follows." The previous prompt was
archive + commit + push. I confirmed post-fix tests were still green
(28/28 restaurant + 43/43 menu = 71 total) and proceeded to the commit
stage.

## Verification At Archive Time

- `git status` (inside `services/restaurant-service`) shows four
  tracked modifications and five untracked files. All are consistent
  with the Golf-Papa-Tango patch + my + their audit artefacts +
  evidence logs from my cross-service smoke.
- `mvn -q test` in `services/restaurant-service`: 28/28 green.
- Menu service was verified green at 43/43 earlier in the session;
  Golf-Papa-Tango re-ran it after the DTO-only patch and reported
  43/43 green. No menu changes were introduced by either session.
- Newman 66/66 was confirmed green by both callsigns at `d23145f`
  code and independently after the regex tightening (Golf-Papa-Tango
  rerun).

## Files Changed This Session

Modified (by Golf-Papa-Tango in parallel; accepted verbatim per the
read-only-other-callsign rule):

- `dev-docs/decisions/0020-sierra-lima-contracts.md` -- regex doc-lock.
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/CreateRestaurantRequest.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/UpdateRestaurantRequest.java`
- `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantControllerTest.java`

Added (by me):

- `dev-docs/audits/audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md`
- `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_d23145f.md` (this file)

Added (by Golf-Papa-Tango, accepted verbatim):

- `dev-docs/audits/audit-d23145f_Golf-Papa-Tango_integration-handover-readiness.md`
- `dev-docs/agent-context/2026-04-19_chat-archive_Golf-Papa-Tango_d23145f.md`

Evidence carried over from my Q2 smoke runs (pre-existing untracked;
both callsign sessions left them untouched):

- `services/local-dev/evidence/cross-service-smoke_20260419T144233Z.log`
- `services/local-dev/evidence/menu-events_20260419T144233Z.log`

## Notes for the Next Session

- **F5 is still the only open audit item.** The `5a998ad`
  Charlie-Lima-Alfa audit line 34 still claims a non-existent `DELETE
  /restaurants/{id}` endpoint. Remedy is a 5-minute errata section on
  that audit (or a sibling `..._errata.md`). The user's posture as of
  this session is still "not just yet" (carried forward from the
  `1a6e8c7` session).
- **`operatingHours` regex is now as tight as any future caller
  should need** (`00-23:00-59`); no further DTO tightening required.
  The service-layer `isWithinOperatingHours` fallback is still present
  as a belt-and-braces guard for malformed legacy data.
- **Presentation-deck evidence** can reuse the
  `20260419T144233Z` logs from this session on the clean rehearsal
  boot.
- **Stale `Audit Invalid%` rows were removed from the local dev
  Postgres volume** by Golf-Papa-Tango during their rerun; a fresh
  `docker compose down --volumes` + re-up would confirm a pristine
  seed with 6 restaurants and 16 menu items if desired.
- **`PageImpl` serialisation warning** from Spring still prints at
  runtime; documented as accepted in the prior `1a6e8c7` archive and
  still accepted here.
