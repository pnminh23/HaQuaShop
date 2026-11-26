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
@Table(name = "product_specs")
public class ProductSpec extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;

    private String specKey;
    private String specValue;

    public ProductSpec(Product product, String specKey, String specValue) {
        this.product = product;
        this.specKey = specKey;
        this.specValue = specValue;

    }
}
