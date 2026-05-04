# Local-Dev Runbook

Bring up Sten's databases and services. From Phase 8 onward
the full stack (databases + Spring Boot services) runs in Docker
Compose. You can still run the services from IntelliJ against just
the DB containers -- see §7.

## 1. One-time setup

```bash
cd services/local-dev
cp .env.example .env.local          # on first run only
```

Edit `.env.local` only if you want to change passwords or host ports.
Everything else can use the defaults.

> **Host-port conflict?** If your machine already runs PostgreSQL on
> `5432` (common on Windows with the `postgresql-x64-*` service) or
> `5433`, the Docker container will fail to start, or the Spring
> Boot service will try to talk to the wrong database. Override
> `RESTAURANT_DB_HOST_PORT` and/or `MENU_DB_HOST_PORT` in
> `.env.local` -- e.g. `5442` / `5443`. The in-container DB URLs are
> unaffected (containers talk over `quickbite-net`), so no further
> service config is needed when running the Docker stack. For the
> IntelliJ mode in §7 you would also need to set `DB_URL` in the
> Run Configuration to match the new host port.

## 2. Start the full stack (Phase 8 onward)

```bash
cd services/local-dev
docker compose --env-file .env.local up --build -d
```

Expected container names: `quickbite-restaurant-db`,
`quickbite-menu-db`, `quickbite-restaurant-service`,
`quickbite-menu-service`. Check all four are **healthy** (not just
running):

```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

Wait until each row shows `Up ... (healthy)`. On a cold start the
first `--build` takes 3-5 minutes (Maven dependency download).
Subsequent `up -d` reuses the cached layers and is ~30 seconds.

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

## 7. Optional: run the services from IntelliJ against Compose DBs

If you want faster feedback loops (hot reload, debugger) than a
`docker compose up --build`, start only the DB services in Compose
and run the Spring Boot services on the host:

```bash
cd services/local-dev
docker compose --env-file .env.local up -d restaurant-db menu-db
```

Then in two terminals:

```bash
cd services/restaurant-service
mvn spring-boot:run

cd services/menu-service
mvn spring-boot:run
```

Or run them from IntelliJ IDEA (`RestaurantServiceApplication.java`
→ Run, `MenuServiceApplication.java` → Run). The default
`application.properties` points at `localhost:5432` / `localhost:5433`,
so no extra config is needed unless you overrode credentials or host
ports in `.env.local`. If you did, set IntelliJ Run Configuration
environment variables:

```
DB_URL=jdbc:postgresql://localhost:5432/restaurant_db
DB_USER=restaurant_user
DB_PASSWORD=<what you put in .env.local>
```

(and the `:5433` variant for Menu Service).

## 8. Verify health endpoints

Once the stack is up (either via §2 or §7):

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Both should return `{"status":"UP"}` plus Flyway/DB details.

## 9. W1 integration smoke test (Phase 10)

With the full stack healthy, run Sten's W1 contract folder via
Newman to exercise the two hops Alfa-Kilo's Order Service will make
(`GET /restaurants/{id}/availability`, then
`POST /menu-items/validate`). Each request carries the customer token
minted by the collection-level pre-request script, mocking the token
Order would relay per [`0033`](../../dev-docs/decisions/0033-inter-service-token-propagation-lock.md).

```bash
npx --yes newman run services/local-dev/postman/QuickBite.postman_collection.json \
    -e services/local-dev/postman/QuickBite.postman_environment.json \
    --folder "W1 Integration"
```

The 9 requests cover every failure-row Order Service has to handle --
open / closed / unknown restaurant, all-valid / missing / unavailable
batch, plus the envelope 400s (quantity 0, empty list) and a
no-token 401. A green Newman run is the authoritative Phase 10 smoke
evidence.

Shorthand scripts for Alfa-Kilo (if Order Service is unavailable and
we need to demonstrate the chain manually):

```bash
# hop 4: availability (open restaurant)
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8081/restaurants/d0000001-0000-0000-0000-000000000001/availability

# hop 5: batch validate
curl -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -X POST \
     -d '{"items":[{"menuItemId":"e0000012-0000-0000-0000-000000000012","quantity":2},
                    {"menuItemId":"e0000013-0000-0000-0000-000000000013","quantity":1}]}' \
     http://localhost:8082/menu-items/validate
```

Swap `d0000003-...` for a closed-restaurant demo, or
`ffffffff-...` for 404 / `MENU_ITEM_NOT_FOUND` per-line evidence. Seed
fixture table is in
[`services/restaurant-service/src/main/resources/db/migration/V2__seed_demo_data.sql`](../restaurant-service/src/main/resources/db/migration/V2__seed_demo_data.sql)
and the matching Menu file.
