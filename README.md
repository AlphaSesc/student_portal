# Student Portal Service

The **Student Portal** is the primary entry point for students in the University system. It handles user authentication, course enrollment, student profile management, and orchestrates communication with the **Finance** and **Library** microservices.

---

## Overview

This service is responsible for:

- **User registration & authentication** using JWT-based stateless security
- **Course catalog management** (browse, create, update, delete courses)
- **Enrollment workflow** — when a student enrolls in a course, this service:
    - Creates a `Student` profile (on first enrollment)
    - Calls the **Finance Service** to create a finance account and generate an invoice
    - Calls the **Library Service** to register the student in the library system
- **Student profile management** (view/update personal details)
- **Graduation eligibility check** — verifies via Finance Service that the student has no outstanding invoices
- **Invoice payment proxy** — forwards payment requests to the Finance Service

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.4 |
| Security | Spring Security + JWT (jjwt 0.13.0) |
| Database | MySQL 8.x |
| ORM | Spring Data JPA / Hibernate |
| Templating | Thymeleaf (for view layer) |
| Build Tool | Maven |
| External Communication | RestTemplate (Finance & Library clients) |

---

## Architecture

```
┌────────────────────────────────────────────────┐
│               Student Portal                   │
│           (Port 8081 — main API)               │
└────────────────────────────────────────────────┘
              │                       │
              ▼                       ▼
   ┌──────────────────┐      ┌──────────────────┐
   │  Finance Service │      │ Library Service  │
   │    (Port 8080)   │      │    (Port 8082)   │
   └──────────────────┘      └──────────────────┘
```

The Student Portal **does not store** financial or library data — it delegates to the respective microservices via REST.

---

## API Endpoints

### Public Endpoints (no auth required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Authenticate and receive a JWT token |

### Authenticated Endpoints (require JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/courses` | List all courses |
| `GET` | `/api/courses/{id}` | Get course by ID |
| `POST` | `/api/courses` | Create a new course |
| `PUT` | `/api/courses/{id}` | Update a course |
| `DELETE` | `/api/courses/{id}` | Delete a course |
| `POST` | `/api/enrollments` | Enroll in a course (STUDENT only) |
| `GET` | `/api/enrollments/me` | View my enrollments |
| `GET` | `/api/students/me` | Get my profile |
| `PUT` | `/api/students/me` | Update my profile |
| `GET` | `/api/students/me/graduation-eligibility` | Check graduation eligibility |
| `POST` | `/api/finance/pay` | Pay an invoice |
| `GET` | `/api/finance/my-invoices` | View my invoice history |

---

## Configuration

Configuration is split between two files in `src/main/resources/`:

### `application.yaml`
```yaml
server:
  port: 8081

spring:
  application:
    name: student-portal-service
  profiles:
    active: dev

finance:
  service:
    base-url: http://localhost:8080
library:
  service:
    base-url: http://localhost:8082
```

### `application-dev.yaml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/student_portal
    username: <your-username>
    password: <your-password>
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: <your-jwt-secret-key>
  expiration: 86400000  # 24 hours in milliseconds
```

> **Note:** Update `username`, `password`, and `jwt.secret` to match your local environment.

---

## Prerequisites

1. **Java 21** ([Adoptium](https://adoptium.net/))
2. **Maven** (or use included `mvnw` wrapper)
3. **MySQL Server** running on `localhost:3306`
4. **Database created**:
   ```sql
   CREATE DATABASE student_portal;
   ```
5. **Finance Service** and **Library Service** running on ports 8080 and 8082 respectively (for full functionality)

---

## Running the Service

### Using Maven Wrapper
```bash
./mvnw spring-boot:run
```

### Using Maven
```bash
mvn spring-boot:run
```

### From IntelliJ IDEA
- Open the project as a Maven project
- Run `StudentPortalApplication.java` directly (right-click → **Run**)

The service will start on **http://localhost:8081**.

---

## Project Structure

```
src/main/java/com/example/student_portal/
├── client/              # REST clients for Finance & Library services
├── config/              # Spring Security configuration
├── controller/          # REST controllers
├── dto/                 # Request/response DTOs
├── entity/              # JPA entities
├── exception/           # Custom exceptions + global handler
├── repository/          # JPA repositories
├── security/            # JWT filter, JwtService, CustomUserDetails
├── service/             # Business logic
└── util/                # Helpers (e.g., StudentIdGenerator)
```

---

## Authentication Flow

1. User calls `POST /api/auth/register` with email, password, and role
2. User calls `POST /api/auth/login` and receives a JWT token
3. Subsequent requests include the token in the `Authorization` header:
   ```
   Authorization: Bearer <token>
   ```
4. The `JwtAuthenticationFilter` validates the token on every protected request

---

## Cross-Service Communication

When a student enrolls in a course (`POST /api/enrollments`), the following happens:

1. Student profile created (if first enrollment)
2. **Finance Service** called to create a finance account
3. **Library Service** called to register the student
4. **Finance Service** called again to create a course enrollment invoice

If any external service is down, the enrollment fails with a `5xx` response. Make sure both services are running before testing enrollment flows.

---

## Default Roles

- `STUDENT` — can enroll in courses, view profile, pay invoices
- `ADMIN` — manages courses (currently no admin-specific endpoints active)

---
