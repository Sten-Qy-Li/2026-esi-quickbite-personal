# Chat Archive - 2026-04-17 - Golf-Papa-Tango (`2ce188a`)

## Session Summary

This chat focused on creating and then refining a long-horizon project-phase plan for the QuickBite ESI project in Sierra-Lima's personal early-start repository.

Two main tasks were completed:

1. Create a comprehensive phased roadmap from empty scaffold to final-presentation readiness.
2. Read the existing `Charlie-Lima-Alfa_2ce188a_project-phases.md` plan and integrate its strongest practical advantages into the new `Golf-Papa-Tango_2ce188a_project-phases.md` plan.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Required course references:
  - `https://courses.cs.ut.ee/2026/esi/spring`
  - `https://courses.cs.ut.ee/2026/esi/spring/Main/Practicals`
  - children of those pages as needed
- Important team context:
  - this course project is group work
  - Sierra-Lima wants an early head start
  - Assignments 1 to 3 are already stored locally

## Critical Constraints From This Chat

### First task constraint

When first writing the new project-phase plan, the agent was explicitly told:

- do **not** read the contents of `Charlie-Lima-Alfa_2ce188a_project-phases.md`

That instruction was respected during the first planning pass.

### Second task change

Later in the same chat, the user explicitly changed the instruction and asked the agent to:

- read `Charlie-Lima-Alfa_2ce188a_project-phases.md`
- integrate its strengths into `Golf-Papa-Tango_2ce188a_project-phases.md`

That second instruction superseded the earlier restriction for the refinement step.

## Course and Project Information Used

The roadmap work was based on the current 2025/26 ESI course pages and the local assignment artifacts.

### Key course pages consulted

- `https://courses.cs.ut.ee/2026/esi/spring`
- `https://courses.cs.ut.ee/2026/esi/spring/Main/Practicals`
- `https://courses.cs.ut.ee/2026/esi/spring/Main/ProjectAndExam`
- practical child pages related to:
  - Spring Boot CRUD
  - OpenAPI/Swagger
  - microservice communication
  - API Gateway
  - resilient microservices
  - Kafka/event-driven integration
  - Vue.js
  - Spring Security / JWT

### Important absolute milestone dates used

- `2026-04-21`: project description practical
- `2026-04-28`: project consultation practical
- `2026-05-05`: project checkpoint #1, backend
- `2026-05-12`: project checkpoint #2, frontend + backend
- `2026-05-19`: project checkpoint #3, final presentation

Note:

- the practicals page showed `28/04/2025` for consultation, but in context this was treated as a likely typo and normalized to `2026-04-28`

## Local Repository State Observed

At the time of this chat:

- the repository was mostly scaffolded
- `services/restaurant-service/`, `services/menu-service/`, and `services/local-dev/` contained only README placeholders
- `dev-docs/` already contained:
  - course materials
  - prior submissions
  - the Charlie-Lima-Alfa phases plan
- `rg` was not installed in the environment, so PowerShell file discovery was used instead

## Assignment and Submission Context Used

The agent extracted and used local assignment context from:

- `dev-docs/course-materials/Assignment_1_2026.pdf`
- `dev-docs/course-materials/Assignment_2_2026.pdf`
- `dev-docs/course-materials/Assignment_3_2026.pdf`
- `dev-docs/prior-submissions/Assignment-1-Submission.pdf`
- `dev-docs/prior-submissions/Assignment-2-Submission.pdf`
- `dev-docs/prior-submissions/Assignment-3_Submission.pdf`
- `dev-docs/prior-submissions/Assignment-1_Feedback.txt`

Important design constraints carried forward from the assignments and feedback:

- keep business architecture separate from infrastructure
- no shared database across microservices
- use ID references across service boundaries
- demonstrate both synchronous and asynchronous integration
- Sierra-Lima owns `Restaurant Service` and `Menu Service`
- Assignment 3 implementation subset stays focused on:
  - business services: `Order`, `Restaurant`, `Menu`, `Payment`, `Delivery`, `Notification`
  - shared components: `API Gateway`, `Kafka/Event Broker configuration`
  - design-only unless feedback changes scope: `User`, `Review`

## Files Created or Updated During This Chat

### Created first

- `dev-docs/roadmaps/Golf-Papa-Tango_2ce188a_project-phases.md`

This became the main phased roadmap.

### Later updated

- `dev-docs/roadmaps/Golf-Papa-Tango_2ce188a_project-phases.md`

It was refined after reading `Charlie-Lima-Alfa_2ce188a_project-phases.md`.

### Read during refinement

- `dev-docs/roadmaps/Charlie-Lima-Alfa_2ce188a_project-phases.md`

## What The Final Golf-Papa-Tango Plan Now Contains

The final refined roadmap includes:

- a 17-phase plan made of 3-hour sessions
- fixed checkpoint alignment from scaffold to final presentation
- Sierra-Lima-first execution strategy
- explicit no-shared-database rule
- implementation subset from Assignment 3
- technology baseline
- Sierra-Lima domain recap for `Restaurant Service` and `Menu Service`
- cross-phase working assets:
  - Postman collection
  - Swagger/OpenAPI
  - seed data
  - Docker Compose runbook
  - health checks
  - demo backups
- more concrete bootstrap tasks
- more concrete service hardening tasks
- stubbing guidance for late team integration
- optional resilience guidance
- suggested session calendar
- coordination trigger table

## Specific Strengths Imported From Charlie-Lima-Alfa

These were judged worth carrying over into the Golf-Papa-Tango version:

1. A clearer technology stack baseline.
2. A tighter recap of Sierra-Lima's assignment-derived domain ownership.
3. More explicit environment/bootstrap expectations.
4. Stronger emphasis on Swagger, Postman, seed data, and health checks as ongoing assets.
5. Better late-team stubbing guidance.
6. A practical session calendar.
7. Explicit coordination checkpoints with the team.
8. Optional resilience as a stretch value-add rather than mandatory scope.

## Current Recommended Reference File

For future planning or execution, the primary roadmap to follow is:

- `dev-docs/roadmaps/Golf-Papa-Tango_2ce188a_project-phases.md`

The Charlie-Lima-Alfa file should now be treated as a secondary historical reference, because the strongest parts of it have already been folded into the Golf-Papa-Tango roadmap.

## Suggested Next Steps For A Future Agent

If work continues from this chat, the highest-value next actions are:

1. Start implementing Phase `01` to Phase `03` from the Golf-Papa-Tango roadmap.
2. Set up the actual code skeleton for `Restaurant Service` and `Menu Service`.
3. Create the local Docker Compose and service configuration baseline.
4. Record decisions and contracts in `dev-docs/decisions` as implementation begins.
5. Keep the roadmap updated if instructor feedback changes the implementation subset.

## Workspace Notes

- Existing unrelated or pre-existing untracked files were left untouched.
- No destructive git operations were used.
- No code implementation work was started in the service folders during this chat; the work was documentation and planning only.
