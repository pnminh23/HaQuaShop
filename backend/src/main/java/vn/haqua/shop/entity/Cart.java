package vn.haqua.shop.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "carts")
public class Cart extends BaseEntity {

    /** null nếu là giỏ của khách vãng lai. */
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String sessionToken;

    @ManyToOne(fetch = FetchType.LAZY)
    private Coupon coupon;

    /** Đã thêm hàng qua khung chat AI → đơn được ghi kênh Chatbot. */
    private boolean fromChatbot;

    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CartItem> items = new ArrayList<>();

    public CartItem find(Long variantId) {
        return items.stream().filter(i -> i.getVariant().getId().equals(variantId)).findFirst().orElse(null);
    }

    public void touch() {
        updatedAt = Instant.now();

    }
}
