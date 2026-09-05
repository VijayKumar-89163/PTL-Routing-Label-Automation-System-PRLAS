package com.PRLAS.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scan_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String lpn;

    @Column(name = "ptl_group", length = 50)
    private String ptlGroup;

    @Column(name = "ptl_merged_groups", length = 255)
    private String ptlMergedGroups;

    @Column(length = 50)
    private String location;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    @PrePersist
    protected void onScan() {
        this.scannedAt = LocalDateTime.now();
    }
}