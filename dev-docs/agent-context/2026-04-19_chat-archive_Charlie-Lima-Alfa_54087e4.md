# Chat Archive - 2026-04-19 - Charlie-Lima-Alfa (`54087e4`)

## Session Summary

This session executed **Phase 18 -- Final Presentation Rehearsal**
for the QuickBite stack, as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
Phase 18 (lines 1620-1660).

The session began on top of `54087e4` ("Land Phase 17 report and
evidence pack"). Phase 18 is a presentation-prep phase: its
Definition of Done is "slides + live demo rehearsed + every
presenter knows what to say + backup materials ready" -- explicitly
*not* a code phase. Deliverable: a self-contained pack under
`dev-docs/presentation/` (slide deck, demo script, fallbacks,
Q&A drill) plus the standard verification note and session
archive.

No service code, no test code, no docker-compose changes, no
report-content edits. Just presentation prep: deck draft, live
click-path, recovery playbook, Q&A timeboxing. The Phase 18 DoD
roll-up is recorded in
`dev-docs/verification/phase-18-verification_Sierra-Lima.md` §6
and reproduced under §2 of this archive. One DoD bullet ("Live
demo rehearsed at least once") is intentionally pending the
2026-05-18 team rehearsal slot -- the entire content of this
phase is what enables that rehearsal to run keystroke-fluent.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`,
  and the `Frontend` under `services/frontend/quickbite-frontend/`.
- Today: 2026-04-19 (Sunday)
- Active branch: `dev`
- Parent commit: `54087e4` -- "Land Phase 17 report and evidence
  pack"
- Environment: Windows 11 + Git Bash + (no Maven / Docker runs
  this session since Phase 18 is documentation-only -- the Phase
  18 rehearsal slot at 2026-05-18 will exercise the full stack).

## User Requests

Initial request: *"Hi Claude, please work on Phase 18 of the master
plan `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.
After completing the implementation of Phase 18, please archive the
session context to `dev-docs/agent-context`, and then commit all
changes and push (try to commit and push the entire local
repository; exclude files only if there's a very good reason,
according to your best judgement). Thanks!"*

Model pinned to `opus` and effort to `max` early via `/model opus`
+ `/effort max`. No mid-session redirections or user corrections.

## Phase 18 Task-by-Task Record

### Task 1 -- Slide deck

`dev-docs/presentation/phase-18-slides_Sierra-Lima.md` is a
16-slide Markdown deck (`## S1`-`## S16`). Each slide carries:

- `Title` -- one line.
- `Visual` -- diagram, table, or screenshot reference (asset
  paths resolve against existing repo files; placeholders for
  yet-to-be-captured screenshots resolve to the same filenames
  the Phase 17 report already adopted in §15).
- `Talk track` -- speaker prose, named speaker callsign per
  paragraph.
- `Hand-off` -- the closing line that names the next speaker, so
  the room never goes silent in transition.

Coverage vs Phase 18 task 1 (the eight required slide topics):

| Phase 18 task 1 bullet | Slide(s) | Speaker |
|------------------------|----------|---------|
| System overview and architecture | S2 | Alfa-Kilo |
| Sierra-Lima's services: Restaurant + Menu | S4, S5 | Sierra-Lima |
| API design (Swagger screenshots) | S6 | Sierra-Lima |
| Synchronous integration: Menu batch validation in W1 | S7, S8 | Alfa-Kilo |
| Asynchronous integration: W2 / W3 end-to-end | S9, S10 | Mike-Alfa |
| Security: JWT + role gating | S11, S12 | Sierra-Lima |
| Frontend walkthrough | S13 | Sierra-Lima |
| (Optional) Resilience demo | S14 | Mike-Alfa |

S1 (cover), S3 (service catalogue), S15 (sacrifices + future
work), and S16 (closing + Q&A) wrap the eight required panes.
The deck is sized to a 25 min talk (~90 s per slide).

Numbers on the slides are quoted from the Phase 17 report --
none invented. JUnit counts (23 / 42), Postman counts (9 / 40 +
14 / 17), service count (7 / 8), endpoint count (6 / 6), topic
count (5 incl. log-only `menu-events`), all matched against
`report-draft-backend_Sierra-Lima.md` §1 and §10.

Render target is left flexible: the file's preamble names Marp,
pandoc, and Slides as the three viable rendering paths. Every
fact is checkable against the report or an ADR, so a renderer
swap requires no rewrite.

### Task 2 -- Live demo script

`dev-docs/presentation/phase-18-demo-script_Sierra-Lima.md`
documents the 12-min keystroke-by-keystroke live demo. Sections:

- **§0 Pre-demo (off-stage).** docker compose up + JWT mint +
  tab pre-open + health curl gate. 5 min before the talk.
- **§1 Browser walkthrough (3.5 min).** Sign-in -> create
  restaurant -> add menu items -> toggle availability ->
  expected `menu-events` log line. Mirrors deck S13.
- **§2 Swagger walk (1.5 min).** Both Swagger UIs; live `Try it
  out` on `/availability` + `/validate`. Mirrors S6.
- **§3 W1 Postman + smoke (2 min).** Postman runner over the W1
  Integration folder + `bash smoke.sh`. Mirrors S8.
- **§4 Negative auth (1.5 min).** 401 (no token) + 403 (owner-A
  on owner-B's restaurant) + WARN log line in tail. Mirrors
  S12.
- **§5 (folded into §1.6).** Menu-events log beat is part of the
  browser walk; terminal stays tailing for §6.
- **§6 Cross-service smoke (2 min).** `smoke-cross-service.sh`
  + `tail` on the produced trace. Mirrors S10.

Each beat names the speaker, the slide it pairs with, the exact
command or click, the expected output, and a per-beat fallback
pointer to the recovery doc. Pre-allocated terminal panes / tabs
keep alt-tabbing-while-cold off the live cadence.

### Task 3 -- Speaking parts

The slide deck preamble carries a single allocation table (one
row per slide block, primary + backup speaker). The Q&A drill
(task 5) carries a parallel table for the seven anticipated
questions (primary + backup answerer per question).

Coverage vs Phase 18 task 3 sub-bullets:

| Sub-bullet | Slides | Primary | Backup |
|------------|--------|---------|--------|
| Architecture overview | S1, S2 | Alfa-Kilo | Sierra-Lima |
| Sierra-Lima services | S4, S5 | Sierra-Lima | Alfa-Kilo |
| Synchronous flow | S7, S8 | Alfa-Kilo | Sierra-Lima |
| Asynchronous flow | S9, S10 | Mike-Alfa | Elephant-Yankee |
| Security | S11, S12 | Sierra-Lima | Alfa-Kilo |
| Frontend | S13 | Sierra-Lima | Alfa-Kilo |

Hand-off lines baked into each slide's `Hand-off:` field; the
room never goes silent in transition. Allocation is consistent
with the Phase 17 report §12 team table and the service-
ownership lines from CP#1 talking-points §0. Ratification is at
the 2026-05-18 rehearsal kick-off.

### Task 4 -- Fallback materials

`dev-docs/presentation/phase-18-fallbacks_Sierra-Lima.md` covers
all four sub-bullets:

| Sub-bullet | Section |
|------------|---------|
| Screenshots | §3.2 (Swagger), §3.5 (resilience), §6 (commit checklist) |
| Pre-recorded flow | §1.3 (CP#1 backup recording fallback) |
| Backup seed data | §4 (owners + restaurants + items + sentinel UUIDs) |
| Recovery commands | §5 (cheat-sheet), §1.1 / §1.2 (re-up + hard reset), §2 (per-container restart) |

Per-beat fallback paths in §3 cover every demo step:
browser -> Postman; Postman -> Newman; Newman -> last-green
report; cross-service smoke exit codes mapped to "presentable"
vs "stop-now". The screenshot pack list (§6) is exactly the six
filenames the Phase 17 report already references; landing them
at the 2026-05-18 rehearsal slot resolves both the report
placeholders and the deck references in one capture pass.

### Task 5 -- Q&A timeboxing

`dev-docs/presentation/phase-18-qa-prep_Sierra-Lima.md` answers
all seven questions from Phase 18 task 5, each with **<= 45 s
spoken** (~120 words), structured as headline + why + cite +
optional "if pressed" follow-up.

| # | Question | Answerer |
|---|----------|----------|
| Q1 | Why these seven services? | Alfa-Kilo |
| Q2 | Why is Review design-only? | Alfa-Kilo |
| Q3 | How were service boundaries chosen? (DDD) | Alfa-Kilo |
| Q4 | How does async work? Topics, envelope, idempotency | Mike-Alfa |
| Q5 | Security at gateway vs service level | Sierra-Lima |
| Q6 | Why no Eureka? | Alfa-Kilo |
| Q7 | What did you sacrifice and why? | Alfa-Kilo |

Three likely follow-ups are pre-rehearsed too: F1 ("how do you
know your tests pass?" -- Sierra-Lima), F2 ("walk us through the
menu-events envelope" -- Sierra-Lima), F3 ("how does the frontend
detect token expiry?" -- Sierra-Lima). The drill protocol at the
foot of the doc tells the team how to time + tighten each answer
at the rehearsal.

## Files Touched This Session

| File | Status | Size |
|------|--------|------|
| `dev-docs/presentation/phase-18-slides_Sierra-Lima.md` | New | ~340 lines |
| `dev-docs/presentation/phase-18-demo-script_Sierra-Lima.md` | New | ~290 lines |
| `dev-docs/presentation/phase-18-fallbacks_Sierra-Lima.md` | New | ~280 lines |
| `dev-docs/presentation/phase-18-qa-prep_Sierra-Lima.md` | New | ~250 lines |
| `dev-docs/verification/phase-18-verification_Sierra-Lima.md` | New | ~240 lines |
| `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_54087e4.md` | New (this file) | -- |

No code, test, compose, Dockerfile, Postman, ADR, frontend, or
report-content changes this session.

## Decisions and Rationale (this session only)

- **One pack under `dev-docs/presentation/`, four files.** The
  master plan lists deck / demo / speaker parts / fallbacks /
  Q&A as five tasks; collapsing speaker-part allocation into the
  deck preamble (where it must stay aligned with slide order
  anyway) keeps the artefact set at four. The Q&A drill is a
  separate file because it is read aloud in a different physical
  setting than the deck (rehearsal-room timing exercise vs
  presentation-room slide flow).
- **Defer screenshot capture to the 2026-05-18 rehearsal.** The
  Phase 17 report already adopted the placeholder filenames, and
  capturing in this session would produce stale-by-rehearsal
  screenshots that we'd have to re-take anyway. The deck and
  demo script reference the placeholders by relative path; once
  the screenshots land, no edits are needed in either artefact.
- **One DoD bullet intentionally pending.** "Live demo rehearsed
  at least once" requires the team rehearsal slot on
  2026-05-18 with all teammate `*_BASE` env vars set. The
  Phase 18 verification note records this explicitly in §6 so
  the gap is visible, not implicit.
- **Quote, do not invent, numbers.** Every statistic on a slide
  is sourced from the Phase 17 report (`report-draft-backend_Sierra-Lima.md`).
  This kept the deck verification step short: rerun is just
  "do the report numbers still hold?" rather than "do the deck
  numbers match the report?".
- **Speaker assignment by service ownership, not by topic
  expertise.** Each callsign owns the slides for the services
  they implemented. This is the most-defendable allocation
  under direct grader questioning ("you wrote this, walk me
  through it"). Backup speakers are assigned cross-callsign so
  any single missing speaker isn't a single point of failure.
- **Resilience demo (S14) flagged optional in three places.**
  Master plan calls it optional; deck preamble flags it; deck
  slide S14 talk track ends with "if we're tight on time, skip";
  fallback doc §3.5 documents the no-go path. Triple flag
  because the demo carries the highest live-failure risk in the
  pack.

## Commit Plan

Expected commit: `Land Phase 18 presentation rehearsal pack`.

Stages:
1. `git status` to confirm only the six intended files are new.
2. `git add` the entire `dev-docs/presentation/` directory plus
   the verification note plus this archive (no other tree
   changes exist).
3. Commit with a HEREDOC message ending in the standard
   `Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>`
   line.
4. `git push` to `origin/dev`.

No files are being excluded. The repository is clean of
generated artefacts (`dist/`, `node_modules/`, `target/`) per
the existing `.gitignore`; no risky files (secrets, credentials,
PII) are introduced.

## Open Items Handed Off to Phase 19 (Buffer & Final Freeze) and the Rehearsal Slot

For the **2026-05-18 rehearsal**:

- Walk the deck top-to-bottom (~25 min).
- Run the live demo end-to-end (`phase-18-demo-script_Sierra-Lima.md`
  §0-§6) with all teammate `*_BASE` env vars set.
- Drill all seven Q&A questions, time each answer, tighten anything
  over 45 s.
- Capture and commit the six screenshot / trace files listed in
  the verification note §4.
- Confirm speaking-part allocations.
- If the resilience demo runs cleanly, capture the two
  `resilience-circuit-*.png` screenshots and keep S14 live; else
  leave S14 narrated-text-only.

For **Phase 19** (Buffer & Final Freeze):

- Re-run `services/local-dev/smoke.sh` and
  `services/local-dev/smoke-cross-service.sh` against the
  rehearsal stack one more time.
- Verify clean startup: `docker compose down -v` then
  `docker compose --env-file .env.local --profile dev-gateway
  up --build -d`.
- Verify seeded demo users, restaurants, menu items, and order
  flow are all reachable.
- Tag the commit (`v1.0.0-cp3`) before the graded run on
  2026-05-19.
- Export the Phase 17 report to PDF (any of `pandoc --from gfm`,
  VS Code "Markdown PDF", GitHub print-to-PDF) for the hand-in.

For the **2026-05-19 graded run**:

- Run §0 of the demo script 30 min before the talk.
- Open all panes / tabs listed in the demo script.
- Have `phase-18-fallbacks_Sierra-Lima.md` §5 (recovery
  cheat-sheet) printed on the table.
- Drive through the deck + demo + Q&A.
- Tag follow-up commits as `v1.0.0-cp3-postdemo` if any
  rehearsal-day fixes are needed (Phase 19's freeze comes after
  the talk).
