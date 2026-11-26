package vn.haqua.shop.controller;

import java.time.Duration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.OrderDtos.LookupCancelRequest;
import vn.haqua.shop.dto.OrderDtos.OrderTrackingDto;
import vn.haqua.shop.security.CurrentUser;
import vn.haqua.shop.service.OrderService;
import vn.haqua.shop.util.RateLimiter;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;
    private final RateLimiter limiter;

    /** UC09 – tra cứu bằng mã đơn + SĐT, giới hạn 10 lần/phút/IP. */
    @GetMapping("/lookup")
    OrderTrackingDto lookup(@RequestParam String code, @RequestParam String phone, HttpServletRequest http) {
        limiter.check("lookup:" + http.getRemoteAddr(), 10, Duration.ofMinutes(1));
        return service.lookup(code, phone);
    }

    @PostMapping("/cancel")
    OrderTrackingDto cancel(@Valid @RequestBody LookupCancelRequest req, HttpServletRequest http) {
        limiter.check("lookup:" + http.getRemoteAddr(), 10, Duration.ofMinutes(1));
        return service.cancelByCustomer(req.code(), req.phone());
    }

    @GetMapping("/mine")
    List<OrderTrackingDto> mine() {
        return service.mine(CurrentUser.requireId());

    }
}
