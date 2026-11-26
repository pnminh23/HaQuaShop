package vn.haqua.shop.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import vn.haqua.shop.service.BrandService;

/** Dữ liệu có sẵn từ trước: tự đánh dấu các hãng đang bán máy tính để menu không bị trống. */
@Component
@Order(30) // sau DataSeeder và CatalogImporter
@RequiredArgsConstructor
public class ComputerBrandInitializer implements ApplicationRunner {

    private final BrandService brandService;

    @Override
    public void run(ApplicationArguments args) {
        brandService.detectComputerBrands();
    }
}
