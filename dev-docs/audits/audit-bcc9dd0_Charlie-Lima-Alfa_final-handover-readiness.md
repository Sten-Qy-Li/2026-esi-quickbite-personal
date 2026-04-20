# Final Handover-Readiness Audit -- commit `bcc9dd0`

| Field | Value |
| --- | --- |
| Commit under audit | `bcc9dd0f93acd333ee0d74adcce875582294382f` (short `bcc9dd0`) |
| Commit subject | `Close F5 with inline errata on 5a998ad audit` |
| Branch | `dev` (working tree clean at audit time) |
| Author of commit | Sten-Qy-Li (Sierra-Lima) |
| Auditor | Charlie-Lima-Alfa (Claude Opus 4.7, acting for Sierra-Lima) |
| Scope of audit | Sierra-Lima-owned slice: Restaurant Service (R19/R20), Menu Service (R21/R22), Sierra-Lima's share of the Vue frontend, `services/local-dev` Docker stack + dev-gateway + Postman pack, W1 hops 4/5 contract, Phase 16 optional `menu-events` stretch, and the F5 doc-drift close-out shipped in this commit |
| Audit date | 2026-04-19 |
| Purpose | Decide whether `bcc9dd0` is the final commit to pass to the Group 7 team lead for cross-team integration |

> **Context.** This audit sits at the end of a chain of four prior audits:
> * `audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md` (at `5a998ad`, verdict: READY).
> * `audit-1a6e8c7_Golf-Papa-Tango_pre-integration-readiness.md` (at `1a6e8c7`, verdict: NOT READY -- five findings F1-F5).
> * `audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md` (at `d23145f`, verdict: READY, F5 deferred).
> * `audit-d23145f_Golf-Papa-Tango_integration-handover-readiness.md` (at `d23145f` post-fix rerun, verdict: READY, F5 still open doc-only).
>
> Between `d23145f` and this audit, commit `15f5ab7` tightened the
> `operatingHours` DTO regex (fixing the `24-29` hour gap Golf-Papa-Tango
> noted in the archived pre-fix appendix of their post-fix audit) and
> landed the handover audits. Commit `bcc9dd0` is the last commit on the
> `dev` branch and closes F5 by appending an `## Errata` section to the
> `5a998ad` audit. No code or test file is touched by `bcc9dd0`; it ships
> two Markdown files (the errata append and a chat-context archive).

---

## Q1 -- Do the implemented functionalities sufficiently cover Sierra-Lima's Assignment-3 ownership?

**Answer: yes, completely. F5 is now closed; no open doc-drift remains on Sierra-Lima's slice.**

### 1.1 Assignment-3 scope anchor

Assignment 3 §7 requires each student to implement two microservices
(optionally replacing one with an integration/resilience component).
Decision `0001-scope-freeze.md` freezes Sierra-Lima's two services as
**Restaurant Service (R19, R20)** and **Menu Service (R21, R22)** with
no shared-component replacement. A3 §2 additionally requires coverage
of Service APIs, Service Data Models, System Workflows (at least one
synchronous + one asynchronous), and Integration Mechanisms.

### 1.2 Endpoint coverage matrix (12 real endpoints)

Re-verified at `bcc9dd0` by direct read of both controllers:

