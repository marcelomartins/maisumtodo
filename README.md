# Mais Um Todo

Mais Um Todo is a simple project-based todo application. It uses a Quarkus backend at the repository root and a Vue 3 frontend in `ui-vue`. The frontend build outputs static files into the Quarkus resources directory so the backend can serve the application in production.

## Features

- Email and password registration.
- Login/logout with an HTTP-only authentication cookie.
- Session restore through `/api/auth/me`.
- Project creation, listing, renaming, and selection.
- Task creation, inline title editing, deletion, and completion toggling.
- Two task views for the selected project:
  - List view for quick execution.
  - Kanban view with `TODO`, `DOING`, and `DONE` columns.
- Drag-and-drop task movement in the kanban view.
- MySQL persistence managed by Flyway migrations.
- Backend tests for the main authenticated project/task flow.

## Tech Stack

### Backend

- Java 25
- Quarkus 3.36.1
- Gradle wrapper
- Quarkus REST with Jackson
- Hibernate ORM with Panache
- Flyway
- MySQL JDBC driver
- BCrypt for password hashing
- H2 only for automated tests

### Frontend

- Vue 3
- Vite
- TypeScript
- Vue Router with hash history
- Nuxt UI v4
- Tailwind CSS v4
- FormKit Drag and Drop
- pnpm

## Repository Layout

```text
.
|-- build.gradle
|-- gradle.properties
|-- settings.gradle
|-- src
|   |-- main
|   |   |-- java/net/marcelomartins/maisumtodo
|   |   |   |-- api
|   |   |   |   |-- AuthResource.java
|   |   |   |   `-- TodoResource.java
|   |   |   |-- domain
|   |   |   |   |-- EntityBase.java
|   |   |   |   |-- SystemLogin.java
|   |   |   |   |-- TaskStatus.java
|   |   |   |   |-- TodoProject.java
|   |   |   |   `-- TodoTask.java
|   |   |   |-- infra
|   |   |   |   |-- AuthTokenService.java
|   |   |   |   `-- UserRequest.java
|   |   |   `-- GreetingResource.java
|   |   `-- resources
|   |       |-- application.properties
|   |       `-- db/migration/V1__create_core_tables.sql
|   |-- test
|   `-- native-test
|-- ui-vue
|   |-- package.json
|   |-- vite.config.ts
|   `-- src
|       |-- core
|       |-- modules/auth
|       `-- modules/todos
`-- documents
    `-- plano-backend-tarefas.md
```

## How It Works

The backend exposes a JSON REST API under `/api`. Authentication is cookie-based: after a successful registration or login, the backend returns an `auth_token` cookie with `HttpOnly`, `SameSite=Strict`, path `/`, and a configurable `Secure` flag.

The token is a signed JWT-like value created by `AuthTokenService` using HMAC-SHA256. It contains the user UUID, email, issuer, issued-at time, expiration time, and the user's current auth token version. Logout increments the stored token version, which invalidates existing cookies for that user.

The frontend calls the API with `credentials: 'include'`, so cookies are sent automatically. During local frontend development, Vite proxies `/api` to `http://localhost:8080`. For production-style builds, Vite writes the frontend output to:

```text
src/main/resources/META-INF/resources
```

Quarkus serves that directory as static content.

## Prerequisites

- JDK 25.
- MySQL running locally or accessible through a JDBC URL.
- Node.js compatible with the frontend package requirement:
  - `^20.19.0` or `>=22.12.0`
- pnpm.

The Gradle wrapper is committed, so a separate Gradle installation is not required.

## Configuration

The main backend configuration is in `src/main/resources/application.properties`.

| Property | Environment variable | Default |
| --- | --- | --- |
| `quarkus.datasource.jdbc.url` | `DB_JDBC_URL` | `jdbc:mysql://127.0.0.1:3306/maisumtodo?useUnicode=true&characterEncoding=UTF-8&character_set_server=utf8mb4` |
| `quarkus.datasource.username` | `DB_USERNAME` | `root` |
| `quarkus.datasource.password` | `DB_PASSWORD` | `mysql123` |
| `quarkus.http.cors.origins` | `CORS_ORIGINS` | `http://localhost:5173,http://localhost:8080` |
| `maisumtodo.auth.issuer` | `AUTH_ISSUER` | `maisumtodo` |
| `maisumtodo.auth.secret` | `AUTH_SECRET` | `dev-change-me-maisumtodo-secret-with-32-chars` |
| `maisumtodo.auth.expires-seconds` | `AUTH_EXPIRES_SECONDS` | `604800` |
| `maisumtodo.auth.cookie-secure` | `AUTH_COOKIE_SECURE` | `false` |

