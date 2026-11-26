package vn.haqua.shop.controller;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.CatalogDtos.*;
import vn.haqua.shop.dto.PageDto;
import vn.haqua.shop.enums.ProductStatus;
import vn.haqua.shop.service.AdminProductService;
import vn.haqua.shop.service.BrandService;
import vn.haqua.shop.service.SeoService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService service;
    private final SeoService seoService;
    private final BrandService brandService;

    @GetMapping("/products")
    PageDto<AdminProductRow> list(@RequestParam(required = false) ProductStatus status,
                                  @RequestParam(required = false) Long categoryId,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return service.list(status, categoryId, q, page, size);
    }

    @GetMapping("/brands")
    java.util.List<BrandDto> brands() {
        return brandService.list();
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    BrandDto createBrand(@Valid @RequestBody BrandRequest req) {
        return brandService.create(req);
    }

    @PutMapping("/brands/{id}")
    BrandDto updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest req) {
        return brandService.update(id, req);
    }

    @DeleteMapping("/brands/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteBrand(@PathVariable Long id) {
        brandService.delete(id);
    }

    @GetMapping("/products/{id}")
    ProductDetailDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    ProductDetailDto create(@Valid @RequestBody AdminProductRequest req) {
        return service.create(req);
    }

    @PutMapping("/products/{id}")
    ProductDetailDto update(@PathVariable Long id, @Valid @RequestBody AdminProductRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void hide(@PathVariable Long id) {
        service.hide(id);
    }

    @PostMapping("/products/{id}/show")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void show(@PathVariable Long id) {
        service.show(id);
    }

    @PostMapping("/seo/evaluate")
    SeoReport evaluate(@RequestBody AdminProductRequest req) {
        return service.seoReport(req);
    }

    @GetMapping("/seo/slug-available")
    java.util.Map<String, Boolean> slugAvailable(@RequestParam String slug, @RequestParam(required = false) Long productId) {
        return java.util.Map.of("available", seoService.slugAvailable(slug, productId));

    }
}
