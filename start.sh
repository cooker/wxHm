#!/bin/bash

# --- 配置区 ---
APP_NAME="wxHm"
PORT=5000
WORKERS=4
LOG_DIR="logs"
PID_FILE="app.pid"

# --- 获取 IP 地址逻辑 ---
# 获取内网 IP (取第一个)
LAN_IP=$(hostname -I | awk '{print $1}')

# 获取外网 IP (通过 API 探测，若无外网则显示等待解析)
WAN_IP=$(curl -s --max-time 2 ifconfig.me || echo "无法获取/无公网IP")

# --- 初始化环境 ---
mkdir -p $LOG_DIR
mkdir -p uploads

echo "------------------------------------------------"
echo "🚀 正在启动 $APP_NAME..."

# 1. 检查端口是否被占用
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null ; then
    echo "⚠️ 错误: 端口 $PORT 已被占用，请先关闭相关进程。"
    exit 1
fi

# 2. 启动 Gunicorn (后台运行)
gunicorn --workers $WORKERS \
         --bind 0.0.0.0:$PORT \
         --access-logfile $LOG_DIR/access.log \
         --error-logfile $LOG_DIR/error.log \
         --pid $PID_FILE \
         --daemon \
         app:app

if [ $? -eq 0 ]; then
    echo "✅ $APP_NAME 启动成功！"
    echo "------------------------------------------------"
    echo "📍 访问地址清单:"
    echo "   - 本地回环: http://127.0.0.1:$PORT"
    echo "   - 内网访问: http://$LAN_IP:$PORT"
    echo "   - 外网访问: http://$WAN_IP:$PORT"
    echo "------------------------------------------------"
    echo "📝 管理台地址: http://$LAN_IP:$PORT/admin"
    echo "📊 统计趋势图: http://$LAN_IP:$PORT/admin/stats"
    echo "------------------------------------------------"
    echo "💡 提示: 外网访问需确保防火墙/安全组已开放 $PORT 端口"
else
    echo "❌ 启动失败，请检查 $LOG_DIR/error.log"
fi