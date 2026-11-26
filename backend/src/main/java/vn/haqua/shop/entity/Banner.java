package vn.haqua.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Slide của banner trang chủ, do admin cấu hình. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "banners")
public class Banner extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(nullable = false)
    private String imageUrl;

    private String altText;

    /** Đường dẫn nội bộ (/san-pham...) hoặc URL ngoài; trống = không có nút. */
    private String linkUrl;

    private String ctaLabel;

    private int sortOrder;

    private boolean active = true;
}
