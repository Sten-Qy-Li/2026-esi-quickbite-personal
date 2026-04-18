# Chat Archive - 2026-04-18 - Charlie-Lima-Alfa (`696da6d`)

## Session Summary

This session executed **Phase 8 -- Dockerise Both Services** end to
end for the Restaurant Service and Menu Service, as defined in
`dev-docs/roadmaps/Charlie-Lima-Alfa_a520963_project-phases-final.md`
§9 Phase 8 (lines 1091-1152).

The session began on top of `696da6d` ("Land Phase 7 hardening: JWT
filter, route matrix, seeds, tests"). A context-compaction
auto-summary occurred mid-session after Phase 7 finished and Phase 8
had just been scoped (six tracked subtasks). The archived context
below covers the full Phase 8 arc from the initial Dockerfile review
to a green `docker compose up --build` with all four containers
healthy.

All six Phase 8 tasks reached the Definition of Done. One
real-Postgres bug surfaced during end-to-end verification and was
fixed in-session (JPQL `LOWER(:param)` on a nullable string bind
tripped Postgres's strict null-type inference; H2 had been hiding
it). `mvn -B test` still produces 46 passing tests across both
services after the fix. The Docker stack is demo-grade for the CP#1
consultation on 2026-04-28.

## User Context

- Repository: `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal`
- Student pseudonym: `Sierra-Lima`
- Author callsign for this chat: `Charlie-Lima-Alfa`
- Team (Group 7): Alfa-Kilo, Sierra-Lima, Elephant-Yankee, Mike-Alfa
- Services owned by Sierra-Lima: `Restaurant Service`, `Menu Service`
- Today: 2026-04-18 (Saturday)
- Active branch: `dev`
- Parent commit: `696da6d` -- "Land Phase 7 hardening: JWT filter,
  route matrix, seeds, tests"
- Environment: Windows 11 + IntelliJ IDEA 2026.1 + Git Bash
- Docker: `Docker version 29.4.0, build 9d7ad9f`
- Docker Compose: `v5.1.1`
- Java: 17.0.18 (Microsoft OpenJDK) via `ms-17` project SDK
- Base images: `maven:3.9-eclipse-temurin-17` (build),
  `eclipse-temurin:17-jre` (runtime), `postgres:15` (DBs)

## Phase 8 Task-by-Task Record

### Task 1 -- Multi-stage Dockerfiles

Both `services/restaurant-service/Dockerfile` and
`services/menu-service/Dockerfile` already had a two-stage
structure from a prior phase. Two targeted refinements applied:

1. **Maven invocation aligned with the roadmap's example.** The
   build stage now runs
   `mvn -B -DskipTests dependency:go-offline || true` followed by
   `mvn -B -DskipTests package`. The `|| true` on `go-offline` keeps
   transient Maven Central hiccups from failing the whole image
   build -- the subsequent `package` step still has to resolve
   everything, so nothing is silently skipped.
2. **`curl` added to the runtime stage.** The `eclipse-temurin:17-jre`
   base image does not include `curl`, which the compose
   healthcheck relies on. A single Debian install:

   ```dockerfile
   RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
   ```

Exposed ports preserved: Restaurant `8081`, Menu `8082`. Final
Dockerfiles:

```dockerfile
# syntax=docker/dockerfile:1

# Build stage -- uses Maven image with JDK 17 pre-installed.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime stage -- JRE plus curl for the compose healthcheck.
FROM eclipse-temurin:17-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8081   # 8082 in menu-service
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Task 2 -- Extended docker-compose.yml

`services/local-dev/docker-compose.yml` grew two new service entries
alongside the existing `restaurant-db` and `menu-db`:

```yaml
restaurant-service:
  build:
    context: ../restaurant-service
    dockerfile: Dockerfile
  image: quickbite/restaurant-service:local
  container_name: quickbite-restaurant-service
  depends_on:
    restaurant-db:
      condition: service_healthy
  environment:
    SPRING_PROFILES_ACTIVE: docker
    SERVER_PORT: "8081"
    DB_URL: jdbc:postgresql://restaurant-db:5432/${RESTAURANT_DB_NAME:-restaurant_db}
    DB_USER: ${RESTAURANT_DB_USER:-restaurant_user}
    DB_PASSWORD: ${RESTAURANT_DB_PASSWORD:-restaurant_pw}
    JWT_SECRET: ${JWT_SECRET:-dGVzdC1zZWNyZXQtZm9yLWRldi1vbmx5LWRvLW5vdC11c2UtaW4tcHJvZC0xMjM0NTY=}
    JWT_ISSUER: ${JWT_ISSUER:-quickbite-user-service}
  ports:
    - "8081:8081"
  networks:
    - quickbite-net
  healthcheck:
    test: ["CMD-SHELL", "curl -fsS http://localhost:8081/actuator/health | grep -q '\"status\":\"UP\"'"]
    interval: 10s
    timeout: 5s
    retries: 12
    start_period: 30s
```

The `menu-service` entry mirrors this with `menu-db`, port `8082`,
and the `menu_*` env-var names. Both services talk to their DB on
the private `quickbite-net` bridge using the container hostnames
`restaurant-db:5432` / `menu-db:5432` -- so the host-side port
override (`RESTAURANT_DB_HOST_PORT=5442` in `.env.local`) has no
effect on in-container connectivity.

No optional teammate placeholders were added yet (api-gateway,
user-service, order-service, etc.) -- those will be uncommented in
Phase 10+ once teammates' images exist. The roadmap explicitly
marks them as "Optional now, required later."

### Task 3 -- Spring `docker` profile

`application-docker.properties` in each service's
`src/main/resources/` overrides only the three DB connection
properties:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://restaurant-db:5432/${RESTAURANT_DB_NAME:restaurant_db}}
spring.datasource.username=${DB_USER:${RESTAURANT_DB_USER:restaurant_user}}
spring.datasource.password=${DB_PASSWORD:${RESTAURANT_DB_PASSWORD:restaurant_pw}}
```

Everything else (Flyway, JPA, JWT, actuator exposure) is inherited
from `application.properties`. Compose activates the profile via
`SPRING_PROFILES_ACTIVE=docker`; `DB_URL` is also passed as a
compose env var, so in-container Spring is driven from `.env.local`,
not hardcoded.

Plain `mvn spring-boot:run` or IntelliJ Run still works unchanged
because no `docker` profile activates by default.

### Task 4 -- Healthchecks

| Container                      | Test                                                   | Interval / retries |
|--------------------------------|--------------------------------------------------------|--------------------|
| `quickbite-restaurant-db`      | `pg_isready -U restaurant_user -d restaurant_db`       | 5s / 10            |
| `quickbite-menu-db`            | `pg_isready -U menu_user -d menu_db`                   | 5s / 10            |
| `quickbite-restaurant-service` | `curl /actuator/health` and grep `"status":"UP"`       | 10s / 12, `start_period: 30s` |
| `quickbite-menu-service`       | same pattern on port 8082                              | 10s / 12, `start_period: 30s` |

`start_period: 30s` accommodates Spring Boot's ~25-second cold
boot so the first few checks don't fail and flip the container to
`unhealthy`.

### Task 5 -- End-to-end verification

First `docker compose --env-file services/local-dev/.env.local -f
services/local-dev/docker-compose.yml up --build -d`:

- Build stage: Maven dep download took ~3 min (expected on cold
  cache; subsequent builds use Docker layer cache).
- Both DB containers went `healthy` within 15s.
- Both app containers went through `health: starting` for ~30s,
  then `healthy`.

All four containers running:

```
NAME                           STATUS                    PORTS
quickbite-menu-db              Up (healthy)              0.0.0.0:5433->5432/tcp
quickbite-menu-service         Up (healthy)              0.0.0.0:8082->8082/tcp
quickbite-restaurant-db        Up (healthy)              0.0.0.0:5442->5432/tcp
quickbite-restaurant-service   Up (healthy)              0.0.0.0:8081->8081/tcp
```

**Initial smoke tests uncovered a 500 on list endpoints.** Both
`GET /restaurants` and `GET /restaurants/{id}/menu-items` returned
`HTTP 500`. The service logs showed:

```
SQL Error: 0, SQLState: 42883
ERROR: function lower(bytea) does not exist
  Hint: No function matches the given name and argument types.
```

Root cause: both services have a JPQL `search` / `searchForRestaurant`
method with the pattern

```jpql
WHERE (:city IS NULL OR LOWER(r.location.city) = LOWER(:city))
```

When `:city` is null, Hibernate binds the parameter as a null
`String`. The PostgreSQL JDBC driver sends nulls typed as `bytea`
by default. Although the `IS NULL` check short-circuits at runtime,
**Postgres parses and type-checks every branch up front** -- so
`LOWER(bytea)` trips the parser even on the never-evaluated branch.
H2 in PostgreSQL mode (used by the test suite) is lenient about
this and let the pattern through, which is why all 46 tests
passed before the fix.

Fix: JPQL `cast(:city as string)` forces the parameter to bind as
an explicit string type. Applied minimally -- only to the string
params, not the Boolean ones:

```java
// Before
WHERE (:city IS NULL OR LOWER(r.location.city) = LOWER(:city))
  AND (:open IS NULL OR r.open = :open)

// After
WHERE (cast(:city as string) IS NULL
       OR LOWER(r.location.city) = LOWER(cast(:city as string)))
  AND (:open IS NULL OR r.open = :open)
```

An intermediate attempt also cast the `Boolean` param, which
produced a *different* Postgres error -- `cannot cast type bytea to
boolean` -- because Postgres has no implicit cast between those
types. The right fix is "cast strings, leave booleans alone":
Hibernate already binds `Boolean` with `Types.BOOLEAN`, which
Postgres accepts both for `IS NULL` and `=`.

Applied in:
- `restaurant-service/.../RestaurantRepository.java#search`
- `menu-service/.../MenuItemRepository.java#searchForRestaurant`

Post-fix: `mvn -B test` still green (18 + 28 = 46 tests), and the
live stack responds correctly:

| Request                                                          | Result              |
|------------------------------------------------------------------|---------------------|
| `GET  /actuator/health` on 8081 and 8082                         | 200 `{"status":"UP"}` |
| `GET  /restaurants`                                              | 200, 7 rows (6 seed + 1 from Phase 7 test) |
| `GET  /restaurants?city=Tartu&isOpen=true`                       | 200, 3 rows (filter works) |
| `GET  /restaurants/{id}/menu-items`                              | 200, 4 rows         |
| `GET  /menu-items/{id}`                                          | 200, Margherita     |
| `POST /restaurants` (no token)                                   | 401                 |

**Persistence check:** `docker compose stop && docker compose start`
rebinds the app containers to the existing `restaurant_db_data` /
`menu_db_data` volumes. All rows (including the stray "Pizza
Antonio (updated)" created during a Phase 7 manual test) were
still present on the next `GET /restaurants`.

### Task 6 -- .dockerignore per service

`services/restaurant-service/.dockerignore` and
`services/menu-service/.dockerignore` both exclude:

```
target/
.idea/
.claude/
.git/
.gitignore
.mvn/
*.iml
*.log
HELP.md
README.md
Dockerfile
.dockerignore
```

Keeps the Docker build context free of IDE metadata, local git
history, and Claude session artefacts. `.mvn/` is safe to exclude
because the Maven wrapper is not used from inside the container
(the `maven:3.9-eclipse-temurin-17` base image ships `mvn` directly).

### Documentation refresh (not a numbered Task, but part of the phase)

- `services/local-dev/README.md` -- updated the layout listing to
  reflect that `docker-compose.yml` now also contains the two
  Spring Boot services, updated the Compose topology section with
  all four containers, replaced the Phase 2 "current state"
  checklist with a Phase 8 one.
- `services/local-dev/runbook.md` -- new §2 describes
  `docker compose up --build -d` as the primary start command,
  notes the ~3-minute cold-build time, and lists all four expected
  container names for the health check. §7 was renamed "Optional:
  run the services from IntelliJ against Compose DBs" to keep that
  mode documented for fast iteration; the IntelliJ env-var
  override example is preserved there.
- `dev-docs/verification/phase-8-verification_Sierra-Lima.md` --
  new sign-off doc parallel to Phase 7, covering all six tasks,
  the `lower(bytea)` bug plus its fix, and the DoD checklist.

## Key decisions within Phase 8

- **Keep the `docker` profile minimal.** `application-docker.properties`
  overrides only the three DB connection properties, not JWT or
  actuator settings. The compose file passes JWT and DB env vars
  explicitly so the main `application.properties` defaults still
  provide sensible fallbacks for non-Docker runs.
- **Accept `start_period: 30s` on app healthchecks.** Spring Boot
  cold-boots in ~25 seconds in these images. A shorter `start_period`
  would cause the first 2-3 healthchecks to fail and mark the
  container `unhealthy`, breaking `depends_on: service_healthy` for
  any future downstream consumer.
- **Use `cast(... as string)` rather than service-layer routing.**
  Branching in the service (4 filter permutations) would have
  bloated the code and demanded either a new repository method
  per branch or an in-memory filter that scales poorly. The one-
  line JPQL cast is the minimal correct fix and keeps the existing
  repository contract.
- **Leave `depends_on: service_healthy` on DBs only, not services.**
  The two services do not call each other (Sierra-Lima's scope).
  Wiring them together would create a false coupling for demo
  purposes.
- **No teammate placeholders yet.** Roadmap flags api-gateway,
  user-service, etc. as "optional now, required later." Phase 10+
  will handle them when real images exist.

## Commands executed during the session

```bash
# Build and start
docker compose --env-file services/local-dev/.env.local \
  -f services/local-dev/docker-compose.yml up --build -d

# Health polling (until both app containers healthy)
until [ "$(docker inspect -f '{{.State.Health.Status}}' quickbite-restaurant-service)" = "healthy" ] \
   && [ "$(docker inspect -f '{{.State.Health.Status}}' quickbite-menu-service)" = "healthy" ]; do
  sleep 3
done

# Smoke tests
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8081/restaurants
curl 'http://localhost:8081/restaurants?city=Tartu&isOpen=true'
curl http://localhost:8082/restaurants/d0000001-0000-0000-0000-000000000001/menu-items
curl http://localhost:8082/menu-items/e0000011-0000-0000-0000-000000000011

# Persistence check
docker compose --env-file services/local-dev/.env.local \
  -f services/local-dev/docker-compose.yml stop
docker compose --env-file services/local-dev/.env.local \
  -f services/local-dev/docker-compose.yml start

# Tests after the JPQL fix
mvn -f services/restaurant-service/pom.xml -B test    # 18 green
mvn -f services/menu-service/pom.xml -B test          # 28 green
```

## Files changed in this session

Modified (7):
- `services/restaurant-service/Dockerfile`
- `services/menu-service/Dockerfile`
- `services/local-dev/docker-compose.yml`
- `services/local-dev/README.md`
- `services/local-dev/runbook.md`
- `services/restaurant-service/src/main/java/ee/ut/esi/quickbite/restaurant/repository/RestaurantRepository.java`
- `services/menu-service/src/main/java/ee/ut/esi/quickbite/menu/repository/MenuItemRepository.java`

New (5):
- `services/restaurant-service/.dockerignore`
- `services/menu-service/.dockerignore`
- `services/restaurant-service/src/main/resources/application-docker.properties`
- `services/menu-service/src/main/resources/application-docker.properties`
- `dev-docs/verification/phase-8-verification_Sierra-Lima.md`

Plus this session archive.

## Pre-existing context reused

- Phase 7 JWT wiring (`JwtAuthFilter`, `JwtDevMint`, `CurrentUser`)
  works unchanged inside Docker: the compose file passes
  `JWT_SECRET` and `JWT_ISSUER` so tokens minted by the Postman
  collection's pre-request script are accepted by the in-container
  services.
- Flyway migrations (`V1__init.sql`, `V2__seed_demo_data.sql`) run
  automatically on first container start; `ON CONFLICT DO NOTHING`
  makes them idempotent across compose restarts.
- `.env.local` (git-ignored) supplies `RESTAURANT_DB_HOST_PORT=5442`
  to dodge the Windows host-level PostgreSQL service on 5432. Does
  not affect in-container connectivity.

## Stack state at session end

The four containers are running and healthy. The user can:

- Demo immediately: endpoints at `http://localhost:8081` and `:8082`,
  Postman collection still works unchanged.
- Tear down without losing data: `docker compose stop`.
- Tear down and reset: `docker compose down -v`.
- Rebuild after source changes:
  `docker compose up --build -d` (layer cache makes this fast once
  Maven deps are cached).

## Phase 8 Definition of Done

- [x] `docker compose up --build` starts everything from scratch.
- [x] Both services reachable and functional (public endpoints
      return real seed data; mutation endpoints auth-gated per
      Phase 7).
- [x] Databases have persistent volumes; data survives `stop && start`.
- [x] No shared database (one Postgres container per service).
- [x] `docker compose down` stops cleanly.
