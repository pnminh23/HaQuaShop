package vn.haqua.shop.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class BannerDtos {

    private BannerDtos() {
    }

    public record BannerDto(Long id, String title, String subtitle, String imageUrl, String altText, String linkUrl,
                            String ctaLabel, int sortOrder, boolean active) {
    }

    public record BannerRequest(@NotBlank @Size(max = 120) String title, @Size(max = 500) String subtitle,
                                @NotBlank String imageUrl, @Size(max = 200) String altText,
                                @Size(max = 255) String linkUrl, @Size(max = 40) String ctaLabel, Boolean active) {
    }

    public record ReorderRequest(List<Long> ids) {
    }
}
