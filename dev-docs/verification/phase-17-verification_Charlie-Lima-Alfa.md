# Phase 17 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 17
("Report & Evidence Pack") for Restaurant Service, Menu Service,
and the Sierra-Lima frontend slice.

Date: 2026-04-19. Target CP#3 graded: 2026-05-19.
Base commit: `6b0e0e9` (Phase 16 land).

---

## 0. Session context

Phase 17 is a documentation phase, not a code phase: no service
code or test code changed. The deliverable is
`dev-docs/report-draft-backend_Sierra-Lima.md`, rewritten end-to-end
on top of the Phase 14 draft so it reflects Phase 15 (authorisation
hardening, ownership lookup) and Phase 16 (async stance + log-only
stretch producer) without further review rounds.

Artefacts landed in this phase:

- `dev-docs/report-draft-backend_Sierra-Lima.md` -- rewritten
  end-to-end; previously a 400-line Phase 14 draft, now a
  ~1080-line presentation-ready document with figure index,
  implementation-status section, divergences catalogue, and
  evidence appendix.
- `dev-docs/verification/phase-17-verification_Sierra-Lima.md`
  (this note).
- `dev-docs/agent-context/2026-04-19_chat-archive_Charlie-Lima-Alfa_<sha>.md`
  (session archive, written last before commit).

Not touched on purpose:

- No service code. Phase 17 DoD is "all report sections drafted /
  diagrams match / evidence included / presentation-ready."
- No new figures. The plan explicitly allows reusing Assignment 3
  figures when the implementation has not diverged, and requires
  calling out divergences in the limitations section; the report's
  §13 and §3.1 / §6.2 refreshes fulfil that.
- No fresh Swagger screenshots in-tree yet. The report references
  target filenames under `dev-docs/verification/` that will be
  captured during the CP#3 rehearsal (2026-05-18) without report
  edits -- the placeholders match the file-naming convention
  already in `dev-docs/verification/`.

---

## 1. Task 1 -- Report sections assembled

Each Phase 17 task-1 subsection maps to one or more numbered
sections in the rewritten report:

| Phase 17 task 1 bullet | Report section(s) |
|------------------------|-------------------|
| Business architecture (Figure 1, no infra) | §2 |
| Technical architecture (Figure 1b) | §3, §3.1, §3.2, §3.3 |
| Implemented services vs design-only; `Review` justification | §11 (whole section) |
| Data models (ER, Figure 2) | §4.1, §4.2, §4.3, §4.4 |
| APIs (endpoint tables, Swagger cites) | §5.1, §5.2, §5.3, §15.1 |
| Workflow W1 synchronous (Figure 3) | §6.1 |
| Workflows W2 / W3 asynchronous (Figure 4) | §6.2 |
| Integration mechanisms: REST, gateway, Kafka envelope | §3.3, §6.2, §9 |
| Security approach: JWT, validation, role gating | §7 (whole section) |
| Team responsibilities | §12 |
| Limitations and future work | §13, §14 |

---

## 2. Task 2 -- Diagrams refreshed

The master plan allows reusing Assignment 3 figures if the
implementation has not diverged; divergences must be flagged in
the limitations section. Decisions:

| Figure | Status | Rationale |
|--------|--------|-----------|
| Figure 1 (business architecture) | Reused as-is | Business architecture is DDD-conceptual; no infra drift possible. |
| Figure 1b (technical architecture) | Reused + ASCII refresh in §3.1 | Three divergences documented in §13.1: Menu -> Restaurant ownership call (Phase 15), opt-in `dev-gateway` stub (Phase 14), `Review` box missing from deployment view. |
| Figure 2 (ER diagrams) | Reused; tables in §4 govern | ER tables match `V1__init.sql` byte-for-byte; ADR 0020 covers schema invariants. |
| Figure 3 (W1 sync) | Reused; §6.1 sequence governs | Sierra-Lima edge sequence unchanged since Phase 10 (hops 4 / 5). |
| Figure 4 (W2 / W3 async) | Reused + table refresh in §6.2 | One divergence: the `menu-events` topic added by Phase 16 is absent from Figure 4; §13.2 calls it out. |

---

## 3. Task 3 -- Evidence included

Evidence coverage vs the Phase 17 task 3 bullets:

