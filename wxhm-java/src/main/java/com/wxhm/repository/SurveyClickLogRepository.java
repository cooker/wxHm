package com.wxhm.repository;

import com.wxhm.entity.SurveyClickLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SurveyClickLogRepository extends JpaRepository<SurveyClickLog, Long> {

    long countByGroupNameAndDate(String groupName, String date);

    @Modifying
    @Query("DELETE FROM SurveyClickLog v WHERE v.date < :beforeDate")
    int deleteByDateBefore(String beforeDate);
}
