package com.PRLAS.repository;

import com.PRLAS.entity.ScanHistory;
import com.PRLAS.entity.ScanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

// JpaSpecificationExecutor already provides BOTH
// findAll(Specification, Pageable) [used for paginated screen] AND
// findAll(Specification, Sort)     [used for the unpaged export flow]
// so no need to redeclare them manually here.
@Repository
public interface ScanHistoryRepository extends JpaRepository<ScanHistory, Long>, JpaSpecificationExecutor<ScanHistory> {
    List<ScanHistory> findTop5ByOrderByScannedAtDesc();

    // Used by the Analytics Dashboard to get Found/Not-Found counts for a period
    long countByStatusAndScannedAtBetween(ScanStatus status, LocalDateTime from, LocalDateTime to);

    /**
     * Daily Found/Not-Found breakdown for the trend chart.
     * Returns rows of [date (yyyy-MM-dd string), status, count] grouped by day.
     */
    @Query(value = "SELECT DATE_FORMAT(scanned_at, '%Y-%m-%d') AS day, status, COUNT(*) AS cnt " +
            "FROM scan_history WHERE scanned_at >= :from " +
            "GROUP BY day, status ORDER BY day ASC", nativeQuery = true)
    List<Object[]> findDailyTrendSince(@Param("from") LocalDateTime from);
}