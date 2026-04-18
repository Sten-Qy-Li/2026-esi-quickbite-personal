# Phase 2-6 Verification Guide (IntelliJ IDEA edition)

**Audience.** Sierra-Lima, running the two services (Restaurant + Menu)
on a Windows laptop with IntelliJ IDEA, for the first time. No prior
Spring Boot experience assumed.

**What this covers.** Everything built in Phases 2-6 of the master plan:
project scaffolding, database containers, CRUD endpoints, validation,
OpenAPI/Swagger, and the Menu Service batch-validation endpoint.

**Time estimate.** 20-40 minutes of active work the first time,
including tool installs. Much faster on subsequent runs.

---

## 0. Mini-glossary (skim this once)

| Term | Plain-English meaning |
|---|---|
| **JDK 17** | The Java Development Kit. Gives you `java` and `javac`. We use version 17 because Spring Boot 3.3 requires it. |
| **Maven** | Java's package manager + build tool. Reads `pom.xml`, downloads libraries, compiles code, runs tests. |
| **pom.xml** | The "project object model" file. Declares dependencies, Java version, and build steps. One per service. |
| **Spring Boot** | The web framework we use. It wires up the web server, database connection, validation, security, etc. automatically. |
| **Spring Data JPA** | The bit of Spring Boot that turns Java classes into database tables and lets us call `save()`, `findById()`, etc. without writing SQL. |
| **Flyway** | A database-migration tool. SQL files in `db/migration/V1__*.sql`, `V2__*.sql`, etc. run in order on startup. Keeps all machines on the same schema. |
| **@Entity** | A Java annotation that marks a class as a row in a database table. |
| **Bean Validation** | `@NotBlank`, `@NotNull`, `@DecimalMin`, `@Pattern`. If input violates them the framework returns HTTP 400 automatically. |
| **@RestController** | Marks a class as a holder of HTTP endpoints. |
| **OpenAPI / Swagger UI** | An auto-generated web page that documents every HTTP endpoint and lets you try them out from the browser. |
| **Actuator** | Spring Boot's built-in health-check endpoints (`/actuator/health`). |
| **Docker Compose** | Runs containers (our two PostgreSQL databases) from a YAML file. |
| **Postman** | A desktop app for hand-crafting HTTP requests. We ship a Postman collection so you don't have to build URLs by hand. |
| **JDBC URL** | How Java connects to PostgreSQL, e.g. `jdbc:postgresql://localhost:5432/restaurant_db`. |
| **@Embeddable** | A Java class whose fields become columns on an `@Entity`. Used for value objects like `Location` and `Price`. |

---

## 1. Prerequisites (install once)

Open PowerShell or Git Bash and check each:

```bash
java -version        # need 17.x (Eclipse Temurin recommended)
mvn -version         # need 3.9+
docker --version     # need Docker Desktop (Docker Compose v2 bundled)
```

If any are missing:

- **JDK 17:** Install Eclipse Temurin 17 from <https://adoptium.net/> (pick
  the `.msi` installer, tick "Set JAVA_HOME"). Restart your terminal after
  install.
- **Maven:** Download from <https://maven.apache.org/download.cgi>, unzip,
  add the `bin/` folder to `PATH`. Or let IntelliJ use its bundled Maven
  (no system install needed).
- **Docker Desktop for Windows:** <https://www.docker.com/products/docker-desktop/>
  -- start the app once before continuing (it takes ~1 minute to boot).
- **IntelliJ IDEA Community or Ultimate:** <https://www.jetbrains.com/idea/download/>
- **Postman desktop app:** <https://www.postman.com/downloads/>

---

## 2. Open the project in IntelliJ

The repository has two Maven modules -- `services/restaurant-service`
and `services/menu-service`. They share no `pom.xml` parent, so we
open them as two separate IntelliJ projects (simplest) or one project
with two modules (slightly more setup).

**Simplest path: open each as its own project.**

1. Launch IntelliJ IDEA. From the Welcome screen click **Open**.
2. Navigate to
   `C:\MSc-Computer-Science\Semester-2\esi\2026-esi-quickbite-personal\services\restaurant-service`
   and click **OK**. Choose **Trust Project** when prompted.
3. IntelliJ sees `pom.xml` and starts downloading dependencies. Watch
   the bottom status bar; this takes 2-5 minutes on the first run.
   When you see `Build: indexing... finished`, you're ready.
4. When indexing completes, set the JDK if IntelliJ asks: **File ->
   Project Structure -> Project**, set "SDK" to your Java 17 install,
   "Language level" to 17. Click **Apply** -> **OK**.
5. Repeat steps 1-4 in a second IntelliJ window for
   `services\menu-service`.

