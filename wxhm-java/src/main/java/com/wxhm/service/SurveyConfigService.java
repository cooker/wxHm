package com.wxhm.service;

import com.wxhm.entity.SurveyConfig;
import com.wxhm.repository.SurveyConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class SurveyConfigService {
    public static final String GLOBAL_KEY = "__GLOBAL__";
    public static final String DEFAULT_BUTTON_TEXT = "填写问卷";

    private final SurveyConfigRepository repository;

    public SurveyConfigService(SurveyConfigRepository repository) {
        this.repository = repository;
    }

    public String resolveSurveyUrl(String groupName) {
        return repository.findByGroupName(GLOBAL_KEY)
                .map(SurveyConfig::getSurveyUrl)
                .map(this::normalizeUrl)
                .orElse(null);
    }

    public String getGlobalUrl() {
        return repository.findByGroupName(GLOBAL_KEY)
                .map(SurveyConfig::getSurveyUrl)
                .map(this::normalizeUrl)
                .orElse("");
    }

    public String resolveButtonText(String groupName) {
        return repository.findByGroupName(GLOBAL_KEY)
                .map(SurveyConfig::getButtonText)
                .map(this::normalizeText)
                .orElse(DEFAULT_BUTTON_TEXT);
    }

    public String getGlobalButtonText() {
        return repository.findByGroupName(GLOBAL_KEY)
                .map(SurveyConfig::getButtonText)
                .map(this::normalizeText)
                .orElse(DEFAULT_BUTTON_TEXT);
    }

    public void saveGlobalUrl(String surveyUrl) {
        saveForGroup(GLOBAL_KEY, surveyUrl, null);
    }

    public void saveGlobalConfig(String surveyUrl, String buttonText) {
        saveForGroup(GLOBAL_KEY, surveyUrl, buttonText);
    }

    public void saveGroupUrl(String groupName, String surveyUrl) {
        saveForGroup(groupName, surveyUrl, null);
    }

    public void saveGroupConfig(String groupName, String surveyUrl, String buttonText) {
        saveForGroup(groupName, surveyUrl, buttonText);
    }

    public void deleteGroupUrl(String groupName) {
        repository.findByGroupName(groupName).ifPresent(repository::delete);
    }

    public List<SurveyConfig> listGroupOverrides() {
        return repository.findAll().stream()
                .filter(row -> !Objects.equals(GLOBAL_KEY, row.getGroupName()))
                .toList();
    }

    private void saveForGroup(String groupName, String surveyUrl, String buttonText) {
        String normalized = normalizeUrl(surveyUrl);
        String normalizedText = normalizeText(buttonText);
        if (normalized == null) {
            repository.findByGroupName(groupName).ifPresent(repository::delete);
            return;
        }
        SurveyConfig row = repository.findByGroupName(groupName).orElseGet(SurveyConfig::new);
        row.setGroupName(groupName);
        row.setSurveyUrl(normalized);
        row.setButtonText(normalizedText);
        repository.save(row);
    }

    private String normalizeUrl(String surveyUrl) {
        if (surveyUrl == null) return null;
        String v = surveyUrl.trim();
        if (v.isBlank()) return null;
        if (!(v.startsWith("http://") || v.startsWith("https://"))) {
            return null;
        }
        return v;
    }

    private String normalizeText(String buttonText) {
        if (buttonText == null) return null;
        String v = buttonText.trim();
        if (v.isBlank()) return null;
        if (v.length() > 50) {
            return v.substring(0, 50);
        }
        return v;
    }
}
