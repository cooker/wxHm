package com.wxhm.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 管理后台会话校验：HTML 路径跳转登录；/api/admin 与 stats/data 未授权返回 JSON。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_ADMIN = "WXHM_ADMIN";
    public static final String SESSION_ADMIN_TOKEN = "WXHM_ADMIN_TOKEN";

    private static String extractToken(HttpServletRequest request) {
        return request.getHeader("X-Admin-Token");
    }

    static String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        if (path.isEmpty()) {
            path = "/";
        }
        return path;
    }

    private static void writeJsonUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String msg = message == null ? "未登录" : message.replace("\"", "\\\"");
        response.getWriter().write("{\"ok\":false,\"message\":\"" + msg + "\"}");
    }

    /**
     * 登录成功后应回到的 /app 路径（含 query）。
     */
    static String buildAppRedirectTarget(HttpServletRequest request) {
        String path = normalizePath(request);
        String q = request.getQueryString();
        String suffix = (q != null && !q.isBlank()) ? "?" + q : "";
        if (path.startsWith("/admin/upload-file")) {
            return "/app/admin/upload" + suffix;
        }
        if (path.startsWith("/admin")) {
            return "/app" + path + suffix;
        }
        if ("/upload".equals(path)) {
            return "/app/admin/upload" + suffix;
        }
        return "/app/admin" + suffix;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = normalizePath(request);
        String method = request.getMethod();

        if (path.startsWith("/admin/login")) {
            return true;
        }

        boolean apiLoginPost = "/api/admin/login".equals(path) && "POST".equalsIgnoreCase(method);
        boolean apiSessionGet = "/api/admin/session".equals(path) && "GET".equalsIgnoreCase(method);
        if (apiLoginPost || apiSessionGet) {
            return true;
        }

        boolean uploadPost = "/upload".equals(path) && "POST".equalsIgnoreCase(method);
        boolean adminArea = path.startsWith("/admin");
        boolean apiAdminArea = path.startsWith("/api/admin");

        if (!adminArea && !uploadPost && !apiAdminArea) {
            return true;
        }

        boolean tokenApi = "/admin/stats/data".equals(path) || "/api/admin/stats/data".equals(path);

        HttpSession session = request.getSession(false);
        boolean logged = session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_ADMIN));
        String sessionToken = session != null ? (String) session.getAttribute(SESSION_ADMIN_TOKEN) : null;
        String reqToken = extractToken(request);

        if (tokenApi) {
            if (logged && sessionToken != null && !sessionToken.isBlank() && sessionToken.equals(reqToken)) {
                return true;
            }
            writeJsonUnauthorized(response, logged ? "无效或过期的令牌" : "未登录");
            return false;
        }

        if (logged) {
            return true;
        }

        if (apiAdminArea) {
            writeJsonUnauthorized(response, "未登录");
            return false;
        }

        String base = request.getContextPath() != null ? request.getContextPath() : "";
        String appTarget = buildAppRedirectTarget(request);
        String loginUrl = base + "/app/admin/login?redirect=" + URLEncoder.encode(appTarget, StandardCharsets.UTF_8);
        response.sendRedirect(loginUrl);
        return false;
    }
}
