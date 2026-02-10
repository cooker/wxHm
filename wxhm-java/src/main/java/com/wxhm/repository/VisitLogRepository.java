package com.wxhm.repository;

import com.wxhm.entity.VisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VisitLogRepository extends JpaRepository<VisitLog, Long> {

    long countByGroupNameAndDate(String groupName, String date);

    @Query("SELECT COUNT(DISTINCT v.ip) FROM VisitLog v WHERE v.groupName = :groupName AND v.date = :date")
    long countDistinctIpByGroupNameAndDate(String groupName, String date);

    @Query("SELECT v.platform, COUNT(v) FROM VisitLog v WHERE v.groupName = :groupName AND v.date = :date GROUP BY v.platform")
    List<Object[]> countByGroupNameAndDateGroupByPlatform(String groupName, String date);

    @Modifying
    @Query("DELETE FROM VisitLog v WHERE v.date < :beforeDate")
    int deleteByDateBefore(String beforeDate);
}
