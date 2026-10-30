# Docker Guide & Complete Explanation

This guide explains how Docker works, how to check if Docker is installed and running on your system, and breaks down the `Dockerfile` and `compose.yaml` used in this **University Management System** project.

---

## 1. How to Check If Docker Is Working Or Not

You can check whether Docker is installed and running using either the command line or the Docker Desktop application.

### Step 1: Open PowerShell or Command Prompt
Run the following command:

```bash
docker --version
```

- **If Docker is installed:** It will print the installed version, for example:
  ```text
  Docker version 26.0.0, build 2ae903e
  ```
- **If Docker is NOT installed:** You will see an error:
  ```text
  docker: The term 'docker' is not recognized as a name of a cmdlet...
  ```
  *(If you see this, download and install [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/)).*

---

### Step 2: Check If the Docker Daemon/Engine Is Actually Running
Even if Docker is installed, the background engine (daemon) must be running. Check it with:

```bash
docker info
```

- **If running:** It outputs system architecture, number of running containers, storage driver, and memory limits.
- **If not running:** You will receive a connection error like:
  ```text
  Cannot connect to the Docker daemon at npipe:////./pipe/docker_engine. Is the docker daemon running?
  ```
  **Fix:** Search for **Docker Desktop** in Windows Start Menu and launch it. Wait until the whale icon in your system tray turns solid (or says "Engine running").

---

### Step 3: Run a Hello-World Test Container
The quickest functional test to verify Docker works from end to end:

```bash
docker run --rm hello-world
```

If you see:
```text
Hello from Docker!
This message shows that your installation appears to be working correctly.
```
Then Docker is 100% operational on your machine.

---

## 2. Docker Architecture in This Project

In this project, Docker containerizes two services together:
1. **`db`**: A PostgreSQL 16 database container with persistent disk storage.
2. **`app`**: The Spring Boot Java application container built using a multi-stage Dockerfile.

```text
+-------------------------------------------------------------+
|                     Docker Compose Network                  |
|                                                             |
|   +---------------------+         +---------------------+   |
|   |       ums_app       |         |       ums_db        |   |
|   |  (Spring Boot App)  | ------> |     (PostgreSQL)    |   |
|   |     Port: 8080      |         |     Port: 5432      |   |
|   +---------------------+         +---------------------+   |
|              |                               |              |
+--------------|-------------------------------|--------------+
               v                               v
       Host Port 8080                  Persistent Volume
   (Browser / Dashboard)                   (pgdata)
```

---

## 3. Line-by-Line Explanation of `Dockerfile`

The project uses a **Multi-Stage Build** to create a secure, minimal container image.

```dockerfile
# ==========================================
# STAGE 1: Build Stage (Full Maven + JDK)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Copy POM and download dependencies in advance (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copy the source code and compile fat JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Runtime Stage (Lightweight JRE)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 3. Copy only the compiled JAR from Stage 1
COPY --from=build /app/target/*.jar app.jar

# 4. Expose Spring Boot's default port and define startup command
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Why Multi-Stage Build?
- **Stage 1 (Build)** contains Maven, compilers, source code, and cached dependencies. This image is large (~800MB+).
- **Stage 2 (Runtime)** uses Alpine Linux with only the Java 21 Runtime Environment (`jre`). It only contains `app.jar`.
- **Result:** The final image is lightweight (~150MB-200MB), boots fast, and does not include source code or development tools, making it much more secure.

---

## 4. Line-by-Line Explanation of `compose.yaml`

Docker Compose allows you to run multiple containers with a single command without configuring network bridges manually.

```yaml
services:
  db:
    image: postgres:16-alpine           # Lightweight official PostgreSQL 16 image
    container_name: ums_db              # Container name
    restart: unless-stopped             # Auto-restarts if the container crashes
    environment:
      POSTGRES_DB: university_db        # Creates default database
      POSTGRES_USER: postgres           # Database master user
      POSTGRES_PASSWORD: postgres       # Database master password
    ports:
      - "5432:5432"                     # Maps host port 5432 to container port 5432
    volumes:
      - pgdata:/var/lib/postgresql/data # Persists database files even if container is deleted
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d university_db"]
      interval: 5s                      # Checks database readiness every 5 seconds
      timeout: 5s
      retries: 5

  app:
    build: .                            # Builds the image using the local Dockerfile
    container_name: ums_app
    restart: unless-stopped
    ports:
      - "8080:8080"                     # Maps host port 8080 to container port 8080
    environment:
      # Connects to 'db' using the container service name as hostname
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/university_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
    depends_on:
      db:
        # Ensures Spring Boot only starts AFTER PostgreSQL is completely healthy and ready
        condition: service_healthy

volumes:
  pgdata:                               # Named Docker volume for persistent storage
```

### Key Highlights:
1. **Service Discovery (`db:5432`)**: Inside the Docker network, containers talk to each other by their service name (`db`), not `localhost`.
2. **`condition: service_healthy`**: Avoids the common Spring Boot startup crash where the Java app starts before PostgreSQL finishes initializing.
3. **`volumes: pgdata`**: When you stop or destroy the containers, your database tables and user data remain safe inside the `pgdata` volume.

---

## 5. How to Run the Project with Docker

Once Docker Desktop is running on your machine:

### 1. Build and Start the Entire System
Run in PowerShell inside the project directory:

```bash
docker compose up -d --build
```

- `--build`: Recompiles the code and creates fresh images.
- `-d`: Detached mode (runs containers in the background).

---

### 2. Check Container Status
Verify that both containers are running and healthy:

```bash
docker compose ps
```

Expected output:
```text
NAME      IMAGE       COMMAND                  SERVICE   CREATED         STATUS                   PORTS
ums_app   ums-app     "java -jar app.jar"      app       2 minutes ago   Up 2 minutes             0.0.0.0:8080->8080/tcp
ums_db    postgres    "docker-entrypoint.s…"   db        2 minutes ago   Up 2 minutes (healthy)   0.0.0.0:5432->5432/tcp
```

---

### 3. View Live Logs
To see Spring Boot or PostgreSQL logs:

```bash
# View all logs
docker compose logs -f

# View only Spring Boot application logs
docker compose logs -f app
```

---

### 4. Access the Application
Open your browser and navigate to:
- **`http://localhost:8080/login.html`**

Log in with:
- `admin` / `admin123`
- `teacher` / `teacher123`
- `student` / `student123`

---

### 5. Stop the Containers
When you are finished:

```bash
# Stop containers (preserves database data)
docker compose down

# Stop containers AND delete stored data (clean reset)
docker compose down -v
```

---

## 6. Common Docker Commands Cheat Sheet

| Command | Description |
| :--- | :--- |
| `docker compose up -d` | Starts all services in the background |
| `docker compose down` | Stops and removes containers and networks |
| `docker compose ps` | Lists status of running services |
| `docker compose logs -f app` | Follows live logs of the Spring Boot application |
| `docker compose restart app` | Restarts only the Spring Boot container |
| `docker system prune -a` | Cleans up unused images and stopped containers to free disk space |
