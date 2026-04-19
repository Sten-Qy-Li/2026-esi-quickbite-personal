# Phase 18 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 18
("Final Presentation Rehearsal") for Restaurant Service, Menu Service,
and the Sierra-Lima frontend slice.

Date: 2026-04-19. Target rehearsal: 2026-05-18. Target CP#3 graded:
2026-05-19. Base commit: `54087e4` (Phase 17 land).

---

## 0. Session context

Phase 18 is a presentation-prep phase, not a code phase: no service
code, no test code, no compose changes. The deliverable is a
self-contained pack under `dev-docs/presentation/` that the team
can rehearse from on 2026-05-18 and present from on 2026-05-19,
plus this verification note.

Phase 17 already landed the report (`dev-docs/report-draft-backend_Sierra-Lima.md`,
~1080 lines) and the report's evidence appendix names the Phase 18
screenshot targets verbatim. Phase 18 turns that report into a
deck + demo script + fallback playbook + Q&A drill, so the
rehearsal slot on 2026-05-18 captures screenshots once and the
graded run on 2026-05-19 is keystroke-rehearsed.

Artefacts landed in this phase:

- `dev-docs/presentation/phase-18-slides_Sierra-Lima.md` (16
  slides + speaker notes + speaking-part assignments).
- `dev-docs/presentation/phase-18-demo-script_Sierra-Lima.md`
  (12 min live click-path with expected outputs and timings).
- `dev-docs/presentation/phase-18-fallbacks_Sierra-Lima.md`
  (recovery commands, backup seed data, screenshot pack list,
  per-beat fallback paths).
- `dev-docs/presentation/phase-18-qa-prep_Sierra-Lima.md`
  (timeboxed answers to the seven anticipated grader questions
  + three likely follow-ups).
- `dev-docs/verification/phase-18-verification_Sierra-Lima.md`
  (this note).
- `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_<sha>.md`
  (session archive, written last before commit).

Not touched on purpose:

- No service code. Phase 18 DoD is "slides + demo + speaker
  parts + backup materials"; nothing requires source changes.
- No new screenshots in-tree yet. The demo + fallback packs
  reference the placeholder filenames the Phase 17 report
  already adopted (`dev-docs/verification/swagger-restaurant.png`,
  `swagger-menu.png`, `negative-auth-401.png`,
  `negative-auth-403.png`); they land at the 2026-05-18
  rehearsal slot without any deck edits.
- No fresh `services/local-dev/evidence/cross-service-smoke_*.log`.
  The script ships from Phase 16; the rehearsal run drops a
  trace under the same path. Same convention as Phase 17.

---

## 1. Task 1 -- Slide deck assembled

`dev-docs/presentation/phase-18-slides_Sierra-Lima.md` is a
16-slide deck (`## S1`-`## S16`). Each slide carries `Title`,
`Visual`, `Talk track`, and `Hand-off`; the file's preamble
explains the rendering choice (Markdown source -> Marp / pandoc /
Slides) and the speaking-part allocation table.

Coverage vs the eight bullets in Phase 18 task 1:

| Phase 18 task 1 bullet | Slide(s) | Speaker (primary) |
|------------------------|----------|-------------------|
| System overview and architecture | S2 | Alfa-Kilo |
| Sierra-Lima's services: Restaurant + Menu | S4, S5 | Sierra-Lima |
| API design (Swagger screenshots) | S6 | Sierra-Lima |
| Synchronous integration: Menu batch validation in W1 | S7, S8 | Alfa-Kilo |
| Asynchronous integration: W2 / W3 end-to-end | S9, S10 | Mike-Alfa |
| Security: JWT issuance and validation, role-gating | S11, S12 | Sierra-Lima |
| Frontend walkthrough | S13 | Sierra-Lima |
| (Optional) Resilience demo | S14 | Mike-Alfa |

Cover (S1), service catalogue (S3), sacrifices/future work (S15),
and closing/Q&A (S16) are scaffolding around the eight required
bullets so the talk runs cohesively.

Numbers used on the slides are quoted from the Phase 17 report;
none are invented. JUnit counts (23 / 42), Postman counts (9 / 40
+ 14 / 17), service count (7 / 8), endpoint count (6 / 6), topic
count (5) all match `report-draft-backend_Sierra-Lima.md` §1, §10.

---

## 2. Task 2 -- Live demo script written

`dev-docs/presentation/phase-18-demo-script_Sierra-Lima.md`
documents the keystroke-by-keystroke 12-minute live demo. Each
beat names the speaker, the deck slide it pairs with, the exact
command or click, and the expected output:

