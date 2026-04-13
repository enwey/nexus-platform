#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"
LOG_DIR="$ROOT_DIR/.logs"

NODE_BIN_DIR="$ROOT_DIR/.tools/node/current/bin"
JAVA_HOME_DIR="$ROOT_DIR/.tools/java/current"
MAVEN_BIN="$ROOT_DIR/apache-maven-3.9.9/bin/mvn"
M2_REPO="$ROOT_DIR/.tools/m2"
SYSTEM_PATH="/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"

mkdir -p "$RUN_DIR" "$LOG_DIR" "$M2_REPO"

require_file() {
  local file_path="$1"
  local message="$2"
  if [[ ! -e "$file_path" ]]; then
    echo "[ERROR] $message"
    exit 1
  fi
}

require_cmd() {
  local cmd="$1"
  local message="$2"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "[ERROR] $message"
    exit 1
  fi
}

is_running() {
  local pid_file="$1"
  if [[ -f "$pid_file" ]]; then
    local pid
    pid="$(cat "$pid_file")"
    # Ignore invalid/stale pid files such as 0/1 or non-numeric values.
    if [[ "$pid" =~ ^[0-9]+$ ]] && (( pid > 1 )) && kill -0 "$pid" >/dev/null 2>&1; then
      return 0
    fi
  fi
  return 1
}

start_bg() {
  local name="$1"
  local pid_file="$2"
  local log_file="$3"
  local cmd="$4"

  if is_running "$pid_file"; then
    local pid
    pid="$(cat "$pid_file")"
    echo "[SKIP] $name already running (pid=$pid)"
    return
  fi

  echo "[START] $name"
  nohup bash -lc "$cmd" >"$log_file" 2>&1 &
  local pid=$!
  echo "$pid" >"$pid_file"
  echo "        pid=$pid log=$log_file"
}

require_cmd docker "Docker is required. Please install and open Docker Desktop first."
require_file "$NODE_BIN_DIR/node" "Node.js not found at .tools/node/current/bin/node"
require_file "$JAVA_HOME_DIR/bin/java" "Java not found at .tools/java/current/bin/java"
require_file "$MAVEN_BIN" "Maven not found at apache-maven-3.9.9/bin/mvn"

echo "[1/4] Starting infrastructure (postgres/redis/minio)..."
docker compose -f "$ROOT_DIR/docker-compose/docker-compose.yml" up -d postgres redis minio

echo "[2/4] Preparing environment..."
set -a
source "$ROOT_DIR/.env"
set +a

export PATH="$NODE_BIN_DIR:$PATH"
export JAVA_HOME="$JAVA_HOME_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
export PATH="$SYSTEM_PATH:$PATH"

# Local fallback for empty DB when migrations are not present.
export SPRING_JPA_HIBERNATE_DDL_AUTO="${SPRING_JPA_HIBERNATE_DDL_AUTO:-update}"

echo "[3/4] Starting backend..."
start_bg \
  "backend" \
  "$RUN_DIR/backend.pid" \
  "$LOG_DIR/backend.log" \
  "cd '$ROOT_DIR' && set -a && source ./.env && set +a && export SPRING_JPA_HIBERNATE_DDL_AUTO='${SPRING_JPA_HIBERNATE_DDL_AUTO}' && export JAVA_HOME='$JAVA_HOME_DIR' && export PATH='$SYSTEM_PATH:$JAVA_HOME_DIR/bin:$NODE_BIN_DIR:$ROOT_DIR/apache-maven-3.9.9/bin:\$PATH' && '$MAVEN_BIN' -Dmaven.repo.local='$M2_REPO' -f backend/pom.xml spring-boot:run"

echo "[4/4] Starting frontends..."
start_bg \
  "dev-portal" \
  "$RUN_DIR/dev-portal.pid" \
  "$LOG_DIR/dev-portal.log" \
  "cd '$ROOT_DIR/dev-portal' && PATH='$NODE_BIN_DIR:\$PATH' npm run dev -- --host 0.0.0.0 --port 5173"

start_bg \
  "ops-portal" \
  "$RUN_DIR/ops-portal.pid" \
  "$LOG_DIR/ops-portal.log" \
  "cd '$ROOT_DIR/ops-portal' && PATH='$NODE_BIN_DIR:\$PATH' npm run dev -- --host 0.0.0.0 --port 5174"

echo ""
echo "Launched."
echo "Dev Portal:  http://localhost:5173"
echo "Ops Portal:  http://localhost:5174"
echo "Backend:     http://localhost:8080/api/v1/actuator/health"
echo ""
echo "Logs:"
echo "  $LOG_DIR/backend.log"
echo "  $LOG_DIR/dev-portal.log"
echo "  $LOG_DIR/ops-portal.log"
