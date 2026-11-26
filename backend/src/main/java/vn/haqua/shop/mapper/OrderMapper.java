package vn.haqua.shop.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import vn.haqua.shop.dto.OrderDtos.*;
import vn.haqua.shop.entity.Order;
import vn.haqua.shop.enums.OrderStatus;

@Component
public class OrderMapper {

    private static final java.util.Map<OrderStatus, String> ACTIONS = java.util.Map.of(
            OrderStatus.PENDING, "Xác nhận đơn",
            OrderStatus.CONFIRMED, "Bắt đầu đóng gói",
            OrderStatus.PACKING, "Giao cho vận chuyển",
            OrderStatus.SHIPPING, "Xác nhận đã giao");

    public List<OrderItemDto> items(Order o) {
        return o.getItems().stream()
                .map(i -> new OrderItemDto(i.getProductName(), i.getVariantLabel(), i.getUnitPrice(), i.getQty(), i.lineTotal()))
                .toList();
    }

    public List<HistoryDto> history(Order o) {
        return o.getHistory().stream()
                .map(h -> new HistoryDto(h.getFromStatus(), h.getToStatus(), h.getNote(), h.getChangedAt())).toList();
    }

    public OrderDetailDto detail(Order o) {
        return new OrderDetailDto(o.getId(), o.getCode(), o.getChannel(), o.getStatus(), o.getPaymentMethod(),
                o.getPaymentStatus(), o.getReceiverName(), o.getPhone(), o.getEmail(), o.getShippingAddress(),
                o.getShippingMethod(), o.getSubtotal(), o.getDiscount(), o.getShippingFee(), o.getTotal(), o.getNote(),
                o.getInvoiceTaxCode(), o.getInvoiceCompany(), o.getCreatedAt(), items(o), history(o),
                o.getStatus().next().isPresent(), ACTIONS.get(o.getStatus()));
    }

    public OrderRowDto row(Order o) {
        return new OrderRowDto(o.getId(), o.getCode(), o.getReceiverName(), o.getChannel(), o.getTotal(), o.getStatus(),
                o.getPaymentStatus(), o.getCreatedAt());
    }

    public OrderTrackingDto tracking(Order o) {
        return new OrderTrackingDto(o.getCode(), o.getStatus(), o.getPaymentStatus(), o.getPaymentMethod(),
                o.getReceiverName(), maskPhone(o.getPhone()), maskAddress(o.getShippingAddress()), o.getTotal(),
                o.getCreatedAt(), o.getStatus() == OrderStatus.PENDING, items(o), history(o));
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "*".repeat(phone.length() - 3) + phone.substring(phone.length() - 3);
    }

    public static String maskAddress(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        int i = address.lastIndexOf(',');
        return i < 0 ? "***" : "***" + address.substring(i);

    }
}
