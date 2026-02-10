这是一份为您精心编写的完整 `README.md`。它不仅包含了项目的最新功能说明，还详细记录了从基础版到当前**数据驱动版**的所有技术演进。

---

# 📅 wxHm 项目变更日志与完整说明书

本文件记录了 `wxHm` (WeChat LiveCode Manager) 从 v1.0 至今的所有重大更新。

---

## 🚀 核心功能概览

`wxHm` 是一个专为微信群运营设计的活码管理系统，旨在解决群二维码 7 天过期的痛点。

* **智能活码分发**：固定链接访问，后台动态轮转最新上传的二维码。
* **图像性能引擎**：自动 WebP 转换与 `wsrv.nl` 全球 CDN 镜像镜像加速。
* **多维数据看板**：集成 ECharts，实时展示 7 日 PV/UV 趋势及今日设备（iOS/安卓/PC）占比。
* **自动化运维**：物理文件与统计数据均支持 7 天自动清理循环。
* **极简管理体验**：3 天免密登录缓存，支持群组一键更名与物理删除。
* **自定义文件托管**：可在 `/admin/upload-file` 上传任意文件，生成稳定下载链接，并在页面中查看、删除。
* **微信公众号通知**：可在 `/admin/notice` 维护公众号模板配置，并在访问群码、管理员操作、群码过期时自动发送模板消息。

---

## 📝 详细变更记录

### v1.6.0 (最新版本) - 微信通知与文件托管

* **[新增]** 微信公众号操作工具类 `WeChatAPI`，支持获取并缓存 `access_token`，统一封装 API 请求。
* **[新增]** 微信公众号模板消息维护页 `/admin/notice`：
  * 可配置 `appid / secret / touser / template_id / 模板 data`；
  * 配置持久化到数据库，并支持多条配置的新增、编辑、删除；
  * 支持「读取最新配置」，自动加载最近更新的一条配置到表单。
* **[新增]** 固定结构的模板消息推送（字段：`group / action / server / user / time`）：
  * 用户访问群码时自动推送访问通知；
  * 管理员上传 / 更名 / 删除群码时推送维护通知；
  * 群码过期被系统清理时推送过期通知。
* **[新增]** 自定义文件托管：
  * 新增 `/admin/upload-file` 页面上传文件至 `uploads/files/`；
  * 文件名即为上传文件名，自动生成外部访问链接（`https://域名/文件名`）；
  * 页面中列出所有已上传文件，并支持一键删除。
* **[优化]** 管理中心群组列表与统计页中，隐藏 `files` 目录，仅展示真实群组。

### v1.6.1 - 配置与推送优化

* **[配置]** 管理密码支持通过环境变量 `ADMIN_PASSWORD` 自定义，未设置时默认 `admin123`，便于 Docker/生产环境不写死密码。
* **[优化]** 微信公众号模板消息改为**异步推送**（后台线程发送），访问群码、管理员操作、群码过期等场景下请求立即返回，不阻塞主流程。

### v1.5.0 - 设备画像与环形图表

* **[新增]** 引入 `user-agents` 库，支持识别访问者的操作系统（iOS, Android, Windows, Mac, Linux）。
* **[新增]** `stats.html` 新增**今日设备分布环形图**，直观展现流量来源比例。
* **[优化]** 统计逻辑升级，支持在一个页面同时查看多个群组的独立趋势图与占比图。
* **[修复]** 修正了 `X-Forwarded-For` 在多层代理下获取真实 IP 的准确性。

### v1.4.0 - 数据统计与图表化

* **[新增]** 集成 **SQLite 数据库**，实现轻量级访问日志存储。
* **[新增]** 引入 **ECharts 5.x**，将枯燥的表格数据转化为**动态面积趋势图**。
* **[逻辑]** 实现统计数据 7 天自动回滚清理，防止数据库文件无限膨胀。

### v1.3.0 - 网络兼容性与安全策略

* **[新增]** 引入 `ProxyFix` 中间件，完美适配 **Cloudflare/Nginx** 反向代理环境。
* **[优化]** 强制 `base_url` 走 **HTTPS** 协议，解决 `wsrv.nl` 在安全环境下抓图失败的问题。
* **[安全]** 调整 `Referrer-Policy` 为 `no-referrer-when-downgrade`，解决跨域策略导致的图片无法显示（strict-origin）。

### v1.2.0 - 性能突破

* **[新增]** 集成 **Pillow (PIL)**，所有上传图片自动压缩并转换为 **WebP** 格式。
* **[新增]** 接入 `wsrv.nl` CDN 镜像加速，实现图片边缘节点缓存，极速秒开。
* **[功能]** 支持图片加载失败时的自动降级逻辑（onerror fallback）。

### v1.1.0 - 多群组协作

