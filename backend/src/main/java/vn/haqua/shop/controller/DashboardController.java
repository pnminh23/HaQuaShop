package vn.haqua.shop.controller;

import lombok.RequiredArgsConstructor;
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
    Dashboard get(@RequestParam(defaultValue = "14") int range) {
        return service.build(range);
    }

    @GetMapping("/export")
    ResponseEntity<String> export(@RequestParam(defaultValue = "14") int range) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"doanh-thu.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(service.exportCsv(range));

    }
}