> **If dependencies won't download:** right-click `pom.xml` -> **Maven
> -> Reload Project**. If you're behind a corporate proxy, tell IntelliJ
> about it under **Settings -> Appearance & Behavior -> System Settings
> -> HTTP Proxy**.

---

## 3. Start the PostgreSQL databases

The services need PostgreSQL running **before** you start them. Docker
Compose takes care of that.

1. Open Git Bash (or PowerShell) in the repo root.
2. First-time setup only:
   ```bash
   cd services/local-dev
   cp .env.example .env.local
   ```
   `.env.local` is git-ignored. It holds DB passwords for your machine.
3. Start the containers:
   ```bash
   docker compose --env-file .env.local up -d
   ```
   `-d` runs them in the background.
4. Verify both containers are healthy:
   ```bash
   docker ps --format "table {{.Names}}\t{{.Status}}"
   ```
   You should see:
   ```
   NAMES                      STATUS
   quickbite-restaurant-db    Up 12 seconds (healthy)
   quickbite-menu-db          Up 12 seconds (healthy)
   ```
   Wait until **both** say `(healthy)` -- `up` alone is not enough.

If you need to reset the data later:
```bash
docker compose down -v
docker compose --env-file .env.local up -d
```

---

## 4. Run the Restaurant Service from IntelliJ

1. In the **restaurant-service** IntelliJ window, open
   `src/main/java/ee/ut/esi/quickbite/restaurant/RestaurantServiceApplication.java`.
2. Click the green ▶ gutter arrow next to the `main` method. Pick
   **Run 'RestaurantServiceApplication'**.
3. The Run panel opens at the bottom. Watch for:
   ```
   Started RestaurantServiceApplication in 4.521 seconds
   Tomcat started on port 8081
   ```
4. You'll also see Flyway apply the schema on first startup:
   ```
   Successfully applied 1 migration to schema "public"
   Migrating schema "public" to version "1 - init"
   ```

> **Terminology.** Tomcat is the embedded web server. Spring Boot
> bundles it so you don't install it separately. "Port 8081" means the
> service listens at `http://localhost:8081`.

---

## 5. Run the Menu Service from IntelliJ (separate window)

1. In the **menu-service** IntelliJ window, open
   `src/main/java/ee/ut/esi/quickbite/menu/MenuServiceApplication.java`.
2. Click ▶ next to `main`. Wait for
   `Tomcat started on port 8082` and the Flyway migration line.

You now have two Spring Boot processes plus two PostgreSQL containers
running concurrently.

---

## 6. Smoke-test: are the services alive?

In Git Bash:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Expected:
```json
{"status":"UP","components":{"db":{"status":"UP", ...}, "diskSpace":{...}, "ping":{"status":"UP"}}}
```

If `"status":"DOWN"` or you get "Connection refused", see the
**Troubleshooting** section at the end.

---

## 7. Swagger UI: exploring the API from a browser

Open these in your browser:

- Restaurant Service -- <http://localhost:8081/swagger-ui.html>
- Menu Service       -- <http://localhost:8082/swagger-ui.html>

You should see a documentation page listing every endpoint, its
parameters, request body schema, and response codes. Expand any
endpoint -> **Try it out** -> fill the body -> **Execute** to call it
from the browser.

**Phase 4/6 DoD check (one box each service):**
- [ ] Restaurant Swagger UI lists: POST /restaurants, GET /restaurants,
      GET /restaurants/{id}, PUT /restaurants/{id},
      PATCH /restaurants/{id}/status, GET /restaurants/{id}/availability
      (6 endpoints).
- [ ] Menu Swagger UI lists: POST /restaurants/{rid}/menu-items,
      GET /restaurants/{rid}/menu-items, GET /menu-items/{id},
      PUT /menu-items/{id}, DELETE /menu-items/{id},
      POST /menu-items/validate (6 endpoints).

---

## 8. Import the Postman collection

1. Open Postman.
2. **File -> Import** -> select
   `services/local-dev/postman/QuickBite.postman_collection.json`.
3. **File -> Import** again -> select
   `services/local-dev/postman/QuickBite.postman_environment.json`.
4. Top-right environment dropdown: pick **QuickBite Local**.

You should now see a **QuickBite** collection with folders:
`Auth (Login)`, `Restaurant CRUD`, `Menu CRUD`, plus three
empty folders (`W1 Integration`, `Async Evidence`, `Negative Auth`)
that fill in later phases.

---

## 9. End-to-end verification against the DoD

Work through the requests below in order. Each block states the
expected status code and a snippet of the expected body.

### 9.1 Restaurant Service

#### Create a restaurant (POST /restaurants)

