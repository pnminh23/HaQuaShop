package vn.haqua.shop.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.haqua.shop.dto.CatalogDtos.MenuBrandDto;
import vn.haqua.shop.service.BrandService;

/** Phía khách: chỉ đọc. Thêm / sửa / xóa thương hiệu nằm ở /api/admin/brands. */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService service;

    @GetMapping("/computers")
    List<MenuBrandDto> computers() {
        return service.computerMenu();
    }
}