For production, set a strong `AUTH_SECRET`, set `AUTH_COOKIE_SECURE=true` when serving over HTTPS, and replace the default database credentials.

Tests use an in-memory H2 database configured with MySQL compatibility mode.

## Database

The application expects a MySQL database named `maisumtodo` by default.

Example local setup:

```sql
CREATE DATABASE maisumtodo
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Flyway runs automatically at application startup:

```properties
quarkus.flyway.migrate-at-start=true
quarkus.flyway.validate-on-migrate=true
```

Hibernate schema generation is set to `validate`, so the database schema must match the migrations.

The initial migration creates:

- `system_login`
- `todo_project`
- `todo_task`

Projects belong to a user. Tasks belong to a project. Deleting a user deletes that user's projects, and deleting a project deletes its tasks through database foreign keys.

## Backend Commands

Run the backend in Quarkus dev mode:

```bash
./gradlew quarkusDev
```

On Windows PowerShell:

```powershell
.\gradlew.bat quarkusDev
```

Run tests:

```bash
./gradlew test
```

Build the backend:

```bash
./gradlew build
```

The JVM application is produced in:

```text
build/quarkus-app
```

Run the packaged JVM application:

```bash
java -jar build/quarkus-app/quarkus-run.jar
```

## Frontend Commands

All frontend commands must be run from `ui-vue`.

Install dependencies:

```bash
pnpm install
```

Run type checking and production build:

```bash
pnpm build
```

Run only the Vite build:

```bash
pnpm build-only
```

Run only TypeScript checks:

```bash
pnpm type-check
```

Run the Vite development server:

```bash
pnpm dev
```

The frontend dev server uses the proxy in `vite.config.ts` to send `/api` requests to the backend on port `8080`.

## Production-Style Build

The frontend build is not currently wired into the Gradle build. Build it explicitly before packaging the backend when the UI has changed.

From `ui-vue`:

```bash
pnpm install
pnpm build
```

Then from the repository root:

```bash
./gradlew build
```

The Quarkus package will include the static frontend files generated under `src/main/resources/META-INF/resources`.

## Docker

The repository includes Quarkus-generated Dockerfiles in `src/main/docker`.

For the JVM image:

```bash
./gradlew build
docker build -f src/main/docker/Dockerfile.jvm -t maisumtodo-jvm .
docker run --rm -p 8080:8080 maisumtodo-jvm
```

Pass the database and auth settings as environment variables when running outside the local defaults.

## API Reference

All authenticated endpoints require the `auth_token` cookie.

### Authentication

#### Register

```http
POST /api/auth/register
Content-Type: application/json
```

Request:

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

Responses:

- `201 Created` with the authenticated user and an `auth_token` cookie.
- `400 Bad Request` when email or password is missing.
- `409 Conflict` when the email already exists.

Response body:

```json
{
  "uuid": "user-uuid",
  "email": "user@example.com"
}
```

#### Login

```http
POST /api/auth/login
Content-Type: application/json
```

Request:

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

Responses:

- `200 OK` with the authenticated user and an `auth_token` cookie.
- `400 Bad Request` when email or password is missing.
- `401 Unauthorized` when credentials are invalid.

#### Current User

```http
GET /api/auth/me
```

Responses:

- `200 OK` with the authenticated user.
- `401 Unauthorized` when the cookie is missing, invalid, expired, or revoked.

#### Logout

```http
POST /api/auth/logout
```

Responses:

- `200 OK`; clears the cookie. If a valid user is present, their token version is incremented to invalidate older cookies.

### Projects

#### List Projects

```http
GET /api/projects
```

Response:

```json
[
  {
    "uuid": "project-uuid",
    "name": "Personal",
    "dateCreated": "2026-06-08T10:00:00",
    "lastUpdated": "2026-06-08T10:00:00"
  }
]
```

Projects are returned in creation order.

#### Create Project

```http
POST /api/projects
Content-Type: application/json
```

Request:

```json
{
  "name": "Personal"
}
```

Responses:

- `201 Created` with the created project.
- `400 Bad Request` when the name is empty.

#### Update Project

```http
PUT /api/projects/{projectUuid}
Content-Type: application/json
```

Request:

```json
{
  "name": "Work"
}
```

Responses:

- `200 OK` with the updated project.
- `400 Bad Request` when the name is empty.
- `404 Not Found` when the project does not exist or belongs to another user.

#### Delete Project

```http
DELETE /api/projects/{projectUuid}
```

Responses:

- `204 No Content`.
- `404 Not Found` when the project does not exist or belongs to another user.

Deleting a project also deletes its tasks.

### Tasks

Valid task statuses:

- `TODO`
- `DOING`
- `DONE`

#### List Tasks

```http
GET /api/projects/{projectUuid}/tasks
```

Response:

```json
[
  {
    "uuid": "task-uuid",
    "projectUuid": "project-uuid",
    "title": "Buy coffee",
    "status": "TODO",
    "sortOrder": 1,
    "dateCreated": "2026-06-08T10:00:00",
    "lastUpdated": "2026-06-08T10:00:00"
  }
]
```

Tasks are returned by `sortOrder ASC`, then `dateCreated ASC`.

#### Create Task

```http
POST /api/projects/{projectUuid}/tasks
Content-Type: application/json
```

Minimal request:

```json
{
  "title": "Buy coffee"
}
```

Full request:

```json
{
  "title": "Buy coffee",
  "status": "TODO",
  "sortOrder": 1
}
```

Responses:

- `201 Created` with the created task.
- `400 Bad Request` when the title is empty.
- `404 Not Found` when the project does not exist or belongs to another user.

If `status` is omitted, the backend uses `TODO`. If `sortOrder` is omitted, the backend places the task at the end of the project.

#### Update Task

```http
PUT /api/tasks/{taskUuid}
Content-Type: application/json
```

Update the title:

```json
{
  "title": "Buy coffee and milk"
}
```

Move a task:

```json
{
  "status": "DOING",
  "sortOrder": 2
}
```

Responses:

- `200 OK` with the updated task.
- `400 Bad Request` when the request body is missing or the supplied title is empty.
- `404 Not Found` when the task does not exist or belongs to another user.

#### Delete Task

```http
DELETE /api/tasks/{taskUuid}
```

Responses:

- `204 No Content`.
- `404 Not Found` when the task does not exist or belongs to another user.

## Frontend Routes

The frontend uses hash-based routing:

| Route | Description |
| --- | --- |
| `#/` | Landing page with login and registration form. Authenticated users are redirected to the app. |
| `#/app/:projectUuid?` | Main todo interface. Requires authentication. |
| `#/app/:projectUuid?tab=kanban` | Main interface with the kanban tab selected. |