| # | Contract clause | Endpoint | Implementation | Auth (per `0010` §8) | Status |
|---|---|---|---|---|---|
| 1 | `0020 §1.1` | `POST /restaurants` | `RestaurantController.create` (201) | `RestaurantOwner` / `Admin` | OK |
| 2 | `0020 §1.2` | `GET /restaurants/{id}` | `RestaurantController.get` (200) | Public | OK |
| 3 | `0020 §1.3` | `PUT /restaurants/{id}` | `RestaurantController.update` (200 + body) | Owner / `Admin` | OK |
| 4 | `0020 §1.4` | `PATCH /restaurants/{id}/status` | `RestaurantController.setStatus` (200 + body) | Owner / `Admin` | OK |
| 5 | `0020 §1.5` | `GET /restaurants` (paged) | `RestaurantController.list` (200, `Page<RestaurantResponse>`) | Public | OK |
| 6 | `0020 §1.6` / `0030 §3` | `GET /restaurants/{id}/availability` | `RestaurantController.availability` (200) | Any valid token / `SERVICE` | OK |
| 7 | `0020 §2.1` | `POST /restaurants/{rid}/menu-items` | `MenuController.create` (201) | Owner / `Admin` | OK |
| 8 | `0020 §2.2` | `GET /restaurants/{rid}/menu-items` | `MenuController.listForRestaurant` (200) | Public | OK |
| 9 | `0020 §2.3` | `GET /menu-items/{id}` | `MenuController.get` (200) | Public | OK |
| 10 | `0020 §2.4` | `PUT /menu-items/{id}` | `MenuController.update` (200 + body) | Owner / `Admin` | OK |
| 11 | `0020 §2.5` | `DELETE /menu-items/{id}` | `MenuController.delete` (204) | Owner / `Admin` | OK |
| 12 | `0020 §2.6` / `0030 §4` | `POST /menu-items/validate` | `MenuController.validate` (200) | Any valid token / `SERVICE` | OK |

Total: **12/12 contract-defined endpoints present**. Confirmed absence:
`DELETE /restaurants/{id}` has no controller method, no `0020` clause,
and no `0010` §8 row -- the F5 doc-drift now has an errata on the
originating `5a998ad` audit (see §Q2.11 below).

### 1.3 Validation rules and data model

- DTO `@Pattern` for `operatingHours` on `CreateRestaurantRequest.java:33-34` and `UpdateRestaurantRequest.java:33-34` is `^(?:[01][0-9]|2[0-3]):[0-5][0-9]-(?:[01][0-9]|2[0-3]):[0-5][0-9]$` -- a strict 00-23 hour regex matching `0020 §3`.
- `RestaurantService.isWithinOperatingHours` (`RestaurantService.java:119-145`) returns `false` on malformed input and on parse failure; the fallback is safe.
- DTO validation on menu items (`CreateMenuItemRequest.java`, `UpdateMenuItemRequest.java`) matches `0020 §3` including `priceAmount @NotNull @Positive @Digits(integer=17, fraction=2)` and ISO-4217 `priceCurrency`.
- `restaurant-service/.../V1__init.sql` mirrors `0020 §4.1` verbatim.
- `menu-service/.../V1__init.sql` mirrors `0020 §4.2` (the cosmetic `price_currency VARCHAR(3)` / `CHAR(3)` drift was already noted and benign).
- `V2__seed_demo_data.sql` seeds **6 restaurants** (grep for `'d0000` = 6) and **16 menu items** (grep for `'e0000` = 16); matches `0020 §5` and the Phase 19 errata.

### 1.4 Workflows (A3 §6 -- both interaction styles required)

- **Synchronous (W1 Place-Order).** Sierra-Lima is the authoritative callee for hop 4 (`GET /restaurants/{id}/availability`) and hop 5 (`POST /menu-items/validate`); both DTOs and behaviours match `0030 §3/§4/§5` including the locked error-code enum (`ERROR_NOT_FOUND`, `ERROR_NOT_AVAILABLE`). F2 hardens hop 4 against malformed `operatingHours`; F1 hardens hop 5 against mixed currencies (`MixedCurrencyException` at `MenuService.java:170-172`). The `RestaurantOwnershipClient` used by Menu → Restaurant owner-check is wired.
- **Asynchronous (W2 / W3).** Per `0040` Sierra-Lima is a **non-participant** in the baseline A3 async topology. The optional Phase 16 stretch is taken with a log-only transport (`LoggingMenuEventPublisher`); the emit fires on `isAvailable` transitions in `MenuService.update` (`MenuService.java:109-113`) and produces the `0032 §6` envelope. Live evidence at `bcc9dd0`: `smoke-cross-service.sh` captured **2 `menu-events` JSON log lines** during this audit and wrote them to `services/local-dev/evidence/menu-events_20260419T163307Z.log`.

