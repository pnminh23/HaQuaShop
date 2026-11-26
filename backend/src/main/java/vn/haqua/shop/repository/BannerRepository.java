package vn.haqua.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.haqua.shop.entity.Banner;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByActiveTrueOrderBySortOrderAscIdAsc();

    List<Banner> findAllByOrderBySortOrderAscIdAsc();
}
