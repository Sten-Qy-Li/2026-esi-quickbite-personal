# Pre-Team-Integration Readiness Audit -- commit `50b8e1d`

| Field | Value |
| --- | --- |
| Commit under audit | `50b8e1db8d5ad7243b4c6939b8e3817fff6c9498` (short `50b8e1d`) |
| Commit subject | `Close Charlie-Lima-Alfa 7b2fa61 gap-analysis F2 (DELETE menu-item UI)` |
| Branch | `dev` (working tree clean at audit time apart from freshly-captured evidence logs) |
| Author of commit | Sten-Qy-Li (Sierra-Lima) |
| Auditor | Charlie-Lima-Alfa (Claude Opus 4.7, acting for Sierra-Lima) |
| Audit date | 2026-04-20 |
| Purpose | Verify the current `dev` tip is free of pre-team-integration bugs before Sierra-Lima hands the branch to the Group 7 team lead |
| Scope anchor | Sierra-Lima's owned subset per `dev-docs/decisions/0001-scope-freeze.md`: Restaurant Service (R19/R20) + Menu Service (R21/R22) + supporting frontend/compose/Postman assets. |

> **Context.** `50b8e1d` lands the DELETE menu-item UI button that the
> `7b2fa61` Charlie-Lima-Alfa gap analysis flagged as F2 rec #1 (Low)
> against Project Brief §4.3 C. Nothing else has landed since the
> `7b2fa61` Golf-Papa-Tango team-lead audit. This audit therefore has
> two jobs: (a) re-validate the whole Sierra-Lima slice against the
> baseline known-good state recorded at `7b2fa61`, and (b) scrutinise
> the new DELETE UI wiring end-to-end.

---

## 1. Scope reviewed

Sierra-Lima's Assignment 3 ownership per decisions `0001` / `0020`:

- `Restaurant Service` for R19 and R20 -- 6 endpoints.
- `Menu Service` for R21 and R22 -- 6 endpoints, plus the optional
  Phase 16 log-only `menu-events` publisher.
- W1 hops 4 and 5 (`GET /restaurants/{id}/availability`, `POST /menu-items/validate`).
- Sierra-Lima's share of the Vue frontend (browse + owner CRUD).
- `services/local-dev/` Docker stack (Postgres x2, both services,
  frontend, opt-in `dev-gateway` profile), smoke scripts, Postman
  pack, runbook.
- Non-participation in W2/W3 per `0040`.

Files re-read at `50b8e1d`:

- `services/restaurant-service/src/main/java/.../controller/RestaurantController.java`
- `services/restaurant-service/src/main/java/.../service/RestaurantService.java`
- `services/restaurant-service/src/main/java/.../config/SecurityConfig.java`
- `services/restaurant-service/src/main/java/.../exception/GlobalExceptionHandler.java`
- `services/menu-service/src/main/java/.../controller/MenuController.java`
- `services/menu-service/src/main/java/.../service/MenuService.java`
- `services/menu-service/src/main/java/.../config/SecurityConfig.java`
- `services/menu-service/src/main/java/.../exception/GlobalExceptionHandler.java`
- `services/menu-service/src/main/java/.../security/RestaurantOwnershipClient.java`
- `services/menu-service/src/main/java/.../dto/MenuItemResponse.java`
- `services/frontend/quickbite-frontend/src/views/MenuItemDetailView.vue` (the new DELETE button)
- `services/frontend/quickbite-frontend/src/views/MenuView.vue`
- `services/frontend/quickbite-frontend/src/router/index.js`
- `services/frontend/quickbite-frontend/src/api/client.js`
- `services/frontend/quickbite-frontend/src/auth/token.js`
- `services/local-dev/docker-compose.yml`, `smoke.sh`, `smoke-cross-service.sh`
- `services/local-dev/postman/QuickBite.postman_collection.json` + environment

Diff range covered: `7b2fa61..50b8e1d` -- two commits, both
Sierra-Lima-authored, shipping (i) the 2026-04-20 gap analysis and its
chat archive, (ii) the DELETE UI wiring and its chat archive. No
backend Java code changed in this range.

