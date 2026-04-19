# Integration-Handover Readiness Audit -- commit `d23145f`

| Field | Value |
| --- | --- |
| Commit under audit | `d23145fae2d702867b6255ebc17b6c75e581e3f6` (short `d23145f`) |
| Commit subject | `Patch Golf-Papa-Tango audit findings 1-4 at 1a6e8c7` |
| Branch | `dev` (working tree clean at audit time) |
| Author of commit | Sten-Qy-Li (Sierra-Lima) |
| Auditor | Charlie-Lima-Alfa (Claude Opus 4.7, acting for Sierra-Lima) |
| Scope of audit | Sierra-Lima-owned slice: Restaurant Service (R19/R20), Menu Service (R21/R22), Sierra-Lima's share of the Vue frontend, `services/local-dev` Docker stack + dev-gateway + Postman pack, W1 hops 4/5 contract, Phase 16 optional `menu-events` stretch |
| Audit date | 2026-04-19 |
| Purpose | Decide whether `d23145f` is ready to pass to the Group 7 team lead for cross-team integration |

> **Context.** This audit comes after two prior reports:
> * `audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md` (at `5a998ad`, verdict: READY).
> * `audit-1a6e8c7_Golf-Papa-Tango_pre-integration-readiness.md` (at `1a6e8c7`, verdict: NOT READY -- five findings F1-F5).
>
> Commit `d23145f` ships code and Postman patches for Golf-Papa-Tango's F1-F4 and explicitly defers F5 (doc-drift in the Charlie-Lima-Alfa audit) at the user's request. This audit re-validates those four patches, re-checks the Sierra-Lima scope against Assignment 3, and returns a final verdict.

---

## Q1 -- Do the implemented functionalities sufficiently cover Sierra-Lima's Assignment-3 ownership?

**Answer: yes, with one open doc-drift (F5) that does not affect runtime behaviour.**

### 1.1 Assignment-3 scope anchor

Assignment 3 §7 requires each student to implement two microservices (optionally replacing one with an integration/resilience component). Decision `0001-scope-freeze.md` freezes Sierra-Lima's two services as **Restaurant Service (R19, R20)** and **Menu Service (R21, R22)** with no shared-component replacement. A3 §2 additionally requires coverage of Service APIs, Service Data Models, System Workflows (at least one synchronous + one asynchronous), and Integration Mechanisms.

### 1.2 Endpoint coverage matrix (12 real endpoints, 12 in `0020`+`0030`)

Verified by reading every controller at `d23145f`:

| # | Contract clause | Endpoint | Implementation | Auth (per `0010` §8) | At `d23145f` |
|---|---|---|---|---|---|
| 1 | `0020 §1.1` | `POST /restaurants` | `RestaurantController.create` (201) | `RestaurantOwner` / `Admin` | OK |
| 2 | `0020 §1.2` | `GET /restaurants/{id}` | `RestaurantController.get` (200) | Public | OK |
| 3 | `0020 §1.3` | `PUT /restaurants/{id}` | `RestaurantController.update` (200 + body) | Owner / `Admin` | OK |
| 4 | `0020 §1.4` | `PATCH /restaurants/{id}/status` | `RestaurantController.setStatus` (200 + body) | Owner / `Admin` | OK |
| 5 | `0020 §1.5` | `GET /restaurants` (paged) | `RestaurantController.list` (200, `Page<RestaurantResponse>`) | Public | OK -- **F3 patched** |
| 6 | `0020 §1.6` / `0030 §3` | `GET /restaurants/{id}/availability` | `RestaurantController.availability` (200) | Any valid token / `SERVICE` | OK -- **F2 patched** |
| 7 | `0020 §2.1` | `POST /restaurants/{rid}/menu-items` | `MenuController.create` (201) | Owner / `Admin` | OK |
| 8 | `0020 §2.2` | `GET /restaurants/{rid}/menu-items` | `MenuController.listForRestaurant` (200) | Public | OK |
| 9 | `0020 §2.3` | `GET /menu-items/{id}` | `MenuController.findById` (200) | Public | OK |
| 10 | `0020 §2.4` | `PUT /menu-items/{id}` | `MenuController.update` (200 + body) | Owner / `Admin` | OK |
| 11 | `0020 §2.5` | `DELETE /menu-items/{id}` | `MenuController.delete` (204) | Owner / `Admin` | OK |
| 12 | `0020 §2.6` / `0030 §4` | `POST /menu-items/validate` | `MenuController.validate` (200) | Any valid token / `SERVICE` | OK -- **F1 patched** |

