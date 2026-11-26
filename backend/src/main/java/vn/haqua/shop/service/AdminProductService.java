package vn.haqua.shop.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.CatalogDtos.*;
import vn.haqua.shop.dto.PageDto;
import vn.haqua.shop.entity.Category;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.entity.ProductImage;
import vn.haqua.shop.entity.ProductSpec;
import vn.haqua.shop.entity.ProductVariant;
import vn.haqua.shop.entity.SeoMeta;
import vn.haqua.shop.entity.SlugRedirect;
import vn.haqua.shop.enums.ProductStatus;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.mapper.ProductMapper;
import vn.haqua.shop.repository.CategoryRepository;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.repository.ProductVariantRepository;
import vn.haqua.shop.repository.SlugRedirectRepository;
import vn.haqua.shop.util.SlugUtil;

/** UC13 – Quản lý sản phẩm, UC14 – SEO sản phẩm (phía quản trị). */
@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CategoryRepository categories;
    private final SlugRedirectRepository redirects;
    private final SeoService seoService;
    private final ProductMapper mapper;
    private final BrandService brandService;

    @Transactional(readOnly = true)
    public PageDto<AdminProductRow> list(ProductStatus status, Long categoryId, String q, int page, int size) {
        Specification<Product> spec = (root, query, cb) -> {
            var preds = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                preds.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (q != null && !q.isBlank()) {
                preds.add(cb.like(root.get("nameUnaccent"), "%" + SlugUtil.unaccent(q.trim()) + "%"));
            }
            return cb.and(preds.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = products.findAll(spec, PageRequest.of(page, Math.min(size, 100), Sort.by("id").descending()));
        return PageDto.of(result, p -> new AdminProductRow(p.getId(), p.getName(), p.getSlug(),
                p.getCategory().getName(), p.getMinPrice(), p.getTotalStock(), p.getStatus(),
                p.getSeo() == null ? 0 : p.getSeo().getSeoScore(), p.mainImageUrl()));
    }

    @Transactional(readOnly = true)
    public ProductDetailDto get(Long id) {
        return mapper.toDetail(find(id), List.of());
    }

    @Transactional
    public ProductDetailDto create(AdminProductRequest req) {
        Product p = new Product();
        apply(p, req, null);
        return mapper.toDetail(products.save(p), List.of());
    }

    @Transactional
    public ProductDetailDto update(Long id, AdminProductRequest req) {
        Product p = find(id);
        String oldSlug = p.getSlug();
        apply(p, req, id);
        if (!oldSlug.equals(p.getSlug())) {
            redirects.save(new SlugRedirect(oldSlug, p.getSlug()));
        }
        return mapper.toDetail(p, List.of());
    }

    /** Sản phẩm đã có trong đơn không xóa được, chỉ chuyển sang Ẩn. */
    @Transactional
    public void hide(Long id) {
        Product p = find(id);
        p.setStatus(ProductStatus.HIDDEN);
    }

    /** Hiện lại sản phẩm đã ẩn: về Đang bán, hoặc Hết hàng nếu kho đang trống (refreshDerived tự quyết). */
    @Transactional
    public void show(Long id) {
        Product p = find(id);
        p.setStatus(ProductStatus.ACTIVE);
        p.refreshDerived();
    }

    @Transactional(readOnly = true)
    public SeoReport seoReport(AdminProductRequest req) {
        return seoService.evaluate(req.seo() == null ? null : req.seo().focusKeyword(), resolveSlug(req),
                req.seo() == null ? null : req.seo().metaTitle(),
                req.seo() == null ? null : req.seo().metaDescription(), req.images());
    }

    // ----------------------------------------------------------------

    private void apply(Product p, AdminProductRequest req, Long id) {
        Category category = categories.findById(req.categoryId())
                .orElseThrow(() -> ApiException.badRequest("Danh mục không tồn tại"));
        boolean publishing = req.status() == ProductStatus.ACTIVE;
        validate(req, publishing, id);

        p.setCategory(category);
        p.setBrand(req.brand() == null || req.brand().isBlank() ? null : brandService.ensure(req.brand()).getName());
        p.rename(req.name());
        p.setDescription(req.description());
        p.setSlug(resolveSlug(req));
        p.setStatus(req.status());

        mergeVariants(p, req.variants());

        p.getImages().clear();
        if (req.images() != null) {
            int order = 0;
            for (ImageInput i : req.images()) {
                p.getImages().add(new ProductImage(p, i.url(), i.altText(), order++));
            }
        }
        p.getSpecs().clear();
        if (req.specs() != null) {
            req.specs().forEach(s -> p.getSpecs().add(new ProductSpec(p, s.key(), s.value())));
        }

        SeoMeta seo = p.getSeo() == null ? new SeoMeta(p) : p.getSeo();
        SeoInput in = req.seo();
        if (in != null) {
            seo.setFocusKeyword(in.focusKeyword());
            seo.setMetaTitle(in.metaTitle());
            seo.setMetaDescription(in.metaDescription());
        }
        // chấm lại điểm phía server rồi mới lưu
        seo.setSeoScore(seoService.evaluate(seo.getFocusKeyword(), p.getSlug(), seo.getMetaTitle(),
                seo.getMetaDescription(), req.images()).score());
        p.setSeo(seo);

        p.refreshDerived();
    }

    private void mergeVariants(Product p, List<VariantInput> inputs) {
        for (VariantInput in : inputs) {
            ProductVariant v = in.id() == null ? null : p.getVariants().stream()
                    .filter(x -> in.id().equals(x.getId())).findFirst()
                    .orElseThrow(() -> ApiException.badRequest("Biến thể không thuộc sản phẩm"));
            if (v == null) {
                if (variants.existsBySku(in.sku())) {
                    throw ApiException.conflict("SKU đã tồn tại: " + in.sku());
                }
                v = new ProductVariant(p, in.sku(), in.color(), in.storage(), in.price(), in.originalPrice(), in.stock());
                p.getVariants().add(v);
            } else {
                v.setColor(in.color());
                v.setStorage(in.storage());
                v.setPrice(in.price());
                v.setOriginalPrice(in.originalPrice());
                v.setStock(in.stock());
            }
        }
    }

    private void validate(AdminProductRequest req, boolean publishing, Long id) {
        String slug = resolveSlug(req);
        boolean taken = id == null ? products.existsBySlug(slug) : products.existsBySlugAndIdNot(slug, id);
        if (taken) {
            throw ApiException.conflict("Slug đã được sản phẩm khác dùng");
        }
        if (!publishing) {
            return; // lưu nháp chỉ cần tên + danh mục
        }
        if (req.images() == null || req.images().isEmpty()) {
            throw ApiException.unprocessable("Cần ít nhất 1 ảnh để đăng bán");
        }
        for (VariantInput v : req.variants()) {
            if (v.originalPrice() > 0 && v.price() > v.originalPrice()) {
                throw ApiException.unprocessable("Giá bán phải nhỏ hơn hoặc bằng giá gốc (SKU " + v.sku() + ")");
            }
        }
    }

    private String resolveSlug(AdminProductRequest req) {
        return req.slug() == null || req.slug().isBlank() ? SlugUtil.toSlug(req.name()) : SlugUtil.toSlug(req.slug());
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> ApiException.notFound("Không thấy sản phẩm"));

    }
}
