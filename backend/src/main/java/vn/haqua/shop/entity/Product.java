package vn.haqua.shop.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import vn.haqua.shop.enums.ProductStatus;
import vn.haqua.shop.util.SlugUtil;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    /** Tên không dấu để tìm kiếm. */
    @Column(nullable = false)
    private String nameUnaccent;

    @Column(nullable = false, unique = true)
    private String slug;

    private String brand;

    @Column(length = 8000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProductStatus status = ProductStatus.DRAFT;

    private double ratingAvg;
    private int reviewCount;
    private int soldCount;

    // --- dữ liệu suy ra từ biến thể, phục vụ lọc/sắp xếp nhanh ---
    private long minPrice;
    private long minOriginalPrice;
    private boolean onSale;
    private int totalStock;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("price ASC")
    private List<ProductVariant> variants = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ProductSpec> specs = new ArrayList<>();

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SeoMeta seo;

    public void rename(String newName) {
        this.name = newName;
        this.nameUnaccent = SlugUtil.unaccent(newName + " " + (brand == null ? "" : brand));
    }

    public void setBrand(String brand) {
        this.brand = brand;
        this.nameUnaccent = SlugUtil.unaccent(name + " " + (brand == null ? "" : brand));
    }

    /** Tính lại giá thấp nhất, tồn kho, cờ giảm giá và trạng thái hết hàng. */
    public void refreshDerived() {
        var cheapest = variants.stream().min(Comparator.comparingLong(ProductVariant::getPrice));
        minPrice = cheapest.map(ProductVariant::getPrice).orElse(0L);
        minOriginalPrice = cheapest.map(ProductVariant::getOriginalPrice).orElse(0L);
        onSale = variants.stream().anyMatch(ProductVariant::hasDiscount);
        totalStock = variants.stream().mapToInt(ProductVariant::getStock).sum();
        if (status == ProductStatus.ACTIVE && totalStock == 0 && !variants.isEmpty()) {
            status = ProductStatus.OUT_OF_STOCK;
        } else if (status == ProductStatus.OUT_OF_STOCK && totalStock > 0) {
            status = ProductStatus.ACTIVE;
        }
    }

    public String mainImageUrl() {
        return images.isEmpty() ? null : images.get(0).getUrl();

    }
}
