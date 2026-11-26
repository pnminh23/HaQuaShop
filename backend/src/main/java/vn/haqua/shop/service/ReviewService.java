package vn.haqua.shop.service;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.ReviewDtos.*;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.entity.Review;
import vn.haqua.shop.enums.ReviewStatus;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.OrderRepository;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.repository.ReviewRepository;
import vn.haqua.shop.repository.UserRepository;

/** UC10 – Đánh giá sản phẩm: chỉ thành viên đã mua (đơn COMPLETED) mới đánh giá được. */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviews;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public ReviewList list(Long productId, int page) {
        var rows = reviews.findByProductIdAndStatus(productId, ReviewStatus.APPROVED,
                PageRequest.of(page, 10, Sort.by("id").descending()));
        return new ReviewList(summary(productId), rows.stream().map(r -> new ReviewDto(r.getId(),
                r.getUser().getFullName(), r.getStars(), r.getContent(), true, r.getCreatedAt())).toList());
    }

    @Transactional
    public ReviewDto create(Long userId, Long productId, ReviewRequest req) {
        Product product = products.findById(productId).orElseThrow(() -> ApiException.notFound("Không thấy sản phẩm"));
        var purchased = orders.findCompletedItems(userId, productId);
        if (purchased.isEmpty()) {
            throw ApiException.forbidden("Chỉ khách đã mua mới đánh giá được");
        }
        Review r = reviews.findByUserIdAndProductId(userId, productId).orElseGet(Review::new); // đã đánh giá → sửa
        r.setProduct(product);
        r.setUser(users.getReferenceById(userId));
        r.setOrderItem(purchased.get(0));
        r.setStars(req.stars());
        r.setContent(req.content());
        reviews.saveAndFlush(r);
        refreshRating(product);
        return new ReviewDto(r.getId(), users.getReferenceById(userId).getFullName(), r.getStars(), r.getContent(),
                true, r.getCreatedAt());
    }

    private void refreshRating(Product product) {
        ReviewSummary s = summary(product.getId());
        product.setRatingAvg(s.average());
        product.setReviewCount(s.count());
    }

    private ReviewSummary summary(Long productId) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (int s = 5; s >= 1; s--) {
            counts.put(s, 0);
        }
        reviews.distribution(productId).forEach(row -> counts.put((Integer) row[0], ((Long) row[1]).intValue()));
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();
        double avg = total == 0 ? 0 : counts.entrySet().stream().mapToDouble(e -> e.getKey() * e.getValue()).sum() / total;
        Map<Integer, Integer> pct = new LinkedHashMap<>();
        counts.forEach((star, n) -> pct.put(star, total == 0 ? 0 : (int) Math.round(n * 100.0 / total)));
        return new ReviewSummary(Math.round(avg * 10) / 10.0, total, pct);

    }
}