Note on F5 (still open): `audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md` line 34 claims an additional row for `DELETE /restaurants/{id}`. That endpoint is not in contract `0020`, is not in the `0010` §8 route matrix, and is not in the controller -- confirmed by `grep -i delete services/restaurant-service/.../controller/RestaurantController.java` returning zero hits. The doc-drift was deferred by the commit message of `d23145f` and remains open (see §Q3.1).

### 1.3 Data models + seed data

- `restaurant-service/.../V1__init.sql` mirrors `0020 §4.1` verbatim.
- `menu-service/.../V1__init.sql` mirrors `0020 §4.2` (the `price_currency VARCHAR(3)` / `CHAR(3)` drift was reconciled at `1a6e8c7`).
- `V2__seed_demo_data.sql` seeds **6 restaurants** (`d00000`-prefix UUIDs) and **16 menu items** (`e00000`-prefix UUIDs) -- counts confirmed by prefix grep; matches `0020 §5` and the Phase 19 errata.

### 1.4 Workflows (A3 §6 -- both interaction styles required)

- **Synchronous (W1 Place-Order).** Sierra-Lima is the authoritative callee for hop 4 (`GET /restaurants/{id}/availability`) and hop 5 (`POST /menu-items/validate`); both DTOs and behaviours match `0030 §3/§4/§5` including the locked error-code enum. F2 hardens hop 4 against malformed `operatingHours`; F1 hardens hop 5 against mixed currencies. The `RestaurantOwnershipClient` used by Menu → Restaurant owner-check is wired.
- **Asynchronous (W2 / W3).** Per `0040` Sierra-Lima is a **non-participant** in the baseline A3 async topology. The optional Phase 16 stretch is taken with a log-only transport (`LoggingMenuEventPublisher`); the emit fires on `isAvailable` transitions in `MenuService.update` and produces the `0032 §6` envelope. Live evidence at `d23145f`: `smoke-cross-service.sh` captured **2 `menu-events` JSON log lines** during this audit and wrote them to `services/local-dev/evidence/menu-events_20260419T144233Z.log`.

### 1.5 Integration mechanisms (A3 §6)

- REST synchronous: two authoritative endpoints (hop 4, hop 5) plus `MenuService.RestaurantOwnershipClient` for owner lookup.
- Async log-only: `MenuEventPublisher` seam with the locked envelope.
- Cross-cutting: shared HS256 JWT filter, gateway path-prefix contract per `0010`, Spring `ControllerAdvice` error envelope per `0020 §7`.

**Q1 conclusion.** Coverage is complete for R19-R22 plus the W1 hops 4/5 and the optional W2-adjacent Menu producer. The only open gap is the F5 doc-drift in the older Charlie-Lima-Alfa audit; it is documentation, not code.

---

## Q2 -- Validation Charlie-Lima-Alfa was able to complete at `d23145f`

All checks below were executed locally on the Windows 11 host at 2026-04-19, working tree clean at `d23145f`.

