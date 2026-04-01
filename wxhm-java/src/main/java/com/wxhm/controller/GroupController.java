package com.wxhm.controller;

import com.wxhm.service.QrService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * 兼容旧群链接；群码图片直出。
 */
@Controller
public class GroupController {

    private final QrService qrService;

    public GroupController(QrService qrService) {
        this.qrService = qrService;
    }

    @GetMapping("/group/{groupName}")
    public RedirectView groupLegacy(@PathVariable String groupName) {
        String encoded = UriUtils.encodePathSegment(groupName, StandardCharsets.UTF_8);
        return new RedirectView("/app/group/" + encoded);
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
