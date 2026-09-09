package com.PRLAS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PrinterController {

    @GetMapping("/printer")
    public String printerPage() {
        return "Printer";   // Printer.html open hoga
    }

    @GetMapping("/test-sentry")
    public String testSentry() {
        // Intentional exception throw kar rahe hain test ke liye
        throw new NullPointerException("Sentry Integration Test - Manual Trigger!");
    }
}