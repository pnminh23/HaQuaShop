package vn.haqua.shop.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.CouponDtos.CouponDto;
import vn.haqua.shop.dto.CouponDtos.CouponRequest;
import vn.haqua.shop.entity.Coupon;
import vn.haqua.shop.enums.CouponType;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.CouponRepository;

/** UC07 (phía khách: kiểm tra mã) và UC19 (admin: quản lý mã). */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository coupons;

    /** Tìm mã và kiểm tra điều kiện với tạm tính. */
    @Transactional(readOnly = true)
    public Coupon validate(String code, long subtotal) {
        Coupon c = coupons.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> ApiException.unprocessable("Mã giảm giá không tồn tại"));
        c.assertApplicable(subtotal);
        return c;
    }

    @Transactional(readOnly = true)
    public List<CouponDto> list() {
        return coupons.findAll(Sort.by("id").descending()).stream().map(CouponService::toDto).toList();
    }

    @Transactional
    public CouponDto create(CouponRequest req) {
        if (coupons.existsByCodeIgnoreCase(req.code())) {
            throw ApiException.conflict("Mã giảm giá đã tồn tại");
        }
        Coupon c = new Coupon();
        c.setCode(req.code().trim().toUpperCase());
        apply(c, req);
        return toDto(coupons.save(c));
    }

    @Transactional
    public CouponDto update(Long id, CouponRequest req) {
        Coupon c = coupons.findById(id).orElseThrow(() -> ApiException.notFound("Không thấy mã giảm giá"));
        apply(c, req);
        return toDto(c);
    }

    /** Tạm ngưng: đặt ngày kết thúc là hiện tại. */
    @Transactional
    public CouponDto suspend(Long id) {
        Coupon c = coupons.findById(id).orElseThrow(() -> ApiException.notFound("Không thấy mã giảm giá"));
        c.setEndAt(java.time.Instant.now());
        return toDto(c);
    }

    private void apply(Coupon c, CouponRequest req) {
        if (req.startAt() != null && req.endAt() != null && req.endAt().isBefore(req.startAt())) {
            throw ApiException.badRequest("Ngày kết thúc phải sau ngày bắt đầu");
        }
        if (req.type() == CouponType.PERCENT && req.value() > 100) {
            throw ApiException.badRequest("Phần trăm giảm tối đa 100");
        }
        c.setType(req.type());
        c.setValue(req.value());
        c.setMaxDiscount(req.maxDiscount());
        c.setMinOrder(req.minOrder());
        c.setStartAt(req.startAt());
        c.setEndAt(req.endAt());
        c.setUsageLimit(req.usageLimit());
    }

    private static CouponDto toDto(Coupon c) {
        return new CouponDto(c.getId(), c.getCode(), c.getType(), c.getValue(), c.getMaxDiscount(), c.getMinOrder(),
                c.getStartAt(), c.getEndAt(), c.getUsageLimit(), c.getUsedCount());

    }
}
