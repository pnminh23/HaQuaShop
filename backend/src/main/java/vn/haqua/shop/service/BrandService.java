package vn.haqua.shop.service;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.CatalogDtos.BrandDto;
import vn.haqua.shop.dto.CatalogDtos.BrandRequest;
import vn.haqua.shop.dto.CatalogDtos.MenuBrandDto;
import vn.haqua.shop.entity.Brand;
import vn.haqua.shop.enums.ProductStatus;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.BrandRepository;
import vn.haqua.shop.repository.CategoryRepository;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.util.SlugUtil;

/** Cấu hình danh mục thương hiệu; cho phép tạo nhanh ngay trong form sản phẩm. */
@Service
@RequiredArgsConstructor
public class BrandService {

    public static final String COMPUTER_CATEGORY = "may-tinh";

    private final BrandRepository brands;
    private final ProductRepository products;
    private final CategoryRepository categories;

    /** Danh sách thương hiệu; tự bổ sung các thương hiệu đã có sẵn trên sản phẩm cũ nhưng chưa có trong bảng. */
    @Transactional
    public List<BrandDto> list() {
        for (String name : products.findDistinctBrands()) ensure(name);
        return brands.findAllByOrderByNameAsc().stream().map(b -> toDto(b, products.countByBrand(b.getName()))).toList();
    }

    /** Lấy thương hiệu theo tên (không phân biệt hoa thường), chưa có thì tạo mới. */
    @Transactional
    public Brand ensure(String rawName) {
        String name = rawName == null ? "" : rawName.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) throw ApiException.badRequest("Tên thương hiệu không được để trống");
        if (name.length() > 100) throw ApiException.badRequest("Tên thương hiệu tối đa 100 ký tự");
        return brands.findByNameIgnoreCase(name).orElseGet(() -> {
            String base = SlugUtil.toSlug(name);
            if (base.isEmpty()) base = "brand";
            String slug = base;
            for (int i = 2; brands.findBySlug(slug).isPresent(); i++) slug = base + "-" + i;
            return brands.save(new Brand(name, slug));
        });
    }

    @Transactional
    public BrandDto create(BrandRequest req) {
        Brand b = ensure(req.name());
        configure(b, req);
        return toDto(b, products.countByBrand(b.getName()));
    }

    /** Các hãng máy tính hiện trên menu cửa hàng. */
    @Transactional(readOnly = true)
    public List<MenuBrandDto> computerMenu() {
        if (categories.findBySlug(COMPUTER_CATEGORY).filter(c -> !c.isShown()).isPresent()) {
            return List.of(); // danh mục máy tính đang ẩn thì menu hãng máy tính cũng ẩn
        }
        return brands.findByComputerBrandTrue().stream()
                .sorted(Comparator.comparingInt(BrandService::order).thenComparing(Brand::getName, String.CASE_INSENSITIVE_ORDER))
                .map(b -> new MenuBrandDto(b.getName(), b.getSlug()))
                .toList();
    }

    /** Hãng chưa cấu hình mà đang có máy tính bán thì tự bật; lựa chọn của admin (bật/tắt) được giữ nguyên. */
    @Transactional
    public void detectComputerBrands() {
        for (String name : products.findDistinctBrands()) {
            Brand b = ensure(name);
            if (b.getComputerBrand() == null && products.existsByBrandAndCategorySlugAndStatusIn(
                    b.getName(), COMPUTER_CATEGORY, EnumSet.of(ProductStatus.ACTIVE, ProductStatus.OUT_OF_STOCK))) {
                b.setComputerBrand(true);
            }
        }
    }

    private static int order(Brand b) {
        return b.getMenuOrder() == null ? 0 : b.getMenuOrder();
    }

    private void configure(Brand b, BrandRequest req) {
        if (req.computerBrand() != null) b.setComputerBrand(req.computerBrand());
        if (req.menuOrder() != null) b.setMenuOrder(req.menuOrder());
    }

    private BrandDto toDto(Brand b, long count) {
        return new BrandDto(b.getId(), b.getName(), b.getSlug(), count, Boolean.TRUE.equals(b.getComputerBrand()), order(b));
    }

    /** Sửa thương hiệu; đổi tên thì cập nhật tên trên mọi sản phẩm đang dùng nó. */
    @Transactional
    public BrandDto update(Long id, BrandRequest req) {
        Brand b = brands.findById(id).orElseThrow(() -> ApiException.notFound("Thương hiệu không tồn tại"));
        String name = req.name() == null ? "" : req.name().trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) throw ApiException.badRequest("Tên thương hiệu không được để trống");
        if (name.length() > 100) throw ApiException.badRequest("Tên thương hiệu tối đa 100 ký tự");
        brands.findByNameIgnoreCase(name).filter(o -> !o.getId().equals(id))
                .ifPresent(o -> { throw ApiException.conflict("Đã có thương hiệu “" + o.getName() + "”"); });
        String old = b.getName();
        if (!old.equals(name)) {
            products.findByBrand(old).forEach(p -> p.setBrand(name));
            b.setName(name);
        }
        configure(b, req);
        return toDto(b, products.countByBrand(name));
    }

    /** Chỉ xóa được thương hiệu chưa gắn với sản phẩm nào. */
    @Transactional
    public void delete(Long id) {
        Brand b = brands.findById(id).orElseThrow(() -> ApiException.notFound("Thương hiệu không tồn tại"));
        if (products.countByBrand(b.getName()) > 0) throw ApiException.conflict("Thương hiệu đang được dùng bởi sản phẩm, không thể xóa");
        brands.delete(b);
    }
}
