# Pre-Integration Readiness Audit — commit `1a6e8c7`

| Field | Value |
| --- | --- |
| Commit under audit | `1a6e8c7c246c58d8ecb56d7612ba68d2fea8be70` (short `1a6e8c7`) |
| Commit subject | `Deliver pre-integration audit at 5a998ad and align contract + DTOs` |
| Auditor | Golf-Papa-Tango |
| Branch | `dev` |
| Audit date | 2026-04-19 |
| Scope | Sierra-Lima-owned slice: Restaurant Service, Menu Service, Sierra-Lima frontend surfaces, local-dev stack, and Sierra-Lima's W1 responsibilities |

> Note on the user's Q2 wording: commit `1a6e8c7` does **not** contain `dev-docs/audits/audit-1a6e8c7_Charlie-Lima-Alfa_pre-integration-readiness.md`. It contains `dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md`, introduced by commit `1a6e8c7` on 2026-04-19. In Q2 below, "the Charlie-Lima-Alfa audit" refers to that file.

## Findings

### 1. High — `POST /menu-items/validate` accepts mixed-currency baskets and returns a nonsense total

- Contract `0020` says mixed currencies must fail the whole request with `400` (`dev-docs/decisions/0020-sierra-lima-contracts.md:266-267`).
- The current implementation simply sums every valid line and picks the first non-null currency (`services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java:191-201`).
- Live verification on 2026-04-19:
  - created one `EUR` item and one `USD` item under the same restaurant;
  - `POST /menu-items/validate` returned `200`;
  - body reported `"allValid": true`, `"totalAmount": 12.00`, `"currency": "EUR"`.
- Impact: Order Service can receive an invalid total/currency pair and accept a basket the contract says must be rejected.

### 2. High — restaurant operating-hours validation accepts impossible times and availability falls back to `acceptsOrders=true`

- Contract `0020` freezes `operatingHours` as a real `HH:MM-HH:MM` value (`dev-docs/decisions/0020-sierra-lima-contracts.md:280`).
- The DTO regex only checks digit placement, so values such as `99:99-99:99` pass bean validation (`services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/CreateRestaurantRequest.java:33-35`, same issue in `UpdateRestaurantRequest.java:33-35`).
- `RestaurantService.isWithinOperatingHours()` returns `true` on malformed input and on parse failure (`services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/service/RestaurantService.java:120-138`).
- Live verification on 2026-04-19:
  - `POST /restaurants` with `operatingHours: "99:99-99:99"` returned `201`;
  - after opening that restaurant, `GET /restaurants/{id}/availability` returned `acceptsOrders: true`.
- Impact: W1 hop 4 can falsely admit orders for malformed restaurant data.

### 3. Medium — restaurant browse endpoint does not match the frozen contract

- Contract `0020` documents `GET /restaurants` as a paged response with `page`, `size`, `sort`, and page metadata (`dev-docs/decisions/0020-sierra-lima-contracts.md:91-115`).
- The controller only accepts `city` and `isOpen` and returns `List<RestaurantResponse>` (`services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantController.java:68-76`).
- The Vue list view is coupled to an array response (`services/frontend/quickbite-frontend/src/views/RestaurantListView.vue:93`).
- Live verification on 2026-04-19:
  - `GET /restaurants?page=1&size=1` returned `200`;
  - body type was a JSON array, not the documented page object.
- Impact: any teammate or team lead integrating against the documented response shape will be surprised by the live API.

### 4. Medium — the checked-in Newman/Postman suite is runnable here, but it is not green

- The Charlie-Lima-Alfa audit said Newman was not executable in that environment (`dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md:99`).
- In this environment, `npx newman` is available and I executed the shipped collection `services/local-dev/postman/QuickBite.postman_collection.json`.
- Result on 2026-04-19: `39` requests executed, `63` assertions executed, `5` assertion failures.
- Causes:
  - the collection still expects the old `422` response for zero-price menu-item creation (`services/local-dev/postman/QuickBite.postman_collection.json:881-888`);
  - the collection deletes `{{menuItemId}}` in the CRUD section (`services/local-dev/postman/QuickBite.postman_collection.json:334-341`) and later reuses the same seed fixture in Phase 15 negative-auth/admin-bypass requests (`services/local-dev/postman/QuickBite.postman_collection.json:974`, `1004`, `1060`).
- Impact: the validation pack itself is not integration-ready and currently produces false negatives.

### 5. Low — the existing Charlie-Lima-Alfa audit overstates what exists in the repo

- It claims `DELETE /restaurants/{id}` exists (`dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md:34`), but the current frozen contract does not define that endpoint and the controller does not implement it.
- Impact: this is an audit accuracy problem, not a runtime defect, but it matters when answering Q2.

## Q1 — Do the implemented functionalities sufficiently cover Sierra-Lima's Assignment-3 ownership slice?

**Answer: partially on breadth, no on sufficiency/readiness.**

What is covered:

- `0001` freezes Sierra-Lima to `Restaurant Service` (`R19`, `R20`) and `Menu Service` (`R21`, `R22`) (`dev-docs/decisions/0001-scope-freeze.md:45-53`).
- `0002` freezes Sierra-Lima's W1 responsibilities to hop 4 `GET /restaurants/{id}/availability` and hop 5 `POST /menu-items/validate` (`dev-docs/decisions/0002-workflows.md:23-30`).
- The repo does implement the expected Restaurant and Menu service surfaces, local-dev Docker stack, Vue screens for browse/manage flows, and the optional menu-events producer.

