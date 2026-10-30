# University Management System (UMS) - File-by-File Guide

This document provides a comprehensive explanation of every file and folder in the **University Management System** project, describing its role, architecture, and interactions with other components.

---

## 1. Directory Tree Overview

```text
university-management-system/
|-- pom.xml                                     # Maven build configuration & dependencies
|-- Dockerfile                                  # Multi-stage container build instructions
|-- compose.yaml                                # Docker Compose services (PostgreSQL & Spring Boot App)
|-- .dockerignore                               # Excludes files from Docker build context
|-- .gitignore                                  # Git version control exclusions
|-- .gitattributes                              # Git line ending configurations
|-- mvnw                                        # Maven Wrapper script for Linux / macOS
|-- mvnw.cmd                                    # Maven Wrapper script for Windows
|-- README.md                                   # Project documentation, RBAC matrix, and curl guide
|-- FILES_EXPLANATION.md                        # Complete file-by-file reference guide (this file)
|
`-- src/
    |-- main/
    |   |-- java/com/example/ums/
    |   |   |-- UmsApplication.java             # Main application entry point
    |   |   |
    |   |   |-- config/
    |   |   |   |-- SecurityConfig.java         # Spring Security & RBAC route configuration
    |   |   |   `-- DataInitializer.java        # Startup seeder for default accounts
    |   |   |
    |   |   |-- controller/
    |   |   |   |-- AdminController.java        # REST Controller for /api/admins
    |   |   |   |-- TeacherController.java      # REST Controller for /api/teachers
    |   |   |   `-- StudentController.java      # REST Controller for /api/students
    |   |   |
    |   |   |-- model/
    |   |   |   |-- BaseUser.java               # Abstract mapped superclass for common user attributes
    |   |   |   |-- Admin.java                  # JPA Entity for Admins
    |   |   |   |-- Teacher.java                # JPA Entity for Teachers
    |   |   |   `-- Student.java                # JPA Entity for Students
    |   |   |
    |   |   |-- repository/
    |   |   |   |-- AdminRepository.java        # Spring Data JPA repository for Admins
    |   |   |   |-- TeacherRepository.java      # Spring Data JPA repository for Teachers
    |   |   |   `-- StudentRepository.java      # Spring Data JPA repository for Students
    |   |   |
    |   |   `-- security/
    |   |       `-- CustomUserDetailsService.java # Database-backed UserDetailsService for Spring Security
    |   |
    |   `-- resources/
    |       `-- application.yml                 # Spring Boot application & database properties
    |
    `-- test/
        `-- java/com/example/ums/
            |-- UmsApplicationTests.java        # Spring context load smoke test
            `-- RbacAndCrudTests.java           # Integration tests for RBAC rules and CRUD operations
```

---

## 2. Root & Build Configuration Files

### `pom.xml`
- **Purpose**: Maven Project Object Model file defining the project metadata, Java version, dependencies, and build plugins.
- **Key Details**:
  - **Parent**: `spring-boot-starter-parent` (version `3.3.4`).
  - **Java Target**: `<java.version>21</java.version>`.
  - **Dependencies**:
    - `spring-boot-starter-web`: Provides embedded Tomcat and Spring MVC for building RESTful APIs.
    - `spring-boot-starter-data-jpa`: Hibernate ORM and Spring Data JPA for object-relational mapping.
    - `spring-boot-starter-security`: Spring Security for authentication and role-based authorization.
    - `postgresql`: Runtime JDBC driver for PostgreSQL.
    - `h2`: Runtime in-memory SQL database used for automated testing without needing an external database.
    - `spring-boot-starter-test` & `spring-security-test`: Testing frameworks (JUnit 5, MockMvc, Mock Security users).
  - **Build Plugin**: `spring-boot-maven-plugin` packages the application as an executable fat JAR.

