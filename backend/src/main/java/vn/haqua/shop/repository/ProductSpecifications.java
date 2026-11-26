package vn.haqua.shop.repository;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import vn.haqua.shop.dto.ProductFilter;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.enums.ProductStatus;
import vn.haqua.shop.util.SlugUtil;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> from(ProductFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(root.get("status").in(ProductStatus.ACTIVE, ProductStatus.OUT_OF_STOCK));
            if (f.q() != null && !f.q().isBlank()) {
                p.add(cb.like(root.get("nameUnaccent"), "%" + SlugUtil.unaccent(f.q().trim()) + "%"));
            }
            if (f.categories() != null && !f.categories().isEmpty()) {
                p.add(root.get("category").get("slug").in(f.categories()));
            }
            if (f.brands() != null && !f.brands().isEmpty()) {
                p.add(root.get("brand").in(f.brands()));
            }
            if (f.priceMin() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("minPrice"), f.priceMin()));
            }
            if (f.priceMax() != null) {
                p.add(cb.lessThan(root.get("minPrice"), f.priceMax()));
            }
            if (f.minRating() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("ratingAvg"), f.minRating()));
            }
            if (Boolean.TRUE.equals(f.onSale())) {
                p.add(cb.isTrue(root.get("onSale")));
            }
            if (Boolean.TRUE.equals(f.inStock())) {
                p.add(cb.greaterThan(root.get("totalStock"), 0));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    public static Sort sort(String key) {
        return switch (key == null ? "popular" : key) {
            case "priceAsc" -> Sort.by("minPrice").ascending();
            case "priceDesc" -> Sort.by("minPrice").descending();
            case "rating" -> Sort.by("ratingAvg").descending().and(Sort.by("reviewCount").descending());
            case "newest" -> Sort.by("id").descending();
            default -> Sort.by("soldCount").descending().and(Sort.by("id").descending());
        };

    }
}
