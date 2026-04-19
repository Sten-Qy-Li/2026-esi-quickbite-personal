# Integration-Handover Readiness Audit -- commit `d23145f` (post-fix rerun)

| Field | Value |
| --- | --- |
| Commit under audit | `d23145fae2d702867b6255ebc17b6c75e581e3f6` (short `d23145f`) |
| Branch observed | `dev` |
| Auditor | Golf-Papa-Tango |
| Audit date | 2026-04-19 |
| Supersedes | Earlier Golf-Papa-Tango audit in this same file, now archived in Appendix A |
| Verdict | `READY TO HAND OVER` |

## Q1 -- Do the implemented functionalities sufficiently cover Sierra-Lima's Assignment 3 ownership?

**Answer: yes.**

The repository still sufficiently covers Sierra-Lima's owned subset:

- `0001-scope-freeze.md` assigns Sierra-Lima to Restaurant Service (`R19`, `R20`) and Menu Service (`R21`, `R22`).
- `0002-workflows.md` assigns Sierra-Lima to W1 hop 4 (`GET /restaurants/{id}/availability`) and hop 5 (`POST /menu-items/validate`), with W2/W3 teammate-owned.
- The codebase implements the expected Restaurant/Menu endpoint surface, the Sierra-Lima frontend slice, the local Docker/dev-gateway stack, the Postman/Newman suite, and the optional `menu.item-availability-changed` producer.

The previously blocking quality issue is now fixed: invalid `operatingHours` values in the `24-29` hour range are rejected at the DTO boundary instead of being persisted.

## Q2 -- What validation within `audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md` was Golf-Papa-Tango able to complete?

**Answer: all locally reproducible validation listed there was completed.**

| Charlie-Lima-Alfa validation area | Golf-Papa-Tango result after the fix |
| --- | --- |
| Restaurant backend tests | Passed: `28/28` |
| Menu backend tests | Passed: `43/43` |
| Frontend lint | Passed |
| Frontend build | Passed |
| Docker/dev-gateway stack health | Passed; all six services healthy |
| Restaurant/Menu actuator health | Passed |
| `smoke.sh` | Passed |
| `smoke-cross-service.sh` | Passed; Sierra-Lima failures `0`, teammate failures `0`, optional menu-events exercise succeeded |
| Newman / Postman collection | Passed: `39` requests, `66` assertions, `0` failures |
| F1 mixed-currency validate patch | Confirmed fixed live: mixed EUR/USD request now returns `400` |
| F2 operating-hours patch | Confirmed fixed live: `99:99-99:99`, `24:00-24:00`, and `29:59-29:59` all return `400`; malformed values no longer leak through to persisted data |
| F3 paginated `GET /restaurants` patch | Confirmed fixed live: response shape contains `content`, `pageable`, `totalElements`, `size`, `number`, and related paging metadata |
| F4 Postman patch | Confirmed fixed live: suite runs clean and CRUD delete no longer destroys the seeded fixture |
| F5 doc-drift check | Still open, but still doc-only and non-blocking |

## Q3 -- What additional validation was Golf-Papa-Tango able to complete beyond that Charlie-Lima-Alfa audit?

The following additional validation was completed during this post-fix rerun:

- Re-verified the exact defect path with live HTTP probes:
  - `POST /restaurants` with `operatingHours="24:00-24:00"` -> `400 Bad Request`
  - `POST /restaurants` with `operatingHours="29:59-29:59"` -> `400 Bad Request`
- Re-checked the paginated restaurant list directly over HTTP:
  - `GET /restaurants?page=0&size=2&city=Tartu&isOpen=true` returned a paged object, not a bare array.
- Re-checked mixed-currency validation directly over HTTP:
  - created a temporary USD menu item,
  - called `POST /menu-items/validate` with one EUR seed item plus that USD item,
  - confirmed `400 Bad Request`,
  - deleted the temporary USD item successfully.
