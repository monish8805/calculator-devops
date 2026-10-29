# Calculator DevOps

A small plain Java (no framework) calculator REST API wrapped in a CI/CD pipeline:
Git/GitHub → Jenkins (build, test, Docker build, push) → Docker Compose deploy (API replicas behind Nginx).

## API

| Operation | Endpoint | Example |
|-----------|----------|---------|
| Add | `GET /api/v1/add?a=&b=` | `/api/v1/add?a=2&b=3` → `{"operation":"add","a":2.0,"b":3.0,"result":5.0}` |
| Subtract | `GET /api/v1/subtract?a=&b=` | `/api/v1/subtract?a=10&b=3` |
| Multiply | `GET /api/v1/multiply?a=&b=` | `/api/v1/multiply?a=3&b=4` |
| Divide | `GET /api/v1/divide?a=&b=` | `/api/v1/divide?a=4&b=0` → HTTP 400 |
| Health | `GET /health` | `{"status":"UP"}` |

Errors return a structured body, e.g.:

```json
{ "status": 400, "error": "DIVISION_BY_ZERO", "message": "Division by zero is not allowed" }
```

Error codes: `DIVISION_BY_ZERO`, `MISSING_PARAMETER`, `INVALID_PARAMETER`.

## Build and test

Requires JDK 17+ and Maven 3.9+.

```bash
mvn clean package     # build the runnable JAR into target/
mvn test              # run JUnit 5 tests; reports in target/surefire-reports/
java -jar target/calculator-devops-1.0.0.jar
```

## Run with Docker

```bash
docker build -t calculator-api .
docker run -p 8080:8080 calculator-api
curl "http://localhost:8080/api/v1/add?a=2&b=3"
```

## Run the full stack with Docker Compose

```bash
docker compose up -d --build
curl "http://localhost:8081/api/v1/multiply?a=6&b=7"   # through Nginx
docker compose ps                                       # both replicas should show (healthy)
docker compose down
```

Architecture: `proxy` (Nginx, host port 8081) → `calculator` (2 replicas, internal port 8080), on the `calc-net` network. Compose health checks use `/health`.

## CI/CD pipeline (Jenkinsfile)

Stages: **Checkout → Build → Test → Docker Build → Push → Deploy → Verify**, with email notification on success and failure.

### Jenkins setup

1. Install plugins: *Pipeline*, *Git*, *JUnit*, *Mailer*, *Credentials Binding*.
2. Make sure the Jenkins agent has Docker and the Docker Compose v2 plugin (`docker compose version`) available, and that the Jenkins user can run Docker.
3. Add a credential in **Manage Jenkins → Credentials**:
   - Kind: *Username with password*
   - Username: your Docker Hub username
   - Password: a Docker Hub access token
   - ID: `dockerhub-creds`
4. Configure SMTP under **Manage Jenkins → System → Extended/Standard E-mail Notification**.
5. Edit `Jenkinsfile`: set `DOCKERHUB_USER` and `NOTIFY_EMAIL`.
6. Create a Pipeline job pointing at this repository (Pipeline script from SCM, `Jenkinsfile`).

Credentials are never stored in the repository; Docker Hub login uses the Jenkins credentials store.

## Git workflow

Work happens on feature branches merged through pull requests into `main`. Do not commit `target/` or secrets (see `.gitignore`).
