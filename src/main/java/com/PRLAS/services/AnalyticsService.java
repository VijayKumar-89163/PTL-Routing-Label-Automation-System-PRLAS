package com.PRLAS.services;

import com.PRLAS.dto.AnalyticsPeriodDTO;
import com.PRLAS.dto.AnalyticsSummaryDTO;
import com.PRLAS.dto.DailyTrendDTO;
import com.PRLAS.entity.ScanStatus;
import com.PRLAS.repository.ScanHistoryRepository;
import com.PRLAS.util.DateRangeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    @Autowired
    private ScanHistoryRepository scanHistoryRepository;

    private static final int TREND_DAYS = 14;

    public AnalyticsSummaryDTO getSummary() {
        LocalDateTime now = DateRangeUtil.now();

        return AnalyticsSummaryDTO.builder()
                .today(buildPeriod(DateRangeUtil.startOfToday(), now))
                .week(buildPeriod(DateRangeUtil.startOfWeek(), now))
                .month(buildPeriod(DateRangeUtil.startOfMonth(), now))
                .qtd(buildPeriod(DateRangeUtil.startOfQuarter(), now))
                .year(buildPeriod(DateRangeUtil.startOfYear(), now))
                .trend(buildTrend())
                .build();
    }

    private AnalyticsPeriodDTO buildPeriod(LocalDateTime from, LocalDateTime to) {
        long found = scanHistoryRepository.countByStatusAndScannedAtBetween(ScanStatus.FOUND, from, to);
        long notFound = scanHistoryRepository.countByStatusAndScannedAtBetween(ScanStatus.NOT_FOUND, from, to);

        return AnalyticsPeriodDTO.builder()
                .found(found)
                .notFound(notFound)
                .total(found + notFound)
                .build();
    }

    /**
     * Builds a continuous last-14-days trend (including zero-count days, so
     * the chart doesn't have gaps) from the raw grouped DB rows.
     */
    private List<DailyTrendDTO> buildTrend() {
        LocalDateTime from = LocalDate.now().minusDays(TREND_DAYS - 1L).atStartOfDay();
        List<Object[]> rows = scanHistoryRepository.findDailyTrendSince(from);

        // date -> [found, notFound]
        Map<String, long[]> dayCounts = new LinkedHashMap<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            String day = LocalDate.now().minusDays(TREND_DAYS - 1L - i).toString();
            dayCounts.put(day, new long[]{0, 0});
        }

        for (Object[] row : rows) {
            String day = String.valueOf(row[0]);
            String status = String.valueOf(row[1]);
            long count = ((Number) row[2]).longValue();

            long[] counts = dayCounts.get(day);
            if (counts == null) continue; // outside our 14-day window, ignore
            if ("FOUND".equals(status)) {
                counts[0] = count;
            } else if ("NOT_FOUND".equals(status)) {
                counts[1] = count;
            }
        }

        return dayCounts.entrySet().stream()
                .map(e -> DailyTrendDTO.builder()
                        .date(e.getKey())
                        .found(e.getValue()[0])
                        .notFound(e.getValue()[1])
                        .build())
                .toList();
    }
}
