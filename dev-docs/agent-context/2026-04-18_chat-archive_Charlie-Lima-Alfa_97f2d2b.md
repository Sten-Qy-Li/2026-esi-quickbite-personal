# Chat Archive - 2026-04-18 - Charlie-Lima-Alfa (`97f2d2b`)

## Session Summary

This session continued from the prior session's compaction (which had
produced commit `7c5daba` for the final master plan and its chat
archive). It executed **Phase 0** and **Phase 1** of
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`.

Three main tasks were completed:

1. **Phase 0 -- Scope Freeze & Repo Conventions.** Produced five
   decision documents and expanded four service READMEs. Committed
   and pushed as `97f2d2b`.
2. **Phase 1 -- Auth & Gateway Contract Alignment.** Produced one
   consolidated decision (`0010-auth-contract.md`) covering all seven
   Phase 1 tasks, and resolved Q6 in `0004-open-questions.md` by
   marking it `Resolved by 0010`.
3. **Status correction on `0010-auth-contract.md`.** On user feedback,
   tightened the document's framing: changed the status from
   "Accepted (Sierra-Lima side; ratification due)..." to plain
   "Accepted" with a shorter note clarifying that §5.1 defaults were
   applied unilaterally per the master plan's explicit instruction,
   and no team ratification is required unless Alfa-Kilo actively
   pushes back.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`
- Today: 2026-04-18 (Saturday)
- Active branch: `dev`

## Local Repository State Observed

