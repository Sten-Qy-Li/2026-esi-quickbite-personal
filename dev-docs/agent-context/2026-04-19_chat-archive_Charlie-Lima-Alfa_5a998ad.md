# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`5a998ad`)

## Session Summary

This session was a **pre-integration readiness audit** of Sierra-Lima's
personal QuickBite headstart repository, followed by patches for every
deviation the audit flagged as "keep or patch". Not a master-plan Phase;
a one-off quality gate before the repo is handed to the Group 7 team
lead for integration.

The session began on top of `5a998ad` ("Add Phase 19 errata: correct
seeded-owner IDs and item counts"), which sat on `v1.0.0-cp3`'s parent
commit `50774fe`. The user wanted:

1. A thorough audit answering four questions -- coverage (Q1),
   validation done (Q2), validation not doable here (Q3), and a final
   verdict (Q4) -- delivered as a Markdown report under `dev-docs/audits/`
   whose filename carried the short hash `5a998ad` and the callsign
   `Charlie-Lima-Alfa`.
2. After the audit, "for anything with a keep-or-patch recommendation,
   please help to patch."
3. After the patches, archive the session and commit + push.

Conversation was compacted once -- between evidence-gathering and the
audit write-up -- triggered by the full-repo read-throughs of the five
ADRs (`0001`, `0010`, `0020`, `0030`, `0040`) plus the
restaurant-service + menu-service source trees, plus the 65-test
run-through, plus the full Docker-stack smoke traces. The post-compact
continuation picked up directly at the audit Markdown draft with no
loss of state.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`,
  and the `Frontend` under `services/frontend/quickbite-frontend/`.
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Parent commit: `5a998ad` -- "Add Phase 19 errata: correct seeded-owner
  IDs and item counts"
- Environment: Windows 11 + Git Bash + Docker Desktop. Maven invoked
  directly (unlike Phase 19's `docker compose up --build`-only posture)
  because this session re-ran the `mvn test` suites to source the
  "Q2 validation completed" evidence. Docker rebuild + recreate was
  required after the DTO patches so smokes could run against patched
  jars.

## User Requests

**Initial request (audit):** *"Hi Claude, thanks for working on Phases 1
to 19! I now want the repository to be as bug-free as possible, so I
can pass this personal headstart repository to our team lead for
integration. Please help to perform a thorough audit of the commit
`5a998ad`, answering the following questions: Q1 (coverage), Q2
(validation completed), Q3 (validation not completable by me), Q4
(final verdict). Please author an audit report as a Markdown file, which
should go under `dev-docs/audits`. Include the short commit hash
`5a998ad` and your call-sign Charlie-Lima-Alfa in the file name, in
addition to these two elements please propose and use a file name that
you'd recommend. Thanks!"*

**Follow-up (patches):** *"Hi Claude, for anything with a keep-or-patch
recommendation, please help to patch. Thanks!"*

**Closing (archive + commit + push):** *"Hi Claude, please help to
archive the session context to `dev-docs/agent-context`, and then
commit and push to remote. Thanks!"*

Model pinned to `opus` and effort to `max` early via `/model opus` +
`/effort max`. No mid-session redirections or corrections beyond the
compaction.

## Audit (Part A)

### Deliverable

`dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md`
-- ~180 lines, structured in four sections + a reproduction appendix:

- **§1 Coverage (Q1).** Traceability matrix mapping all 11 REST
  endpoints + the cross-service workflow responsibilities + the data
  model + the seed data back to the five ADRs. Verdict: **coverage is
  complete**, with three documentation drifts listed in §4 of the
  report.
- **§2 Validation completed (Q2).** Twelve checks: two `mvn test`
  suites (23/23 + 42/42 = 65/65 green), `npm run build`, `npm run
  lint`, `docker ps` (6/6 healthy), `smoke.sh`, `smoke-cross-service.sh`
  (2 `menu-events` envelopes captured), `curl` on three `/actuator/health`
  endpoints, auth-matrix spot-checks against `0010`, contract shape
  spot-checks against `0020`/`0030`, Flyway migration review, and a
  static code walk.
- **§3 Validation NOT completable (Q3).** Seven follow-ups:
  Newman/Postman (39 requests), full W1 chain (teammate services
  absent), W2/W3 async observation, real `user-service` JWT issuer,
  presentation-deck screenshots (deferred to 2026-05-18 rehearsal),
  optional load profile, and the encrypted
  `Assignment-3-Submission.pdf` cross-check.
- **§4 Minor drifts (for user's awareness).** Three items with a
  keep-or-patch recommendation: (4.1) `PUT`/`PATCH` returning
  `200 + body` vs contract `204`, (4.2) `price_currency VARCHAR(3)`
  vs contract `CHAR(3)`, (4.3) DTO bean-validation gaps (city
  `@NotBlank`, priceAmount `@Positive`/`@Digits`, isAvailable `@NotNull`
  on create).
- **§5 Final verdict (Q4).** READY TO HAND OVER. No defects, no
  integration-blocking issues.

### How the audit was sourced

The canonical-scope anchor was not the team's `Assignment-3-Submission.pdf`
(password-protected; I couldn't open it). I pivoted to the five ADRs
`dev-docs/decisions/0001`/`0010`/`0020`/`0030`/`0040`, which already
formalise Sierra-Lima's frozen scope end-to-end. That was sufficient
for the audit; I listed the PDF as a follow-up in §3.7 so the user can
open it and confirm no unmentioned obligations.

### Filename choice

Slug `pre-integration-readiness` -- short, describes the audit's
purpose, and disambiguates from any future audit that's targeted at a
different lifecycle moment (release review, defect dive, etc).

## Keep-or-Patch Resolution (Part B)

Each §4 item from the audit was resolved under the user's follow-up
request.

### 4.1 PUT/PATCH response (keep code; patch contract)

Contract `0020 §1.3 / §1.4 / §2.4` said `204 No Content` empty body.
Code returns `200 OK` with the updated resource. Tests and the Vue
frontend (`MenuItemDetailView.vue`, restaurant edit view) consume the
returned body. Reverting to 204 would break UI.

Patched the contract text instead. Added a one-line motivation next
to each (*"avoids a second round-trip by the frontend's edit view",
"lets the owner dashboard refresh its badge without a follow-up GET",
"lets the menu-item detail view refresh the price/availability badges
in place"*) so the drift is not reopened by a future reader.

### 4.2 `price_currency` type (keep code; patch contract)

Code uses `VARCHAR(3)`; contract said `CHAR(3)`. Storage semantics are
identical here (both hold `'EUR'` without padding). Patched contract
§4.2 to `VARCHAR(3)`.

### 4.3 DTO bean-validation gaps (patch code + patch contract text)

Three sub-items, each handled individually:

- **`city @NotBlank` on both restaurant DTOs.** Added. No test sends
  blank city; no breakage. Straight contract alignment.
- **`priceAmount @Positive` + `@Digits(integer=17, fraction=2)` on
  both menu DTOs.** Added. The `integer=17` cap mirrors the DB column
  `NUMERIC(19,2)`. One controller test
  (`createMenuItem_invalidPriceReturns422`) was mocking the
  service-layer `InvalidPriceException` to produce a 422 for
  `priceAmount: 0`. After the patch, bean validation rejects `0`
  before the service is called, so the mock path is dead and the
  response is `400` with `validationErrors[].field = "priceAmount"`.
  Renamed the test to `createMenuItem_zeroPriceReturns400` and
  adjusted its expectations; dropped the now-unused
  `InvalidPriceException` import. The service-layer `validatePrice`
  method itself was kept as a defensive guard (called by unit tests
  that bypass bean validation).
- **`isAvailable @NotNull` on `CreateMenuItemRequest`.** *Did not
  patch the code.* Reason: `MenuServiceTest.create_persistsItemWithDefaultAvailable`
  (line 86) deliberately sends `null` and asserts the service defaults
  to `true`; the test name itself documents this as intentional. The
  Vue `AddMenuItemView.vue` always supplies a value anyway (checkbox
  default `true`). Adding `@NotNull` would turn a sensible ergonomic
  default into a 400 for any client that omits the field. Instead,
  patched contract `0020 §3` to document the CREATE-vs-UPDATE
  asymmetry: optional on POST (defaults to `true`), `@NotNull` on PUT
  (already enforced in `UpdateMenuItemRequest`). This makes the code
  and the contract agree, in the direction the code already goes.

### Post-patch verification

- `mvn test` on both services: **65/65 green** (no count change;
  controller test renamed, not added or removed).
- Rebuilt `restaurant-service` + `menu-service` jars with
  `mvn -DskipTests package`.
- `docker compose build` + `docker compose up -d` recreated both
  service containers from the fresh images. All 6 containers back
  to `healthy`.
- `bash smoke.sh`: exit 0, "Sierra-Lima smoke test passed".
- `bash smoke-cross-service.sh`: exit 0; `sierra-lima failures = 0`;
  `teammate failures = 0`; 2 `menu-events` envelope lines captured
  at `evidence/menu-events_20260419T125247Z.log`.

The audit document was intentionally *not* rewritten after the patches
-- it's a point-in-time record against commit `5a998ad`. The patches
land as follow-up work visible in the diff.

## Artefacts Produced / Modified

| Path | Change |
|------|--------|
| `dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md` | New (the audit report) |
| `dev-docs/decisions/0020-sierra-lima-contracts.md` | Modified (§1.3/§1.4/§2.4 to 200+body; §3 priceAmount `@Digits` + isAvailable create/update asymmetry; §4.2 VARCHAR(3)) |
| `services/restaurant-service/src/main/java/.../dto/CreateRestaurantRequest.java` | Modified (`@NotBlank` on city) |
| `services/restaurant-service/src/main/java/.../dto/UpdateRestaurantRequest.java` | Modified (`@NotBlank` on city) |
| `services/menu-service/src/main/java/.../dto/CreateMenuItemRequest.java` | Modified (`@Positive` + `@Digits(17,2)` on priceAmount) |
| `services/menu-service/src/main/java/.../dto/UpdateMenuItemRequest.java` | Modified (`@Positive` + `@Digits(17,2)` on priceAmount) |
| `services/menu-service/src/test/java/.../controller/MenuControllerTest.java` | Modified (zero-price test renamed + updated; unused import removed) |
| `services/local-dev/evidence/cross-service-smoke_20260419T122526Z.log` | New (pre-patch smoke evidence captured during audit §2) |
| `services/local-dev/evidence/cross-service-smoke_20260419T125247Z.log` | New (post-patch smoke evidence captured after §4.3 DTO changes) |
| `services/local-dev/evidence/menu-events_20260419T122526Z.log` | New (pre-patch menu-events sample; 2 envelopes) |
| `services/local-dev/evidence/menu-events_20260419T125247Z.log` | New (post-patch menu-events sample; 2 envelopes) |
| `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_5a998ad.md` | New (this archive) |

Additionally, 9 verification-doc renames already staged at session
start (carry-over from the earlier `phase-7/8/9/11/14/16/17/18/19` →
`_Charlie-Lima-Alfa` rename pass; see memory
`feedback_callsign_author_suffix.md`). These were committed as a
focused first commit of the session, before the audit bundle.

## Notable Decisions

1. **Write the audit against the ADRs, not the encrypted team PDF.**
   The team's `Assignment-3-Submission.pdf` under
   `dev-docs/prior-submissions/` is password-protected. Rather than
   asking the user to unlock it (adds a round-trip and makes the
   session longer), I pivoted to the five ADRs that already formalise
   Sierra-Lima's frozen scope. Listed the PDF in §3.7 of the audit as a
   "please cross-check" follow-up so the user retains a traceable gap
   they can close in seconds.

2. **Keep the §4.1 behaviour (200+body), patch the contract.** The
   frontend and tests both depend on `PUT`/`PATCH` returning a body.
   Reverting to 204 would require UI refactors and test rewrites for a
   cosmetic contract match. Documented the rationale inline in the
   contract so the drift doesn't reappear.

3. **Do NOT add `@NotNull` to `CreateMenuItemRequest.isAvailable`.**
   The asymmetry (optional on create, required on update) is
   intentional and asserted by a test name. Added the asymmetry to the
   contract instead. A `@NotNull` patch here would have broken a
   test-documented feature for a textbook-alignment win that nobody
   asked for.

4. **Patch contract text for deltas; patch code for genuine misses.**
   Followed this split for all three §4 items: 4.1 and 4.2 were pure
   documentation drift (code is correct, text is stale); 4.3 was a
   mix -- `city @NotBlank` and `priceAmount @Positive/@Digits` are
   genuine textbook gaps so they got code patches; `isAvailable`
   required a contract patch only (see #3 above). This keeps blast
   radius proportional to each item.

5. **Renamed `createMenuItem_invalidPriceReturns422` → `...zeroPriceReturns400`.**
   With bean validation catching `priceAmount: 0` before the service,
   the old test name (referring to the service-layer 422 path) became
   misleading. Renaming keeps the test's assertion legible at a glance
   and matches the actual code path being exercised. Kept the
   service-layer `validatePrice` method untouched -- it's still
   exercised by three unit tests (`MenuServiceTest.create_priceAmountZero/Negative/ScaleThreeDecimals`)
   which call `service.create()` directly and so bypass bean
   validation.

6. **Two commits, not one.** The 9 verification-doc renames were
   already staged at session start -- carry-over from the earlier
   rename pass. Rather than lumping them into the audit bundle (which
   would produce a confused commit covering unrelated work), split
   into a focused rename commit plus the audit+patches commit. Each
   commit has a clean narrative.

7. **No tag on this commit.** `v1.0.0-cp3` is the final A3 freeze
   (on `50774fe`). This session's post-freeze polish is a regular
   `dev` commit, not a new tag. The presentation-rehearsal slot on
   2026-05-18 is the next natural tag point.

## Carry-overs out of this session

- §3 of the audit lists seven follow-up validations that Sierra-Lima
  (or the team lead) will need to close on the integrated stack.
  The only one Sierra-Lima can close personally before handover is
  **§3.1 Newman run** (one-liner: `npm i -g newman && newman run
  dev-docs/contracts/postman/QuickBite-Sierra-Lima.postman_collection.json
  -e <env>.json`); the rest require teammate services or the 2026-05-18
  rehearsal slot.

- §3.7 encrypted `Assignment-3-Submission.pdf` cross-check: a
  two-minute unlock-and-skim that closes out the audit completely.

## Environment Notes

- **Parallel Bash cwd isolation.** When I ran two `mvn ... package`
  calls in parallel (one per service, each prefixed with its own `cd`),
  the second one produced an empty target. Re-ran it serially with an
  explicit `cd && mvn ...` and the jar was built correctly. Worth
  remembering: `cd` persists between sequential Bash calls but each
  parallel call effectively starts in its own cwd.

- **CRLF line-ending warnings.** `git diff --stat` surfaced LF→CRLF
  conversion warnings for each modified file. Harmless under the
  repo's `.gitattributes`; the files land on disk as CRLF on Windows,
  LF in Git storage.

## Closing State

Commits (created at end-of-session, in this order):

1. `Rename verification docs to Charlie-Lima-Alfa author suffix` --
   commits the 9 already-staged renames (pure rename, 0 line changes).
2. `Deliver pre-integration audit at 5a998ad and align contract + DTOs`
   -- the audit report, the §4 patches to contract/DTOs, the
   controller test adjustment, this archive, and the four fresh smoke
   evidence logs.

Push: branch `dev` to `origin`. No tag. No force-push, no amend, no
rebase.
