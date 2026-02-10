# wxHm 项目提示词

> 供 AI 助手、开发者快速理解项目结构与开发规范，便于协作与扩展。

---

## 一、项目定位

**wxHm**（WeChat LiveCode Manager）是一个**微信群活码管理系统**，核心解决微信群二维码 7 天过期的痛点。

- **用户场景**：用户通过固定链接（如 `/group/群组名`）访问，系统展示最新有效群二维码，支持长按识别入群或保存到本地。
- **运营场景**：管理员上传/轮换群码，系统自动清理过期图片、记录访问数据、可选推送微信公众号模板消息。

---

## 二、技术栈

| 层级 | 技术 | 用途 |
|------|------|------|
| 后端 | Flask 2.x | 路由、业务逻辑、模板渲染 |
| 数据库 | SQLite + Flask-SQLAlchemy | 访问日志、公众号配置 |
| 图像 | Pillow | 上传时转 WebP、压缩 |
| CDN | wsrv.nl | 图片全球镜像加速 |
| 前端 | 原生 HTML/CSS/JS + Jinja2 | 无框架，内联样式 |
| 图表 | ECharts 5.x | 统计看板 |
| 设备识别 | user-agents | 区分 iOS/Android/PC |
| 微信 | 自封装 WeChatAPI | access_token 缓存、模板消息 |

---

## 三、目录结构

```
wxHm/
├── app.py              # 主应用：路由、业务逻辑、数据库模型
├── wechat_api.py       # 微信公众号 API 工具类
├── requirements.txt    # 依赖
├── templates/          # Jinja2 模板
│   ├── index.html      # 群码预览页（用户访问 /group/<name>）
│   ├── admin.html      # 管理中心：上传群码、更名、删除
│   ├── stats.html      # 数据看板：PV/UV 趋势、设备占比
│   ├── notice.html     # 微信公众号模板消息配置
│   ├── upload.html     # 自定义文件上传
│   └── home.html       # 项目介绍首页
├── uploads/            # 上传存储（gitignore）
│   ├── <群组名>/       # 各群组二维码（.webp/.png/.jpg）
│   └── files/          # 自定义文件
├── stats.db            # SQLite 数据库（运行时生成）
└── PROJECT_PROMPT.md   # 本文件
```

---

## 四、核心路由

| 路由 | 方法 | 说明 |
|------|------|------|
| `/` | GET | 项目首页 |
| `/group/<group_name>` | GET | 群码预览页，记录访问日志、可选推送通知 |
| `/admin` | GET/POST | 管理中心：上传群码（需密码） |
| `/admin/stats` | GET | 统计看板 |
| `/admin/upload-file` | GET | 自定义文件上传页面 |
| `/admin/notice` | GET/POST | 微信公众号模板消息配置 |
| `/admin/rename` | POST | 群组更名 |
| `/admin/delete/<group_name>` | POST | 删除群组 |
| `/uploads/<group_name>/<filename>` | GET | 静态提供群码图片 |
| `/<filename>` | GET | 自定义文件根路径访问 |

---

## 五、数据模型

### VisitLog（访问日志）

- `group_name`：群组名  
- `date`：日期 YYYY-MM-DD  
- `ip`：访客 IP（支持 X-Forwarded-For）  
- `platform`：iOS / Android / Windows / Mac / Linux / Other  

### WeChatTemplate（公众号配置）

- `appid`, `secret`, `touser`, `template_id`, `template_data`（JSON）  
- 模板消息固定字段：`group`, `action`, `server`, `user`, `time`  

---

## 六、关键业务逻辑

### 1. 群码轮转与过期

- `get_active_qr(group_name)`：取群目录下最近 7 天内最新的有效图片  
- 超过 7 天的图片自动删除，并异步推送「群码过期自动清理」模板消息  

### 2. 图片展示策略

- 优先用 `wsrv.nl` CDN 代理原图（解决跨域、加速）  
- `onerror` 降级为本站 `/uploads/...` 直链  

### 3. 微信模板消息

- 通过 `send_wechat_template_for_event()` 异步发送，不阻塞请求  
- 触发场景：用户访问群码、管理员更新/更名/删除群码、群码过期清理  

### 4. 群码预览页特性

- 微信内置浏览器检测（User-Agent 含 `MicroMessenger`）：显示「建议使用浏览器打开」提示条  
- 「保存到本地」按钮：`<a href="/uploads/..." download="...">`，同源直链下载  

---

## 七、配置与环境

- `ADMIN_PASSWORD`：管理密码，默认 `admin123`  
- `EXPIRE_DAYS`：群码有效期天数，默认 7  
- `Referrer-Policy`：`no-referrer-when-downgrade`，保证图片可加载  
- 反向代理需透传：`X-Forwarded-For`、`X-Forwarded-Proto`  

---

## 八、扩展与修改指南

### 新增页面

1. 在 `app.py` 添加路由，`render_template('xxx.html', ...)`  
2. 在 `templates/` 新增对应模板  

### 修改群码展示逻辑

- 修改 `templates/index.html`：样式、文案、微信检测、保存按钮等  
- 修改 `app.py` 中 `group_page()`：传参、统计、通知逻辑  

### 新增统计维度

- 扩展 `VisitLog` 或新增模型  
- 修改 `stats.html` 和 `/admin/stats` 路由中的查询与图表  

### 微信相关

- `wechat_api.py`：access_token 缓存、`send_template_message`  
- 模板数据格式须符合微信要求，`data` 为 `{ "key": { "value": "..." } }`  

---

## 九、代码约定

- **语言**：业务与注释以中文为主  
- **模板**：Jinja2，内联 CSS/JS，无构建步骤  
- **样式**：主色 `#07c160`（微信绿），与项目风格一致  
- **安全**：管理操作均校验 `ADMIN_PASSWORD`；文件路径使用 `os.path.basename` 防遍历  
- **异步**：微信通知用 `threading.Thread(daemon=True)`，不阻塞主流程  

---

## 十、常见问题速查

| 问题 | 处理思路 |
|------|----------|
| 群码不显示 | 检查 wsrv.nl 可访问性、Referrer-Policy、onerror 降级 |
| 模板消息失败 | 检查 appid/secret、touser(openid)、IP 白名单、模板 ID |
| 统计 IP 不准 | 确认 Nginx 等透传 X-Forwarded-For |
| 保存到本地失效 | 微信内用 `download` 可能受限，建议提示用浏览器打开 |

---

*最后更新：基于 wxHm 当前代码结构整理*
