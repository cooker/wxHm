这是一份为您精心编写的完整 `README.md`。它不仅包含了项目的最新功能说明，还详细记录了从基础版到当前**数据驱动版**的所有技术演进。

---

# 📅 wxHm 项目变更日志与完整说明书

本文件记录了 `wxHm` (WeChat LiveCode Manager) 从 v1.0 到 v1.5 的所有重大更新。

---

## 🚀 核心功能概览

`wxHm` 是一个专为微信群运营设计的活码管理系统，旨在解决群二维码 7 天过期的痛点。

* **智能活码分发**：固定链接访问，后台动态轮转最新上传的二维码。
* **图像性能引擎**：自动 WebP 转换与 `wsrv.nl` 全球 CDN 镜像镜像加速。
* **多维数据看板**：集成 ECharts，实时展示 7 日 PV/UV 趋势及今日设备（iOS/安卓/PC）占比。
* **自动化运维**：物理文件与统计数据均支持 7 天自动清理循环。
* **极简管理体验**：3 天免密登录缓存，支持群组一键更名与物理删除。

---

## 📝 详细变更记录

### v1.5.0 (最新版本) - 设备画像与环形图表

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
| **数据库** | SQLite | 存储 7 日访问统计（PV/UV/设备） |
| **图像处理** | Pillow | 实现 WebP 自动压缩与格式转换 |
| **CDN 加速** | wsrv.nl | 全球边缘镜像分发，降低带宽压力 |
| **前端图表** | ECharts 5.x | 数据可视化看板 |
| **设备识别** | user-agents | 解析请求头识别操作系统 |

---

## 📦 部署指南

### 1. 环境安装

```bash
pip install Flask Flask-SQLAlchemy Pillow user-agents werkzeug gunicorn

```

### 2. 关键配置

修改 `app.py` 中的 `ADMIN_PASSWORD`。

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
* **清理**：系统会自动清理 7 天前的文件，无需手动操作 `uploads` 文件夹。

---

## 如何使用 Docker 部署
A. 构建并启动
在项目根目录运行：

```Bash
docker-compose up -d
```

B. 查看容器状态
```Bash
docker ps
```
C. 查看运行日志
```Bash

docker logs -f wxhm_app
```

**您觉得这份 README 是否还需要增加一个“常见问题 (FAQ)”板块，来解决 Cloudflare 拦截等具体环境配置问题？**

**项目地址**: [https://github.com/cooker/wxHm](https://github.com/cooker/wxHm)

如果你觉得这个项目有帮助，请给一个 **Star ⭐️**！
![](zsm.jpg)
---
