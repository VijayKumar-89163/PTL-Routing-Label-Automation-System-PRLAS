package com.PRLAS.controller;

import com.PRLAS.entity.ScanHistory;
import com.PRLAS.entity.ScanStatus;
import com.PRLAS.exception.InvalidCsvException;
import com.PRLAS.services.ScanHistoryExportService;
import com.PRLAS.services.ScanHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/scan-history")
@CrossOrigin(origins = "*") // Frontend CORS setup ke liye
@Slf4j
public class ScanHistoryController {

    @Autowired
    private ScanHistoryService scanHistoryService;

    @Autowired
    private ScanHistoryExportService scanHistoryExportService;

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Screen ke liye paginated, filtered list (search + date range + status)
    @GetMapping
    public ResponseEntity<Page<ScanHistory>> getScanHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) ScanStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ScanHistory> result = scanHistoryService.getScanHistory(search, fromDate, toDate, status, page, size);
        return ResponseEntity.ok(result);
    }

    // Export: SAME filters, lekin saari matching rows ek file me (CSV ya Excel)
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportScanHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) ScanStatus status,
            @RequestParam(defaultValue = "csv") String format) {

        List<ScanHistory> records = scanHistoryService.getScanHistoryForExport(search, fromDate, toDate, status);
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP);

        byte[] fileBytes;
        String fileName;
        MediaType mediaType;

        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            fileBytes = scanHistoryExportService.buildExcel(records);
            fileName = "scan-history_" + timestamp + ".xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if ("csv".equalsIgnoreCase(format)) {
            fileBytes = scanHistoryExportService.buildCsv(records);
            fileName = "scan-history_" + timestamp + ".csv";
            mediaType = MediaType.parseMediaType("text/csv");
        } else {
            throw new InvalidCsvException("Unsupported export format: '" + format + "'. Use 'csv' or 'excel'.");
        }

        log.info("Scan History export generated: format={}, rows={}, file={}", format, records.size(), fileName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(fileBytes);
    }
}