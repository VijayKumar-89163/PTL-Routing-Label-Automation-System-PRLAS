package com.PRLAS.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class MasterDataDeletePreviewDTO {
    private long matchingRecords;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
