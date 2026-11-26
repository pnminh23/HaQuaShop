package vn.haqua.shop.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(@NotNull Long variantId, @Min(1) int qty) {
    }

    public record UpdateItemRequest(Integer qty, Boolean selected, Boolean savedForLater) {
    }

    public record CouponRequest(@NotBlank String code) {
    }

    public record CartItemDto(Long id, Long variantId, String productSlug, String name, String variantLabel,
                              String imageUrl, long unitPrice, long originalPrice, int qty, boolean selected,
                              boolean savedForLater, int stock, long lineTotal) {
    }

    public record CartSummary(long subtotal, long productDiscount, String couponCode, long couponDiscount,
                              long shippingFee, long total, int selectedCount) {
    }

    public record CartDto(String cartToken, List<CartItemDto> items, CartSummary summary, int itemCount,
                          String notice) {
    }
}
