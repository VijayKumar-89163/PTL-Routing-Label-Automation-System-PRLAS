package com.PRLAS.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CsvUploadResponseDTO {
    private int totalRows;
    private int insertedRows;
    private int skippedDuplicates;
    private String message;
}