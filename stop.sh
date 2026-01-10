#!/bin/bash
PID_FILE="app.pid"

if [ -f $PID_FILE ]; then
    PID=$(cat $PID_FILE)
    echo "🛑 正在停止进程 $PID..."
    kill $PID
    rm $PID_FILE
    echo "✅ 已停止。"
else
    echo "❓ 未发现运行中的进程文件 ($PID_FILE)。"
fi