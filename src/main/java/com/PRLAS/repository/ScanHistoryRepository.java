package com.PRLAS.repository;

import com.PRLAS.entity.ScanHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScanHistoryRepository extends JpaRepository<ScanHistory, Long> {
    List<ScanHistory> findTop5ByOrderByScannedAtDesc();

    Page<ScanHistory> findAll(Specification<ScanHistory> spec, Pageable pageable);
}