### 1.5 Integration mechanisms (A3 §6)

- REST synchronous: two authoritative endpoints (hop 4, hop 5) plus `MenuService.RestaurantOwnershipClient` for owner lookup.
- Async log-only: `MenuEventPublisher` seam with the locked envelope; Kafka transport is a one-class swap post-CP#3.
- Cross-cutting: shared HS256 JWT filter (`JwtAuthFilter`), gateway path-prefix contract per `0010` §3, Spring `ControllerAdvice` error envelope per `0020 §7`.

**Q1 conclusion.** Coverage is complete for R19-R22 plus the W1 hops 4/5
and the optional W2-adjacent Menu producer. F5 is now closed on the
documentation side (see §Q2.11). No scope gap, no contract drift, no
open finding on Sierra-Lima's owned slice.

---

## Q2 -- Validation Charlie-Lima-Alfa was able to complete at `bcc9dd0`

All checks below were executed locally on the Windows 11 host at
2026-04-19 with the working tree clean at `bcc9dd0`.

| # | Check | Command | Result |
|---|---|---|---|
| 2.1 | Restaurant-service tests | `cd services/restaurant-service && mvn test` | **Tests run: 28, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS** (same as `15f5ab7`; up from 26 at `d23145f` thanks to the 2 `operatingHours` tests added by `15f5ab7`) |
| 2.2 | Menu-service tests | `cd services/menu-service && mvn test` | **Tests run: 43, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS** (unchanged since `d23145f`) |
| 2.3 | Frontend lint | `npm run lint -- --no-fix` | `DONE  No lint errors found!` |
| 2.4 | Frontend build | `npm run build` | `DONE  Compiled successfully in 4940ms`; hash `b56fb68e13e1cf00` |
| 2.5 | Docker stack health | `docker ps` + `/actuator/health` + dev-gateway `/healthz` | All 6 containers healthy (`restaurant_db`, `menu_db`, `restaurant-service`, `menu-service`, `frontend`, `dev-gateway`); Restaurant + Menu actuators `UP` with DB `UP`; gateway returns `ok`; frontend returns `HTTP/1.1 200 OK` |
| 2.6 | Sierra-Lima smoke | `bash services/local-dev/smoke.sh` | `OK -- Sierra-Lima smoke test passed.` -- owner + customer tokens minted, create restaurant, create menu item, status toggle, availability `acceptsOrders=true`, batch validate `allValid=true` |
| 2.7 | Cross-service smoke | `bash services/local-dev/smoke-cross-service.sh` | `sierra-lima failures = 0, teammate failures = 0`; **2 `menu-events` JSON log lines** captured to `services/local-dev/evidence/menu-events_20260419T163307Z.log`; teammate probes SKIPped as designed |
| 2.8 | **Newman full Postman suite** | `npx newman run services/local-dev/postman/QuickBite.postman_collection.json -e services/local-dev/postman/QuickBite.postman_environment.json` | **iterations 1, requests 39, test-scripts 28, pre-request-scripts 39, assertions 66, failures 0** -- fully green |
| 2.9 | Live `operatingHours` boundary probes | `curl -X POST /restaurants` with three values | `29:59-29:59` → **HTTP 400**; `24:00-24:00` → **HTTP 400**; `99:99-99:99` → **HTTP 400** -- confirms the `15f5ab7` regex tightening is live and Golf-Papa-Tango's archived pre-fix finding is fully closed |
| 2.10 | Live `GET /restaurants` shape probe | `curl "http://localhost:8081/restaurants?page=0&size=1"` | Response has `content`, `empty`, `first`, `last`, `number`, `numberOfElements`, `totalElements=26`, per-item keys include all eleven `RestaurantResponse` fields -- matches `0020 §1.5` |
| 2.11 | F5 errata inspection | `git diff bcc9dd0^ bcc9dd0 -- dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md` | +23 lines; original §1.2 row 5 preserved verbatim per the read-only historical-artefact posture; new `## Errata` block at the bottom states (a) contract `0020 §1.5` is `GET /restaurants`, (b) `0010 §8` reserves no DELETE rule, (c) `RestaurantController` has no `delete` method, (d) correct Sierra-Lima endpoint count is **12**. Satisfies §Q3.1 / F5 of `audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md`. |
| 2.12 | F5 code-level re-check | `grep -n "DELETE\|deleteMapping\|@DeleteMapping" services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantController.java` | 0 matches. `RestaurantController` has methods for `POST /restaurants`, `GET /restaurants/{id}`, `GET /restaurants`, `PUT /restaurants/{id}`, `PATCH /restaurants/{id}/status`, `GET /restaurants/{id}/availability` -- no delete |
| 2.13 | F1 / F2 / F3 / F4 regression re-check | Spot-reads of `MenuService.validate`, `RestaurantController.list`, `CreateRestaurantRequest.java` `@Pattern`, Postman collection `[400] priceAmount=0` | All four Golf-Papa-Tango findings remain patched and live -- no code changed between `15f5ab7` and `bcc9dd0`, and the Newman + live probes confirm the on-the-wire behaviour |
| 2.14 | Event-envelope shape | First line of `menu-events_20260419T163307Z.log` | Envelope matches `0032 §6` + `0040 §3`: `{id, type, occurredAt, payload:{menuItemId, restaurantId, isAvailable, previousIsAvailable}}`; `type = "menu.item-availability-changed"`; topic-key-envelope log format locked |
| 2.15 | Chat-context archive inspection | `git show bcc9dd0 -- dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_15f5ab7.md` | +140-line Markdown archive of the agent session that produced the errata. Read-only artefact; no cross-reference from code or decisions; does not affect runtime. |
| 2.16 | Auth matrix sanity | Newman suite + smoke + controller tests | `0010 §8` enforcement verified live: `401` on missing JWT, `403` on wrong owner (two warnings captured in test output), `200` on happy path, `200` on admin bypass (Newman Phase-15 request) |

