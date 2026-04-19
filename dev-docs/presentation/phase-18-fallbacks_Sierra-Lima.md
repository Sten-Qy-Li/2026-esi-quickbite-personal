# CP#3 Fallback Materials and Recovery -- Sierra-Lima slice

- **Owner.** Sierra-Lima.
- **Source.** `Charlie-Lima-Alfa_a520963_project-phases-final.md`
  Phase 18 task 4.
- **Companions.** `phase-18-slides_Sierra-Lima.md` (deck),
  `phase-18-demo-script_Sierra-Lima.md` (live click-path).
- **Goal.** Whatever breaks during the live demo, we have a path
  the audience never feels as a stall.

The fallback strategy follows a single principle: **always have a
narrate-able artefact within one keystroke**. Live first, recorded
second, screenshot third, narration over the report fourth. We
never go silent.

---

## 1. If the stack won't come up at all

Symptoms: `docker compose ps` shows containers `Exited` or stuck
on `(starting)` past 90 seconds; `actuator/health` returns
`DOWN` or `connection refused`.

### 1.1 Quick re-up (60 s)

```
cd services/local-dev
docker compose --env-file .env.local --profile dev-gateway down
docker compose --env-file .env.local --profile dev-gateway \
  up -d --build
docker compose ps
```

Watch for `(healthy)` on every row. If a database container is
in a restart loop, the host port is almost always the cause --
override `RESTAURANT_DB_HOST_PORT` or `MENU_DB_HOST_PORT` in
`.env.local` per `services/local-dev/runbook.md` §1, then re-up.

### 1.2 Hard reset (2 min, destroys data)

```
docker compose --env-file .env.local --profile dev-gateway down -v
docker compose --env-file .env.local --profile dev-gateway \
  up -d --build
docker compose --env-file .env.local exec restaurant-service \
  curl -sf localhost:8081/actuator/health
docker compose --env-file .env.local exec menu-service \
  curl -sf localhost:8082/actuator/health
```

The `V2__seed_demo_data.sql` migrations re-seed `Pizza Antonio`,
`Cafe Nero`, `Sushi Yuki`, the four sample menu items, and the
two demo owner accounts. We are back to a known-good fixture in
under 2 minutes from a cold disk.

### 1.3 Worst case -- skip live, narrate the recording

If §1.1 + §1.2 both fail in front of the audience:

1. Open `dev-docs/checkpoint-1-backup/` and play the CP#1 backup
   recording (the W1 happy-path, byte-for-byte).
2. Narrate over it using the deck's S7-S8 talk track. The
   audience sees the same flow; we just don't have a live
   keyboard.
3. Resume live demo at §3 (Postman) once the stack stabilises in
   the background.

---

## 2. If a single service is unhealthy

| Container | Restart command | Knock-on effect |
|---|---|---|
| `quickbite-restaurant-db` | `docker compose restart restaurant-db` | Restaurant + Menu both fail (Menu's ownership lookup hits Restaurant). |
| `quickbite-menu-db` | `docker compose restart menu-db` | Menu only. |
| `quickbite-restaurant-service` | `docker compose restart restaurant-service` | Menu mutations 503 until back. |
| `quickbite-menu-service` | `docker compose restart menu-service` | Menu endpoints fail; Restaurant is fine. |
| `quickbite-frontend` | `docker compose restart frontend` | Browser path dead; Postman still works. |
| `quickbite-dev-gateway` | `docker compose restart dev-gateway` | Browser routes via gateway -- redirect to direct ports `8081/8082` for the demo only. |

If the Spring Boot service is stuck in startup loop, the
common causes are:

1. DB host port collision (override per runbook §1).
2. Wrong `JWT_SECRET` (compose file sets a dev default; do not
   override during a live demo).
3. `docker compose logs` shows a Flyway error -- the database
   has data from a previous schema version. Run `docker compose
   down -v` (§1.2 above).

---

## 3. Per-beat fallbacks

### 3.1 Browser flow fails

Switch to Postman's `Restaurant CRUD` + `Menu CRUD` folders. The
collection mirrors §1.4-§1.6 of the demo script (create
restaurant -> add items -> toggle availability) without a
browser.

If both fail, fall back to:

```
bash services/local-dev/smoke.sh
```

which performs the same beats end-to-end in ~10 s and prints a
green status line. This proves the backend half is alive even
when the SPA can't render.

### 3.2 Swagger UI doesn't load

Show the pre-captured screenshot pack:

- `dev-docs/verification/swagger-restaurant.png`
- `dev-docs/verification/swagger-menu.png`

Both are committed at the 2026-05-18 rehearsal slot and named
in `phase-18-verification_Sierra-Lima.md` §3. The deck slide S6
already references these by relative path; the live `Try it out`
moment becomes a narrated walk-through of the screenshot.

### 3.3 Postman pack shows red

Two layers of fallback:

1. **Newman in the terminal.**

   ```
   newman run services/local-dev/postman/QuickBite.postman_collection.json \
     --folder "W1 Integration"
   ```

   Same coverage; smaller surface; less likely to misbehave than
   the desktop runner.

2. **Last green Newman report.** The Phase 10 verification note
   (`phase-10-verification_Charlie-Lima-Alfa.md` §8.3-§8.4)
   captures a known-good run. Open and screen-share that section.

### 3.4 Cross-service smoke exits non-zero

The script's exit codes are explicit (Phase 16 verification §4.2
table):

