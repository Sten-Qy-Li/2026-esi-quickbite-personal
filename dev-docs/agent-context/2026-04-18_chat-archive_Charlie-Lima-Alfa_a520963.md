# Chat Archive - 2026-04-18 - Charlie-Lima-Alfa (`a520963`)

## Session Summary

This chat focused on auditing the latest phased roadmap, patching it based on
audit findings, committing and pushing the patched artifacts, and then writing
a clean-slate final master plan that consolidates the strengths of every prior
roadmap in the repository.

Five main tasks were completed:

1. **Audit** `dev-docs/roadmaps/Charlie-Lima-Alfa_aac68b0_project-phases.md`
   against the older `Charlie-Lima-Alfa_2ce188a_project-phases.md`, determine
   whether it is a sufficient plan for Sierra-Lima's part of the ESI Project,
   and patch the `aac68b0` file based on audit findings.
2. Clarify whether purchasing EUR 20 of extra Anthropic credit would be usable
   inside the current Claude Code environment.
3. **Commit and push** the current state of the local repository, producing
   commit `a520963` on `origin/dev`.
4. **Verify sync** between local repository and `origin/dev` at `a520963`
   (branch up-to-date, working tree clean).
5. **Write a final master plan**
   `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`:
   a clean-slate phased roadmap for Sierra-Lima's portion of the project,
   written in B1-level English, thorough enough for any coding agent to
   execute, integrating strengths from all earlier roadmaps.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`
- Required course references:
  - `https://courses.cs.ut.ee/2026/esi/spring`
  - `https://courses.cs.ut.ee/2026/esi/spring/Main/Practicals`
  - children of those pages as needed
- Team context notes:
  - this course project is group work (Group 7)
  - Sierra-Lima wants an early head start; some teammates tend to be last-minute
  - Assignments 1 to 3 are already stored locally under `dev-docs/`

## Course and Project Information Used

### Key course pages already referenced in prior archives

- `https://courses.cs.ut.ee/2026/esi/spring/Main/HomePage`
- `https://courses.cs.ut.ee/2026/esi/spring/Main/Practicals`
- `https://courses.cs.ut.ee/2026/esi/spring/Main/ProjectAndExam`
- Practical sub-pages previously consulted:
  - `PS041` -- microservice inter-service communication (WebClient)
  - `PS051` -- microservices discovery & load balancing (Eureka)
  - `PS061` -- API Gateway (Spring Cloud Gateway)
  - `PS072` -- resilient microservices (Resilience4j)
  - `PS081` / `PS082` -- Kafka event-driven integration
  - `PS091` / `PS092` / `PS093` -- Vue.js and CRUD with Spring Boot
  - `PS101` -- Spring Boot Security
  - `PS111` -- JWT authentication service
  - `PS121` -- Secure full-stack (Spring Boot + Vue.js with JWT)

### Important absolute milestone dates used

- `2026-04-18`: session start (Saturday; today).
- `2026-04-21`: project description practical
- `2026-04-28`: project consultation practical (practicals page shows
  `28/04/2025`, treated as a likely typo)
- `2026-05-05`: project checkpoint #1, backend
- `2026-05-12`: project checkpoint #2, frontend + backend
- `2026-05-19`: project checkpoint #3, final presentation
- `2026-05-25`: exam 1
- `2026-06-08`: exam 2
- `2026-06-22`: resit

### Grading context

- Assignments: 20 points (A1: 4pt, A2: 8pt, A3: 8pt)
- Project: 30 points (3 checkpoints)
- Exam: 50 points (minimum 21 to pass)
- Total: 100 points (minimum 51 to pass)
- Assignment 1 score: 3.50/4.00 (penalties: infra in business diagram; shared
  DB)

### Design constraints carried forward

- Business architecture and infrastructure stay separate in diagrams.
- No shared database across microservices; each implemented service owns its
  own PostgreSQL.
- Cross-service references use ID fields only (no FK crossing DB boundaries).
- Demonstrate both synchronous (REST) and asynchronous (Kafka) integration.
- Sierra-Lima owns Restaurant Service and Menu Service; does not replace
  either with a shared integration component.
- Assignment 3 implementation subset:
  - Implemented business services: Order, User, Restaurant, Menu, Payment,
    Delivery, Notification
  - Shared components: API Gateway, Kafka/Event Broker
  - Design-only: Review Service

## Assignment and Submission Context Used

Prior-submission artifacts read and used this session:

