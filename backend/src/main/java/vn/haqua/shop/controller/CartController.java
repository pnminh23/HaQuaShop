package vn.haqua.shop.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.CartDtos.*;
import vn.haqua.shop.dto.CartKey;
import vn.haqua.shop.security.CurrentUser;
import vn.haqua.shop.service.CartService;

/**
 * Khách đăng nhập dùng JWT; khách vãng lai gửi header X-Cart-Token (lấy từ
 * cartToken trong phản hồi).
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private static final String TOKEN = "X-Cart-Token";

    private final CartService service;

    private CartKey key(String token) {
        return new CartKey(CurrentUser.id().orElse(null), token);
    }

    @GetMapping
    CartDto get(@RequestHeader(value = TOKEN, required = false) String token) {
        return service.get(key(token));
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    CartDto add(@RequestHeader(value = TOKEN, required = false) String token, @Valid @RequestBody AddItemRequest req) {
        return service.addItem(key(token), req);
    }

    @PatchMapping("/items/{id}")
    CartDto update(@RequestHeader(value = TOKEN, required = false) String token, @PathVariable Long id,
            @RequestBody UpdateItemRequest req) {
        return service.updateItem(key(token), id, req);
    }

    @DeleteMapping("/items/{id}")
    CartDto remove(@RequestHeader(value = TOKEN, required = false) String token, @PathVariable Long id) {
        return service.removeItem(key(token), id);
    }

    @DeleteMapping("/items")
    CartDto removeSelected(@RequestHeader(value = TOKEN, required = false) String token) {
        return service.removeSelected(key(token));
    }

    @PostMapping("/coupon")
    CartDto applyCoupon(@RequestHeader(value = TOKEN, required = false) String token,
            @Valid @RequestBody CouponRequest req) {
        return service.applyCoupon(key(token), req.code());
    }

    @DeleteMapping("/coupon")
    CartDto removeCoupon(@RequestHeader(value = TOKEN, required = false) String token) {
        return service.removeCoupon(key(token));

    }
}
