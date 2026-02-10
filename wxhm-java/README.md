# wxHm - Java/Spring 实现

微信群活码管理系统的 Java 版本，基于 **JDK 21** 与 **Spring Boot 3.2**。

## 技术栈

- **JDK 21**
- **Spring Boot 3.2**
- **Spring Data JPA** + **SQLite**
- **Thymeleaf** 模板引擎
- **wsrv.nl** CDN 图片加速
- **ECharts** 数据看板
- **WebP** 图像支持 (webp-imageio)

## 功能概览

与 Python 版本一致：

- 群码预览页（微信环境提示、保存到本地）
- 管理后台：上传群码、更名、删除
- 7 日 PV/UV 统计、设备分布
- 微信公众号模板消息配置与推送
- 自定义文件上传与托管
- 二维码 7 天自动过期清理

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+

### 运行

```bash
cd wxhm-java
mvn spring-boot:run
```

或打包后运行：

```bash
mvn package
java -jar target/wxhm-1.0.0.jar
```

### 配置

通过环境变量或 `application.yml` 配置：

| 配置项 | 环境变量 | 默认值 |
|--------|----------|--------|
| 管理密码 | `ADMIN_PASSWORD` | admin123 |
| 端口 | - | 8092 |
| 群码有效期(天) | - | 7 |

## 目录结构

```
wxhm-java/
├── pom.xml
├── src/main/java/com/wxhm/
│   ├── WxHmApplication.java
│   ├── config/          # 配置
│   ├── entity/          # JPA 实体
│   ├── repository/      # 数据访问
│   ├── service/         # 业务逻辑
│   ├── controller/      # 控制器
│   ├── wechat/          # 微信公众号 API
│   └── util/            # 工具类
└── src/main/resources/
    ├── application.yml
    └── templates/       # Thymeleaf 模板
```

## 与 Python 版本差异

- 使用 Thymeleaf 替代 Jinja2
- 使用 JPA 替代 Flask-SQLAlchemy
- 使用 RestTemplate 调用微信 API
- 群码保存优先 WebP，失败时降级为 PNG

## 项目地址

原项目: [https://github.com/cooker/wxHm](https://github.com/cooker/wxHm)
