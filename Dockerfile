# 使用轻量级 Python 镜像
FROM python:3.10-slim

# 设置工作目录
WORKDIR /app

# 安装必要的系统依赖（Pillow 处理 WebP 需要基础库）
RUN apt-get update && apt-get install -y --no-install-recommends \
    libwebp-dev \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖清单并安装
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制项目所有文件
COPY . .

# 创建上传目录和日志目录
RUN mkdir -p uploads logs

# 暴露 Flask/Gunicorn 运行端口
EXPOSE 8092

# 启动命令：使用 Gunicorn，绑定 0.0.0.0
CMD ["gunicorn", "--workers", "4", "--bind", "0.0.0.0:8092", "app:app"]