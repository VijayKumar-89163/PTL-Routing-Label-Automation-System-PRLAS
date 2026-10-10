package com.PRLAS.controller;

import com.PRLAS.dto.MasterDataDeletePreviewDTO;
import com.PRLAS.dto.MasterDataDeleteRequestDTO;
import com.PRLAS.dto.MasterDataDeleteResultDTO;
import com.PRLAS.services.MasterDataDeleteService;
import com.PRLAS.util.DateRangeUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/master-data")
@CrossOrigin(origins = "*")
public class MasterDataDeleteController {

    @Autowired
    private MasterDataDeleteService masterDataDeleteService;

    /**
     * Preview — how many master_lpn_data rows would be deleted for this
     * range. NO data is changed here; this is a safe, read-only check,
     * shown to the user before they're asked for the password.
     * Either pass a named "range" (today|week|month|qtd|year) OR explicit
     * fromDate/toDate — named range takes priority if both are given.
     */
    @GetMapping("/delete-preview")
    public ResponseEntity<MasterDataDeletePreviewDTO> previewDelete(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        LocalDateTime resolvedFrom = DateRangeUtil.resolveNamedRangeStart(range);
        LocalDateTime from = resolvedFrom != null ? resolvedFrom : fromDate;
        LocalDateTime to = resolvedFrom != null ? DateRangeUtil.now() : toDate;

        return ResponseEntity.ok(masterDataDeleteService.previewDelete(from, to));
    }

    /**
     * Actual delete — requires the hardcoded password in the request body.
     * Irreversible. Frontend must show its own confirmation dialog before
     * calling this; the password check here is the real server-side gate.
     */
    @PostMapping("/delete")
    public ResponseEntity<MasterDataDeleteResultDTO> deleteByRange(@Valid @RequestBody MasterDataDeleteRequestDTO request) {
        return ResponseEntity.ok(masterDataDeleteService.deleteByDateRange(request));
    }
}
