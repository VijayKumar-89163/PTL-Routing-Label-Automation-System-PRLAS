package com.PRLAS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class AnalyticsSummaryDTO {
    private AnalyticsPeriodDTO today;
    private AnalyticsPeriodDTO week;
    private AnalyticsPeriodDTO month;
    private AnalyticsPeriodDTO qtd;
    private AnalyticsPeriodDTO year;
    private List<DailyTrendDTO> trend; // last 14 days, for the chart
}
