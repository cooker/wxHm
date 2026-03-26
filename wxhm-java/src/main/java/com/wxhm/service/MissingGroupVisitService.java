package com.wxhm.service;

import com.wxhm.entity.MissingGroupVisit;
import com.wxhm.repository.MissingGroupVisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 未创建群码链接访问统计服务（缓存保留 3 天）
 */
@Service
public class MissingGroupVisitService {

    private final MissingGroupVisitRepository repository;

    public MissingGroupVisitService(MissingGroupVisitRepository repository) {
        this.repository = repository;
    }

    private LocalDateTime cutoff() {
        return LocalDateTime.now().minusDays(3);
    }

    @Transactional
    public void logVisit(String groupName, String ip, String platform) {
        repository.deleteByCreatedAtBefore(cutoff());
        MissingGroupVisit v = new MissingGroupVisit();
        v.setGroupName(groupName);
        v.setIp(ip);
        v.setPlatform(platform);
        v.setCreatedAt(LocalDateTime.now());
        repository.save(v);
    }

    @Transactional
    public List<Map<String, Object>> summaryRows() {
        repository.deleteByCreatedAtBefore(cutoff());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] r : repository.summarizeByGroup()) {
            Map<String, Object> m = new HashMap<>();
            m.put("groupName", String.valueOf(r[0]));
            m.put("pv", ((Number) r[1]).longValue());
            m.put("uv", ((Number) r[2]).longValue());
            LocalDateTime last = (LocalDateTime) r[3];
            m.put("lastVisit", last.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            rows.add(m);
        }
        return rows;
    }

    @Transactional
    public List<Map<String, Object>> recentRows() {
        repository.deleteByCreatedAtBefore(cutoff());
        List<Map<String, Object>> rows = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (MissingGroupVisit v : repository.findTop200ByOrderByCreatedAtDesc()) {
            Map<String, Object> m = new HashMap<>();
            m.put("groupName", v.getGroupName());
            m.put("ip", v.getIp());
            m.put("platform", v.getPlatform());
            m.put("createdAt", v.getCreatedAt() != null ? v.getCreatedAt().format(fmt) : "");
            rows.add(m);
        }
        return rows;
    }
}

