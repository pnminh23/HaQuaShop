package vn.haqua.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chuyển hướng 301 từ slug cũ sang slug mới khi admin đổi đường dẫn. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "slug_redirects")
public class SlugRedirect extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String oldSlug;

    @Column(nullable = false)
    private String newSlug;

    public SlugRedirect(String oldSlug, String newSlug) {
        this.oldSlug = oldSlug;
        this.newSlug = newSlug;

    }
}
