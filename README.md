# SecreGest

A task-management platform for a secretarial agency. A single administrator
(the agency) manages the tasks requested by its clients: from the initial
request through budgeting, payment, delivery, and final rating, with
messaging, document attachments, and calendar tracking all integrated into
the same platform.

Final Degree Project (TFG) — microservices architecture built with
Spring Boot 4.1 / Java 21 on the backend, and Angular 20 on the frontend.

---

## Table of contents

- [Architecture](#architecture)
- [Task lifecycle](#task-lifecycle)
- [Prerequisites](#prerequisites)
- [Getting started with Docker (recommended)](#getting-started-with-docker-recommended)
- [Getting started locally, without Docker](#getting-started-locally-without-docker)
- [Environment variables](#environment-variables)
- [Folder structure](#folder-structure)
- [Testing](#testing)
- [Development blocks](#development-blocks)
- [Additional documentation](#additional-documentation)
- [Future work](#future-work)

---

## Architecture

One microservice per domain, each with its own PostgreSQL database,
communicating with each other via RestTemplate (with an interceptor that
forwards the original user's JWT token) and secured with a JWT signed with
a shared secret key.

| Service | Port | Responsibility | Database |
|---|---|---|---|
| `users` | 8081 | Registration, login, JWT, roles (`ROLE_USER`/`ROLE_ADMIN`) | `usersSecGest` |
| `categories` | 8083 | Task categories (name, icon, image, indicative price) | `categoriesSecGest` |
| `tasks` | 8082 | The core: full task lifecycle, budgeting, payment (simulated and real TPV via Redsys), delivery, rating, calendar, metrics | `tasksSecGest` |
| `notifications` | 8084 | Email sending (Gmail SMTP + Thymeleaf) | — (no database) |
| `documents` | 8085 | Task attachments (metadata in the DB, files on disk) | `documentsSecGest` |
| `messages` | 8086 | Client-admin chat per task, and admin-only private notes | `messagesSecGest` |
| `frontend` | 4200 | Angular 20, standalone components, Bootstrap + a custom design system | — |

Each backend microservice generally follows this internal structure:

```
src/main/java/com/igarciamen/<service>/
  config/       # Security (JWT), CORS, OpenAPI/Swagger, RestTemplate
  controller/   # REST endpoints
  service/      # Business logic
  repository/   # Spring Data JPA
  model/        # JPA entities
  payloads/     # Request and response DTOs
    request/
    response/
```
(`messages` uses `dto/` instead of `payloads/`, plus architecture tests
with ArchUnit that automatically verify this same structure.)

### Why microservices instead of a monolith

Each domain (users, categories, tasks, notifications, documents,
messaging) has a clearly distinct change cycle and responsibility, and
they communicate through explicit HTTP contracts rather than sharing a
database — this allows each piece to be deployed, scaled, or replaced
independently (for example, `notifications` could be swapped for a
different email provider without touching the rest of the system).

---

## Task lifecycle

```
PENDING_REVIEW → BUDGETED → ACCEPTED → PAID → DELIVERED → COMPLETED
                       ↑______________|
                  (client rejection)
```

1. The **client** requests a task (`POST /api/tasks`).
2. The **admin** sets a price (`PUT /api/tasks/{id}/budget`) → email to the client.
3. The **client** accepts (`PUT /api/tasks/{id}/accept`) or rejects (`PUT /api/tasks/{id}/reject`, back to pending, with the rejected price kept in history).
4. The **client** pays: simulated (`PUT /api/tasks/{id}/pay`) or via a real TPV through Redsys/BBVA (`POST /api/tasks/{id}/pay/redsys/start`, sandbox environment).
5. The **admin** marks the task as delivered (`PUT /api/tasks/{id}/deliver`) → email to the client.
6. The **client** confirms receipt, with an optional 1-to-5 rating (`PUT /api/tasks/{id}/complete`).

At any point in the process: chat between client and admin, document
attachments, admin-only private notes (never visible to the client), and
calendar tracking based on each task's due date.

---

## Prerequisites

- **Docker** and **Docker Compose** (recommended option), or:
- **Java 21** and **Maven** (to run each microservice individually)
- **Node.js** and **Angular CLI** (for the frontend)
- **PostgreSQL 16** locally, if not using Docker

---

## Getting started with Docker (recommended)

1. Clone or download the full project, with all 8 folders (`users`,
   `categories`, `tasks`, `notifications`, `documents`, `messages`,
   `proyecto frontend`) alongside `docker-compose.yml` and
   `init-databases.sql`.

2. Create a `.env` file in the project root (next to
   `docker-compose.yml`) with this content:

   ```env
   JWT_SECRET=0123456789ABCDEFGHIJKLMNOPQRSTUV
   DB_PASSWORD=apertura15
   GMAIL_APP_PASSWORD=vssu vlen tqqq jqjd
   REDSYS_SECRET_KEY=sq7HjrUOBfKmC576ILgskD5srU870gJ7
   ```
   > **Important:** save it as UTF-8. If you create it with Windows
   > Notepad and Docker fails to read the variables (they show up as
   > empty strings in `docker compose config`), regenerate it from
   > PowerShell instead:
   > ```powershell
   > @"
   > JWT_SECRET=0123456789ABCDEFGHIJKLMNOPQRSTUV
   > DB_PASSWORD=apertura15
   > GMAIL_APP_PASSWORD=vssu vlen tqqq jqjd
   > REDSYS_SECRET_KEY=sq7HjrUOBfKmC576ILgskD5srU870gJ7
   > "@ | Out-File -FilePath ".env" -Encoding utf8 -NoNewline
   > ```

3. Check that the variables resolve correctly before starting anything:
   ```bash
   docker compose config
   ```
   No variable should show up empty (`""`), and there should be no
   `"is not set"` warnings.

4. Bring everything up:
   ```bash
   docker compose up --build
   ```
   The first run takes a few minutes (it builds 6 Java images from
   scratch). `init-databases.sql` only runs the first time the database
   volume is created, and creates the additional databases
   (`tasksSecGest`, `categoriesSecGest`, `documentsSecGest`,
   `messagesSecGest`) on top of the `usersSecGest` that Postgres creates
   by default.

5. Open `http://localhost:4200`.

To fully reset everything (for example, after changing a database
password in `.env`):
```bash
docker compose down -v
docker compose up --build
```
The `-v` flag also removes the volumes — required if you changed
`DB_PASSWORD`, since Postgres only sets the password the first time the
volume is created.

---

## Getting started locally, without Docker

1. Create these 5 databases in your local Postgres: `usersSecGest`,
   `categoriesSecGest`, `tasksSecGest`, `documentsSecGest`,
   `messagesSecGest` (`notifications` doesn't need a database).

2. In each microservice, check `src/main/resources/application.properties`
   and adjust your local Postgres username/password if they differ from
   the defaults.

3. Start the backends **in this order** (some depend on others to
   validate tokens or look up data):
   ```
   users (8081) → categories (8083) → notifications (8084) → tasks (8082) → documents (8085) → messages (8086)
   ```
   From each folder: `mvn spring-boot:run`, or run the
   `*Application.java` class from your IDE.

4. Start the frontend:
   ```bash
   cd "proyecto frontend"
   npm install
   ng serve
   ```
   Open `http://localhost:4200`.

5. Each backend exposes interactive API documentation at
   `http://localhost:<port>/swagger-ui/index.html`.

---

## Environment variables

| Variable | Used by | Description |
|---|---|---|
| `JWT_SECRET` | All 6 backend microservices | Shared key used to sign/validate JWTs |
| `DB_PASSWORD` | The 5 microservices with a database, and `db` | PostgreSQL password |
| `GMAIL_APP_PASSWORD` | `notifications` | Gmail app password (SMTP) |
| `REDSYS_SECRET_KEY` | `tasks` | Redsys sandbox merchant secret key (TPV) |

Locally, each `application.properties` has a default value
(`${JWT_SECRET:0123456789ABCDEFGHIJKLMNOPQRSTUV}`), so defining these
variables **is not required** for development — the `.env` file is only
needed when using Docker Compose, or if you want to override a value.

`.env` is **never** committed to the repository (it's listed in
`.gitignore`).

---

## Folder structure

```
PROYECTO SERVICIOS SECRETARIADO/
├── docker-compose.yml
├── init-databases.sql
├── .env                    # real credentials (not versioned)
├── .gitignore
├── users/                  # microservice: authentication and users
├── categories/              # microservice: task categories
├── tasks/                  # microservice: core business logic
├── notifications/           # microservice: email sending
├── documents/                # microservice: task attachments
├── messages/                # microservice: chat and internal notes
└── proyecto frontend/       # Angular 20
```

---

## Testing

Each backend microservice includes:
- **Unit tests** for the service layer (JUnit 5 + Mockito).
- **Integration tests** for the controllers (`@SpringBootTest` +
  `MockMvc`, with an in-memory H2 database and the RestTemplate clients
  to other microservices replaced with `@MockitoBean`).
- `messages` additionally includes **architecture tests** (ArchUnit),
  which automatically verify that the package structure is respected
  (controllers living in `controller` and ending in `Controller`, etc.).

```bash
# In any backend microservice:
mvn test
```

The frontend includes Jasmine/Karma tests for services and components:
```bash
cd "proyecto frontend"
ng test
```

---

## Development blocks

The project was built incrementally, block by block, each one closed
with its own tests and a documented guide in Word:

1. Authentication, roles, light/dark theme
2. Task categories (CRUD)
3. Task creation and listing
4. Enriched categories (icon, color, price) and additional task fields
5. Email notifications + agency budgeting
6. Client acceptance, rejection, and resubmission
7. Payment (simulated + real BBVA/Redsys TPV, sandbox environment)
8. Document attachments
9. Task delivery and closure, with rating
10. Google Calendar-style calendar view
11. Internal messaging (chat) and admin private notes
12. Admin dashboard and metrics
13. Advanced security (configurable CORS, rate limiting, credentials out of the codebase) and deployment

In addition, a full visual redesign of the frontend (the "Dossier" design
system: each task as a file-folder-style accordion, with its own palette
and identity) applied across the board after Block 11.

---

## Additional documentation

Each block has its own Word document with the step-by-step guide, the
full code, and the real issues resolved during development (useful as a
process record for the TFG report). The documents most relevant to
understanding the final state of the system:

- Guide for each block (`Bloque_1_SecreGest.docx` … `Bloque_13_SecreGest.docx`)
- `Rediseno_Visual_SecreGest.docx` — the frontend design system
- `TPV_Redsys_Camino_a_Produccion.docx` — what it would take to make the TPV payment fully real

---

## Future work

Reasonable lines of continuation, not implemented as they fall outside
the scope of a TFG:

- **Real production TPV**: contract with BBVA, strong customer
  authentication (SCA/3D Secure, mandatory under PSD2 regulation),
  notification idempotency, refunds. See
  `TPV_Redsys_Camino_a_Produccion.docx`.
- **Document storage in a dedicated service** (MinIO/S3) instead of the
  local filesystem, for deployments running more than one instance of
  the `documents` microservice.
- **Distributed rate limiting** (Redis) if the system were ever deployed
  with multiple `users` instances in parallel.
- **A secrets manager** (Vault, AWS Secrets Manager) instead of plain
  environment variables, should the project scale to a production
  environment with multiple people responsible for deployment.
- **Push notifications / WebSockets** for the chat, instead of the
  current *polling* approach (refreshing every 5 seconds), for a truly
  real-time experience.

---

## Tech stack

**Backend:** Spring Boot 4.1, Java 21, Spring Security (OAuth2 Resource
Server + JWT), Spring Data JPA, PostgreSQL 16, H2 (tests), Maven,
springdoc-openapi (Swagger), Thymeleaf (email templates), ArchUnit.

**Frontend:** Angular 20 (standalone components), TypeScript, RxJS,
Bootstrap 5, Angular Reactive Forms.

**Infrastructure:** Docker, Docker Compose.

**External integrations:** Gmail SMTP, Redsys (TPV, sandbox environment).
