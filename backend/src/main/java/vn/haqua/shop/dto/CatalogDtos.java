package vn.haqua.shop.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import vn.haqua.shop.enums.ProductStatus;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record ProductCardDto(Long id, String slug, String name, String brand, String categorySlug,
                                 String categoryName, long price, long originalPrice, int discountPercent,
                                 double rating, int reviewCount, int soldCount, String imageUrl,
                                 ProductStatus status, Long defaultVariantId) {
    }

    public record ProductListResponse(PageDto<ProductCardDto> page, Map<String, Long> categoryFacets) {
    }

    public record SuggestDto(String slug, String name, long price, String imageUrl) {
    }

    public record VariantDto(Long id, String sku, String color, String storage, long price, long originalPrice,
                             int discountPercent, int stock) {
    }

    public record ImageDto(String url, String altText) {
    }

    public record SpecDto(String key, String value) {
    }

    public record SeoDto(String focusKeyword, String metaTitle, String metaDescription, int seoScore) {
    }

    public record ProductDetailDto(Long id, String slug, String name, String brand, String categorySlug,
                                   String categoryName, String description, ProductStatus status,
                                   double rating, int reviewCount, int soldCount,
                                   List<VariantDto> variants, List<ImageDto> images, List<SpecDto> specs,
                                   SeoDto seo, List<ProductCardDto> related) {
    }

    public record CategoryDto(Long id, String name, String slug, String seoDescription, Long parentId,
                              long productCount) {
    }

    // ---------- Admin ----------

    public record VariantInput(Long id, @NotBlank String sku, String color, String storage,
                               @Min(0) long price, @Min(0) long originalPrice, @Min(0) int stock) {
    }

    public record ImageInput(@NotBlank String url, String altText) {
    }

    public record SeoInput(String focusKeyword, String metaTitle, String metaDescription) {
    }

    public record BrandDto(Long id, String name, String slug, long productCount) {}

    public record BrandRequest(@NotBlank String name) {}

    public record AdminProductRequest(@NotBlank String name, @NotNull Long categoryId, String brand,
                                      String description, @NotNull ProductStatus status, String slug,
                                      @NotEmpty @Valid List<VariantInput> variants,
                                      @Valid List<ImageInput> images, List<SpecDto> specs,
                                      @Valid SeoInput seo) {
    }

    public record AdminProductRow(Long id, String name, String slug, String categoryName, long price,
                                  int totalStock, ProductStatus status, int seoScore, String imageUrl) {
    }

    public record SeoCriterion(String label, boolean ok) {
    }

    public record SeoReport(int score, List<SeoCriterion> criteria) {
    }

    public record CategoryRequest(@NotBlank String name, String slug, String seoDescription, Long parentId) {
    }
}
