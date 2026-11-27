package vn.haqua.shop.entity;

import jakarta.persistence.Column;
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
@Table(name = "categories")
public class Category extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private Category parent;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(length = 2000)
    private String seoDescription;

    private int sortOrder;

    /** Ẩn khỏi cửa hàng cùng mọi sản phẩm thuộc nó. Trạng thái sản phẩm giữ nguyên nên hiện lại là như cũ. */
    @Column(name = "hidden")
    private Boolean hidden;

    /** Khách có được thấy không: bản thân và mọi danh mục cha đều không bị ẩn. */
    public boolean isShown() {
        return !Boolean.TRUE.equals(hidden) && (parent == null || parent.isShown());
    }

    public Category(String name, String slug, String seoDescription, int sortOrder) {
        this.name = name;
        this.slug = slug;
        this.seoDescription = seoDescription;
        this.sortOrder = sortOrder;

    }
}
