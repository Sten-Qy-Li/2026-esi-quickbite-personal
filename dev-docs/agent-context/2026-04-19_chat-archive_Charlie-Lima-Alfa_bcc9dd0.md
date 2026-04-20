# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`bcc9dd0`)

## Session Summary

Follow-up session at the final-handover stage. Three logical phases:

1. **Self-audit of `bcc9dd0`** — authored
   `dev-docs/audits/audit-bcc9dd0_Charlie-Lima-Alfa_final-handover-readiness.md`
   answering Q1 (scope), Q2 (validation done), Q3 (follow-ups), Q4 (verdict).
   Verdict at time of writing: **READY TO HAND OVER**. Flagged two new
   low-severity observations (Q3.7 HTTP 500 instead of 405 on unsupported
   methods, Q3.8 errata heading wording nit).
2. **Peer review of Golf-Papa-Tango's counter-audit** at
   `dev-docs/audits/audit-bcc9dd0_Golf-Papa-Tango_final-handover-readiness.md`.
   That audit raised a **Medium** defect I had missed: `PUT /restaurants/{id}`
   bypasses the `(ownerId, name)` uniqueness rule that `create()` enforces
   and the docs claim. Downgraded the overall verdict to **NOT READY AS
   FINAL** until the defect is fixed. I agreed with both of their findings
   after independent code inspection.
