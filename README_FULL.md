# Shift Scheduler - Staff Shift Management System

シフトスケジューラー - スタッフシフト管理システム

A comprehensive Spring Boot and React-based staff shift scheduling and management system with role-based access control, permission management, and comprehensive shift management capabilities.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Development Guidelines](#development-guidelines)
- [Project Structure](#project-structure)

## 🎯 Overview

**Shift Scheduler** is an enterprise-grade staff management system designed to streamline shift assignment, scheduling, and calendar management with comprehensive role-based permissions and audit logging.

### Key Features

- **Staff Management**: Register and manage staff members with role-based access (MEMBER/CHIEF/MASTER)
- **Shift Assignment**: Create and manage confirmed shift assignments with date range queries
- **Shift Requests**: Allow staff to submit desired shifts with approval workflow
- **Group Management**: Organize staff into logical groups for permission scoping
- **Calendar Permissions**: Control inter-staff calendar viewing permissions
- **Shift Types**: Define flexible shift templates (morning, evening, night, off)
- **Qualifications**: Track staff certifications and qualifications
- **Member Provisioning**: Issue initial login information for new members and deliver it by SMTP when configured
- **Password Reset**: Issue one-time password reset URLs and verification codes with a one-hour validity period
- **System Settings**: Configure system-wide preferences via admin dashboard
- **Audit Logging**: Track all modifications with editor/updater information

## 🏗️ Architecture

### Layered Architecture

```
┌─────────────────────────────────────────┐
│      React Frontend (Vite)              │
├─────────────────────────────────────────┤
│      REST API Layer (Spring Web)        │
│  ├─ Controllers                         │
│  ├─ Error Handling                      │
│  └─ Request/Response DTOs               │
├─────────────────────────────────────────┤
│      Business Logic Layer               │
│  ├─ Services                            │
│  ├─ Permission Checks                   │
│  └─ State Management                    │
├─────────────────────────────────────────┤
│      Data Access Layer                  │
│  ├─ JPA Repositories                    │
│  ├─ Custom Queries                      │
│  └─ Domain Entities                     │
├─────────────────────────────────────────┤
│      Database Layer                     │
│  ├─ PostgreSQL                          │
│  ├─ Flyway Migrations                   │
│  └─ Sequences/Indices                   │
└─────────────────────────────────────────┘
```

### Domain-Driven Design (DDD)

Bounded Contexts:

1. **Staff Management**: Staff profiles, qualifications, group assignments
2. **Shift Management**: Shift types, assignments, requests
3. **Calendar Permissions**: Inter-staff calendar access control
4. **Member Onboarding**: Login provisioning and access management
5. **System Administration**: Global settings and configuration

### Access Control Model

Role-based permission matrix:

| Action | MASTER | CHIEF | MEMBER |
|--------|--------|-------|--------|
| View any staff shift | ✓ | ✓ (group only) | ✗ (self only) |
| Edit any staff shift | ✓ | ✓ (group only) | ✗ (self only) |
| View desired shifts | ✓ | ✓ (group only) | ✓ (self only) |
| Approve desired shifts | ✓ | ✓ (group only) | ✗ |
| Manage groups | ✓ | ✗ | ✗ |
| Manage system settings | ✓ | ✗ | ✗ |

## 🛠️ Technology Stack

### Backend

- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL 14+
- **ORM**: Spring Data JPA / Hibernate
- **Database Versioning**: Flyway
- **Build Tool**: Maven
- **Language**: Java 17+
- **Server**: Apache Tomcat (embedded)

### Frontend

- **Framework**: React 18+
- **Build Tool**: Vite
- **Language**: JavaScript/JSX
- **Styling**: CSS3 with responsive design
- **State Management**: React Hooks

### DevOps

- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **Version Control**: Git

## 📋 Prerequisites

### System Requirements

- Java 17 or higher
- PostgreSQL 14 or higher
- Node.js 16+ and npm 8+
- Docker & Docker Compose (optional)
- Git

### Mandatory WSL Execution Policy

All development, build, test, and run operations must be executed inside WSL.
Windows PowerShell / cmd is not the standard execution path for this project and should not be used for backend, frontend, Docker, or test commands.
Do not run project commands in Windows PowerShell or cmd.

Before running any command, verify the environment in WSL:

- Use WSL terminal for all Maven, Docker, and Spring Boot commands
- Use WSL tasks/debug launch configurations instead of Windows shell commands
- Treat WSL as the default execution environment for the project

```bash
uname -a
cat /proc/version
echo "$SHELL"
pwd
```

Validation checklist:

- `uname -a` or `cat /proc/version` includes `microsoft` / `WSL`
- `SHELL` is `/bin/bash` or `/bin/zsh`
- Working path is under `/mnt/...` (for example `/mnt/c/Users/.../Shift-Scheduler`)

### Local Development

```bash
# Check Java version
java -version

# Check PostgreSQL
psql --version

# Check Node.js
node --version
npm --version
```

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Shift-Scheduler
```

### 2. Database Setup

#### Using Docker Compose (Recommended)

```bash
docker-compose up -d postgres
```

#### Manual PostgreSQL Setup

```sql
-- Create database and user
createdb shift_scheduler
createuser shift_user
psql -c "ALTER USER shift_user WITH PASSWORD 'shift_password';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE shift_scheduler TO shift_user;" shift_scheduler
```

### 3. Backend Setup

```bash
cd backend

# Install dependencies and build
./mvnw clean install

# Run tests (optional)
./mvnw test
```

**Maven Dependencies Overview**:

- `spring-boot-starter-web`: REST API support
- `spring-boot-starter-data-jpa`: ORM and database access
- `spring-boot-starter-validation`: Input validation
- `spring-boot-starter-mail`: SMTP delivery for initial login and password reset messages
- `postgresql`: PostgreSQL driver
- `flyway-core`: Database versioning and migrations
- `junit-jupiter`: Testing framework
- `mockito`: Mocking framework for tests

### 4. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Build for production
npm run build
```

## ▶️ Running the Application

### Development Mode

#### Terminal 1: Backend Server

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Backend will start on `http://localhost:8080` (local development) or `http://localhost:8000` (Docker Compose)

#### Terminal 2: Frontend Development Server

```bash
cd frontend
npm run dev
```

Frontend will start on `http://localhost:5173` (by default Vite uses 5173)

### Using Docker Compose

```bash
# Build and start all services
docker-compose up --build

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Admin Page

- URL: `http://localhost:8000/admin` (Docker Compose) or `http://localhost:8080/admin` (local development)
- Available roles: `CHIEF` and `MASTER`
- Main tabs:
   - Staff Management
   - Qualification Management
   - Shift Type Management
   - System Settings
- Shift Edit page: `http://localhost:8000/admin/shifts` or `http://localhost:8080/admin/shifts`
- To return to the member page, use `http://localhost:8000/member` or `http://localhost:8080/member`

### Environment Variables

Create `.env` file in project root:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=shift_scheduler
DB_USER=postgres
DB_PASSWORD=postgres

# Server
SERVER_PORT=8080

# Frontend
CLIENT_DIST_DIR=../frontend/dist/

# Profiles
SPRING_PROFILES_ACTIVE=dev

# Optional SMTP delivery
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=your-smtp-user
SMTP_PASSWORD=your-smtp-password
SMTP_FROM=no-reply@example.com
PASSWORD_RESET_BASE_URL=https://scheduler.example.com/password-reset
```

## 📚 API Documentation

### Base URL

```
http://localhost:8080/api (local development)

Docker Compose で起動した場合は `http://localhost:8000/api`
```

### Staff Management Endpoints

```
GET    /api/staffs                    - List all staffs
GET    /api/staffs/{id}               - Get staff by ID
POST   /api/staffs                    - Create staff
PUT    /api/staffs/{id}               - Update staff
DELETE /api/staffs/{id}               - Deactivate staff
POST   /api/staffs/{id}/reactivate    - Reactivate staff
```

### Group Management Endpoints

```
GET    /api/groups                    - List all groups
GET    /api/groups/active             - List active groups
POST   /api/groups                    - Create group
PUT    /api/groups/{id}               - Update group
DELETE /api/groups/{id}               - Deactivate group
```

### Shift Management Endpoints

```
GET    /api/shift-types               - List shift types
POST   /api/shift-types               - Create shift type
GET    /api/shift-assignments         - Get assignments (date range)
POST   /api/shift-assignments         - Create assignment
PUT    /api/shift-assignments/{id}    - Update assignment
```

### Shift Request Endpoints

```
GET    /api/shift-requests/{id}       - Get shift request
POST   /api/shift-requests            - Submit desired shift
POST   /api/shift-requests/{id}/submit    - Transition to SUBMITTED
POST   /api/shift-requests/{id}/approve   - Approve (MASTER/CHIEF)
POST   /api/shift-requests/{id}/reject    - Reject (MASTER/CHIEF)
```

### Complete API Reference

See [API Documentation](docs/api_reference.md) for detailed endpoint specifications.

## 🗄️ Database Schema

### Core Tables

1. **staffs**: Staff/user profiles
   - Auto-generated staff_code (STF-00001 format)
   - Phone field (optional, numbers/hyphens)
   - Role-based access levels
   - Group assignment support

2. **groups**: Organizational units
   - Group code and name
   - Active/inactive status

3. **qualifications**: Staff certifications
   - Qualification definitions
   - Active tracking

4. **shift_types**: Shift templates
   - Start/end times
   - Off-shift designation
   - Display ordering

5. **shift_assignments**: Confirmed schedules
   - Staff-to-shift mapping
   - Date-based assignments
   - Editor audit trail

6. **shift_requests**: Desired shift submissions
   - Status workflow (DRAFT → SUBMITTED → APPLIED/REJECTED)
   - Date range queries
   - Submission/decision timestamps

7. **calendar_view_permissions**: Inter-staff calendar access
   - Permission request workflow
   - Expiration support

8. **member_login_provisionings**: New member access
   - Login code generation
   - Provisioning status tracking

9. **system_settings**: Configuration store
   - Key-value pairs
   - Boolean and text values

10. **password_reset_tokens**: One-time password reset credentials
   - Hashes for the URL token and verification code
   - One-hour expiry and single-use tracking

### Database Migrations

Located in `backend/src/main/resources/db/migration/`:

- `V001__001_initialize_schema.sql`: Initial schema
- `V002__002_seed_dev_data.sql`: Development/test data
- `V003__003_seed_production_defaults.sql`: Production defaults
- `V004__004_add_staff_ng_shift_time_bands.sql`: Staff NG shift bands
- `V005__005_add_staff_preferred_shift_time_bands.sql`: Staff preferred shift bands
- `V006__006_allow_null_desired_shift_type.sql`: Vacation request support
- `V007__007_add_role_labels_system_setting.sql`: Role label setting
- `V008__008_add_password_reset_tokens.sql`: Password reset tokens
- `V009__009_add_password_changed_at.sql`: JWT invalidation after password changes

### Entity Relationships

```
Group ← Staff → Qualification (M2M via staff_qualifications)
       ↓
       ShiftAssignment
       ShiftRequest
       CalendarViewPermission (Requester/Target Staff)
       MemberLoginProvisioning
       SystemSetting (Updater)
```

## 💻 Development Guidelines

### Code Organization

```
backend/
├── src/main/java/com/shiftscheduler/server/
│   ├── ShiftSchedulerApplication.java      (Spring Boot entry point)
│   ├── api/                                (REST Controllers + DTOs)
│   ├── entity/                             (JPA Entities)
│   ├── repository/                         (Data Access Layer)
│   ├── service/                            (Business Logic)
│   ├── exception/                          (Custom Exceptions)
│   └── controller/                         (SPA forwarding)
├── src/main/resources/
│   ├── application.properties              (Default configuration)
│   ├── application-dev.properties          (Development profile)
│   ├── application-prod.properties         (Production profile)
│   └── db/migration/                       (Flyway migrations)
└── pom.xml                                 (Maven dependencies)

frontend/
├── src/
│   ├── components/                         (React components)
│   ├── services/                           (API client utilities)
│   ├── styles/                             (CSS modules)
│   ├── App.jsx                             (Main component)
│   └── main.jsx                            (React DOM entry)
├── package.json                            (npm dependencies)
└── vite.config.js                          (Vite configuration)
```

### Running Tests

```bash
cd backend

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=StaffServiceTest

# Run with coverage
mvn test jacoco:report
```

### Code Formatting

```bash
cd backend

# Format code with Maven Spotless plugin
mvn spotless:apply
```

### Building Docker Image

```bash
# Build both backend and frontend images
docker build -t shift-scheduler:latest -f Dockerfile .

# Run container
docker run -p 8000:8080 shift-scheduler:latest
```

### Common Development Tasks

#### Add New Entity

1. Create entity class in `backend/src/main/java/com/shiftscheduler/server/entity/`
2. Create repository interface extending `JpaRepository`
3. Create service class with business logic
4. Create API controller with REST endpoints
5. Create DTO request/response classes
6. Create Flyway migration for database table

#### Add New API Endpoint

1. Create controller method with `@RequestMapping` or `@GetMapping`/`@PostMapping`
2. Create corresponding service method
3. Add repository query if needed
4. Create request/response DTOs
5. Add validation and error handling
6. Document endpoint in API reference

#### Add New Database Migration

1. Create file: `backend/src/main/resources/db/migration/VX__description.sql`
2. Follow migration naming convention: `VX__snake_case_description.sql`
3. Write idempotent SQL (include `IF NOT EXISTS` clauses)
4. Test migration locally before committing

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/feature-name

# Make changes and commit
git add .
git commit -m "feat: description of changes"

# Push to remote
git push origin feature/feature-name

# Create pull request on GitHub
```

### Logging

Use SLF4J with Logback:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyService {
  private static final Logger logger = LoggerFactory.getLogger(MyService.class);
  
  public void doSomething() {
    logger.debug("Debug message with {}", variable);
    logger.info("Info message");
    logger.warn("Warning message");
    logger.error("Error message", exception);
  }
}
```

## 📊 Project Statistics

- **Total Java Files**: 57
- **Total Lines of Backend Code**: ~7,000+
- **REST Endpoints**: 57
- **Database Tables**: 11
- **Database Migrations**: 10
- **React Components**: 5
- **Frontend Lines of Code**: ~1,500+

## 🐛 Troubleshooting

### Database Connection Issues

```
ERROR: Error: connect ECONNREFUSED 127.0.0.1:5432
```

**Solution**: Ensure PostgreSQL is running and connection parameters match.

```bash
# Check if PostgreSQL is running (macOS)
brew services list

# Start PostgreSQL
brew services start postgresql
```

### Port Already in Use

```
ERROR: Address already in use
```

**Solution**: Change port in `application.properties` or kill existing process.

```bash
# macOS/Linux: Find and kill process on port 8080
lsof -i :8080
kill -9 <PID>
```

### Flyway Migration Fails

```
ERROR: Validate failed: Migration checksum mismatch for version 3
```

**Solution**: Ensure migration files haven't been modified. Use `PRAGMA foreign_keys=OFF;` to disable constraints during cleanup (if necessary).

### Frontend Build Issues

```bash
# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install
npm run build
```

## 📖 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Vite Documentation](https://vitejs.dev/)

## 📝 License

This project is proprietary and confidential.

## 👥 Contributing

Team development guidelines:

1. Follow existing code style and patterns
2. Add tests for new features
3. Update documentation when making changes
4. Use meaningful commit messages
5. Review code before merging

## 📞 Support

For issues, questions, or feature requests, please contact the development team or create an issue in the repository.

---

**Last Updated**: 2026-07-26
**Version**: 1.0.0
