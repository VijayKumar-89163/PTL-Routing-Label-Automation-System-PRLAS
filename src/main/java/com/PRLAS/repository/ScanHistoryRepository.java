package com.PRLAS.repository;

import com.PRLAS.entity.ScanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

// JpaSpecificationExecutor already provides BOTH
// findAll(Specification, Pageable) [used for paginated screen] AND
// findAll(Specification, Sort)     [used for the unpaged export flow]
// so no need to redeclare them manually here.
@Repository
public interface ScanHistoryRepository extends JpaRepository<ScanHistory, Long>, JpaSpecificationExecutor<ScanHistory> {
    List<ScanHistory> findTop5ByOrderByScannedAtDesc();
}