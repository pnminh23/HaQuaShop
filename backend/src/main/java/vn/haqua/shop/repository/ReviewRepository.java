package vn.haqua.shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import vn.haqua.shop.entity.Review;
import vn.haqua.shop.enums.ReviewStatus;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductIdAndStatus(Long productId, ReviewStatus status, Pageable pageable);

    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);

    @Query("select r.stars, count(r) from Review r where r.product.id = :productId and r.status = 'APPROVED' group by r.stars")
    List<Object[]> distribution(Long productId);

}
