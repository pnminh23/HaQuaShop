package vn.haqua.shop.event;

public record OrderPlacedEvent(String orderCode, String phone, String email, long total) {
}
