package vn.haqua.shop.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.dto.BannerDtos.BannerDto;
import vn.haqua.shop.dto.BannerDtos.BannerRequest;
import vn.haqua.shop.entity.Banner;
import vn.haqua.shop.exception.ApiException;
import vn.haqua.shop.repository.BannerRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class BannerService {

    private final BannerRepository banners;

    @Transactional(readOnly = true)
    public List<BannerDto> active() {
        return banners.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<BannerDto> all() {
        return banners.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toDto).toList();
    }

    public BannerDto create(BannerRequest req) {
        Banner b = new Banner();
        apply(b, req);
        b.setSortOrder((int) banners.count());
        return toDto(banners.save(b));
    }

    public BannerDto update(Long id, BannerRequest req) {
        Banner b = find(id);
        apply(b, req);
        return toDto(b);
    }

    public void delete(Long id) {
        banners.delete(find(id));
    }

    /** Sắp xếp lại theo thứ tự danh sách id gửi lên. */
    public List<BannerDto> reorder(List<Long> ids) {
        Map<Long, Integer> pos = new HashMap<>();
        for (int i = 0; ids != null && i < ids.size(); i++) {
            pos.put(ids.get(i), i);
        }
        banners.findAll().forEach(b -> b.setSortOrder(pos.getOrDefault(b.getId(), Integer.MAX_VALUE / 2)));
        return all();
    }

    private void apply(Banner b, BannerRequest r) {
        String link = blankToNull(r.linkUrl());
        if (link != null && !(link.startsWith("/") || link.startsWith("https://") || link.startsWith("http://"))) {
            throw ApiException.badRequest("Liên kết phải bắt đầu bằng / hoặc http(s)://");
        }
        b.setTitle(r.title().trim());
        b.setSubtitle(blankToNull(r.subtitle()));
        b.setImageUrl(r.imageUrl().trim());
        b.setAltText(blankToNull(r.altText()));
        b.setLinkUrl(link);
        b.setCtaLabel(blankToNull(r.ctaLabel()));
        b.setActive(r.active() == null || r.active());
    }

    private Banner find(Long id) {
        return banners.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy banner"));
    }

    private BannerDto toDto(Banner b) {
        return new BannerDto(b.getId(), b.getTitle(), b.getSubtitle(), b.getImageUrl(), b.getAltText(), b.getLinkUrl(),
                b.getCtaLabel(), b.getSortOrder(), b.isActive());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
