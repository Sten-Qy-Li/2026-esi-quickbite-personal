# Phase 8 Verification -- Sierra-Lima

Scope: `Charlie-Lima-Alfa_a520963_project-phases-final.md` Phase 8
("Dockerise Both Services") for Restaurant Service and Menu Service.

Date: 2026-04-18. Target CP#1 consultation: 2026-04-28.

---

## 1. Multi-stage Dockerfiles (Task 1)

Both `services/restaurant-service/Dockerfile` and
`services/menu-service/Dockerfile` are two-stage:

1. **Build stage** -- `maven:3.9-eclipse-temurin-17`. Copies `pom.xml`
   first, primes the dependency cache (`mvn -B -DskipTests
   dependency:go-offline`), then copies `src/` and produces the fat
   jar (`mvn -B -DskipTests package`). `|| true` on the go-offline
   step absorbs transient Maven Central hiccups so a second `mvn
   package` still resolves everything.
2. **Runtime stage** -- `eclipse-temurin:17-jre` plus `curl` (needed
   for the compose healthcheck; the JRE image does not ship one).
   Copies only the jar, exposes `8081` / `8082`, runs `java -jar`.

Port-per-service: Restaurant `EXPOSE 8081`, Menu `EXPOSE 8082`.

---

## 2. Docker Compose topology (Task 2)

`services/local-dev/docker-compose.yml` now runs four services on a
single `quickbite-net` bridge network:

| Container                      | Image                                | Host port | Internal DNS      | Depends on             |
|--------------------------------|--------------------------------------|-----------|-------------------|------------------------|
| `quickbite-restaurant-db`      | `postgres:15`                        | 5442      | `restaurant-db`   | --                     |
| `quickbite-menu-db`            | `postgres:15`                        | 5433      | `menu-db`         | --                     |
| `quickbite-restaurant-service` | `quickbite/restaurant-service:local` | 8081      | `restaurant-service` | `restaurant-db` healthy |
| `quickbite-menu-service`       | `quickbite/menu-service:local`       | 8082      | `menu-service`    | `menu-db` healthy      |

Host port `5442` for the Restaurant DB is the collision-avoidance
override from `.env.local` (a host-level PostgreSQL already owns
5432 on the dev laptop). In-container hostnames
(`restaurant-db:5432`, `menu-db:5432`) are unaffected.

Named volumes: `restaurant_db_data`, `menu_db_data`. No
cross-container volume sharing -- independent persistence per
service.

---

## 3. Spring `docker` profile (Task 3)

`application-docker.properties` in each service overrides only the
DB URL / user / password and points at the container hostname:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://restaurant-db:5432/${RESTAURANT_DB_NAME:restaurant_db}}
```

The compose file activates the profile via
`SPRING_PROFILES_ACTIVE=docker` on each app service. `DB_URL` is
also passed explicitly as a compose env var, so the in-container
Spring config is driven from `.env.local` values, not hardcoded.

`application.properties` is untouched: running the services from
IntelliJ against the DB containers still works (Section 7 of the
runbook).

---

## 4. Healthchecks (Task 4)

| Container                      | Healthcheck                                                   | Interval / retries |
|--------------------------------|---------------------------------------------------------------|--------------------|
| `quickbite-restaurant-db`      | `pg_isready -U restaurant_user -d restaurant_db`              | 5s / 10            |
| `quickbite-menu-db`            | `pg_isready -U menu_user -d menu_db`                          | 5s / 10            |
| `quickbite-restaurant-service` | `curl -fsS http://localhost:8081/actuator/health \| grep UP`  | 10s / 12, start_period 30s |
| `quickbite-menu-service`       | `curl -fsS http://localhost:8082/actuator/health \| grep UP`  | 10s / 12, start_period 30s |

App services `depends_on` their DB with `condition: service_healthy`,
so Compose does not start a service until its database is accepting
connections.

---

## 5. End-to-end verification (Task 5)

Command used (from the repo root):

```bash
docker compose --env-file services/local-dev/.env.local \
               -f services/local-dev/docker-compose.yml \
               up --build -d
```

Cold start: images built in ~3 min (Maven dep download dominates),
containers healthy ~30 s after that. Final `docker ps`:

```
NAME                           STATUS                    PORTS
quickbite-menu-db              Up (healthy)              0.0.0.0:5433->5432/tcp
quickbite-menu-service         Up (healthy)              0.0.0.0:8082->8082/tcp
quickbite-restaurant-db        Up (healthy)              0.0.0.0:5442->5432/tcp
quickbite-restaurant-service   Up (healthy)              0.0.0.0:8081->8081/tcp
```

Smoke tests against the running stack:

| Request                                                                   | Result     |
|---------------------------------------------------------------------------|------------|
| `GET  http://localhost:8081/actuator/health`                              | 200 `UP`   |
| `GET  http://localhost:8082/actuator/health`                              | 200 `UP`   |
| `GET  http://localhost:8081/restaurants`                                  | 200, 6 rows |
| `GET  http://localhost:8081/restaurants?city=Tartu&isOpen=true`           | 200, filtered |
| `GET  http://localhost:8082/restaurants/{id}/menu-items`                  | 200, 4 rows |
| `GET  http://localhost:8082/menu-items/{id}`                              | 200, Margherita |
| `POST http://localhost:8081/restaurants` (no token)                       | 401        |

Persistence check: `docker compose stop && docker compose start`
rebinds the containers to the existing `restaurant_db_data` /
`menu_db_data` volumes; all rows (including one ad-hoc row created
during a Phase 7 test run) are still present on the next `GET
/restaurants`.

### 5.1 Bug surfaced by the real Postgres boot

The H2 test profile (`MODE=PostgreSQL`) tolerated a JPQL pattern
that real PostgreSQL rejected: `LOWER(:param)` where `:param` is
bound as a nullable `String`. The JDBC driver sends the null as
`bytea`, and PostgreSQL fails at parse time with `function
lower(bytea) does not exist` -- even on a `WHERE :param IS NULL
OR LOWER(:param) ...` branch that would short-circuit at runtime,
because Postgres type-checks all branches up front.

Fix (minimal, one-line per repo):

```java
// Before
WHERE (:city IS NULL OR LOWER(r.location.city) = LOWER(:city))
// After
WHERE (cast(:city as string) IS NULL
       OR LOWER(r.location.city) = LOWER(cast(:city as string)))
```

Applied in
`restaurant-service/.../RestaurantRepository.java#search` and
`menu-service/.../MenuItemRepository.java#searchForRestaurant`.
The `Boolean` parameter (`:open` / `:available`) does not need the
same cast because Hibernate binds it with `Types.BOOLEAN`, which
PostgreSQL accepts both for `IS NULL` and `=`.

Tests (H2): `mvn -B test` green on both services after the fix,
same 46-test total as Phase 7.

---

## 6. `.dockerignore` per service (Task 6)

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

Keeps the Docker build context lean (no IDE metadata, no local git
history, no Claude session artefacts). `.mvn/` is safe to exclude
because the Maven wrapper is not used from inside the container
(the base image ships `mvn` directly).

---

## Definition of Done (Phase 8)

- [x] `docker compose up --build` starts everything from scratch.
- [x] Both services reachable and functional (public endpoints
      return real seed data; mutation endpoints are auth-gated per
      Phase 7).
- [x] Databases have persistent volumes (`restaurant_db_data`,
      `menu_db_data`; data survives `docker compose stop && start`).
- [x] No shared database (one Postgres container per service).
- [x] `docker compose down` stops cleanly.
