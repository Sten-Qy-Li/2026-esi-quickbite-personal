# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`a5fc5f9`)

## Session Summary

This session executed **Phase 15 -- Authorisation Hardening & Role-Aware
Behaviour** for the QuickBite stack, as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
Phase 15 (lines 1485-1522).

The session began on top of `a5fc5f9` ("Land Phase 14 frontend-backend
integration and CP#2 prep"). One mid-session context compaction
occurred after the restaurant-service tests went green; work resumed
from the compacted state and completed the menu-service test run,
Postman refresh, and archive/commit cleanly.

Phase 15 lifts the Phase 7 "any authenticated user" gating into
proper role-aware authorisation. Both services now:

1. Gate mutating HTTP verbs at the URL level (`hasAnyRole(
   RESTAURANT_OWNER, ADMIN)`) -- unchanged from Phase 7 but verified
   with negative cases in Postman.
2. Enforce ownership at the service layer -- the authenticated
   principal's `userId` must match `ownerId` on the target
   restaurant, or the principal must have role `Admin`. Denials
   throw `AccessDeniedException`.
3. Log every denial at `WARN` with enough detail to debug: actor
   userId, role, HTTP verb, path, target resource, and owner.
4. Menu Service resolves ownership by calling Restaurant Service's
   existing public `GET /restaurants/{id}` and reading `ownerId` off
   the response. Cross-service calls use Spring's `RestClient` on
   Spring Boot 3.3.4's auto-configured builder.

No `permitAll()` routes existed from Phase 3-7 (we always gated
mutations with `hasAnyRole` and left GETs open by design in Phase 1),
so no removal was needed for Task 3. Public read routes
(`GET /restaurants/**` and `GET /restaurants/{rid}/menu-items` etc.)
are preserved as required.

All tests pass: Restaurant Service **23/23**, Menu Service **35/35**
(14 service tests + 20 controller tests + 1 Spring context).

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`,
  and the `Frontend` under `services/frontend/quickbite-frontend/`.
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Parent commit: `a5fc5f9` -- "Land Phase 14 frontend-backend
  integration and CP#2 prep"
- Environment: Windows 11 + Git Bash + Maven

## User Requests

Initial request: *"Hi Claude, please work on Phase 15 of the master
plan `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.
After completing the implementation of Phase 15, please archive the
session context to `dev-docs/agent-context`, and then commit all
changes and push (try to commit and push the entire local repository;
exclude files only if there's a very good reason, according to your
best judgement). Thanks!"*

No mid-session corrections or redirections from the user. One
context compaction occurred between the restaurant-service and
menu-service test runs; the archive here captures both pre- and
post-compaction work.

## Phase 15 Task-by-Task Record

### Task 1 -- Role-gated mutations on Restaurant Service

**Edit -- `RestaurantService.java`:**

Added a private `requireOwnerOrAdmin(Restaurant r, String endpoint)`
helper and invoked it from `update`, `setStatus`. The helper:

1. Reads the authenticated principal via `CurrentUser.require()`.
2. Short-circuits for role `Admin`.
3. Compares `actor.userId()` to `r.getOwnerId()`; a match passes.
4. Otherwise, logs a `WARN`-level denial line and throws
   `AccessDeniedException` with a message naming the offending
   principal and the target restaurantId.

Top-of-class `private static final Logger log = LoggerFactory.getLogger(...)`
was added alongside the existing `Clock` / `CurrentUser` fields.

The URL-level `hasAnyRole(SecurityRoles.RESTAURANT_OWNER,
SecurityRoles.ADMIN)` gating in `SecurityConfig.java` already
existed from Phase 7; no change was needed there.

`create(...)` is intentionally *not* ownership-gated -- a restaurant
can only be created for the authenticated principal (we always
stamp `ownerId = currentUser.require().userId()`), so ownership is
set at write time rather than checked against a prior owner.

### Task 2 -- Role-gated mutations on Menu Service

Two new top-level files were added to resolve the cross-service
ownership lookup, plus matching service-layer enforcement.

**New -- `RestaurantOwnershipClient.java` (security package):**

A Spring `@Component` wrapping a `RestClient` built off Spring Boot
3.3.4's auto-configured `RestClient.Builder`. Methods:

- `Optional<UUID> findOwnerId(UUID restaurantId)` -- GETs
  `/restaurants/{id}` on Restaurant Service. On 200, deserialises
  the `restaurantId` + `ownerId` fields via the
  `RestaurantSummary(UUID restaurantId, UUID ownerId)` record and
  returns `Optional.of(ownerId)`. On 404, returns `Optional.empty()`.
  On any other 4xx/5xx or network error, throws
  `OwningRestaurantLookupException`.

- Base URL is injected from the new
  `restaurant-service.base-url` property; defaults tuned per profile
  (see Task 3).

A one-line startup log (`INFO RestaurantOwnershipClient base-url=...`)
documents which upstream the client is bound to -- useful when
triaging compose stacks.

**New -- `OwningRestaurantLookupException.java` and
`RestaurantNotFoundForMenuException.java`:**

Added to the `exception` package. The latter is thrown from
`MenuService.requireOwnerOrAdmin(...)` when the upstream returns 404
for the `restaurantId` the caller is trying to create/update/delete
under. Mapped in `GlobalExceptionHandler` to HTTP 404 so the
caller sees a clean "that restaurant doesn't exist" rather than an
internal 5xx.

The first is thrown when the upstream Restaurant Service is
unreachable or returns an unexpected status. Mapped to HTTP 502
(Bad Gateway) so the caller understands this is an upstream
problem, not a request-format problem.

**Edit -- `MenuService.java`:**

Constructor now accepts `CurrentUser currentUser, RestaurantOwnershipClient
restaurantOwnership` alongside the existing `MenuItemRepository menuItems`.

Added `requireOwnerOrAdmin(UUID restaurantId, String endpoint)`:

1. Reads the principal; short-circuits for `Admin`.
2. `restaurantOwnership.findOwnerId(restaurantId)` -- if empty,
   throws `RestaurantNotFoundForMenuException`.
3. Compares the resolved `ownerId` to the actor; match passes.
4. Otherwise logs `WARN` denial and throws `AccessDeniedException`.

Called from `create(UUID restaurantId, ...)`, `update(UUID id, ...)`,
and `delete(UUID id, ...)`. For `update`/`delete` we look up the
existing `MenuItem` first and pass its `restaurantId` into the
helper -- callers can't spoof ownership by referencing a menu-item
id while authenticated as a different restaurant's owner.

`POST /menu-items/validate` is explicitly *not* ownership-gated --
it's an inter-service call from Order Service and must accept
queries against any menu item. Per Phase 9 decisions §4, the
endpoint still requires an authenticated principal (any role).

**Edit -- property files:**

- `application.properties`: `restaurant-service.base-url=${RESTAURANT_SERVICE_BASE_URL:http://localhost:8081}`
- `application-docker.properties`: `restaurant-service.base-url=${RESTAURANT_SERVICE_BASE_URL:http://restaurant-service:8081}`
- `application-test.properties` (test classpath): set to
  `http://restaurant-service.test.invalid` so any forgotten mock
  triggers a loud failure instead of silently hitting localhost.

**Edit -- `docker-compose.yml`:**

`menu-service` now `depends_on: restaurant-service: { condition:
service_healthy }` and receives `RESTAURANT_SERVICE_BASE_URL:
${RESTAURANT_SERVICE_BASE_URL:-http://restaurant-service:8081}`.
This guarantees Menu Service only starts once Restaurant Service can
answer the ownership-lookup GET.

### Task 3 -- Remove any `permitAll()` / debug shortcuts

Survey result: **none existed** in either service. Phase 3-7
intentionally left public GETs at `permitAll()` as the design
choice (see Decision 0010 §2) and gated all mutations behind
`hasAnyRole(RESTAURANT_OWNER, ADMIN)`. No cleanup was needed.

### Task 4 -- Log security denials at WARN

Three denial paths now log at `WARN`; together they cover
**route-level gating**, **ownership failures**, and
**unauthenticated access**:

**`RestAuthEntryPoints.java`** (both services):

- `AuthenticationEntryPoint` (401) logs
  `security denial 401 method=X path=Y reason=Z` before writing the
  canonical error envelope.
- `AccessDeniedHandler` (403, fired by URL-level role checks) logs
  `security denial 403 method=X path=Y actor=<uuid>/<role>
  reason=<msg>`.

**`GlobalExceptionHandler.java`** (both services):

- `@ExceptionHandler(AccessDeniedException.class)` was added in
  both services to catch the service-layer ownership-denial throws
  (Spring Security's own 403 handler only fires for URL-level
  rejections). Logs at `WARN`, returns the canonical 403 envelope.

**`MenuService.java` / `RestaurantService.java`** service-layer
denial log lines carry more context than the global handler can:
actor userId, role, endpoint label, target restaurantId, and
expected ownerId. Triage can diff the two log lines to see whether
the rejection happened at the URL gate or deeper in the service.

### Task 5 -- Tests for the auth matrix

**Restaurant Service** (`RestaurantServiceTest.java`): added 3 tests.

- `update_deniedWhenCallerIsNotOwnerOrAdmin` -- stubs a different
  owner as principal; asserts `AccessDeniedException`.
- `update_adminPassesOwnershipCheck` -- admin principal; asserts
  the update goes through.
- `setStatus_deniedWhenCallerIsNotOwnerOrAdmin` -- as above for the
  status PATCH path.

Existing `update_appliesDetailsAndReturnsResponse` was edited to
inline `when(currentUser.require()).thenReturn(ownerPrincipal)`
because the Phase 15 ownership check now reads the principal even
in the happy path.

**Restaurant Service** (`RestaurantControllerTest.java`): added 2 tests.

- `putRestaurant_foreignOwnerForbidden` -- stubs the service layer
  to throw `AccessDeniedException`; asserts 403 with the canonical
  envelope.
- `patchStatus_adminBypassesOwnership` -- asserts admin can PATCH
  status.

**Menu Service** (`MenuServiceTest.java`): added 4 tests, plus a
refactor.

- Refactor: replaced `@InjectMocks` with explicit constructor in
  `@BeforeEach` so we can pass the new `CurrentUser` +
  `RestaurantOwnershipClient` collaborators. Default stubs are
  `lenient()` to avoid Mockito `UnnecessaryStubbingException` in
  tests that override them.
- `create_deniedWhenCallerIsNotOwnerOrAdmin` -- stubs other-owner;
  asserts `AccessDeniedException`.
- `create_failsWhenOwningRestaurantUnknown` -- ownership client
  returns empty; asserts `RestaurantNotFoundForMenuException`.
- `create_adminBypassesOwnershipCheck` -- admin principal; asserts
  the create goes through.
- `delete_deniedWhenCallerIsNotOwnerOrAdmin` -- the restaurantId
  behind the menu-item is looked up before the denial; exercises
  the full `findById` -> ownership-lookup -> denial path.

**Menu Service** (`MenuControllerTest.java`): added 3 tests.

- `@MockBean RestaurantOwnershipClient` so the Spring context
  doesn't hit the real HTTP client during `@SpringBootTest`.
- `deleteMenuItem_foreignOwnerForbidden` -- customer token hits
  an owner endpoint -> 403.
- `putMenuItem_foreignOwnerForbidden` -- other-owner token on a
  menu item whose ownership resolves elsewhere -> 403.
- `putMenuItem_adminBypassesOwnership` -- admin token -> 200.

**Unused import cleanup:** removed the no-longer-needed `Optional`
import from `MenuControllerTest.java` and the `Duration` import
from the draft of `RestaurantOwnershipClient.java`.

### Task 6 -- Postman negative-auth cases

`QuickBite.postman_collection.json` updates:

- Pre-request script now mints a 4th token, `otherOwnerToken`, for
  user `00000000-0000-0000-0000-000000000002` (the seed owner of
  Cafe Nero and Burger Bros). That ID is *not* the owner of
  `d0000001` (Pizza Antonio), so it's exactly right for the "Owner A
  on restaurant B" 403 case.

- Environment (`QuickBite.postman_environment.json`) gained
  `otherOwnerToken` as an auto-populated blank.

- Six new requests appended to the **Negative Auth** folder:
  - `[403] PATCH /restaurants/{id}/status  other owner (Phase 15)`
  - `[403] PUT /restaurants/{id}  other owner (Phase 15)`
  - `[403] PUT /menu-items/{id}  other owner (Phase 15)`
  - `[403] DELETE /menu-items/{id}  other owner (Phase 15)`
  - `[200] PATCH /restaurants/{id}/status  admin bypass (Phase 15)`
  - `[200] PUT /menu-items/{id}  admin bypass (Phase 15)`

The two admin-bypass cases use idempotent values (patch `isOpen`
back to its seed value, re-post Margherita's current name/price)
so running the folder end-to-end doesn't leave mutable state
lingering in the seed data.

### Task 7 -- Archive and commit

This archive file and the subsequent commit/push land Phase 15 on
`origin/dev`.

## Validation Evidence

### Test runs

Both service suites ran green:

```
Restaurant Service:  Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
Menu Service:        Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
```

WARN denial log lines observed during the menu-service run:

```
WARN RestAuthEntryPoints: security denial 401 method=POST
  path=/restaurants/d0000001-.../menu-items
  reason=Full authentication is required to access this resource

WARN GlobalExceptionHandler: security denial 403 method=PUT
  path=/menu-items/e0000011-...
  reason=User does not own restaurant d0000001-...

WARN RestAuthEntryPoints: security denial 403 method=DELETE
  path=/menu-items/e0000011-...
  actor=00000000-...c1/Customer reason=Access Denied

WARN MenuService: ownership denial
  actor=00000000-...002 role=RestaurantOwner
  endpoint=POST /restaurants/d0000001-.../menu-items
  restaurantId=d0000001-...001 ownerId=00000000-...001
```

The same pattern fires in restaurant-service tests.

## Files Touched

**Restaurant Service (5 files):**
- `service/RestaurantService.java` -- `requireOwnerOrAdmin` helper +
  WARN denial log.
- `exception/GlobalExceptionHandler.java` -- new
  `@ExceptionHandler(AccessDeniedException.class)` returning 403.
- `security/RestAuthEntryPoints.java` -- WARN on 401 + 403.
- `src/test/.../controller/RestaurantControllerTest.java` -- 2 new
  tests.
- `src/test/.../service/RestaurantServiceTest.java` -- 3 new tests
  + happy-path fixup.

**Menu Service (9 files, 3 new):**
- New -- `security/RestaurantOwnershipClient.java`
- New -- `exception/OwningRestaurantLookupException.java`
- New -- `exception/RestaurantNotFoundForMenuException.java`
- `service/MenuService.java` -- constructor + ownership helper.
- `exception/GlobalExceptionHandler.java` -- 3 new handlers.
- `security/RestAuthEntryPoints.java` -- WARN on 401 + 403.
- `resources/application.properties` +
  `resources/application-docker.properties` +
  `src/test/resources/application-test.properties` -- base-url
  property for the ownership client.
- `src/test/.../controller/MenuControllerTest.java` -- 3 new tests.
- `src/test/.../service/MenuServiceTest.java` -- constructor
  refactor + 4 new tests.

**Local dev / Postman (3 files):**
- `services/local-dev/docker-compose.yml` -- `menu-service`
  depends_on restaurant-service healthy + RESTAURANT_SERVICE_BASE_URL.
- `services/local-dev/postman/QuickBite.postman_collection.json` --
  mint `otherOwnerToken` + 6 Phase 15 negative-auth cases.
- `services/local-dev/postman/QuickBite.postman_environment.json` --
  `otherOwnerToken` var.

## Design Decisions / References

- **Decision 0010 §5 + §8** -- Menu -> Restaurant REST call for
  ownership resolution was already authored by Charlie-Lima-Alfa in
  an earlier phase; this phase just realises it. No new ADR needed.
- **Spring Boot 3.3.4** auto-configures `RestClient.Builder`; no
  new bean definition.
- **Role-check placement** -- URL gating (`hasAnyRole(...)`) stays
  in `SecurityConfig`. Ownership lives in the service layer so
  that new verbs (e.g. DELETE restaurant in Phase 17+) inherit the
  check automatically when they call `requireOwnerOrAdmin`.
- **Mockito strict stubbing** vs `@BeforeEach` defaults: we use
  `lenient()` on the default principal + ownership stubs so tests
  that override them (admin, foreign-owner) don't trip
  `UnnecessaryStubbingException`.

## Carry-Over / Next Phase

Nothing blocking for Phase 16. Open threads:

- When Alfa-Kilo's real `User Service` lands, the seeded UUIDs in
  Postman (`000001`, `000002`, `0000c1`, `0000a1`) need to be
  reconciled with the real user directory. The dev-mint flow stays
  as a fallback when hitting services directly.
- Phase 18 (privacy hardening) will likely want to redact the
  `userId` from the WARN denial lines or move to a dedicated
  security-audit channel. For CP#3 the current verbosity is what
  the plan asks for.
