# Server deployment runbook — Strength Labs Backend

This file is meant to be read by **Claude Code running on the production-ish server**. The goal: pull the latest changes, line up the new pieces (Flyway V5/V6, Spring Boot 4 Flyway module, `client_request_id` idempotency, optimistic locking, correlation IDs, RestTemplate timeouts), and verify end-to-end with a few HTTP calls.

The frontend repo (`YeisenK/StrengthLabs`) does not need to run on the server — it ships on phones. Only this backend repo plus the Python compute microservice (`recursos/`) belong here.

---

## 1. Pull and inspect

```bash
cd ~/StrengthLabsBackend       # or wherever the repo lives
git pull --ff-only origin main
git log --oneline -5            # last sanity check
```

If `git pull` complains about a divergent local state, **stop and ask** — do not force-pull, the server may have local config you don't want to overwrite.

---

## 2. What's already running (do not touch unless broken)

Run these and read the output before changing anything. **Two things vary
between dev boxes and the real server** — confirm before assuming the
defaults match what you actually have:

- **Listen port.** This runbook talks about `:8000` because that's the
  default in `application.yml`. The server may override it via `PORT=…`
  in its `.env` (e.g. `PORT=8080`). Look at the actual unit / `.env`
  first; the tests in §5 derive `BASE` from whatever port it picks.
- **JDK location.** The Fedora package is at
  `/usr/lib/jvm/java-21-openjdk`; Debian/Ubuntu installs it at
  `/usr/lib/jvm/java-21-openjdk-amd64`. If `java -version` already prints
  21, you don't need `JAVA_HOME` exported at all — leave it off the unit
  rather than hardcoding the wrong path.

```bash
# Postgres + Redis. May be running natively (systemd) rather than via
# the project's compose file — check both before deciding anything.
docker compose -f docker/docker-compose.yml ps 2>/dev/null \
  || systemctl is-active postgresql redis 2>/dev/null

# Java toolchain (must be 21)
java -version 2>&1 | head -1

# Discover the actual listen ports. Don't assume.
ss -tlnp | grep -E ':(8000|8080|8001) '

# .env values that influence behaviour
grep -E '^(PORT|JWT_|GOOGLE_|COMPUTE_ENGINE_URL|CORS_)' .env 2>/dev/null \
  | sed 's/=.*$/=<set>/'
```

Note what you find. If Postgres is already up and migrated against an older schema, the new V5/V6 migrations will apply on next backend start — they are additive (no destructive operations).