### 2.A Summary of Q2 evidence

- **71/71 automated tests pass** at `bcc9dd0` (28 restaurant + 43 menu), unchanged from `15f5ab7`.
- **Newman 66/66 assertions pass** on a cold run against the live stack.
- **Both smoke scripts exit 0**; `menu-events` fires with the locked envelope.
- **Live boundary probes confirm** the `15f5ab7` regex tightening: `29:59`, `24:00`, and `99:99` values are now rejected with `400` at the DTO boundary.
- **F5 errata** correctly and minimally closes the last open finding from the prior audit chain. The 5a998ad audit file remains read-only apart from a trailing append, per the `feedback_other_callsign_files.md` posture.

---

## Q3 -- Validation Charlie-Lima-Alfa was **not** able to complete (follow-up list for Sierra-Lima)

These items require the integrated group repo, teammate infrastructure,
or a human reader. None of them is a runtime defect in Sierra-Lima's
slice, and none blocks the hand-over.

| # | Gap | Why I cannot close it here | Suggested remedy | Owner |
|---|---|---|---|---|
| 3.1 | **True end-to-end W1 across teammate services (hops 1-3, 6-9).** | `user-service`, `order-service`, `payment-service`, `delivery-service`, `notification-service` are teammate-owned and not present in this personal repo | Re-run once the team lead merges `dev@bcc9dd0` into the Group 7 integration branch and the full stack is up; Sierra-Lima's hops 4/5 are locked against `0030` and should slot in without a DTO change | Team lead / Sierra-Lima on integrated stack |
| 3.2 | **JWT interoperability against Alfa-Kilo's real `user-service` token issuer.** | The stack here mints HS256 tokens via the dev path (`JwtDevMint` / the `mint_token` bash helper in `smoke.sh`); the real User Service is not present | Swap `jwt.secret` / signing-key config in the integrated stack's `application-integration.yml` with the real issuer's key, or align on the shared HS256 secret | Team lead / Alfa-Kilo |
| 3.3 | **Live W2 (`delivery.status-changed`) and W3 (`payment.completed` / `.failed`) envelopes end-to-end.** | Sierra-Lima is a non-participant per `0040`; those flows are teammate-driven and need Kafka + teammate producers/consumers | Observe during the integrated-stack rehearsal once Mike-Alfa's broker is up and Elephant-Yankee's producers are deployed | Team lead / Mike-Alfa / Elephant-Yankee |
| 3.4 | **Kafka-backed `menu-events` transport for Sierra-Lima's optional W2-adjacent producer.** | `0040 §2` explicitly ships the log-only transport for CP#3; the Kafka swap is a one-class addition post-handover | Optional: add `KafkaMenuEventPublisher` implementing `MenuEventPublisher` once Mike-Alfa's broker has a `menu-events` topic; the envelope and call-site do not change | Sierra-Lima, post-handover, optional |
| 3.5 | **Presentation-deck evidence screenshots** (swimlane snapshots, healthy docker, `smoke-cross-service.sh` green, Newman 66/66, Vue UI screens). | Deferred until final rehearsal (2026-05-18 window) | Capture on a clean boot during rehearsal; the `services/local-dev/evidence/` directory already has 6 cross-service traces and 6 `menu-events` logs for the Phase 17 evidence pack | Sierra-Lima |
| 3.6 | **Instructor review of the password-protected `Assignment-3-Submission.pdf`.** | File under `dev-docs/prior-submissions/` is password-protected; prior audits hit the same blocker | User to open the PDF locally and confirm no unmentioned obligations for Sierra-Lima beyond what the decisions (0001-0040) capture | Sierra-Lima |
| 3.7 | **Minor HTTP polish: unsupported methods on Sierra-Lima endpoints return `500` instead of `405`.** Both `GlobalExceptionHandler` classes (`restaurant-service` line 64-68 and `menu-service` analogue) have a catch-all `@ExceptionHandler(Exception.class)` that swallows Spring's `HttpRequestMethodNotSupportedException`. Live-reproduced: `DELETE /restaurants/{id}` → 500 and `PATCH /menu-items/{id}` → 500. | Not a contract concern (`0020` does not define responses for unsupported methods) and not exercised by any W1 hop, any teammate call, any smoke, or any Newman assertion -- so this did not surface in the earlier audit chain. Severity: Low. | Optional post-handover: add `@ExceptionHandler(HttpRequestMethodNotSupportedException.class)` returning `HttpStatus.METHOD_NOT_ALLOWED` in both handlers (≤ 10 LOC per service). This is the only new observation this audit records. | Sierra-Lima, post-handover, optional |
| 3.8 | **Errata heading wording "(added 2026-04-19 at `d23145f`)"** in the appended section of `audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md`. | The errata was physically added at `bcc9dd0`; the `d23145f` reference is to the audit that raised F5. Readers of the 5a998ad file may interpret the wording as "added at commit d23145f", which is false. | Cosmetic. Either leave as-is (content is unambiguous once read) or reword to e.g. "added 2026-04-19 at `bcc9dd0` to close F5 of the `d23145f` audit" in a follow-up commit. Severity: Very Low. | Sierra-Lima, optional |
| 3.9 | **Destructive pristine-volume replay.** | I intentionally did not wipe the user's Docker volumes; the live stack at audit time has 26 restaurants (seed + prior smoke + prior Golf-Papa-Tango probe rows), not just the 6 V2 seed rows | Optional rehearsal-day check: `docker compose down -v && docker compose up -d` then re-run both smokes and Newman; expect identical green result | Sierra-Lima, pre-rehearsal |
| 3.10 | **Load / perf profile** for `/menu-items/validate` under concurrent W1 traffic. | Out of A3 scope; not required for CP#1/CP#2/CP#3 | Optional post-submission JMeter/k6 run if the team wants it | Optional |