## 2. Validations completed

All checks below ran locally on the Windows 11 host at 2026-04-20.

| # | Check | Command | Result |
|---|---|---|---|
| 2.1 | Restaurant-service tests | `mvn test` (in `services/restaurant-service`) | **Tests run: 33, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS**. Unchanged from `7b2fa61`. |
| 2.2 | Menu-service tests | `mvn test` (in `services/menu-service`) | **Tests run: 47, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS**. Unchanged from `7b2fa61`. |
| 2.3 | Frontend lint | `npm run lint -- --no-fix` | `DONE  No lint errors found!` -- the new `btn-danger` styles and `onDelete()` handler lint clean. |
| 2.4 | Frontend build | `npm run build` | `DONE  Compiled successfully in 3338ms`; app 47.98 KiB, vendor 130.84 KiB, hash `e665785ceb70f60d`. |
| 2.5 | Docker stack health | `docker compose --profile dev-gateway up -d --build` + `docker ps` + actuators | All 6 containers healthy: `quickbite-restaurant-db`, `quickbite-menu-db`, `quickbite-restaurant-service`, `quickbite-menu-service`, `quickbite-frontend`, `quickbite-dev-gateway`. Both Spring actuators `UP` with DB `UP`; dev-gateway `/healthz` returns `ok`. |
| 2.6 | Sierra-Lima smoke | `bash services/local-dev/smoke.sh` | `OK -- Sierra-Lima smoke test passed.` Fresh tokens minted, restaurant and menu-item created, status toggle, availability `acceptsOrders=true`, batch validate `allValid=true`. |
| 2.7 | Cross-service smoke | `bash services/local-dev/smoke-cross-service.sh` | `sierra-lima failures = 0, teammate failures = 0`. 2 `menu-events` JSON log lines captured to `services/local-dev/evidence/menu-events_20260420T115714Z.log`. Teammate probes SKIPped as designed. |
| 2.8 | Newman full suite | `npx newman run postman/QuickBite.postman_collection.json -e postman/QuickBite.postman_environment.json` | iterations 1, requests 39, test-scripts 30, pre-request-scripts 39, **assertions 68, failures 0**. See §3 Finding 1 for why this green signal partially masks a brittle request. |
| 2.9 | Live DELETE `/menu-items/{id}` happy path | curl with freshly-minted owner token after creating a throwaway restaurant + menu item | POST `/restaurants` → `201`; POST `/restaurants/{rid}/menu-items` → `201`; DELETE `/menu-items/{id}` (customer) → `403`; DELETE `/menu-items/{id}` (owner) → `204`; GET `/menu-items/{id}` after delete → `404`. |
| 2.10 | Live DELETE `/menu-items/{id}` not-found | curl DELETE with an unused UUID and owner token | `404` -- matches `MenuItemNotFoundException` path. |
| 2.11 | Live method-not-allowed (authenticated) | DELETE `/restaurants/{id}` with owner token, PATCH `/menu-items/{id}` with owner token | Both return **`405`** -- confirms the `7b2fa61` patch that adds `HttpRequestMethodNotSupportedException` handling in both `GlobalExceptionHandler`s is live. (Unauthenticated variants still return `401` because the Spring Security chain runs before Spring MVC method resolution, which is standard and correct.) |
| 2.12 | Live `operatingHours` regex | POST `/restaurants` with `29:59-29:59` and with `""` | Both return `400` -- confirms `15f5ab7` regex tightening (`^(?:[01][0-9]|2[0-3]):[0-5][0-9]-(?:[01][0-9]|2[0-3]):[0-5][0-9]$`) is enforced at DTO layer. |
| 2.13 | Live mixed-currency validate | created a USD-priced and a EUR-priced menu item in one owned restaurant, then POST `/menu-items/validate` with both | `400` -- `MixedCurrencyException` path live. |
| 2.14 | Live paged `GET /restaurants` | `curl /restaurants?page=0&size=1` | `200` -- paged `Page<RestaurantResponse>` shape. |
| 2.15 | Frontend DELETE UI -- code review | read `MenuItemDetailView.vue:27-47`, `:248-265` | `canManage` gates the button on `RestaurantOwner`/`Admin` role (`auth/token.js:65-68`); `onDelete()` prompts via `window.confirm`, captures `restaurantId` before the call, DELETEs via `api.delete('/api/menu-items/...')`, routes back to `{ name: 'restaurant-menu', params: { id: restaurantId } }` on success, and surfaces `ApiError.message` in a scoped `deleteError` banner on failure. The `deleting` flag disables the sibling Toggle button and itself to prevent double-submits. |
| 2.16 | Frontend DELETE UI -- route resolution | read `router/index.js:44-48`, `:56-60` | `restaurant-menu` resolves to `/restaurants/:id/menu` (i.e. `MenuView.vue`); `menu-item-detail` resolves to `/menu-items/:id` (the view hosting the new button). The redirect target matches the list view, so the deleted item disappears on the landing page. |
| 2.17 | Frontend DELETE UI -- backend-test parity | read `MenuControllerTest.java:219-272` | Happy path (`deleteMenuItem_ownerReturns204`), `401` unauthenticated, `403` customer-role, `403` foreign owner, `404` not-found are all covered. Matches what the new UI will exercise live. |