| # | Check | Command | Result |
|---|---|---|---|
| 2.1 | Restaurant-service tests | `cd services/restaurant-service && mvn test` | **Tests run: 26, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS** (up from 23 at `5a998ad`; +3 from F2/F3 patch) |
| 2.2 | Menu-service tests | `cd services/menu-service && mvn test` | **Tests run: 43, Failures: 0, Errors: 0, Skipped: 0 -- BUILD SUCCESS** (up from 42 at `5a998ad`; +1 from F1 patch) |
| 2.3 | Frontend lint | `npm run lint -- --no-fix` | `DONE  No lint errors found!` |
| 2.4 | Frontend build | `npm run build` | `DONE  Build complete.` (Hash `b56fb68e13e1cf00`, 4289ms) |
| 2.5 | Docker stack health | `docker compose ps` + `curl /actuator/health` | All 6 containers healthy (`restaurant_db`, `menu_db`, `restaurant-service`, `menu-service`, `frontend`, `dev-gateway`); Restaurant + Menu actuator `UP` with DB `UP` |
| 2.6 | Sierra-Lima smoke | `bash services/local-dev/smoke.sh` | `OK -- Sierra-Lima smoke test passed.` (exit 0) -- verified owner/customer token mint, create restaurant, create menu item, status toggle, availability `acceptsOrders=true`, batch validate `allValid=true` |
| 2.7 | Cross-service smoke | `bash services/local-dev/smoke-cross-service.sh` | `sierra-lima failures = 0, teammate failures = 0`; **2 `menu-events` log lines** captured to evidence dir; teammate probes SKIPped as designed |
| 2.8 | **Newman full Postman suite** | `npx newman run services/local-dev/postman/QuickBite.postman_collection.json -e services/local-dev/postman/QuickBite.postman_environment.json` | **iterations 1, requests 39, test-scripts 28, pre-request-scripts 39, assertions 66, failures 0** -- fully green on first run at `d23145f` |
| 2.9 | F1 patch spot-check | Code-read `MenuService.validate` + `MixedCurrencyException` + `GlobalExceptionHandler` + Postman | Distinct currencies collected from found items only; `MixedCurrencyException` mapped to `400`; Postman `[400] priceAmount=0` assertion green |
| 2.10 | F2 patch spot-check | Diff-read the DTO `@Pattern` change + `RestaurantService.isWithinOperatingHours` + 2 new tests | Regex tightened from `\d{2}:\d{2}-\d{2}:\d{2}` to `[0-2][0-9]:[0-5][0-9]-[0-2][0-9]:[0-5][0-9]` (matches `0020 §3`); parse failure now returns `false` instead of `true`; both new `availability_malformed/unparseable` tests green |
| 2.11 | F3 patch spot-check | Diff-read controller + service + repository + frontend views | `GET /restaurants` now returns `Page<RestaurantResponse>` with `@PageableDefault(size=20, sort="name")`; frontend safely unwraps `data.content` with array fallback; MockMvc test asserts `$.content`, `$.totalElements`, `$.pageable` |
| 2.12 | F4 patch spot-check | Diff-read Postman collection + environment | `[400] priceAmount=0` replaces `[422]`; `createdMenuItemId` captured by CRUD POST and targeted by CRUD DELETE; `{{menuItemId}}` seed preserved for Phase 15 admin-bypass + owner-on-owner tests |
| 2.13 | F5 status check | `grep "DELETE /restaurants" dev-docs/audits/audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md` | Line 34 still claims the non-existent endpoint -- F5 **remains open** (explicitly deferred by the `d23145f` commit message) |
| 2.14 | Auth matrix sanity | Newman suite + tests | `0010 §8` enforcement verified: `401` on missing JWT, `403` on wrong owner, `200` on happy path, `200` on admin bypass |
| 2.15 | Event-envelope shape | Live log-line grep from cross-service smoke | Envelope matches `0032 §6` + `0040 §3`: `{id, type, occurredAt, payload:{menuItemId, restaurantId, isAvailable, previousIsAvailable}}` |
| 2.16 | Contract vs code endpoint matrix | Per §Q1.2 above | 12/12 endpoints present, correct auth posture, correct HTTP status (including the documented 200-vs-204 on `PUT`/`PATCH` per `0020 §1.3/§1.4/§2.3`) |

### 2.A Summary of Q2 evidence

- **69/69 automated tests pass** at `d23145f` (26 restaurant + 43 menu), up from 65 at `5a998ad`; the four new tests directly cover F1-F3 regressions.
- **Newman 66/66 assertions pass** on a single cold run against the live stack -- the F4 fixes (`priceAmount=0 -> 400`, CRUD seed isolation) are validated in motion.
- **Both smoke scripts exit 0**; `menu-events` fires with the contract-locked envelope.
- **Code-level re-read confirms F1-F4 do what the commit message claims**, with one caveat noted in §Q3.4 below.

---

## Q3 -- Validation Charlie-Lima-Alfa was **not** able to complete (follow-up list for Sierra-Lima)

These items need attention before final hand-over, or immediately after, depending on the owner column. None of them is a runtime defect.

