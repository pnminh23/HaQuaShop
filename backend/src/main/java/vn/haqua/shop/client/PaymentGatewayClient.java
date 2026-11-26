package vn.haqua.shop.client;

import vn.haqua.shop.entity.Order;

/** Cổng thanh toán bên ngoài. Đổi nhà cung cấp chỉ cần viết thêm một class cài đặt. */
public interface PaymentGatewayClient {

    /** Dữ liệu mã QR chuyển khoản. */
    String createQr(Order order, String transactionId);

    /** URL trang thanh toán của cổng (thẻ, ví, trả góp) – HaQua không lưu số thẻ/CVV. */
    String createCardSession(Order order, String transactionId);

}
