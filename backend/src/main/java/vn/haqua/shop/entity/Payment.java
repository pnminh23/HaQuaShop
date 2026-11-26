package vn.haqua.shop.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.haqua.shop.enums.PaymentMethod;
import vn.haqua.shop.enums.PaymentTxnStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    private String providerTxnId;
    private long amount;
    private int installmentMonths;

    @Enumerated(EnumType.STRING)
    private PaymentTxnStatus status = PaymentTxnStatus.PENDING;

    private Instant paidAt;

    public Payment(Order order, PaymentMethod method, long amount, int installmentMonths) {
        this.order = order;
        this.method = method;
        this.amount = amount;
        this.installmentMonths = installmentMonths;

    }
}
