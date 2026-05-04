# Menu Service and Restaurant Service in a Food Delivery System

## Section 1 of 3: Introduction

### Repository owner

Student name: Sten (Qun-yan Li)

Student group: 7

### About

Individual-level (not team-level) repository for the course project of Enterprise System Integration, a course taught at the University of Tartu in the spring of 2026.

The repository is solely for grading at Project Checkpoint 1 only. For Project Checkpoints 2 and 3, kindly see the team-level repository: [https://github.com/anup28kmr/esi](https://github.com/anup28kmr/esi).

The repository and its contents were produced with the assistance of AI agents.

### Repository layout for Project Checkpoint 1

```
2026-esi-quickbite-personal/
├── frontend/quickbite-frontend/
├── local-dev/
├── menu-service/
├── restaurant-service/
├── .gitignore
└── README.md
```

## Section 2 of 3: Instructions for grader

### Service under examination

The **Menu Service** (`menu-service/`) is the service submitted for Project Checkpoint 1 grading. The Restaurant Service (`restaurant-service/`) is also fully implemented and is included so that the Menu Service's cross-service test (item E below) can exercise a real collaborator interaction; per the "1 service per student" rule in `Project2026.md` §4.1, the second service is not itself in scope for CP1.

### Mapping to Checkpoint 1 deliverables (`Project2026.md` §4.1)

| Item | Where to look |
|---|---|
| **A. Running service** | Menu Service runs on `http://localhost:8082` after the local stack is up (see Quick start below). Health probe: `GET /actuator/health`. Spring Boot entry point: [`menu-service/src/main/java/ee/ut/esi/quickbite/menu/MenuServiceApplication.java`](menu-service/src/main/java/ee/ut/esi/quickbite/menu/MenuServiceApplication.java). |
| **B. API endpoints** | [`menu-service/src/main/java/ee/ut/esi/quickbite/menu/controller/MenuController.java`](menu-service/src/main/java/ee/ut/esi/quickbite/menu/controller/MenuController.java) -- 6 endpoints (create, list-by-restaurant, get-by-id, replace, delete, batch-validate). |
| **C. OpenAPI / Swagger UI** | <http://localhost:8082/swagger-ui.html> (UI). <http://localhost:8082/v3/api-docs> (JSON). Every endpoint carries `@Operation` and `@ApiResponse` annotations. |
| **D. Persistence** | Postgres 15 (`menu_db`, host port 5433). Flyway migrations: [`V1__init.sql`](menu-service/src/main/resources/db/migration/V1__init.sql) and [`V2__seed_demo_data.sql`](menu-service/src/main/resources/db/migration/V2__seed_demo_data.sql). JPA entities under `menu-service/src/main/java/.../domain/`. |
| **E. Test with mocked cross-service dependency, happy + error case** | [`menu-service/src/test/java/ee/ut/esi/quickbite/menu/controller/MenuControllerTest.java`](menu-service/src/test/java/ee/ut/esi/quickbite/menu/controller/MenuControllerTest.java) declares `@MockBean RestaurantOwnershipClient` -- the HTTP collaborator that Menu Service calls into Restaurant Service. The 21 cases include happy paths (`createMenuItem_ownerTokenReturns201`, `validate_succeedsWithCustomerToken`) and error cases (`getMenuItemById_returns404WhenMissing`, `createMenuItem_adminUnknownRestaurantReturns404`). The class uses `@SpringBootTest + @AutoConfigureMockMvc + @MockBean` rather than `@WebMvcTest`, which CP1 §4.1.E permits ("or any other testing framework you prefer"). |
| **F. API demonstration** | Postman pack: [`local-dev/postman/QuickBite.postman_collection.json`](local-dev/postman/QuickBite.postman_collection.json) and the matching environment file. Run via Newman (command below), or use Swagger UI directly. |

### Quick start

Prerequisites: Docker Desktop, Java 17+ (for `mvn test`), and a shell that can run either `.sh` or `.ps1` scripts.

```bash
# Bring up the Sten stack (Postgres x2 + both services + frontend)
cd local-dev
docker compose --profile dev-gateway up -d --build
docker ps                                     # expect 6 healthy containers

# Sanity-check the Menu Service
curl -fsS http://localhost:8082/actuator/health
```

### Reproduction commands

```bash
# Backend unit + integration tests
cd menu-service        && mvn clean test          # 47/47
cd ../restaurant-service && mvn clean test        # 33/33

# Postman / Newman (after stack is up)
cd ../local-dev
npx newman run postman/QuickBite.postman_collection.json \
              -e postman/QuickBite.postman_environment.json

# Smoke probes (after stack is up)
bash smoke.sh                                     # POSIX shell
./smoke.ps1                                       # PowerShell equivalent
```

### Authentication during demo

The local stack uses dev HS256 JWTs minted by `JwtDevMint`; smoke scripts and Postman pre-request scripts mint them automatically. Three roles are exercised:

- `dev-customer` -- Customer (read-only on public endpoints, batch-validate)
- `dev-owner` -- RestaurantOwner (full CRUD on items they own)
- `dev-admin` -- Admin (bypasses ownership checks)

Token-minting commands and the per-endpoint auth matrix are documented in [`local-dev/runbook.md`](local-dev/runbook.md) §4 and §9.


## Section 3 of 3: Additional documentation

### Per-component READMEs

| Component | README |
|---|---|
| Menu Service (graded for CP1) | [`menu-service/README.md`](menu-service/README.md) |
| Restaurant Service | [`restaurant-service/README.md`](restaurant-service/README.md) |
| Frontend (Vue 3) | [`frontend/quickbite-frontend/README.md`](frontend/quickbite-frontend/README.md) |
| Local-dev stack (Docker, Postman, smokes) | [`local-dev/README.md`](local-dev/README.md) |

The local-dev runbook ([`local-dev/runbook.md`](local-dev/runbook.md)) covers operational steps in detail: stack up/down, running services from IntelliJ against Compose-managed databases, log inspection, healthcheck endpoints, the W1 cross-service smoke probe, and reset-and-replay procedures.

### Team-level project

This individual repository contains only Sten's owned slice (the Menu Service, the Restaurant Service, his share of the frontend, and the local-dev stack). The full QuickBite system -- 8 microservices across 5 students -- is integrated in the team-level repository: [`https://github.com/anup28kmr/esi`](https://github.com/anup28kmr/esi). For Project Checkpoints 2 and 3, the team-level repository supersedes this one.

### Notes on internal links

Some links in the per-component READMEs reference design documents (decisions log, audits, gap analyses, roadmaps) kept outside the CP1 grading view. They are not required reading for Checkpoint 1 grading; the per-component READMEs above and the source code are self-contained for the rubric in Section 2.

