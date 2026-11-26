package vn.haqua.shop.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import vn.haqua.shop.entity.Coupon;
import vn.haqua.shop.enums.CouponType;
import vn.haqua.shop.exception.ApiException;

class CouponTest {

    private Coupon percent(long pct, long max, long min) {
        Coupon c = new Coupon();
        c.setCode("HAQUA10");
        c.setType(CouponType.PERCENT);
        c.setValue(pct);
        c.setMaxDiscount(max);
        c.setMinOrder(min);
        return c;
    }

    @Test
    void percentDiscountIsRoundedToThousandAndCapped() {
        assertThat(percent(10, 2_000_000, 0).discountFor(24_990_000)).isEqualTo(2_000_000); // bị chặn bởi mức tối đa
        assertThat(percent(10, 0, 0).discountFor(1_234_000)).isEqualTo(123_000);
    }

    @Test
    void rejectsBelowMinimumOrderWithShortfall() {
        assertThatThrownBy(() -> percent(10, 0, 500_000).assertApplicable(300_000))
                .isInstanceOf(ApiException.class).hasMessageContaining("200000");
    }

    @Test
    void rejectsExpiredAndExhausted() {
        Coupon expired = percent(10, 0, 0);
        expired.setEndAt(Instant.now().minus(1, ChronoUnit.DAYS));
        assertThatThrownBy(() -> expired.assertApplicable(1_000_000)).hasMessageContaining("hết hạn");

        Coupon used = percent(10, 0, 0);
        used.setUsageLimit(1);
        used.consume();
        assertThatThrownBy(() -> used.assertApplicable(1_000_000)).hasMessageContaining("hết lượt");

    }
}
