/**
 * wxHm Cloudflare Workers 反向代理
 * 将 CF 域名流量转发到后端服务（支持非标准端口如 8092）
 *
 * 环境变量（通过 wrangler secret 或 wrangler.toml vars 配置）：
 *   ORIGIN_URL - 后端地址，如 http://your-server:8092 或 https://tunnel.example.com
 */
export default {
  async fetch(request, env) {
    const originUrl = env.ORIGIN_URL || env.ORIGIN_URL_DEFAULT;
    if (!originUrl) {
      return new Response('ORIGIN_URL 未配置，请在 wrangler.toml 或 Secrets 中设置', {
        status: 500,
        headers: { 'Content-Type': 'text/plain; charset=utf-8' }
      });
    }

    const url = new URL(request.url);
    const targetUrl = new URL(originUrl);
    targetUrl.pathname = url.pathname;
    targetUrl.search = url.search;

    const clientIp = request.headers.get('CF-Connecting-IP') || '';
    const xForwardedFor = request.headers.get('X-Forwarded-For');
    const forwardedFor = clientIp ? (xForwardedFor ? `${xForwardedFor}, ${clientIp}` : clientIp) : xForwardedFor;

    const headers = new Headers(request.headers);
    if (forwardedFor) headers.set('X-Forwarded-For', forwardedFor);
    headers.set('X-Forwarded-Proto', 'https');
    headers.set('Host', targetUrl.host);

    try {
      const modifiedRequest = new Request(targetUrl.toString(), {
        method: request.method,
        headers,
        body: request.body,
        redirect: 'follow'
      });

      const response = await fetch(modifiedRequest);
      const responseHeaders = new Headers(response.headers);
      responseHeaders.set('Referrer-Policy', 'no-referrer-when-downgrade');

      return new Response(response.body, {
        status: response.status,
        statusText: response.statusText,
        headers: responseHeaders
      });
    } catch (err) {
      return new Response(`代理请求失败: ${err.message}`, {
        status: 502,
        headers: { 'Content-Type': 'text/plain; charset=utf-8' }
      });
    }
  }
};
