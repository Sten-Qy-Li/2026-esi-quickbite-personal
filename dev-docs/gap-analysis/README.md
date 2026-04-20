# Gap Analysis

Side-by-side comparisons of the ESI Project Brief (and, historically,
Assignment 3) against the state of this repository at a named commit.
Each gap analysis answers one question: **"Which brief requirements
are Met / Partial / Gap at commit `<X>`?"**

## File-naming

```
gap-analysis-<commit>_<callsign>_<scope>.md
```

- `<commit>` -- the short SHA the analysis ran against.
- `<callsign>` -- the authoring AI agent. Not a service owner; see
  [`../README.md`](../README.md#file-naming-conventions).
- `<scope>` -- the source document being compared against. Seen so
  far: `project-brief-vs-repo`.

## How this differs from an audit

| Aspect | Audit ([`../audits/`](../audits/)) | Gap analysis (this folder) |
|---|---|---|
| **Question** | "Does the code pass its tests and smokes?" | "Does the code deliver what the Brief requires?" |
| **Source of truth** | The code itself, under test. | The Brief / Assignment PDF. |
| **Output** | Go/no-go verdict + findings. | Met / Partial / Gap matrix + remediation list. |
| **When to run** | Before handover, before integration, before a checkpoint. | When the Brief or Assignment changes, or when scope drifts. |
| **Severity** | Low / Medium / High. | Blocker for CP#1 / CP#2 / CP#3, or nice-to-have. |

Gap analyses and audits complement each other: an audit confirms the
code does what it is written to do; a gap analysis confirms it is
written to do the right thing.

## Shape (for authors)

Each gap-analysis file follows this skeleton:

1. **Metadata table** -- commit SHA, branch, author, date, source
   document, scope anchor.
2. **Purpose** -- why this analysis exists (e.g. a new Brief dropped,
   or checkpoint is approaching).
3. **Evidence used** -- the Brief sections referenced + the repo
   anchors re-read.
4. **Coverage matrix** -- one row per Brief requirement, Met /
   Partial / Gap, with a pointer into the code or a decision record.
5. **Findings** -- grouped by Brief section, with recommendation and
   target checkpoint (CP#1 / CP#2 / CP#3).
6. **Appendix** -- any reproduction commands or raw evidence.

## How to use these

- **Team lead integrating this workspace.** Read the latest gap
  analysis to see which Brief requirements Sierra-Lima has already
  met and which depend on teammate code.
- **Sierra-Lima before a checkpoint.** The "Findings" section lists
  the gaps tagged for that checkpoint. Close them before demo day.
- **AI agent planning a phase.** If a phase claims to satisfy a Brief
  requirement, the gap analysis should report Met (or Partial with a
  named teammate dependency) at the phase's tip commit.

## Conventions

- **Gap analyses are append-only** (same as audits). Write a new one
  when the Brief or the repo state changes; do not edit old ones.
- **Every "Gap" row has a remediation.** A bare "Gap" with no
  follow-up is not useful; the author should name the file, endpoint,
  or decision that needs to change.
- **Met with caveat** goes in the Partial bucket, not Met. A clean
  matrix is more useful than an optimistic one.

## Related folders

- [`../audits/`](../audits/) -- audits reference the current gap
  analysis in their scope anchor.
- [`../decisions/`](../decisions/) -- if a gap analysis recommends a
  contract change, the change lands as a new decision, not as a
  patch to the gap analysis.
- [`../course-materials/Project2026.pdf`](../course-materials/Project2026.pdf)
  -- the authoritative source for every Brief requirement.
