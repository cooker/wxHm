# wxHm (Java版)

微信群活码管理系统（Spring Boot + SQLite + Thymeleaf）。

核心目标：使用固定链接分发群码，自动处理过期二维码，并提供后台管理、数据看板和公众号模板消息能力。

---

## 功能概览

- 固定访问地址：`/group/{groupName}`
- 群码自动过期：超过配置天数后自动清理
- 管理后台登录保护：登录后才可访问管理页面
- 管理中心：上传/替换群码、内联改名、删除群组
- 数据看板：7日 PV/UV 趋势 + 今日设备分布（ECharts）
- 自定义文件：上传、粘贴批量创建、删除、根路径访问
- 微信公众号模板消息：配置、保存、发送测试
- 未创建群码访问统计：统计访问了不存在群码链接的请求（保留 3 天）

---

## 技术栈

- Java 21
- Spring Boot 3.2.5
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-thymeleaf`
- SQLite (`org.xerial:sqlite-jdbc`)
- Hibernate SQLite 方言 (`hibernate-community-dialects`)
- WebP 写入支持 (`org.sejda.imageio:webp-imageio`，失败自动回退 PNG)

---

## 目录结构

- `wxhm-java/`：Java 主项目
  - `src/main/java/com/wxhm/controller/`：页面和接口控制器
  - `src/main/java/com/wxhm/service/`：核心业务逻辑
  - `src/main/java/com/wxhm/entity/`：JPA 实体
  - `src/main/java/com/wxhm/repository/`：数据访问层
  - `src/main/resources/templates/`：后台与前台页面模板
  - `src/main/resources/application.yml`：应用配置
- `cf-workers/`：Cloudflare Workers 反向代理示例

> 根目录的旧 Python 运行链路已移除，当前以 `wxhm-java` 为准。

---

## 环境要求

- JDK 21+
- Maven 3.8+

---

## 快速启动

### 1) 编译

```bash
cd wxhm-java
mvn clean package -DskipTests
```

### 2) 运行

```bash
java -jar target/app.jar
```

默认端口：`8092`

启动后访问：

- 首页：`http://127.0.0.1:8092/`
- 后台登录：`http://127.0.0.1:8092/admin/login`

---

## 配置说明

配置文件：`wxhm-java/src/main/resources/application.yml`

关键项：

- `server.port`：服务端口（默认 `8092`）
- `spring.datasource.url`：SQLite 数据库路径（默认 `jdbc:sqlite:stats.db`）
- `wxhm.upload-base`：群码根目录（默认 `/data/wxHm/uploads`）
- `wxhm.files-dir`：自定义文件目录（默认 `/data/wxHm/uploads/files`）
- `wxhm.expire-days`：群码过期天数（默认 `7`）
- `wxhm.admin-password`：后台管理密码（支持环境变量覆盖）

### 推荐用环境变量覆盖管理密码

```bash
export ADMIN_PASSWORD=your_strong_password
java -jar target/app.jar
```

---

## 后台页面说明

登录后先进入管理中心，通过卡片菜单进入各功能页。

### 1) 管理中心 `/admin`

- 上传并发布群码
- 点击群名直接改名（失焦自动提交）
- 群状态标识：`有效` / `已失效`
- 操作：预览、修改群码、删除

### 2) 数据看板 `/admin/stats`

- 多群组 7 日趋势（PV/UV）
- 今日设备分布
- 自动刷新 / 手动刷新
- 图表数据通过 `/admin/stats/data` 无感更新

### 3) 自定义文件 `/admin/upload-file`

- 上传文件
- 粘贴样式批量创建文件
- 文件列表删除
- 文件可通过 `/{filename}` 访问

### 4) 公众号维护 `/admin/notice`

- 读取最新配置
- 保存/更新模板配置
- 发送测试模板消息
- 删除配置

### 5) 未创建群码访问 `/admin/missing-groups`

- 统计访问了不存在群码链接的请求
- 汇总（按群名 PV/UV）+ 最近访问明细
- 数据自动保留 3 天（超期自动清理）

---

## 访问与统计逻辑

- 访问 `/group/{groupName}` 时：
  - 若群存在：记录正常访问日志（`VisitLog`）
  - 若群不存在：记录到 `MissingGroupVisit`
- 正常统计看板使用 `VisitLog`
- 未创建群码统计使用 `MissingGroupVisit`

---

## 微信模板消息

可在 `/admin/notice` 配置：

- `appid`
- `secret`
- `touser`
- `template_id`
- `template_data`（JSON）
- `url`（可选）

模板数据建议结构：

```json
{
  "group": { "value": "" },
  "action": { "value": "" },
  "server": { "value": "" },
  "user": { "value": "" },
  "time": { "value": "" }
}
```

自动触发场景：

- 管理员更新群码
- 管理员更名群码
- 管理员删除群码
- 群码过期自动清理

---

## 反向代理与真实 IP

项目通过 `X-Forwarded-For` 获取客户端 IP。若使用 Nginx，请透传：

```nginx
location / {
    proxy_pass http://127.0.0.1:8092;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

---

## Cloudflare Workers 代理（可选）

目录：`cf-workers/`

可用于将 CF 域名请求转发到后端（支持非标准端口）。
详细步骤见：`cf-workers/README.md`

---

## 常见问题

### 1) 后台提示未登录/跳回登录页

- 检查 Cookie/Session 是否被浏览器拦截
- 反向代理需保留会话相关 Header 与 Cookie

### 2) 微信模板消息发送失败

- 检查 `appid/secret/touser/template_id`
- 检查公众号接口权限、IP 白名单
- 查看服务日志中的错误输出

### 3) 群码上传成功但前台不显示

- 检查群名目录下是否有有效图片
- 检查 `wxhm.expire-days` 是否过短
- 确认服务器时间是否正确

---

## 开发命令

```bash
# 编译
cd wxhm-java && mvn clean compile

# 打包
cd wxhm-java && mvn clean package -DskipTests

# 运行
cd wxhm-java && java -jar target/app.jar
```

---

## License

MIT

---

## 项目地址

- GitHub: https://github.com/cooker/wxHm

如果你觉得这个项目有帮助，请给一个 Star ⭐️！

![](zsm.jpg)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=cooker/wxHm&type=date&legend=top-left)](https://www.star-history.com/#cooker/wxHm&type=date&legend=top-left)