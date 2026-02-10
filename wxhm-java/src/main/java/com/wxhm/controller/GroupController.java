package com.wxhm.controller;

import com.wxhm.config.WxHmProperties;
import com.wxhm.service.QrService;
import com.wxhm.service.WeChatNotifyService;
import com.wxhm.util.PlatformUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * 群码展示、静态资源
 */
@Controller
public class GroupController {

    private final QrService qrService;
    private final WeChatNotifyService weChatNotifyService;
    private final WxHmProperties properties;

    public GroupController(QrService qrService, WeChatNotifyService weChatNotifyService, WxHmProperties properties) {
        this.qrService = qrService;
        this.weChatNotifyService = weChatNotifyService;
        this.properties = properties;
    }

    @GetMapping("/group/{groupName}")
    public String groupPage(@PathVariable String groupName, HttpServletRequest request, Model model) {
        String qrFile = qrService.getActiveQr(groupName);

        String userAgent = request.getHeader("User-Agent");
        String platform = PlatformUtils.parsePlatform(userAgent);
        String clientIp = qrService.getClientIp(request);

        qrService.logVisit(groupName, clientIp, platform);
        weChatNotifyService.sendAsync(groupName, "用户访问群码", clientIp, "访客");

        String wsrvUrl = "";
        if (qrFile != null) {
            String host = request.getHeader("Host");
            if (host == null) host = "localhost:8092";
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isBlank()) scheme = "https";
            String rawUrl = scheme + "://" + host + "/uploads/" + groupName + "/" + qrFile;
            wsrvUrl = "https://wsrv.nl/?url=" + URLEncoder.encode(rawUrl, StandardCharsets.UTF_8) + "&we=1&v=" + System.currentTimeMillis() / 1000;
        }

        model.addAttribute("groupName", groupName);
        model.addAttribute("qrFile", qrFile);
        model.addAttribute("wsrvUrl", wsrvUrl);
        return "index";
    }

    @GetMapping("/uploads/{groupName}/{filename}")
    public ResponseEntity<Resource> serveQr(@PathVariable String groupName, @PathVariable String filename) {
        try {
            Path file = qrService.getGroupQrPath(groupName, filename);
            if (!java.nio.file.Files.exists(file) || !java.nio.file.Files.isRegularFile(file)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(file.toUri());
            String contentType = "image/webp";
            if (filename.toLowerCase().endsWith(".png")) contentType = "image/png";
            else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) contentType = "image/jpeg";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