Required env vars (verify they're set in whatever `systemd`/`docker` unit runs the backend):

| Variable | What it is |
|---|---|
| `DB_PASSWORD` | Password for the `stlabs` Postgres role. |
| `JWT_PRIVATE_KEY` | RSA private key (PKCS8 base64). **No fallback** is present in `application.yml` anymore. Missing it = backend won't start. |
| `JWT_PUBLIC_KEY` | RSA public key (X.509 base64). Same — required. |
| `GOOGLE_CLIENT_ID` | OAuth client ID. |
| `COMPUTE_ENGINE_URL` | URL of the Python service (typically `http://localhost:8001`). |
| `CORS_ALLOWED_ORIGINS` | CSV of allowed origins. |

If `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` aren't set yet, generate a pair and set them in the environment (do **not** copy the dev keys from `application-dev.yml`):

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out priv.pem
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in priv.pem -out priv-pkcs8.pem
openssl rsa -in priv.pem -pubout -out pub.pem
# Base64-strip headers for env vars:
export JWT_PRIVATE_KEY=$(grep -v '^---' priv-pkcs8.pem | tr -d '\n')
export JWT_PUBLIC_KEY=$(grep -v '^---'  pub.pem        | tr -d '\n')
```

Store those wherever the backend's systemd unit / docker env file lives. Wipe `priv.pem` / `priv-pkcs8.pem` from disk afterwards.

---

## 3. New things this commit brings

Even before running anything, make sure you understand what changed:

- **Flyway V5** adds `client_request_id` (UUID, nullable) + `version` (BIGINT) columns to `workouts`, plus a partial unique index `(user_id, client_request_id) WHERE client_request_id IS NOT NULL`. Existing rows get `version=0` and `client_request_id=NULL` — backwards compatible.
- **Flyway V6** alters `training_metrics` and `workout_sets` numeric columns from `NUMERIC(p,s)` to `DOUBLE PRECISION` so Hibernate's `@Column double` validates the schema.
- **`spring-boot-flyway` dependency** added in `pom.xml`. SB4 moved Flyway autoconfig into its own module; without it, the previous build was silently skipping migrations.
- **Idempotent `POST /workouts`** — sending the same `client_request_id` twice returns the original row with `200 OK` instead of duplicating.
- **Optimistic locking on `PUT /workouts/{id}`** — clients send `If-Match: <version>`; server returns `409 Conflict` on stale version.
- **`GET /workouts?page=&size=`** — paginated variant returning `{items, page, size, total, has_more}`. The legacy unpaginated shape is preserved when no query params are sent.
- **`CorrelationIdFilter`** — every request gets an `X-Request-Id` echoed back in headers and stamped on every log line via MDC.
- **`RestTemplate` timeouts** — calls to the Python compute service are now bounded (2s connect, 5s read). If the engine is down the backend returns zero-valued fatigue immediately instead of hanging.
- **`ExportController` deleted** — the mobile app generates `.xlsx`/`.csv` locally. No server-side export endpoint anymore.
- **Dev JWT keys removed from `application.yml`** — they live only in `application-dev.yml` / `application-test.yml`. Production must use env vars.

---

## 4. Rebuild and run

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./mvnw -q clean package -DskipTests
```

If the build fails with "release version 21 not supported", the JDK in `JAVA_HOME` is older than 21 — install OpenJDK 21 first.

Restart the backend the way the server already does it (systemd unit, docker container, etc.). On start you should see Flyway log lines like:

```
Migrating schema "public" to version "5 - client request id and version"
Migrating schema "public" to version "6 - align metric column types"
```

If you see `Schema validation: missing table [exercises]`, the `spring-boot-flyway` dependency didn't pick up — confirm the new `pom.xml` is what got built (`grep -A1 spring-boot-flyway pom.xml`).

The Python compute service runs independently. Restart only if its code changed (`git log --oneline -- recursos/`):

```bash
cd recursos
pip install -r requirements.txt --upgrade
# Restart however it's managed (uvicorn under systemd / supervisor / nohup):
pkill -f 'uvicorn .*api.main:app' || true
nohup uvicorn api.main:app --host 0.0.0.0 --port 8001 \
      > ~/compute.log 2>&1 &
```

---

## 5. Smoke tests

Use `curl` against the running server. Replace `localhost:8000` with the actual host if you're testing from outside the box.

```bash
BASE=http://localhost:8000
EMAIL=smoke+$(date +%s)@test.com

# 5.1 Register and capture the access token.
TOKEN=$(curl -s -X POST $BASE/auth/register \
  -H 'Content-Type: application/json' \
  -d "{\"name\":\"smoke\",\"email\":\"$EMAIL\",\"password\":\"Password1\"}" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
[ -n "$TOKEN" ] || { echo "FAIL: no token"; exit 1; }
echo "OK: got token"

# 5.2 List exercises (needed to build a workout payload).
EXID=$(curl -s $BASE/exercises -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)[0]["id"])')
echo "OK: using exercise $EXID"

# 5.3 Send the same workout TWICE with the same client_request_id.
CRID=$(uuidgen)
PAYLOAD=$(cat <<JSON
{"name":"Smoke","date":"2026-05-12T10:00:00Z","duration_seconds":60,
 "client_request_id":"$CRID",
 "exercises":[{"exercise_id":"$EXID","order":0,
   "sets":[{"weight":60,"reps":5,"rpe":7,"order":0}]}]}
JSON
)
ID1=$(curl -s -X POST $BASE/workouts -H "Authorization: Bearer $TOKEN" \
      -H 'Content-Type: application/json' -d "$PAYLOAD" \
      | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
ID2=$(curl -s -X POST $BASE/workouts -H "Authorization: Bearer $TOKEN" \
      -H 'Content-Type: application/json' -d "$PAYLOAD" \
      | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
if [ "$ID1" = "$ID2" ]; then
  echo "OK: idempotency works — $ID1 == $ID2"
else
  echo "FAIL: idempotency broken — got $ID1 and $ID2"
  exit 1
fi

# 5.4 GET /workouts paginated.
curl -s "$BASE/workouts?page=0&size=10" -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);assert "has_more" in d, d;print("OK: paginated response shape")'

# 5.5 Optimistic locking — first PUT bumps version 0 → 1, second PUT with If-Match: 0 must fail.
HTTP1=$(curl -s -o /dev/null -w '%{http_code}' -X PUT $BASE/workouts/$ID1 \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H 'If-Match: 0' -d '{"name":"updated"}')
HTTP2=$(curl -s -o /dev/null -w '%{http_code}' -X PUT $BASE/workouts/$ID1 \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H 'If-Match: 0' -d '{"name":"updated again"}')
echo "first PUT: $HTTP1  second PUT (stale If-Match): $HTTP2"
[ "$HTTP1" = "200" ] && [ "$HTTP2" = "409" ] || { echo "FAIL: optimistic locking"; exit 1; }
echo "OK: 409 on stale version"

# 5.6 Correlation ID echo.
RID=$(uuidgen)
ECHOED=$(curl -s -D - -o /dev/null $BASE/workouts \
  -H "Authorization: Bearer $TOKEN" -H "X-Request-Id: $RID" \
  | grep -i '^X-Request-Id' | tr -d '\r' | awk '{print $2}')
[ "$ECHOED" = "$RID" ] && echo "OK: X-Request-Id echoed" \
                       || echo "FAIL: X-Request-Id missing or rewritten ($ECHOED)"

# 5.7 Fatigue endpoint — must respond even if compute engine is down.
curl -s $BASE/fatigue/summary -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import sys,json;d=json.load(sys.stdin);print("OK: fatigue, compute_available=",d.get("compute_available"))'
```

All `OK:` lines should print. If any `FAIL:` appears, capture the response body before changing anything and report back.

---

## 6. Troubleshooting cheatsheet

| Symptom | Cause | Fix |
|---|---|---|
| Schema validation: missing table | Flyway didn't run | Verify `spring-boot-flyway` is in `pom.xml` and you rebuilt the jar. |
| 500 on every endpoint right after deploy | `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` missing | Export them in the env, restart. See §2. |
| 409 on every PUT after deploy | Clients sending `If-Match: 0` after server already bumped versions | Have clients GET first, use the returned `version`. Older clients without `If-Match` still work. |
| Fatigue returns all zeros | Python compute service is down | `ss -tlnp \| grep 8001`, restart `recursos`. Backend behaves correctly (degraded) — no need to roll back. |
| `duration_seconds` is 0 in returned workouts | Old frontend payload using `durationSeconds` | Upgrade the mobile clients. The server already accepts only `duration_seconds`. |
| `PUT /workouts/{id}` response body shows the pre-update `version` (e.g. `0` after the first successful PUT) | The controller serialised before Hibernate flushed `@Version`. Clients reading `version` back for the next `If-Match` will 409 forever. | Fixed in commit that switched `workoutRepo.save(workout)` → `saveAndFlush`. If you ever see this again, check whether someone reverted it. |
| Migration V5 fails: `relation "workouts" does not exist` | DB is empty and Flyway is skipping V1–V4 for some reason | `SELECT * FROM flyway_schema_history;` to see state; if empty, drop the schema and let Flyway repopulate from V1. |
| `Found non-empty schema(s) "public" but no schema history table` on first start | Hibernate `ddl-auto=update` previously built the schema bypassing Flyway. The tables exist with real data, but `flyway_schema_history` does not. | Adopt the existing schema as the V4 baseline: set `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` and `SPRING_FLYWAY_BASELINE_VERSION=4` in `.env`. If the relaxed env-var binding doesn't kick in (depends on Spring Boot relaxation rules per release), pass them as `-Dspring.flyway.baseline-on-migrate=true -Dspring.flyway.baseline-version=4` in `ExecStart`. Then remove `SPRING_JPA_HIBERNATE_DDL_AUTO=update` from the systemd unit — `validate` from `application-prod.yml` is the canary §7 says not to disable. After this fix, V5/V6 run on top of the adopted baseline and preserve existing data. |

---

## 7. Don't do

- **Don't** copy the dev JWT keys from `application-dev.yml` into prod env vars. Generate fresh ones (see §2).
- **Don't** edit applied Flyway migrations (`V1`…`V6`). Add a new `V7` if you need a fix.
- **Don't** restart the backend during a migration — Flyway holds a lock, but a hard kill mid-migration can leave the schema in a partial state. If you have to, then `SELECT * FROM flyway_schema_history` and `flyway repair` from the host.
- **Don't** disable `ddl-auto: validate`. It's the canary that catches entity ↔ schema drift before users do.
