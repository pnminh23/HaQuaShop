package vn.haqua.shop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.haqua.shop.entity.SlugRedirect;

public interface SlugRedirectRepository extends JpaRepository<SlugRedirect, Long> {

    Optional<SlugRedirect> findByOldSlug(String oldSlug);

}
