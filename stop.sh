#!/usr/bin/env bash
set -euo pipefail

APP_NAME="wxHm-java"
BASE_DIR="$(pwd -P)"
PID_FILE="${BASE_DIR}/run/app.pid"
WAIT_SECONDS="${WAIT_SECONDS:-20}"

if [[ ! -f "$PID_FILE" ]]; then
  echo "[$APP_NAME] 未找到 PID 文件：$PID_FILE"
  exit 0
fi

PID="$(<"$PID_FILE")"
if [[ -z "$PID" ]]; then
  echo "[$APP_NAME] PID 文件为空，已清理"
  rm -f "$PID_FILE"
  exit 0
fi

if ! kill -0 "$PID" 2>/dev/null; then
  echo "[$APP_NAME] 进程不存在（PID=$PID），已清理 PID 文件"
  rm -f "$PID_FILE"
  exit 0
fi

echo "[$APP_NAME] 正在停止进程 PID=$PID ..."
kill "$PID"

for ((i=1; i<=WAIT_SECONDS; i++)); do
  if ! kill -0 "$PID" 2>/dev/null; then
    rm -f "$PID_FILE"
    echo "[$APP_NAME] 已优雅停止"
    exit 0
  fi
  sleep 1
done

echo "[$APP_NAME] 优雅停止超时，执行强制停止"
kill -9 "$PID" 2>/dev/null || true
rm -f "$PID_FILE"
echo "[$APP_NAME] 已强制停止"