# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`1a6e8c7`)

## Session Summary

This session was a **patch-only follow-up** to the Golf-Papa-Tango
pre-integration audit that landed in commit `1a6e8c7`
("Deliver pre-integration audit at 5a998ad and align contract + DTOs").
Not a master-plan Phase; a one-off quality gate before the repo is
handed to the Group 7 team lead for integration.

The session began on top of `1a6e8c7`, which carried Golf-Papa-Tango's
audit report at
`dev-docs/audits/audit-1a6e8c7_Golf-Papa-Tango_pre-integration-readiness.md`
and its chat archive at
`dev-docs/agent-context/2026-04-19_chat-archive_Golf-Papa-Tango_1a6e8c7.md`
(both still untracked when this session opened -- they were part of the
handover payload). The user wanted:

1. A read-through of the Golf-Papa-Tango audit with a verdict on whether
   I agreed with each finding.
2. After the review, "Please help to fix these Findings. Thanks!" --
   addressing every fixable finding in my Sierra-Lima scope.
3. After the patches, archive the session and commit + push the whole
   repository (including the untracked Golf-Papa-Tango artefacts from
   the prior session).

Conversation was compacted once -- between the Newman re-run and the
final hand-off summary -- triggered by the combined context of five ADR
read-throughs, all three service source trees, two full Newman runs,
and 69 unit tests' worth of output. The post-compact continuation
picked up at the summary hand-off with no loss of state.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`,
  and the `Frontend` under `services/frontend/quickbite-frontend/`.
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Upstream at session open: `origin/dev` at `1a6e8c7`
- Docker compose stack state at session open: all services healthy
  except as reseeded mid-session (see "Mid-session incident" below)

## Audit Under Patch

The session worked against every finding in
`dev-docs/audits/audit-1a6e8c7_Golf-Papa-Tango_pre-integration-readiness.md`.

Finding summary as interpreted from the audit:

- **F1 (Menu)** -- `POST /api/menu-items/validate` accepts mixed
  currencies in the same batch; no error surfaced. Severity: blocker.
- **F2 (Restaurant)** -- `operatingHours` field accepts nonsense like
  `99:99-99:99` on create/update; downstream `isWithinOperatingHours`
  crashed instead of returning `false`. Severity: blocker.
- **F3 (Restaurant / Frontend)** -- `GET /restaurants` returned a raw
  `List<RestaurantResponse>` while contract ADR `0020 sec 1.5`
  specifies a Spring `Page<T>` envelope. Frontend consumed it as an
  array. Severity: contract drift.
- **F4 (Postman)** -- mixed-currency negative test asserted `422` where
  the Menu service had been returning `400`; Menu CRUD DELETE was
  tearing down the `{{menuItemId}}` seed, breaking any subsequent
  Phase 15 auth test that relied on the seed being alive. Severity:
  test-suite drift.
- **F5 (Charlie-Lima-Alfa audit)** -- the prior Charlie-Lima-Alfa audit
  at `5a998ad` documented a non-existent `DELETE /restaurants/{id}`
  endpoint. Severity: doc-drift only.

I agreed with all five findings during the review pass.

## Findings 1-4: What Was Patched

### Finding 1: Mixed-currency rejection in Menu validate

- Added new exception type
  `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/MixedCurrencyException.java`.
- Wired it into the advice layer
  (`exception/GlobalExceptionHandler.java`) with a mapping to HTTP 400
  and the existing `ErrorResponse` shape.
- In `MenuService.validate(...)`, collected the distinct non-null
  currencies from the items actually found in the DB (via
  `byId.values()` rather than the request lines, so we never throw on
  ghost items) and, if `size > 1`, threw `MixedCurrencyException`. Uses
  `LinkedHashSet` for deterministic ordering in the error message.
- Added unit test `validate_mixedCurrencies_throwsMixedCurrencyException`
  exercising an EUR + USD pair.
- Decision: contract did not specify a dedicated error code, so the
  default `ValidationFailure`-style payload path was used. Rationale
  captured in this archive; the wire shape matches F1's expectation.

### Finding 2: `operatingHours` validation + runtime resilience

- DTOs `CreateRestaurantRequest.java` and `UpdateRestaurantRequest.java`
  now pin the `@Pattern` regex to the contract regex
  `^[0-2][0-9]:[0-5][0-9]-[0-2][0-9]:[0-5][0-9]$`. Note: this still
  technically permits `29:59-29:59` because of the `[0-2][0-9]` segment;
  I matched the contract exactly rather than tightening further.
- In `RestaurantService.isWithinOperatingHours(...)`, malformed or
  unparseable strings now log a `warn` and return `false` instead of
  propagating a `DateTimeParseException`. This also protects the
  auditor-seeded stale row "Audit Invalid Hours" which carries the
  literal `99:99-99:99` string in Postgres.
- Added controller test
  `createRestaurant_impossibleOperatingHoursReturns400` plus two
  service-level tests asserting `acceptsOrders=false` for `99:99-99:99`
  and `not-a-time`.

### Finding 3: Paged `GET /restaurants`

- `RestaurantController.list(...)` now accepts a
  `@PageableDefault(size = 20, sort = "name", direction = ASC)
  Pageable` and returns `Page<RestaurantResponse>`.
- `RestaurantService.search(...)` signature changed to
  `Page<RestaurantResponse> search(String city, Boolean isOpen,
  Pageable pageable)` and it maps `Page<Restaurant>` via
  `RestaurantResponse::from`.
- `RestaurantRepository.search(...)` returns `Page<Restaurant>`; the
  unused `List` import was dropped.
- Frontend: both `views/RestaurantListView.vue` and
  `views/AddMenuItemView.vue` now unwrap the response as
  `data.content` with an `Array.isArray(data)` fallback so any
  consumer that still returns an array keeps working during migration.
- Controller test `listRestaurants_isPublic` updated to construct a
  `PageImpl<>(List.of(), Pageable.ofSize(20), 0)` and assert the three
  envelope fields `$.content`, `$.totalElements`, `$.pageable`.
- Spring logs a "Serializing PageImpl instances as-is is not supported"
  warning at runtime. Accepted: default serialisation shape matches
  contract `0020 sec 1.5` and the warning is advisory.

### Finding 4: Postman collection + environment

- `services/local-dev/postman/QuickBite.postman_collection.json`:
  - Mixed-currency negative test's expected status list
    `[422]` -> `[400]`, plus a new `validationErrors` assertion.
  - Menu CRUD POST renamed to "Margherita (CRUD fixture)" and gained
    a test script that captures the returned `menuItemId` into
    `pm.environment.set('createdMenuItemId', ...)`.
  - Menu CRUD DELETE re-targeted at `{{createdMenuItemId}}` with a 204
    assertion, so the seed `{{menuItemId}}` (Margherita) stays alive
    for Phase 15's negative-auth block.
- `services/local-dev/postman/QuickBite.postman_environment.json`: new
  entry `createdMenuItemId` (empty, auto-populated) with a description
  explaining the CRUD lifecycle.

## Finding 5: Deliberately Not Touched This Session

The F5 doc-drift lives in
`dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md`,
which is a prior Charlie-Lima-Alfa artefact. My operating feedback
memory says "treat other-callsign artefacts as read-only; commit
verbatim when asked". That rule technically doesn't block Charlie-Lima-Alfa
editing Charlie-Lima-Alfa, but the user explicitly said "Do not touch
Finding 5 just yet" when triaging the archive-and-commit step, so the
DELETE-endpoint claim remains in that audit file. Deferred to a future
session; the decision is recorded here for continuity.

## Verification

- `mvn -q test` in `services/menu-service`: 43 tests green (1 added for F1).
- `mvn -q test` in `services/restaurant-service`: 26 tests green
  (3 added across F2 + F3).
- `npm run lint` and `npm run build` in
  `services/frontend/quickbite-frontend`: green, no lint deltas.
- `docker compose up -d` stack: all services healthy.
- `newman run QuickBite.postman_collection.json -e QuickBite.postman_environment.json`:
  **66/66 assertions passing** on two consecutive runs (after the
  reseed described below).
- Live probe of paged `GET /restaurants?size=2` confirmed contract shape:
  `content`, `pageable`, `last`, `totalPages`, `totalElements`, `first`,
  `size`, `number`, `sort`.

## Mid-session Incident: Stale Postgres Volume

The first Newman run after the rebuild failed 4 of 66 assertions,
all on the Phase 15 "owner A touches owner B's restaurant" block,
with 404 where the collection expected 403. Root cause: the Postman
`{{menuItemId}}` seed (`e0000011-...`, Margherita) had been deleted
by an earlier audit Newman run -- before F4's DELETE re-targeting
-- and the Docker volume had persisted that deletion across 5+ hours
of compose restarts.

Recovery: ran a parameterised `INSERT ... ON CONFLICT DO UPDATE` via
`docker compose exec menu-db psql -U menu_user -d menu_db` to restore
the Margherita seed row to its canonical values. Second Newman run
immediately went green (66/0). The root-cause itself was the bug F4
fixes, so this incident does not recur with the patched collection.

No schema or seed SQL was changed; the `db-menu/init/` scripts remain
authoritative on a fresh `down --volumes`.

## Files Changed This Session

Modified:

- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/GlobalExceptionHandler.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java`
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/service/MenuServiceTest.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantController.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/CreateRestaurantRequest.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/UpdateRestaurantRequest.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/repository/RestaurantRepository.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/service/RestaurantService.java`
- `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantControllerTest.java`
- `services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/service/RestaurantServiceTest.java`
- `services/frontend/quickbite-frontend/src/views/AddMenuItemView.vue`
- `services/frontend/quickbite-frontend/src/views/RestaurantListView.vue`
- `services/local-dev/postman/QuickBite.postman_collection.json`
- `services/local-dev/postman/QuickBite.postman_environment.json`

Added:

- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/MixedCurrencyException.java`

