package com.PRLAS.services;

import com.PRLAS.dto.MasterDataDeletePreviewDTO;
import com.PRLAS.dto.MasterDataDeleteRequestDTO;
import com.PRLAS.dto.MasterDataDeleteResultDTO;
import com.PRLAS.exception.InvalidDeletePasswordException;
import com.PRLAS.repository.MasterLpnRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles the Delete Data module — bulk deletion of master_lpn_data rows
 * within a date range. This is a destructive, irreversible operation, so
 * it is gated behind:
 *   1) a preview step (shows how many rows WOULD be deleted, no changes made)
 *   2) a hardcoded password check (set via app.delete.master-password)
 * The actual UI confirmation dialog is a frontend concern; this service
 * only enforces the password check server-side (never trust the client alone).
 */
@Service
@Slf4j
public class MasterDataDeleteService {

    @Autowired
    private MasterLpnRepository masterLpnRepository;

    // Hardcoded (admin-set) password for bulk delete — set this in application.properties
    @Value("${app.delete.master-password}")
    private String deletePassword;

    public MasterDataDeletePreviewDTO previewDelete(LocalDateTime fromDate, LocalDateTime toDate) {
        long matching = masterLpnRepository.countByCreatedAtBetween(fromDate, toDate);

        return MasterDataDeletePreviewDTO.builder()
                .matchingRecords(matching)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
    }

    @Transactional
    public MasterDataDeleteResultDTO deleteByDateRange(MasterDataDeleteRequestDTO request) {
        if (request.getPassword() == null || !request.getPassword().equals(deletePassword)) {
            throw new InvalidDeletePasswordException("Incorrect password. Bulk delete was NOT performed.");
        }

        long deletedCount = masterLpnRepository.deleteByCreatedAtBetween(request.getFromDate(), request.getToDate());

        log.warn("BULK DELETE performed on master_lpn_data: {} rows deleted, range {} to {}",
                deletedCount, request.getFromDate(), request.getToDate());

        return MasterDataDeleteResultDTO.builder()
                .deletedCount(deletedCount)
                .message(deletedCount + " record(s) deleted successfully from Master LPN Data.")
                .build();
    }
}