At session start (commit `7c5daba` on branch `dev` -- inherited from
prior session's push):

- `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
  -- authoritative master plan.
- `dev-docs/decisions/` did not exist yet.
- `services/README.md`, `services/restaurant-service/README.md`,
  `services/menu-service/README.md`, `services/local-dev/README.md`
  were still one-line placeholders.
- Two roadmap files still untracked (pre-existing state):
  `dev-docs/roadmaps/Charlie-Lima-Alfa_aac68b0_project-phases.md`,
  `dev-docs/roadmaps/Golf-Papa-Tango_aac68b0_project-phases.md` --
  left alone (outside scope of this session).

After the Phase 0 commit (`97f2d2b`):

- `dev-docs/decisions/0001`-`0005` created.
- All four service READMEs expanded to describe the planned Maven +
  Docker Compose layout, responsibilities, and relevant decisions.
- Branch `dev` at `97f2d2b`, up-to-date with `origin/dev`.

At session end (Phase 1 artifacts still uncommitted at the time of
writing this archive):

- `dev-docs/decisions/0010-auth-contract.md` created and tightened.
- `dev-docs/decisions/0004-open-questions.md` updated (Q6 resolved
  by 0010).
- This archive file created.
- All three will be committed together as the next commit on `dev`.

## Files Created or Updated During This Session

### Created

- `dev-docs/decisions/0001-scope-freeze.md` -- implementation subset
  frozen (7 implemented services, 2 shared components, 1 design-only);
  Sierra-Lima's ownership of Restaurant + Menu (R19-R22) locked in.
- `dev-docs/decisions/0002-workflows.md` -- W1 / W2 / W3 labels frozen
  with exact Sierra-Lima responsibility (W1 callee; W2/W3 not
  involved in the baseline).
- `dev-docs/decisions/0003-conventions.md` -- Git workflow,
  commit-message style (kept imperative sentence-case matching the
  existing repo history, not conventional commits), Java 17, Maven
  groupId `ee.ut.esi.quickbite`, Docker image naming
  `quickbite-<service>:dev`, env-var naming, directory conventions,
  decision-document conventions.
- `dev-docs/decisions/0004-open-questions.md` -- 10 open questions
  (Q1-Q10) each with current lean and target phase; resolution
  protocol defined.
- `dev-docs/decisions/0005-non-goals.md` -- 13 non-goals (N1-N13)
  explicitly out of scope for the first implementation pass.
- `dev-docs/decisions/0010-auth-contract.md` -- consolidated Phase 1
  decision covering public routes, default protected-route rule,
  gateway path map (Appendix F.5 copied verbatim), token propagation
  model, identity context claims, browse-route decision (public),
  JWT claims shape (example payload), and a per-endpoint
  route-protection matrix for all 12 Sierra-Lima endpoints.
- `dev-docs/agent-context/2026-04-18_chat-archive_Charlie-Lima-Alfa_97f2d2b.md`
  -- this archive.

### Updated

- `services/README.md` -- layout table, ownership note (only
  Sierra-Lima's services live here; teammate services in their own
  repos), conventions summary.
- `services/restaurant-service/README.md` -- planned Maven layout,
  responsibilities (R19, R20, W1 availability endpoint), API-surface
  pointer to Appendix F, current-state notes, related decisions.
- `services/menu-service/README.md` -- planned Maven layout,
  responsibilities (R21, R22, W1 batch validate endpoint), API-surface
  pointer to Appendix F, current-state notes, related decisions.
- `services/local-dev/README.md` -- planned Compose layout, standard
  usage, env-var prefixes, explicit "not included here" list
  (teammate services, Eureka, Kafka until Phase 10), current state.
- `dev-docs/decisions/0004-open-questions.md` -- Q6 resolved by
  `0010` (browse routes public, short rationale recorded).

### Read (not modified)

- `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
  §9 Phase 0 and Phase 1, §5.1, Appendix F.1, F.2, F.5.
- `dev-docs/agent-context/2026-04-18_chat-archive_Charlie-Lima-Alfa_a520963.md`
  -- used as the format template for this archive.

## Phase 0 Artifacts (Summary)

Five numbered decisions encode the full Phase 0 output. The master
plan's Phase 0 Definition of Done was satisfied as follows:

| DoD item | Satisfied by |
|----------|--------------|
| Implementation subset confirmed in writing | `0001-scope-freeze.md` |
| Workflow labels W1/W2/W3 frozen | `0002-workflows.md` |
| Folder layout agreed | `0003-conventions.md` §Directory conventions + four READMEs |
| Conventions documented | `0003-conventions.md` |
| Non-goals list exists | `0005-non-goals.md` |
| No open scope debate blocking Phase 1 | `0004-open-questions.md` (all Q's target Phase 1+ -- none block Phase 1) |

Non-obvious Phase 0 choice recorded for future-agent reference: the
commit-message style was kept **imperative sentence-case**, not
switched to conventional commits (`feat:`/`fix:`/...). Rationale:
every commit since `9126f15` uses imperative sentence-case, and no
tooling in this repo consumes conventional-commit prefixes.
Consistency beat conformance.

## Phase 1 Artifacts (Summary)

One numbered decision (`0010-auth-contract.md`) covers all seven
Phase 1 tasks from the master plan. The document's §8 contains an
endpoint-by-endpoint route-protection matrix that is the authoritative
reference for Phases 3-7.

Browse-route protection (`GET /restaurants`, `GET /restaurants/{id}`,
`GET /restaurants/{rid}/menu-items`, `GET /menu-items/{id}`) is
**public** -- no token required. Rationale: W1 does not browse, the
tightening is a security-filter change only, and no login wall is
needed for the CP#1 browse demo.

JWT claims shape is canonical: `iss`, `sub`, `userId`, `role`
(`Customer`|`Driver`|`RestaurantOwner`|`Admin`), `tokenType`
(`USER`|`SERVICE`, optional), `iat`, `exp`. HS256 shared secret
(`JWT_SECRET`) from `.env.local`; no Vault.

Service-to-service tokens: two permitted modes -- token relay (caller
forwards end-user token) or service token (`tokenType: SERVICE` +
`serviceName`, short TTL). Services mint their own service tokens;
gateway never mints tokens.

Gateway path map (Appendix F.5) copied verbatim -- Sierra-Lima's
prefixes are `/api/restaurants/**` (Restaurant Service) and
`/api/menu-items/**`, `/api/restaurants/*/menu-items/**` (Menu
Service). Gateway strips `/api` before forwarding; controllers map
to `/restaurants/**` and `/menu-items/**`.

### Phase 1 Definition of Done

| DoD item | Satisfied |
|----------|-----------|
| Every Sierra-Lima endpoint has a documented required auth posture | `0010` §8 route-protection matrix (12 endpoints) |
| Sierra-Lima can implement against the documented JWT shape without waiting for Alfa-Kilo | `0010` §7 JWT example + Phase 3 `JwtDevMint` utility |

## 0010 Status Correction

Initial draft of `0010-auth-contract.md` marked its status as
"Accepted (Sierra-Lima side; team-wide ratification due at the
2026-04-21 session with Alfa-Kilo)." The user asked whether this
meant Phase 1 was blocked on teammate input. Correct answer: **no**.
The master plan §9 Phase 1 "Team dependency" block explicitly says
"If Alfa-Kilo is not available, use the defaults in §5.1 and note
any unilateral choices in the decisions log." Applying §5.1
unilaterally is the intended path, not a provisional stopgap.
Master plan Assumption 6 (§7) similarly states that Phases 0-8 are
**designed to make progress without any teammate's code**.

The status line was therefore tightened to plain `Accepted`, and the
Context section was rewritten to state that the §5.1 defaults are
the contract (not a draft awaiting sign-off). The Team alignment
note was trimmed in the same spirit.

## Commits Created in This Session

1. `97f2d2b -- Record Phase 0 decisions 0001-0005 and expand service READMEs`
   - Parent: `7c5daba`
   - Files: 9 (+806 / -4). Five new decisions + four modified READMEs.
2. (Second commit at end of session) -- stages Phase 1 artifacts and
   this archive; subject recorded in git history at commit time.

## Feedback and Preferences Learned

- **Don't over-qualify unilateral decisions.** When the master plan
  explicitly blesses applying default X under condition Y, record
  the decision as `Accepted` outright, not as "Accepted (pending
  team sign-off)". Overcautious framing reads as a block and forces
  the user to explicitly unblock.
