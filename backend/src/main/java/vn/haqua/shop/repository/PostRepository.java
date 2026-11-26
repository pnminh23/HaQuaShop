package vn.haqua.shop.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import vn.haqua.shop.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByPublishedAtNotNull(Pageable pageable);

    Optional<Post> findBySlugAndPublishedAtNotNull(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

}