- Rebuilt the running `restaurant-service` container so the live local stack matches the patched source.
- Removed the two invalid `Audit Invalid%` restaurant rows that had been inserted into the local dev Postgres volume during the earlier pre-fix audit probe.

## Q4 -- What validation was Golf-Papa-Tango not able to complete?

The following follow-up items still require a broader integration environment or user follow-up:

- Full end-to-end W1 through teammate-owned services (User, Order, Payment, Delivery, Notification).
- JWT interoperability against Alfa-Kilo's real token issuer rather than the local dev HS256 minting path.
- True W2/W3 broker-backed integration with teammate producers/consumers.
- Cross-checking the password-protected prior submission PDF for any obligations not already reflected in the decision records.
- A destructive pristine-volume replay from a fully reset Docker data state. I intentionally did not wipe the user's Docker volumes.

## Q5 -- Final verdict on readiness to send the repository to the team lead for integration

**Final verdict: `READY TO HAND OVER`.**

Why:

- Sierra-Lima scope coverage is present and still aligned with Assignment 3 ownership.
- The full integration matrix rerun is green:
  - restaurant tests `28/28`
  - menu tests `43/43`
  - frontend lint/build passed
  - Docker/dev-gateway stack healthy
  - `smoke.sh` passed
  - `smoke-cross-service.sh` passed
  - Newman passed with `39` requests and `66` assertions, `0` failures
- The previously blocking runtime defect is closed:
  - impossible `operatingHours` values like `24:00-24:00` and `29:59-29:59` are now rejected with `400` instead of being accepted and persisted.
- The remaining open item from Charlie-Lima-Alfa's handover note is still the doc-only F5 drift about a non-existent `DELETE /restaurants/{id}` row. That is not a runtime or integration blocker.

## Evidence summary

- Backend automated tests: `71/71` passed total
- Frontend lint/build: passed
- Live stack health: passed
- Repo smoke harnesses: passed
- Newman/Postman: passed
- Direct targeted probes:
  - invalid `operatingHours` (`99:99-99:99`, `24:00-24:00`, `29:59-29:59`) -> `400`
  - paginated `GET /restaurants` response shape preserved
  - mixed-currency validate -> `400`

## Runtime-state note

- I removed the invalid `Audit Invalid%` rows that had been introduced by the earlier pre-fix audit probe.
- The temporary USD menu item used in this rerun's mixed-currency probe was deleted successfully.
- I also removed the extra `20260419T151743Z` evidence logs generated by this rerun so the repo was not left with additional audit noise under `services/local-dev/evidence`.
- The pre-existing untracked Charlie-Lima-Alfa handover note and the pre-existing `20260419T144233Z` evidence logs were left untouched.

## Appendix A -- Archived superseded Golf-Papa-Tango audit (pre-fix version)

The following archived text preserves the previous Golf-Papa-Tango audit exactly as it stood before the `operatingHours` validation patch and post-fix rerun.

````markdown
# Integration-Handover Readiness Audit -- commit `d23145f`

| Field | Value |
| --- | --- |
| Commit under audit | `d23145fae2d702867b6255ebc17b6c75e581e3f6` (short `d23145f`) |
| Branch observed | `dev` |
| Auditor | Golf-Papa-Tango |
| Audit date | 2026-04-19 |
| Reference handover note reviewed | `dev-docs/audits/audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md` |
| Verdict | `NOT READY TO HAND OVER AS-IS` |

## Findings

### 1. Medium -- invalid `operatingHours` still enters the system through create/update DTO validation

