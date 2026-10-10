package com.PRLAS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class DailyTrendDTO {
    private String date; // yyyy-MM-dd
    private long found;
    private long notFound;
}
