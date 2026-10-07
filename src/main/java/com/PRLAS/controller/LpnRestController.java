package com.PRLAS.controller;

import com.PRLAS.dto.CsvUploadResponseDTO;
import com.PRLAS.dto.LpnResponseDTO;
import com.PRLAS.entity.ScanHistory;
import com.PRLAS.services.LpnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST API Endpoints supporting Scanner & Dashboard Operations
 */
@RestController
@RequestMapping("/api/lpn")
@RequiredArgsConstructor
public class LpnRestController {

    private final LpnService lpnService;

    // Search LPN barcode data
    @GetMapping("/scan")
    public ResponseEntity<LpnResponseDTO> scanLpn(@RequestParam("lpn") String lpn) {
        return ResponseEntity.ok(lpnService.scanAndSearchLpn(lpn));
    }

    // Fetch Last 10 Scans History for Scanner Screen Table
    @GetMapping("/recent-scans")
    public ResponseEntity<List<ScanHistory>> getRecentScans() {
        return ResponseEntity.ok(lpnService.getRecentScans());
    }

    // Native High-Speed CSV Upload Endpoint
    @PostMapping("/upload-csv")
    public ResponseEntity<CsvUploadResponseDTO> uploadCsv(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(lpnService.processAndSaveCsv(file));
    }
}