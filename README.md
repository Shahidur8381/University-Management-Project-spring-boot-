# University Management System (UMS)

CSE 3230 Home Task - 4th Lab

A simple University Management System developed using Java Spring Boot, Spring Security, and PostgreSQL.

---

## Features

1. **REST API**: Full CRUD operations for Admin, Teacher, and Student resources following REST conventions and standard HTTP status codes.
2. **Spring Security**: Form login, logout, BCrypt password hashing, and Role-Based Access Control (RBAC).
3. **Dockerization**: Containerized multi-stage `Dockerfile` and `compose.yaml` to run the application with PostgreSQL.
4. **Data Seeding**: Automatically seeds default accounts on application startup.

---

## Role-Based Access Control (RBAC) Rules

| Role | Admin Resource (`/api/admins`) | Teacher Resource (`/api/teachers`) | Student Resource (`/api/students`) |
| :--- | :---: | :---: | :---: |
| **Admin** | Read & Write (Full CRUD) | Read & Write (Full CRUD) | Read & Write (Full CRUD) |
| **Teacher** | Access Denied (403) | Access Denied (403) | Read & Write (Full CRUD) |
| **Student** | Access Denied (403) | Access Denied (403) | Access Denied (403) |

- **Admins** can manage all three resources.
- **Teachers** can manage only students.
- **Students** cannot manage any resource.

---

## Default Credentials

The application initializes the following accounts automatically on startup:

| Role | Username | Password | Email | Details |
| :--- | :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | `admin@university.edu` | Department: Administration |
| **Teacher** | `teacher` | `teacher123` | `teacher@university.edu` | Department: Computer Science, Designation: Professor |
| **Student** | `student` | `student123` | `student@university.edu` | Student ID: CSE-2026-001, Semester: 4 |

---

## Project Structure

```
university-management-system/
|-- compose.yaml
|-- Dockerfile
|-- .dockerignore
|-- .gitignore
|-- pom.xml
|-- README.md
`-- src/main/
    |-- java/com/example/ums/
    |   |-- UmsApplication.java
    |   |-- config/
    |   |   |-- DataInitializer.java
    |   |   `-- SecurityConfig.java
    |   |-- controller/
    |   |   |-- AdminController.java
    |   |   |-- StudentController.java
    |   |   `-- TeacherController.java
    |   |-- model/
    |   |   |-- Admin.java
    |   |   |-- BaseUser.java
    |   |   |-- Student.java
    |   |   `-- Teacher.java
    |   |-- repository/
    |   |   |-- AdminRepository.java
    |   |   |-- StudentRepository.java
    |   |   `-- TeacherRepository.java
    |   `-- security/
    |       `-- CustomUserDetailsService.java
    `-- resources/
        `-- application.yml