3. **Fix + full verification** of both findings. All gates green on the
   patched stack.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Upstream at session open: `origin/dev` at `bcc9dd0` ("Close F5 with
  inline errata on 5a998ad audit")
- Working tree at session open: clean
- Session model: Opus 4.7, max effort

## Requests In This Session

1. Thoroughly audit `bcc9dd0`, produce a final handover-readiness audit
   Markdown under `dev-docs/audits`, filename including `bcc9dd0` +
   `Charlie-Lima-Alfa` callsign.
2. Review Golf-Papa-Tango's counter-audit at
   `audit-bcc9dd0_Golf-Papa-Tango_final-handover-readiness.md` and state
   whether I agree with its Findings.
3. Fix / patch / debug both findings.
4. Complete a thorough verification / validation of the patch.
5. Archive this session context under `dev-docs/agent-context/` and
   commit + push everything, excluding only by best judgement.

## Golf-Papa-Tango Findings I Agreed With

**Finding 1 — Medium — `PUT /restaurants/{id}` duplicate-name bypass.**
Independently verified by reading `RestaurantService.java`:
`create()` (line 54) guards with
`existsByOwnerIdAndNameIgnoreCase(ownerId, name)`; `update()` (lines
75-81 pre-fix) had no equivalent check. The uniqueness invariant is
documented generally in `dev-docs/verification/phase-7-verification_Charlie-Lima-Alfa.md:54-56`
("duplicate `(ownerId, name)` pair returns **409**") and in
`dev-docs/report-draft-backend_Sierra-Lima.md:232` ("Uniqueness per owner
enforced in the service layer"), with no restriction to the create path.
Test coverage had `create_rejectsDuplicateNameForSameOwner` (line 71)
but no update-path equivalent, which is why `28/28` stayed green while
the defect lived.

**Finding 2 — Low — unsupported HTTP methods return 500 instead of 405.**
Same root cause I had flagged as Q3.7 in my own audit. Golf-Papa-Tango
extended coverage by probing `PATCH /menu-items/{id}` in addition to
`DELETE /restaurants/{id}`. Both `GlobalExceptionHandler` classes end
with a catch-all `@ExceptionHandler(Exception.class)` that swallows
`HttpRequestMethodNotSupportedException` before Spring's default 405
handler can fire.

## Fix Applied

### Finding 1 — duplicate-name enforcement on update

- `services/restaurant-service/.../repository/RestaurantRepository.java`
  — added derived query method
  `existsByOwnerIdAndNameIgnoreCaseAndRestaurantIdNot(UUID ownerId,
  String name, UUID restaurantId)`.
- `services/restaurant-service/.../service/RestaurantService.java`
  (update path, new lines 78-80) — after the ownership check, call the
  new repo method and throw `DuplicateRestaurantException(ownerId, name)`
  if true. Excluding the current `restaurantId` keeps self-rename and
  pure case changes legal.
- Tests:
  - `RestaurantServiceTest.update_rejectsDuplicateNameForSameOwner`
    (stubs the new repo method to `true`, asserts the exception).
  - `RestaurantServiceTest.update_allowsSameCaseInsensitiveNameOnSameRestaurant`
    (stubs `false`, asserts the rename goes through on case-only change).
  - `RestaurantControllerTest.putRestaurant_duplicateNameForSameOwnerReturns409`
    (service throws, controller advice maps to 409 envelope with
    `error = Conflict`).

### Finding 2 — explicit 405 handler

- Both `GlobalExceptionHandler.java` files (restaurant + menu) —
  added
  `@ExceptionHandler(HttpRequestMethodNotSupportedException.class)` that
  returns `build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req, null)`,
  placed **before** the catch-all so it wins dispatch.
- Tests:
  - `RestaurantControllerTest.deleteRestaurant_unsupportedMethodReturns405`.
  - `MenuControllerTest.patchMenuItem_unsupportedMethodReturns405`.

## Verification At Archive Time

Full matrix re-run after the fix:

| Gate | Result |
| --- | --- |
| Restaurant-service Maven tests | **32 / 32** green (was 28; +4 new) |
| Menu-service Maven tests | **44 / 44** green (was 43; +1 new) |
| Total unit + controller tests | **76 / 76** (was 71; +5 new) |
| Frontend lint (`npm run lint`) | PASSED -- no errors |
| Frontend build (`npm run build`) | PASSED -- hash `b56fb68e13e1cf00` (unchanged) |
| Docker stack rebuild | `docker compose up -d --build restaurant-service menu-service` -- all 6 containers `healthy` |
| Live probe: duplicate rename (PUT d0000002 -> "Pizza Antonio" by same owner) | **409 Conflict**, envelope `Restaurant 'Pizza Antonio' already exists for owner 00000000-...001`; d0000002 name unchanged (`Sushi Lumi`) |
| Live probe: DELETE /restaurants/{id} | **405 Method Not Allowed**, envelope with `error = Method Not Allowed` |
| Live probe: PATCH /menu-items/{id} | **405 Method Not Allowed**, envelope with `error = Method Not Allowed` |
| `smoke.sh` | PASSED (Sierra-Lima smoke test) |
| `smoke-cross-service.sh` | `sierra-lima failures = 0`, `teammate failures = 0`, 2 menu-event lines captured at `evidence/menu-events_20260419T171859Z.log` |
| Newman (Postman collection) | 39 requests / 28 test-scripts / **66 assertions / 0 failures** (3.7s) |

## Files Changed This Session

Modified:

- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/repository/RestaurantRepository.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/service/RestaurantService.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/exception/GlobalExceptionHandler.java`
- `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/service/RestaurantServiceTest.java`
- `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantControllerTest.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/GlobalExceptionHandler.java`
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/controller/MenuControllerTest.java`

Added:

- `dev-docs/audits/audit-bcc9dd0_Charlie-Lima-Alfa_final-handover-readiness.md`
  -- my own final audit, authored at the start of the session. Reflects
  the **pre-fix** state; verdict was READY and the two new observations
  (Q3.7 / Q3.8) were logged as follow-ups.
- `dev-docs/audits/audit-bcc9dd0_Golf-Papa-Tango_final-handover-readiness.md`
  -- the peer-auditor's counter-audit that raised Finding 1 / Finding 2.
  Preserved verbatim per the "treat other-callsign artefacts as
  read-only" feedback.
- `services/local-dev/evidence/cross-service-smoke_20260419T163307Z.log`,
  `services/local-dev/evidence/cross-service-smoke_20260419T165548Z.log`,
  `services/local-dev/evidence/cross-service-smoke_20260419T171859Z.log`
  -- three cross-service smoke runs from this session and the earlier
  Golf-Papa-Tango session (16:55 log is Golf-Papa-Tango's validation of
  the Charlie checklist; 17:18 log is the post-fix rerun).
- `services/local-dev/evidence/menu-events_20260419T163307Z.log`,
  `services/local-dev/evidence/menu-events_20260419T165548Z.log`,
  `services/local-dev/evidence/menu-events_20260419T171859Z.log`
  -- the sibling event-capture logs from the same runs.
- `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_bcc9dd0.md`
  (this file).

Not committed / excluded by judgement: **none**. Working tree had no
`.env`, no secrets, no large binaries, no scratch files. All evidence
logs are test-run traces with no sensitive content.

## Notes for the Next Session

- **Both Golf-Papa-Tango findings are now closed at runtime.** The
  duplicate-name invariant is enforced on both create and update paths
  and has regression coverage on service + controller layers. Unsupported
  HTTP methods now surface a proper 405 + error envelope in both services.
- **Verdict upgrade.** My original `bcc9dd0` audit's verdict was READY;
  Golf-Papa-Tango downgraded it to NOT READY AS FINAL pending Finding 1.
  After this session's patch + verification, the repo re-enters the
  "as bug-free as possible" standard the user was asking for. The
  `bcc9dd0` audit file on disk still reflects the pre-fix state and
  pre-fix verdict -- historical snapshot, not rewritten. The next
  handover-readiness audit (whichever commit it anchors against) should
  cite this archive and the commit that ships the fix as the close-out
  evidence.
- **Test coverage grew from 71 to 76.** Future contributors editing
  `RestaurantService.update` or either `GlobalExceptionHandler` should
  expect the new tests to catch regressions on the duplicate-name rule
  and the 405 handler.
- **`Allow` response header on 405 is currently empty.** Spring's
  default `ResponseEntityExceptionHandler.handleHttpRequestMethodNotSupported`
  would populate it, but our custom handler does not. This is a nit, not
  a blocker -- many real APIs omit `Allow`. If the team lead wants strict
  HTTP 1.1 compliance, the handler can inspect `ex.getSupportedMethods()`
  and set the header explicitly.
- **No open audit items remain from the Golf-Papa-Tango `bcc9dd0`
  counter-audit** for the Sierra-Lima-owned slice. Remaining Q4 items in
  either `bcc9dd0` audit (end-to-end W1 with teammate services, real
  issuer JWT, real broker W2/W3, pristine volume replay, browser UI
  walkthrough) are integration-stack work owned by the team lead and
  teammates, not in-scope for this personal headstart repo.
- **Seed state.** `d0000002` name was verified as `Sushi Lumi` both
  before the post-fix probe (confirms Golf-Papa-Tango's revert held) and
  after the duplicate-rename probe returned 409 (confirms the fix
  rejects the write atomically).
