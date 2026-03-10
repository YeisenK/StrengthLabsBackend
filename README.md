# StrengthLabs Backend

Backend of the **Adaptive Strength Intelligence Platform** — a training analytics system that computes fatigue accumulation, injury risk, and periodization recommendations based on workout session data.

---

## Architecture

The system follows **Clean Architecture** with strict layer separation. Dependencies point inward only (Dependency Rule): no internal layer depends on external ones.

| Layer | Components | Responsibility |
|---|---|---|
| Domain | `User`, `WorkoutSession`, `TrainingPlan`, `FatigueScore`, `RiskLevel` | Pure business logic. No external dependencies. Defines repository interfaces (ports). |
| Application | `RegisterSessionUseCase`, `CalculateFatigueUseCase`, `CalculateRiskUseCase`, `GeneratePlanUseCase` | Orchestrates domain services. Framework-independent. |
| Infrastructure | JPA adapters, `PythonComputeAdapter`, `RedisMetricsCache`, JWT/RBAC | Concrete implementations of domain ports. |
| Presentation | REST controllers, `GlobalExceptionHandler`, OpenAPI config | HTTP entry point: validation, auth, response serialization. |
| Compute Engine | Python/FastAPI (`/compute/fatigue`, `/compute/risk`, `/compute/plan`) | Mathematical models for fatigue and recovery curves. Never touches the database. |

---

## Tech Stack

| Area | Technology |
|---|---|
| API | Spring Boot 3 · Java 21 |
| Compute Engine | Python 3.12 · FastAPI |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Auth | JWT RS256 + OAuth 2.0 |
| Containers | Docker · Docker Compose |
| CI/CD | GitHub Actions |
| Observability | Prometheus + Grafana |

---

## Project Structure

```
api/
├── src/main/java/com/strengthlabs/
│   ├── domain/
│   │   ├── entities/         # User, WorkoutSession, TrainingPlan
│   │   ├── valueobjects/     # FatigueScore, RiskLevel
│   │   ├── repositories/     # Port interfaces (no JPA)
│   │   └── services/         # PeriodizationService
│   ├── application/
│   │   ├── usecases/         # One class per use case
│   │   ├── dtos/             # Input/output transfer objects
│   │   └── ports/            # ComputeEnginePort
│   ├── infrastructure/
│   │   ├── persistence/      # JPA entities + repository adapters
│   │   ├── compute/          # PythonComputeAdapter (HTTP → FastAPI)
│   │   ├── cache/            # RedisMetricsCache
│   │   └── security/         # JwtTokenProvider, RbacFilter
│   └── presentation/
│       ├── controllers/      # AuthController, SessionController, MetricsController, AdminController
│       ├── middleware/        # GlobalExceptionHandler
│       └── config/           # SecurityConfig, OpenApiConfig
├── compute-engine/
│   ├── domain/               # fatigue_model.py, risk_model.py, periodization_model.py
│   ├── api/
│   │   ├── main.py
│   │   ├── routers/          # fatigue.py, risk.py, plan.py
│   │   └── schemas/          # Pydantic input/output schemas
│   └── tests/
├── docker/
│   ├── Dockerfile.api        # Spring Boot multi-stage (Alpine runtime)
│   ├── Dockerfile.compute    # Python Alpine
│   └── docker-compose.yml
└── docs/
    └── openapi.yml
```

---

## API Endpoints

All endpoints are versioned under `/api/v1/`. A centralized `GlobalExceptionHandler` ensures stack traces, SQL queries, and infrastructure details are never exposed to the client.

| Endpoint | Method | Role Required |
|---|---|---|
| `/api/v1/auth/login` | POST | Public |
| `/api/v1/sessions` | GET / POST | USER, TRAINER |
| `/api/v1/metrics/fatigue` | GET | USER, TRAINER |
| `/api/v1/metrics/risk` | GET | USER, TRAINER |
| `/api/v1/plans/generate` | POST | USER, TRAINER |
| `/api/v1/dashboard` | GET | USER, TRAINER |
| `/api/v1/admin/users` | GET / DELETE | ADMIN |

---

## Roles

| Role | Permissions | Restrictions |
|---|---|---|
| User | Register own sessions. View own metrics and risk index. View active plan. | Only accesses own data. |
| Trainer | Everything a User can do. View assigned athletes' metrics. Generate/modify plans. | Only manages assigned athletes. |
| Admin | User management. Global config. Audit log access. | All actions are audited. |

---

## Security

- JWT RS256 + OAuth 2.0
- RBAC enforced at endpoint and data level
- TLS 1.3 in transit · AES-256 at rest for sensitive data
- Rate limiting on login: 5 attempts / 15 min
- OWASP Top 10 mitigations: ORM parameterized queries, CSP/HSTS headers, SameSite=Strict cookies, Dependabot + OWASP Dependency-Check in CI pipeline

---

## Local Development

### Prerequisites

- Java 21
- Docker + Docker Compose
- Python 3.12
- Maven

### Setup

```bash
# Clone the repo
git clone https://github.com/YeisenK/StrengthLabsBackend.git
cd StrengthLabsBackend

# Create environment file
cp .env.example .env
# Edit .env with your actual values

# Start PostgreSQL and Redis
docker compose -f docker/docker-compose.yml up postgres redis -d

# Run Spring Boot API
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run Python compute engine
cd compute-engine
pip install -r requirements.txt
uvicorn api.main:app --host 0.0.0.0 --port 8001 --reload
```

### Environment Variables

| Variable | Description |
|---|---|
| `DB_PASSWORD` | PostgreSQL password |

---

## Data Flow

```
Flutter client
    → Spring Boot (validates, authenticates, persists raw session)
        → FastAPI compute engine (calculates fatigue, risk, plan)
    → Spring Boot (stores computed metrics, returns response)
→ Flutter client
```

Spring Boot never runs mathematical models. FastAPI never touches the database.

---

## Author

**Yeisen Kenneth López Reyes** · IT Engineering · Universidad Anáhuac  
[linkedin.com/in/yeisen-kenneth-lópez-reyes-a423b633b](https://linkedin.com/in/yeisen-kenneth-lópez-reyes-a423b633b) · [github.com/YeisenK](https://github.com/YeisenK)