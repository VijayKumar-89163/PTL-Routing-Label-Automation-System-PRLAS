package com.PRLAS.services.impl;

import com.PRLAS.dto.CsvUploadResponseDTO;
import com.PRLAS.dto.LpnResponseDTO;
import com.PRLAS.entity.MasterLpn;
import com.PRLAS.entity.ScanHistory;
import com.PRLAS.entity.ScanStatus;
import com.PRLAS.exception.InvalidCsvException;
import com.PRLAS.exception.LpnNotFoundException;
import com.PRLAS.repository.MasterLpnRepository;
import com.PRLAS.repository.ScanHistoryRepository;
import com.PRLAS.services.LpnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LpnServiceImpl implements LpnService {

    private final MasterLpnRepository masterLpnRepository;
    private final ScanHistoryRepository scanHistoryRepository;

    @Override
    @Transactional
    public CsvUploadResponseDTO processAndSaveCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidCsvException("Uploaded CSV file is empty!");
        }

        File tempFile = null;
        try {
            // 1. Create a temporary file on Disk
            tempFile = File.createTempFile("lpn_upload_", ".csv");
            file.transferTo(tempFile);

            // Escape Backslashes for MySQL File Path compatibility (Windows/Linux)
            String absolutePath = tempFile.getAbsolutePath().replace("\\", "/");

            // 2. Direct High-Speed MySQL LOAD DATA INFILE Execution
            int insertedRows = masterLpnRepository.loadCsvDataDirectly(absolutePath);

            log.info("Direct MySQL INFILE Import Completed. Rows affected: {}", insertedRows);

            return CsvUploadResponseDTO.builder()
                    .totalRows(insertedRows)
                    .insertedRows(insertedRows)
                    .skippedDuplicates(0)
                    .message("CSV File loaded ultra-fast via MySQL Native Engine!")
                    .build();

        } catch (Exception e) {
            log.error("Fast CSV Upload Failed: ", e);
            throw new InvalidCsvException("Failed to execute LOAD DATA INFILE: " + e.getMessage());
        } finally {
            // Clean up temporary file
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Override
    @Transactional(noRollbackFor = LpnNotFoundException.class)
    public LpnResponseDTO scanAndSearchLpn(String lpnQuery) {
        String cleanedLpn = lpnQuery.trim();
        List<MasterLpn> records = masterLpnRepository.findByLpnIgnoreCase(cleanedLpn);

        // ❌ IF LPN NOT FOUND IN MASTER DB
        if (records.isEmpty()) {
            ScanHistory history = ScanHistory.builder()
                    .lpn(cleanedLpn)
                    .ptlGroup("--")
                    .ptlMergedGroups("--")
                    .location("--")
                    .status(ScanStatus.NOT_FOUND) // 🌟 Status Set to NOT_FOUND
                    .build();

            ScanHistory saved = scanHistoryRepository.save(history);

            log.warn("LPN NOT FOUND: {} | Scanned At: {}", cleanedLpn, saved.getScannedAt());

            throw new LpnNotFoundException(
                    "LPN '" + cleanedLpn + "' NOT FOUND in Master Database.");
        }

        // ✅ IF LPN IS FOUND
        String ptlGroup = records.get(0).getPtlGroup();
        String location = records.get(0).getLocation();
        LocalDateTime dbCreatedAt = records.get(0).getCreatedAt();

        // ZONES SORTING LOGIC
        String aggregatedMergedGroups = records.stream()
                .map(MasterLpn::getPtlMergedGroup)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));

        ScanHistory history = ScanHistory.builder()
                .lpn(cleanedLpn)
                .ptlGroup(ptlGroup)
                .ptlMergedGroups(aggregatedMergedGroups)
                .location(location)
                .status(ScanStatus.FOUND) // 🌟 Status Set to FOUND
                .build();

        ScanHistory savedHistory = scanHistoryRepository.save(history);

        // 🌟 SUCCESS LOG FOR CONSOLE PRINTING
        log.info("LPN FOUND: {} | Scanned At: {}", cleanedLpn, savedHistory.getScannedAt());

        return LpnResponseDTO.builder()
                .lpn(cleanedLpn)
                .ptlGroup(ptlGroup)
                .ptlMergedGroups(aggregatedMergedGroups)
                .location(location)
                .createdAt(dbCreatedAt)
                .scannedAt(savedHistory.getScannedAt())
                .build();
    }

    @Override
    public List<ScanHistory> getRecentScans() {
        return scanHistoryRepository.findTop5ByOrderByScannedAtDesc();
    }
}