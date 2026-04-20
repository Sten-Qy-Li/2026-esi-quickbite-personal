# Chat Archive - 2026-04-20 - Charlie-Lima-Alfa (`7b2fa61`)

## Session Summary

Short, single-purpose session. The user dropped a new document --
`dev-docs/course-materials/Project2026.pdf` -- into the repository
and asked for a gap analysis against the current state at commit
`7b2fa61`. No code changes were requested and none were made. One
new Markdown report was produced, with the Charlie-Lima-Alfa
callsign in the filename per the user's instruction.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Today: 2026-04-20 (Monday)
- Active branch: `dev`
- Upstream at session open: `origin/dev` at `7b2fa61` ("Patch Golf-Papa-Tango 60fa710 audit_3 F1-F2")
- Working tree at session open: clean of modifications, 8 untracked files
  (one new course-materials PDF, one Golf-Papa-Tango audit from the
  same wall-clock morning, six evidence logs from 2026-04-19 and
  2026-04-20 smoke runs).
- Session model: Opus 4.7, max effort

## Requests In This Session

1. Read the Project Brief at `dev-docs/course-materials/Project2026.pdf`,
   compare the current state of the repository against it, and write
   a Markdown audit under `dev-docs/gap-analysis/` with a filename
   including the Charlie-Lima-Alfa callsign.
2. Archive this session's context under `dev-docs/agent-context/`
   and commit + push the repo, excluding only by best judgement.

## Project Brief Key Points (as parsed)

Four-page document, `Enterprise System Integration -- Project Guidelines 2026`.

- Total: **30 points** over three checkpoints -- CP#1 (12 pts, 05 May),
  CP#2 (8 pts, 12 May), CP#3 (10 pts, 19 May). Commits freeze 14:00
  Estonian time, discussions 14:15 same day.
- §1 Objective: **implement** Assignments 1/2/3 as already submitted;
  not a redesign.
- §2 Individual Responsibilities: each student either 2 microservices
  OR 1 microservice + 1 integration/resilience component; frozen at
  A3.
- §3 Technical Requirements:
  - 3.1 System-level: multiple interacting services, a frontend, basic
    security (authN + authZ), Dockerised run.
  - 3.2 Per-service: 5-layer (Controller/DTO/Service/Repository/Domain),
    own DB, ~5-8 REST endpoints, at least one async workflow.
- §4 Checkpoints:
  - 4.1 CP#1: Running Service / API Implementation / Swagger / Persistence /
    `@WebMvcTest` with cross-service dep / Postman or Swagger demo.
    Grading split 7+2+2+1.
  - 4.2 CP#2: Second service or integration component (A+B+D of CP#1,
    no tests/docs required), one real (not mocked) integration,
    frontend calling at least one endpoint per student with real data.
    Grading split 4+2+2.
  - 4.3 CP#3: Two services complete + Docker Compose + frontend fully
    functional + security + "at least one event/message-based interaction
    between services". Grading split 3+2+2+3.

## Artefact Produced

`dev-docs/gap-analysis/gap-analysis-7b2fa61_Charlie-Lima-Alfa_project-brief-vs-repo.md`

Structure:

