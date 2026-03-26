package com.wxhm.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 未创建群码链接访问记录（仅保留最近 3 天）
 */
@Entity
@Table(name = "missing_group_visit", indexes = {
        @Index(name = "idx_mgv_group_created", columnList = "group_name, created_at"),
        @Index(name = "idx_mgv_created", columnList = "created_at")
})
public class MissingGroupVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(length = 50)
    private String ip;

    @Column(length = 20)
    private String platform;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

