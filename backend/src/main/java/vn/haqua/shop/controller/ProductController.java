package vn.haqua.shop.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.CatalogDtos.*;
import vn.haqua.shop.dto.ProductFilter;
import vn.haqua.shop.service.ProductService;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping
    ProductListResponse list(@RequestParam(required = false) String q,
                             @RequestParam(required = false) List<String> category,
                             @RequestParam(required = false) Long priceMin,
                             @RequestParam(required = false) Long priceMax,
                             @RequestParam(required = false) Double minRating,
                             @RequestParam(required = false) Boolean onSale,
                             @RequestParam(required = false) Boolean inStock,
                             @RequestParam(defaultValue = "popular") String sort,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "12") int size) {
        return service.search(new ProductFilter(q, category, priceMin, priceMax, minRating, onSale, inStock, sort), page, size);
    }

    @GetMapping("/suggest")
    List<SuggestDto> suggest(@RequestParam String q) {
        return service.suggest(q);
    }

    @GetMapping("/featured")
    List<ProductCardDto> featured(@RequestParam(defaultValue = "8") int limit) {
        return service.featured(Math.min(limit, 24));
    }

    @GetMapping("/slug/{slug}")
    ProductDetailDto detail(@PathVariable String slug) {
        return service.detail(slug);
    }

    @ExceptionHandler(ProductService.SlugMovedException.class)
    ResponseEntity<Void> moved(ProductService.SlugMovedException ex) {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header(HttpHeaders.LOCATION, "/api/products/slug/" + ex.getNewSlug()).build();

    }
}
