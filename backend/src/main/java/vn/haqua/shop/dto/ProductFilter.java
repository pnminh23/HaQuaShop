package vn.haqua.shop.dto;

import java.util.List;

public record ProductFilter(String q, List<String> categories, Long priceMin, Long priceMax, Double minRating,
                            Boolean onSale, Boolean inStock, String sort) {
}
