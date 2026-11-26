package vn.haqua.shop.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.CartKey;
import vn.haqua.shop.dto.OrderDtos.CheckoutRequest;
import vn.haqua.shop.dto.OrderDtos.CheckoutResponse;
import vn.haqua.shop.security.CurrentUser;
import vn.haqua.shop.service.OrderService;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CheckoutResponse checkout(@RequestHeader(value = "X-Cart-Token", required = false) String token,
            @Valid @RequestBody CheckoutRequest req) {
        return service.placeOrder(new CartKey(CurrentUser.id().orElse(null), token), req);

    }
}
