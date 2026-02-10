# wxHm Cloudflare Workers 反向代理

将 CF 域名流量转发到 wxHm 后端服务，**支持非标准端口（如 8092）**，解决 CF 仅支持 80/443 等有限端口的问题。

## 架构

```
用户 → CF 域名(HTTPS:443) → Worker → 后端服务(HTTP:8092)
```

- 用户访问 `https://wxhm.yourdomain.com`（或 workers.dev 子域）
- Worker 转发到配置的后端地址（如 `http://your-server:8092`）
- 透传 `X-Forwarded-For`、`X-Forwarded-Proto`、`Host`，与 Nginx/CF 代理行为一致

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
