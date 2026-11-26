package vn.haqua.shop.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record ReviewRequest(@Min(1) @Max(5) int stars,
                                @NotBlank @Size(min = 10, max = 1000, message = "Nội dung từ 10 đến 1.000 ký tự") String content) {
    }

    public record ReviewDto(Long id, String author, int stars, String content, boolean verifiedPurchase, Instant createdAt) {
    }

    public record ReviewSummary(double average, int count, Map<Integer, Integer> percentByStar) {
    }

    public record ReviewList(ReviewSummary summary, List<ReviewDto> reviews) {
    }
}
