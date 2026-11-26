package vn.haqua.shop.service;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import vn.haqua.shop.dto.CatalogDtos.SeoCriterion;
import vn.haqua.shop.dto.CatalogDtos.SeoReport;
import vn.haqua.shop.dto.CatalogDtos;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.util.SlugUtil;

/** UC14 – chấm điểm SEO /100 (6 tiêu chí). Cùng logic với bản xem trước ở frontend. */
@Service
@RequiredArgsConstructor
public class SeoService {

    private final ProductRepository products;

    public SeoReport evaluate(String keyword, String slug, String title, String description,
                              List<CatalogDtos.ImageInput> images) {
        String kw = SlugUtil.unaccent(keyword == null ? "" : keyword.trim());
        boolean hasKw = !kw.isEmpty();
        String t = SlugUtil.unaccent(title == null ? "" : title);
        String d = SlugUtil.unaccent(description == null ? "" : description);
        String s = slug == null ? "" : slug;
        int tl = title == null ? 0 : title.length();
        int dl = description == null ? 0 : description.length();

        List<SeoCriterion> list = new ArrayList<>();
        list.add(new SeoCriterion("Từ khóa chính có trong meta title", hasKw && t.contains(kw)));
        list.add(new SeoCriterion("Từ khóa chính có trong meta description", hasKw && d.contains(kw)));
        list.add(new SeoCriterion("Từ khóa chính có trong đường dẫn (slug)", hasKw && s.contains(SlugUtil.toSlug(kw))));
        list.add(new SeoCriterion("Meta title dài 30–60 ký tự", tl >= 30 && tl <= 60));
        list.add(new SeoCriterion("Meta description dài 120–160 ký tự", dl >= 120 && dl <= 160));
        boolean altOk = images != null && !images.isEmpty()
                && images.stream().allMatch(i -> i.altText() != null && !i.altText().isBlank());
        list.add(new SeoCriterion("Mọi ảnh đều có văn bản thay thế (alt)", altOk));
        long ok = list.stream().filter(SeoCriterion::ok).count();
        return new SeoReport((int) Math.round(ok * 100.0 / list.size()), list);
    }

    public boolean slugAvailable(String slug, Long excludeProductId) {
        return excludeProductId == null ? !products.existsBySlug(slug) : !products.existsBySlugAndIdNot(slug, excludeProductId);

    }
}
