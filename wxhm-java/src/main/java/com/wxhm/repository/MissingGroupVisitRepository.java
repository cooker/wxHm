package com.wxhm.repository;

import com.wxhm.entity.MissingGroupVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface MissingGroupVisitRepository extends JpaRepository<MissingGroupVisit, Long> {

    @Modifying
    @Query("DELETE FROM MissingGroupVisit m WHERE m.createdAt < :before")
    int deleteByCreatedAtBefore(LocalDateTime before);

    @Query("""
            SELECT m.groupName, COUNT(m), COUNT(DISTINCT m.ip), MAX(m.createdAt)
            FROM MissingGroupVisit m
            GROUP BY m.groupName
            ORDER BY COUNT(m) DESC, MAX(m.createdAt) DESC
            """)
    List<Object[]> summarizeByGroup();

    List<MissingGroupVisit> findTop200ByOrderByCreatedAtDesc();
}

