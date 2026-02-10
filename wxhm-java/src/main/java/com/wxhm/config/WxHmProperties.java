package com.wxhm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * wxHm 应用配置
 * 上传路径会解析为绝对路径，避免在 Tomcat 等容器中相对路径落到 work 目录导致 FileNotFoundException。
 */
@Component
@ConfigurationProperties(prefix = "wxhm")
public class WxHmProperties {

    private String uploadBase = "uploads";
    private String filesDir = "uploads/files";
    private String adminPassword = "admin321";
    private int expireDays = 7;
    private String githubUrl = "https://github.com/cooker/wxHm";

    /** 解析为绝对路径，相对路径基于进程工作目录（user.dir） */
    private static Path toAbsolutePath(String dir) {
        Path p = Paths.get(dir);
        if (!p.isAbsolute()) {
            p = Paths.get(System.getProperty("user.dir", ".")).resolve(p).normalize();
        }
        return p;
    }

    public Path getUploadBasePath() {
        return toAbsolutePath(uploadBase);
    }

    public Path getFilesDirPath() {
        return toAbsolutePath(filesDir);
    }

    public Path getGroupPath(String groupName) {
        return getUploadBasePath().resolve(groupName);
    }

    public String getUploadBase() {
        return uploadBase;
    }

    public void setUploadBase(String uploadBase) {
        this.uploadBase = uploadBase;
    }

    public String getFilesDir() {
        return filesDir;
    }

    public void setFilesDir(String filesDir) {
        this.filesDir = filesDir;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public int getExpireDays() {
        return expireDays;
    }

    public void setExpireDays(int expireDays) {
        this.expireDays = expireDays;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }
}
