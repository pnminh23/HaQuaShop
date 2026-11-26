package vn.haqua.shop.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.entity.KnowledgeSource;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.enums.ChatEnums.KnowledgeType;
import vn.haqua.shop.enums.ProductStatus;
import vn.haqua.shop.repository.KnowledgeSourceRepository;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.util.SlugUtil;

/** Lấy ngữ cảnh từ các nguồn kiến thức đang bật: sản phẩm (giá, tồn kho thật), chính sách, FAQ. */
@Component
@RequiredArgsConstructor
public class KnowledgeRetriever {

    private static final Set<String> STOP = Set.of("toi", "minh", "muon", "can", "tim", "mua", "cho", "co", "la",
            "va", "nao", "gi", "the", "nhe", "duoc", "khong", "mot", "cai", "ban", "tu", "van", "hoi");

    private final ProductRepository products;
    private final KnowledgeSourceRepository sources;

    public record Retrieved(String context, List<Product> products) {
    }

    @Transactional(readOnly = true)
    public Retrieved retrieve(String question) {
        Set<KnowledgeType> enabled = EnumSet.noneOf(KnowledgeType.class);
        sources.findByEnabledTrue().forEach(s -> enabled.add(s.getType()));

        StringBuilder ctx = new StringBuilder();
        List<Product> matched = new ArrayList<>();
        String q = SlugUtil.unaccent(question);

        if (enabled.contains(KnowledgeType.CATALOG)) {
            List<String> keywords = java.util.Arrays.stream(q.split("[^a-z0-9]+"))
                    .filter(w -> w.length() >= 3 && !STOP.contains(w)).toList();
            for (String k : keywords) {
                for (Product p : products.findByStatusInAndNameUnaccentContaining(
                        EnumSet.of(ProductStatus.ACTIVE, ProductStatus.OUT_OF_STOCK), k, PageRequest.of(0, 3))) {
                    if (matched.stream().noneMatch(m -> m.getId().equals(p.getId())) && matched.size() < 3) {
                        matched.add(p);
                    }
                }
            }
            if (matched.isEmpty() && (q.contains("goi y") || q.contains("tu van") || q.contains("nen mua"))) {
                matched.addAll(products.findByStatusIn(EnumSet.of(ProductStatus.ACTIVE), PageRequest.of(0, 3,
                        org.springframework.data.domain.Sort.by("soldCount").descending())));
            }
            matched.forEach(p -> ctx.append("- ").append(p.getName()).append(": ")
                    .append(String.format("%,d", p.getMinPrice()).replace(',', '.')).append("₫")
                    .append(p.getTotalStock() > 0 ? " (còn hàng)" : " (hết hàng)").append('\n'));
        }

        for (KnowledgeType t : List.of(KnowledgeType.POLICY, KnowledgeType.FAQ)) {
            if (!enabled.contains(t)) {
                continue;
            }
            for (KnowledgeSource s : sources.findByTypeAndEnabledTrue(t)) {
                if (relevant(q, SlugUtil.unaccent(s.getName() + " " + s.getContent()))) {
                    ctx.append("- ").append(s.getContent()).append('\n');
                }
            }
        }
        return new Retrieved(ctx.toString().trim(), matched);
    }

    private boolean relevant(String question, String text) {
        return java.util.Arrays.stream(question.split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 4 && !STOP.contains(w)).anyMatch(text::contains);

    }
}
