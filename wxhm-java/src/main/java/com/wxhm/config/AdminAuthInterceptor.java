package com.wxhm.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 管理后台会话校验：未登录访问受保护路径时跳转登录页。
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_ADMIN = "WXHM_ADMIN";
    public static final String SESSION_ADMIN_TOKEN = "WXHM_ADMIN_TOKEN";

    private static String extractToken(HttpServletRequest request) {
        return request.getHeader("X-Admin-Token");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }
        if (path.isEmpty()) {
            path = "/";
        }

        if (path.startsWith("/admin/login")) {
            return true;
        }

        boolean uploadPost = "/upload".equals(path) && "POST".equalsIgnoreCase(request.getMethod());
        boolean adminArea = path.startsWith("/admin");
        if (!adminArea && !uploadPost) {
            return true;
        }

        HttpSession session = request.getSession(false);
        boolean logged = session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_ADMIN));
        String sessionToken = session != null ? (String) session.getAttribute(SESSION_ADMIN_TOKEN) : null;
        String reqToken = extractToken(request);
        if (logged) {
            // 后台页面与表单操作采用会话；接口（如 stats/data）采用 X-Admin-Token
            boolean tokenApi = "/admin/stats/data".equals(path);
            if (!tokenApi) return true;
            if (sessionToken != null && !sessionToken.isBlank() && sessionToken.equals(reqToken)) return true;
        }

        if ("/admin/stats/data".equals(path)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String full = request.getRequestURI();
        if (request.getQueryString() != null) {
            full += "?" + request.getQueryString();
        }
        String base = ctx != null ? ctx : "";
        String loginUrl = base + "/admin/login?redirect=" + URLEncoder.encode(full, StandardCharsets.UTF_8);
        response.sendRedirect(loginUrl);
        return false;
    }
}
