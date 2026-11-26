package vn.haqua.shop.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.haqua.shop.enums.CouponType;
import vn.haqua.shop.exception.ApiException;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "coupons")
public class Coupon extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CouponType type;

    /** PERCENT: số phần trăm; FIXED: số tiền. */
    @Column(name = "discount_value")
    private long value;

    /** 0 = không giới hạn. */
    private long maxDiscount;

    private long minOrder;
    private Instant startAt;
    private Instant endAt;

    /** 0 = không giới hạn. */
    private int usageLimit;
    private int usedCount;

    /** Kiểm tra mã dùng được với tạm tính này; ném lỗi 422 kèm lý do cụ thể. */
    public void assertApplicable(long subtotal) {
        Instant now = Instant.now();
        if (startAt != null && now.isBefore(startAt)) {
            throw ApiException.unprocessable("Mã giảm giá chưa đến thời gian áp dụng");
        }
        if (endAt != null && now.isAfter(endAt)) {
            throw ApiException.unprocessable("Mã giảm giá đã hết hạn");
        }
        if (usageLimit > 0 && usedCount >= usageLimit) {
            throw ApiException.unprocessable("Mã giảm giá đã hết lượt sử dụng");
        }
        if (subtotal < minOrder) {
            throw ApiException.unprocessable("Cần mua thêm " + (minOrder - subtotal) + "₫ để dùng mã này");
        }
    }

    public long discountFor(long subtotal) {
        long d = type == CouponType.PERCENT
                ? Math.round(subtotal * value / 100.0 / 1000.0) * 1000
                : value;
        if (maxDiscount > 0) {
            d = Math.min(d, maxDiscount);
        }
        return Math.min(d, subtotal);
    }

    public void consume() {
        usedCount++;
    }

    public void restore() {
        if (usedCount > 0) {
            usedCount--;

        }
    }
}
