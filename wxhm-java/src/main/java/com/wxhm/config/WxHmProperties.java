package com.wxhm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * wxHm 应用配置
 */
@Component
@ConfigurationProperties(prefix = "wxhm")
public class WxHmProperties {

    private String uploadBase = "uploads";
    private String filesDir = "uploads/files";
    private String adminPassword = "admin321";
    private int expireDays = 7;
    private String githubUrl = "https://github.com/cooker/wxHm";

    public Path getUploadBasePath() {
        return Paths.get(uploadBase);
    }

    public Path getFilesDirPath() {
        return Paths.get(filesDir);
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
