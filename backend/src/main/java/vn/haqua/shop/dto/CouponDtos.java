package vn.haqua.shop.dto;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import vn.haqua.shop.enums.CouponType;

public final class CouponDtos {

    private CouponDtos() {
    }

    public record CouponRequest(@NotBlank String code, @NotNull CouponType type, @Min(1) long value,
                                @Min(0) long maxDiscount, @Min(0) long minOrder, Instant startAt, Instant endAt,
                                @Min(0) int usageLimit) {
    }

    public record CouponDto(Long id, String code, CouponType type, long value, long maxDiscount, long minOrder,
                            Instant startAt, Instant endAt, int usageLimit, int usedCount) {
    }
}