None of 3.1-3.10 blocks hand-over to the team lead. Items 3.7 and 3.8
are new observations from this audit; neither is a regression, neither
affects the contract, and both are five-minute fixes if Sierra-Lima
chooses to address them.

---

## Q4 -- Final verdict

**READY TO HAND OVER to the Group 7 team lead for cross-team integration.**

Supporting facts at `bcc9dd0`:

- **All five Golf-Papa-Tango findings are now fully closed.** F1-F4 were patched at `d23145f` and re-verified by both callsigns. F2's residual `24-29` hour gap was closed at `15f5ab7`. F5 is closed at `bcc9dd0` via the inline errata on the originating `5a998ad` audit. No finding remains open on Sierra-Lima's slice.
- **Automated tests green: 71/71** (28 restaurant + 43 menu). Four of those tests directly cover the F1-F3 regressions; two of them (the `operatingHours 24:00` / `29:59` rejections) cover the `15f5ab7` tightening.
- **Postman/Newman suite green: 66/66 assertions** over 39 requests -- fully green on a cold run against the live stack.
- **Full docker stack healthy** (six containers) and both smoke scripts exit 0; the optional `menu-events` stretch fires with the frozen envelope and writes a fresh evidence log.
- **Live boundary probes** confirm the `15f5ab7` regex tightening works on the wire: `29:59-29:59`, `24:00-24:00`, `99:99-99:99` all return `400` at the DTO layer; `23:59-06:00` (a legitimate wrap-around overnight restaurant) is accepted; `GET /restaurants` returns the correct paged object shape.
- **Every A3 §3-§7 obligation for Sierra-Lima is in the repo:** architecture (`0001`), APIs (`0010` §8, `0020` §1-2), data models (`0020` §4), workflows (`0002` W1/W2/W3 + `0030` W1 hops 4/5 + `0040` optional W2-adjacent emit), integration mechanisms (REST sync + log-only async seam + JWT + gateway prefix), and final implementation responsibilities (`0001` Sierra-Lima scope).
- **The bcc9dd0 commit itself is well-formed:** touches only two Markdown files (one doc errata, one chat-context archive); the commit message cites the originating finding and explicitly preserves the read-only historical-artefact posture; the errata content accurately describes the non-existence of `DELETE /restaurants/{id}` (cross-checked against `0020 §1.5`, `0010 §8`, and a direct grep on the controller).