### `Dockerfile`
- **Purpose**: Defines instructions to build a lightweight, production-ready container image for the Spring Boot application using a **two-stage build**:
  1. **Build Stage (`maven:3.9.6-eclipse-temurin-21 AS build`)**:
     - Copies `pom.xml` and downloads dependencies (`mvn dependency:go-offline -B`).
     - Copies `src/` and builds the fat JAR using `mvn clean package -DskipTests`.
  2. **Runtime Stage (`eclipse-temurin:21-jre-alpine`)**:
     - Uses a minimal Alpine Linux JRE image to minimize image footprint and attack surface.
     - Copies only the resulting `*.jar` from the build stage into `app.jar`.
     - Exposes port `8080` and defines entrypoint: `["java", "-jar", "app.jar"]`.

### `compose.yaml`
- **Purpose**: Orchestrates the multi-container environment with Docker Compose.
- **Services**:
  1. `db`:
     - Image: `postgres:16-alpine`.
     - Environment: Initializes database `university_db` with user `postgres` and password `postgres`.
     - Exposes port `5432:5432`.
     - Health check: Executes `pg_isready -U postgres -d university_db`.
     - Volume: Maps named volume `pgdata` to `/var/lib/postgresql/data` for persistent database storage.
  2. `app`:
     - Builds the local `Dockerfile`.
     - Connects to database using `SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/university_db`.
     - Depends on `db` with `condition: service_healthy` to guarantee PostgreSQL is ready before starting the web application.
     - Maps port `8080:8080` to the host.

### `.dockerignore`
- **Purpose**: Specifies files and directories that should not be transferred into the Docker build context (e.g., `target/`, `.git/`, IDE directories, and local wrapper scripts), accelerating build performance.

### `.gitignore` & `.gitattributes`
- **`.gitignore`**: Excludes build outputs (`/target/`), IDE settings (`.idea/`, `.vscode/`, `*.iml`), system files (`.DS_Store`, `Thumbs.db`), and log files from git tracking.
- **`.gitattributes`**: Normalizes line endings across operating systems (forces `LF` on Unix scripts and normalizes text files).

### `mvnw` & `mvnw.cmd`
- **Purpose**: The Apache Maven Wrapper.
  - `mvnw`: Shell script for macOS/Linux.
  - `mvnw.cmd`: Batch script for Windows.
- Allows building and testing the project on any machine with Java installed, without requiring a pre-installed standalone Maven installation.

### `README.md`
- **Purpose**: Project documentation providing an executive summary, RBAC privilege matrix, default accounts, how-to-run instructions (local & Docker), and example `curl` commands for all endpoints.

---

## 3. Application Resources

### `src/main/resources/application.yml`
- **Purpose**: Central configuration file for application properties.
- **Key Settings**:
  - `spring.datasource.url`: Defaults to `jdbc:postgresql://localhost:5432/university_db`, with fallback overridable by environment variable `SPRING_DATASOURCE_URL`.
  - `spring.datasource.username` & `password`: Default to `postgres`/`postgres`.
  - `spring.jpa.hibernate.ddl-auto`: Set to `${SPRING_JPA_HIBERNATE_DDL_AUTO:update}` to automatically update database schemas upon entity changes.
  - `server.port`: Configured to port `8080`.

---

## 4. Application Source Code (`src/main/java/com/example/ums/`)

### Application Entry Point
- **`UmsApplication.java`**:
  - Contains `@SpringBootApplication` and the `main(String[] args)` method.
  - Bootstraps the Spring context, triggers component scanning, autoconfiguration, and launches the embedded Tomcat server.

---

### Configuration Layer (`config/`)

