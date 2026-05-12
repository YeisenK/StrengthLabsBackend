# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

---

## Project overview

**Strength Labs** is a fitness training tracker delivered as an academic / portfolio project. The repo is a monorepo containing:

| Folder | What it is |
|---|---|
| `strengthlabs_beta/` | Flutter mobile app (iOS + Android). Offline-first writes, Drift local DB, sync queue against the backend. |
| `StrengthLabsBackend/` | Java 21 + Spring Boot 4 API. PostgreSQL via Flyway migrations, JWT (RS256), per-request correlation IDs. |
| `StrengthLabsBackend/recursos/` | Independent Python 3 + FastAPI microservice that computes fatigue metrics (ATL/CTL/TSB/ACWR, risk model). Stateless. The Spring backend calls it over HTTP at `localhost:8001`. |
| `docs/` | LaTeX architecture document and supporting diagrams. |

> Earlier versions of this file claimed the backend was Python/FastAPI — that was wrong. The main API is Java/Spring; the only Python service is the small fatigue compute engine.

---

## Frontend — `strengthlabs_beta/`

### Stack

| Layer | Choice |
|---|---|
| Framework | Flutter 3.x |
| State management | `flutter_bloc` (Cubit) |
| Navigation | `go_router` |
| HTTP | `dio` with custom interceptors (auth refresh, correlation id, retry/backoff) |
| Local DB | `drift` (SQLite) |
| Local KV | `flutter_secure_storage` (JWT), `shared_preferences` (settings, active workout draft) |
| Connectivity | `connectivity_plus` |
| Identity | `uuid` (idempotency keys, local row ids) |
| Auth | Email/password + Google Sign-In |

### Layout

```
lib/
├── app.dart
├── main.dart
├── core/
│   ├── connectivity/    # ConnectivityCubit — surfaces online/offline status
│   ├── constants/       # API base URLs, etc.
│   ├── database/        # Drift schema + generated code (app_database.dart)
│   ├── demo/            # Demo-mode bypass for the example@example.com login
│   ├── network/         # Dio client + interceptors
│   ├── router/          # GoRouter routes + auth guards
│   ├── storage/         # Secure token storage
│   └── sync/            # SyncManager — drains the offline write queue
├── features/
│   ├── auth/            # data/{auth_repository, user_cache, session_restore_result}, presentation/cubit
│   ├── workouts/        # data/{workout_repository, offline_workout_service, active_workout_draft_store}
│   ├── fatigue/         # data/{fatigue_repository, fatigue_response_parser}
│   ├── routines/
│   ├── plan/
│   ├── export/          # Local xlsx/csv generation (no server round-trip)
│   ├── settings/
│   └── onboarding/
├── shared/
│   ├── widgets/         # OfflineBanner, common UI
│   └── utils/           # Validators, formatters
└── l10n/                # ARB-driven translations
```

### Offline-first model

The app treats the local Drift database as the source of truth and the backend as a write-behind:

1. **Mutations** (`OfflineWorkoutService.createWorkout`, `deleteWorkout`) write to `workouts` table and enqueue a row in `sync_queue`. They return immediately.
2. **Sync** — `SyncManager.processPending()` drains the queue FIFO when connectivity returns or the app resumes. Each operation carries a `client_request_id` so retries are idempotent on the backend.
3. **Reads** stream from Drift (`watchWorkouts`). The UI updates reactively as syncs resolve.
4. **Active workout state** — `ActiveWorkoutCubit` serialises every emit to `shared_preferences` via `ActiveWorkoutDraftStore`. If the OS kills the app mid-session, the next launch offers recovery.
5. **Auth** — `restoreSession()` tolerates a flaky network: token + cached user → still authenticated, no bounce to login. Only 401 forces logout.

### Network behaviour

- `DioClient` chains three interceptors:
  - `CorrelationIdInterceptor` — adds `X-Request-Id` (UUID v4) to every request; the backend echoes it back.
  - `_AuthInterceptor` — proactively refreshes the access token before requests using `flutter_secure_storage`, retries once on 401.
  - `RetryInterceptor` — retries idempotent **reads only** (`GET`) on transient failures (connection error, 502/503/504) with exponential backoff + jitter. Mutations are never retried at this layer — they go via `SyncQueue` so the `client_request_id` is preserved across attempts.