### 2.A Summary of Q2 evidence

- **80/80 automated tests pass** at `50b8e1d` (33 restaurant + 47 menu), matching the `7b2fa61` baseline.
- **Newman 68/68 assertions pass** over 39 requests -- but see Finding 1 for a request that is quietly returning `409` instead of `200` because it has no assertion. No test-script failure is raised, so the top-line green is correct but misleading.
- **Both smoke scripts exit 0**; `menu-events` fires with the frozen `0032 §6` envelope.
- **DELETE menu-item is live and role-gated** end-to-end from the new UI button through the API through the service ownership check through the repository.
- **Live regression probes confirm** the `7b2fa61` and `15f5ab7` patches (`405` method-not-allowed, strict `operatingHours` regex, mixed-currency rejection) are all intact.
- **No backend regression** and no new lint/build error was introduced by `50b8e1d` / `8b0a2b5`.

## 3. Findings and risks

### Finding 1. Medium -- Postman `PUT /restaurants/{id}` silently returns `409` on non-pristine volumes

- **File.** `services/local-dev/postman/QuickBite.postman_collection.json:226-243`.
- **Evidence captured this run.** Newman log line: `PUT http://localhost:8081/restaurants/33dd1836-a9a3-4da6-904a-813bed7bdaa4 [409 Conflict, 677B, 11ms]`. The full Newman run still reports `68/68` assertions and `0` failures because that specific request has no `event` block with a `test` script (unlike the sibling POST at `:181-194`, which asserts `status is 201` and captures `restaurantId`).
- **Root cause.** The update body hard-codes `"name": "Pizza Antonio (updated)"` (line 235) with no uniqueness suffix. On non-pristine local volumes -- which is the steady state, because Sierra-Lima has no `DELETE /restaurants/{id}` endpoint so earlier audit runs accumulate rows -- the duplicate-name protection added at `1a6e8c7` correctly rejects the rename with `409`. The preceding POST (`:180-212`) does use `{{$timestamp}}`, so each run creates a fresh restaurant; but the PUT tries to rename that fresh restaurant to the *same* fixed name every time, so as soon as one prior run succeeded the second run onwards will 409.
- **Identity with prior audits.** This is exactly the finding recorded as `Finding 1 (Medium)` in `audit-7b2fa61_Golf-Papa-Tango_team-lead-integration-readiness_1.md:145-158`. It was observed but deferred at `7b2fa61`; nothing in `7b2fa61..50b8e1d` addresses it. At `50b8e1d` it is now the **last open reliability issue on the Sierra-Lima Postman pack**, and it will arrive unfixed on the team lead's desk unless closed here.
- **Why this matters for *pre-team-integration*.** The Postman collection is the advertised demonstrator pack for CP#1/CP#2/CP#3 per `0010 §9` and the runbook. The team lead (or the instructor during checkpoint defence) will re-run this collection -- probably against a non-pristine Docker volume, because no one wipes `menu_db_data` / `restaurant_db_data` between demos. The collection will report "all green", but one of the 12 endpoints is actually failing silently. That is a worse state than an honest red, because nobody sees the problem until it is dissected.
- **Impact on readiness.** Not a runtime bug in the service itself: backend tests cover positive PUT exhaustively (`RestaurantControllerTest.update_returns200_whenOwnerPatchValid` and siblings), and the smoke scripts already create and mutate restaurants under unique per-run names. The issue is confined to the Postman asset.
- **Severity: Medium** (unchanged from the `7b2fa61` Golf-Papa-Tango classification). Blocks "the Postman pack is a reliable positive gate" claim, does not block the service itself.
- **Recommended fix.** Two minimal edits:
  1. Add `{{$timestamp}}` to the PUT body name so each run writes a unique name (matching the POST pattern on line 204).
  2. Add a collection-level `test` event on this request asserting `pm.response.to.have.status(200)` and echoing the new name back.