| Exit | Meaning | What to say |
|---|---|---|
| 0 | All Sierra-Lima OK + teammate probes OK / SKIP | "Smoke passed; here's the trace." |
| 1 | At least one Sierra-Lima step failed | Stop the demo on this beat; jump to §1.1 of this doc and re-run. |
| 2 | Sierra-Lima OK but a teammate probe failed | "Sierra-Lima's slice is green; the failing probe is X -- I'll hand to <owner-callsign> for context." Then move on. |

The trace file at `services/local-dev/evidence/cross-service-smoke_<RUN_TAG>.log`
is the artefact to point at if we cannot show the script live --
tail the latest one and read the summary line.

### 3.5 Resilience demo (S14) misbehaves

The demo is *optional*. If `docker compose stop restaurant-service`
doesn't trip Order's circuit breaker within 10 s, do not chase
it on stage:

1. Skip to the next deck slide (S15 "Sacrifices").
2. Show the pre-captured screenshot pair under
   `dev-docs/verification/resilience-circuit-open.png` and
   `dev-docs/verification/resilience-circuit-closed.png`
   (committed at the 2026-05-18 rehearsal slot if the team runs
   the demo end-to-end successfully there; otherwise the slide
   stays narrated text only).

---

## 4. Backup seed data and demo IDs

Sourced from `V2__seed_demo_data.sql` in each service. These IDs
are stable across resets; memorise the first six characters so
you can read them aloud without squinting.

### 4.1 Demo owners

| Account | userId | Role | Use |
|---|---|---|---|
| Owner A | `00000000-...-...000099` | RestaurantOwner | owns Pizza Antonio, Sushi Yuki |
| Owner B | `00000000-...-...000098` | RestaurantOwner | owns Cafe Nero (closed) |
| Customer C | `00000000-...-...0000c1` | Customer | smoke + negative auth path |
| Admin | `00000000-...-...0000a1` | Admin | bypasses ownership check |

### 4.2 Demo restaurants

| restaurantId | Name | Owner | isOpen | Hours |
|---|---|---|---|---|
| `d0000099-...000099` | Pizza Antonio | Owner A | true | 11:00-22:00 |
| `d0000098-...000098` | Cafe Nero | Owner B | false | 08:00-15:00 |
| `d0000097-...000097` | Sushi Yuki | Owner A | true | 12:00-22:00 |

### 4.3 Demo menu items

| menuItemId | Restaurant | Name | Price | Available |
|---|---|---|---|---|
| `e0000010-...000010` | Pizza Antonio | Margherita | 10.50 EUR | true |
| `e0000011-...000011` | Pizza Antonio | Quattro Formaggi | 12.50 EUR | true |
| `e0000012-...000012` | Sushi Yuki | Salmon nigiri | 5.00 EUR | true |
| `e0000032-...000032` | Cafe Nero | Cheesecake | 4.50 EUR | **false** |

### 4.4 Sentinel UUIDs

| Use | UUID |
|---|---|
| Always-unknown restaurant (404) | `ffffffff-ffff-ffff-ffff-ffffffffffff` |
| Always-unknown menu item (NOT_FOUND in batch validate) | `ffffffff-ffff-ffff-ffff-ffffffffaa01` |

---

## 5. Recovery commands cheat-sheet

Print this section A4-landscape and have it on the table.

```
# Health
docker compose ps
curl -s http://localhost:8081/actuator/health
curl -s http://localhost:8082/actuator/health

# Restart one service
docker compose restart restaurant-service
docker compose restart menu-service
docker compose restart frontend

# Tail a service log
docker compose logs -f restaurant-service
docker compose logs -f menu-service

# Smoke (Sierra-Lima only)
bash services/local-dev/smoke.sh

# Smoke (cross-service, captures evidence trace)
bash services/local-dev/smoke-cross-service.sh

# Hard reset (last resort; throws away data)
docker compose --env-file .env.local --profile dev-gateway down -v
docker compose --env-file .env.local --profile dev-gateway up -d --build

# Mint a dev JWT (until User Service is the sole issuer)
java services/restaurant-service/src/test/java/ee/ut/esi/quickbite/restaurant/JwtDevMint.java
```

---

## 6. What to commit before the talk

By 2026-05-18 EOD (Phase 18 rehearsal):

- `dev-docs/verification/swagger-restaurant.png`
- `dev-docs/verification/swagger-menu.png`
- `dev-docs/verification/negative-auth-401.png`
- `dev-docs/verification/negative-auth-403.png`
- `services/local-dev/evidence/cross-service-smoke_<RUN_TAG>.log`
  (one good run from the rehearsal stack)
- `services/local-dev/evidence/menu-events_<RUN_TAG>.log`
  (paired grep slice from the same run)

The deck and the demo script reference all of these by relative
path; once they land, no slide edits are needed.

If the resilience demo runs cleanly at rehearsal, additionally
commit:

- `dev-docs/verification/resilience-circuit-open.png`
- `dev-docs/verification/resilience-circuit-closed.png`

Otherwise leave slide S14 as narrated-text-only and skip live on
the day if we run hot.

---

## 7. Where this script ends

When we walk into the room on 2026-05-19, we have:

- 16 slides (`phase-18-slides_Sierra-Lima.md`).
- 12 minutes of live demo (`phase-18-demo-script_Sierra-Lima.md`).
- This fallback doc for everything that goes sideways.
- A Q&A drill (`phase-18-qa-prep_Sierra-Lima.md`).
- A clean stack on the laptop, validated 30 minutes earlier.

If something we did not anticipate goes wrong, the failure mode
is: **stop, breathe, narrate from the deck, recover off-stage**.
The audience never sees a frozen window.