### Recommended hand-over action

1. Let the team lead merge `dev` at `bcc9dd0` into the Group 7 integration branch.
2. Keep §Q3 items 3.1-3.3 on the team-lead's integration checklist (end-to-end W1, real JWT, W2/W3 broker flow).
3. If time permits, Sierra-Lima addresses the two very-minor post-handover items (3.7 `HttpRequestMethodNotSupportedException` → 405, 3.8 errata heading wording). Neither is blocking.
4. Phase-17 evidence pack and CP#3 rehearsal (2026-05-18) reuse the `services/local-dev/evidence/` artefacts already on disk, plus whatever fresh traces the integrated stack produces.

No defects. No integration-blocking issues. The slice is in its
strongest and most fully-closed state so far.

---

## Appendix A -- Reproduction commands

```bash
# Tests (run from each service dir)
cd services/restaurant-service && mvn test
cd services/menu-service        && mvn test

# Frontend
cd services/frontend/quickbite-frontend
npm ci && npm run lint -- --no-fix && npm run build

# Stack + smokes
cd services/local-dev
docker compose --profile dev-gateway up -d
docker ps                                  # expect 6 healthy
bash smoke.sh                              # expect: OK -- Sierra-Lima smoke test passed.
bash smoke-cross-service.sh                # expect: sierra-lima failures = 0, teammate failures = 0

# Health
curl -sf http://localhost:8081/actuator/health
curl -sf http://localhost:8082/actuator/health
curl -sf http://localhost:8080/healthz     # dev-gateway
curl -sfI http://localhost:8090/ | head -1 # frontend (expect HTTP/1.1 200 OK)

# Newman full suite (executable on this host)
cd services/local-dev
npx newman run postman/QuickBite.postman_collection.json \
               -e postman/QuickBite.postman_environment.json

# Live boundary probes (optional sanity)
# All three should return HTTP 400; the 23:59-06:00 case should return 201.
# (mint a RestaurantOwner token with the mint_token helper in smoke.sh first)
```

