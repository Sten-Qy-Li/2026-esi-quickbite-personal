# Presentation

Checkpoint-demo assets for Sierra-Lima's slice of the final
presentation (CP#3, 2026-05-19). Currently covers Phase 18 of the
master plan.

## Contents

| File | Purpose |
|---|---|
| `phase-18-slides_Sierra-Lima.md` | The actual slide text / per-slide script for the Sierra-Lima segment of the group deck. |
| `phase-18-demo-script_Sierra-Lima.md` | Live-demo script: exact sequence of clicks, URLs, curl / Postman calls, and expected responses. |
| `phase-18-qa-prep_Sierra-Lima.md` | Q&A preparation: anticipated instructor questions + the crisp answer, with pointers to the evidence (decision, audit, or verification file). |
| `phase-18-fallbacks_Sierra-Lima.md` | Fallback plan for each demo beat if a live call fails on stage (cached response, pre-recorded clip, verbal workaround). |

## How to use these

- **Demo day.** Read `phase-18-demo-script_Sierra-Lima.md` top to
  bottom the morning of the demo; rehearse once with the actual
  local stack up; then follow it live. Keep
  `phase-18-fallbacks_Sierra-Lima.md` in a second window.
- **Rehearsal.** Run the full demo against `docker compose --profile
  dev-gateway up -d --build` on a clean target; every URL and
  response in the script should be reproducible.
- **Written report integration.** The Q&A prep doubles as source
  material for the written report -- each answer cites the evidence
  you want referenced.
- **AI agent extending or adjusting.** Do not invent new demo
  scenarios; every demo beat must trace back to an endpoint in
  [`../decisions/0020-sierra-lima-contracts.md`](../decisions/0020-sierra-lima-contracts.md)
  or a workflow hop in [`../decisions/0030`](../decisions/0030-w1-synchronous-contract-lock.md).

## File shape

Each Phase 18 artefact starts with a metadata header (owner, source
commit, the roadmap phase it implements), a scope block, and then
the content in the form appropriate to its purpose (slides,
step-by-step script, Q&A pairs, fallback table).

## What is **not** here (yet)

- **CP#1 and CP#2 decks.** Talking-points outlines live at the
  `dev-docs/` root (`checkpoint-1-talking-points.md`,
  `checkpoint-2-talking-points.md`); their full demo scripts land
  under this folder as the checkpoints approach.

## Related folders

- [`../roadmaps/`](../roadmaps/) -- Phase 18 is defined in the final
  master plan; demo content must satisfy that phase's DoD.
- [`../verification/`](../verification/) -- the corresponding
  verification report (`phase-18-verification_Charlie-Lima-Alfa.md`)
  is the evidence that the demo is live-probable.
- [`../audits/`](../audits/) -- the most recent audit is the
  authoritative record of which parts of the demo are currently
  green.