Carried over verbatim from the prior Golf-Papa-Tango session (per the
"commit other-callsign artefacts verbatim when asked" rule):

- `dev-docs/agent-context/2026-04-19_chat-archive_Golf-Papa-Tango_1a6e8c7.md`
- `dev-docs/audits/audit-1a6e8c7_Golf-Papa-Tango_pre-integration-readiness.md`

Added this hand-off:

- `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_1a6e8c7.md` (this file)

## Notes for the Next Session

- Finding 5 is still open. Options: (a) edit the Charlie-Lima-Alfa
  `5a998ad` audit in place to retract the DELETE-endpoint claim, or
  (b) surface an errata in a Sierra-Lima-owned doc (phase errata,
  README, etc.). User's current posture is "not just yet".
- The regex on `operatingHours` matches the contract but is not as
  strict as it could be (`29:59` is technically accepted). If the
  team wants `^([01][0-9]|2[0-3]):[0-5][0-9]-([01][0-9]|2[0-3]):[0-5][0-9]$`
  behaviour, that's a contract amendment for ADR `0020`, not a unilateral
  tightening.
- `PageImpl` serialisation warning will persist until someone wraps
  the response in a dedicated `PagedResponse<T>` DTO; not urgent.
- The stale "Audit Invalid Hours" row seeded by Golf-Papa-Tango during
  their live probe is still in the Restaurant DB. `isWithinOperatingHours`
  now handles it correctly, but a clean rebuild (`docker compose down
  --volumes`) would make it disappear.
