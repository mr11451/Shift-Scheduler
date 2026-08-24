# Production Setup

This document describes a production-oriented deployment of Shift Scheduler. Run all commands from WSL. Do not use the development defaults in `docker-compose.yml` for a public deployment.

## Requirements

- WSL2 with Docker Engine or Docker Desktop integration
- Docker and Docker Compose
- PostgreSQL 14 or higher when using an external database
- Node.js 20 or higher and npm 9 or higher for local frontend builds
- A DNS name and TLS termination through a reverse proxy or load balancer

Verify the WSL environment before running commands:

```bash
uname -a
cat /proc/version
echo "$SHELL"
pwd
node --version
npm --version
docker --version
docker compose version
```

The Node.js and npm commands must resolve to the WSL/Linux installation. The Docker build uses Node.js 20 for the frontend stage.

## Configuration

Store secrets outside tracked files. Use a deployment secret manager or an untracked `.env` file with restricted permissions.

Required backend variables:

```env
DB_HOST=postgres.example.internal
DB_PORT=5432
DB_NAME=shift_scheduler
DB_USER=shift_scheduler
DB_PASSWORD=<strong-database-password>
SPRING_PROFILES_ACTIVE=prod
```

Before production deployment, replace the default JWT signing key in `backend/src/main/java/com/shiftscheduler/server/util/JwtTokenUtil.java` with a long random secret managed by the deployment platform. The current application reads this key from code, not from a `JWT_SECRET` environment variable.

SMTP variables are required when the application sends initial login or password reset mail:

```env
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=<smtp-user>
SMTP_PASSWORD=<smtp-password>
SMTP_FROM=no-reply@example.com
SMTP_AUTH=true
SMTP_STARTTLS=true
PASSWORD_RESET_BASE_URL=https://scheduler.example.com/password-reset
```

Do not commit database, SMTP, JWT, or reset URL credentials. Change all example values before deployment.

## Database

Create an empty production database and a dedicated application user. Grant only the permissions required by the application and migrations.

Flyway applies the two migration files in `backend/src/main/resources/db/migration/` when the backend starts:

- `V001__001_initialize_schema.sql`: base schema and indexes
- `V002__002_seed_dev_data.sql`: consolidated schema additions and canonical defaults

The consolidated migrations include development seed records. Review `V002__002_seed_dev_data.sql` before using it against a production database and remove or replace non-production seed data as part of the deployment process. Never reset a production database to resolve a migration issue without a verified backup and a rollback plan.

## Docker Deployment

Build the image from the repository root in WSL:

```bash
docker build -t shift-scheduler:<version> -f Dockerfile .
```

Run the image with a secret file or an equivalent deployment platform configuration. Do not publish the debug port `5005` in production.

```bash
docker run -d \
  --name shift-scheduler \
  --restart unless-stopped \
  -p 8000:8080 \
  --env-file /secure/path/shift-scheduler.env \
  shift-scheduler:<version>
```

The container serves the built React application and the Spring Boot API. Put a reverse proxy or load balancer in front of port `8000` (or the platform equivalent) to provide HTTPS, the production hostname, security headers, and request size/rate limits.

For a production orchestration platform, configure:

- A managed PostgreSQL instance or a separately managed database container
- Persistent log collection for `/var/log/shift-scheduler/shift-scheduler.log`
- Health checks and automatic restart policy
- Secret injection for all credentials
- Network rules allowing the application to reach PostgreSQL and SMTP only
- Scheduled PostgreSQL backups and a tested restore procedure

The checked-in `docker-compose.yml` is intended for local development. It exposes PostgreSQL on `5433`, uses development credentials, and enables JDWP on `5005`; do not expose those settings publicly.

## Local Production-Profile Run

To verify the production profile without Docker, build the frontend and backend separately:

```bash
cd frontend
npm ci
npm run build

cd ../backend
./mvnw -DskipTests package
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

Ensure PostgreSQL is reachable through the configured `DB_*` variables. The backend listens on port `8080` by default. For this local profile run, set `CLIENT_DIST_DIR` to the built frontend directory if the application is serving the SPA from disk.

## Post-Deployment Checks

1. Confirm the application responds through the HTTPS hostname.
2. Confirm `GET /api/system-settings` requires authentication.
3. Log in with a non-administrative test account created through the approved provisioning process.
4. Verify that migrations completed and no development credentials remain.
5. Test staff creation, shift requests, calendar permissions, and password reset mail.
6. Confirm that `CHIEF` and `MASTER` users do not see the member permission-target section.
7. Confirm logs contain no passwords, JWTs, reset tokens, SMTP credentials, or verification codes.
8. Confirm backups and the documented rollback procedure are usable.

For feature and API details, see [docs/api_reference.md](api_reference.md), [docs/domain_model.md](domain_model.md), and [docs/README.md](README.md).