The main todo screen has a project sidebar, a task list tab, and a kanban tab. The selected project is encoded in the route, which allows direct links to a project.

## Tests

Backend tests are under `src/test/java`.

Current coverage includes:

- `/hello` scaffold endpoint.
- Registering a user.
- Restoring the authenticated session through `/api/auth/me`.
- Creating a project.
- Creating a task.
- Updating a task status.
- Logging out and invalidating the previous token.

Run:

```bash
./gradlew test
```

## Development Notes

- Keep backend changes simple and local to the existing resources/entities unless a broader abstraction is clearly needed.
- Use Flyway migrations for database changes.
- Do not rely on Hibernate to create or update the production schema; it only validates it.
- Keep frontend API contracts aligned with the Java record responses in `AuthResource` and `TodoResource`.
- Build the frontend with `pnpm build` before packaging the backend if the production UI needs to be updated.
- The root README documents the full application. The `ui-vue/README.md` is still the default Vue template README and is not the source of truth for the whole project.

## Troubleshooting

### The backend cannot start because of schema validation

Confirm that MySQL is reachable, the selected database exists, and Flyway migrations have run successfully. The application validates the schema at startup.

### Login works locally but cookies are not kept in another environment

Check `AUTH_COOKIE_SECURE`, HTTPS, domain/host usage, and whether requests are sent with credentials. The frontend API client always uses `credentials: 'include'`.

### The packaged backend shows an old frontend

Run `pnpm build` inside `ui-vue` before running `./gradlew build`. The frontend output is copied into Quarkus resources by Vite, not by Gradle.

### Frontend API calls fail during `pnpm dev`

Make sure the Quarkus backend is running on `http://localhost:8080`. Vite proxies `/api` to that address.
