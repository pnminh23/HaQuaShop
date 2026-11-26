package vn.haqua.shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.haqua.shop.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByNameIgnoreCase(String name);

    Optional<Brand> findBySlug(String slug);

    List<Brand> findAllByOrderByNameAsc();
}
