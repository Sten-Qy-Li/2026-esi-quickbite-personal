# Decisions

Architecture and implementation decision records for Sierra-Lima's
slice of the QuickBite ESI project. ADR-style: one decision per
numbered file, with status, date, author, context, decision,
consequences, and supersessions.

**These files are the contracts.** When an audit, chat archive, or
service README says "per `0003-conventions.md`", that decision file
wins over the older document.

## Reading order

The files are numbered so that a first-time reader can walk them
top-to-bottom and arrive at the current contract without backtracking.
The number *is* the reading order; it is not a priority ranking.

| # | File | What it pins |
|---|---|---|
| 0001 | [`0001-scope-freeze.md`](0001-scope-freeze.md) | The 7 implemented business services + 2 shared components + 1 design-only; Sierra-Lima owns Restaurant and Menu. |
| 0002 | [`0002-workflows.md`](0002-workflows.md) | The three workflows (W1 place-order, W2 driver-accepts, W3 order-delivered) and who serves which hop. |
| 0003 | [`0003-conventions.md`](0003-conventions.md) | Maven groupId, package root, Docker image tag, Compose service name, env-var naming. |
| 0004 | [`0004-open-questions.md`](0004-open-questions.md) | The open design questions (Q1-Q7) that still need a team answer. |
| 0005 | [`0005-non-goals.md`](0005-non-goals.md) | Explicit non-goals (no Eureka, no Vault, no cross-DB FKs, no Review Service code, etc.) with rationale. |
| 0010 | [`0010-auth-contract.md`](0010-auth-contract.md) | JWT shape, issuer, signing key, claims; role-to-endpoint authorisation matrix. |
| 0020 | [`0020-sierra-lima-contracts.md`](0020-sierra-lima-contracts.md) | The full HTTP API surface of `restaurant-service` and `menu-service`, including error envelope. |
| 0030 | [`0030-w1-synchronous-contract-lock.md`](0030-w1-synchronous-contract-lock.md) | W1 hop-by-hop request/response shapes (availability probe + batch validate). |
| 0031 | [`0031-cross-service-status-code-table.md`](0031-cross-service-status-code-table.md) | Canonical HTTP status codes for every documented failure mode, cross-service. |
| 0032 | [`0032-w2-w3-event-contract-lock.md`](0032-w2-w3-event-contract-lock.md) | The Kafka event envelopes for W2 and W3, including `menu-events` producer schema. |
| 0033 | [`0033-inter-service-token-propagation-lock.md`](0033-inter-service-token-propagation-lock.md) | Which hops propagate the caller's bearer token and which mint a service-to-service token. |
| 0040 | [`0040-phase-16-async-stance.md`](0040-phase-16-async-stance.md) | Sierra-Lima's non-participation stance for W2/W3; the log-only `menu-events` fallback and the one-class Kafka swap plan. |

The gap between `0005` and `0010` (then `0010` and `0020`, etc.) is
intentional headroom for inserting future decisions close to the
topic they amend without renumbering everything below.

## Status field

Every decision file has a `Status:` line in its header table. Values
used so far:

- **Accepted** -- the decision is in force. Code and docs must
  follow it.
- **Superseded by <newer doc>** -- the decision has been replaced;
  read the newer doc. Superseded decisions are kept for history.

There is no "Proposed" state in this workspace: decisions land only
when Sierra-Lima has applied the plan-blessed default (see the
[memory note](../../README.md) about not hedging on plan-blessed
defaults).

## File shape (for authors)

Each decision file has this skeleton:

```markdown
# <NNNN> -- <Short Title>

- **Status:** Accepted
- **Date:** YYYY-MM-DD
- **Author:** <callsign> (for Sierra-Lima)
- **Base commit:** `<short-SHA>`
- **Source:** <assignments, master-plan section, or prior decision it derives from>

## Context
## Decision
## Consequences
## Supersedes
```

## How to use these

- **Human reviewer during code review.** If a PR changes an endpoint
  shape, a status code, or a package name, it must either (a) already
  match the relevant decision, or (b) land with an amending decision
  that supersedes the relevant one. No silent drift.
- **AI agent writing code.** Read the decisions that cover the file
  you are editing *before* writing. Do not invent a status code, an
  endpoint path, or an env-var name -- look it up in `0020` / `0031`
  / `0003`.
- **Team lead integrating this workspace.** The decisions here are
  Sierra-Lima's commitments into the group contract. If group-wide
  decisions differ, the group-wide ones win; add a superseding
  decision to `group-repo/decisions/` (or equivalent) and note the
  override.

## Supersession and editing

- To change an accepted decision, write a **new** file with a new
  number and a `Supersedes: <old-number>` line in the body. Flip the
  old file's `Status:` to `Superseded by <NNNN>`.
- Do not rewrite accepted decisions in place; the git history matters
  for grading and for reconstructing why a contract is the way it is.
