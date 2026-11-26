package vn.haqua.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bản chụp tên, biến thể, giá tại lúc mua: đổi giá hay xóa sản phẩm sau này không ảnh hưởng đơn cũ. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ProductVariant variant;

    private String productName;
    private String variantLabel;
    private long unitPrice;
    private int qty;

    public OrderItem(Order order, ProductVariant v, int qty) {
        this.order = order;
        this.variant = v;
        this.productName = v.getProduct().getName();
        this.variantLabel = v.label();
        this.unitPrice = v.getPrice();
        this.qty = qty;
    }

    public long lineTotal() {
        return unitPrice * qty;

    }
}
