#!/bin/bash

# --- 配置区 ---
APP_NAME="wxHm"
PORT=8092
WORKERS=4  # 建议设为 CPU 核心数 * 2 + 1
LOG_DIR="logs"
PID_FILE="app.pid"

# --- 初始化环境 ---
mkdir -p $LOG_DIR
mkdir -p uploads

echo "🚀 正在启动 $APP_NAME..."

# 1. 检查端口是否被占用
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
    echo "⚠️ 错误: 端口 $PORT 已被占用，请先关闭相关进程。"
    exit 1
fi

# 2. 启动 Gunicorn (后台运行)
# --access-log: 记录访问请求
# --error-log: 记录运行错误
# --daemon: 后台守护进程运行
gunicorn --workers $WORKERS \
         --bind 0.0.0.0:$PORT \
         --access-logfile $LOG_DIR/access.log \
         --error-logfile $LOG_DIR/error.log \
         --pid $PID_FILE \
         --daemon \
         app:app

if [ $? -eq 0 ]; then
    echo "✅ $APP_NAME 启动成功！"
    echo "📍 访问地址: http://你的服务器IP:$PORT"
    echo "📝 日志文件位于 $LOG_DIR/ 目录下"
    echo "🔢 进程 PID 已写入 $PID_FILE"
else
    echo "❌ 启动失败，请检查 logs/error.log"
fi