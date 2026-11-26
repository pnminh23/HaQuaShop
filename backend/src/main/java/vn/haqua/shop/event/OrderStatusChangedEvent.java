package vn.haqua.shop.event;

public record OrderStatusChangedEvent(String orderCode, String phone, String email, String newStatus) {
}
