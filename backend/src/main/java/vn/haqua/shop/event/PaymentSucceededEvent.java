package vn.haqua.shop.event;

public record PaymentSucceededEvent(Long orderId, String providerTxnId) {
}