1. Metadata + purpose framing (two rules: "implementing, not
   designing"; "graded individually -- Sierra-Lima's slice only").
2. Evidence list -- Brief pages plus the repo anchors re-read at
   `7b2fa61`.
3. Coverage matrix split by Brief section (3.1 system, 3.2 per-service
   for each Sierra-Lima service, 4.1 CP#1, 4.2 CP#2, 4.3 CP#3), each
   row tagged Met / Partial / Gap with a file-reference evidence column.
4. Deadline position: today is +15/+22/+29 days from the three
   deadlines; all three checkpoints defendable from `7b2fa61` with
   time to spare.
5. Four findings (F1 Medium, F2 Low, F3 Low, F4 Informational).
6. Explicit non-gaps -- items a naive reading of the Brief might
   flag, annotated with the scope-freeze or decision that keeps
   them out of scope.
7. Verdict plus four ordered next-action recommendations.

## Findings (summary)

- **F1 Medium -- Async transport is log-only; no bytes cross services.**
  `LoggingMenuEventPublisher` emits the `0032 §6` envelope to logger
  `menu-events`, not to a broker. Satisfies "envelope exists + emit
  rule fires" per `0040 §2`; does not satisfy Brief §4.3 E "between
  services" in transport terms. Residual risk is contingent on
  teammate Kafka/W2/W3 demo landing. Recommendation: confirm teammate
  commitment first, prepare a `KafkaMenuEventPublisher` behind an
  env-flag fallback if at risk.
- **F2 Low -- Three endpoints have no UI trigger.** `DELETE
  /menu-items/{id}`, `POST /menu-items/validate`, and `GET
  /restaurants/{id}/availability`. The last two are B2B-by-design
  (Order Service consumer); `DELETE /menu-items/{id}` is the one
  genuinely UI-missing path. Recommendation: add a delete control
  to `MenuItemDetailView.vue` before 19 May.
- **F3 Low -- "Full system in Docker" depends on the team's combined
  compose assembly.** Sierra-Lima's compose boots her own slice +
  `dev-gateway` stub; the real gateway, the broker, and the five
  teammate services are not in this repo by design (`0001`).
  Recommendation: ask the team lead who owns the combined compose.
- **F4 Informational -- Untracked evidence artefacts** at session
  open. Normal cadence; logs are safe to commit (plain-text test
  traces).

## Explicit Non-Gaps Called Out

- No Spring Cloud Gateway code -- Alfa-Kilo's component.
- No Kafka dep in `menu-service/pom.xml` -- `0040 §2` keeps it out.
- `Review Service` absent -- `0001 §3` flags design-only.
- Restaurant Service emits no events -- `0040 §1` keeps it a
  non-participant in W2/W3.
- Newman's `PUT /restaurants/{id}` has no assertion -- already
  tracked as Golf-Papa-Tango F1 at `7b2fa61`, not duplicated here.
- Frontend operating-hours regex looser than backend -- already
  tracked as Golf-Papa-Tango F2 at `7b2fa61`, not duplicated here.

## Evidence Gathered

- Project Brief read at the pdf path above (4 pages, rendered in
  full via the PDF reader tool).
- `7b2fa61` Golf-Papa-Tango audit read for continuity of prior
  findings and baseline live-verification numbers (33/33 + 47/47
  tests, 39/68/0 Newman).
- Decision pack read or re-read: `0001` scope freeze, `0010` auth,
  `0020` contracts, `0040` phase-16 async stance.
- Controllers, service-layer, security-config, and OpenAPI config
  for both Sierra-Lima services re-read to confirm endpoint,
  validation, and auth coverage.
- Frontend router + `src/api/client.js` + all owner/browse views
  grepped to map UI → endpoint coverage (9/12 endpoints reachable,
  3 gaps with different defences -- see F2).
- `services/local-dev/docker-compose.yml` re-read for the "full
  system running" framing (F3).
- `.gitignore` re-read so the evidence-log exception
  (`!services/local-dev/evidence/*.log`) is honoured during the
  commit step.

No live execution in this session. No backend tests were re-run;
the Golf-Papa-Tango `7b2fa61` audit's green numbers are current
enough for the gap-analysis claims.

## Files Changed This Session

Added:

- `dev-docs/gap-analysis/gap-analysis-7b2fa61_Charlie-Lima-Alfa_project-brief-vs-repo.md`
  -- the gap-analysis report itself.
- `dev-docs/agent-context/2026-04-20_chat-archive_Charlie-Lima-Alfa_7b2fa61.md`
  -- this file.

Committed as part of this session but **not authored in it** (carried
over from earlier wall-clock work on 2026-04-19 and 2026-04-20):

- `dev-docs/course-materials/Project2026.pdf` -- the user dropped
  the Brief into the repo before opening this chat. Same directory
  already tracks the A1/A2/A3 prompts and the lecture decks; the
  Brief is the matching rubric document, so committing it fits the
  pattern.
- `dev-docs/audits/audit-7b2fa61_Golf-Papa-Tango_team-lead-integration-readiness_1.md`
  -- Golf-Papa-Tango's own integration-readiness audit from the same
  morning. Preserved verbatim per the feedback memory
  `preserve-other-callsign-files` (treat other-callsign artefacts as
  read-only; commit verbatim when asked).
- `services/local-dev/evidence/cross-service-smoke_20260419T190125Z.log`,
  `services/local-dev/evidence/cross-service-smoke_20260420T051833Z.log`,
  `services/local-dev/evidence/cross-service-smoke_20260420T054823Z.log`,
  `services/local-dev/evidence/menu-events_20260419T190125Z.log`,
  `services/local-dev/evidence/menu-events_20260420T051833Z.log`,
  `services/local-dev/evidence/menu-events_20260420T054823Z.log`
  -- six smoke-run evidence logs. The `.gitignore` rule
  `!services/local-dev/evidence/*.log` explicitly carves them out of
  the global `*.log` ignore; they are Phase 16+ DoD evidence by
  design. No sensitive content (UUIDs and timestamps only).

Not committed / excluded by judgement: **none**. Working tree had no
`.env.local`, no secrets, no large binaries, no scratch-only files.
The one file that could be debated is `Project2026.pdf` -- 120KB of
course material, functionally a reference document -- but the
`dev-docs/course-materials/` convention already tracks every other
such PDF, so excluding this one would be the inconsistent choice.

## Notes for the Next Session

- **Sierra-Lima has buffer to every deadline.** Today is 2026-04-20.
  CP#1 is 2026-05-05 (+15d), CP#2 is 2026-05-12 (+22d), CP#3 is
  2026-05-19 (+29d). The gap analysis concludes all three are
  defendable from `7b2fa61` with time to spare; the time between now
  and CP#1 should go to integration rehearsal, not new feature work.
- **F1 is the one decision that still needs team input.** Before any
  code change on the async-transport question, confirm with Mike-Alfa
  whether Kafka + teammate producers/consumers will exist by CP#3.
  Record the answer in a new decision (`0041-cp3-async-demo-commitment.md`
  or similar) before acting. If the confirmation is yes, Sierra-Lima
  needs no code change; if no, `KafkaMenuEventPublisher` behind an
  env flag per `0040 §2` is the right next step.
- **F2 delete-button is a single-view change** and should be closed
  opportunistically rather than left on the risk register. Wire
  `api.delete('/api/menu-items/{id}')` to a button in
  `MenuItemDetailView.vue` (owner/admin-only), refresh the menu
  listing on success.
- **F3 is a cross-team ask, not a code change.** Next team lead sync
  is the right place to surface it.
- **No open items in Golf-Papa-Tango's `7b2fa61` audit** for
  Sierra-Lima's slice that are not already acknowledged here. Their
  F1 (Newman `PUT` positive path) and F2 (frontend hours regex) are
  in the explicit-non-gaps section of the gap analysis.
- **Commit + push step intentionally deferred** to a second round of
  the user's own instruction at session wrap; the archive above is
  authored in advance of that commit, describing the state that is
  about to land.