### Commands

```bash
cd strengthlabs_beta
flutter pub get
flutter run                       # device or emulator
flutter test                      # ALWAYS pass --concurrency=1 — Drift tests
flutter test --concurrency=1      # share an in-memory SQLite under parallel isolates
dart analyze lib/ test/           # static analysis
dart run build_runner build       # regenerate Drift code after schema edits
flutter gen-l10n                  # regenerate localizations from app_*.arb
flutter build apk                 # Android
flutter build ios                 # iOS
```

### Conventions

- Feature-first layout under `features/`. Each feature has `data/`, `domain/`, `presentation/` where applicable.
- State management is **Bloc/Cubit only**. `setState` is reserved for purely local UI toggles inside a single widget (password visibility, etc.).
- All API calls go through `core/network/dio_client.dart`. The interceptor chain handles JWT, correlation ids, and read retries.
- The access token lives in `flutter_secure_storage`. Never put it in `shared_preferences`.
- Navigation is declarative via GoRouter; route guards check authentication.
- Do not hardcode URLs — use `core/constants/`.
- All `Workout` JSON uses **snake_case** to match the backend (`duration_seconds`, `muscle_group`, etc.). `Workout.date` is always serialised as UTC ISO-8601 to avoid off-by-one bugs in fatigue weekly aggregates.

---

## Backend — `StrengthLabsBackend/`

### Stack

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Web | Spring MVC |
| Security | Spring Security + JJWT 0.12.6 (RS256 asymmetric) |
| Persistence | Spring Data JPA + Hibernate 7 |
| Database | PostgreSQL 16 |
| Migrations | Flyway (with the new `spring-boot-flyway` SB4 module) |
| Build | Maven (`mvnw`) |
| Tests | JUnit 5 + Testcontainers (Postgres) |

### Layout (hexagonal)

```
src/main/java/com/strengthlabs/
├── api/                 # Spring Boot application + config (security, i18n, RestTemplate timeouts)
├── application/
│   ├── dtos/            # Records moving between layers
│   ├── ports/           # Outbound interfaces (ComputeEnginePort)
│   └── usecases/        # GetFatigueSummaryUseCase, etc.
├── domain/              # Entities (User…) and pure services
├── infrastructure/
│   ├── compute/         # PythonComputeAdapter — RestTemplate with explicit timeouts
│   ├── persistence/     # JPA entities + repositories
│   ├── security/        # JWT provider, RBAC filter, rate limiter, revocation
│   └── web/             # CorrelationIdFilter
└── presentation/
    ├── controllers/     # AuthController, WorkoutController, FatigueController, ...
    └── middleware/      # GlobalExceptionHandler, LocalizedStatusException
```

### Database schema

Flyway migrations live in `src/main/resources/db/migration/`:

- `V1__initial_schema.sql` — users, exercises, workouts, workout_exercises, workout_sets, training_metrics
- `V2__routines.sql` — routine catalogue
- `V3__i18n_catalog.sql`, `V4__i18n_translations.sql` — Spanish columns
- `V5__client_request_id_and_version.sql` — idempotency + optimistic locking on workouts
- `V6__align_metric_column_types.sql` — converts NUMERIC fatigue columns to DOUBLE PRECISION so `@Column double` validates

### API endpoints

Same surface the mobile app consumes:

| Method | Path | Notes |
|---|---|---|
| POST | `/auth/register` | Returns `{access_token, refresh_token}`. |
| POST | `/auth/login` | Same shape. Rate-limited per IP. |
| POST | `/auth/refresh` | Rotates both tokens. |
| POST | `/auth/google` | Verifies a Google ID token, issues our pair. |
| GET  | `/auth/me` | Current authenticated user. |
| POST | `/auth/logout` | Revokes the refresh token JTI. |
| GET  | `/.well-known/jwks.json` | Public key for external verifiers. |
| GET  | `/workouts` | Either `{items}` (legacy) or `{items, page, size, total, has_more}` (when `?page=&size=` is supplied). |
| POST | `/workouts` | Accepts optional `client_request_id` (UUID). Same id → same row, no duplicates. |
| GET  | `/workouts/{id}` | |
| PUT  | `/workouts/{id}` | `If-Match: <version>` header enables optimistic concurrency. Returns 409 on stale version. |
| DELETE | `/workouts/{id}` | Hard delete with cascade. |
| GET  | `/exercises`, POST `/exercises`, GET `/exercises/{id}/last-set` | |
| GET  | `/fatigue/summary`, `/fatigue/weekly` | Calls the Python compute service, falls back to zero values if it's down. Daily result is cached in `training_metrics`. |
| GET  | `/routines`, `/routines/{id}` | Read-only catalogue. |

