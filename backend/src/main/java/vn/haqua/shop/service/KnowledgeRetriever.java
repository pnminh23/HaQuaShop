package vn.haqua.shop.service;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.haqua.shop.entity.KnowledgeSource;
import vn.haqua.shop.entity.Product;
import vn.haqua.shop.enums.ChatEnums.KnowledgeType;
import vn.haqua.shop.enums.ProductStatus;
import vn.haqua.shop.repository.KnowledgeSourceRepository;
import vn.haqua.shop.repository.ProductRepository;
import vn.haqua.shop.service.chat.ProductAdvisor;
import vn.haqua.shop.service.chat.ProductAdvisor.Candidate;
import vn.haqua.shop.service.chat.ProductContext;

/** Lấy ngữ cảnh từ các nguồn kiến thức đang bật: sản phẩm (giá, tồn kho thật), chính sách, FAQ. */
@Component
@RequiredArgsConstructor
public class KnowledgeRetriever {

    private final ProductRepository products;
    private final KnowledgeSourceRepository sources;

    /** Số thẻ sản phẩm hiện dưới câu trả lời; khớp với lời dặn AI giới thiệu tối đa 3 máy theo thứ tự. */
    private static final int CARDS = 3;

    public record Retrieved(String context, List<Product> products) {
    }

    /** @param userMessages các câu khách đã hỏi, cũ trước mới sau; câu cuối là câu đang cần trả lời */
    @Transactional(readOnly = true)
    public Retrieved retrieve(List<String> userMessages) {
        Set<KnowledgeType> enabled = EnumSet.noneOf(KnowledgeType.class);
        sources.findByEnabledTrue().forEach(s -> enabled.add(s.getType()));

        StringBuilder ctx = new StringBuilder();
        List<Product> cards = List.of();

        if (enabled.contains(KnowledgeType.CATALOG)) {
            Map<Long, Product> byId = products.findByStatusIn(
                            EnumSet.of(ProductStatus.ACTIVE, ProductStatus.OUT_OF_STOCK), Pageable.unpaged())
                    .stream().collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
            List<Candidate> catalog = byId.values().stream().map(KnowledgeRetriever::candidate).toList();
            ProductAdvisor.Advice advice = ProductAdvisor.advise(userMessages, catalog);
            String text = ProductContext.describe(advice, catalog);
            if (!text.isEmpty()) {
                ctx.append(text).append('\n');
            }
            if (advice.productQuestion()) {
                cards = advice.items().stream().limit(CARDS).map(c -> byId.get(c.id())).toList();
            }
        }

        // chính sách và FAQ chỉ vài dòng: đưa hết cho trợ lý, lọc theo từ khóa dễ sót ("trả góp" toàn từ ngắn)
        for (KnowledgeType t : List.of(KnowledgeType.POLICY, KnowledgeType.FAQ)) {
            if (enabled.contains(t)) {
                sources.findByTypeAndEnabledTrue(t).forEach(k -> ctx.append("- ").append(k.getContent()).append('\n'));
            }
        }

        return new Retrieved(ctx.toString().trim(), cards);
    }

    private static Candidate candidate(Product p) {
        Map<String, String> specs = new LinkedHashMap<>();
        p.getSpecs().forEach(s -> {
            if (s.getSpecKey() != null && s.getSpecValue() != null && !s.getSpecValue().isBlank()) {
                specs.merge(s.getSpecKey(), s.getSpecValue(), (a, b) -> a + ", " + b);
            }
        });
        return new Candidate(p.getId(), p.getName(), p.getBrand(), p.getCategory().getSlug(), p.getMinPrice(),
                p.getTotalStock() > 0, p.getSoldCount(), specs);
    }
}
