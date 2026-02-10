package com.wxhm.service;

import com.wxhm.config.WxHmProperties;
import com.wxhm.repository.VisitLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统计服务：7 日 PV/UV 趋势、今日设备占比
 */
@Service
public class StatsService {

    private final VisitLogRepository visitLogRepository;
    private final QrService qrService;
    private final WxHmProperties properties;

    public StatsService(VisitLogRepository visitLogRepository, QrService qrService, WxHmProperties properties) {
        this.visitLogRepository = visitLogRepository;
        this.qrService = qrService;
        this.properties = properties;
    }

    @Transactional
    public Map<String, Map<String, Object>> getStatsData() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<String> dates = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            dates.add(LocalDate.now().minusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        String beforeDate = LocalDate.now().minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE);
        visitLogRepository.deleteByDateBefore(beforeDate);

        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (String group : qrService.listGroups()) {
            List<Map<String, Object>> trend = new ArrayList<>();
            for (String d : dates) {
                long pv = visitLogRepository.countByGroupNameAndDate(group, d);
                long uv = visitLogRepository.countDistinctIpByGroupNameAndDate(group, d);
                trend.add(Map.of("date", d, "pv", pv, "uv", uv));
            }

            List<Map<String, Object>> pie = visitLogRepository
                    .countByGroupNameAndDateGroupByPlatform(group, today)
                    .stream()
                    .map(row -> Map.<String, Object>of("name", row[0], "value", row[1]))
                    .collect(Collectors.toList());
            if (pie.isEmpty()) {
                pie.add(Map.of("name", "无数据", "value", 0));
            }

            result.put(group, Map.of("trend", trend, "pie", pie));
        }
        return result;
    }
}
