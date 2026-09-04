package com.PRLAS.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PrinterController {

    @GetMapping("/printer")
    public String printerPage() {
        return "Printer";   // Printer.html open hoga
    }
}