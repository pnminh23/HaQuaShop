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
@Table(name = "product_images")
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;

    private String url;
    private String altText;
    private int sortOrder;

    public ProductImage(Product product, String url, String altText, int sortOrder) {
        this.product = product;
        this.url = url;
        this.altText = altText;
        this.sortOrder = sortOrder;

    }
}
