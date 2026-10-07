package com.PRLAS.entity;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScanHistorySpecification {

    public static Specification<ScanHistory> filterScanHistory(
            String keyword,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            ScanStatus status) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search Keyword (LPN, Location, ya PTL Group me search karega)
            if (StringUtils.hasText(keyword)) {
                String searchPattern = "%" + keyword.toLowerCase() + "%";
                Predicate lpnMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("lpn")), searchPattern);
                Predicate locationMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), searchPattern);
                Predicate ptlGroupMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("ptlGroup")), searchPattern);

                predicates.add(criteriaBuilder.or(lpnMatch, locationMatch, ptlGroupMatch));
            }

            // From Date Filter
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("scannedAt"), fromDate));
            }

            // To Date Filter
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("scannedAt"), toDate));
            }

            // Status Filter (FOUND / NOT_FOUND). null/absent => ALL (no filter applied)
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}