No other Postman request in the collection has the same static-name + no-assertion combination. Positive-path POSTs use `{{$timestamp}}` + assertion; negative-path requests assert the expected 4xx code; the admin-bypass PUT (`[200] PUT /menu-items/{id}  admin bypass (Phase 15)`) asserts `200` and `name is echoed`.

### No other code-level findings at `50b8e1d`

- The DELETE UI wiring in `MenuItemDetailView.vue` is correct end-to-end and fully backed by existing backend tests (§2.15-2.17). No new observation.
- All `7b2fa61` patches (strict `operatingHours` regex, duplicate-name 409 on rename, JWT issuer pinning, 405 on unsupported methods, menu ownership lookup hardening, frontend client-side validation alignment) are still live on the wire (§2.11-2.14).
- `restaurant-service` and `menu-service` `SecurityConfig` matrices unchanged; role-based matrix in `0010 §8` is still enforced end-to-end.
- `RestaurantOwnershipClient` still calls the public `GET /restaurants/{id}` (no token required); no change needed.
- Docker healthchecks for all six containers return healthy; compose rebuild is clean on a fresh target directory.
- Frontend lint + build pass with no warnings.

## 4. Explicit gaps or unverified assumptions

These items require the Group 7 integration branch, teammate
infrastructure, or a human reader. None is a runtime defect in
Sierra-Lima's slice.

| # | Gap | Why not closable here | Suggested remedy | Owner |
|---|---|---|---|---|
| 4.1 | True end-to-end W1 across teammate services (hops 1-3, 6-9) | `user-service`, `order-service`, `payment-service`, `delivery-service`, `notification-service` are not in this personal repo | Re-run on the Group 7 integration branch with the full stack up | Team lead / Sierra-Lima on integrated stack |
| 4.2 | JWT interoperability against the real User Service token issuer (Alfa-Kilo) | The local stack uses the dev HS256 secret + issuer `quickbite-user-service`; we do include wrong-issuer probes, but not against a real alternate issuer | Align on shared HS256 secret in `application-integration.yml` or accept Alfa-Kilo's signing key | Team lead / Alfa-Kilo |
| 4.3 | Broker-backed W2 / W3 end-to-end | Sierra-Lima is a non-participant per `0040 §1`; the only Sierra-Lima async surface is log-only | Observe during the integrated-stack rehearsal (Mike-Alfa's broker, Elephant-Yankee's producers) | Team lead / Mike-Alfa / Elephant-Yankee |
| 4.4 | Kafka transport for Sierra-Lima's optional `menu-events` producer | `0040 §2` commits to the one-class swap post-CP#3 if teammate async is not on-track by 2026-04-21 (gap analysis F1 recommendation) | Drop-in `KafkaMenuEventPublisher implements MenuEventPublisher` behind an env flag if needed after the 2026-04-21 sync | Sierra-Lima, conditional |
| 4.5 | Browser-level frontend interaction tests (manual click-through of the new DELETE button) | No headless browser run was performed this audit; lint + build + code review + live DELETE curl probes are the substitute | Click-test during CP#2 rehearsal | Sierra-Lima, pre-rehearsal |
| 4.6 | Destructive pristine-volume replay | Volumes were not wiped; the `PUT /restaurants/{id}` `409` described in Finding 1 is itself partially a symptom of this | Optional before rehearsal: `docker compose down -v && docker compose up -d --build`, then re-run smokes and Newman | Sierra-Lima, pre-rehearsal |

