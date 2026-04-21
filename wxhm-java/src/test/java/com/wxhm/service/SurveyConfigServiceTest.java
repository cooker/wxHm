package com.wxhm.service;

import com.wxhm.entity.SurveyConfig;
import com.wxhm.repository.SurveyConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyConfigServiceTest {

    @Mock
    private SurveyConfigRepository repository;

    @InjectMocks
    private SurveyConfigService service;

    @Test
    void shouldIgnoreGroupConfigAndUseGlobalUrl() {
        SurveyConfig global = new SurveyConfig();
        global.setGroupName(SurveyConfigService.GLOBAL_KEY);
        global.setSurveyUrl("https://example.com/global-survey");
        global.setButtonText("全局问卷");

        when(repository.findByGroupName(SurveyConfigService.GLOBAL_KEY)).thenReturn(Optional.of(global));

        String url = service.resolveSurveyUrl("dev-group");
        String text = service.resolveButtonText("dev-group");

        assertEquals("https://example.com/global-survey", url);
        assertEquals("全局问卷", text);
    }

    @Test
    void shouldReturnGlobalUrl() {
        SurveyConfig global = new SurveyConfig();
        global.setGroupName(SurveyConfigService.GLOBAL_KEY);
        global.setSurveyUrl("https://example.com/global-survey");
        global.setButtonText("全局问卷");

        when(repository.findByGroupName(SurveyConfigService.GLOBAL_KEY)).thenReturn(Optional.of(global));

        String url = service.resolveSurveyUrl("ops-group");
        String text = service.resolveButtonText("ops-group");

        assertEquals("https://example.com/global-survey", url);
        assertEquals("全局问卷", text);
    }

    @Test
    void shouldReturnNullWhenNoConfigAvailable() {
        when(repository.findByGroupName(SurveyConfigService.GLOBAL_KEY)).thenReturn(Optional.empty());

        String url = service.resolveSurveyUrl("none-group");

        assertNull(url);
    }

    @Test
    void shouldUseDefaultButtonTextWhenNoConfigText() {
        SurveyConfig global = new SurveyConfig();
        global.setGroupName(SurveyConfigService.GLOBAL_KEY);
        global.setSurveyUrl("https://example.com/global-survey");

        when(repository.findByGroupName(SurveyConfigService.GLOBAL_KEY)).thenReturn(Optional.of(global));

        String text = service.resolveButtonText("qa-group");

        assertEquals("填写问卷", text);
    }
}
