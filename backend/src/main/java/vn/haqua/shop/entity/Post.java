package vn.haqua.shop.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "posts")
public class Post extends BaseEntity {

    private Long authorId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    private String tag;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String coverUrl;
    private String metaTitle;
    private String metaDescription;

    /** null = bản nháp. */
    private Instant publishedAt;

}
