package com.PRLAS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class AnalyticsPeriodDTO {
    private long found;
    private long notFound;
    private long total;
}
