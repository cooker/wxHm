package com.wxhm.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * NutUI-Vue SPA：/app 下 History 路由回退到 index.html；静态资源直出。
 */
@Controller
public class AppSpaController {

    private static MediaType guessType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".js")) return MediaType.valueOf("application/javascript");
        if (lower.endsWith(".mjs")) return MediaType.valueOf("application/javascript");
        if (lower.endsWith(".css")) return MediaType.valueOf("text/css");
        if (lower.endsWith(".json")) return MediaType.APPLICATION_JSON;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".webp")) return MediaType.valueOf("image/webp");
        if (lower.endsWith(".ico")) return MediaType.valueOf("image/x-icon");
        if (lower.endsWith(".svg")) return MediaType.valueOf("image/svg+xml");
        if (lower.endsWith(".woff2")) return MediaType.valueOf("font/woff2");
        if (lower.endsWith(".woff")) return MediaType.valueOf("font/woff");
        if (lower.endsWith(".ttf")) return MediaType.valueOf("font/ttf");
        return MediaType.TEXT_HTML;
    }

    private static ResponseEntity<Resource> index() {
        Resource index = new ClassPathResource("static/app/index.html");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(index);
    }

    @GetMapping({"/app", "/app/", "/app/**"})
    public ResponseEntity<Resource> spa(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        if (uri.isEmpty()) {
            uri = "/";
        }

        String sub;
        if ("/app".equals(uri) || "/app/".equals(uri)) {
            sub = "";
        } else if (uri.startsWith("/app/")) {
            sub = uri.substring("/app/".length());
        } else {
            sub = "";
        }

        if (sub == null || sub.isEmpty()) {
            return index();
        }

        Resource r = new ClassPathResource("static/app/" + sub);
        if (r.exists() && r.isReadable()) {
            return ResponseEntity.ok().contentType(guessType(sub)).body(r);
        }
        return index();
    }
}
