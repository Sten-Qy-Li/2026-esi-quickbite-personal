# Dev Docs

Developer-facing documentation for Sierra-Lima's slice of the QuickBite
ESI project -- context, decisions, audits, roadmaps, verification logs,
and checkpoint talking points.

**Audience.** Human programmers (Sierra-Lima, Group 7 team lead, future
Group 7 members) and coding agents (Claude Code, IDE assistants). Both
should be able to find authoritative project state here without having
to re-read chat archives from scratch.

**Code vs. docs.** Anything under `../services/` is executable and
under test. Anything under this directory is documentation: it
describes intent, rationale, or evidence, never runtime behaviour.

## Directory map

| Folder | Purpose | Authoritative for... |
|---|---|---|
| [`agent-context/`](agent-context/) | Dated chat-archive logs from AI-coding-agent sessions. | Reproducing why a commit was made, not what the code currently does. |
| [`audits/`](audits/) | Readiness audits at named commits (pre-integration, final-handover, pre-team-integration). | Snapshot of which tests/smokes/Newman runs were green on which commit, and which findings were open. |
| [`course-materials/`](course-materials/) | Course PDFs and weekly-exercise source links. | Only for reference -- **do not re-derive requirements from here**; they are already pinned in `decisions/` and the roadmaps. |
| [`decisions/`](decisions/) | ADR-style decision records (`0001-` ... `0040-`). | Scope, workflows, conventions, API/event contracts, non-goals. **These are the contracts.** |
| [`gap-analysis/`](gap-analysis/) | Comparisons of the Project Brief vs. a named commit. | Which brief requirements are Met / Partial / Gap at a given moment. |
| [`presentation/`](presentation/) | Phase-18 demo script, slides, Q&A prep, fallback notes. | Ingredients for the checkpoint demos -- not executable. |
| [`prior-submissions/`](prior-submissions/) | Submitted Assignment 1-3 PDFs/DOCX + figures. | Immutable record of what the instructor already graded. |
| [`roadmaps/`](roadmaps/) | Master plan versions (phase-by-phase). The "final" is `Charlie-Lima-Alfa_a520963_project-phases-final.md`. | End-to-end phase structure, tasks, DoD. |
| [`verification/`](verification/) | Per-phase verification reports + supporting logs/screenshots. | Evidence that a given phase was completed to DoD. |

Top-level files:

- [`checkpoint-1-talking-points.md`](checkpoint-1-talking-points.md)
  -- CP#1 (backend, 2026-05-05) demo script outline.
- [`checkpoint-2-talking-points.md`](checkpoint-2-talking-points.md)
  -- CP#2 (frontend + backend, 2026-05-12) demo script outline.
- [`report-draft-backend_Sierra-Lima.md`](report-draft-backend_Sierra-Lima.md)
  -- draft of the CP#1 backend report section.

## File-naming conventions

Most artefacts encode their author and/or the commit they describe in
the filename. Conventions vary by folder; the header below applies
across the tree:

- `YYYY-MM-DD_<kind>_<callsign>_<commit>.md` -- agent-context chat
  archives. `<commit>` is the short SHA of the commit that session
  produced (or the last one it observed).
- `audit-<commit>_<callsign>_<scope>.md` -- audit reports. A
  numeric suffix (`_1`, `_2`, `_3`) disambiguates multiple audits on
  the same commit by the same author.
- `gap-analysis-<commit>_<callsign>_<scope>.md` -- project-brief-vs-repo
  gap analyses.
- `phase-<n>-verification_<callsign>.md` -- per-phase verification
  reports.
- `<callsign>_<commit>_project-phases*.md` -- master-plan roadmap
  revisions.

**Callsigns are authors, not service owners.** `Charlie-Lima-Alfa`
and `Golf-Papa-Tango` are AI coding agents (different Claude Code
sessions) that authored these artefacts for Sierra-Lima. Sierra-Lima
is the only human owner of this workspace. Treat artefacts written by
other callsigns as read-only historical evidence; do not rewrite
existing audits or chat archives to "improve" them.

## When you (human or agent) land here

1. **Looking for the current contract?** Start in
   [`decisions/`](decisions/). The files are numbered in reading
   order: `0001` (scope freeze) -> `0040` (async stance).
2. **Looking for "does the code actually do X?"** Go to
   [`../services/`](../services/) (the code) and, for a point-in-time
   verdict, the latest audit in [`audits/`](audits/).
3. **Writing a new audit / gap analysis / verification?** Match the
   filename convention above and link the commit you audited.
4. **AI agent starting a session?** Read the latest chat archive in
   [`agent-context/`](agent-context/) for recent context, then the
   latest audit for the current state. At the end of the session,
   write a new chat-archive file with today's date and the tip commit.

## What is **not** here

- **Runtime config.** All environment variables, ports, images, and
  compose topology live under [`../services/local-dev/`](../services/local-dev/).
- **Tests.** Tests are next to the code under each service's
  `src/test/java/`.
- **Teammates' services.** Alfa-Kilo, Elephant-Yankee, and Mike-Alfa
  each own their own code repositories; this workspace only references
  their services by callsign, contract, and port.
