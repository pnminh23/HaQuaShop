package vn.haqua.shop.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.OrderDtos.*;
import vn.haqua.shop.dto.PageDto;
import vn.haqua.shop.security.CurrentUser;
import vn.haqua.shop.service.OrderService;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService service;

    @GetMapping
    PageDto<OrderRowDto> list(@RequestParam(defaultValue = "ALL") String tab,
                              @RequestParam(required = false) String q,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        return service.adminList(tab, q, page, size);
    }

    @GetMapping("/export")
    List<OrderRowDto> export(@RequestParam(defaultValue = "ALL") String tab) {
        return service.adminExport(tab);
    }

    @GetMapping("/{id}")
    OrderDetailDto get(@PathVariable Long id) {
        return service.adminGet(id);
    }

    @PostMapping("/{id}/advance")
    OrderDetailDto advance(@PathVariable Long id) {
        return service.advance(id, CurrentUser.requireId());
    }

    @PostMapping("/{id}/cancel")
    OrderDetailDto cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest req) {
        return service.cancel(id, CurrentUser.requireId(), req.reason());

    }
}
