package com.wxhm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wxhm.config.WxHmProperties;
import com.wxhm.entity.WeChatTemplate;
import com.wxhm.repository.WeChatTemplateRepository;
import com.wxhm.service.QrService;
import com.wxhm.service.StatsService;
import com.wxhm.service.WeChatNotifyService;
import com.wxhm.wechat.WeChatApi;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 管理后台：上传群码、更名、删除、统计、公众号配置、自定义文件
 */
@Controller
public class AdminController {

    private final WxHmProperties properties;
    private final QrService qrService;
    private final StatsService statsService;
    private final WeChatNotifyService weChatNotifyService;
    private final WeChatTemplateRepository templateRepository;
    private final WeChatApi weChatApi;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminController(WxHmProperties properties, QrService qrService, StatsService statsService,
                           WeChatNotifyService weChatNotifyService, WeChatTemplateRepository templateRepository,
                           WeChatApi weChatApi) {
        this.properties = properties;
        this.qrService = qrService;
        this.statsService = statsService;
        this.weChatNotifyService = weChatNotifyService;
        this.templateRepository = templateRepository;
        this.weChatApi = weChatApi;
    }

    private boolean checkPassword(String password) {
        return properties.getAdminPassword().equals(password);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ==================== 首页 ====================
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("githubUrl", properties.getGithubUrl());
        return "home";
    }

