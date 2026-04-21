package com.wxhm.controller;

import com.wxhm.config.WxHmProperties;
import com.wxhm.service.MissingGroupVisitService;
import com.wxhm.service.QrService;
import com.wxhm.service.SurveyClickService;
import com.wxhm.service.SurveyConfigService;
import com.wxhm.util.PlatformUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 公开 JSON 接口（无需登录）
 */
@RestController
@RequestMapping("/api/public")
public class ApiPublicController {

    private final WxHmProperties properties;
    private final QrService qrService;
    private final MissingGroupVisitService missingGroupVisitService;
    private final SurveyConfigService surveyConfigService;
    private final SurveyClickService surveyClickService;

    public ApiPublicController(WxHmProperties properties, QrService qrService,
                               MissingGroupVisitService missingGroupVisitService,
                               SurveyConfigService surveyConfigService,
                               SurveyClickService surveyClickService) {
        this.properties = properties;
        this.qrService = qrService;
        this.missingGroupVisitService = missingGroupVisitService;
        this.surveyConfigService = surveyConfigService;
        this.surveyClickService = surveyClickService;
    }

    @GetMapping("/home")
    public Map<String, Object> home() {
        return Map.of("githubUrl", properties.getGithubUrl());
    }

    @GetMapping("/group/{groupName}")
    public Map<String, Object> group(@PathVariable String groupName, HttpServletRequest request) {
        boolean groupExists = qrService.groupExists(groupName);
        String qrFile = groupExists ? qrService.getActiveQr(groupName) : null;

        String userAgent = request.getHeader("User-Agent");
        String platform = PlatformUtils.parsePlatform(userAgent);
        String clientIp = qrService.getClientIp(request);

        long todayVisitCount = 0;
        if (groupExists) {
            qrService.logVisit(groupName, clientIp, platform);
            todayVisitCount = qrService.countTodayVisits(groupName);
        } else {
            missingGroupVisitService.logVisit(groupName, clientIp, platform);
        }

        String wsrvUrl = "";
        if (qrFile != null) {
            String host = request.getHeader("Host");
            if (host == null) host = "localhost:8092";
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isBlank()) scheme = "https";
            String rawUrl = scheme + "://" + host + "/uploads/" + groupName + "/" + qrFile;
            wsrvUrl = rawUrl + "?we=1&v=" + System.currentTimeMillis() / 1000;
        }

        Map<String, Object> m = new HashMap<>();
        m.put("groupName", groupName);
        m.put("groupExists", groupExists);
        m.put("qrFile", qrFile);
        m.put("wsrvUrl", wsrvUrl);
        m.put("todayVisitCount", todayVisitCount);
        m.put("surveyUrl", surveyConfigService.getGlobalUrl());
        m.put("surveyButtonText", surveyConfigService.getGlobalButtonText());
        return m;
    }

    @PostMapping("/group/{groupName}/survey-click")
    public Map<String, Object> surveyClick(@PathVariable String groupName, HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String platform = PlatformUtils.parsePlatform(userAgent);
        String clientIp = qrService.getClientIp(request);
        surveyClickService.logClick(groupName, clientIp, platform);
        return Map.of("ok", true);
    }
}
