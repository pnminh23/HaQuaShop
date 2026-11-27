package vn.haqua.shop.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.haqua.shop.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Category> findAllByOrderBySortOrderAscIdAsc();

    /** Id các danh mục khách không được thấy; luôn có -1 để dùng được trong "not in". */
    default Set<Long> hiddenIds() {
        Set<Long> ids = findAll().stream().filter(c -> !c.isShown()).map(Category::getId)
                .collect(Collectors.toCollection(HashSet::new));
        ids.add(-1L);
        return ids;
    }

}