    // ==================== 管理中心 ====================
    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("groups", qrService.listGroups());
        return "admin";
    }

    @PostMapping("/admin")
    public String adminPost(@RequestParam(required = false) String password,
                            @RequestParam(required = false) String group_name,
                            @RequestParam(required = false) MultipartFile file,
                            HttpServletRequest request,
                            RedirectAttributes ra) {
        if (!checkPassword(password)) {
            ra.addFlashAttribute("message", "密码错误");
            return "redirect:/admin";
        }
        if (group_name != null && !group_name.isBlank() && file != null && !file.isEmpty()) {
            try {
                qrService.saveGroupQr(group_name.trim(), file.getBytes());
                ra.addFlashAttribute("message", "更新成功");
                String ip = getClientIp(request);
                weChatNotifyService.sendAsync(group_name.trim(), "管理员更新群码", ip, "管理员");
            } catch (IOException e) {
                ra.addFlashAttribute("message", "上传失败: " + e.getMessage());
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/rename")
    public String rename(@RequestParam String password,
                         @RequestParam String old_name,
                         @RequestParam String new_name,
                         HttpServletRequest request,
                         RedirectAttributes ra) {
        if (checkPassword(password) && new_name != null && !new_name.isBlank()) {
            try {
                qrService.renameGroup(old_name, new_name.trim());
                String ip = getClientIp(request);
                weChatNotifyService.sendAsync(new_name.trim(), "管理员更名群码（" + old_name + " → " + new_name + "）", ip, "管理员");
            } catch (IOException e) {
                ra.addFlashAttribute("message", "更名失败");
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/delete/{groupName}")
    public String delete(@RequestParam String password,
                         @PathVariable String groupName,
                         HttpServletRequest request,
                         RedirectAttributes ra) {
        if (checkPassword(password)) {
            try {
                qrService.deleteGroup(groupName);
                String ip = getClientIp(request);
                weChatNotifyService.sendAsync(groupName, "管理员删除群码", ip, "管理员");
            } catch (IOException e) {
                ra.addFlashAttribute("message", "删除失败");
            }
        }
        return "redirect:/admin";
    }

    // ==================== 统计看板 ====================
    @GetMapping("/admin/stats")
    public String stats(Model model) throws com.fasterxml.jackson.core.JsonProcessingException {
        var statsData = statsService.getStatsData();
        var statsList = new java.util.ArrayList<Map<String, Object>>();
        for (var e : statsData.entrySet()) {
            var item = new java.util.HashMap<String, Object>();
            item.put("groupName", e.getKey());
            item.put("trendJson", objectMapper.writeValueAsString(e.getValue().get("trend")));
            item.put("pieJson", objectMapper.writeValueAsString(e.getValue().get("pie")));
            statsList.add(item);
        }
        model.addAttribute("statsList", statsList);
        return "stats";
    }

    // ==================== 自定义文件 ====================
    @GetMapping("/admin/upload-file")
    public String uploadPage(Model model) {
        try {
            List<String> files = java.nio.file.Files.list(qrService.getFilesDirPath())
                    .filter(java.nio.file.Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
            model.addAttribute("files", files);
        } catch (IOException e) {
            model.addAttribute("files", List.<String>of());
        }
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam String password,
                             @RequestParam MultipartFile file,
                             RedirectAttributes ra) {
        if (!checkPassword(password)) {
            ra.addFlashAttribute("message", "密码错误");
            return "redirect:/admin/upload-file";
        }
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("message", "请选择文件");
            return "redirect:/admin/upload-file";
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            ra.addFlashAttribute("message", "文件名无效");
            return "redirect:/admin/upload-file";
        }
        filename = java.nio.file.Paths.get(filename).getFileName().toString();
        try {
            java.nio.file.Path dest = qrService.getFilePath(filename);
            java.nio.file.Files.createDirectories(dest.getParent());
            file.transferTo(dest.toFile());
            ra.addFlashAttribute("message", "上传成功！文件已保存为: " + filename);
        } catch (IOException e) {
            ra.addFlashAttribute("message", "上传失败: " + e.getMessage());
        }
        return "redirect:/admin/upload-file";
    }

    @PostMapping("/admin/upload-file/delete")
    public String deleteFile(@RequestParam String password,
                             @RequestParam String filename,
                             RedirectAttributes ra) {
        if (!checkPassword(password)) {
            ra.addFlashAttribute("message", "密码错误");
            return "redirect:/admin/upload-file";
        }
        String safe = java.nio.file.Paths.get(filename).getFileName().toString();
        if (safe.isBlank()) {
            ra.addFlashAttribute("message", "文件名无效");
            return "redirect:/admin/upload-file";
        }
        try {
            java.nio.file.Path path = qrService.getFilePath(safe);
            if (java.nio.file.Files.exists(path)) {
                java.nio.file.Files.delete(path);
                ra.addFlashAttribute("message", "删除成功: " + safe);
            } else {
                ra.addFlashAttribute("message", "文件不存在");
            }
        } catch (IOException e) {
            ra.addFlashAttribute("message", "删除失败: " + e.getMessage());
        }
        return "redirect:/admin/upload-file";
    }

    // ==================== 自定义文件根路径访问 ====================
    @GetMapping("/{filename}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> serveFile(@PathVariable String filename) {
        if (filename.contains("/") || List.of("admin", "group", "uploads", "upload").contains(filename)) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        String safe = java.nio.file.Paths.get(filename).getFileName().toString();
        try {
            java.nio.file.Path path = qrService.getFilePath(safe);
            if (java.nio.file.Files.exists(path) && java.nio.file.Files.isRegularFile(path)) {
                org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(path.toUri());
                return org.springframework.http.ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safe + "\"")
                        .body(resource);
            }
        } catch (Exception ignored) {
        }
        return org.springframework.http.ResponseEntity.notFound().build();
    }

    // ==================== 微信公众号配置 ====================
    @GetMapping("/admin/notice")
    public String notice(@RequestParam(required = false) Long edit, Model model) {
        model.addAttribute("configs", templateRepository.findAllByOrderByUpdatedAtDescIdDesc());
        if (edit != null) {
            templateRepository.findById(edit).ifPresent(c -> model.addAttribute("editConfig", c));
        }
        return "notice";
    }

    @PostMapping("/admin/notice")
    public String noticePost(@RequestParam String password,
                             @RequestParam(defaultValue = "save") String action,
                             @RequestParam(required = false) Long config_id,
                             @RequestParam(required = false) String name,
                             @RequestParam(required = false) String appid,
                             @RequestParam(required = false) String secret,
                             @RequestParam(required = false) String touser,
                             @RequestParam(required = false) String template_id,
                             @RequestParam(required = false) String template_data,
                             @RequestParam(required = false) String url,
                             RedirectAttributes ra) {
        if (!checkPassword(password)) {
            ra.addFlashAttribute("message", "密码错误");
            return "redirect:/admin/notice";
        }

        if ("load".equals(action)) {
            var latest = templateRepository.findFirstByOrderByUpdatedAtDescIdDesc();
            if (latest.isEmpty()) {
                ra.addFlashAttribute("message", "暂无可用配置，请先保存配置");
            } else {
                ra.addFlashAttribute("message", "已加载最新配置：" + latest.get().getName());
                return "redirect:/admin/notice?edit=" + latest.get().getId();
            }
            return "redirect:/admin/notice";
        }

        if ("send".equals(action)) {
            if (config_id == null) {
                ra.addFlashAttribute("message", "请选择要发送的配置");
                return "redirect:/admin/notice";
            }
            var config = templateRepository.findById(config_id);
            if (config.isEmpty()) {
                ra.addFlashAttribute("message", "配置不存在");
                return "redirect:/admin/notice";
            }
            var c = config.get();
            try {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, String>> data = objectMapper.readValue(c.getTemplateData(), Map.class);
                var result = weChatApi.sendTemplateMessage(c.getAppid(), c.getSecret(), c.getTouser(), c.getTemplateId(), data, c.getUrl());
                if (result != null && result.get("msgid") != null) {
                    ra.addFlashAttribute("message", "模板消息发送成功！消息ID: " + result.get("msgid"));
                } else {
                    ra.addFlashAttribute("message", "模板消息发送失败，请检查配置信息");
                }
            } catch (Exception e) {
                ra.addFlashAttribute("message", "发送失败: " + e.getMessage());
            }
            return "redirect:/admin/notice";
        }

        if ("save".equals(action)) {
            if (appid == null || appid.isBlank() || secret == null || secret.isBlank() ||
                    touser == null || touser.isBlank() || template_id == null || template_id.isBlank() ||
                    template_data == null || template_data.isBlank()) {
                ra.addFlashAttribute("message", "请填写 AppID、Secret、用户ID、模板ID 和模板数据");
                return "redirect:/admin/notice";
            }
            try {
                objectMapper.readTree(template_data);
            } catch (Exception e) {
                ra.addFlashAttribute("message", "模板数据JSON格式错误: " + e.getMessage());
                return "redirect:/admin/notice";
            }
            if (config_id != null) {
                var opt = templateRepository.findById(config_id);
                if (opt.isPresent()) {
                    var c = opt.get();
                    c.setAppid(appid);
                    c.setSecret(secret);
                    c.setTouser(touser);
                    c.setTemplateId(template_id);
                    c.setTemplateData(template_data);
                    c.setUrl(url != null && !url.isBlank() ? url : null);
                    templateRepository.save(c);
                    ra.addFlashAttribute("message", "配置更新成功");
                } else {
                    ra.addFlashAttribute("message", "配置不存在");
                }
            } else {
                WeChatTemplate c = new WeChatTemplate();
                c.setName(name != null && !name.isBlank() ? name : "配置_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                c.setAppid(appid);
                c.setSecret(secret);
                c.setTouser(touser);
                c.setTemplateId(template_id);
                c.setTemplateData(template_data);
                c.setUrl(url != null && !url.isBlank() ? url : null);
                templateRepository.save(c);
                ra.addFlashAttribute("message", "配置保存成功");
            }
        }
        return "redirect:/admin/notice";
    }

    @PostMapping("/admin/notice/delete/{configId}")
    public String deleteNoticeConfig(@RequestParam String password, @PathVariable Long configId, RedirectAttributes ra) {
        if (!checkPassword(password)) {
            ra.addFlashAttribute("message", "密码错误");
            return "redirect:/admin/notice";
        }
        templateRepository.findById(configId).ifPresentOrElse(
                c -> {
                    templateRepository.delete(c);
                    ra.addFlashAttribute("message", "配置删除成功");
                },
                () -> ra.addFlashAttribute("message", "配置不存在")
        );
        return "redirect:/admin/notice";
    }
}
