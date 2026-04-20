# Agent Context

Dated chat-archive logs from AI-coding-agent sessions (Claude Code,
running as `Charlie-Lima-Alfa` or `Golf-Papa-Tango`) that contributed
to Sierra-Lima's personal workspace. One archive per session; each
archive summarises what the session tried to do, what it landed, and
any handover notes for the next session.

## File-naming

```
YYYY-MM-DD_chat-archive_<callsign>_<commit>.md
```

- `YYYY-MM-DD` -- the session date (local time, Tallinn).
- `<callsign>` -- the author AI agent. Not a service owner. See
  [`../README.md`](../README.md#file-naming-conventions).
- `<commit>` -- the short SHA of the tip commit the session produced
  (or the latest one it observed, for read-only sessions).

Files are sorted oldest-first in `git ls-files`; file-manager sort may
put `README.md` at the bottom.

## What's in each archive

Session archives follow a consistent shape -- purpose, scope worked
on, commits landed, findings/open threads, and a handover note for the
next session. They are **narrative**, not normative: they record what
the session did, not what the code currently does.

## How to use these

- **Human reader picking up context.** Read the most recent archive
  first; walk backwards only if you need to understand why a specific
  decision was made.
- **AI agent starting a new session.** Always read the latest archive
  before touching code. Additionally, read the latest audit in
  [`../audits/`](../audits/) for the point-in-time state of the
  codebase. Do not rewrite earlier archives -- write a new one when
  your session ends.
- **Grepping for a commit.** The commit SHA appears both in the
  filename and in the first table of each archive, so
  `grep -l '<commit>' .` will locate the session that produced it.

## What **not** to do

- Do not modify existing chat-archive files. They are historical
  evidence; treat them as append-only.
- Do not derive current state from these files -- always cross-check
  against the code under [`../../services/`](../../services/) and the
  latest audit before acting. Archives describe the state as it was
  at the time of writing; the code may have moved on.
- Do not delete archives when cleaning up. Storage is cheap and the
  audit trail is valuable going into checkpoint defence.

## Related folders

- [`../audits/`](../audits/) -- every chat archive with a non-trivial
  change typically has a matching or follow-on audit. Cross-reference
  by commit SHA.
- [`../decisions/`](../decisions/) -- if an archive mentions a
  contract or convention, the authoritative version is in `decisions/`.
