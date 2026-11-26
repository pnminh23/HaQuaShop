package vn.haqua.shop.enums;

import java.util.Optional;

public enum OrderStatus {
    PENDING, CONFIRMED, PACKING, SHIPPING, COMPLETED, CANCELLED;

    /** Bước kế tiếp hợp lệ: Chờ xác nhận → Đã xác nhận → Đang đóng gói → Đang giao → Hoàn tất. */
    public Optional<OrderStatus> next() {
        return switch (this) {
            case PENDING -> Optional.of(CONFIRMED);
            case CONFIRMED -> Optional.of(PACKING);
            case PACKING -> Optional.of(SHIPPING);
            case SHIPPING -> Optional.of(COMPLETED);
            default -> Optional.empty();
        };
    }

    public boolean isCancellable() {
        return this == PENDING || this == CONFIRMED || this == PACKING;

    }
}