Every response carries the request's `X-Request-Id` (generated server-side if the client didn't supply one). Errors come back as `{"error": "..."}` with HTTP status reflecting the failure class.

### Key safety properties

- **Idempotent writes** — `client_request_id` + unique partial index `(user_id, client_request_id)` makes POST retries safe across timeouts.
- **Optimistic locking** — `@Version` on `WorkoutJpaEntity`; concurrent PUTs are rejected as 409 via `GlobalExceptionHandler.handleOptimisticLock`.
- **No race on fatigue cache** — `persistMetrics` does a recheck before insert and catches `DataIntegrityViolationException` so two concurrent computes for the same day collapse cleanly.
- **Bounded compute calls** — `RestTemplate` is built with `connectTimeout=2s`, `readTimeout=5s`. Compute-engine outages return zero fallback instead of hanging the request thread.
- **JWT key hygiene** — no signing keys in `application.yml`. `application-dev.yml` and `application-test.yml` carry throwaway keys for local work; `application-prod.yml` fails fast unless `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` are set as env vars.
- **N+1 mitigation** — `@EntityGraph(["exercises", "exercises.exercise"])` on the read queries; `WorkoutSetJpaEntity` is LAZY with `@BatchSize(25)` so sets fetch in bounded batches.
- **Correlation IDs** — `CorrelationIdFilter` reads or generates `X-Request-Id`, puts it in MDC, echoes it back, and removes it from MDC in `finally`.

### Commands

```bash
cd StrengthLabsBackend
# JDK 21 is required (default-jdk on Fedora may be newer).
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./mvnw test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./mvnw spring-boot:run

# Tests use Testcontainers — needs Docker daemon reachable.
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./mvnw test -Dtest=WorkoutControllerIT
```

### Compute engine (Python)

```bash
cd StrengthLabsBackend/recursos
pip install -r requirements.txt
uvicorn api.main:app --host 0.0.0.0 --port 8001
```

Endpoints: `POST /compute/fatigue`, `POST /compute/risk`. The backend treats it as best-effort; if it's unreachable, the API still answers with degraded fatigue data (zero-valued, `compute_available=false`).

---

## Local dev orchestration

```bash
# 1. Postgres + Redis
docker-compose -f StrengthLabsBackend/docker/docker-compose.yml up -d

# 2. Compute engine (separate terminal)
cd StrengthLabsBackend/recursos && uvicorn api.main:app --port 8001

# 3. Backend
cd StrengthLabsBackend && JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./mvnw spring-boot:run

# 4. Flutter app
cd strengthlabs_beta && flutter run
```

---

## Things to keep in mind when editing

- **Migrations are append-only.** Never edit a `V*` file that has already been applied somewhere. Add a new `V<N+1>` migration.
- **Drift schema changes need codegen.** After editing `app_database.dart`, run `dart run build_runner build`. The generated `.g.dart` is committed.
- **Flutter tests must run serially.** The in-memory SQLite shared by Drift across parallel test isolates produces flaky failures. Run `flutter test --concurrency=1` in CI.
- **Mutations on the client go through `OfflineWorkoutService`**, not `WorkoutRepository` directly, so they hit the local DB + sync queue.
- **Don't add a JWT key fallback to `application.yml`.** Keys belong in profile-specific files (dev/test) or env vars (prod).
- **Idempotency keys are sacred.** When `SyncManager` retries a `createWorkout`, it must replay the same `client_request_id`. Never regenerate it.
