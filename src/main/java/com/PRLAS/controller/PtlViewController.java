package com.PRLAS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller mapping dual routing screens for V-Mart PRLAS System
 */
@Controller
public class PtlViewController {

    // Route 1: Direct Handheld Scanner Screen (/ptl-scanner)
    @GetMapping("/ptl-scanner")
    public String showScannerScreen() {
        return "ptl-scanner";
    }

    // Route 2: Administrative Overview Dashboard (/ptl-dashboard)
    @GetMapping("/ptl-dashboard")
    public String showDashboardScreen() {
        return "ptl-dashboard";
    }

    // Route 3: Scan History (search / date-range / status filter + export)
    @GetMapping("/scan-history")
    public String showScanHistoryScreen() {
        return "scan-history";
    }

    // Route 4: Delete Data (bulk delete on master_lpn_data, password protected)
    @GetMapping("/delete-data")
    public String showDeleteDataScreen() {
        return "delete-data";
    }

    // Fallback Root Route redirecting to Dashboard
    @GetMapping("/")
    public String index() {
        return "redirect:/ptl-dashboard";
    }
}