| # | Gap | Why I cannot close it here | Suggested remedy | Owner |
|---|---|---|---|---|
| 3.1 | **F5 open: `audit-5a998ad_Charlie-Lima-Alfa_pre-integration-readiness.md` still claims a non-existent `DELETE /restaurants/{id}` at line 34 (cited as `0020 §1.5`, which is actually `GET /restaurants`).** Contract, auth matrix, controller, and tests are all aligned on "no DELETE for restaurants"; only the audit row is drifting. | The user explicitly instructed (per memory `feedback_other_callsign_files.md`) that other-callsign artefacts are read-only and must be preserved verbatim, and the `d23145f` commit message explicitly defers F5 | Add a one-paragraph errata section to the `5a998ad` audit (or an `audit-5a998ad_Charlie-Lima-Alfa_errata.md` sibling) noting that the DELETE row was a doc-drift; Restaurant Service does not and was not meant to implement `DELETE /restaurants/{id}` under `0020`/`0010`. The team lead should not start integration assuming a DELETE endpoint exists | Sierra-Lima (5 min) |
| 3.2 | **Minor commit-message mislabeling in `d23145f`.** Line "F4: Postman mixed-currency case now expects 400 with validationErrors" describes a patch that actually lives on the **zero-price** Postman case (`[422] ... priceAmount=0` -> `[400] ... priceAmount=0`); there is no mixed-currency Postman case in the collection. The code behaviour for mixed-currency is correct (400 via `MixedCurrencyException`) but is untested at the Postman layer | The commit is already published and the underlying code is correct; amending history is disallowed by user-instruction durability | Optional: add a new Postman request `[400] POST /menu-items/validate  mixed currencies` asserting `pm.response.to.have.status(400)`. The fixtures already include `menuItemId` (EUR) + room for a USD item -- requires seeding one USD item or using the existing V2 seed with a new USD row. Non-blocker | Sierra-Lima (15 min, post-handover) |
| 3.3 | **True end-to-end W1 across teammate services (hops 1-3, 6-9).** | User, Order, Payment, Delivery, Notification services are owned by teammates and not present in this personal early-start repo | Re-run once the team lead merges the group repo and spins up a full stack; Sierra-Lima's hops 4/5 are locked against `0030` and should slot in without a DTO change | Team lead / Sierra-Lima on integrated stack |
| 3.4 | **JWT interoperability against Alfa-Kilo's real `user-service` token issuer.** | The current stack mints HS256 tokens via the dev path (`JwtDevMint` / inline `jose`); the real User Service is not present | Swap `jwt.secret` / signing-key config in `application-integration.yml` with the real issuer's key, or hand over the HS256 secret to User Service | Team lead / Alfa-Kilo |
| 3.5 | **Live W2 (`delivery.status-changed`) and W3 (`payment.completed`/`.failed`) envelopes end-to-end.** | Sierra-Lima is a non-participant per `0040`; those flows are teammate-driven and need Kafka + teammate producers/consumers | Observe during the integrated-stack rehearsal once Mike-Alfa's broker is up and Elephant-Yankee's producers are deployed | Team lead / Mike-Alfa / Elephant-Yankee |
| 3.6 | **Presentation-deck evidence screenshots** (swimlane snapshots, healthy docker, `smoke-cross-service.sh` green, Newman 66/66, Vue UI screens). | Deferred until final rehearsal (2026-05-18 window per memory) | Capture on a clean boot during rehearsal; the evidence directory already contains the cross-service trace and `menu-events` log from this audit and can be reused | Sierra-Lima |
| 3.7 | **Minor DTO `@Pattern` permissiveness.** The contract-aligned regex `[0-2][0-9]:[0-5][0-9]-[0-2][0-9]:[0-5][0-9]` still admits "hours" like `23:59-29:59` (hours 23-29 in `[0-2][0-9]`). The service layer's `LocalTime.parse` catches this at runtime and `acceptsOrders` correctly returns `false`, but the DTO is still looser than a real `00-23` time | Exactly matches `0020 §3`, so it is not drift; tightening to `(?:[01][0-9]|2[0-3])` would require a superseding contract decision and a DTO change | Optional post-CP#1 (5 min with a decision `0021`) |
| 3.8 | **Encrypted `Assignment-3-Submission.pdf` cross-check** (password-protected file in `dev-docs/prior-submissions/`). | Still password-protected at this commit; `5a998ad` audit had the same blocker | User to open the PDF locally and confirm no unmentioned obligations for Sierra-Lima beyond what the ADRs capture | Sierra-Lima |
| 3.9 | **Load / perf profile** for `/menu-items/validate` under concurrent W1 traffic. | Out of A3 scope; not required for CP#1/CP#2/CP#3 | Optional post-submission JMeter/k6 run if the team wants it | Optional |

