package vn.haqua.shop.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import vn.haqua.shop.enums.OrderStatus;
import vn.haqua.shop.util.SlugUtil;

class OrderStatusTest {

    @Test
    void followsFixedOrder() {
        assertThat(OrderStatus.PENDING.next()).contains(OrderStatus.CONFIRMED);
        assertThat(OrderStatus.SHIPPING.next()).contains(OrderStatus.COMPLETED);
        assertThat(OrderStatus.COMPLETED.next()).isEmpty();
        assertThat(OrderStatus.CANCELLED.next()).isEmpty();
    }

    @Test
    void cannotCancelOnceShipping() {
        assertThat(OrderStatus.PACKING.isCancellable()).isTrue();
        assertThat(OrderStatus.SHIPPING.isCancellable()).isFalse();
    }

    @Test
    void slugRemovesVietnameseDiacritics() {
        assertThat(SlugUtil.toSlug("Điện thoại Nova X 5G")).isEqualTo("dien-thoai-nova-x-5g");

    }
}
