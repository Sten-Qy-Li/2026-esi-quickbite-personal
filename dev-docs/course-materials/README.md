# Course Materials

Local mirror of the ESI 2026 course PDFs (assignments, lectures, the
Project Brief) plus links to the instructor's weekly-exercise repos.
These files are tracked so that every reader -- human or AI agent --
is working against the same source document as every audit and gap
analysis.

## Contents

### Pinned PDFs (tracked)

| File | Purpose |
|---|---|
| `Project2026.pdf` | **The Brief.** Authoritative source for every grading rubric. Referenced by every gap analysis. |
| `Assignment_1_2026.pdf` | A1 requirements (individual report: business architecture). |
| `Assignment_2_2026.pdf` | A2 requirements (team report: detailed architecture). |
| `Assignment_3_2026.pdf` | A3 requirements (team report: design + ERDs + workflows). |
| `ESI26-Introduction.pdf` | Course intro slides (2026 cohort). |
| `ESI2025 Assignments.pdf` | 2025 cohort's assignment pack (for cross-reference only). |
| `ESI2025 ESI_Lecture*.pdf` | Lecture slides, W1-W10, 2025 cohort. |

The 2025 lecture and assignment pack are included because the 2026
cohort reuses most of that material; the 2026-specific assignments
supersede them where they differ.

### Instructor repos (linked, not mirrored)

The instructor publishes one GitHub repository per weekly exercise.
They are reference implementations, not assignment code -- helpful
patterns but **not** authoritative for this project's API or
architecture. Source: [M-Gharib repositories](https://github.com/M-Gharib?tab=repositories).

| Week | Repo |
|---|---|
| 2 | [ESI-W2-CRUD1](https://github.com/M-Gharib/ESI-W2-CRUD1) |
| 3 | [ESI-W3-CRUD2](https://github.com/M-Gharib/ESI-W3-CRUD2), [ESI-W3-DockerCompose](https://github.com/M-Gharib/ESI-W3-DockerCompose) |
| 4 | [ESI-W4](https://github.com/M-Gharib/ESI-W4) |
| 5 | [ESI-W5](https://github.com/M-Gharib/ESI-W5) |
| 6 | [ESI-W6](https://github.com/M-Gharib/ESI-W6) |
| 7 | [ESI-W7.1](https://github.com/M-Gharib/ESI-W7.1), [ESI-W7.2](https://github.com/M-Gharib/ESI-W7.2) |
| 8 | [ESI-W8.1](https://github.com/M-Gharib/ESI-W8.1), [ESI-W8.2](https://github.com/M-Gharib/ESI-W8.2) |
| 9 | [ESI-W9.1](https://github.com/M-Gharib/ESI-W9.1), [ESI-W9.2](https://github.com/M-Gharib/ESI-W9.2), [ESI-W9.3](https://github.com/M-Gharib/ESI-W9.3), [ESI-W9.4](https://github.com/M-Gharib/ESI-W9.4) |
| 10 | [ESI-W10.1](https://github.com/M-Gharib/ESI-W10.1), [ESI-W10.2](https://github.com/M-Gharib/ESI-W10.2) |
| Exam | [ESI-ExamExample](https://github.com/M-Gharib/ESI-ExamExample) |

### Course websites

- Main course page: <https://courses.cs.ut.ee/2026/esi/spring>
- Practicals (weekly exercises): <https://courses.cs.ut.ee/2026/esi/spring/Main/Practicals>

## How to use these

- **First-time reader.** Read `Project2026.pdf` first. Every
  checkpoint is graded against its rubric.
- **Rewriting a contract?** Check that the change does not contradict
  the Brief or the relevant Assignment. If it does, the Brief wins.
- **AI agent generating code.** Do **not** re-derive requirements
  from these PDFs directly -- the Brief has already been translated
  into [`../decisions/`](../decisions/) and the gap analyses. Start
  from those; consult the PDFs only to disambiguate.

## What **not** to do

- Do not edit the PDFs. They are the graded source of truth.
- Do not delete older materials; cross-referencing the 2025 cohort's
  material is occasionally useful.
- Do not add instructor repos from unverified forks here. Only the
  official `M-Gharib` repos are canonical.

## Related folders

- [`../gap-analysis/`](../gap-analysis/) -- `Project2026.pdf` is the
  source for every gap analysis.
- [`../decisions/`](../decisions/) -- the Assignments feed directly
  into decisions `0001` (scope) and `0002` (workflows).
- [`../prior-submissions/`](../prior-submissions/) -- what Sierra-Lima
  has already submitted against these assignments.
