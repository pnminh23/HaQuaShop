package vn.haqua.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import vn.haqua.shop.entity.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySku(String sku);

    @Query("select v from ProductVariant v join fetch v.product where v.stock <= :threshold and v.product.status in ('ACTIVE','OUT_OF_STOCK') order by v.stock asc")
    List<ProductVariant> findLowStock(int threshold);

}
