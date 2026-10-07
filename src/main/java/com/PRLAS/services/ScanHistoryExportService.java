package com.PRLAS.services;

import com.PRLAS.entity.ScanHistory;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds export files (CSV / Excel) for the Scan History module.
 * Takes the SAME filtered list the on-screen table shows (search + date
 * range + status) and converts it into a downloadable file.
 */
@Service
@Slf4j
public class ScanHistoryExportService {

    private static final String[] HEADERS = {"LPN", "PTL Group", "PTL Merged Groups", "Location", "Status", "Scanned At"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Builds a plain CSV file (UTF-8, comma separated, values double-quote
     * escaped) from the given records.
     */
    public byte[] buildCsv(List<ScanHistory> records) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.join(",", HEADERS)).append("\n");

        for (ScanHistory record : records) {
            sb.append(csvEscape(record.getLpn())).append(",")
                    .append(csvEscape(record.getPtlGroup())).append(",")
                    .append(csvEscape(record.getPtlMergedGroups())).append(",")
                    .append(csvEscape(record.getLocation())).append(",")
                    .append(csvEscape(record.getStatus() != null ? record.getStatus().name() : "")).append(",")
                    .append(csvEscape(record.getScannedAt() != null ? record.getScannedAt().format(DATE_FORMAT) : ""))
                    .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Wraps a CSV field in double quotes and escapes any internal quotes,
     * so commas/quotes/newlines inside a value never break the CSV structure.
     */
    private String csvEscape(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * Builds an .xlsx (Excel) workbook from the given records using Apache POI.
     */
    public byte[] buildExcel(List<ScanHistory> records) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Scan History");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIndex = 1;
            for (ScanHistory record : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(nullSafe(record.getLpn()));
                row.createCell(1).setCellValue(nullSafe(record.getPtlGroup()));
                row.createCell(2).setCellValue(nullSafe(record.getPtlMergedGroups()));
                row.createCell(3).setCellValue(nullSafe(record.getLocation()));
                row.createCell(4).setCellValue(record.getStatus() != null ? record.getStatus().name() : "");
                row.createCell(5).setCellValue(record.getScannedAt() != null ? record.getScannedAt().format(DATE_FORMAT) : "");
            }

            // Auto-size columns so the exported file is readable without manual resizing
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Excel export generation failed: ", e);
            throw new RuntimeException("Failed to generate Excel export: " + e.getMessage());
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