- Files: `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/CreateRestaurantRequest.java:33`, `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/UpdateRestaurantRequest.java:33`, `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/service/RestaurantService.java:119`
- The patched regex `^[0-2][0-9]:[0-5][0-9]-[0-2][0-9]:[0-5][0-9]$` blocks `99:99-99:99`, but it still accepts impossible hours such as `29:59-29:59`.
- I reproduced this live on 2026-04-19: `POST /restaurants` with `operatingHours="29:59-29:59"` returned `201 Created` and persisted the invalid value. The same endpoint correctly returned `400 Bad Request` for `99:99-99:99`.
- `RestaurantService.isWithinOperatingHours()` now safely returns `false` when `LocalTime.parse(...)` fails, so the earlier `acceptsOrders=true` bug is fixed. That hardening is real, but it only masks the downstream availability check. It does not prevent invalid restaurant data from being created, updated, returned by `GET /restaurants/{id}`, or listed by `GET /restaurants`.
- Recommended fix before handover: tighten both DTO regexes to a real `00-23` matcher such as `(?:[01][0-9]|2[0-3]):[0-5][0-9]-(?:[01][0-9]|2[0-3]):[0-5][0-9]`, then add MVC/service coverage for `23:59` accepted and `24:00` / `29:59` rejected.

No other integration-blocking runtime defects were reproduced in this audit. The previous blockers around mixed currencies, paginated `GET /restaurants`, and the Newman suite are fixed at `d23145f`.

## Q1 -- Scope coverage for Sierra-Lima

**Answer: yes for functional scope coverage, no for integration readiness.**

Sierra-Lima's Assignment 3 ownership is still correctly covered in breadth:

- `0001-scope-freeze.md` assigns Sierra-Lima to Restaurant Service (`R19`, `R20`) and Menu Service (`R21`, `R22`).
- `0002-workflows.md` assigns Sierra-Lima to W1 hop 4 (`GET /restaurants/{id}/availability`) and hop 5 (`POST /menu-items/validate`), while W2/W3 remain teammate-owned.
- The codebase still implements the expected 12 Sierra-Lima endpoints across Restaurant and Menu, includes the Vue frontend slice, local Docker stack, dev gateway, Postman/Newman pack, and the optional `menu.item-availability-changed` producer.

So the repository does cover Sierra-Lima's owned subset. The reason for the negative final verdict is quality, not missing scope: invalid `operatingHours` values can still be created and updated.

## Q2 -- Validation from `audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md` that Golf-Papa-Tango was able to complete

I was able to complete every locally reproducible validation Charlie-Lima-Alfa listed, with one important correction to the conclusion around `operatingHours`.

| Charlie audit area | Validation completed by Golf-Papa-Tango | Result |
| --- | --- | --- |
| Backend tests | `mvn test` in `restaurant-service` and `menu-service` | Passed: `26/26` restaurant tests, `43/43` menu tests |
| Frontend checks | `npm run lint -- --no-fix`, `npm run build` | Passed |
| Local stack health | `docker compose --profile dev-gateway up -d --build`, `docker compose ps`, actuator checks, gateway/frontend checks | Passed; all compose services healthy |
| Smoke scripts | `bash smoke.sh`, `bash smoke-cross-service.sh` | Passed; cross-service smoke captured 2 `menu-events` log lines |
| Newman suite | `npx newman run ...` against the checked-in collection/environment | Passed: `39` requests, `66` assertions, `0` failures |
| F1 code-path validation | Diff/code review plus live API probe | Confirmed fixed: mixed EUR/USD validation now returns `400` |
| F2 code-path validation | Diff/code review plus live API probe | Partially fixed: `99:99-99:99` now rejected and malformed parse no longer yields `acceptsOrders=true`, but `29:59-29:59` is still accepted |
| F3 code-path validation | Diff/code review plus live API probe | Confirmed fixed: `GET /restaurants` now returns paged JSON with `content` and paging metadata |
| F4 code-path validation | Diff/code review plus Newman run | Confirmed fixed: Postman zero-price case expects `400`, CRUD delete targets `createdMenuItemId`, collection now runs green |
| F5 status check | Audit/doc review | Confirmed still open and still doc-only |
| Auth / event / endpoint matrix checks | Newman, smoke evidence, controller review | Completed |

Correction to Charlie-Lima-Alfa's report: the item described there as "minor DTO permissiveness" is a live runtime defect, not just a theoretical or optional cleanup item, because the public API still accepts impossible time values.

## Q3 -- Additional validation Golf-Papa-Tango was able to complete beyond Charlie-Lima-Alfa's report

