package com.wxhm.repository;

import com.wxhm.entity.AdminLoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AdminLoginAttemptRepository extends JpaRepository<AdminLoginAttempt, Long> {

    long countByIpAndSuccessFalseAndCreatedAtAfter(String ip, LocalDateTime after);

    Optional<AdminLoginAttempt> findFirstByIpAndSuccessFalseAndCreatedAtAfterOrderByCreatedAtDesc(String ip, LocalDateTime after);

    List<AdminLoginAttempt> findTop200BySuccessFalseOrderByCreatedAtDesc();

    long deleteByCreatedAtBefore(LocalDateTime before);
}