* **[新增]** 支持多群组隔离，每个群组拥有独立的 `uploads` 文件夹。
* **[新增]** 增加后台管理功能：群组一键重命名、一键物理删除。
* **[体验]** 增加管理员密码本地缓存（localStorage），3 天内无需重复输入。

---

## 🛠️ 技术架构说明

| 组件 | 技术选型 | 说明 |
| --- | --- | --- |
| **后端框架** | Flask 2.x | 核心逻辑处理与路由分发 |
| **数据库** | SQLite | 存储 7 日访问统计（PV/UV/设备）与公众号模板配置 |
| **图像处理** | Pillow | 实现 WebP 自动压缩与格式转换 |
| **CDN 加速** | wsrv.nl | 全球边缘镜像分发，降低带宽压力 |
| **前端图表** | ECharts 5.x | 数据可视化看板 |
| **设备识别** | user-agents | 解析请求头识别操作系统 |
| **微信公众号** | 官方接口 + 自封装工具类 | 获取 `access_token` 与发送模板消息 |

---

## 📦 部署与运行指南

### 1. 环境安装

```bash
pip install -r requirements.txt
```

### 2. 关键配置

1. **管理密码**：通过环境变量 `ADMIN_PASSWORD` 设置，未设置时使用默认值 `admin123`。

   ```bash
   export ADMIN_PASSWORD=你的强密码
   ```

   或在启动时传入：

   ```bash
   ADMIN_PASSWORD=你的强密码 python app.py
   ```

   Docker 部署时在 `docker-compose.yml` 或 `docker run -e ADMIN_PASSWORD=...` 中配置即可。

2. **（可选）微信公众号模板消息**：

   * 打开 `/admin/notice` 页面；
   * 新增一条配置，填写 `appid / secret / touser / template_id`；
   * 按如下结构填写模板数据 `data`（JSON 格式）：

   ```json
   {
     "group":  { "value": "" },
     "action": { "value": "" },
     "server": { "value": "" },
     "user":   { "value": "" },
     "time":   { "value": "" }
   }
   ```

### 3. Nginx 配置建议 (若有)

务必添加以下 Header 以支持 IP 识别：

```nginx
location / {
    proxy_pass http://127.0.0.1:5000;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

```

---

## 💡 维护建议

* **备份**：由于数据存储在 `stats.db`，建议定期备份该单文件。
* **清理二维码**：系统会自动清理 7 天前的二维码图片，无需手动操作对应群组文件夹。
* **清理自定义文件**：在 `/admin/upload-file` 中可查看所有已上传文件，并删除不再需要的文件。

---

## 🐳 使用 Docker 部署

**A. 构建并启动**

在项目根目录运行：

```bash
docker-compose up -d
```

建议在 `docker-compose.yml` 中为服务配置环境变量，例如：

```yaml
environment:
  - TZ=Asia/Shanghai
  - ADMIN_PASSWORD=你的管理密码
```

**B. 查看容器状态**

```bash
docker ps
```

**C. 查看运行日志**

```bash
docker logs -f wxhm_app
```

---

## ☁️ 使用 Cloudflare Workers 部署（绑定 CF 域名）

若需将 wxHm 绑定到 Cloudflare 域名，并支持非标准端口（如 8092），可使用项目内的 `cf-workers/` 反向代理：

```bash
cd cf-workers
npx wrangler secret put ORIGIN_URL   # 输入后端地址，如 http://your-server:8092
npx wrangler deploy
```

随后在 CF 控制台为 Worker 添加自定义域名即可。详见 [cf-workers/README.md](cf-workers/README.md)。

---

## ❓ 常见问题 (FAQ)

### 1. 微信公众号模板消息发不出去怎么办？

1. 在 `/admin/notice` 中确认：
   * `appid / secret` 与公众号后台一致；
   * `touser` 为已关注该公众号的真实 `openid`；
   * 模板 ID 开通且未被删除。
2. 在微信平台的 IP 白名单中放行你的服务器出口 IP。
3. 查看容器或进程日志，搜索「自动发送微信模板消息失败」关键字定位错误。

### 2. 访问统计里 IP 不准确？

请确认 Nginx / 反向代理中已正确透传 `X-Forwarded-For` 与 `X-Forwarded-Proto`，并参考上文 Nginx 示例配置。

### 3. 自定义文件访问 404？

1. 确认已在 `/admin/upload-file` 上传成功，对应文件出现在「已上传文件」列表；
2. 访问路径应为：`https://你的域名/<文件名>`（根路径直出，不带 `uploads/` 前缀）；
3. 若文件名包含特殊字符，建议使用英文与数字组合。

---

**项目地址**: [https://github.com/cooker/wxHm](https://github.com/cooker/wxHm)

如果你觉得这个项目有帮助，请给一个 Star ⭐️！

![](zsm.jpg)
