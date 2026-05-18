#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

export DOCKER_CONFIG="${DOCKER_CONFIG:-$ROOT_DIR/.docker-config}"
mkdir -p "$DOCKER_CONFIG"

COMPOSE=(docker compose)
BASE_SERVICES=(mysql redis crmeb-admin crmeb-front admin-web)
APP_H5_ARTIFACT="app/unpackage/dist/build/h5/index.html"

usage() {
  cat <<'USAGE'
Usage:
  ./depoly.sh [options]

Options:
  --with-app-h5      Build and run the uni-app H5 container.
  --no-app-h5        Do not run the uni-app H5 container, even if H5 artifacts exist.
  --pull             Pull base images before rebuilding.
  --reset-db         Stop containers and remove volumes before starting. This re-imports SQL and deletes runtime data.
  --no-cache         Rebuild project images without Docker cache.
  -h, --help         Show this help.

Default:
  Rebuild and restart mysql, redis, crmeb-admin, crmeb-front, and admin-web.
  If app/unpackage/dist/build/h5/index.html exists, app-h5 is included automatically.
USAGE
}

WITH_APP_H5=auto
PULL_IMAGES=0
RESET_DB=0
NO_CACHE=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-app-h5)
      WITH_APP_H5=1
      ;;
    --no-app-h5)
      WITH_APP_H5=0
      ;;
    --pull)
      PULL_IMAGES=1
      ;;
    --reset-db)
      RESET_DB=1
      ;;
    --no-cache)
      NO_CACHE=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 2
      ;;
  esac
  shift
done

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required but was not found in PATH." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "docker compose is required but is not available." >&2
  exit 1
fi

if [[ ! -f .env ]]; then
  cp .env.example .env
  echo "Created .env from .env.example. Edit .env if you need custom ports or passwords."
fi

env_file_value() {
  local key="$1"
  local default="$2"
  local value

  value="$(grep -E "^${key}=" .env | tail -n 1 | cut -d= -f2- || true)"
  printf '%s' "${value:-$default}"
}

SERVICES=("${BASE_SERVICES[@]}")
PROFILE_ARGS=()
if [[ "$WITH_APP_H5" == "1" || ( "$WITH_APP_H5" == "auto" && -f "$APP_H5_ARTIFACT" ) ]]; then
  PROFILE_ARGS=(--profile app-h5)
  SERVICES+=(app-h5)
fi

echo "Validating docker compose configuration..."
"${COMPOSE[@]}" "${PROFILE_ARGS[@]}" config >/dev/null

if [[ "$RESET_DB" == "1" ]]; then
  echo "Removing containers and volumes because --reset-db was specified..."
  "${COMPOSE[@]}" "${PROFILE_ARGS[@]}" down -v
fi

if [[ "$PULL_IMAGES" == "1" ]]; then
  echo "Pulling base images..."
  "${COMPOSE[@]}" "${PROFILE_ARGS[@]}" pull mysql redis || true
fi

BUILD_ARGS=()
if [[ "$NO_CACHE" == "1" ]]; then
  BUILD_ARGS+=(--no-cache)
fi

wait_for_container_health() {
  local container="$1"
  local attempt

  for attempt in {1..60}; do
    local status
    status="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)"
    if [[ "$status" == "healthy" || "$status" == "running" ]]; then
      return 0
    fi
    sleep 2
  done

  echo "Timed out waiting for $container to become healthy." >&2
  return 1
}

migrate_quartz_table_case() {
  local db="${MYSQL_DATABASE:-$(env_file_value MYSQL_DATABASE crmeb)}"
  local user="${MYSQL_USER:-$(env_file_value MYSQL_USER crmeb)}"
  local password="${MYSQL_PASSWORD:-$(env_file_value MYSQL_PASSWORD crmeb123)}"
  local lower_count upper_count

  lower_count="$(docker exec crmeb-mysql mysql -N -B -u"$user" -p"$password" "$db" -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${db}' AND table_name='qrtz_locks';" 2>/dev/null || echo 0)"
  upper_count="$(docker exec crmeb-mysql mysql -N -B -u"$user" -p"$password" "$db" -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${db}' AND table_name='QRTZ_LOCKS';" 2>/dev/null || echo 0)"

  if [[ "$lower_count" == "1" && "$upper_count" == "0" ]]; then
    echo "Migrating Quartz table names to the case expected by Quartz..."
    docker exec -i crmeb-mysql mysql -u"$user" -p"$password" "$db" <<'SQL'
SET FOREIGN_KEY_CHECKS=0;
RENAME TABLE
  `qrtz_blob_triggers` TO `QRTZ_BLOB_TRIGGERS`,
  `qrtz_cron_triggers` TO `QRTZ_CRON_TRIGGERS`,
  `qrtz_fired_triggers` TO `QRTZ_FIRED_TRIGGERS`,
  `qrtz_job_details` TO `QRTZ_JOB_DETAILS`,
  `qrtz_locks` TO `QRTZ_LOCKS`,
  `qrtz_paused_trigger_grps` TO `QRTZ_PAUSED_TRIGGER_GRPS`,
  `qrtz_scheduler_state` TO `QRTZ_SCHEDULER_STATE`,
  `qrtz_simple_triggers` TO `QRTZ_SIMPLE_TRIGGERS`,
  `qrtz_simprop_triggers` TO `QRTZ_SIMPROP_TRIGGERS`,
  `qrtz_triggers` TO `QRTZ_TRIGGERS`;
SET FOREIGN_KEY_CHECKS=1;
SQL
  fi
}

echo "Building project images..."
"${COMPOSE[@]}" "${PROFILE_ARGS[@]}" build "${BUILD_ARGS[@]}" crmeb-admin crmeb-front admin-web
if [[ " ${SERVICES[*]} " == *" app-h5 "* ]]; then
  "${COMPOSE[@]}" "${PROFILE_ARGS[@]}" build "${BUILD_ARGS[@]}" app-h5
fi

echo "Starting infrastructure services..."
"${COMPOSE[@]}" "${PROFILE_ARGS[@]}" up -d mysql redis
wait_for_container_health crmeb-mysql
wait_for_container_health crmeb-redis
migrate_quartz_table_case

echo "Starting services: ${SERVICES[*]}"
"${COMPOSE[@]}" "${PROFILE_ARGS[@]}" up -d "${SERVICES[@]}"

echo
echo "Deployment finished."
echo "Admin web:     http://0.0.0.0:${ADMIN_WEB_PORT:-8080}"
echo "Admin API:     http://0.0.0.0:${ADMIN_API_PORT:-20400}"
echo "Front API:     http://0.0.0.0:${FRONT_API_PORT:-20410}"
if [[ " ${SERVICES[*]} " == *" app-h5 "* ]]; then
  echo "App H5:        http://0.0.0.0:${APP_H5_PORT:-8082}"
else
  echo "App H5:        skipped. Generate $APP_H5_ARTIFACT or run ./depoly.sh --with-app-h5"
fi