- `dev-docs/prior-submissions/Assignment-1-Submission.pdf` -- QuickBite
  definition, 8 business services, Sierra-Lima ownership table.
- `dev-docs/prior-submissions/Assignment-1_Feedback.txt` -- 3.50/4.00, minus
  0.25 for infra in architecture diagram and minus 0.25 for shared DB.
- `dev-docs/prior-submissions/Assignment-2-Submission.pdf` -- DDD domain
  model; 22 requirements R1 to R22; Sierra-Lima reqs R19-R22; aggregate
  roots and value objects; cross-service references.
- `dev-docs/prior-submissions/Assignment-3-Submission.pdf` -- REST endpoints
  for all services, data models, workflows, integration mechanisms, and
  implementation responsibilities. Extracted via `pdftotext -layout` after
  an earlier session found the PDF to be password-protected for `Read`.
- `dev-docs/prior-submissions/assignment-3_figure*.png` -- five figures:
  business architecture, implementation architecture, service ER diagrams,
  W1 sequence, W2/W3 events.

The master plan's Appendix F pins the following A3 facts so the plan can be
executed without reopening the PDFs:

- Restaurant Service endpoints (six; Table F.1) with the availability
  response shape.
- Menu Service endpoints (six; Table F.2) with the batch-validation request
  and response shapes.
- Restaurant and Menu data models (Tables F.3 and F.4).
- Gateway path map (Table F.5).
- W1 synchronous call chain (Section F.6) with failure handling.
- W2 / W3 event contracts (Section F.7) with optional `menu-events` stretch.
- Other-service data model quick reference (Section F.8).

## Local Repository State Observed

At session start (commit `aac68b0` on branch `dev`):

- `services/restaurant-service/`, `services/menu-service/`,
  `services/local-dev/` contained only README placeholders.
- `dev-docs/roadmaps/` contained four phased-roadmap variants:
  - `Charlie-Lima-Alfa_2ce188a_project-phases.md`
  - `Charlie-Lima-Alfa_aac68b0_project-phases.md`
  - `Golf-Papa-Tango_2ce188a_project-phases.md`
  - `Golf-Papa-Tango_aac68b0_project-phases.md`
- Untracked: `.idea/`, `.claude/` (intentionally ignored via `.gitignore`).

By mid-session (after commit and push), `HEAD` moved to `a520963`:

> `a520963 Add audited and patched phased implementation roadmaps for aac68b0 baseline`

followed by:

> `aac68b0 Expand course materials index, add Assignment 3 figures and source files, and ignore IDE/Claude local settings`

At session end:

- Working tree clean **except** for the final master plan
  (`Charlie-Lima-Alfa_a520963_project-phases-final.md`) and this chat archive,
  which are untracked at the time of writing.
- Branch `dev` is up-to-date with `origin/dev` up through `a520963`.

## Files Created or Updated During This Chat

### Created

