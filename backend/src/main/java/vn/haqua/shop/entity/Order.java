package vn.haqua.shop.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.haqua.shop.enums.OrderChannel;
import vn.haqua.shop.enums.OrderStatus;
import vn.haqua.shop.enums.PaymentMethod;
import vn.haqua.shop.enums.PaymentStatus;
import vn.haqua.shop.enums.ShippingMethod;
import vn.haqua.shop.exception.ApiException;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    /** Dạng HQ-yyMMdd-xxxx. */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Coupon coupon;

    private String receiverName;
    private String phone;
    private String email;

    @Column(length = 500)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private OrderChannel channel = OrderChannel.WEBSITE;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ShippingMethod shippingMethod;

    private long subtotal;
    private long discount;
    private long shippingFee;
    private long total;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private OrderStatus status = OrderStatus.PENDING;

    private String invoiceTaxCode;
    private String invoiceCompany;
    private String invoiceEmail;

    private int pointsEarned;

    @Column(length = 1000)
    private String note;

    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC, id ASC")
    private List<OrderStatusHistory> history = new ArrayList<>();

    public void changeStatus(OrderStatus to, Long actorId, String reason) {
        history.add(new OrderStatusHistory(this, status, to, actorId, reason));
        this.status = to;
    }

    /** Chuyển sang bước kế tiếp theo đúng thứ tự. */
    public OrderStatus advance(Long actorId) {
        OrderStatus next = status.next()
                .orElseThrow(() -> ApiException.badRequest("Đơn ở trạng thái cuối, không thể chuyển tiếp"));
        changeStatus(next, actorId, null);
        return next;
    }

    public void assertCancellable() {
        if (!status.isCancellable()) {
            throw ApiException.badRequest(status == OrderStatus.CANCELLED
                    ? "Đơn đã bị hủy trước đó"
                    : "Không thể hủy đơn ở trạng thái hiện tại");

        }
    }
}