- **`SecurityConfig.java`**:
  - Configures Spring Security via `@EnableWebSecurity` and a `@Bean SecurityFilterChain`.
  - **CSRF**: Disabled (`csrf(AbstractHttpConfigurer::disable)`) to allow straightforward REST API interaction with tools like `curl` and Postman.
  - **Route Authorization (RBAC)**:
    - `/api/admins/**` requires authority `ROLE_ADMIN`.
    - `/api/teachers/**` requires authority `ROLE_ADMIN`.
    - `/api/students/**` requires authority `ROLE_ADMIN` or `ROLE_TEACHER`.
    - All other endpoints require authentication.
  - **Authentication Mechanisms**:
    - `httpBasic()`: Enables HTTP Basic Authentication (ideal for REST testing headers: `Authorization: Basic ...`).
    - `formLogin()`: Enables browser-based login form.
    - `logout()`: Configures `/logout` endpoint and redirection.
  - **Password Encoder**: Declares `BCryptPasswordEncoder` bean for one-way salt hashing.

- **`DataInitializer.java`**:
  - Implements `CommandLineRunner`, running automatically after the Spring Application Context is loaded.
  - Checks if database tables are empty (`count() == 0`) and seeds the initial default accounts:
    - **Admin**: `username: admin`, `password: admin123` (hashed via BCrypt), department: `Administration`.
    - **Teacher**: `username: teacher`, `password: teacher123`, department: `Computer Science`, designation: `Professor`.
    - **Student**: `username: student`, `password: student123`, student ID: `CSE-2026-001`, semester: `4`.

---

### Domain Model Layer (`model/`)

- **`BaseUser.java`**:
  - Annotated with `@MappedSuperclass` to provide common properties to all user entities without requiring a dedicated polymorphic base table.
  - Fields:
    - `id`: Auto-generated identity primary key (`@GeneratedValue(strategy = GenerationType.IDENTITY)`).
    - `username`: Non-null, unique identifier string.
    - `password`: Marked with `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` so passwords can be received during user creation/update but are **never returned in JSON responses**.
    - `role`: Role string (`ROLE_ADMIN`, `ROLE_TEACHER`, `ROLE_STUDENT`).
    - `name`: Display name.
    - `email`: Non-null, unique email.

- **`Admin.java`**:
  - Extends `BaseUser` and annotated with `@Entity`, `@Table(name = "admins")`.
  - Automatically sets role to `ROLE_ADMIN`.
  - Additional field: `department`.

- **`Teacher.java`**:
  - Extends `BaseUser` and annotated with `@Entity`, `@Table(name = "teachers")`.
  - Automatically sets role to `ROLE_TEACHER`.
  - Additional fields: `department`, `designation`.

- **`Student.java`**:
  - Extends `BaseUser` and annotated with `@Entity`, `@Table(name = "students")`.
  - Automatically sets role to `ROLE_STUDENT`.
  - Additional fields: `studentId`, `department`, `semester`.

---

### Repository Layer (`repository/`)

- **`AdminRepository.java`**:
  - Interface extending `JpaRepository<Admin, Long>`.
  - Provides CRUD methods plus custom query methods: `findByUsername(String username)`, `existsByUsername(String username)`, and `existsByEmail(String email)`.

- **`TeacherRepository.java`**:
  - Interface extending `JpaRepository<Teacher, Long>`.
  - Query methods: `findByUsername(String username)`, `existsByUsername(String username)`, and `existsByEmail(String email)`.

- **`StudentRepository.java`**:
  - Interface extending `JpaRepository<Student, Long>`.
  - Query methods: `findByUsername(String username)`, `existsByUsername(String username)`, `existsByEmail(String email)`, and `existsByStudentId(String studentId)`.

---

### Security Services (`security/`)

- **`CustomUserDetailsService.java`**:
  - Implements Spring Security's `UserDetailsService`.
  - When a user logs in, `loadUserByUsername(String username)` searches sequentially:
    1. `AdminRepository.findByUsername(username)`
    2. `TeacherRepository.findByUsername(username)`
    3. `StudentRepository.findByUsername(username)`
  - Throws `UsernameNotFoundException` if the username is not found in any repository.
  - Returns a Spring Security `UserDetails` (instantiated as `org.springframework.security.core.userdetails.User`) with the BCrypt-hashed password and granted authority matching `user.getRole()`.

