# DevOps Foundation: Dars.ma
**Architecture**: docs/architecture-dars-ma.md
**Security**: docs/security-dars-ma.md
**Version**: 1.0 | **Date**: 2026-07-04 | **Author**: DevOps/DevSecOps

## 1. Environment Strategy
| Environment | Purpose | Deploy Trigger |
|---|---|---|
| local | Development | `docker compose up`, manual |
| staging | QA / Preview | Auto on merge to `main` |
| production | Live users | Manual tag + approval |

## 2. CI Pipeline (GitHub Actions)
```yaml
name: ci
on: [push, pull_request]
permissions:
  contents: read
jobs:
  backend-lint-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: pgvector/pgvector:pg16
        env: { POSTGRES_DB: dars_ma_test, POSTGRES_USER: test, POSTGRES_PASSWORD: test }
        ports: ["5432:5432"]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '25', distribution: 'temurin' }
      - name: Build & test (Maven, coverage via JaCoCo)
        run: ./mvnw -B verify
      - name: Coverage gate (fail if < 80%)
        run: ./mvnw jacoco:check

  frontend-lint-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22' }
      - run: npm ci
      - run: npm run lint
      - run: npm test -- --code-coverage --watch=false

  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: returntocorp/semgrep-action@v1
        with: { config: p/owasp-top-ten }
      - uses: aquasecurity/trivy-action@master
        with: { scan-type: fs, severity: CRITICAL,HIGH, exit-code: 1 }
      - uses: gitleaks/gitleaks-action@v2

  build:
    needs: [backend-lint-test, frontend-lint-test, security]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker images
        run: docker compose -f docker-compose.yml build
```

## 3. Infrastructure
- **Hosting**: Single VPS (or equivalent), Docker Compose only — Kubernetes explicitly deferred (System Design SDR-2), revisit only if multi-host scaling becomes a real need
- **Compute**: 3 containers — `backend` (Spring Boot), `frontend` (Angular build served via Nginx), `db` (Postgres+pgvector)
- **Database**: Self-hosted in Docker, volume-mounted, nightly `pg_dump` to external storage
- **Secrets**: Environment variables injected via `.env` (gitignored) locally, and via the hosting platform's secret store in staging/production — never committed
- **Monitoring**: Container stdout logs via Docker's log driver at launch; revisit Prometheus/Grafana only if incident history justifies it (YAGNI)

## 4. Security Scanning Gates
| Scanner | Scan Type | Fail Threshold |
|---|---|---|
| Semgrep | SAST — code vulnerabilities (OWASP Top 10 ruleset) | Critical findings |
| Trivy | SCA — dependency CVEs (Maven + npm) | Critical CVEs |
| Gitleaks | Secrets detection | Any secrets found |
| Trivy (image scan) | Container image CVEs | Critical CVEs |

## 5. Docker Setup

### docker-compose.yml (dev)
```yaml
services:
  backend:
    build: ./backend
    env_file: .env
    ports: ["8080:8080"]
    depends_on:
      db: { condition: service_healthy }
  frontend:
    build: ./frontend
    ports: ["4200:80"]
    depends_on: [backend]
  db:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes: ["pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER}"]
      interval: 5s
volumes:
  pgdata:
```

### Backend Dockerfile (Spring Boot, multi-stage)
```dockerfile
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder --chown=app:app /app/target/*.jar app.jar
USER app
HEALTHCHECK --interval=30s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Frontend Dockerfile (Angular, multi-stage)
```dockerfile
FROM node:22-slim AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

FROM nginx:alpine
COPY --from=builder /app/dist/dars-ma-frontend/browser /usr/share/nginx/html
EXPOSE 80
```

## 6. Monitoring Baseline
| Signal | Tool | Alert Threshold |
|---|---|---|
| Logs | Docker log driver → stdout | Error rate spike (manual review at launch scale) |
| Health | Spring Actuator `/actuator/health` | Container restart on failed healthcheck |
| Uptime | Manual/simple uptime check (e.g., UptimeRobot) | Downtime alert |

## 7. Environment Variables (collected during UNDERSTAND/DevOps phase)

| Variable | Purpose | Dev default (this session) |
|---|---|---|
| `DB_NAME` | Postgres database name | `dars_ma` |
| `DB_USER` | Postgres user | `dars_ma_app` |
| `DB_PASSWORD` | Postgres password | generated, local dev only — see `.env` (gitignored) |
| `JWT_SIGNING_KEY` | RS256 key for JWT auth | generated dev keypair, rotated for staging/prod |
| `JWT_ACCESS_TTL_MIN` | Access token lifetime | `15` |
| `JWT_REFRESH_TTL_DAYS` | Refresh token lifetime | `7` |
| `CMI_MODE` | `mock` or `live` — selects `EscrowPaymentProvider` impl | `mock` (no CMI sandbox creds yet — see PRD risk) |
| `CMI_MERCHANT_ID` | CMI merchant identifier | unset in dev (mock mode ignores it) |
| `CMI_API_KEY` | CMI API key | unset in dev (mock mode ignores it) |
| `SMTP_HOST` / `SMTP_PORT` | Email delivery | dev: local Mailhog container (not yet added — add if notification story needs real send-testing) |
| `SPRING_PROFILES_ACTIVE` | Spring profile selector | `dev` |
| `ANGULAR_API_BASE_URL` | Frontend → backend API base | `http://localhost:8080` |

📝 Log: ENV_VARS_COLLECTED → .logs/activity.md
