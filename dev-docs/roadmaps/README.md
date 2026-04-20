# Roadmaps

Successive revisions of Sierra-Lima's master plan -- the end-to-end,
phase-by-phase implementation schedule for Restaurant Service, Menu
Service, their local-dev stack, and Sierra-Lima's share of the
frontend.

Only **one** of these files is the "current" master plan; the others
are earlier drafts retained for history and traceability.

## File-naming

```
<callsign>_<base-commit>_project-phases[-final].md
```

- `<callsign>` -- the authoring AI agent. See
  [`../README.md`](../README.md#file-naming-conventions). Not a
  service owner.
- `<base-commit>` -- the commit SHA the revision was based on.
- `-final` suffix -- applied to the active master plan. Exactly one
  file in this folder has this suffix at any time.

## Current master plan

**`Charlie-Lima-Alfa_a520963_project-phases-final.md`** is the
active master plan. Every phase verification, audit, presentation
script, and decision lock refers back to it. When a document says
"Phase 2 Task 10" or "Appendix F.2", the reference is to this file.

## File shape

Each roadmap file covers (in reading order):

1. **Metadata header** -- author, base commit, student callsign,
   date.
2. **Scope + ownership reminder** -- what is in scope for Sierra-Lima
   under decision `0001`.
3. **Phase list** -- Phase 0 (scope freeze) through Phase 19
   (post-CP#3 wrap). Each phase has numbered tasks, a clearly stated
   Definition of Done, and a reference to the decision(s) it
   implements.
4. **Appendices** -- API contracts (F.1, F.2), ER diagrams, W1 hop
   diagrams, port matrices, env-var matrices, test inventories, demo
   scripts.

## How to use these

- **Human planner or reviewer.** Read the `-final` file. Earlier
  revisions are only relevant if you need to trace why a phase
  shape changed.
- **AI agent planning a new phase.** Read the phase you are about to
  execute, including its DoD. Do not invent new phases; if scope
  expands, amend the master plan in a new revision (and retire the
  current `-final`).
- **Team lead integrating the workspace.** The master plan is
  Sierra-Lima-specific; it is not a group-wide plan. For group
  integration, the most useful sections are the port and env-var
  matrices in the appendices and the W1 hop diagrams.

## Conventions

- **Roadmaps are snapshots.** When a major shift happens, write a
  new file with the new base-commit SHA; flip the `-final` suffix to
  the new one; leave the old file in place.
- **Every phase references a decision.** A phase that does not map
  to a decision record should not exist -- the phase should be
  preceded by a decision landing.
- **Phase numbering is stable across revisions.** Phase 10 means the
  same thing in every revision, even as its tasks drift.

## Related folders

- [`../decisions/`](../decisions/) -- the contracts each phase
  implements.
- [`../verification/`](../verification/) -- per-phase DoD evidence.
- [`../audits/`](../audits/) -- cross-phase readiness checks.
- [`../presentation/`](../presentation/) -- CP#3 demo built on top
  of the completed phases.
