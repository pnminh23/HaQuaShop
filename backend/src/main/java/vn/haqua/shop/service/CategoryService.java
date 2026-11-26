package vn.haqua.shop.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.CatalogDtos.CategoryDto;
import vn.haqua.shop.dto.CatalogDtos.CategoryRequest;
import vn.haqua.shop.entity.Category;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.CategoryRepository;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.util.SlugUtil;

/** UC18 – Quản lý danh mục. */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;

    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return categories.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public CategoryDto create(CategoryRequest req) {
        String slug = req.slug() == null || req.slug().isBlank() ? SlugUtil.toSlug(req.name()) : req.slug();
        if (categories.existsBySlug(slug)) {
            throw ApiException.conflict("Slug đã tồn tại");
        }
        Category c = new Category(req.name(), slug, req.seoDescription(), (int) categories.count());
        applyParent(c, req.parentId());
        return toDto(categories.save(c));
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest req) {
        Category c = categories.findById(id).orElseThrow(() -> ApiException.notFound("Không thấy danh mục"));
        String slug = req.slug() == null || req.slug().isBlank() ? SlugUtil.toSlug(req.name()) : req.slug();
        if (categories.existsBySlugAndIdNot(slug, id)) {
            throw ApiException.conflict("Slug đã tồn tại");
        }
        c.setName(req.name());
        c.setSlug(slug);
        c.setSeoDescription(req.seoDescription());
        applyParent(c, req.parentId());
        return toDto(c);
    }

    @Transactional
    public void delete(Long id) {
        if (products.countByCategoryId(id) > 0) {
            throw ApiException.conflict("Danh mục còn sản phẩm, không thể xóa");
        }
        categories.deleteById(id);
    }

    private void applyParent(Category c, Long parentId) {
        c.setParent(parentId == null ? null
                : categories.findById(parentId).orElseThrow(() -> ApiException.notFound("Không thấy danh mục cha")));
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getSlug(), c.getSeoDescription(),
                c.getParent() == null ? null : c.getParent().getId(), products.countByCategoryId(c.getId()));

    }
}