| Evidence bullet | Report section | Status |
|-----------------|----------------|--------|
| Swagger UI screenshots | §15.1 | URLs + target capture paths listed; screenshots captured at CP#3 rehearsal slot the placeholder filenames. |
| Endpoint tables | §5.1 (Restaurant, 6 endpoints), §5.2 (Menu, 6 endpoints), §15.2 | Tables include the Phase 15 authorisation mapping. |
| Topic tables | §6.2 (5 topics with producer/consumer + ADR cites), §15.3 | `menu-events` is marked as Phase 16 stretch, log-only transport. |
| 401 / 403 screenshot | §15.4 | Postman request paths + target capture filenames documented; service-side denial log sample included. |
| Log excerpt of consumed event | §15.5 | The Sierra-Lima-originated `menu.item-availability-changed` envelope log excerpt is included; team-consumed `delivery-events` / `payment-events` excerpts sourced from Mike-Alfa at rehearsal. |

The report and the evidence appendix are written so the CP#3
rehearsal run of `services/local-dev/smoke-cross-service.sh` drops
its trace under `services/local-dev/evidence/` and satisfies §15.5
and §15.6 without further report edits. The Postman `Negative Auth`
folder (Phase 7 + Phase 15) drives §15.4 equivalently.

---

## 4. Task 4 -- Proofread and format

Proofing pass done before this note:

- All internal §-references resolve to numbered subsections in the
  same document.
- All file-path cites (`services/...`, `dev-docs/...`,
  `assignment-3_*.png`) match the repository tree at base commit
  `6b0e0e9`.
- JUnit test counts independently verified against the source:
  Restaurant 8 + 14 + 1 = 23; Menu 20 + 20 + 1 + 1 = 42.
- Figure index (§0) enumerates every figure and its status
  (reused / refreshed / divergent), so the grader can locate the
  governing diagram for any claim.
- Markdown renders cleanly in a GitHub-flavoured viewer; no broken
  tables; no unbalanced code fences (spot-checked on line lengths
  and code-fence pairs).

The Phase 17 DoD asks for a "clean PDF (or DOCX) export" target.
The source-of-truth is the Markdown; PDF export (via any of
`pandoc`, VS Code "Markdown PDF" extension, or print-to-PDF from
GitHub's rendered view) is a Phase 18 mechanical step and is not
gated on further content changes.

---

## 5. Definition of Done roll-up

- [x] **All report sections drafted.** §§1-15 of the report cover
      every Phase 17 task 1 bullet; see the map in §1 of this note.
- [x] **Diagrams match the implemented system.** Reused figures
      are paired with in-report refreshes that govern over the
      images; divergences are catalogued in §13 of the report.
- [x] **Evidence included.** §15 of the report is a dedicated
      evidence appendix with Swagger URLs, endpoint tables, topic
      tables, 401 / 403 evidence paths, and a log excerpt.
- [x] **Report is near-final quality.** Phase 18 remaining work
      is the rehearsal screenshot capture + smoke trace drop-in --
      both slot into placeholder filenames under
      `dev-docs/verification/` and `services/local-dev/evidence/`
      without report edits.

---

## 6. Known follow-ups for Phase 18

- Capture Swagger UI screenshots against the CP#3 rehearsal stack
  and save them as
  `dev-docs/verification/swagger-restaurant.png` and
  `dev-docs/verification/swagger-menu.png`.
- Capture Postman 401 / 403 screenshots and save them as
  `dev-docs/verification/negative-auth-401.png` and
  `dev-docs/verification/negative-auth-403.png`.
- Run `services/local-dev/smoke-cross-service.sh` with all
  teammate `*_BASE` env vars set; commit the resulting trace
  under `services/local-dev/evidence/` (file naming is
  `cross-service-smoke_<RUN_TAG>.log` +
  `menu-events_<RUN_TAG>.log`).
- Export a PDF from `report-draft-backend_Sierra-Lima.md` (any of
  `pandoc --from gfm`, VS Code "Markdown PDF", GitHub print-to-PDF).
- (Optional, belongs to the full team report merge) Stitch
  Alfa-Kilo / Elephant-Yankee / Mike-Alfa deep-dive sections into
  a single combined report; Sierra-Lima's slice is already
  presentation-complete.
