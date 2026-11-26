package vn.haqua.shop.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.ReviewDtos.*;
import vn.haqua.shop.security.CurrentUser;
import vn.haqua.shop.service.ReviewService;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService service;

    @GetMapping
    ReviewList list(@PathVariable Long productId, @RequestParam(defaultValue = "0") int page) {
        return service.list(productId, page);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ReviewDto create(@PathVariable Long productId, @Valid @RequestBody ReviewRequest req) {
        return service.create(CurrentUser.requireId(), productId, req);

    }
}