- `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
  -- the final master plan for Sierra-Lima's part of the project. About
  2,237 lines. Structure: §0 use instructions + source artifacts; §1
  ownership scope and key dates; §2 planning principles (10); §3 delivery
  strategy and cross-phase working assets; §4 baseline project scope
  (business system, implementation subset, team ownership, Sierra-Lima
  domain recap, design decisions); §5 technology stack + working
  implementation defaults; §6 named workflows (W1, W2, W3); §7 explicit
  assumptions (10); §8 phase map; §9 detailed phase plan (Phases 0-19);
  §10 checkpoint readiness gates; §11 solo-capable work + stubbing
  strategy; §12 final demo success criteria; §13 bottom line. Appendices
  A-H: 23-session calendar, compression guidance, team coordination
  points, 14-item risk register, A1 feedback actions, canonical A3
  reference, directory conventions, and "memory for future agents".
- `dev-docs/agent-context/2026-04-18_chat-archive_Charlie-Lima-Alfa_a520963.md`
  -- this archive.

### Updated

- `dev-docs/roadmaps/Charlie-Lima-Alfa_aac68b0_project-phases.md` -- eight
  audit-driven patches applied (see *Patches* below). Committed as part of
  `a520963`.
- `dev-docs/roadmaps/Golf-Papa-Tango_aac68b0_project-phases.md` -- audited
  and committed as part of `a520963` (no patches applied; integrated its
  strengths into the Charlie-Lima-Alfa master plan instead).

### Read (not modified)

- `dev-docs/roadmaps/Charlie-Lima-Alfa_2ce188a_project-phases.md` -- older
  Charlie-Lima-Alfa roadmap; compared against `aac68b0` to audit what
  changed and what could be consolidated.
- `dev-docs/roadmaps/Golf-Papa-Tango_2ce188a_project-phases.md` -- older
  Golf-Papa-Tango roadmap; read to confirm no additional strengths were
  being missed.
- `dev-docs/agent-context/2026-04-17_chat-archive_Charlie-Lima-Alfa_b99b261.md`
- `dev-docs/agent-context/2026-04-17_chat-archive_Golf-Papa-Tango_2ce188a.md`
- `dev-docs/prior-submissions/Assignment-1-Submission.pdf`
- `dev-docs/prior-submissions/Assignment-1_Feedback.txt`
- `dev-docs/prior-submissions/Assignment-2-Submission.pdf`
- `dev-docs/prior-submissions/Assignment-3-Submission.pdf` (via
  `pdftotext -layout`, 568 lines of extracted text)
- `dev-docs/course-materials/README.md`
- `services/*/README.md`, `services/local-dev/README.md`, repo-root
  `README.md`

### Memory files touched

- `~/.claude/projects/.../memory/MEMORY.md` -- already contained
  `user_profile.md` and `project_quickbite.md`; no changes required.

## Audit Findings and Patches Applied to `Charlie-Lima-Alfa_aac68b0_project-phases.md`

The audit concluded the `aac68b0` roadmap was largely sufficient for
Sierra-Lima's portion of the project but had eight concrete gaps or
under-specifications compared to a strict implementation-ready baseline.
Each was patched in place:

1. **Spring Security bootstrap caveat** -- Adding
   `spring-boot-starter-security` locks every endpoint behind a default form
   login. The Phase 2 DoD requires `/actuator/health` to return 200, which
   fails by default. Patch: added a warning block and a concrete
   `SecurityConfig` stub (`permitAll()` + stateless session) to place
   immediately after Spring Initializr generation, to be replaced by the
   real JWT filter in Phase 7.
2. **`jjwt` dependency explicitness** -- Spring Initializr does not include
   JJWT. Patch: added the three `jjwt` dependencies (api, impl, jackson,
   all `0.11.5`) to Phase 2, so a future agent does not overlook them.
3. **`DevTokenGenerator` utility** -- Phase 7 wired token validation without
   first producing test tokens. Patch: added an explicit dev JWT generator
   utility (HS256 with Base64-encoded `DEV_SECRET` and `jwt.secret` property)
   so tokens can actually be tested before the real User Service exists.
4. **Resilience4j scope note** -- Phase 10 treated Resilience4j as
   mandatory. Per A3, Sierra-Lima has no outbound REST calls and is not a
   Kafka producer or consumer. Patch: scoped the resilience tasks as
   optional; resilient-callee hardening (timeouts, mapped errors) remains
   in scope regardless.
5. **Phase 3 DoD clarification** -- The original DoD for Phase 3 implied
   full validation and OpenAPI were required. Patch: scoped Phase 3's DoD
   to "basic working" and deferred full validation and OpenAPI to Phase 4,
   matching the titles.
6. **No-DELETE note on Restaurant Service** -- Some contributors might
   reach for a DELETE endpoint out of habit. Patch: added an explicit note
   that there is no `DELETE /restaurants/{id}`; status toggling uses
   `PATCH /{id}/status`, and a future soft-delete uses a `status: INACTIVE`
   field.
7. **CORS + Security integration** -- A bare `@CrossOrigin` annotation on
   controllers is silently overridden by Spring Security's filter chain.
   Patch: specified a global `CorsConfigurationSource` bean wired through
   `SecurityFilterChain` via `http.cors(cors -> cors.configurationSource(...))`.
8. **Phase 14 report update** -- The original Phase 14 did not include
   refreshing the report draft. Patch: added an explicit report-update task
   (data models, endpoint tables, diagrams) to prevent last-minute drift
   between implementation and diagrams.

These patches were committed as part of `a520963`.

## Credit Purchase Clarification

The user asked whether purchasing EUR 20 of additional Anthropic credit
would be usable in this environment. Short answer confirmed: yes, the
credit is purchased at the Anthropic Console and is consumed by the same
API that Claude Code uses, regardless of which Claude Code tier is active.
No special configuration is required for the additional credit to be
available.

## What the Final Master Plan Contains (at a glance)

- 20 phases (Phase 0 to Phase 19) of focused 3-hour working sessions.
- Three workflow labels (W1 sync place-order, W2 async delivery events,
  W3 async payment events), with Sierra-Lima being a callee only in W1 and
  not a producer or consumer in W2/W3 under the baseline A3 scope.
- Explicit prerequisites per phase, including both prior-phase and
  teammate dependencies.
- Concrete acceptance criteria and a Definition-of-Done checklist per
  phase.
- A 23-session suggested calendar anchored to 2026-04-18, ending before
  2026-05-19 (CP#3) with buffer.
- Compression guidance for slipped phases.
- Team coordination points with who-agrees-what-when.
- A 14-item risk register.
- Assignment 1 feedback addressed inline.
- Canonical A3 reference (endpoints, payloads, data models, gateway map,
  W1 chain, W2/W3 events) in Appendix F, pinned so the A3 PDF does not
  need to be reopened during execution.
- Directory conventions and a "memory for future agents" appendix.

## Strengths Integrated from Earlier Roadmaps

From **Charlie-Lima-Alfa_aac68b0** (the audited and patched direct
predecessor):

- Full contract pinning for endpoints, payloads, and data models.
- Phased Definition-of-Done checklists.
- The eight patches listed above.

From **Golf-Papa-Tango_aac68b0** (the parallel callsign's roadmap):

- Section 3.7 "Working implementation defaults for Sierra-Lima" -- lifted
  into the master plan's §5.1 as six numbered defaults.
- Flyway migrations from the first persistent version onward; the master
  plan specifies `V1__init.sql` and `V2__seed_demo_data.sql` migrations
  with explicit SQL, plus `spring.jpa.hibernate.ddl-auto=validate` once
  the first migration is in place.
- A 24-session calendar idea, compressed here into a 23-session calendar
  that matches the remaining runway from 2026-04-18.

From **Charlie-Lima-Alfa_2ce188a** (older variant):

- The strategic shape (re-baseline -> build Sierra-Lima services ->
  assemble W1 -> layer frontend -> harden authorisation -> finalise).
- Bottom-line summary closing the document.

## Workspace Notes

- The session used the main conversation context together with file tools
  (Read, Write, Edit, Glob, Grep, Bash) and TaskList / TaskUpdate for
  tracking.
- No destructive git operations were used.
- Task tracking visible at session end:
  - #1 [completed] Read all prior submissions (A1, A2, A3) in full
  - #2 [completed] Read existing roadmaps and agent-context archives
  - #3 [deleted] Fetch ESI course webpage and Practicals page
  - #4 [deleted] Read key course lecture PDFs
  - #5 [completed] Explore current repo scaffold and service directories
  - #6 [completed] Write master plan Markdown file
- Tasks #3 and #4 were deleted because sufficient context was already
  available from the committed course materials and prior submissions to
  write a thorough master plan without fetching external pages or reading
  additional lecture PDFs.
- `.idea/` and `.claude/` continue to be ignored by `.gitignore`.

## Suggested Next Steps For a Future Agent

1. Review and commit the final master plan
   `Charlie-Lima-Alfa_a520963_project-phases-final.md` and this archive
   file to the `dev` branch once the user has reviewed them.
2. Begin **Phase 0 -- Scope Freeze & Repo Conventions** from the final
   master plan (§9):
   - Record the implementation subset, frozen workflows, folder layout,
     conventions, open design questions, and non-goals in
     `dev-docs/decisions/`.
3. Proceed to **Phase 1 -- Auth & Gateway Contract Alignment**, ideally
   after lightweight coordination with Alfa-Kilo. If Alfa-Kilo is not
   available, use the §5.1 defaults from the master plan and log any
   unilateral choices in the decisions log.
4. Begin **Phase 2 -- Contract Pack & Local-Dev Bootstrap**: freeze the
   12 endpoints, seed SQL, Flyway `V1__init.sql`, Spring Boot
   scaffolding, Spring Security stub, PostgreSQL via Docker Compose, and
   Postman workspace.
5. Update the memory index after any significant milestone so
   conversations can pick up without re-deriving context from scratch.

## Workspace Safety Notes

- No files were deleted or truncated in this session.
- No git force operations were used; only a standard `push` to `origin/dev`.
- The `.gitignore` continues to exclude `.idea/` and `.claude/`.
- No secrets were committed; no `.env` files were created during this
  session.