Collection: **Restaurant CRUD -> POST /restaurants**. Send.

- **Expected:** `201 Created`, body contains a generated `restaurantId`
  (UUID) and `"isOpen": false`.
- **Next step:** copy `restaurantId` from the response and paste it
  into the environment variable `restaurantId` (click the eye icon in
  the top-right, then **Edit**).

#### Retrieve it (GET /restaurants/{id})

- **Expected:** `200 OK`, same fields echoed back.

#### Try an invalid id

Manually change the last digit of `restaurantId` to a `z`. Re-send.

- **Expected:** `400 Bad Request` with the project error envelope:
  ```json
  {
    "timestamp": "2026-...",
    "status": 400,
    "error": "Bad Request",
    "message": "Invalid value for parameter 'id'",
    "path": "/restaurants/..."
  }
  ```
  **Phase 4 DoD check:** consistent error envelope.

Restore the id.

#### List with a city filter

**Restaurant CRUD -> GET /restaurants?city=Tartu&isOpen=true**. Send.

- **Expected:** `200 OK`, empty array (since `isOpen=false` by default
  on create). Re-send with `isOpen=false` to get the row back.

#### Update it (PUT /restaurants/{id})

Change `name` in the body to `"Pizza Antonio (updated)"`. Send.

- **Expected:** `200 OK`, body shows the new name and an **updated**
  `updatedAt` timestamp while `createdAt` stays the same. **Phase 4
  DoD check:** auditing works.

#### Toggle open (PATCH /restaurants/{id}/status)

Body: `{ "isOpen": true }`. Send.

- **Expected:** `200 OK`, `"isOpen": true`.

#### Availability check (GET /restaurants/{id}/availability)

- **Expected:** `200 OK`,
  ```json
  {"restaurantId":"...","isOpen":true,"operatingHours":"11:00-22:00"}
  ```

#### Validation failure

Pick **POST /restaurants**, empty out the `name` in the body, send.

- **Expected:** `400 Bad Request`, `validationErrors[]` lists
  `name` with the reason "name is required". **Phase 4 DoD check:**
  Bean Validation is wired.

Reset the body.

### 9.2 Menu Service

#### Create a menu item

**Menu CRUD -> POST /restaurants/{rid}/menu-items**. Send.

- **Expected:** `201 Created`, generated `menuItemId`, `isAvailable: true`.
- Copy `menuItemId` into the environment variable `menuItemId`.

#### Retrieve it (GET /menu-items/{id})

- **Expected:** `200 OK`, body echoes fields.

#### List for the restaurant

**Menu CRUD -> GET /restaurants/{rid}/menu-items?category=Main&available=true**.

- **Expected:** `200 OK`, array containing your new item.

#### Invalid price

Set `priceAmount` to `"-5"` in **POST /restaurants/{rid}/menu-items**.
Send.

- **Expected:** `400 Bad Request`, `validationErrors[]` lists
  `priceAmount` with the reason "priceAmount must be greater than 0".

Reset the body.

#### Update (PUT /menu-items/{id})

Change `priceAmount` to `"9.00"`. Send.

- **Expected:** `200 OK`, new price, new `updatedAt`.

#### Batch validate (POST /menu-items/validate)

Body (use the collection's default, which references
`{{menuItemId}}`):

```json
{
  "items": [
    { "menuItemId": "{{menuItemId}}", "quantity": 2 }
  ]
}
```

- **Expected:** `200 OK`,
  ```json
  {
    "allValid": true,
    "results": [
      {
        "menuItemId": "...",
        "quantity": 2,
        "exists": true,
        "available": true,
        "unitPriceAmount": 9.00,
        "unitPriceCurrency": "EUR",
        "lineTotalAmount": 18.00
      }
    ]
  }
  ```
  **Phase 6 DoD check:** batch validation returns prices + availability
  per item.

Now add a second entry with a nonexistent id:

```json
{
  "items": [
    { "menuItemId": "{{menuItemId}}", "quantity": 2 },
    { "menuItemId": "11111111-1111-1111-1111-111111111111", "quantity": 1 }
  ]
}
```

- **Expected:** `200 OK`, `allValid: false`, second entry has
  `exists: false`, `reason: "not_found"`.

#### Delete (DELETE /menu-items/{id})

- **Expected:** `204 No Content`, empty body. Follow up with GET
  /menu-items/{id} and expect `404 Not Found`.

---

## 10. Persistence check (restart test)

1. Stop **both** Spring Boot processes (red square button in IntelliJ's
   Run panel).
2. Start them again.
3. In Postman, GET /restaurants/{id} and GET /menu-items/{id} for an id
   you created earlier.