## 5. Final verdict (pre-fix)

**READY TO HAND OVER once Finding 1 is patched.** Sierra-Lima's
Assignment 3 scope at `50b8e1d` is complete, green across every
automated check (80/80 backend tests, lint, build, 6-container
compose health, both smoke scripts, Newman 68/68), and every prior
audit finding (`5a998ad` → `1a6e8c7` → `d23145f` → `bcc9dd0` →
`60fa710` audits 1-3 → `7b2fa61`) is closed or demonstrably still
patched on the wire. The only open issue is a demonstration-pack
brittleness in the Postman collection (§3 Finding 1) that was
observed but not fixed at `7b2fa61`; it is fully localised to one
request body and one missing assertion.

The DELETE menu-item UI landed in `50b8e1d` is well-formed, test-backed,
and live-probable (§2.9 / §2.15-2.17); it does not introduce any new
risk.

---

## Appendix A -- Reproduction commands

```bash
# From the repo root
cd services/restaurant-service && mvn test
cd ../menu-service             && mvn test

cd ../frontend/quickbite-frontend
npm ci
npm run lint -- --no-fix
npm run build

cd ../../local-dev
docker compose --profile dev-gateway up -d --build
docker ps                                  # expect 6 healthy
bash smoke.sh                              # OK -- Sierra-Lima smoke test passed.
bash smoke-cross-service.sh                # 0/0 failures; menu-events log captured

curl -sf http://localhost:8081/actuator/health
curl -sf http://localhost:8082/actuator/health
curl -sf http://localhost:8080/healthz     # dev-gateway

npx newman run postman/QuickBite.postman_collection.json \
               -e postman/QuickBite.postman_environment.json
```

## Appendix B -- Deltas vs. the prior audit chain

