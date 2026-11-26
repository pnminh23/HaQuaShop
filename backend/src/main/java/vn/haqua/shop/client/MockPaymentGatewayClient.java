package vn.haqua.shop.client;

import org.springframework.stereotype.Component;

import vn.haqua.shop.entity.Order;

/** Cài đặt giả lập cho môi trường dev; thay bằng VNPay/MoMo/PayOS… ở prod. */
@Component
public class MockPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public String createQr(Order order, String transactionId) {
        return "HAQUA-QR|" + order.getCode() + "|" + order.getTotal() + "|" + transactionId;
    }

    @Override
    public String createCardSession(Order order, String transactionId) {
        return "https://sandbox-pay.example.com/checkout/" + transactionId;

    }
}
