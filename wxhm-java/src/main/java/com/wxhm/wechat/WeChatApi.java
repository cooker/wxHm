package com.wxhm.wechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信公众号 API 工具类
 */
@Component
public class WeChatApi {

    private static final String BASE_URL = "https://api.weixin.qq.com";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private record TokenEntry(String token, long expiresAt) {}
    private final Map<String, TokenEntry> tokenCache = new ConcurrentHashMap<>();

    public String getAccessToken(String appid, String secret, boolean forceRefresh) {
        String key = appid + ":" + secret;
        TokenEntry entry = tokenCache.get(key);
        if (!forceRefresh && entry != null && System.currentTimeMillis() < entry.expiresAt) {
            return entry.token;
        }

        String url = BASE_URL + "/cgi-bin/token?grant_type=client_credential&appid=" + appid + "&secret=" + secret;
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
        if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
            try {
                JsonNode node = objectMapper.readTree(resp.getBody());
                if (node.has("access_token")) {
                    String token = node.get("access_token").asText();
                    int expiresIn = node.has("expires_in") ? node.get("expires_in").asInt() : 7200;
                    long expiresAt = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
                    tokenCache.put(key, new TokenEntry(token, expiresAt));
                    return token;
                }
            } catch (Exception e) {
                System.err.println("获取access_token失败: " + e.getMessage());
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> sendTemplateMessage(String appid, String secret,
                                                   String touser, String templateId,
                                                   Map<String, Map<String, String>> data,
                                                   String url) {
        String token = getAccessToken(appid, secret, false);
        if (token == null) {
            return null;
        }

        String apiUrl = BASE_URL + "/cgi-bin/message/template/send?access_token=" + token;
        Map<String, Object> payload = new HashMap<>();
        payload.put("touser", touser);
        payload.put("template_id", templateId);
        payload.put("data", data);
        if (url != null && !url.isBlank()) {
            payload.put("url", url);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(apiUrl, entity, String.class);

        if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
            try {
                return objectMapper.readValue(resp.getBody(), Map.class);
            } catch (Exception e) {
                System.err.println("解析模板消息响应失败: " + e.getMessage());
            }
        }
        return null;
    }
}
