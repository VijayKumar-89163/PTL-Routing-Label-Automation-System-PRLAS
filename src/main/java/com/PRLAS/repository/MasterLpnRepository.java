package com.PRLAS.repository;

import com.PRLAS.entity.MasterLpn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MasterLpnRepository extends JpaRepository<MasterLpn, Long> {

    List<MasterLpn> findByLpnIgnoreCase(String lpn);

    // ===== Delete Data module (master_lpn_data only) =====

    // Preview count — shown to the user BEFORE they confirm the delete
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // Bulk delete by date range, used only after password + confirmation
    @Modifying
    @Transactional
    long deleteByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    // Native MySQL LOAD DATA INFILE Query (IGNORE duplicates dynamically)
    @Modifying
    @Query(value = "LOAD DATA LOCAL INFILE :filePath " +
            "IGNORE INTO TABLE master_lpn_data " +
            "FIELDS TERMINATED BY ',' " +
            "ENCLOSED BY '\"' " +
            "LINES TERMINATED BY '\\n' " +
            "IGNORE 1 LINES " +
            "(lpn, ptl_group, ptl_merged_group, location) " +
            "SET created_at = NOW()", nativeQuery = true)
    int loadCsvDataDirectly(@Param("filePath") String filePath);
}