- This has been saved as a feedback memory for future sessions.

## Workspace Notes

- Task tracking at session end:
  - #7 to #12 -- Phase 0 decision work -- all completed.
  - #13 -- Draft `0010-auth-contract.md` -- completed.
  - #14 -- Resolve Q6 in `0004-open-questions.md` -- completed.
  - #15 -- Write session archive (this file) -- in progress at time
    of writing.
  - #16 -- Commit and push Phase 1 artifacts -- pending.
- No destructive git operations were used.
- No secrets were committed; `.env.local` remains git-ignored.
- `.gitignore` unchanged.

## Suggested Next Steps For a Future Agent

1. **Begin Phase 2 -- Contract Pack & Local-Dev Bootstrap.** The
   master plan splits it into Part A (contract pack) and Part B
   (local-dev bootstrap) and allows them to be executed in one or
   two sessions. Phase 2 is Sierra-Lima-only (no teammate
   dependency), same as Phase 0 and Phase 1.
2. Part A deliverables (see master plan §9 Phase 2):
   - Freeze the 12 REST endpoints with one example request and one
     example response each (target: `0020-sierra-lima-contracts.md`).
   - Freeze validation rules (Restaurant: name/lat/lng/operating
     hours/city; Menu: priceAmount/priceCurrency/name/category).
   - Freeze DB schemas as Flyway `V1__init.sql` for each service.
   - Plan seed data (4-6 restaurants, 12-18 items, at least two
     cities, three categories, one closed restaurant).
3. Part B deliverables:
   - Spring Boot scaffolds for both services (Spring Initializr),
     including the `jjwt 0.11.5` dependencies (api, impl, jackson).
   - Ship the Spring Security stub (`permitAll()` + stateless) so
     `/actuator/health` returns 200 without form login.
   - `docker-compose.yml`, `.env.example`, runbook in
     `services/local-dev/`.
   - Postman workspace skeleton.
4. Before starting Phase 2, consider addressing the remaining open
   questions that target Phase 2: **Q1** (error envelope shape --
   freeze), **Q8** (seed data format -- Flyway preferred), if not
   already resolvable from the master plan's Appendix F.
5. Update `dev-docs/decisions/` with any new decisions taken in
   Phase 2; supersede prior decisions rather than editing them in
   place.

## Workspace Safety Notes

- No files were deleted or truncated in this session.
- No `git push --force` operations.
- Single `git push origin dev` for `97f2d2b`; a second push planned
  for the Phase 1 bundle (this archive plus `0010` plus the `0004`
  Q6 update).
- `.idea/` and `.claude/` continue to be ignored.
