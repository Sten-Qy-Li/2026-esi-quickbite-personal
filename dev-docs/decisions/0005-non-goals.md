# 0005 -- Non-Goals for the First Implementation Pass

- **Status:** Accepted
- **Date:** 2026-04-18
- **Author:** Charlie-Lima-Alfa (for Sierra-Lima)
- **Base commit:** `7c5daba`
- **Source:**
  `dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md` §9
  Phase 0 Task 6.

## Context

The QuickBite project is scoped to what Assignment 3 asks for, plus
integration work. Every extra feature delays the first integration and
raises the risk of missing a checkpoint. This document lists things that
are **deliberately out of scope** for the first implementation pass, so
nobody on the team spends a session on them by accident.

"First implementation pass" means from Phase 0 through Checkpoint #3
(2026-05-19). Items below may be added back as future work after the
project ends, or earlier if the instructor explicitly asks.

## Non-Goals

### N1 -- No frontend before CP#1 prep

- **What.** The Vue.js frontend (`services/frontend/`) is not created
  until Phase 12, immediately after Checkpoint #1.
- **Why.** Backend APIs, auth, and W1 integration are the scoring spine
  of CP#1. A half-built frontend before CP#1 steals time from the backend
  and gives the CP#1 reviewer nothing extra.

### N2 -- No real payment gateway integration

- **What.** Payment Service does not integrate with Stripe, PayPal, or
  any real payment processor. It simulates success and failure outcomes.
- **Why.** Real gateways require accounts, test keys, and extra compliance
  worry. The course requires demonstrating the integration pattern, not
  end-to-end real payments.
- **Owner.** Elephant-Yankee. Sierra-Lima's only responsibility is not to
  request real money.

### N3 -- No mobile app

- **What.** No Android or iOS native app, and no React Native / Flutter
  client.
- **Why.** Vue.js web app is what the course practicals and Assignment 3
  target. Mobile is pure scope creep.

### N4 -- No Eureka / service discovery

- **What.** Services are addressed by static hostnames from environment
  variables, not through a discovery server.
- **Why.** Master plan §5 and Assignment 3 §6.1 both mark service
  discovery as out of the baseline. Eureka is optional only. Adding it
  doubles the Docker Compose complexity without demoable benefit.

### N5 -- No second data store per service

- **What.** Each implemented service has exactly one PostgreSQL database.
  No Redis, no MongoDB, no secondary search index.
- **Why.** Assignment 1 feedback already penalised data-layer shortcuts;
  keeping one DB per service keeps the story simple.

### N6 -- No client-side load balancing

- **What.** No Spring Cloud LoadBalancer, Ribbon, or equivalent on
  inter-service REST calls.
- **Why.** Same reason as N4: complexity without measurable benefit at
  this scale. The API Gateway handles external routing; internal calls
  use direct env-var URLs.

### N7 -- No distributed tracing

- **What.** No Sleuth / Zipkin / OpenTelemetry plumbing.
- **Why.** Nice to have, not on the CP#1-CP#3 scoring sheet. Logs are
  enough to explain request paths during the demo.

### N8 -- No production secrets management

- **What.** Dev JWT signing secret is a Base64 shared HS256 key in
  `.env.local`. No Vault, no AWS KMS.
- **Why.** Student project. The env-var name stays stable
  (`JWT_SECRET`) so a future production swap is a config change only.

### N9 -- No real email / SMS / push delivery

- **What.** Notification Service logs notifications or persists them, but
  does not actually send an email, SMS, or push notification.
- **Why.** Out of scope for the checkpoints. Owner: Mike-Alfa.

### N10 -- No second region, no HA, no backups

- **What.** Single-region, single-host Docker Compose. No replica sets.
  No backup strategy for the PostgreSQL volumes.
- **Why.** Student project running on a laptop.

### N11 -- No feature flags or A/B experimentation

- **What.** No LaunchDarkly, no GrowthBook, no home-grown flag gate.
- **Why.** Unnecessary for a system with one deployed build and one demo
  audience.

### N12 -- No continuous deployment pipeline

- **What.** No GitHub Actions deploy job, no Render / Railway / Fly
  integration. `docker compose up --build` on the demo host is the
  deploy step.
- **Why.** The checkpoint demos happen on one machine.

### N13 -- No `Review Service` code

- **What.** `Review Service` is design-only per `0001-scope-freeze.md`.
- **Why.** A3 §2.4 explicitly marks it as design-only. Recorded here so
  nobody accidentally scaffolds it.

## Consequences

- Any pull request that adds one of the above needs a superseding
  decision document that explains **why scope has grown**.
- "We had time, so I added X" is not a valid reason. Extra time goes into
  hardening, tests, or report quality -- not into bypassing non-goals.

## Supersedes

None.
