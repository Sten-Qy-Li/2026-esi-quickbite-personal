# Audits

Readiness audits of Sierra-Lima's slice of the QuickBite codebase,
each pinned to a specific commit. An audit is a snapshot: it records
what the tests, smoke scripts, Newman runs, and docker compose health
probes reported **on that exact commit**, plus a prose findings list
and a go/no-go verdict.

## File-naming

```
audit-<commit>_<callsign>_<scope>[_<n>].md
```

- `<commit>` -- the short SHA the audit ran against.
- `<callsign>` -- the authoring AI agent (e.g. `Charlie-Lima-Alfa`,
  `Golf-Papa-Tango`). Not a service owner; see
  [`../README.md`](../README.md#file-naming-conventions).
- `<scope>` -- free-text tag naming the audit's purpose. Examples
  used so far: `pre-integration-readiness`,
  `integration-handover-readiness`, `final-handover-readiness`,
  `team-lead-integration-readiness`, `pre-team-integration-readiness`.
- `_<n>` -- numeric suffix disambiguating multiple audits on the same
  commit by the same author (`_1`, `_2`, `_3`, ...). Omitted when
  there is only one.

## Audit shape (for authors)

Every audit in this folder follows the same structure, so readers
(human and agent) can jump to the section they need:

1. **Metadata table** -- commit SHA, subject, branch, author,
   auditor, date, purpose, scope anchor. Always first.
2. **Scope reviewed** -- which services, endpoints, workflows, and
   files were covered; which were explicitly excluded.
3. **Validations completed** -- a numbered table of checks: `mvn test`
   per service, `npm run lint` + `npm run build`, `docker compose up`
   + healthchecks, `smoke.sh` / `smoke-cross-service.sh`, Newman,
   live-wire curl probes, and targeted code reviews. Each row records
   the command and the observed result.
4. **Findings and risks** -- prose descriptions of defects, flakies,
   or brittleness, each classified **Low / Medium / High** with
   recommended fixes.
5. **Explicit gaps or unverified assumptions** -- what could not be
   closed in this workspace (usually teammate services, broker, or
   browser-level interaction tests).
6. **Verdict** -- a single-line go/no-go for the audit's purpose
   ("READY TO HAND OVER", "READY TO HAND OVER once Finding N is
   patched", etc.).
7. **Appendices** -- reproduction commands and raw evidence snippets.

## How to use these

- **Human reviewer asking "is the current tip safe to merge?"** Read
  the *most recent* audit whose `<commit>` matches the tip (or is an
  ancestor of the tip with no material changes since). The verdict
  line is the summary; the findings section is the nuance.
- **AI agent about to write code.** Read the latest audit to learn
  which invariants are currently green and what the open findings
  are. Do not reintroduce an issue that a past audit already closed.
- **Checkpoint defence.** Every "is X really working?" question from
  the instructor has an answer in the audits: find the latest audit
  row that exercises X and quote the observed result.

## Conventions

- **Audits are append-only.** Once written, they are historical
  evidence -- do not edit an audit to "update" it; write a new audit
  on the new commit instead.
- **Every finding names a file.** A finding without a `path:line`
  anchor is not actionable. Authors should include at least a path;
  line numbers where practical.
- **Severity is consistent across audits.** Low = cosmetic or
  documentation; Medium = a real issue that does not block the audit
  purpose; High = must-fix before the verdict can flip to green.

## Related folders

- [`../decisions/`](../decisions/) -- when an audit references a
  contract (auth matrix, status codes, workflow hops), the
  authoritative version is a decision record.
- [`../verification/`](../verification/) -- audits typically cite
  phase-verification reports in evidence; the full verification logs
  live there.
- [`../gap-analysis/`](../gap-analysis/) -- gap analyses are broader
  (Brief vs. repo) and feed into audits as context.
- [`../agent-context/`](../agent-context/) -- every audit by an AI
  agent has a chat archive with the same commit SHA.
