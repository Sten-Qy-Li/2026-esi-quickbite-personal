# Final Handover-Readiness Audit -- commit `bcc9dd0`

| Field | Value |
| --- | --- |
| Commit under audit | `bcc9dd0f93acd333ee0d74adcce875582294382f` (short `bcc9dd0`) |
| Branch observed | `dev` |
| Auditor | Golf-Papa-Tango |
| Audit date | 2026-04-19 |
| Scope | Sierra-Lima-owned slice: Restaurant Service (`R19`, `R20`), Menu Service (`R21`, `R22`), Sierra-Lima frontend slice, `services/local-dev`, W1 hops 4/5, and the F5 documentation close-out in `bcc9dd0` |
| Reference note reviewed | `dev-docs/audits/audit-bcc9dd0_Charlie-Lima-Alfa_final-handover-readiness.md` |
| Important packaging note | The Charlie-Lima-Alfa note above was present in the working tree but was **untracked** during this audit, so I treated it as a checklist, not as evidence shipped by commit `bcc9dd0` itself. |
| Verdict | `NOT READY TO HAND OVER AS FINAL` |

## Findings

### 1. Medium -- `PUT /restaurants/{id}` bypasses the repository's duplicate-name-per-owner rule

- Files:
  - `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/service/RestaurantService.java:52-61`
  - `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/service/RestaurantService.java:75-80`
  - `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/service/RestaurantServiceTest.java:70-77`
  - `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/service/RestaurantServiceTest.java:89-138`
  - `dev-docs/verification/phase-7-verification_Charlie-Lima-Alfa.md:54-56`
  - `dev-docs/report-draft-backend_Sierra-Lima.md:232`
- `create()` checks `existsByOwnerIdAndNameIgnoreCase(...)` and throws `DuplicateRestaurantException`, but `update()` performs no equivalent check before renaming.
- Live reproduction on 2026-04-19:
  - `GET /restaurants/d0000002-0000-0000-0000-000000000002` confirmed the row belongs to owner `00000000-0000-0000-0000-000000000001`.
  - With that owner's token, `PUT /restaurants/d0000002-0000-0000-0000-000000000002` using `"name":"Pizza Antonio"` returned `200 OK`.
  - That created two same-owner restaurants with the same name (`d0000001` and `d0000002`) even though the repo's own verification/docs state that duplicate `(ownerId, name)` pairs should return `409`.
  - I reverted the probe immediately by renaming `d0000002` back to `Sushi Lumi`.
- Impact:
  - The documented business invariant is not actually enforced on the update path.
  - Create and update semantics are inconsistent for the same rule.
  - Owner-facing UI and team integration will still function by `restaurantId`, but the repository is not as bug-free as the current handover note claims.
- Recommended fix before handover:
  - Add an update-time duplicate check that excludes the current restaurant id.
  - Add service and controller coverage for duplicate rename attempts.

### 2. Low -- unsupported HTTP methods are surfaced as `500` instead of `405`

- Files:
  - `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/exception/GlobalExceptionHandler.java:64-67`
  - `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/GlobalExceptionHandler.java:80-83`
- Both services end with a catch-all `@ExceptionHandler(Exception.class)` and do not special-case `HttpRequestMethodNotSupportedException`.
- Live reproduction on 2026-04-19:
  - `DELETE /restaurants/d0000001-0000-0000-0000-000000000001` returned `500`.
  - `PATCH /menu-items/e0000011-0000-0000-0000-000000000011` returned `500`.
- Impact:
  - This is not a contract blocker for W1 and does not affect the implemented teammate-facing routes.
  - It is still incorrect HTTP behavior and slightly weakens confidence in the error-envelope story.
- Recommended fix:
  - Add an explicit `HttpRequestMethodNotSupportedException -> 405 Method Not Allowed` handler in both services.

## Q1 -- Do the implemented functionalities sufficiently cover Sierra-Lima's Assignment 3 ownership?

**Answer: yes in breadth, no as a final-quality sign-off.**

What I confirmed from the original assignment materials and the prior submission:

- `Assignment_3_2026.pdf` requires:
  - service APIs,
  - service data models,
  - system workflows,
  - both synchronous and asynchronous integration mechanisms,
  - and final implementation responsibilities.
- The prior `Assignment-3-Submission.pdf` and repo ADRs align Sierra-Lima to:
  - Restaurant Service,
  - Menu Service,
  - participation in synchronous W1,
  - no direct baseline role in W2/W3.

What the repo currently covers:

- Restaurant Service exposes the expected six endpoints.
- Menu Service exposes the expected six endpoints.
- W1 hop 4 (`GET /restaurants/{id}/availability`) and hop 5 (`POST /menu-items/validate`) are implemented and live.
- The optional `menu.item-availability-changed` producer exists with the log-only seam defined by `0040`.
- Sierra-Lima's frontend slice, dev-gateway stub, Docker stack, smoke scripts, and Postman/Newman assets are present and runnable.

So the owned subset is functionally covered. The negative verdict comes from quality defects, not from missing scope.

## Q2 -- What validation within `dev-docs/audits/audit-bcc9dd0_Charlie-Lima-Alfa_final-handover-readiness.md` was Golf-Papa-Tango able to complete?

**Answer: all repository-local validation steps in that note were reproducible here.**

Completed items from the Charlie-Lima-Alfa checklist:

