package com.PRLAS.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LpnResponseDTO {
    private String lpn;
    private String ptlGroup;
    private String ptlMergedGroups;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime scannedAt;
}