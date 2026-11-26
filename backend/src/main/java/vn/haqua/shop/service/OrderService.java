package vn.haqua.shop.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.config.AppProperties;
import vn.haqua.shop.dto.CartKey;
import vn.haqua.shop.dto.OrderDtos.*;
import vn.haqua.shop.dto.PageDto;
import vn.haqua.shop.dto.PaymentInit;
import vn.haqua.shop.entity.CartItem;
import vn.haqua.shop.entity.Coupon;
import vn.haqua.shop.entity.Order;
import vn.haqua.shop.entity.OrderItem;
import vn.haqua.shop.entity.OrderStatusHistory;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.entity.ProductVariant;
import vn.haqua.shop.enums.OrderChannel;
import vn.haqua.shop.enums.OrderStatus;
import vn.haqua.shop.enums.PaymentMethod;
import vn.haqua.shop.enums.PaymentStatus;
import vn.haqua.shop.enums.ShippingMethod;
import vn.haqua.shop.event.OrderPlacedEvent;
import vn.haqua.shop.event.OrderStatusChangedEvent;
import vn.haqua.shop.event.PaymentSucceededEvent;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.mapper.OrderMapper;
import vn.haqua.shop.repository.OrderRepository;
import vn.haqua.shop.repository.UserRepository;

/** UC08 đặt hàng, UC09 tra cứu, UC15 quản lý đơn. */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orders;
    private final CartService carts;
    private final CouponService coupons;
    private final ShippingFeeCalculator shipping;
    private final PaymentService payments;
    private final UserRepository users;
    private final OrderMapper mapper;
    private final ApplicationEventPublisher events;
    private final AppProperties props;

    // ================================================================ UC08

    /**
     * Tính lại giá từ CSDL, kiểm tra mã giảm, trừ tồn kho và tạo đơn trong một giao
     * dịch.
     */
    @Transactional
    public CheckoutResponse placeOrder(CartKey key, CheckoutRequest req) {
        List<CartItem> items = carts.selectedItems(key);
        if (items.isEmpty()) {
            throw ApiException.badRequest("Giỏ hàng không có sản phẩm nào được chọn");
        }
        validate(req);

        Order o = new Order();
        o.setCode(nextCode());
        if (key.userId() != null) {
            o.setUser(users.getReferenceById(key.userId()));
        }
        o.setReceiverName(req.receiverName());
        o.setPhone(req.phone());
        o.setEmail(req.email());
        o.setShippingAddress(String.join(", ", req.detail(), req.ward(), req.province()));
        o.setNote(req.note());
        o.setShippingMethod(req.shippingMethod());
        o.setPaymentMethod(req.paymentMethod());
        o.setChannel(carts.isFromChatbot(key) ? OrderChannel.CHATBOT : OrderChannel.WEBSITE);
        if (req.invoice() != null) {
            o.setInvoiceTaxCode(req.invoice().taxCode());
            o.setInvoiceCompany(req.invoice().company());
            o.setInvoiceEmail(req.invoice().email());
        }

        for (CartItem ci : items) {
            ProductVariant v = ci.getVariant();
            v.reserve(ci.getQty()); // ném 409 nếu hết hàng; @Version chặn mua trùng món cuối
            v.getProduct().refreshDerived();
            o.getItems().add(new OrderItem(o, v, ci.getQty()));
        }
        long subtotal = o.getItems().stream().mapToLong(OrderItem::lineTotal).sum();

        long discount = 0;
        Coupon coupon = carts.couponOf(key);
        if (coupon != null) {
            try {
                coupon = coupons.validate(coupon.getCode(), subtotal);
                discount = coupon.discountFor(subtotal);
                coupon.consume();
                o.setCoupon(coupon);
            } catch (ApiException e) {
                throw ApiException.conflict("Mã giảm giá không còn hợp lệ: " + e.getMessage());
            }
        }
        long fee = shipping.fee(req.shippingMethod(), subtotal);
        o.setSubtotal(subtotal);
        o.setDiscount(discount);
        o.setShippingFee(fee);
        o.setTotal(subtotal - discount + fee);
        o.setPointsEarned((int) (o.getTotal() / props.shop().greenPointRate()));
        o.getHistory().add(new OrderStatusHistory(o, null, OrderStatus.PENDING, null, "Đặt hàng"));
        orders.save(o);

        PaymentInit init = payments.createPayment(o, req.installmentMonths() == null ? 0 : req.installmentMonths());
        carts.clearPurchased(key);
        events.publishEvent(new OrderPlacedEvent(o.getCode(), o.getPhone(), o.getEmail(), o.getTotal()));
        return new CheckoutResponse(o.getCode(), o.getTotal(), estimate(req.shippingMethod()), o.getPointsEarned(),
                init.qrData(), init.transferContent(), init.redirectUrl(), null);
    }

    private void validate(CheckoutRequest req) {
        if (req.shippingMethod() == ShippingMethod.EXPRESS_2H
                && !(req.province().contains("Hà Nội") || req.province().contains("Hồ Chí Minh"))) {
            throw ApiException.unprocessable("Giao nhanh 2 giờ chỉ áp dụng tại Hà Nội và TP.HCM");
        }
        if (req.paymentMethod() == PaymentMethod.INSTALLMENT
                && (req.installmentMonths() == null || req.installmentMonths() < 3)) {
            throw ApiException.badRequest("Vui lòng chọn kỳ hạn trả góp");
        }
    }

    private String estimate(ShippingMethod m) {
        return switch (m) {
            case STANDARD -> "Giao trong 1–3 ngày";
            case EXPRESS_2H -> "Giao trong 2 giờ";
            case STORE_PICKUP -> "Nhận tại cửa hàng sau khi được xác nhận";
        };
    }

    private String nextCode() {
        String prefix = "HQ-" + LocalDate.now().format(DAY) + "-";
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(prefix);
            for (int j = 0; j < 4; j++) {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            if (!orders.existsByCode(sb.toString())) {
                return sb.toString();
            }
        }
        throw new IllegalStateException("Không sinh được mã đơn");
    }

    /** Cổng thanh toán báo đã nhận tiền → đơn Đã thanh toán + Đã xác nhận. */
    @EventListener
    @Transactional
    public void onPaymentSucceeded(PaymentSucceededEvent e) {
        Order o = orders.findById(e.orderId()).orElseThrow();
        o.setPaymentStatus(PaymentStatus.PAID);
        if (o.getStatus() == OrderStatus.PENDING) {
            o.changeStatus(OrderStatus.CONFIRMED, null, "Đã nhận thanh toán " + e.providerTxnId());
            notifyStatus(o);
        }
    }

    // ================================================================ UC09

    @Transactional(readOnly = true)
    public OrderTrackingDto lookup(String code, String phone) {
        return mapper.tracking(orders.findByCodeAndPhone(code.trim(), phone.trim())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng")));
    }

    @Transactional(readOnly = true)
    public List<OrderTrackingDto> mine(Long userId) {
        return orders.findByUserIdOrderByIdDesc(userId).stream().map(mapper::tracking).toList();
    }

    /** Khách tự hủy khi đơn còn Chờ xác nhận. */
    @Transactional
    public OrderTrackingDto cancelByCustomer(String code, String phone) {
        Order o = orders.findByCodeAndPhone(code.trim(), phone.trim())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
        if (o.getStatus() != OrderStatus.PENDING) {
            throw ApiException.badRequest("Chỉ hủy được đơn đang Chờ xác nhận");
        }
        doCancel(o, null, "Khách hủy đơn");
        return mapper.tracking(o);
    }

    // ================================================================ UC15

    private static final java.util.Map<String, Set<OrderStatus>> TABS = java.util.Map.of(
            "PENDING", EnumSet.of(OrderStatus.PENDING),
            "PROCESSING", EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PACKING),
            "SHIPPING", EnumSet.of(OrderStatus.SHIPPING),
            "COMPLETED", EnumSet.of(OrderStatus.COMPLETED),
            "CANCELLED", EnumSet.of(OrderStatus.CANCELLED));

    @Transactional(readOnly = true)
    public PageDto<OrderRowDto> adminList(String tab, String q, int page, int size) {
        Set<OrderStatus> statuses = TABS.getOrDefault(tab == null ? "ALL" : tab.toUpperCase(),
                EnumSet.allOf(OrderStatus.class));
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id").descending());
        var result = q == null || q.isBlank() ? orders.findByStatusIn(statuses, pageable)
                : orders.search(statuses, q.trim(), pageable);
        return PageDto.of(result, mapper::row);
    }

    @Transactional(readOnly = true)
    public OrderDetailDto adminGet(Long id) {
        return mapper.detail(find(id));
    }

    @Transactional(readOnly = true)
    public List<OrderRowDto> adminExport(String tab) {
        return adminList(tab, null, 0, 100).content();
    }

    @Transactional
    public OrderDetailDto advance(Long id, Long staffId) {
        Order o = find(id);
        OrderStatus next = o.advance(staffId);
        if (next == OrderStatus.COMPLETED) {
            if (o.getPaymentMethod() == PaymentMethod.COD) {
                o.setPaymentStatus(PaymentStatus.PAID);
            }
            if (o.getUser() != null) {
                o.getUser().addPoints(o.getPointsEarned());
            }
            o.getItems().forEach(i -> {
                Product p = i.getVariant().getProduct();
                p.setSoldCount(p.getSoldCount() + i.getQty());
            });
        }
        notifyStatus(o);
        orders.flush(); // phát hiện xung đột phiên bản ngay (OptimisticLock → 409)
        return mapper.detail(o);
    }

    @Transactional
    public OrderDetailDto cancel(Long id, Long staffId, String reason) {
        Order o = find(id);
        o.assertCancellable();
        doCancel(o, staffId, reason);
        return mapper.detail(o);
    }

    // ================================================================ nội bộ

    /**
     * Hoàn tồn kho, hoàn lượt mã; đơn đã trả tiền online thì đánh dấu chờ hoàn
     * tiền.
     */
    private void doCancel(Order o, Long actorId, String reason) {
        o.changeStatus(OrderStatus.CANCELLED, actorId, reason);
        o.getItems().forEach(i -> {
            i.getVariant().release(i.getQty());
            i.getVariant().getProduct().refreshDerived();
        });
        if (o.getCoupon() != null) {
            o.getCoupon().restore();
        }
        if (o.getPaymentStatus() == PaymentStatus.PAID && o.getPaymentMethod().isOnline()) {
            o.setPaymentStatus(PaymentStatus.REFUNDED); // tạo yêu cầu hoàn tiền với cổng thanh toán (ngoài phạm vi)
        }
        notifyStatus(o);
    }

    /** Tự hủy đơn QR chưa nhận được tiền sau thời hạn. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelExpiredQrOrders() {
        Instant limit = Instant.now().minus(props.shop().qrExpireMinutes(), ChronoUnit.MINUTES);
        orders.findByStatusAndPaymentMethodAndPaymentStatusAndCreatedAtBefore(
                OrderStatus.PENDING, PaymentMethod.QR, PaymentStatus.UNPAID, limit)
                .forEach(o -> doCancel(o, null, "Quá hạn thanh toán QR"));
    }

    private void notifyStatus(Order o) {
        events.publishEvent(new OrderStatusChangedEvent(o.getCode(), o.getPhone(), o.getEmail(), o.getStatus().name()));
    }

    private Order find(Long id) {
        return orders.findById(id).orElseThrow(() -> ApiException.notFound("Không thấy đơn hàng"));

    }
}
