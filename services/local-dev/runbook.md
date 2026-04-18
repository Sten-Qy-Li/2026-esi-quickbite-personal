# Local-Dev Runbook

Bring up the two PostgreSQL databases used by Sierra-Lima's services.

## 1. One-time setup

```bash
cd services/local-dev
cp .env.example .env.local          # on first run only
```

Edit `.env.local` only if you want to change passwords. Everything
else can use the defaults.

## 2. Start the stack

```bash
cd services/local-dev
docker compose --env-file .env.local up -d
```

Expected container names: `quickbite-restaurant-db`,
`quickbite-menu-db`. Check both are **healthy** (not just running):

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Wait until each row shows `Up ... (healthy)`.

## 3. Connect to a database

From any PostgreSQL client (psql, DBeaver, IntelliJ Database tool):

| Service | Host | Port | Database | User | Password |
|---------|------|------|----------|------|----------|
| Restaurant Service | localhost | 5432 | restaurant_db | restaurant_user | see `.env.local` |
| Menu Service       | localhost | 5433 | menu_db       | menu_user       | see `.env.local` |

From `psql` on the host:

```bash
psql -h localhost -p 5432 -U restaurant_user -d restaurant_db
psql -h localhost -p 5433 -U menu_user       -d menu_db
```

## 4. Reset data (throw away everything)

```bash
cd services/local-dev
docker compose down -v     # -v removes the named volumes too
docker compose --env-file .env.local up -d
```

## 5. Inspect logs

```bash
cd services/local-dev
docker compose logs -f restaurant-db
docker compose logs -f menu-db
```

## 6. Stop the stack

```bash
cd services/local-dev
docker compose down         # keeps data in volumes
```

## 7. Run the Spring Boot services

Services do **not** run in Docker Compose during Phases 2-6 -- only
the databases do. Start each service locally with:

```bash
cd services/restaurant-service
mvn spring-boot:run

# In a second terminal:
cd services/menu-service
mvn spring-boot:run
```

Alternatively, run them from IntelliJ IDEA:
`RestaurantServiceApplication.java` → Run, and
`MenuServiceApplication.java` → Run.

Each service reads DB connection info from environment variables. When
running from IntelliJ on the host, the defaults in
`application.properties` already point at the Compose ports
(`localhost:5432` for Restaurant, `localhost:5433` for Menu), so no
extra setup is needed unless you changed credentials in `.env.local`.
If you did, override in IntelliJ's Run Configuration under
"Environment variables":

```
DB_URL=jdbc:postgresql://localhost:5432/restaurant_db
DB_USER=restaurant_user
DB_PASSWORD=<what you put in .env.local>
```

(and the `:5433` variant for Menu Service).

## 8. Verify health endpoints

Once both services are running:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Both should return `{"status":"UP"}` plus Flyway/DB details.
