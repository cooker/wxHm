# 构建 Jar 包
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# 复制项目文件并打包
COPY wxhm-java ./wxhm-java
WORKDIR /build/wxhm-java
RUN mvn clean package -DskipTests

# 运行环境
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 复制生成的 Jar 包
COPY --from=builder /build/wxhm-java/target/app.jar /app/app.jar

# 创建上传目录和日志目录
RUN mkdir -p /data/wxHm/uploads /app/logs

EXPOSE 8092

ENV TZ=Asia/Shanghai
CMD ["java", "-jar", "app.jar", "--server.port=8092"]
