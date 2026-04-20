# Verification

Per-phase verification reports: evidence that each phase of the
master plan has met its Definition of Done. Each file names the
phase it covers and the author callsign that ran the verification.

## File-naming

```
phase-<N[-to-M]>-verification_<callsign>.md
```

- `<N>` -- the single phase verified (e.g. `phase-10-verification_...`).
- `<N-to-M>` -- a span of phases verified in one sitting
  (e.g. `phase-2-to-6-verification_...`).
- `<callsign>` -- the authoring AI agent (e.g. `Charlie-Lima-Alfa`,
  `Golf-Papa-Tango`) or `Sierra-Lima` for the human-authored span
  in Phase 2-6. Not a service owner; see
  [`../README.md`](../README.md#file-naming-conventions).

Supporting evidence (PNG screenshots, raw `docker compose` logs)
lives in the same folder with neutral names (`img.png`, `img_1.png`)
or in `.tmp-*` subfolders that capture the exact command output from
a verification run.

## Contents

| File | Phase | Anchor |
|---|---|---|
| `phase-2-to-6.md` | Phases 2-6 (compose scaffold → first endpoints) | Author: human Sierra-Lima. |
| `phase-2-to-6-verification_Sierra-Lima.md` | Phases 2-6 | Human verification, full narrative. |
| `phase-2-to-6-verification_Golf-Papa-Tango.md` | Phases 2-6 | AI re-verification, same scope. |
| `phase-7-verification_*.md` | Phase 7 (JWT auth) | Security matrix evidence. |
| `phase-8-verification_*.md` | Phase 8 (Dockerisation) | Compose healthcheck + `.dockerignore` evidence. |
| `phase-9-verification_*.md` | Phase 9 (Smoke scripts) | `smoke.sh` / `smoke.ps1` evidence. |
| `phase-10-verification_*.md` | Phase 10 (W1 Integration) | Cross-service smoke evidence + Postman W1 folder. |
| `phase-11-verification_*.md` | Phase 11 (Role matrix + error envelope) | 4xx coverage evidence. |
| `phase-12-verification_*.md` | Phase 12 (Frontend scaffold) | Vue 3 + Router + auth guard evidence. |
| `phase-14-verification_*.md` | Phase 14 (Frontend CRUD + role gating) | End-to-end browser probe evidence. |
| `phase-16-verification_*.md` | Phase 16 (Async stance + `menu-events`) | Log-only producer evidence per `0040`. |
| `phase-17-verification_*.md` | Phase 17 (Documentation polish) | README + runbook + OpenAPI coverage. |
| `phase-18-verification_*.md` | Phase 18 (Presentation pack) | Slides + demo script + Q&A + fallbacks. |
| `phase-19-verification_*.md` | Phase 19 (Final handover) | Pre-handover audit tie-out. |
| `img*.png` | Supporting screenshots referenced inline. | -- |
| `.tmp-phase-2-to-6-golf-papa-tango/` | Raw logs from the phase-2-to-6 Golf-Papa-Tango verification run. | -- |

Phases 0, 1, 13, 15 intentionally do not have verification files:
they are scope / design / contract phases whose DoD is satisfied by
a decision record rather than runtime evidence.

## File shape

Each verification report follows this skeleton:

1. **Metadata table** -- phase number, phase title, base commit,
   auditor, date.
2. **Scope** -- which master-plan tasks the file verifies.
3. **Evidence** -- per task, the commands run and the observed
   output, plus any screenshots.
4. **DoD checklist** -- one row per DoD criterion, with `Met` /
   `Partial` / `Gap` + evidence pointer.
5. **Sign-off** -- one-line verdict ("Phase N DoD met at commit
   `<SHA>`") or a list of remaining items.

## How to use these

- **Human reviewer asking "was Phase N actually completed?"** Read
  the corresponding `phase-<N>-verification_*.md` file; the sign-off
  line is the answer.
- **Checkpoint defence.** If an instructor asks "how do you know W1
  works?", `phase-10-verification_*.md` is the answer, supported by
  the latest audit.
- **AI agent preparing to move on to Phase N+1.** Verify that Phase
  N has a sign-off first; if not, close the gap before moving on.

## Conventions

- **Verification reports are append-only** -- do not edit them to
  reflect later state; write a new audit instead.
- **Evidence must reproduce.** Every command in a verification file
  must succeed against the named base commit if re-run.
- **Screenshots live next to the report.** If `img_3.png` is added,
  the file that references it stays in the same folder.

## Related folders

- [`../roadmaps/`](../roadmaps/) -- the master plan defines each
  phase's DoD.
- [`../audits/`](../audits/) -- readiness audits cite verification
  reports as primary evidence.
- [`../agent-context/`](../agent-context/) -- the session archive
  for the run that produced a verification file.
