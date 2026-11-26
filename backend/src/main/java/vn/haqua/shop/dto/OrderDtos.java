package vn.haqua.shop.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import vn.haqua.shop.enums.OrderChannel;
import vn.haqua.shop.enums.OrderStatus;
import vn.haqua.shop.enums.PaymentMethod;
import vn.haqua.shop.enums.PaymentStatus;
import vn.haqua.shop.enums.ShippingMethod;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record InvoiceInfo(@NotBlank String taxCode, @NotBlank String company, @NotBlank @Email String email) {
    }

    public record CheckoutRequest(
            @NotBlank String receiverName,
            @NotBlank @Pattern(regexp = "^0\\d{9}$", message = "SĐT gồm 10 số, bắt đầu bằng 0") String phone,
            @Email String email,
            @NotBlank String province, @NotBlank String ward, @NotBlank String detail,
            String note,
            @NotNull ShippingMethod shippingMethod,
            @NotNull PaymentMethod paymentMethod,
            Integer installmentMonths,
            @Valid InvoiceInfo invoice) {
    }

    public record CheckoutResponse(String orderCode, long total, String estimatedDelivery, int pointsEarned,
                                   String qrData, String transferContent, String redirectUrl, String cartToken) {
    }

    public record OrderItemDto(String productName, String variantLabel, long unitPrice, int qty, long lineTotal) {
    }

    public record HistoryDto(String fromStatus, String toStatus, String note, Instant changedAt) {
    }

    /** Dùng cho admin. */
    public record OrderDetailDto(Long id, String code, OrderChannel channel, OrderStatus status,
                                 PaymentMethod paymentMethod, PaymentStatus paymentStatus,
                                 String receiverName, String phone, String email, String shippingAddress,
                                 ShippingMethod shippingMethod, long subtotal, long discount, long shippingFee,
                                 long total, String note, String invoiceTaxCode, String invoiceCompany,
                                 Instant createdAt, List<OrderItemDto> items, List<HistoryDto> history,
                                 boolean canAdvance, String nextAction) {
    }

    public record OrderRowDto(Long id, String code, String receiverName, OrderChannel channel, long total,
                              OrderStatus status, PaymentStatus paymentStatus, Instant createdAt) {
    }

    /** Dùng cho tra cứu của khách: SĐT, địa chỉ đã che một phần. */
    public record OrderTrackingDto(String code, OrderStatus status, PaymentStatus paymentStatus,
                                   PaymentMethod paymentMethod, String receiverName, String maskedPhone,
                                   String maskedAddress, long total, Instant createdAt, boolean cancellable,
                                   List<OrderItemDto> items, List<HistoryDto> timeline) {
    }

    public record CancelRequest(@NotBlank String reason) {
    }

    public record LookupCancelRequest(@NotBlank String code, @NotBlank String phone) {
    }
}
