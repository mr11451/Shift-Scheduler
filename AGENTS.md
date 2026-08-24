# Shift Scheduler Agent Guide

## Working rules

- Run development, build, test, and Docker commands in WSL, not Windows PowerShell or cmd.
- Before running a command, confirm the shell with `uname -a`, `cat /proc/version`, `echo "$SHELL"`, and `pwd`. The kernel should identify WSL and the working directory should be under `/mnt/...`.
- Keep changes focused and preserve the existing Java, React, and documentation patterns. Do not commit changes unless explicitly requested.
- Treat authentication, role checks, and password-reset flows as security-sensitive. Preserve DTO boundaries and never expose JPA entities directly from REST endpoints.

## Repository map

- `backend/` is a Java 17 Spring Boot 3.3.4 service using Spring Web, JPA, Flyway, PostgreSQL, JWT, and mail.
  - `api/` contains REST controllers and request/response DTOs.
  - `service/` owns business rules and permission checks.
  - `domain/` contains JPA entities; `repository/` contains Spring Data repositories.
  - `filter/`, `annotation/`, `aspect/`, `exception/`, and `util/` provide cross-cutting behavior.
- `frontend/` is a React 18 + Vite application using React Router.
  - Route-level pages live in `src/pages/` and top-level app views in `src/`.
  - Reusable UI is in `src/components/`; JWT state is in `src/context/AuthContext.jsx`.
  - Shared API, access, role-label, date, and shift-status logic belongs in `src/utils/`.
  - Component styles are colocated as CSS files. Utility tests use the `*.test.js` convention.
- `backend/src/main/resources/db/migration/` contains ordered Flyway migrations. Use `V###__descriptive_name.sql`; keep schema changes and seed changes in migrations rather than application startup code.
- `.vscode/tasks.json` contains WSL-wrapped tasks for backend run, compile, tests, and frontend build.

## Commands

Run these from WSL at the repository root unless noted otherwise:

```bash
# Backend
cd backend
./mvnw -DskipTests compile
./mvnw test
./mvnw -Dtest=ShiftAssignmentServiceTests test
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev

# Frontend
cd frontend
npm install
npm run build
npx vitest run
npm run dev
```

The frontend dev server runs on `http://127.0.0.1:5173` and proxies `/api`; the local backend normally runs on port `8080`. For the integrated stack, run `docker compose up --build` in WSL and use port `8000`. See [`.vscode/tasks.json`](.vscode/tasks.json) for task equivalents.

## Implementation guidance

- Follow the existing controller -> service -> repository/domain flow. Put authorization and business invariants in services, with controller validation and transport concerns at the API boundary.
- Use the existing `fetchWithAuth` and `AuthContext` flows for authenticated frontend requests. Keep role and view decisions aligned with `ProtectedRoute` and the utilities in `frontend/src/utils/`.
- When changing an API, update the relevant frontend caller, tests, and documentation together. Check [docs/api_reference.md](docs/api_reference.md) and [docs/api_db_model.md](docs/api_db_model.md) before changing request or response shapes.
- When changing shift-generation behavior, consult [docs/auto_shift_generation_rules.md](docs/auto_shift_generation_rules.md) and add or update focused service tests.
- When changing screens or routes, consult [docs/README.md](docs/README.md) and the matching screen specification under `docs/`.
- Use SLF4J parameterized logging in backend code. Avoid logging passwords, reset tokens, SMTP credentials, or JWTs.
- Prefer focused tests first, then run the full affected module test/build. Do not treat a successful frontend build as a substitute for backend tests.

## Environment and documentation

- Development profile configuration is in `backend/src/main/resources/application-dev.properties`; test configuration uses H2 and test properties. Docker uses PostgreSQL with different host and port settings, so verify the active profile before diagnosing connection failures.
- Keep SMTP and database credentials in environment variables or deployment secrets, never in tracked files. See [README.md](README.md) for required variables and WSL/Docker setup.
- Use [README_FULL.md](README_FULL.md) for the broader setup and troubleshooting guide.
- Use [docs/domain_model.md](docs/domain_model.md) for domain rules, [docs/shift_request_flow.md](docs/shift_request_flow.md) for request states, and [backend/src/main/resources/db/migration/README.md](backend/src/main/resources/db/migration/README.md) for migration conventions.