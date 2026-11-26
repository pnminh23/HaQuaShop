package vn.haqua.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Danh mục thương hiệu (cấu hình). Product.brand lưu tên chuẩn của thương hiệu này. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "brands")
public class Brand extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    /** Hiện trên menu "hãng máy tính". null = chưa ai cấu hình, hệ thống tự bật khi hãng có máy tính. */
    @Column(name = "computer_brand")
    private Boolean computerBrand;

    /** Thứ tự trên menu, số nhỏ đứng trước; trùng số thì xếp theo tên. */
    @Column(name = "menu_order")
    private Integer menuOrder;

    public Brand(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }
}