---

### Controller Layer (`controller/`)

- **`AdminController.java` (`/api/admins`)**:
  - Secured exclusively to `ROLE_ADMIN`.
  - Endpoints:
    - `GET /api/admins`: Returns all admins.
    - `GET /api/admins/{id}`: Returns a specific admin or `404 Not Found`.
    - `POST /api/admins`: Hashes the password with `BCryptPasswordEncoder`, sets role to `ROLE_ADMIN`, persists, and returns `201 Created`.
    - `PUT /api/admins/{id}`: Updates name, email, department, and conditionally re-encodes the password if a new non-blank password is provided; returns `200 OK` or `404 Not Found`.
    - `DELETE /api/admins/{id}`: Deletes the record and returns `204 No Content` or `404 Not Found`.

- **`TeacherController.java` (`/api/teachers`)**:
  - Secured exclusively to `ROLE_ADMIN`.
  - Endpoints:
    - `GET /api/teachers`: Returns list of teachers.
    - `GET /api/teachers/{id}`: Returns teacher details or `404`.
    - `POST /api/teachers`: Enforces role `ROLE_TEACHER`, encodes password, saves, and returns `201 Created`.
    - `PUT /api/teachers/{id}`: Updates name, email, department, designation, and optional new password.
    - `DELETE /api/teachers/{id}`: Deletes record or returns `404`.

- **`StudentController.java` (`/api/students`)**:
  - Secured to `ROLE_ADMIN` and `ROLE_TEACHER`.
  - Endpoints:
    - `GET /api/students`: Returns all students.
    - `GET /api/students/{id}`: Returns student details or `404`.
    - `POST /api/students`: Enforces role `ROLE_STUDENT`, encodes password, saves, and returns `201 Created`.
    - `PUT /api/students/{id}`: Updates name, email, studentId, department, semester, and optional new password.
    - `DELETE /api/students/{id}`: Deletes record or returns `404`.

---

## 5. Automated Tests (`src/test/java/com/example/ums/`)

- **`UmsApplicationTests.java`**:
  - Basic context loading sanity test (`contextLoads()`).
  - Configures an in-memory H2 database (`spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`) so tests run without requiring a running PostgreSQL instance.

- **`RbacAndCrudTests.java`**:
  - Full Spring Boot integration test with `MockMvc` and mock security contexts:
    - `unauthenticatedUser_isUnauthorized()`: Verifies that unauthenticated requests to `/api/students` return `401 Unauthorized`.
    - `admin_canAccessAllResources()`: Uses `@WithMockUser(roles = "ADMIN")` to verify full `200 OK` access across `/api/admins`, `/api/teachers`, and `/api/students`.
    - `teacher_canAccessStudents_butForbiddenFromAdminsAndTeachers()`: Uses `@WithMockUser(roles = "TEACHER")` to verify `200 OK` on `/api/students` and `403 Forbidden` on `/api/admins` and `/api/teachers`.
    - `student_isForbiddenFromAllManagementEndpoints()`: Uses `@WithMockUser(roles = "STUDENT")` to verify `403 Forbidden` on all three management routes.
    - `admin_canCreateAndReadStudent()`: Posts a new student JSON payload, asserts `201 Created`, validates returned fields, and verifies that the `password` field is excluded from the JSON response (`jsonPath("$.password").doesNotExist()`).

---

## 6. Component Relationship Diagram

```text
[ Client (Browser / Postman / curl) ]
                |
                v
       [ SecurityConfig ]  <--- (CustomUserDetailsService)
                |
    +-----------+-----------+
    |           |           |
    v           v           v
[AdminCtrl] [TeacherCtrl] [StudentCtrl]
    |           |           |
    v           v           v
[AdminRepo] [TeacherRepo] [StudentRepo]
    |           |           |
    +-----------+-----------+
                |
                v
    [ PostgreSQL Database / H2 Test DB ]
    (admins, teachers, students tables)
```

