package com.wxhm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 访问日志
 */
@Entity
@Table(name = "visit_log", indexes = {
        @Index(name = "idx_group_date", columnList = "group_name, date")
})
public class VisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", length = 50)
    private String groupName;

    @Column(length = 10)
    private String date;

    @Column(length = 50)
    private String ip;

    @Column(length = 20)
    private String platform;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // getters and setters
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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
