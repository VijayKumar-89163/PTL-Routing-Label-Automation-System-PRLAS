package com.PRLAS.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Shared helper for computing start/end LocalDateTime boundaries for the
 * standard reporting periods used across the Analytics Dashboard and the
 * Master Data Delete module: Today, This Week, This Month, QTD, This Year.
 */
public class DateRangeUtil {

    public static LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    public static LocalDateTime startOfWeek() {
        // Week starts Monday
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
    }

    public static LocalDateTime startOfMonth() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }

    public static LocalDateTime startOfQuarter() {
        int currentMonth = LocalDate.now().getMonthValue();
        int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1; // 1, 4, 7, or 10
        return LocalDate.now().withMonth(quarterStartMonth).withDayOfMonth(1).atStartOfDay();
    }

    public static LocalDateTime startOfYear() {
        return LocalDate.now().withDayOfYear(1).atStartOfDay();
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Resolves a named range keyword ("today" | "week" | "month" | "qtd" | "year")
     * into a [from, now] boundary. Returns null if the keyword is unrecognized,
     * so the caller can fall back to explicit fromDate/toDate params instead.
     */
    public static LocalDateTime resolveNamedRangeStart(String range) {
        if (range == null) return null;
        return switch (range.toLowerCase()) {
            case "today" -> startOfToday();
            case "week" -> startOfWeek();
            case "month" -> startOfMonth();
            case "qtd" -> startOfQuarter();
            case "year" -> startOfYear();
            default -> null;
        };
    }
}
