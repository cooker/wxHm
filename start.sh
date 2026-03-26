#!/usr/bin/env bash
set -euo pipefail

APP_NAME="微信活码"
BASE_DIR="$(pwd -P)"
APP_DIR="${BASE_DIR}"
RUN_DIR="${BASE_DIR}/run"
LOG_DIR="${BASE_DIR}/logs"
PID_FILE="${RUN_DIR}/app.pid"
OUT_LOG="${LOG_DIR}/app.out.log"

PORT="${PORT:-8092}"
JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS_DEFAULT="-Xms512m -Xmx1536m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR} -XX:+ExitOnOutOfMemoryError -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
JAVA_OPTS="${JAVA_OPTS:-$JAVA_OPTS_DEFAULT}"

find_jar() {
  local jar=""
  if [[ -f "${APP_DIR}/app.jar" ]]; then
    jar="${APP_DIR}/app.jar"
  else
    jar="$(ls -t "${APP_DIR}"/*.jar 2>/dev/null | rg -v 'original-' | head -n 1 || true)"
  fi
  [[ -n "$jar" ]] && [[ -f "$jar" ]] && echo "$jar"
}

is_running() {
  if [[ -f "$PID_FILE" ]]; then
    local pid
    pid="$(<"$PID_FILE")"
    [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null
  else
    return 1
  fi
}

mkdir -p "$RUN_DIR" "$LOG_DIR"

if is_running; then
  echo "[$APP_NAME] 已在运行，PID=$(<"$PID_FILE")"
  exit 0
fi

if [[ -f "$PID_FILE" ]]; then
  rm -f "$PID_FILE"
fi

JAR_FILE="$(find_jar || true)"
if [[ -z "$JAR_FILE" ]]; then
  echo "[$APP_NAME] 未找到可运行 jar，请先执行：cd wxhm-java && mvn clean package -DskipTests"
  exit 1
fi

if lsof -iTCP:"$PORT" -sTCP:LISTEN -t >/dev/null 2>&1; then
  echo "[$APP_NAME] 端口 $PORT 已被占用，请先释放后再启动"
  exit 1
fi

echo "[$APP_NAME] 启动中..."
echo "[$APP_NAME] JAR: $JAR_FILE"
echo "[$APP_NAME] PORT: $PORT"
echo "[$APP_NAME] LOG: $OUT_LOG"

nohup "$JAVA_BIN" $JAVA_OPTS -jar "$JAR_FILE" --server.port="$PORT" >>"$OUT_LOG" 2>&1 &
echo $! > "$PID_FILE"
sleep 1

if is_running; then
  echo "[$APP_NAME] 启动成功，PID=$(<"$PID_FILE")"
  echo "访问地址: http://127.0.0.1:${PORT}"
  echo "管理后台: http://127.0.0.1:${PORT}/admin/login"
else
  echo "[$APP_NAME] 启动失败，请检查日志: $OUT_LOG"
  rm -f "$PID_FILE"
  exit 1
fi