package vn.haqua.shop.service;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.CatalogDtos.*;
import vn.haqua.shop.dto.PageDto;
import vn.haqua.shop.dto.ProductFilter;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.enums.ProductStatus;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.mapper.ProductMapper;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.repository.ProductSpecifications;
import vn.haqua.shop.repository.SlugRedirectRepository;
import vn.haqua.shop.util.SlugUtil;

/** UC03, UC04, UC05 – tìm, lọc, xem chi tiết (phía khách). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final EnumSet<ProductStatus> VISIBLE = EnumSet.of(ProductStatus.ACTIVE, ProductStatus.OUT_OF_STOCK);

    private final ProductRepository products;
    private final SlugRedirectRepository redirects;
    private final ProductMapper mapper;

    public ProductListResponse search(ProductFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 48),
                ProductSpecifications.sort(filter.sort()));
        var result = products.findAll(ProductSpecifications.from(filter), pageable);
        Map<String, Long> facets = new LinkedHashMap<>();
        products.countByCategory(VISIBLE).forEach(r -> facets.put((String) r[0], (Long) r[1]));
        return new ProductListResponse(PageDto.of(result, mapper::toCard), facets);
    }

    public List<SuggestDto> suggest(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return products.findByStatusInAndNameUnaccentContaining(VISIBLE, SlugUtil.unaccent(q.trim()), PageRequest.of(0, 6))
                .stream().map(p -> new SuggestDto(p.getSlug(), p.getName(), p.getMinPrice(), p.mainImageUrl())).toList();
    }

    /** Trả chi tiết; nếu slug cũ đã đổi, ném RedirectException để controller trả 301. */
    public ProductDetailDto detail(String slug) {
        Product p = products.findBySlug(slug).filter(x -> x.getStatus().isVisible()).orElse(null);
        if (p == null) {
            redirects.findByOldSlug(slug).ifPresent(r -> {
                throw new SlugMovedException(r.getNewSlug());
            });
            throw ApiException.notFound("Không tìm thấy sản phẩm");
        }
        List<ProductCardDto> related = products
                .findByCategoryIdAndStatusInAndIdNot(p.getCategory().getId(), VISIBLE, p.getId(),
                        PageRequest.of(0, 4, ProductSpecifications.sort("popular")))
                .stream().map(mapper::toCard).toList();
        return mapper.toDetail(p, related);
    }

    public List<ProductCardDto> featured(int limit) {
        return products.findByStatusIn(VISIBLE, PageRequest.of(0, limit, ProductSpecifications.sort("popular")))
                .stream().map(mapper::toCard).toList();
    }

    public static class SlugMovedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final String newSlug;

        public SlugMovedException(String newSlug) {
            super(newSlug);
            this.newSlug = newSlug;
        }

        public String getNewSlug() {
            return newSlug;

        }
    }
}