```

---

## How It Works (Explanation for Evaluation)

1. **Model Layer (`BaseUser`, `Admin`, `Teacher`, `Student`)**:
   - `BaseUser` is an abstract `@MappedSuperclass` containing shared user credentials and attributes (`id`, `username`, `password`, `role`, `name`, `email`).
   - The `@JsonProperty(access = Access.WRITE_ONLY)` annotation ensures hashed passwords are never returned in JSON responses.
   - `Admin`, `Teacher`, and `Student` inherit from `BaseUser` and define their role-specific fields.

2. **Security & Authentication Layer (`SecurityConfig`, `CustomUserDetailsService`)**:
   - `CustomUserDetailsService` queries the repositories to load the user and their granted authority (`ROLE_ADMIN`, `ROLE_TEACHER`, or `ROLE_STUDENT`).
   - `SecurityConfig` defines route-level authorization matching the project requirements:
     - `/api/admins/**` requires `ROLE_ADMIN`
     - `/api/teachers/**` requires `ROLE_ADMIN`
     - `/api/students/**` requires `ROLE_ADMIN` or `ROLE_TEACHER`
   - Passwords are encrypted using `BCryptPasswordEncoder`.
   - Supports both HTTP Basic authentication (for API testing) and Spring Security Form Login/Logout.

3. **Controller Layer (`AdminController`, `TeacherController`, `StudentController`)**:
   - Provides REST endpoints with standard HTTP status codes:
     - `200 OK`: Successful retrieval or update
     - `201 Created`: Successful creation
     - `204 No Content`: Successful deletion
     - `401 Unauthorized`: Missing or invalid credentials
     - `403 Forbidden`: Insufficient permissions for user's role
     - `404 Not Found`: Resource does not exist

4. **Data Initialization (`DataInitializer`)**:
   - Implements `CommandLineRunner` to seed one Admin, one Teacher, and one Student if tables are empty.

---

## How to Run

### Option 1: Run with Docker Compose (Recommended)

Make sure Docker and Docker Compose are installed and running on your system:

```bash
docker compose up --build
```

To stop the containers:

```bash
docker compose down
```

### Option 2: Run Locally (Standalone)

Run with Maven:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw spring-boot:run
```

---

## REST API Endpoints

### 1. Student Endpoints (Admin, Teacher)

| Method | Endpoint | Description | Permitted Roles | Status Code |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/students` | Get all students | Admin, Teacher | 200 OK |
| `GET` | `/api/students/{id}` | Get student by ID | Admin, Teacher | 200 OK / 404 |
| `POST` | `/api/students` | Create new student | Admin, Teacher | 201 Created |
| `PUT` | `/api/students/{id}` | Update student by ID | Admin, Teacher | 200 OK / 404 |
| `DELETE` | `/api/students/{id}` | Delete student by ID | Admin, Teacher | 204 No Content |

#### Create Student Payload (`POST /api/students`):
```json
{
  "username": "student2",
  "password": "password123",
  "name": "Jane Doe",
  "email": "jane@university.edu",
  "studentId": "CSE-2026-002",
  "department": "Computer Science",
  "semester": 2
}
```

---

### 2. Teacher Endpoints (Admin Only)

| Method | Endpoint | Description | Permitted Roles | Status Code |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/teachers` | Get all teachers | Admin | 200 OK |
| `GET` | `/api/teachers/{id}` | Get teacher by ID | Admin | 200 OK / 404 |
| `POST` | `/api/teachers` | Create new teacher | Admin | 201 Created |
| `PUT` | `/api/teachers/{id}` | Update teacher by ID | Admin | 200 OK / 404 |
| `DELETE` | `/api/teachers/{id}` | Delete teacher by ID | Admin | 204 No Content |

#### Create Teacher Payload (`POST /api/teachers`):
```json
{
  "username": "teacher2",
  "password": "password123",
  "name": "Grace Hopper",
  "email": "grace@university.edu",
  "department": "Computer Science",
  "designation": "Associate Professor"
}
```

---

### 3. Admin Endpoints (Admin Only)

| Method | Endpoint | Description | Permitted Roles | Status Code |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/admins` | Get all admins | Admin | 200 OK |
| `GET` | `/api/admins/{id}` | Get admin by ID | Admin | 200 OK / 404 |
| `POST` | `/api/admins` | Create new admin | Admin | 201 Created |
| `PUT` | `/api/admins/{id}` | Update admin by ID | Admin | 200 OK / 404 |
| `DELETE` | `/api/admins/{id}` | Delete admin by ID | Admin | 204 No Content |

#### Create Admin Payload (`POST /api/admins`):
```json
{
  "username": "admin2",
  "password": "password123",
  "name": "Second Admin",
  "email": "admin2@university.edu",
  "department": "IT Support"
}
```

---

## Testing RBAC with cURL

1. **Admin access (Allowed for all resources):**
```bash
curl -u admin:admin123 http://localhost:8080/api/students
curl -u admin:admin123 http://localhost:8080/api/teachers
curl -u admin:admin123 http://localhost:8080/api/admins
```

2. **Teacher access (Allowed for students, Forbidden for teachers/admins):**
```bash
# Returns 200 OK
curl -u teacher:teacher123 http://localhost:8080/api/students

# Returns 403 Forbidden
curl -u teacher:teacher123 http://localhost:8080/api/teachers
curl -u teacher:teacher123 http://localhost:8080/api/admins
```

3. **Student access (Forbidden for all management endpoints):**
```bash
# Returns 403 Forbidden
curl -u student:student123 http://localhost:8080/api/students
curl -u student:student123 http://localhost:8080/api/teachers
curl -u student:student123 http://localhost:8080/api/admins
```

---

## Running Automated Tests

Run the included JUnit integration tests:

```powershell
.\mvnw test
```