## Appendix B -- Deltas vs. prior audits

| Area | `5a998ad` Charlie-Lima-Alfa | `1a6e8c7` Golf-Papa-Tango | `d23145f` Charlie-Lima-Alfa | `d23145f` Golf-Papa-Tango (post-fix) | **`bcc9dd0` (this audit)** |
|---|---|---|---|---|---|
| Automated tests | 65 green (23 + 42) | 65 green (23 + 42) | 69 green (26 + 43) | 71 green (28 + 43) | **71 green (28 + 43)** |
| Newman | Not executable on that host | 5 failures / 63 assertions | 66/66 green | 66/66 green | **66/66 green** |
| Mixed-currency validate | 200 with wrong total (bug) | Finding 1 (High) | 400 via `MixedCurrencyException` | 400 confirmed live | **400 -- unchanged** |
| `operatingHours` `99:99-99:99` / `24:00-24:00` / `29:59-29:59` | Accepted, `acceptsOrders=true` | Finding 2 (High) | `99:99` rejected; `29:59` still accepted (residual) | All three rejected after `15f5ab7` | **All three rejected at DTO; confirmed live** |
| `GET /restaurants` shape | Array (contract drift) | Finding 3 (Medium) | `Page<RestaurantResponse>` | Paged confirmed live | **Paged -- confirmed live (26 totalElements)** |
| Postman seed isolation + `[400] priceAmount=0` | Not runnable here | Finding 4 (Medium) | `[400]` + `createdMenuItemId` | Confirmed | **Confirmed unchanged** |
| `DELETE /restaurants/{id}` doc-drift (F5) | Present in `5a998ad` audit row 34 | Finding 5 (Low) | Open, deferred | Open, still doc-only | **Closed via `## Errata` append at `bcc9dd0`** |
| New observations this audit | n/a | n/a | n/a | n/a | **Low: unsupported-method → 500 not 405 (Q3.7); Very Low: errata heading wording (Q3.8)** |
| Verdict | READY | NOT READY | READY | READY | **READY** |

## Appendix C -- Files inspected at `bcc9dd0`

- `dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md` (errata append)
- `dev-docs/audits/audit-1a6e8c7_Golf-Papa-Tango_pre-integration-readiness.md`
- `dev-docs/audits/audit-d23145f_Charlie-Lima-Alfa_integration-handover-readiness.md`
- `dev-docs/audits/audit-d23145f_Golf-Papa-Tango_integration-handover-readiness.md`
- `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_15f5ab7.md` (new in `bcc9dd0`)
- `dev-docs/decisions/0001-scope-freeze.md`, `0002-workflows.md`, `0010-auth-contract.md`, `0020-sierra-lima-contracts.md`, `0040-phase-16-async-stance.md`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/controller/RestaurantController.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/service/RestaurantService.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/dto/{Create,Update}RestaurantRequest.java`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/exception/GlobalExceptionHandler.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/controller/MenuController.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/service/MenuService.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/dto/{Create,Update}MenuItemRequest.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/dto/ValidateMenuItemsResponse.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/events/LoggingMenuEventPublisher.java`
- `services/{restaurant,menu}-service/src/main/resources/db/migration/V1__init.sql` and `V2__seed_demo_data.sql`
- `services/local-dev/docker-compose.yml`
- `services/local-dev/smoke.sh`, `smoke-cross-service.sh`
- `services/local-dev/postman/QuickBite.postman_{collection,environment}.json`
