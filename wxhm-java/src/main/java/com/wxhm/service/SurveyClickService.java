package com.wxhm.service;

import com.wxhm.entity.SurveyClickLog;
import com.wxhm.repository.SurveyClickLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class SurveyClickService {

    private final SurveyClickLogRepository surveyClickLogRepository;

    public SurveyClickService(SurveyClickLogRepository surveyClickLogRepository) {
        this.surveyClickLogRepository = surveyClickLogRepository;
    }

    public void logClick(String groupName, String ip, String platform) {
        SurveyClickLog log = new SurveyClickLog();
        log.setGroupName(groupName);
        log.setDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        log.setIp(ip);
        log.setPlatform(platform);
        surveyClickLogRepository.save(log);
    }
}
