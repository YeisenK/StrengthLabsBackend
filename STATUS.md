# Estado del proyecto — StrengthLabsBackend

**Última actualización**: 2026-05-10

---

## Arrancar en 5 comandos

```bash
# 1. Infraestructura
docker compose -f docker/docker-compose.yml up postgres redis -d

# 2. Backend Java (dev)
DB_PASSWORD=changeme ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Swagger UI (cuando el backend esté arriba)
# http://localhost:8000/swagger-ui/index.html

# 4. Compute engine (opcional, necesario para /fatigue/summary)
cd recursos
uvicorn api.main:app --host 0.0.0.0 --port 8001 --reload

# 5. Parar todo
pkill -f spring-boot:run
docker compose -f docker/docker-compose.yml down
```

---

## Qué está implementado y funcionando

| Componente | Estado | Notas |
|---|---|---|
| Auth (register/login/refresh/me) | ✅ | JWT RS256, BCrypt |
| Auth (Google Sign-In) | ✅ | Verifica id_token con `GoogleIdTokenVerifier` |
| Workout CRUD | ✅ | Jerarquía workout → exercises → sets |
| Exercise catálogo | ✅ | 62 ejercicios globales seeded + custom por usuario |
| Routines | ✅ | 5 rutinas (hardcoded en controller, sin BD aún) |
| Fatigue /summary | ✅ shape | 19 campos correctos; valores reales si compute engine arriba |
| Fatigue /weekly | ✅ | Volumen semanal por muscle group |
| Export CSV/XLSX | ✅ | Apache POI, una fila por set |
| Admin endpoints | ✅ | List/detail/deactivate users (RBAC ADMIN) |
| RBAC (USER/TRAINER/ADMIN) | ✅ | Enforced en SecurityConfig |
| JWKS endpoint | ✅ | `/auth/.well-known/jwks.json` |
| GlobalExceptionHandler | ✅ | Oculta detalles internos al cliente |
| Compute engine Python | ✅ | `/compute/fatigue`, `/compute/risk`, `/compute/plan` |

---

## Qué falta (resumen de fases)

| Fase | Descripción | Prioridad |
|---|---|---|
| **1** | Fix `user_id=1` hardcoded en FatigueController:134 | 🔴 Alta |
| **1** | Loguear + devolver 503 cuando compute engine cae | 🔴 Alta |
| **2** | Extraer `PythonComputeAdapter` de FatigueController | 🟠 Alta |
| **2** | Crear `GetFatigueSummaryUseCase` (Clean Arch) | 🟠 Alta |
| **3** | Flyway: `V1__initial_schema.sql` + borrar `schema.sql` huérfano | 🔴 Alta |
| **3** | Rutinas en BD (tablas `routines/routine_days/routine_exercises`) | 🟠 Alta |
| **3** | Cache de métricas en `training_metrics` (upsert por día) | 🟠 Alta |
| **4** | Tests integración con Testcontainers (controllers + auth) | 🟠 Alta |
| **5** | Rate limiting login con Redis (Bucket4j o manual) | 🔴 Alta |
| **5** | Logout con blacklist de refresh tokens en Redis | 🔴 Alta |
| **5** | CORS restrictivo (hoy `allowedOrigins("*")`) | 🟠 Alta |
| **7** | Spring Boot Actuator + Prometheus + GitHub Actions CI | 🟡 Media |

Ver detalles en [`~/StrengthLabs/documentacion/FASES_IMPLEMENTACION.md`](../StrengthLabs/documentacion/FASES_IMPLEMENTACION.md).

---

## Servicios y puertos

| Servicio | Puerto | Cómo arranca |
|---|---|---|
| Backend Java (Spring Boot) | 8000 | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` |
| Compute engine (FastAPI) | 8001 | `uvicorn api.main:app --port 8001 --reload` (en `recursos/`) |
| PostgreSQL 16 | 5432 | `docker compose up postgres -d` |
| Redis 7 | 6379 | `docker compose up redis -d` |

---

## Variables de entorno relevantes

| Variable | Descripción | Dev default |
|---|---|---|
| `DB_PASSWORD` | Password de PostgreSQL | `changeme` |
| `JWT_PRIVATE_KEY` | Clave privada RS256 (Base64 PKCS8) | Hardcoded en `application.yml` |
| `JWT_PUBLIC_KEY` | Clave pública RS256 (Base64 X.509) | Hardcoded en `application.yml` |
| `GOOGLE_CLIENT_ID` | Web Client ID de Google OAuth | `REPLACE_WITH_REAL_GOOGLE_CLIENT_ID` |

> ⚠️ Las claves JWT hardcodeadas en `application.yml` son SOLO para dev. En prod usar variables de entorno reales.

---

## Deuda técnica conocida

- `schema.sql` en raíz del repo: obsoleto (modelo plano viejo). Migrar a Flyway en Fase 3 y borrar.
- `training_sessions` tabla en `schema.sql`: modelo plano viejo, no se usa. Eliminar con Flyway.
- `training_metrics` tabla en `schema.sql`: existe pero ningún código la escribe/lee aún (pendiente Fase 3).
- `FatigueController` accede directamente a `WorkoutJpaRepository` (violación Clean Arch). Corregir en Fase 2.
- `RoutineController` tiene 5 rutinas hardcoded inline. Mover a BD en Fase 3.

---

## Documentación API

Springdoc auto-genera la spec desde las anotaciones:
- Swagger UI: `http://localhost:8000/swagger-ui/index.html`
- YAML: `http://localhost:8000/v3/api-docs.yaml`
- JSON: `http://localhost:8000/v3/api-docs`

Para actualizar `docs/openapi.yml`:
```bash
curl http://localhost:8000/v3/api-docs.yaml > docs/openapi.yml
```