I completed the following additional checks that were not explicitly covered in the Charlie-Lima-Alfa handover note:

- Reviewed the actual delta from `1a6e8c7` to `d23145f` to verify which earlier Golf-Papa-Tango findings were patched in code, frontend, and Postman assets.
- Sent a live `POST /restaurants` request with `operatingHours="99:99-99:99"` and confirmed the endpoint now rejects it with `400 Bad Request`.
- Sent a live `POST /restaurants` request with `operatingHours="29:59-29:59"` and confirmed the endpoint still accepts it with `201 Created`. This is the remaining blocker.
- Sent a live `GET /restaurants?page=0&size=2&city=Tartu&isOpen=true` request and confirmed the response is paginated JSON with `content`, `pageable`, `totalElements`, `size`, `number`, and related metadata rather than a bare array.
- Created a temporary USD menu item, called `POST /menu-items/validate` with one EUR item plus that USD item, and confirmed the live API returns `400 Bad Request` for mixed currencies. The temporary USD menu item was deleted afterward.
- Verified the dev gateway responds on `http://localhost:8080/healthz` and the built frontend responds on `http://localhost:8090/`.

## Q4 -- Validation Golf-Papa-Tango was not able to complete

These follow-up items remain outside what I could responsibly complete in this personal repository audit:

- Full end-to-end W1 across teammate-owned services (User, Order, Payment, Delivery, Notification). I could only validate Sierra-Lima's owned hops and optional gateway probe.
- JWT interoperability against Alfa-Kilo's real token issuer. Local validation here uses the dev HS256 path.
- True W2/W3 broker-backed integration with teammate producers/consumers. Sierra-Lima's optional `menu-events` producer was exercised, but teammate-owned event flows were not available here.
- Cross-checking the password-protected prior submission PDF for any obligations not already reflected in the decision records.
- A destructive "fresh volumes only" replay from a pristine Docker state. I intentionally did not reset the user's existing Postgres volumes.

## Q5 -- Final verdict on readiness for handover to the team lead

**Final verdict: `NOT READY TO HAND OVER AS-IS`.**

Why:

- Scope coverage is in place for Sierra-Lima.
- The important regressions found at `1a6e8c7` are mostly fixed at `d23145f`: mixed-currency validation is now rejected, `GET /restaurants` is paginated again, and the Newman suite is fully green.
- One real defect remains at the Restaurant API boundary: impossible `operatingHours` values in the `24-29` hour range are still accepted and persisted. That means invalid data can enter the integration branch even though the availability check later falls back safely.

If Sierra-Lima tightens the DTO validation for `operatingHours`, adds direct tests for `24:00` / `29:59`, and reruns the same validation matrix, the repository is likely ready for handover.

## Evidence summary

- Restaurant tests: `26/26` passed
- Menu tests: `43/43` passed
- Frontend lint: passed
- Frontend build: passed
- Docker Compose with dev gateway: healthy
- `smoke.sh`: passed
- `smoke-cross-service.sh`: passed
- Newman: `39` requests, `66` assertions, `0` failures
- Live direct probes:
  - `POST /restaurants` with `99:99-99:99` -> `400`
  - `POST /restaurants` with `29:59-29:59` -> `201`
  - `GET /restaurants?page=0&size=2` -> paginated object
  - `POST /menu-items/validate` with EUR + USD items -> `400`

## Runtime-state note

- The local Docker-backed Postgres volumes were already non-pristine before this audit.
- This audit created one restaurant with invalid `operatingHours` (`29:59-29:59`) to prove the remaining bug. There is no Sierra-Lima-owned `DELETE /restaurants/{id}` endpoint, so that row remains in the local dev volume.
- The temporary USD menu item created for the mixed-currency probe was deleted successfully.
- Filesystem changes from this audit are limited to this Markdown report. The pre-existing untracked Charlie-Lima-Alfa handover note and pre-existing evidence logs under `services/local-dev/evidence` were left untouched.
````
