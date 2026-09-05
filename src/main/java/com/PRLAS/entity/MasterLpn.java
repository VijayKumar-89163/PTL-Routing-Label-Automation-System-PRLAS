package com.PRLAS.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "master_lpn_data", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lpn_record", columnNames = {"lpn", "ptlGroup", "ptlMergedGroup", "location"})
}, indexes = {
        @Index(name = "idx_lpn", columnList = "lpn")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterLpn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String lpn;

    @Column(name = "ptl_group", nullable = false, length = 50)
    private String ptlGroup;

    @Column(name = "ptl_merged_group", nullable = false, length = 100)
    private String ptlMergedGroup;

    @Column(nullable = false, length = 50)
    private String location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}