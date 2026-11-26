package vn.haqua.shop.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.CouponDtos.CouponDto;
import vn.haqua.shop.dto.CouponDtos.CouponRequest;
import vn.haqua.shop.service.CouponService;

@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService service;

    @GetMapping
    List<CouponDto> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CouponDto create(@Valid @RequestBody CouponRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    CouponDto update(@PathVariable Long id, @Valid @RequestBody CouponRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/suspend")
    CouponDto suspend(@PathVariable Long id) {
        return service.suspend(id);

    }
}
