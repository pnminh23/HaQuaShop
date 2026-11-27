package vn.haqua.shop.controller;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.DashboardDtos.Dashboard;
import vn.haqua.shop.service.DashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping
    Dashboard get(@RequestParam(defaultValue = "14") int range,
                  @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                  @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return service.build(range, from, to);
    }

    @GetMapping("/export")
    ResponseEntity<String> export(@RequestParam(defaultValue = "14") int range,
                                  @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"doanh-thu.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(service.exportCsv(range, from, to));

    }
}
