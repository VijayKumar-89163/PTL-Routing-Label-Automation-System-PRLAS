package com.PRLAS.services;

import com.PRLAS.dto.CsvUploadResponseDTO;
import com.PRLAS.dto.LpnResponseDTO;
import com.PRLAS.entity.ScanHistory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LpnService {
    CsvUploadResponseDTO processAndSaveCsv(MultipartFile file);
    LpnResponseDTO scanAndSearchLpn(String lpnQuery);
    List<ScanHistory> getRecentScans();
}