package vn.haqua.shop.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import vn.haqua.shop.dto.CatalogDtos.*;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.entity.ProductVariant;
import vn.haqua.shop.entity.SeoMeta;

@Component
public class ProductMapper {

    public ProductCardDto toCard(Product p) {
        int discount = p.getMinOriginalPrice() > p.getMinPrice() && p.getMinOriginalPrice() > 0
                ? (int) Math.round((1 - (double) p.getMinPrice() / p.getMinOriginalPrice()) * 100) : 0;
        Long defaultVariant = p.getVariants().stream().filter(v -> v.getStock() > 0).findFirst()
                .or(() -> p.getVariants().stream().findFirst()).map(ProductVariant::getId).orElse(null);
        return new ProductCardDto(p.getId(), p.getSlug(), p.getName(), p.getBrand(), p.getCategory().getSlug(),
                p.getCategory().getName(), p.getMinPrice(), discount > 0 ? p.getMinOriginalPrice() : 0, discount,
                p.getRatingAvg(), p.getReviewCount(), p.getSoldCount(), p.mainImageUrl(), p.getStatus(), defaultVariant);
    }

    public VariantDto toVariant(ProductVariant v) {
        int discount = v.hasDiscount() ? (int) Math.round((1 - (double) v.getPrice() / v.getOriginalPrice()) * 100) : 0;
        return new VariantDto(v.getId(), v.getSku(), v.getColor(), v.getStorage(), v.getPrice(),
                v.hasDiscount() ? v.getOriginalPrice() : 0, discount, v.getStock());
    }

    public ProductDetailDto toDetail(Product p, List<ProductCardDto> related) {
        SeoMeta s = p.getSeo();
        SeoDto seo = s == null ? null : new SeoDto(s.getFocusKeyword(), s.getMetaTitle(), s.getMetaDescription(), s.getSeoScore());
        return new ProductDetailDto(p.getId(), p.getSlug(), p.getName(), p.getBrand(), p.getCategory().getSlug(),
                p.getCategory().getName(), p.getDescription(), p.getStatus(), p.getRatingAvg(), p.getReviewCount(),
                p.getSoldCount(), p.getVariants().stream().map(this::toVariant).toList(),
                p.getImages().stream().map(i -> new ImageDto(i.getUrl(), i.getAltText())).toList(),
                p.getSpecs().stream().map(sp -> new SpecDto(sp.getSpecKey(), sp.getSpecValue())).toList(),
                seo, related);

    }
}
