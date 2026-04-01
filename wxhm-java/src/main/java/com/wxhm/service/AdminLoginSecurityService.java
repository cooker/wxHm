package com.wxhm.service;

import com.wxhm.entity.AdminLoginAttempt;
import com.wxhm.repository.AdminLoginAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理登录安全策略：
 * - 记录登录输入（重点记录失败密码）
 * - 同一 IP 30 分钟内输错 >=5 次，锁定 30 分钟
 */
@Service
public class AdminLoginSecurityService {

    private static final int MAX_FAILS = 5;
    private static final long WINDOW_MINUTES = 30;
    private static final long LOCK_MINUTES = 30;

    private final AdminLoginAttemptRepository repository;

    public AdminLoginSecurityService(AdminLoginAttemptRepository repository) {
        this.repository = repository;
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private LocalDateTime windowStart() {
        return now().minusMinutes(WINDOW_MINUTES);
    }

    @Transactional
    public void recordAttempt(String ip, String platform, String inputPassword, boolean success) {
        AdminLoginAttempt a = new AdminLoginAttempt();
        a.setIp(ip != null && !ip.isBlank() ? ip : "unknown");
        a.setPlatform(platform != null && !platform.isBlank() ? platform : "Other");
        a.setSuccess(success);
        // 仅失败时记录输入密码，成功场景不留明文
        a.setInputPassword(success ? null : (inputPassword == null ? "" : inputPassword));
        repository.save(a);
        repository.deleteByCreatedAtBefore(now().minusDays(7));
    }

    @Transactional(readOnly = true)
    public LockState lockState(String ip) {
        if (ip == null || ip.isBlank()) {
            return new LockState(false, 0, 0, null);
        }
        LocalDateTime after = windowStart();
        long fails = repository.countByIpAndSuccessFalseAndCreatedAtAfter(ip, after);
        if (fails < MAX_FAILS) {
            return new LockState(false, fails, MAX_FAILS, null);
        }
        Optional<AdminLoginAttempt> lastFail = repository.findFirstByIpAndSuccessFalseAndCreatedAtAfterOrderByCreatedAtDesc(ip, after);
        if (lastFail.isEmpty()) {
            return new LockState(false, fails, MAX_FAILS, null);
        }
        LocalDateTime lockUntil = lastFail.get().getCreatedAt().plusMinutes(LOCK_MINUTES);
        if (lockUntil.isAfter(now())) {
            return new LockState(true, fails, MAX_FAILS, lockUntil);
        }
        return new LockState(false, fails, MAX_FAILS, null);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> failedRows() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AdminLoginAttempt a : repository.findTop200BySuccessFalseOrderByCreatedAtDesc()) {
            Map<String, Object> m = new HashMap<>();
            m.put("ip", a.getIp());
            m.put("platform", a.getPlatform());
            m.put("inputPassword", a.getInputPassword());
            m.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().format(fmt) : "");
            rows.add(m);
        }
        return rows;
    }

    public record LockState(boolean blocked, long failCount, long threshold, LocalDateTime lockUntil) {
        public long remainingMinutes() {
            if (!blocked || lockUntil == null) return 0;
            long mins = Duration.between(LocalDateTime.now(), lockUntil).toMinutes();
            return Math.max(1, mins);
        }
    }
}
