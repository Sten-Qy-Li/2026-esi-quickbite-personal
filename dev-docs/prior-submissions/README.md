# Prior Submissions

Immutable archive of assignment submissions (A1, A2, A3) that
Sierra-Lima handed in through the course submission system. Kept in
this repository so that audits, gap analyses, and the Group 7 team
lead can cross-reference what was actually graded.

## Contents

| File | Assignment | Purpose |
|---|---|---|
| `Assignment-1-Submission.pdf` / `.docx` | A1 | Individual report: business architecture. Graded 3.50/4.00 (feedback in `Assignment-1_Feedback.txt`). |
| `Assignment-1_Feedback.txt` | A1 | Instructor feedback text. |
| `Assignment-2-Submission.pdf` / `.docx` | A2 | Team report: detailed architecture. |
| `Assignment-3-Submission.pdf` / `.docx` | A3 | Team report: design + ERDs + workflows. |
| `assignment-3_figure1_business-architecture.png` | A3 fig | Fresh export of Fig. 1 (business architecture). |
| `assignment-3_figure1b_implementation-architecture.png` | A3 fig | Fresh export of Fig. 1b (implementation architecture). |
| `assignment-3_figure2_service-er-diagrams.png` | A3 fig | Fresh export of Fig. 2 (service ER diagrams). |
| `assignment-3_figure3_workflow-w1-sequence.png` | A3 fig | Fresh export of Fig. 3 (W1 sequence diagram). |
| `assignment-3_figure4_workflow-w2-w3-events.png` | A3 fig | Fresh export of Fig. 4 (W2/W3 event flow). |

PDFs and DOCX are kept together so that a reader can (a) cite the
graded PDF, and (b) inspect comments and revision marks in the DOCX.
The standalone PNGs are the source figures used in A3, re-exported at
full resolution.

## How to use these

- **Grader / team lead verifying "was this design submitted?"** The
  PDF is the ground truth. The DOCX is editable history.
- **Checkpoint defence.** If an instructor question references
  "your A3 Figure 3", open `assignment-3_figure3_workflow-w1-sequence.png`
  for the high-resolution version.
- **Writing a new decision or audit.** If a decision restates or
  refines something that was already submitted in A1-A3, cite the
  relevant submission file so the reader can verify continuity.

## Assignment scores (recorded for context)

- A1 = 3.50/4.00. Feedback penalised for (a) including
  infrastructure in the business-architecture diagram and (b) the
  shared-DB assumption. Both have been remediated before A3.
- A2 and A3 scores not yet recorded at the time of this README.

## What **not** to do

- **Do not edit these files.** Submissions are immutable historical
  evidence. If an error is discovered, document it in an audit or
  decision, not by rewriting the submission.
- Do not strip the DOCX files even though the PDF is enough for
  grading -- the DOCX preserves formatting sources that the
  instructor sometimes asks for at checkpoint defence.
- Do not add unsubmitted drafts here. Drafts belong in their author's
  working folder (for Sierra-Lima's report draft, see
  `../report-draft-backend_Sierra-Lima.md`).

## Related folders

- [`../course-materials/`](../course-materials/) -- the assignment
  briefs these submissions answer.
- [`../gap-analysis/`](../gap-analysis/) -- for A3 specifically, the
  gap analyses compare the A3 commitments against current code.
