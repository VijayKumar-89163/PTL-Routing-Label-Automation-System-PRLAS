package com.PRLAS.services;

import com.PRLAS.entity.ScanHistory;
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

@Service
public class ScanHistoryService {

    @Autowired
    private ScanHistoryRepository scanHistoryRepository;

    public Page<ScanHistory> getScanHistory(String search, LocalDateTime fromDate, LocalDateTime toDate, int page, int size) {
        Specification<ScanHistory> spec = ScanHistorySpecification.filterScanHistory(search, fromDate, toDate);

        // Latest scans ko sabse upar dikhane ke liye scannedAt DESC sorting
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scannedAt"));

        return scanHistoryRepository.findAll(spec, pageable);
    }
}