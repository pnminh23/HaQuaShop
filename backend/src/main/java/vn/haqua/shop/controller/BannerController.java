package vn.haqua.shop.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.BannerDtos.BannerDto;
import vn.haqua.shop.dto.BannerDtos.BannerRequest;
import vn.haqua.shop.dto.BannerDtos.ReorderRequest;
import vn.haqua.shop.service.BannerService;

/** Banner trang chủ: công khai đọc các slide đang bật; admin quản lý. */
@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService service;

    @GetMapping("/api/banners")
    List<BannerDto> publicList() {
        return service.active();
    }

    @GetMapping("/api/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    List<BannerDto> list() {
        return service.all();
    }

    @PostMapping("/api/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    BannerDto create(@Valid @RequestBody BannerRequest req) {
        return service.create(req);
    }

    @PutMapping("/api/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    BannerDto update(@PathVariable Long id, @Valid @RequestBody BannerRequest req) {
        return service.update(id, req);
    }

    @PutMapping("/api/admin/banners/order")
    @PreAuthorize("hasRole('ADMIN')")
    List<BannerDto> reorder(@RequestBody ReorderRequest req) {
        return service.reorder(req.ids());
    }

    @DeleteMapping("/api/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
