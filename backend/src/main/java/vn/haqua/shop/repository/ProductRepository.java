package vn.haqua.shop.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import vn.haqua.shop.entity.Product;
import vn.haqua.shop.enums.ProductStatus;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("select distinct p.brand from Product p where p.brand is not null and p.brand <> '' order by p.brand")
    List<String> findDistinctBrands();

    long countByBrand(String brand);

    boolean existsByBrandAndCategorySlugAndStatusIn(String brand, String categorySlug, Collection<ProductStatus> statuses);

    List<Product> findByBrand(String brand);

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByCategoryId(Long categoryId);

    List<Product> findByStatusInAndNameUnaccentContaining(Collection<ProductStatus> statuses, String q, Pageable pageable);

    List<Product> findByCategoryIdAndStatusInAndIdNot(Long categoryId, Collection<ProductStatus> statuses, Long id, Pageable pageable);

    List<Product> findByStatusIn(Collection<ProductStatus> statuses, Pageable pageable);

    @Query("select p.category.slug, count(p) from Product p where p.status in :statuses group by p.category.slug")
    List<Object[]> countByCategory(Collection<ProductStatus> statuses);

}
