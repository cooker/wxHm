# wxHm Cloudflare Workers 反向代理

使用 **Cloudflare Workers** 实现类似 **Nginx** 的反向代理：访问绑定域名时，将请求转发到 wxHm 后端（如 `http://117.72.103.32:8092`），支持非标准端口 8092。

## 架构

```
用户访问 https://你的域名  →  Worker  →  http://117.72.103.32:8092
```

- 用户访问 `https://wxhm.yourdomain.com` 或 `https://wxhm-proxy.xxx.workers.dev`
- Worker 将路径、查询、方法、Body、常用头原样转发到 `ORIGIN_URL_DEFAULT`（当前为 `http://117.72.103.32:8092`）
- 透传 `X-Forwarded-For`、`X-Forwarded-Proto`，并设置 `Host` 为访问域名，避免后端报「Direct IP access not allowed」
- 后端返回的 `Location` 重定向会改写为当前域名，避免跳回 IP

## 部署步骤

### 1. 安装 Wrangler

```bash
npm install -g wrangler
# 或
npx wrangler login
```

### 2. 配置后端地址

**方式 A：wrangler.toml 中直接配置（仅测试）**

```toml
[vars]
ORIGIN_URL_DEFAULT = "http://your-vps-ip:8092"
```

**方式 B：使用 Secret（推荐生产）**

```bash
cd cf-workers
npx wrangler secret put ORIGIN_URL
# 输入: http://your-server:8092
```

### 3. 部署 Worker

```bash
npx wrangler deploy
```

### 4. 绑定自定义域名

1. 打开 [Cloudflare Dashboard](https://dash.cloudflare.com) → Workers & Pages
2. 选择 `wxhm-proxy` → 设置 → 域名与路由
3. 添加自定义域：如 `wxhm.yourdomain.com`（域名需已在 CF 托管）

或使用 **workers.dev** 免费子域：`https://wxhm-proxy.<你的账号>.workers.dev`

## 后端部署说明

Worker 需要能访问你的后端，常见方式：

| 方式 | ORIGIN_URL 示例 | 说明 |
|------|-----------------|------|
| VPS 公网 IP | `http://1.2.3.4:8092` | 需开放防火墙 8092 端口 |
| VPS 域名 | `http://backend.example.com:8092` | 域名解析到 VPS |
| Cloudflare Tunnel | `http://localhost:8092` | 见下方 Tunnel 配置 |

### 配合 Cloudflare Tunnel（内网/无公网 IP）

若后端在内网或不想开放 8092 端口，可先用 [cloudflared](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/) 创建 Tunnel：

1. 安装 cloudflared，创建 Tunnel，配置 ingress：`http://localhost:8092`
2. 为 Tunnel 分配域名（如 `wxhm-internal.yourdomain.com`）
3. Worker 的 `ORIGIN_URL` 填该 Tunnel 域名：`https://wxhm-internal.yourdomain.com`

**或**直接让 Tunnel 对外提供服务，跳过 Worker：将 `wxhm.yourdomain.com` 解析到 Tunnel，由 Tunnel 直连 localhost:8092。

## 环境变量

| 变量 | 说明 |
|------|------|
| `ORIGIN_URL` | 后端地址（Secret 优先） |
| `ORIGIN_URL_DEFAULT` | 默认后端（wrangler.toml vars） |

## 验证

部署后访问 `https://你的域名/`，应看到 wxHm 首页；访问 `/group/群组名` 可测试群码页。

## 常见问题

### 出现 "Direct IP access not allowed"

当后端或前置代理拒绝「直连 IP」请求时会返回该提示。Worker 已改为将 **用户访问的域名**（如 `wxhm.yourdomain.com` 或 `wxhm-proxy.xxx.workers.dev`）作为 `Host` 头转发给后端，而不是后端地址里的 IP/主机名，从而避免被判定为直连 IP。若仍出现，请检查后端或 Nginx 是否对 `Host` 做了白名单限制，并放行你绑定的 CF 域名。
