package com.wxhm.service;

import com.wxhm.config.WxHmProperties;
import com.wxhm.entity.WeChatTemplate;
import com.wxhm.repository.WeChatTemplateRepository;
import com.wxhm.wechat.WeChatApi;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信公众号模板消息异步推送
 */
@Service
public class WeChatNotifyService {

    private final WeChatTemplateRepository templateRepository;
    private final WeChatApi weChatApi;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeChatNotifyService(WeChatTemplateRepository templateRepository, WeChatApi weChatApi) {
        this.templateRepository = templateRepository;
        this.weChatApi = weChatApi;
    }

    @Async
    public void sendAsync(String groupName, String action, String serverIp, String userLabel) {
        WeChatTemplate config = templateRepository.findFirstByOrderByUpdatedAtDescIdDesc().orElse(null);
        if (config == null) {
            return;
        }
        try {
            Map<String, Map<String, String>> data = new HashMap<>();
            data.put("group", Map.of("value", groupName != null ? groupName : ""));
            data.put("action", Map.of("value", action != null ? action : ""));
            data.put("server", Map.of("value", serverIp != null ? serverIp : ""));
            data.put("user", Map.of("value", userLabel != null ? userLabel : ""));
            data.put("time", Map.of("value", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

            var result = weChatApi.sendTemplateMessage(
                    config.getAppid(), config.getSecret(),
                    config.getTouser(), config.getTemplateId(),
                    data, config.getUrl()
            );
            if (result == null || (result.get("errcode") != null && !Integer.valueOf(0).equals(result.get("errcode")))) {
                System.err.println("自动发送微信模板消息失败");
            }
        } catch (Exception e) {
            System.err.println("自动发送微信模板消息失败: " + e.getMessage());
        }
    }
}
