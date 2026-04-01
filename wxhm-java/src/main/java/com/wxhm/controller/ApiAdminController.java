package com.wxhm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wxhm.config.AdminAuthInterceptor;
import com.wxhm.config.WxHmProperties;
import com.wxhm.entity.WeChatTemplate;
import com.wxhm.repository.WeChatTemplateRepository;
import com.wxhm.service.MissingGroupVisitService;
import com.wxhm.service.QrService;
import com.wxhm.service.StatsService;
import com.wxhm.service.WeChatNotifyService;
import com.wxhm.service.AdminLoginSecurityService;
import com.wxhm.util.PlatformUtils;
import com.wxhm.wechat.WeChatApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理端 JSON API（需登录 Session；部分接口需 X-Admin-Token）
 */
@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

    private final WxHmProperties properties;
    private final QrService qrService;
    private final StatsService statsService;
    private final WeChatNotifyService weChatNotifyService;
    private final AdminLoginSecurityService adminLoginSecurityService;
    private final MissingGroupVisitService missingGroupVisitService;
    private final WeChatTemplateRepository templateRepository;
    private final WeChatApi weChatApi;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiAdminController(WxHmProperties properties, QrService qrService, StatsService statsService,
                              WeChatNotifyService weChatNotifyService, AdminLoginSecurityService adminLoginSecurityService,
                              MissingGroupVisitService missingGroupVisitService,
                              WeChatTemplateRepository templateRepository, WeChatApi weChatApi) {
        this.properties = properties;
        this.qrService = qrService;
        this.statsService = statsService;
        this.weChatNotifyService = weChatNotifyService;
        this.adminLoginSecurityService = adminLoginSecurityService;
        this.missingGroupVisitService = missingGroupVisitService;
        this.templateRepository = templateRepository;
        this.weChatApi = weChatApi;
    }

    private boolean checkPassword(String password) {
        return password != null && properties.getAdminPassword().equals(password);
    }

    private boolean isAdminSession(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        return s != null && Boolean.TRUE.equals(s.getAttribute(AdminAuthInterceptor.SESSION_ADMIN));
    }

    private String currentAdminToken(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s == null) return null;
        Object token = s.getAttribute(AdminAuthInterceptor.SESSION_ADMIN_TOKEN);
        return token instanceof String ? (String) token : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String safeAppRedirect(String redirect) {
        if (redirect == null || redirect.isBlank()) {
            return "/app/admin";
        }
        if (redirect.startsWith("//") || redirect.contains("://")) {
            return "/app/admin";
        }
        if (!redirect.startsWith("/app")) {
            return "/app/admin";
        }
        return redirect;
    }

    private static Long parseNullableLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/session")
    public Map<String, Object> session(HttpServletRequest request) {
        boolean logged = isAdminSession(request);
        Map<String, Object> m = new HashMap<>();
        m.put("loggedIn", logged);
        m.put("token", logged ? currentAdminToken(request) : null);
        return m;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String password = body != null ? body.get("password") : null;
        String redirect = body != null ? body.get("redirect") : null;
        String ip = getClientIp(request);
        String platform = PlatformUtils.parsePlatform(request.getHeader("User-Agent"));

        var lock = adminLoginSecurityService.lockState(ip);
        if (lock.blocked()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "ok", false,
                    "message", "输入错误次数过多，请 " + lock.remainingMinutes() + " 分钟后再试",
                    "locked", true,
                    "remainingMinutes", lock.remainingMinutes()
            ));
        }

        if (!checkPassword(password)) {
            adminLoginSecurityService.recordAttempt(ip, platform, password, false);
            var latest = adminLoginSecurityService.lockState(ip);
            if (latest.blocked()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                        "ok", false,
                        "message", "输入错误次数过多，请 " + latest.remainingMinutes() + " 分钟后再试",
                        "locked", true,
                        "remainingMinutes", latest.remainingMinutes()
                ));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ok", false, "message", "密码错误"));
        }
        adminLoginSecurityService.recordAttempt(ip, platform, null, true);
        String token = UUID.randomUUID().toString().replace("-", "");
        request.getSession().setAttribute(AdminAuthInterceptor.SESSION_ADMIN, Boolean.TRUE);
        request.getSession().setAttribute(AdminAuthInterceptor.SESSION_ADMIN_TOKEN, token);
        Map<String, Object> ok = new HashMap<>();
        ok.put("ok", true);
        ok.put("token", token);
        ok.put("redirect", safeAppRedirect(redirect));
        return ResponseEntity.ok(ok);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s != null) {
            s.invalidate();
        }
        return Map.of("ok", true);
    }

    @GetMapping("/groups")
    public List<Map<String, Object>> groups() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String g : qrService.listGroups()) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", g);
            row.put("qrActive", qrService.hasActiveQr(g));
            rows.add(row);
        }
        return rows;
    }

    @PostMapping("/groups/upload")
    public ResponseEntity<Map<String, Object>> uploadGroup(@RequestParam String group_name,
                                                             @RequestParam MultipartFile file,
                                                             HttpServletRequest request) {
        if (group_name == null || group_name.isBlank() || file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "请填写群组名并选择文件"));
        }
        try {
            qrService.saveGroupQr(group_name.trim(), file.getBytes());
            weChatNotifyService.sendAsync(group_name.trim(), "管理员更新群码", getClientIp(request), "管理员");
            return ResponseEntity.ok(Map.of("ok", true, "message", "更新成功"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "上传失败: " + e.getMessage()));
        }
    }

    @PostMapping("/groups/rename")
    public ResponseEntity<Map<String, Object>> rename(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String oldName = body != null ? body.get("old_name") : null;
        String newName = body != null ? body.get("new_name") : null;
        if (oldName == null || oldName.isBlank() || newName == null || newName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "参数无效"));
        }
        try {
            qrService.renameGroup(oldName, newName.trim());
            weChatNotifyService.sendAsync(newName.trim(), "管理员更名群码（" + oldName + " → " + newName + "）", getClientIp(request), "管理员");
            return ResponseEntity.ok(Map.of("ok", true, "message", "更名成功"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "更名失败"));
        }
    }

    @DeleteMapping("/groups/{groupName}")
    public ResponseEntity<Map<String, Object>> deleteGroup(@PathVariable String groupName, HttpServletRequest request) {
        try {
            qrService.deleteGroup(groupName);
            weChatNotifyService.sendAsync(groupName, "管理员删除群码", getClientIp(request), "管理员");
            return ResponseEntity.ok(Map.of("ok", true, "message", "删除成功"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "删除失败"));
        }
    }

    @GetMapping("/stats")
    public List<Map<String, Object>> stats() {
        var statsData = statsService.getStatsData();
        List<Map<String, Object>> list = new ArrayList<>();
        for (var e : statsData.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("groupName", e.getKey());
            item.put("trend", e.getValue().get("trend"));
            item.put("pie", e.getValue().get("pie"));
            list.add(item);
        }
        return list;
    }

    @GetMapping("/stats/data")
    public List<Map<String, Object>> statsData() {
        var statsData = statsService.getStatsData();
        List<Map<String, Object>> chartDataList = new ArrayList<>();
        for (var e : statsData.entrySet()) {
            Map<String, Object> chartEntry = new HashMap<>();
            chartEntry.put("groupName", e.getKey());
            chartEntry.put("trend", e.getValue().get("trend"));
            chartEntry.put("pie", e.getValue().get("pie"));
            chartDataList.add(chartEntry);
        }
        return chartDataList;
    }

    @GetMapping("/missing-groups")
    public Map<String, Object> missingGroups() {
        return Map.of(
                "summaryRows", missingGroupVisitService.summaryRows(),
                "recentRows", missingGroupVisitService.recentRows()
        );
    }

    @GetMapping("/login-monitor")
    public Map<String, Object> loginMonitor() {
        return Map.of("rows", adminLoginSecurityService.failedRows());
    }

    @GetMapping("/files")
    public List<String> files() {
        try {
            return java.nio.file.Files.list(qrService.getFilesDirPath())
                    .filter(java.nio.file.Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    @PostMapping("/files/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "请选择文件"));
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "文件名无效"));
        }
        filename = java.nio.file.Paths.get(filename).getFileName().toString();
        try {
            java.nio.file.Path dest = qrService.getFilePath(filename);
            java.nio.file.Files.createDirectories(dest.getParent());
            file.transferTo(dest.toFile());
            return ResponseEntity.ok(Map.of("ok", true, "message", "上传成功！文件已保存为: " + filename));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "上传失败: " + e.getMessage()));
        }
    }

    @PostMapping("/files/paste")
    public ResponseEntity<Map<String, Object>> paste(@RequestBody Map<String, String> body) {
        String pasteContent = body != null ? body.get("paste_content") : null;
        if (pasteContent == null || pasteContent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "请粘贴文本内容"));
        }
        String text = pasteContent.trim();
        int start = text.indexOf("##文本内容");
        if (start >= 0) {
            start = text.indexOf("\n", start) + 1;
            if (start <= 0) start = 0;
        } else {
            start = 0;
        }
        int end = text.indexOf("##", start);
        if (end > start) {
            text = text.substring(start, end).trim();
        } else if (start > 0) {
            text = text.substring(start).trim();
        }
        int created = 0;
        StringBuilder errors = new StringBuilder();
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int firstSpace = line.indexOf(' ');
            String filename;
            String content;
            if (firstSpace <= 0) {
                filename = line;
                content = "";
            } else {
                filename = line.substring(0, firstSpace).trim();
                content = line.substring(firstSpace + 1);
            }
            filename = java.nio.file.Paths.get(filename).getFileName().toString();
            if (filename.isBlank()) continue;
            try {
                java.nio.file.Path dest = qrService.getFilePath(filename);
                java.nio.file.Files.createDirectories(dest.getParent());
                java.nio.file.Files.writeString(dest, content, java.nio.charset.StandardCharsets.UTF_8);
                created++;
            } catch (IOException e) {
                if (errors.length() > 0) errors.append("；");
                errors.append(filename).append(": ").append(e.getMessage());
            }
        }
        if (created > 0) {
            return ResponseEntity.ok(Map.of("ok", true, "message",
                    "成功创建 " + created + " 个文件" + (errors.length() > 0 ? "，部分失败: " + errors : "")));
        }
        if (errors.length() > 0) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "创建失败: " + errors));
        }
        return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "未解析到有效行，请按「文件名 内容」每行一条粘贴"));
    }

    @PostMapping("/files/delete")
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestBody Map<String, String> body) {
        String filename = body != null ? body.get("filename") : null;
        String safe = filename != null ? java.nio.file.Paths.get(filename).getFileName().toString() : "";
        if (safe.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "文件名无效"));
        }
        try {
            java.nio.file.Path path = qrService.getFilePath(safe);
            if (java.nio.file.Files.exists(path)) {
                java.nio.file.Files.delete(path);
                return ResponseEntity.ok(Map.of("ok", true, "message", "删除成功: " + safe));
            }
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "文件不存在"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "删除失败: " + e.getMessage()));
        }
    }

    @GetMapping("/notice/configs")
    public List<Map<String, Object>> noticeConfigs() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (WeChatTemplate c : templateRepository.findAllByOrderByUpdatedAtDescIdDesc()) {
            list.add(templateToMap(c));
        }
        return list;
    }

    @GetMapping("/notice/configs/{id}")
    public ResponseEntity<Map<String, Object>> noticeConfig(@PathVariable Long id) {
        return templateRepository.findById(id)
                .map(c -> ResponseEntity.ok(templateToMap(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> templateToMap(WeChatTemplate c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("appid", c.getAppid());
        m.put("secret", c.getSecret());
        m.put("touser", c.getTouser());
        m.put("template_id", c.getTemplateId());
        m.put("template_data", c.getTemplateData());
        m.put("url", c.getUrl());
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);
        return m;
    }

    @PostMapping("/notice/action")
    public ResponseEntity<Map<String, Object>> noticeAction(@RequestBody Map<String, Object> body) {
        String action = body != null && body.get("action") != null ? String.valueOf(body.get("action")) : "save";

        if ("load".equals(action)) {
            var latest = templateRepository.findFirstByOrderByUpdatedAtDescIdDesc();
            if (latest.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "暂无可用配置，请先保存配置"));
            }
            WeChatTemplate c = latest.get();
            Map<String, Object> m = new HashMap<>(templateToMap(c));
            m.put("ok", true);
            m.put("message", "已加载最新配置");
            return ResponseEntity.ok(m);
        }

        if ("send".equals(action)) {
            Long configId = parseNullableLong(body != null ? body.get("config_id") : null);
            if (configId == null) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "请选择要发送的配置"));
            }
            var config = templateRepository.findById(configId);
            if (config.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "配置不存在"));
            }
            var c = config.get();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, String>> data = objectMapper.readValue(c.getTemplateData(), Map.class);
                var result = weChatApi.sendTemplateMessage(c.getAppid(), c.getSecret(), c.getTouser(), c.getTemplateId(), data, c.getUrl());
                if (result != null && result.get("msgid") != null) {
                    return ResponseEntity.ok(Map.of("ok", true, "message", "模板消息发送成功！消息ID: " + result.get("msgid")));
                }
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "模板消息发送失败，请检查配置信息"));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "发送失败: " + e.getMessage()));
            }
        }

        if ("save".equals(action)) {
            String appid = body != null && body.get("appid") != null ? String.valueOf(body.get("appid")) : "";
            String secret = body != null && body.get("secret") != null ? String.valueOf(body.get("secret")) : "";
            String touser = body != null && body.get("touser") != null ? String.valueOf(body.get("touser")) : "";
            String templateId = body != null && body.get("template_id") != null ? String.valueOf(body.get("template_id")) : "";
            String templateData = body != null && body.get("template_data") != null ? String.valueOf(body.get("template_data")) : "";
            String url = body != null && body.get("url") != null ? String.valueOf(body.get("url")) : "";
            Long configId = parseNullableLong(body != null ? body.get("config_id") : null);
            String name = body != null && body.get("name") != null ? String.valueOf(body.get("name")) : "";

            if (appid.isBlank() || secret.isBlank() || touser.isBlank() || templateId.isBlank() || templateData.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "请填写 AppID、Secret、用户ID、模板ID 和模板数据"));
            }
            try {
                objectMapper.readTree(templateData);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "模板数据JSON格式错误: " + e.getMessage()));
            }
            if (configId != null) {
                var opt = templateRepository.findById(configId);
                if (opt.isPresent()) {
                    var c = opt.get();
                    c.setAppid(appid);
                    c.setSecret(secret);
                    c.setTouser(touser);
                    c.setTemplateId(templateId);
                    c.setTemplateData(templateData);
                    c.setUrl(url.isBlank() ? null : url);
                    templateRepository.save(c);
                    return ResponseEntity.ok(Map.of("ok", true, "message", "配置更新成功", "id", c.getId()));
                }
                return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "配置不存在"));
            }
            WeChatTemplate c = new WeChatTemplate();
            c.setName(name.isBlank() ? "配置_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) : name);
            c.setAppid(appid);
            c.setSecret(secret);
            c.setTouser(touser);
            c.setTemplateId(templateId);
            c.setTemplateData(templateData);
            c.setUrl(url.isBlank() ? null : url);
            templateRepository.save(c);
            return ResponseEntity.ok(Map.of("ok", true, "message", "配置保存成功", "id", c.getId()));
        }

        return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "未知操作"));
    }

    @DeleteMapping("/notice/configs/{configId}")
    public ResponseEntity<Map<String, Object>> deleteNotice(@PathVariable Long configId) {
        var opt = templateRepository.findById(configId);
        if (opt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "配置不存在"));
        }
        templateRepository.delete(opt.get());
        return ResponseEntity.ok(Map.of("ok", true, "message", "配置删除成功"));
    }
}
