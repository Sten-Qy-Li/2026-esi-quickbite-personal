# Checkpoint #1 Backup Materials

This folder holds the backup recording of the Sierra-Lima CP#1 demo.
If the live demo fails on 2026-05-05, the backup plays in place.

## Expected contents (to be recorded before 2026-04-28)

| File | Duration | Source |
|------|----------|--------|
| `demo-happy-path.mp4` | 2-3 min | Screen recording of the script in [`../checkpoint-1-talking-points.md`](../checkpoint-1-talking-points.md) §7 |
| `demo-negative-auth.mp4` | 45-60 sec | Postman `Negative Auth` folder run + summary screen |
| `smoke-script.mp4` | 20-30 sec | `bash services/local-dev/smoke.sh` run from a clean stack |

## Recording script (happy path)

Follow §7 of `../checkpoint-1-talking-points.md` verbatim. Record
with OBS or the built-in Windows Xbox Game Bar; target 1080p, 24 fps,
narrate each step in the same words that will be used live.

Before recording:

1. `docker compose down -v` and `up --build` so Flyway seeds run
   against fresh DBs; the restaurant and menu UUIDs shown on
   screen will then match the seed (`V2__seed_demo_data.sql`).
2. Open Postman with the `QuickBite.postman_environment.json`
   environment selected. Clear any previously captured response
   bodies so the recording shows fresh calls, not stale 304s.
3. Open a terminal large enough to read from the back of the
   room -- at least 16 pt font, dark-on-light or light-on-dark
   (whatever presents cleanest).

While recording:

- Speak the URL aloud on the first hit (`POST localhost:8081
  /restaurants`). After that, refer to the service by name only.
- For each response, point the cursor at the status code and the
  single field that matters (`restaurantId`, `acceptsOrders`,
  `allValid`, `totalAmount`). Do not narrate the entire JSON.
- If a call glitches during the recording, do not stop the
  recording -- retry the call, then edit out the slip in post. An
  uninterrupted take is the goal.

## Storage

Commit the files as-is. Binary blobs in git are acceptable for a
2-3 minute MP4 at demo resolution (<50 MB total expected); this
is the single place the graders can get ground-truth evidence if
the live laptop fails, so version control is the safest home.

If the files exceed 50 MB combined, move them to the team's shared
OneDrive folder and commit a pointer here (`OFFSITE.md`) with the
link and an MD5 of each file so we can detect tampering between
recording day and checkpoint day.

## Version marker

Recorded against commit: _fill in on recording day_.
Recorded by: _fill in on recording day_.
Verified playback on: _fill in on recording day_.