Why I still answer **no** for sufficiency:

- W1 hop 5 is contract-incorrect for mixed currencies.
- W1 hop 4 can produce false positives for malformed operating hours.
- Restaurant browse no longer matches the frozen response contract.
- The checked-in validation pack is not green.

So the repo broadly covers the Sierra-Lima slice, but not cleanly enough to be considered pre-integration-ready.

## Q2 — What validation from the Charlie-Lima-Alfa audit am I able to complete?

Interpreted against `dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md`.

| Charlie-Lima-Alfa item | Golf-Papa-Tango status | Result on 2026-04-19 |
| --- | --- | --- |
| `2.1` Restaurant tests | Completed | `mvn test` green, `23/23` tests passed |
| `2.2` Menu tests | Completed | `mvn test` green, `42/42` tests passed |
| `2.3` Frontend build + lint | Completed | `npm run build` green, `npm run lint -- --no-fix` green |
| `2.4` Docker Compose stack up | Completed | `docker compose up -d` and `docker compose ps` showed all 6 containers healthy |
| `2.5` Sierra-Lima smoke | Completed | `bash services/local-dev/smoke.sh` exited `0` |
| `2.6` Cross-service smoke | Completed for Sierra-Lima-owned scope | `bash services/local-dev/smoke-cross-service.sh` exited `0`; teammate probes skipped as designed; 2 live `menu-events` lines observed |
| `2.7` Health endpoints | Completed | Restaurant `UP`, Menu `UP`, frontend returned `HTTP 200` |
| `2.8` Auth matrix spot-checks | Completed | Covered via smoke flows, Newman negative-auth checks, and service/controller tests |
| `2.9` Contract shape spot-checks | Completed, with failures found | Happy paths verified, but I found the mixed-currency and browse-shape mismatches |
| `2.10` Event-envelope shape | Completed | Structured `menu-events` lines observed in live `docker logs quickbite-menu-service` |
| `2.11` Flyway migrations | Completed to the extent observable locally | Services booted healthy against empty compose DBs, seeded IDs resolved, and Flyway startup lines were present in service logs |
| `2.12` Static review | Completed | Produced the findings in this report |

Two important corrections to the Charlie-Lima-Alfa audit:

- its coverage table includes a non-existent `DELETE /restaurants/{id}` row;
- its "Newman not executable here" statement is not true in this environment.

## Q3 — What additional validation, not listed in the Charlie-Lima-Alfa audit, was I able to complete?

1. **Full Newman run of the shipped Postman collection.**
   Result: executable here, but not green: `39` requests, `63` assertions, `5` failures.

2. **Targeted live probe for mixed-currency validation.**
   Result: confirmed the contract violation described in Finding 1.

3. **Targeted live probe for malformed operating hours.**
   Result: confirmed both acceptance on create and false-positive `acceptsOrders=true` on availability.

4. **Targeted live probe for restaurant list pagination/shape.**
   Result: confirmed `GET /restaurants?page=1&size=1` still returns a plain array and ignores the documented page contract.

5. **Cross-check of the repo-local scope documents against Assignment-3-derived artefacts.**
   Result: `0001` and `0002` are internally consistent about Sierra-Lima owning Restaurant, Menu, and W1 hops 4/5.

## Q4 — What validation was I not able to complete?

1. **True end-to-end W1 across teammate services.**
   I do not have User, Order, Payment, and Delivery services in this repo; only Sierra-Lima's side and the local gateway are runnable here.

2. **Real W2 / W3 event flow with teammate producers/consumers.**
   Sierra-Lima's optional `menu-events` producer is visible, but the teammate-owned async chain is not locally present.

3. **JWT interoperability against Alfa-Kilo's real token issuer.**
   All successful auth checks here used the dev HS256 minting path, not a live User Service issuer.

4. **Staging-like cross-team contract verification on the team lead's merged branch.**
   This repo can only validate Sierra-Lima's standalone slice plus gateway exposure.

5. **A literal comparison against `audit-1a6e8c7_Charlie-Lima-Alfa_pre-integration-readiness.md`.**
   That file does not exist at commit `1a6e8c7`.

## Q5 — Final verdict on readiness for handover to the team lead

**Verdict: NOT READY TO HAND OVER AS-IS.**

I would want the following fixed before calling this integration-ready:

1. Reject mixed-currency `POST /menu-items/validate` requests with `400`, per `0020`.
2. Tighten `operatingHours` validation to real times and stop treating parse failures as `acceptsOrders=true`.
3. Resolve the `GET /restaurants` contract drift:
   - either implement the documented paged response and query params;
   - or explicitly supersede `0020` and align frontend/tests/docs to the array response.
4. Repair the Newman/Postman collection so it runs green again:
   - update the zero-price expectation from `422` to `400`;
   - stop deleting a seed fixture that later requests depend on, or generate an isolated fixture per request group.
5. Refresh the Charlie-Lima-Alfa audit so it no longer claims a non-existent restaurant delete endpoint.

If those items are closed and the core suite is rerun green, the repo should be in a much stronger state for team-lead integration.

## Commands executed

```powershell
mvn test
npm run build
npm run lint -- --no-fix
docker compose up -d
docker compose ps
bash services/local-dev/smoke.sh
bash services/local-dev/smoke-cross-service.sh
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl -I http://localhost:8090
npx newman run services/local-dev/postman/QuickBite.postman_collection.json -e services/local-dev/postman/QuickBite.postman_environment.json
```