| Beat | Slide | Speaker | Duration | Hooks |
|------|-------|---------|----------|-------|
| §0 Pre-demo (off-stage) | -- | Sierra-Lima | 5 min | docker compose up + tab pre-open |
| §1 Browser walkthrough | S13 | Sierra-Lima | 3.5 min | sign-in -> create -> add items -> toggle |
| §2 Swagger walk | S6 | Sierra-Lima | 1.5 min | both Swagger UIs |
| §3 W1 in Postman + smoke.sh | S8 | Alfa-Kilo + Sierra-Lima | 2 min | Postman runner + bash smoke |
| §4 Negative auth | S12 | Sierra-Lima | 1.5 min | 401 + 403 + WARN log |
| §5 Toggle -> menu-events | (folded into §1.6) | -- | -- | terminal A stays tailing |
| §6 Cross-service smoke | S10 | Mike-Alfa | 2 min | smoke-cross-service.sh + tail trace |

Pre-demo checklist is explicit (`docker compose up`, JWT mint,
tab pre-open, health curls). Tabs / panes are pre-allocated so
the live cadence has zero alt-tabbing-while-cold.

The Phase 16 toggle -> log-line beat appears as §1.6 and §5 of
the demo script; deck slide S10 reuses the same envelope sample
shape as Phase 17 report §15.5, so audience sees consistent
output between deck and live console.

---

## 3. Task 3 -- Speaking parts assigned

The slide-deck preamble carries the speaking-part allocation
table (one row per slide block, primary + backup speaker named).
Coverage vs Phase 18 task 3 bullets:

| Phase 18 task 3 part | Block in deck | Primary | Backup |
|----------------------|---------------|---------|--------|
| Architecture overview | S1, S2 | Alfa-Kilo | Sierra-Lima |
| Sierra-Lima services | S4, S5 | Sierra-Lima | Alfa-Kilo |
| Synchronous flow | S7, S8 | Alfa-Kilo | Sierra-Lima |
| Asynchronous flow | S9, S10 | Mike-Alfa | Elephant-Yankee |
| Security | S11, S12 | Sierra-Lima | Alfa-Kilo |
| Frontend | S13 | Sierra-Lima | Alfa-Kilo |

Service catalogue (S3), API design (S6), resilience demo (S14),
sacrifices/future work (S15), and closing/Q&A (S16) also have
named speakers in the same table. Hand-off lines are baked into
each slide's `Hand-off:` field so the room never goes silent in
transition.

Speaking-order ratification: the deck preamble notes the
allocations are pending team confirmation at the 2026-05-18
rehearsal. Sierra-Lima's slots are stable (services, APIs,
security, frontend, fallback for everything); the
Alfa-Kilo / Mike-Alfa / Elephant-Yankee allocations follow the
service-ownership lines from the Phase 17 report §12 team table.

---

## 4. Task 4 -- Fallback materials

`dev-docs/presentation/phase-18-fallbacks_Sierra-Lima.md`
covers all four sub-bullets of Phase 18 task 4:

| Phase 18 task 4 sub-bullet | Fallback section |
|----------------------------|------------------|
| Screenshots | §3.2 (Swagger UI), §3.5 (resilience), §6 (commit checklist) |
| Pre-recorded flow | §1.3 (CP#1 backup recording fallback) |
| Backup seed data | §4 (owners + restaurants + items + sentinel UUIDs) |
| Recovery commands | §5 (cheat-sheet) + §1.1 / §1.2 (re-up + hard reset) + §2 (per-container restart) |

Per-beat fallback paths are documented in §3 so each demo step
has a known recovery: browser dies -> Postman; Postman dies ->
Newman in terminal; Newman dies -> last-green Phase 10 report;
cross-service smoke exits non-zero -> exit-code-table mapping
to "presentable" vs "stop-now" failure modes.

The screenshot pack (§6 of the fallback doc) lists the six
filenames to capture at the 2026-05-18 rehearsal:

- `dev-docs/verification/swagger-restaurant.png`
- `dev-docs/verification/swagger-menu.png`
- `dev-docs/verification/negative-auth-401.png`
- `dev-docs/verification/negative-auth-403.png`
- `services/local-dev/evidence/cross-service-smoke_<RUN_TAG>.log`
- `services/local-dev/evidence/menu-events_<RUN_TAG>.log`

(Plus optional `resilience-circuit-open.png` /
`resilience-circuit-closed.png` if the resilience demo runs
cleanly.) None of these gate the deck or demo script; the
references resolve once the screenshots land.

---

## 5. Task 5 -- Q&A rehearsal

`dev-docs/presentation/phase-18-qa-prep_Sierra-Lima.md` answers
the seven anticipated questions from Phase 18 task 5, each with
**<= 45 s** spoken (~120 words), structured as headline + why +
cite. Coverage:

| # | Phase 18 task 5 question | Answerer | Backup |
|---|--------------------------|----------|--------|
| Q1 | Why these seven services implemented? | Alfa-Kilo | Sierra-Lima |
| Q2 | Why is Review design-only? | Alfa-Kilo | Sierra-Lima |
| Q3 | How were service boundaries chosen? (DDD, Assignment 2) | Alfa-Kilo | Mike-Alfa |
| Q4 | How does async integration work? Topics, envelope, idempotency | Mike-Alfa | Elephant-Yankee |
| Q5 | How was security enforced at gateway vs service level? | Sierra-Lima | Alfa-Kilo |
| Q6 | Why no Eureka? | Alfa-Kilo | Sierra-Lima |
| Q7 | What did you sacrifice and why? | Alfa-Kilo | Sierra-Lima |

Three likely follow-ups (F1 tests pass, F2 menu-events envelope,
F3 token expiry handling) are pre-rehearsed too; each has a
named primary answerer. Drill protocol (§ "Drill protocol for the
rehearsal") tells the team how to time and tighten each answer
at the 2026-05-18 slot.

---

## 6. Definition of Done roll-up

- [x] **Slides complete.** 16 slides covering all eight Phase 18
      task 1 bullets, plus cover / catalogue / sacrifices /
      closing scaffolding. Source is Markdown; rendering target
      (Marp / pandoc / Slides) is a no-op transformation since
      every slide quotes from the Phase 17 report.
- [ ] **Live demo rehearsed at least once.** **Pending the
      2026-05-18 team rehearsal.** The demo script
      (`phase-18-demo-script_Sierra-Lima.md`) is keystroke-ready
      with expected outputs at each beat; Sierra-Lima can self-
      rehearse the §1-§5 portion solo against the local stack
      ahead of the team rehearsal. The §6 cross-service portion
      requires teammate `*_BASE` env vars and runs at the team
      rehearsal slot. This is the only DoD bullet that is not
      satisfied at the time of this verification note; the
      24-hour gap between rehearsal and graded run is by design.
- [x] **Every presenter knows what to say and click.** The deck
      preamble (speaking-part assignment table) and each slide's
      `Hand-off:` field name the speaker per slide. The Q&A drill
      names the answerer per question. The demo script names the
      driver per beat. Every callsign has a backup partner so a
      missing speaker isn't a single point of failure.
- [x] **Backup materials ready.**
      `phase-18-fallbacks_Sierra-Lima.md` covers screenshots,
      recovery commands, backup seed data, per-beat fallback
      paths, and the rehearsal-slot capture checklist.

---

## 7. Known follow-ups for Phase 19 (or for the rehearsal slot)

These are the only outstanding items between this verification
note and the 2026-05-19 graded run.

- **Run the live rehearsal.** Schedule: 2026-05-18, ~90 min.
  Walk the deck top-to-bottom; run §0-§6 of the demo script
  end-to-end; drill all seven Q&A questions. Tighten any answer
  that exceeds 45 s aloud.
- **Capture and commit the screenshot pack** during that
  rehearsal -- six filenames listed in §4 of this note. Drop
  them under `dev-docs/verification/` and
  `services/local-dev/evidence/`; the deck and demo script
  reference them by relative path so no edits are needed.
- **Export the Phase 17 report to PDF** for the hand-in
  (`pandoc --from gfm`, VS Code "Markdown PDF", or print-to-PDF
  from GitHub). The export is mechanical; report content has
  been frozen since Phase 17.
- **Confirm speaking-part allocations** with Alfa-Kilo,
  Elephant-Yankee, Mike-Alfa at the rehearsal kick-off. The
  current allocation in the deck is consistent with the report
  §12 team table but each callsign owns the final word on their
  own delivery.
- **Resilience demo go/no-go.** If S14 runs cleanly at
  rehearsal, capture the two screenshots and keep S14 live on
  the day. If not, leave S14 as narrated text and skip live --
  the slide is explicitly marked "optional, time-permitting"
  in both the deck preamble and the talk track.

---

## 8. Verification of artefacts

| Artefact | Path | Status |
|----------|------|--------|
| Slide deck | `dev-docs/presentation/phase-18-slides_Sierra-Lima.md` | Drafted |
| Demo script | `dev-docs/presentation/phase-18-demo-script_Sierra-Lima.md` | Drafted |
| Fallbacks | `dev-docs/presentation/phase-18-fallbacks_Sierra-Lima.md` | Drafted |
| Q&A drill | `dev-docs/presentation/phase-18-qa-prep_Sierra-Lima.md` | Drafted |
| Verification note (this file) | `dev-docs/verification/phase-18-verification_Sierra-Lima.md` | Drafted |
| Session archive | `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_<sha>.md` | Pending commit |

All cross-references in the deck, demo script, fallbacks, and
Q&A drill resolve to existing repository paths (verified against
the tree at base commit `54087e4`). The screenshot placeholders
in the deck (`dev-docs/verification/swagger-*.png`,
`negative-auth-*.png`) are intentional Phase 18 outputs that
land at the rehearsal slot without report or deck edits.
