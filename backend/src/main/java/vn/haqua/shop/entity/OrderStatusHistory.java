package vn.haqua.shop.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import vn.haqua.shop.enums.OrderStatus;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;

    private String fromStatus;
    private String toStatus;

    /** null nếu hệ thống tự đổi (webhook, tự hủy). */
    private Long changedBy;

    private String note;
    private Instant changedAt = Instant.now();

    public OrderStatusHistory(Order order, OrderStatus from, OrderStatus to, Long changedBy, String note) {
        this.order = order;
        this.fromStatus = from == null ? null : from.name();
        this.toStatus = to.name();
        this.changedBy = changedBy;
        this.note = note;

    }
}
