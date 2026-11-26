package vn.haqua.shop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cart_items")
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ProductVariant variant;

    private int qty;
    private boolean selected = true;
    private boolean savedForLater;

    public CartItem(Cart cart, ProductVariant variant, int qty) {
        this.cart = cart;
        this.variant = variant;
        this.qty = qty;

    }
}