None of 3.1-3.9 blocks hand-over to the team lead.

---

## Q4 -- Final verdict

**READY TO HAND OVER to the Group 7 team lead for cross-team integration.**

Supporting facts at `d23145f`:

- **All five Golf-Papa-Tango findings are accounted for.** F1-F4 are patched and re-verified (unit tests + Newman + smoke). F5 is a documentation-only doc-drift in an older audit, explicitly deferred by the user; it has zero runtime effect and a 5-minute remedy (§Q3.1) that can happen before or after hand-over.
- **Automated tests green: 69/69.** Restaurant 26, Menu 43. Up from 65 at the prior-audit `5a998ad`; the four new tests directly cover the F1-F3 regressions.
- **Postman/Newman suite green: 66/66 assertions.** Golf-Papa-Tango's Finding 4 was that the suite was red (5 failures at `1a6e8c7`). At `d23145f` it is green on a cold run against the live stack.
- **Full docker stack healthy** and both smokes exit 0; the optional `menu-events` stretch fires with the frozen envelope.
- **Every A3 §3-§7 obligation for Sierra-Lima is in the repo:** architecture (`dev-docs/decisions/0001`), APIs (`0010 §8`, `0020 §1-2`), data models (`0020 §4`), workflows (`0002` W1/W2/W3 + `0030` W1 hops 4/5 + `0040` optional W2-adjacent emit), integration mechanisms (REST sync + log-only async seam + JWT + gateway prefix), and final implementation responsibilities (`0001 §Sierra-Lima scope`).

### Recommended hand-over action

1. Let the team lead merge `dev` at `d23145f` into the Group 7 integration branch.
2. Keep §Q3 items 3.3-3.5 on the team-lead's integration checklist (end-to-end W1, real JWT, W2/W3).
3. Sierra-Lima closes 3.1 (F5 errata) either before the hand-over email or in the first post-merge patch -- small and low-risk.

No defects. No integration-blocking issues. The slice is in the strongest state it has been in during this workspace.

---

## Appendix A -- Reproduction commands

```bash
# Tests (run from each service dir)
cd services/restaurant-service && mvn test
cd services/menu-service && mvn test

# Frontend
cd services/frontend/quickbite-frontend && npm ci && npm run lint -- --no-fix && npm run build

# Stack + smokes
cd services/local-dev
docker compose up -d
docker compose ps                     # expect 6 healthy
bash smoke.sh                         # expect: OK -- Sierra-Lima smoke test passed.
bash smoke-cross-service.sh           # expect: sierra-lima failures = 0, teammate failures = 0

# Health
curl -sf http://localhost:8081/actuator/health
curl -sf http://localhost:8082/actuator/health

# Newman full suite (executable on this host)
npx newman run services/local-dev/postman/QuickBite.postman_collection.json \
               -e services/local-dev/postman/QuickBite.postman_environment.json
```

## Appendix B -- Deltas vs. prior audits

| Area | `5a998ad` Charlie-Lima-Alfa | `1a6e8c7` Golf-Papa-Tango | `d23145f` (this audit) |
|---|---|---|---|
| Automated tests | 65 green (23 + 42) | 65 green (23 + 42) | **69 green (26 + 43)** |
| Newman | Not executable on that host | **5 failures / 63 assertions** | **66/66 green** |
| Mixed-currency validate | 200 with wrong total (bug) | Finding 1 (High) | **400 via `MixedCurrencyException`** |
| `operatingHours` `99:99-99:99` | Accepted, `acceptsOrders=true` | Finding 2 (High) | **DTO rejects at `@Pattern`; parse fallback returns `false`** |
| `GET /restaurants` shape | Array (contract drift) | Finding 3 (Medium) | **`Page<RestaurantResponse>` (matches `0020 §1.5`)** |
| Postman `[422] priceAmount=0` + CRUD seed delete | Not runnable here | Finding 4 (Medium) | **`[400] priceAmount=0` with `validationErrors`; CRUD DELETE uses `createdMenuItemId`** |
| `DELETE /restaurants/{id}` doc-drift | Present in `5a998ad` audit row 34 | Finding 5 (Low) | **Still open; deferred by user (see §Q3.1)** |
| Verdict | READY | NOT READY | **READY** |
