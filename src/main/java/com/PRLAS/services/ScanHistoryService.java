package com.PRLAS.services;

import com.PRLAS.entity.ScanHistory;
import com.PRLAS.entity.ScanStatus;
import com.PRLAS.repository.ScanHistoryRepository;
import com.PRLAS.entity.ScanHistorySpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScanHistoryService {

    @Autowired
    private ScanHistoryRepository scanHistoryRepository;

    public Page<ScanHistory> getScanHistory(String search, LocalDateTime fromDate, LocalDateTime toDate,
                                            ScanStatus status, int page, int size) {
        Specification<ScanHistory> spec = ScanHistorySpecification.filterScanHistory(search, fromDate, toDate, status);

        // Latest scans ko sabse upar dikhane ke liye scannedAt DESC sorting
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scannedAt"));

        return scanHistoryRepository.findAll(spec, pageable);
    }

    /**
     * Export ke liye — SAME filters (search/date-range/status) se match
     * hone wale SAARE records laata hai, bina pagination ke. Isko CSV/Excel
     * file banane wale export service me use kiya jayega.
     */
    public List<ScanHistory> getScanHistoryForExport(String search, LocalDateTime fromDate, LocalDateTime toDate,
                                                     ScanStatus status) {
        Specification<ScanHistory> spec = ScanHistorySpecification.filterScanHistory(search, fromDate, toDate, status);
        Sort sort = Sort.by(Sort.Direction.DESC, "scannedAt");

        return scanHistoryRepository.findAll(spec, sort);
    }
}