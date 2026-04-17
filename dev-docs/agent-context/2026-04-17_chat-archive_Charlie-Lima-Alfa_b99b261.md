# Chat Archive - 2026-04-17 - Charlie-Lima-Alfa (`b99b261`)

## Session Summary

This chat focused on creating a comprehensive phased project plan for the QuickBite ESI project, then merging strengths from a parallel plan, and finally committing and pushing to GitHub.

Three main tasks were completed:

1. Create a comprehensive phased roadmap (`Charlie-Lima-Alfa_2ce188a_project-phases.md`) from empty scaffold to final-presentation readiness.
2. Read the existing `Golf-Papa-Tango_2ce188a_project-phases.md` plan and integrate its strongest strategic advantages into the Charlie-Lima-Alfa plan.
3. Commit all new files and push to `origin/dev`.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Required course references:
  - `https://courses.cs.ut.ee/2026/esi/spring`
  - `https://courses.cs.ut.ee/2026/esi/spring/Main/Practicals`
  - children of those pages as needed
- Important team context:
  - this course project is group work (Group 7)
  - teammates: Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
  - Sierra-Lima wants an early head start; teammates are last-minute types
  - Assignments 1 to 3 are already stored locally

## Course and Project Information Used

### Key course pages consulted

- `https://courses.cs.ut.ee/2026/esi/spring/Main/HomePage`
- `https://courses.cs.ut.ee/2026/esi/spring/Main/Practicals`
- `https://courses.cs.ut.ee/2026/esi/spring/Main/ProjectAndExam`
- Practical sub-pages consulted:
  - `PS041` -- microservice inter-service communication (WebClient)
  - `PS051` -- microservices discovery & load balancing (Eureka)
  - `PS061` -- API Gateway (Spring Cloud Gateway)
  - `PS072` -- resilient microservices (Resilience4j: circuit breaker, retry, time limiter, rate limiter)
  - `PS081` -- Kafka event-driven integration (producer/consumer with string and JSON messages)
  - `PS082` -- Kafka bidirectional event flow (order-created -> payment-processed)
  - `PS091` -- Vue.js / Node.js setup
  - `PS092` -- Vue.js basics and Fetch API
  - `PS093` -- Vue.js CRUD with Spring Boot backend (Fetch API, component structure, routing)
  - `PS101` -- Spring Boot Security (in-memory auth, BCrypt, role-based access)
  - `PS111` -- JWT authentication service (JwtAuthFilter, JwtService, stateless sessions)
  - `PS121` -- Secure full-stack (Spring Boot + Vue.js with JWT, localStorage token, route guards)

### Important absolute milestone dates used

- `2026-04-21`: project description practical
- `2026-04-28`: project consultation practical (practicals page shows `28/04/2025`, treated as typo)
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
- Assignment 1 score: 3.50/4.00

## Assignment and Submission Context Used

The agent read and extracted context from:

- `dev-docs/course-materials/Assignment_1_2026.pdf` -- system definition, 8 microservices, architecture
- `dev-docs/course-materials/Assignment_2_2026.pdf` -- DDD domain model, entities, value objects, aggregates
- `dev-docs/course-materials/Assignment_3_2026.pdf` -- REST APIs, data models, workflows, integration, implementation responsibilities
- `dev-docs/prior-submissions/Assignment-1-Submission.pdf` -- QuickBite system definition and architecture
- `dev-docs/prior-submissions/Assignment-2-Submission.pdf` -- domain model with DDD elements
- `dev-docs/prior-submissions/Assignment-3_Submission.pdf` -- could not read (password-protected)
- `dev-docs/prior-submissions/Assignment-1_Feedback.txt` -- 3.50/4.00, penalties for infra in diagram and shared DB

### Design constraints carried forward

- Keep business architecture separate from infrastructure in diagrams
- No shared database across microservices (each service owns its own PostgreSQL)
- Cross-service references use ID fields only
- Demonstrate both synchronous (REST) and asynchronous (Kafka) integration
- Sierra-Lima owns Restaurant Service and Menu Service
- Assignment 3 implementation subset:
  - Implemented business services: Order, Restaurant, Menu, Payment, Delivery, Notification
  - Shared components: API Gateway, Kafka/Event Broker
  - Design-only: User, Review

## Local Repository State Observed

At session start (commit `2ce188a` on `dev` branch):

- `services/restaurant-service/`, `services/menu-service/`, `services/local-dev/` contained only README placeholders
- `dev-docs/` had scaffold READMEs in sub-directories
- Untracked: `.idea/`, course material PDFs, prior submission files

By session end (commit `b99b261` on `dev` branch):

- All course materials, submissions, feedback, roadmaps, and chat archive committed and pushed
- Only `.idea/` and `.claude/` remain untracked (intentionally excluded)

## Files Created or Updated During This Chat

### Created

- `dev-docs/roadmaps/Charlie-Lima-Alfa_2ce188a_project-phases.md` -- primary phased roadmap (initially 16 phases)

### Later rewritten (merged with Golf-Papa-Tango strengths)

