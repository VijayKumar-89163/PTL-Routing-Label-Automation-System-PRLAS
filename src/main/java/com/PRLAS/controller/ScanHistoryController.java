package com.PRLAS.controller;

import com.PRLAS.entity.ScanHistory;
import com.PRLAS.services.ScanHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/scan-history")
@CrossOrigin(origins = "*") // Frontend CORS setup ke liye
public class ScanHistoryController {

    @Autowired
    private ScanHistoryService scanHistoryService;

    @GetMapping
    public ResponseEntity<Page<ScanHistory>> getScanHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ScanHistory> result = scanHistoryService.getScanHistory(search, fromDate, toDate, page, size);
        return ResponseEntity.ok(result);
    }
}