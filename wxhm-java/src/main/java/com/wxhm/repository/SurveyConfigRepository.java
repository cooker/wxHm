package com.wxhm.repository;

import com.wxhm.entity.SurveyConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyConfigRepository extends JpaRepository<SurveyConfig, Long> {
    Optional<SurveyConfig> findByGroupName(String groupName);
}
