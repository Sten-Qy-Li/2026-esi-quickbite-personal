# 0003 -- Repository and Code Conventions

- **Status:** Accepted
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `7c5daba`
- **Source:**
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §9
  Phase 0 Task 4 (conventions), and the existing commit history in this
  repository.

## Context

Every file created from Phase 2 onward needs a consistent shape so later
phases stop spending time on naming debates. This document pins the
conventions that are repeated most often: Git workflow, commit messages,
Java version, Maven coordinates, package names, Docker image names, and
environment-variable names.

## Decision

### Git workflow

- **`dev`** is the daily working branch. All Phase work happens on `dev`
  or a short feature branch that merges back to `dev`.
- **Feature branches** (when used) are named `feature/<phase>-<short-slug>`
  -- for example `feature/phase-03-restaurant-foundation`. They merge back
  into `dev` without squashing unless the history is noisy.
- **`main`** is reserved for release milestones (CP#1, CP#2, CP#3). Tag
  those commits `v<major>.<minor>.<patch>-cp<n>` -- for example
  `v1.0.0-cp1` on 2026-05-05.
- **Never** force-push to `main`. Force-push on `dev` is allowed only on
  an unshared branch.
- **Never** skip Git hooks (`--no-verify`) or bypass signing unless the
  user explicitly asks for it.

### Commit-message style

Keep the existing repository style: **imperative sentence-case, no
conventional-commit prefix.** Recent history examples:

```
Add audited and patched phased implementation roadmaps for aac68b0 baseline
Add final master plan for Sierra-Lima and archive 2026-04-18 chat session
Expand course materials index, add Assignment 3 figures and source files, and ignore IDE/Claude local settings
```

Rules:

1. Subject line is imperative (`Add`, `Update`, `Fix`, `Remove`, `Move`,
   `Refactor`, `Document`).
2. Subject line starts with a capital letter and does not end with a
   period.
3. Keep the subject under ~80 characters where possible; if longer, move
   detail into the body.
4. Body (when present) explains **why**, not what -- the diff already
   shows what.
5. When Claude is a co-author, add a trailing
   `Co-Authored-By: Claude <model> <noreply@anthropic.com>` line.

Reason for keeping this style rather than switching to conventional
commits (`feat:`, `fix:`, `chore:` ...): the repository already uses
imperative sentence-case in every commit since `9126f15`. Consistency is
more valuable than conformance with a convention that no tooling in this
repo consumes.

### Java and build

- **Java 17** (Eclipse Temurin recommended) is the default for all Spring
  Boot services. Raise to Java 21 only if every teammate agrees.
- **Maven 3.9+** is the build tool (the course practicals use Maven).
- **Maven groupId:** `ee.ut.esi.quickbite`.
- **Maven artifactId** per service: `<service>-service` -- for example
  `restaurant-service`, `menu-service`.
- **Root Java package per service:** `ee.ut.esi.quickbite.<service>` --
  for example `ee.ut.esi.quickbite.restaurant`,
  `ee.ut.esi.quickbite.menu`. Sub-packages follow Spring Boot's usual
  layout: `controller`, `service`, `repository`, `domain`, `dto`,
  `config`, `security`.

### Docker and Compose

- **Docker image naming:** `quickbite-<service>:dev` for locally built
  development images -- for example `quickbite-restaurant-service:dev`.
- **Compose service names:** `<service>` without the `quickbite-` prefix
  -- for example `restaurant-service`, `menu-service`, `restaurant-db`,
  `menu-db`. This matches the port matrix in the master plan §9 Phase 2
  Task 10.
- **Network name:** a single bridge network named `quickbite-net` shared
  by all Compose services.
- **Volume names:** `<service>_db_data` -- for example
  `restaurant_db_data`, `menu_db_data`.

### Environment-variable naming

- **Case:** `SCREAMING_SNAKE_CASE`.
- **Grouped by prefix**, one prefix per domain:
  - `DB_` -- database connection (`DB_URL`, `DB_USER`, `DB_PASSWORD`).
  - `JWT_` -- token config (`JWT_SECRET`, `JWT_ISSUER`).
  - `SPRING_` -- Spring's own variables (`SPRING_PROFILES_ACTIVE`).
  - `<SERVICE>_SERVICE_URL` -- downstream URL only when one service calls
    another (for example `USER_SERVICE_URL`,
    `RESTAURANT_SERVICE_URL`).
- **Local defaults** live in `services/local-dev/.env.example` (tracked)
  and `services/local-dev/.env.local` (git-ignored).
- **Secrets** (real keys, real passwords) never enter Git. `.env.local`
  is the local override file.

### Directory conventions

Master-plan Appendix G is the authoritative layout. Summary:

```
services/
  restaurant-service/     Maven + Spring Boot project
  menu-service/           Maven + Spring Boot project
  local-dev/              docker-compose.yml, .env.example, runbook
  frontend/               Vue.js project (created in Phase 12)
dev-docs/
  agent-context/          chat archives (<date>_chat-archive_<callsign>_<commit>.md)
  audits/                 roadmap audits
  course-materials/       lecture PDFs and assignment PDFs
  decisions/              NNNN-<slug>.md numbered decisions
  gap-analysis/
  instructor-feedback/
  prior-submissions/      A1, A2, A3 submissions + feedback
  roadmaps/               phased roadmaps
```

### Decision-document conventions

- Files named `NNNN-<kebab-slug>.md` where `NNNN` is zero-padded to four
  digits, starting at `0001`.
- Every decision records: `Status`, `Date`, `Author`, `Base commit`,
  `Source`.
- `Status` is one of: `Proposed`, `Accepted`, `Superseded by NNNN`,
  `Deprecated`.
- A superseding decision keeps the old number live and marks its own
  document as the replacement.

## Consequences

- A future coding agent can pick up the plan and know, without asking:
  what to name a new package, how to tag a commit, what Docker image name
  to use, what env-var key to set.
- Deviations are allowed but must be recorded as a new decision that
  supersedes the relevant section of this one.

## Supersedes

None. This is the first conventions decision.
