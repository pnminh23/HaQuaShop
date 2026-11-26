package vn.haqua.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "seo_meta")
public class SeoMeta extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;

    private String focusKeyword;

    @Column(length = 120)
    private String metaTitle;

    @Column(length = 320)
    private String metaDescription;

    private int seoScore;

    public SeoMeta(Product product) {
        this.product = product;

    }
}
