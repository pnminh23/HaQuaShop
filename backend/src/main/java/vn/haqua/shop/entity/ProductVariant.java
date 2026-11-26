package vn.haqua.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;

    @Column(nullable = false, unique = true)
    private String sku;

    private String color;
    private String storage;

    /** Giá bán, đơn vị đồng. */
    private long price;

    /** Giá gốc; 0 nếu không giảm giá. */
    private long originalPrice;

    private int stock;

    /** Optimistic lock: 2 khách mua món cuối cùng không làm tồn kho âm. */
    @Version
    private long version;

    public ProductVariant(Product product, String sku, String color, String storage, long price,
                          long originalPrice, int stock) {
        this.product = product;
        this.sku = sku;
        this.color = color;
        this.storage = storage;
        this.price = price;
        this.originalPrice = originalPrice;
        this.stock = stock;
    }

    public String label() {
        return java.util.stream.Stream.of(color, storage)
                .filter(s -> s != null && !s.isBlank())
                .collect(java.util.stream.Collectors.joining(" · "));
    }

    public boolean hasDiscount() {
        return originalPrice > price;
    }

    public void reserve(int qty) {
        if (qty > stock) {
            throw new vn.haqua.shop.exception.ApiException(org.springframework.http.HttpStatus.CONFLICT,
                    "Chỉ còn " + stock + " sản phẩm", java.util.Map.of("available", stock, "variantId", getId()));
        }
        stock -= qty;
    }

    public void release(int qty) {
        stock += qty;

    }
}