The service should return the data unchanged. This confirms:
- The schema Flyway created survives restart.
- JPA auditing wrote `createdAt`/`updatedAt` to disk.
- **Phase 3/5 DoD check:** data persists across restarts.

---

## 11. Definition of Done consolidated checklist

Tick these once all steps above pass.

**Phase 2 -- Scaffolding.**
- [ ] Both Spring Boot apps start without errors
- [ ] Both PostgreSQL containers report `healthy`
- [ ] Both `/actuator/health` return `{"status":"UP"}`
- [ ] Postman collection imports and the environment loads

**Phase 3 -- Restaurant foundation.**
- [ ] `restaurant` table auto-created (check via IntelliJ's Database
      tool window, or `psql -h localhost -p 5432 -U restaurant_user -d restaurant_db -c "\dt"`)
- [ ] All 6 Restaurant endpoints reachable via Postman
- [ ] Data survives a service restart

**Phase 4 -- Restaurant polish.**
- [ ] Swagger UI at <http://localhost:8081/swagger-ui.html> renders all
      endpoints with request/response schemas
- [ ] Invalid bodies produce `400` with the project error envelope
- [ ] CORS headers present (inspect in browser DevTools on an OPTIONS
      preflight, or `curl -I -X OPTIONS http://localhost:8081/restaurants
      -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: GET"`
      and expect `Access-Control-Allow-Origin: http://localhost:5173`)

**Phase 5 -- Menu foundation.**
- [ ] `menu_item` table auto-created (`psql -h localhost -p 5433 -U menu_user -d menu_db -c "\dt"`)
- [ ] All 6 Menu endpoints reachable via Postman
- [ ] Batch validation returns per-line existence, availability, prices
- [ ] Data survives a service restart

**Phase 6 -- Menu polish.**
- [ ] Swagger UI at <http://localhost:8082/swagger-ui.html> renders all
      endpoints
- [ ] `POST /menu-items/validate` returns the locked-down JSON shape
      (see §9.2 above)
- [ ] Invalid bodies produce `400` with the project error envelope

---

## 12. What is **not** expected yet

These are explicitly deferred and will feel missing. Don't worry.

- **Authentication / JWT.** Every endpoint is open. `Authorization`
  headers are accepted but not checked. Wiring lands in **Phase 7**.
- **Seed data.** Databases start empty. You create everything via
  Postman. Seed restaurants and menu items land in **Phase 7** (we
  decided Flyway `V2__seed_demo_data.sql`).
- **Integration with other teams.** Order, Payment, Delivery Services
  are not involved in Phase 2-6.
- **Frontend.** Phase 11+ concern.

---

## 13. Troubleshooting cheat sheet

| Symptom | Likely cause | Fix |
|---|---|---|
| `Port 8081 already in use` | A previous `RestaurantServiceApplication` is still running | In IntelliJ, look for another Run tab with a green square and stop it. On Windows: `netstat -ano | findstr :8081` -> `taskkill /PID <pid> /F`. |
| `FATAL: password authentication failed for user "restaurant_user"` | `.env.local` password changed but you didn't restart the DB container | `cd services/local-dev && docker compose down -v && docker compose --env-file .env.local up -d` |
| `Connection refused` on localhost:5432 | Docker container isn't running | `docker ps` -- if empty, re-run step 3 |
| Flyway error `Schema "public" contains a failed migration` | A previous run crashed mid-migration | `docker compose down -v` (wipes data), then `up -d`. For production this would require `flyway repair`. |
| Swagger UI returns 404 | Service started but `springdoc` didn't load; check the console for a red stack trace | Usually means `pom.xml` change didn't trigger a Maven reload -- right-click `pom.xml` -> **Maven -> Reload Project** |
| IntelliJ doesn't recognise `@Entity` etc. | Maven indexing didn't finish | Open a file, watch the bottom bar; if it says "Indexing..." wait. If stuck: **File -> Invalidate Caches -> Invalidate and Restart**. |
| `java.lang.ClassNotFoundException: ...` at runtime | Dependency wasn't pulled; classpath is stale | `mvn clean package` from the service folder; then restart the app |
| `500 Internal Server Error` on a request | Unhandled exception; check the service console | Stack trace points to the real cause; most common during Phase 2-6 is a malformed UUID in the URL path |

---

## 14. Stop everything

```bash
# In each IntelliJ Run panel: press the red square
# In Git Bash:
cd services/local-dev
docker compose down           # keep data
# or
docker compose down -v        # wipe data
```

Next time, **start** is three steps:
1. `docker compose --env-file .env.local up -d`
2. Run `RestaurantServiceApplication` in IntelliJ
3. Run `MenuServiceApplication` in IntelliJ

That's it.