| Charlie note item | Golf-Papa-Tango result |
| --- | --- |
| 2.1 Restaurant tests | Passed: `28/28` |
| 2.2 Menu tests | Passed: `43/43` |
| 2.3 Frontend lint | Passed |
| 2.4 Frontend build | Passed; hash `b56fb68e13e1cf00` |
| 2.5 Docker stack health | Passed; 6 containers healthy, both actuator health endpoints `UP`, gateway `/healthz` returned `ok`, frontend `HTTP/1.1 200 OK` |
| 2.6 `smoke.sh` | Passed |
| 2.7 `smoke-cross-service.sh` | Passed; `sierra-lima failures = 0`, `teammate failures = 0`, new logs written to `services/local-dev/evidence/cross-service-smoke_20260419T165548Z.log` and `menu-events_20260419T165548Z.log` |
| 2.8 Newman suite | Passed: `39` requests, `66` assertions, `0` failures |
| 2.9 `operatingHours` boundary probes | Confirmed: `24:00-24:00` and `29:59-29:59` return `400` |
| 2.10 `GET /restaurants` shape probe | Confirmed paged shape; in my non-pristine volume run `totalElements=28` rather than `26` |
| 2.11 F5 errata inspection | Confirmed via `git diff bcc9dd0^ bcc9dd0 -- dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md` |
| 2.12 No Restaurant DELETE route | Confirmed; no `delete` or `@DeleteMapping` in Restaurant controller |
| 2.13 Regression re-check | Confirmed for mixed currencies, `operatingHours`, paged restaurant list, and Postman suite |
| 2.14 Event-envelope shape | Confirmed from `menu-events_20260419T165548Z.log` |
| 2.15 Chat-context archive inspection | Confirmed: `bcc9dd0` adds the `2026-04-19_chat-archive_Charlie-Lima-Alfa_15f5ab7.md` archive |
| 2.16 Auth matrix sanity | Confirmed through Newman, smoke runs, and controller test outputs |

One important qualification:

- The Charlie-Lima-Alfa checklist items are reproducible.
- The Charlie-Lima-Alfa **conclusion** ("no defects") is not sustainable after the additional live probe in Finding 1.

## Q3 -- What additional validation was Golf-Papa-Tango able to complete beyond that Charlie-Lima-Alfa note?

I completed the following additional validation not explicitly present in the Charlie-Lima-Alfa note:

1. Cross-checked the original `Assignment_3_2026.pdf` directly with `pdftotext`.
   Result: the repo still matches the assignment's required categories and the "each student implements two microservices" rule.

2. Cross-checked the prior `Assignment-3-Submission.pdf` directly with `pdftotext`.
   Result: Sierra-Lima ownership and W1/W2/W3 role assignment in the repo match the prior submission.

3. Ran a live duplicate-name-on-update probe against Restaurant Service.
   Result: reproduced the medium-severity defect in Finding 1 and then reverted the changed seed row.

4. Ran live unsupported-method probes against both services.
   Result: reproduced the low-severity `500` vs `405` defect in Finding 2.

5. Verified commit packaging separately from the untracked Charlie note.
   Result: `bcc9dd0` itself changes only two Markdown files, while the referenced Charlie handover note remains outside the audited commit.

## Q4 -- What validation was Golf-Papa-Tango not able to complete?

The following still require broader integration infrastructure, destructive environment reset, or manual follow-up:

1. Full end-to-end W1 across teammate-owned services (`User`, `Order`, `Payment`, `Delivery`, `Notification`).

2. JWT interoperability against Alfa-Kilo's real issuer rather than the local HS256 dev minting path.

3. Real broker-backed W2/W3 validation with teammate producers and consumers.

4. A pristine Docker-volume replay (`docker compose down -v` and rebuild from empty databases). I did not wipe the existing local state.

5. Manual browser walkthrough and screenshot-grade UI verification for the frontend slice. I verified build output, health, gateway pathing, and API behavior, but I did not perform a full human browser session in this audit.

## Q5 -- Final verdict on readiness to send the repository to the team lead for integration

**Final verdict: `NOT READY TO HAND OVER AS FINAL`.**

Why:

- The scope coverage is there.
- The main executable matrix is green:
  - Restaurant tests: `28/28`
  - Menu tests: `43/43`
  - Frontend lint/build: passed
  - Docker/dev-gateway stack: healthy
  - `smoke.sh`: passed
  - `smoke-cross-service.sh`: passed
  - Newman: `39` requests, `66` assertions, `0` failures
- F5 is genuinely closed in the committed `bcc9dd0` diff.
- But Finding 1 is a real runtime defect on an owned mutation path. It violates the repo's own documented invariant and was not caught by the existing automated coverage.

My practical recommendation:

1. Fix the duplicate-name check on `PUT /restaurants/{id}`.
2. Add regression coverage for duplicate rename attempts.
3. Preferably also fix the `405` handling issue.
4. Re-run the same local matrix.

If the team lead urgently needs the branch for early integration work, the repo is likely **integration-capable** because the W1 contract surface is stable and green. But it is not yet the "final, as bug-free as possible" handover state requested here.

## Evidence summary

- Assignment and prior-submission scope cross-check: passed
- Automated tests: `71/71` passed
- Newman: `66/66` assertions passed
- Smoke scripts: both passed
- Live probes:
  - invalid `operatingHours` `24:00-24:00` -> `400`
  - invalid `operatingHours` `29:59-29:59` -> `400`
  - mixed-currency validate -> `400`
  - unsupported `DELETE /restaurants/{id}` -> `500`
  - unsupported `PATCH /menu-items/{id}` -> `500`
  - duplicate-name restaurant rename by same owner -> `200` (bug, reverted)

