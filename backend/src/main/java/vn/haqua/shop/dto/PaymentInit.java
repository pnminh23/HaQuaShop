package vn.haqua.shop.dto;

/** Kết quả khởi tạo thanh toán trả về cho frontend. */
public record PaymentInit(String qrData, String transferContent, String redirectUrl) {

    public static PaymentInit none() {
        return new PaymentInit(null, null, null);

    }
}