| Area | `bcc9dd0` Charlie-Lima-Alfa | `7b2fa61` Golf-Papa-Tango | **`50b8e1d` (this audit)** |
|---|---|---|---|
| Automated tests | 71 green (28 + 43) | 80 green (33 + 47) | **80 green (33 + 47)** |
| Newman | 66/66 assertions green | 68/68 assertions green | **68/68 assertions green (but see Finding 1)** |
| Compose stack | 6 healthy | 6 healthy | **6 healthy** |
| DELETE `/menu-items/{id}` | Backend only | Backend only | **Backend + UI button (owner/admin-gated)** |
| `405` on unsupported methods | Returns `500` (bcc9dd0 §Q3.7) | Returns `405` after `7b2fa61` patch | **Returns `405` -- confirmed live (auth'd); `401` if unauth as expected** |
| `operatingHours` strict regex | Live | Live | **Live -- `29:59-29:59` and `""` both 400** |
| Mixed-currency validate | 400 | 400 | **400 -- confirmed live with probe** |
| `Page<RestaurantResponse>` shape | 200 / paged | 200 / paged | **200 / paged** |
| Postman `PUT /restaurants/{id}` reliability | Green on pristine only | Flagged Medium (unfixed) | **Still flagged Medium -- §3 Finding 1** |
| Verdict | READY | READY with 2 caveats | **READY once Finding 1 is patched** |

## Appendix C -- Files inspected at `50b8e1d`

- `dev-docs/audits/audit-7b2fa61_Golf-Papa-Tango_team-lead-integration-readiness_1.md`
- `dev-docs/audits/audit-bcc9dd0_Charlie-Lima-Alfa_final-handover-readiness.md`
- `dev-docs/gap-analysis/gap-analysis-7b2fa61_Charlie-Lima-Alfa_project-brief-vs-repo.md`
- `dev-docs/decisions/0001-scope-freeze.md`, `0010-auth-contract.md`, `0020-sierra-lima-contracts.md`, `0030-workflow-w1.md`, `0040-phase-16-async-stance.md`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantController.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/config/SecurityConfig.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/controller/MenuController.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/config/SecurityConfig.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/exception/GlobalExceptionHandler.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/security/RestaurantOwnershipClient.java`
- `services/menu-service/src/test/java/ee/ut/esi/quickbite/menu/controller/MenuControllerTest.java`
- `services/frontend/quickbite-frontend/src/views/MenuItemDetailView.vue`
- `services/frontend/quickbite-frontend/src/router/index.js`, `src/api/client.js`, `src/auth/token.js`
- `services/local-dev/docker-compose.yml`, `smoke.sh`, `smoke-cross-service.sh`
- `services/local-dev/postman/QuickBite.postman_collection.json` + `.postman_environment.json`

---

## 6. Resolution (added post-audit, same session)

### Finding 1 -- resolved in this session.

- **File patched.** `services/local-dev/postman/QuickBite.postman_collection.json:225-260` (the `PUT /restaurants/{id}` item in the `Restaurant CRUD` folder).
- **Changes.**
  1. Replaced the hard-coded `"name": "Pizza Antonio (updated)"` with `"name": "Pizza Antonio (updated) {{$timestamp}}"` so every Newman/Postman run writes a unique name, eliminating the duplicate-name collision against prior-run rows on non-pristine volumes.
  2. Added a collection-level `test` script event asserting `pm.response.to.have.status(200)` and `body.name` matching `/^Pizza Antonio \(updated\) \d+$/`. The Newman suite now fails loudly if the PUT ever returns `409` again.
- **Post-fix verification (performed this session against the same live stack).**
  - Newman run 1: `requests 39, test-scripts 31, assertions 70, failures 0`. PUT line: `PUT http://localhost:8081/restaurants/8fb031d9-... [200 OK, 776B, 15ms]`.
  - Newman run 2 (repeated against the already-dirty volume to prove reliability): same result -- `[200 OK, 776B, 19ms]` on the PUT, assertions still `70/70`.
  - Newman run 3: same result -- `[200 OK, 776B, 13ms]` on the PUT, assertions still `70/70`.
  - Assertion count moved from the pre-fix `68` (silently masking the `409`) to `70` (both new asserts enforced).
- **No other file touched.** Backend code, frontend code, smoke scripts, decisions, and other Postman requests were not modified by this fix.
- **Regression check.** `bash services/local-dev/smoke.sh` and `bash services/local-dev/smoke-cross-service.sh` still exit `0`; the `menu-events` log line still captures cleanly; both backend `mvn test` suites remain `33/33` + `47/47` green (no backend change involved); frontend lint + build remain green (no frontend change involved).

### Updated verdict.

**READY TO HAND OVER.** All findings closed. At `50b8e1d` + the in-tree Postman patch landed this session:

- Backend tests: `80/80` green.
- Frontend lint + build: clean.
- Compose stack: six healthy containers.
- Smoke and cross-service smoke: both pass; `menu-events` evidence captured.
- Newman: `70/70` assertions green with the `PUT /restaurants/{id}` request now reliable across repeated runs.
- DELETE menu-item UI: end-to-end wired, role-gated, and live-probable.
- All prior audits' findings (`5a998ad` → `1a6e8c7` → `d23145f` → `bcc9dd0` → `60fa710` audits 1-3 → `7b2fa61` F1) now closed on Sierra-Lima's slice.
