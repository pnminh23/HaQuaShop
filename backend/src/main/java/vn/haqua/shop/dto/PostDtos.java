package vn.haqua.shop.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public final class PostDtos {

    private PostDtos() {
    }

    public record PostRequest(@NotBlank String title, String slug, String tag, String content, String coverUrl,
                              String metaTitle, String metaDescription, boolean publish) {
    }

    public record PostDto(Long id, String title, String slug, String tag, String content, String coverUrl,
                          String metaTitle, String metaDescription, Instant publishedAt) {
    }
}
