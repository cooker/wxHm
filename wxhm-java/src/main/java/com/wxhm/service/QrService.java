package com.wxhm.service;

import com.wxhm.config.WxHmProperties;
import com.wxhm.entity.VisitLog;
import com.wxhm.repository.VisitLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 群码服务：获取有效二维码、上传处理、过期清理
 */
@Service
public class QrService {

    private final WxHmProperties properties;
    private final VisitLogRepository visitLogRepository;
    private final WeChatNotifyService weChatNotifyService;

    private static final List<String> IMAGE_EXTENSIONS = List.of(".webp", ".png", ".jpg", ".jpeg");
    private static final long SECONDS_PER_DAY = 86400;

    public QrService(WxHmProperties properties, VisitLogRepository visitLogRepository,
                     WeChatNotifyService weChatNotifyService) {
        this.properties = properties;
        this.visitLogRepository = visitLogRepository;
        this.weChatNotifyService = weChatNotifyService;
        ensureDirectories();
    }

    private void ensureDirectories() {
        try {
            Files.createDirectories(properties.getUploadBasePath());
            Files.createDirectories(properties.getFilesDirPath());
        } catch (IOException e) {
            throw new RuntimeException("创建上传目录失败", e);
        }
    }

    /**
     * 获取群组下最近 7 天内最新的有效二维码文件名，过期文件自动删除并通知
     */
    public String getActiveQr(String groupName) {
        Path groupPath = properties.getGroupPath(groupName);
        if (!Files.isDirectory(groupPath)) {
            return null;
        }

        long now = System.currentTimeMillis();
        long expireSeconds = (long) properties.getExpireDays() * SECONDS_PER_DAY;

        try (Stream<Path> stream = Files.list(groupPath)) {
            List<Path> imageFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> IMAGE_EXTENSIONS.stream()
                            .anyMatch(ext -> p.getFileName().toString().toLowerCase().endsWith(ext)))
                    .sorted(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime((Path) p).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    }).reversed())
                    .toList();

            for (Path path : imageFiles) {
                long ageSeconds = (now - Files.getLastModifiedTime(path).toMillis()) / 1000;
                if (ageSeconds < expireSeconds) {
                    return path.getFileName().toString();
                } else {
                    Files.delete(path);
                    weChatNotifyService.sendAsync(groupName, "群码过期自动清理", "", "系统");
                }
            }
        } catch (IOException e) {
            System.err.println("读取群码目录失败: " + e.getMessage());
        }
        return null;
    }

    /**
     * 保存上传的群码图片，优先转为 WebP；Mac ARM64 等环境下 native 库不可用时自动回退为 PNG。
     */
    public void saveGroupQr(String groupName, byte[] imageBytes) throws IOException {
        Path groupPath = properties.getGroupPath(groupName);
        Files.createDirectories(groupPath);

        BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("无法解析图片");
        }
        // 统一转为 RGB（含 RGBA、P、带透明通道等），避免部分格式在部分平台上写入异常
        image = ensureRgb(image);

        String baseName = "qr_" + System.currentTimeMillis();
        Path outputPath = groupPath.resolve(baseName + ".webp");
        try {
            if (ImageIO.write(image, "webp", outputPath.toFile())) {
                return;
            }
        } catch (Throwable t) {
            // WebP 依赖 native 库，Mac ARM64 上可能为 x86_64 导致 UnsatisfiedLinkError，回退为 PNG
            System.err.println("WebP 写入失败，回退为 PNG: " + t.getMessage());
        }
        outputPath = groupPath.resolve(baseName + ".png");
        if (!ImageIO.write(image, "png", outputPath.toFile())) {
            throw new IOException("无法保存为 PNG");
        }
    }

    private static BufferedImage ensureRgb(BufferedImage image) {
        if (image.getType() == BufferedImage.TYPE_INT_RGB && image.getColorModel().getNumColorComponents() == 3) {
            return image;
        }
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    public List<String> listGroups() {
        try (Stream<Path> stream = Files.list(properties.getUploadBasePath())) {
            return stream
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> !"files".equals(name))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    public void renameGroup(String oldName, String newName) throws IOException {
        Files.move(properties.getGroupPath(oldName), properties.getGroupPath(newName));
    }

    public void deleteGroup(String groupName) throws IOException {
        Path path = properties.getGroupPath(groupName);
        if (Files.exists(path)) {
            deleteRecursively(path);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                stream.forEach(p -> {
                    try {
                        deleteRecursively(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        Files.delete(path);
    }

    public String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public void logVisit(String groupName, String ip, String platform) {
        VisitLog log = new VisitLog();
        log.setGroupName(groupName);
        log.setDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        log.setIp(ip);
        log.setPlatform(platform);
        visitLogRepository.save(log);
    }

    public Path getGroupQrPath(String groupName, String filename) {
        return properties.getGroupPath(groupName).resolve(Paths.get(filename).getFileName().toString());
    }

    public Path getFilesDirPath() {
        return properties.getFilesDirPath();
    }

    public Path getFilePath(String filename) {
        return properties.getFilesDirPath().resolve(Paths.get(filename).getFileName().toString());
    }
}