- `dev-docs/roadmaps/Charlie-Lima-Alfa_2ce188a_project-phases.md` -- expanded to 20 phases with strategic sections

### Staged and committed (not created by this chat, but committed)

- `dev-docs/course-materials/` -- 16 PDF files (assignments, lectures, intro)
- `dev-docs/course-materials/README.md` -- modified with course links
- `dev-docs/prior-submissions/` -- 6 files (PDFs, DOCX, feedback)
- `dev-docs/roadmaps/Golf-Papa-Tango_2ce188a_project-phases.md` -- pre-existing, committed
- `dev-docs/agent-context/2026-04-17_chat-archive_Golf-Papa-Tango_2ce188a.md` -- pre-existing, committed

### Memory files created

- `~/.claude/projects/.../memory/user_profile.md` -- Sierra-Lima student profile
- `~/.claude/projects/.../memory/project_quickbite.md` -- QuickBite project overview
- `~/.claude/projects/.../memory/MEMORY.md` -- memory index

## What The Final Charlie-Lima-Alfa Plan Contains

The merged roadmap includes:

- 20 phases (0-19) across ~60 hours of 3-hour sessions
- 11 structured sections plus 4 appendices
- Strategic sections integrated from Golf-Papa-Tango:
  - Named Workflows (W1 sync place-order, W2 async delivery, W3 async payment)
  - Planning Principles (9 rules including "reject shared DB immediately")
  - Delivery Strategy with cross-phase working assets
  - Implementation Subset clarity (built vs design-only)
  - Checkpoint Readiness Gates (go/no-go per checkpoint)
  - What Sierra-Lima Can Do Solo (7 high-value early tasks + stubbing strategy)
  - Final Demo Success Criteria (7 concrete demonstration goals)
  - Bottom Line strategic summary
- Detailed task lists with definition-of-done checklists per phase (from original CLA)
- Full technical depth (entity fields, annotations, Docker stages, Kafka config) (from original CLA)
- Additional phases from Golf-Papa-Tango:
  - Phase 0: Scope Freeze with non-goals list
  - Phase 1: Contract Pack (freeze payloads/validation before coding)
  - Phase 6: Hardening Pass (demo-grade polish)
  - Phase 11: W1 Assembly (explicit end-to-end workflow wiring)
  - Phase 18: Report & Evidence Pack
  - Phase 19 includes buffer/stabilisation time
- Expanded risk register (10 risks, up from 6)
- Compression guidance for tight schedules
- Enhanced coordination triggers table

## Specific Strengths Imported From Golf-Papa-Tango

1. Scope freeze as a distinct early phase before any coding begins.
2. Contract pack phase -- freeze API payloads, validation rules, and DB schemas before implementation.
3. Named workflow labels (W1, W2, W3) used consistently throughout the plan.
4. Planning principles section -- reusable decision rules for every phase.
5. Delivery strategy section -- explicit 5-step ordering rationale.
6. Cross-phase working assets -- Postman, Swagger, seed data, runbook, health checks, demo backups.
7. Hardening pass concept -- dedicated session to make services demo-grade.
8. Team integration contract lock concept -- formalised team sync point.
9. Backend workflow assembly as an explicit phase (not just per-service work).
10. Checkpoint readiness gates with go/no-go checklists.
11. Solo-capable work section with stubbing strategy.
12. Report & evidence pack as a dedicated phase (was entirely missing).
13. Buffer/stabilisation time before final presentation.
14. Final demo success criteria and bottom line strategic summary.
15. Additional risks: shared-DB shortcut reappearing, scope expansion, security postponement, diagrams falling behind code.
16. Compression guidance -- priority order for what to sacrifice when time is tight.

## Current Recommended Reference Files

For future planning or execution, both roadmaps exist and are complementary:

- `dev-docs/roadmaps/Charlie-Lima-Alfa_2ce188a_project-phases.md` -- detailed tactical plan with full technical task lists, definition-of-done checklists, and strategic framing. This is the most complete version.
- `dev-docs/roadmaps/Golf-Papa-Tango_2ce188a_project-phases.md` -- strategic companion with strong narrative flow and planning philosophy.

The Charlie-Lima-Alfa plan now subsumes the key strengths of both, so it should be treated as the primary reference.

## Suggested Next Steps For A Future Agent

1. Start implementing Phase 0 (scope freeze) and Phase 1 (contract pack + environment bootstrap) from the Charlie-Lima-Alfa roadmap.
2. Scaffold actual Spring Boot projects for Restaurant Service and Menu Service via Spring Initializr.
3. Create the local Docker Compose with separate PostgreSQL containers.
4. Record decisions and contracts in `dev-docs/decisions/` as implementation begins.
5. Add `.idea/` and `.claude/` to `.gitignore` to keep them permanently excluded.
6. Keep the roadmap updated if instructor feedback (after 2026-04-21 or 2026-04-28) changes the implementation subset.

## Workspace Notes

- `.idea/` and `.claude/` were intentionally excluded from the commit.
- No destructive git operations were used.
- No code implementation was started; the session was documentation, planning, and git operations only.
- Assignment 3 submission PDF was password-protected and could not be read.
