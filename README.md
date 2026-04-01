# wxHm (Java + NutUI-Vue)

微信群活码管理系统，后端基于 Spring Boot + SQLite，前端基于 Vue3 + NutUI，支持群码轮转、数据看板、文件托管与后台风控。

---

## 项目现状（重要）

- 已完成从旧模板页向 SPA 迁移，主入口为：`/app/`
- 根路径自定义文件访问仍保留：`/{filename}`
- 旧链接兼容：
  - `/` 会跳转到 `/app/`
  - `/group/{groupName}` 会跳转到 `/app/group/{groupName}`

---

## 核心功能

- 活码访问与轮转：固定群入口，后台上传新码即可替换
- 群码访问统计：7 日 PV/UV 趋势 + 设备分布（ECharts）
- 未创建群码访问监控：统计访问不存在群码链接的请求（3 天保留）
- 自定义文件托管：上传、批量创建、预览、删除，支持 `/{filename}` 直链
- 后台登录与会话鉴权：Session + `X-Admin-Token`
- 登录风控与审计：
  - 记录登录失败的 IP、设备、输入错误密码、时间
  - 同 IP 30 分钟内输错 5 次，锁定 30 分钟
  - 后台提供“登录监控”页面查看最近记录
- 公众号模板消息：配置、发送测试、维护多套配置

---

## 技术栈

### 后端（`wxhm-java`）

- Java 21
- Spring Boot 3.2.5
- Spring MVC + Spring Data JPA
- SQLite + Hibernate SQLite Dialect
- Thymeleaf（保留少量兼容入口）

### 前端（`wxhm-web`）

- Vue 3 + Vue Router
- NutUI
- ECharts
- Vite（`base=/app/`，构建产物输出到 Java `static/app`）

---

## 目录结构

- `wxhm-java/`：后端工程
  - `src/main/java/com/wxhm/controller/`：页面跳转与 API 控制器
  - `src/main/java/com/wxhm/service/`：业务服务（统计、风控、群码、通知）
  - `src/main/java/com/wxhm/entity/`：JPA 实体
  - `src/main/java/com/wxhm/repository/`：数据仓储
  - `src/main/resources/static/app/`：前端打包产物（由 `wxhm-web` 构建同步）
  - `src/main/resources/application.yml`：配置
- `wxhm-web/`：NutUI-Vue 前端工程
- `cf-workers/`：Cloudflare Workers 反向代理示例

---

## 快速启动

### 1) 构建前端（同步到 Java static）

```bash
cd wxhm-web
npm install
npm run build
```

### 2) 构建后端

```bash
cd wxhm-java
mvn clean package -DskipTests
```

### 3) 运行

```bash
java -jar target/app.jar
```

默认端口：`8092`

常用地址：

- 前端首页：`http://127.0.0.1:8092/app/`
- 管理登录：`http://127.0.0.1:8092/app/admin/login`
- 兼容群链接：`http://127.0.0.1:8092/group/你的群名`

---

## 一键脚本

根目录提供：

- `start.sh`：启动（按执行时当前目录写 `run/app.pid` 与 `logs/app.out.log`）
- `stop.sh`：停止（优雅停止，超时强制结束）

```bash
./start.sh
./stop.sh
```

---

## 配置说明

配置文件：`wxhm-java/src/main/resources/application.yml`

关键配置：

- `server.port`：端口（默认 `8092`）
- `spring.datasource.url`：SQLite 地址（默认 `jdbc:sqlite:stats.db`）
- `wxhm.upload-base`：群码目录
- `wxhm.files-dir`：自定义文件目录
- `wxhm.expire-days`：群码有效天数
- `wxhm.admin-password`：后台密码（建议走环境变量）

推荐：

```bash
export ADMIN_PASSWORD=your_strong_password
```

---

## API 概览

### 公开接口

- `GET /api/public/home`
- `GET /api/public/group/{groupName}`

### 管理接口（需登录）

- 会话：`/api/admin/login`、`/api/admin/logout`、`/api/admin/session`
- 群码：`/api/admin/groups`、`/api/admin/groups/upload`、`/api/admin/groups/rename`、`/api/admin/groups/{groupName}`
- 统计：`/api/admin/stats`、`/api/admin/stats/data`
- 自定义文件：`/api/admin/files`、`/api/admin/files/upload`、`/api/admin/files/paste`、`/api/admin/files/delete`
- 未创建群码：`/api/admin/missing-groups`
- 登录监控：`/api/admin/login-monitor`
- 公众号：`/api/admin/notice/*`

---

## 后台页面（SPA）

- `/app/admin`：管理中心（卡片菜单、上传群码、改名、换码、删群）
- `/app/admin/stats`：数据看板
- `/app/admin/upload`：自定义文件（含预览）
- `/app/admin/notice`：公众号模板配置
- `/app/admin/missing-groups`：未创建群码访问统计
- `/app/admin/login-monitor`：登录失败监控（IP/设备/错误密码）

---

## 反向代理建议（Nginx）

```nginx
location / {
    proxy_pass http://127.0.0.1:8092;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

说明：

- 系统通过 `X-Forwarded-For` 识别真实 IP（用于统计与登录风控）

---

## 构建说明（Maven + 前端）

`wxhm-java/pom.xml` 已包含 `frontend-maven-plugin`，默认属性：

- `wxhm.skip.frontend=true`（默认跳过 npm 构建，适合无 Node 环境）

如需在 Maven 打包时自动构建前端：

```bash
cd wxhm-java
mvn clean package -DskipTests -Dwxhm.skip.frontend=false
```

（前提：本机已安装 Node/npm）

---

## 常见问题

### 1) 前端改了但页面没更新

- 在 `wxhm-web` 执行 `npm run build`
- 确认产物写入 `wxhm-java/src/main/resources/static/app`
- 再重启 Java 进程

### 2) 后台提示未登录

- 检查浏览器是否拦截 Cookie
- 反向代理是否透传会话

### 3) 登录被限制

- 触发条件：同一 IP 30 分钟内连续输错 5 次
- 锁定时长：30 分钟
- 可在后台“登录监控”查看失败记录

### 4) 自定义文件访问 404

- 检查文件是否在 `/app/admin/upload` 列表中
- 访问方式应为：`https://域名/文件名`
- 文件名避免与保留路径冲突：`app`、`api`、`admin`、`group`、`uploads` 等

---

## License

MIT

---

## 项目地址

- GitHub: [https://github.com/cooker/wxHm](https://github.com/cooker/wxHm)

如果这个项目对你有帮助，欢迎 Star。