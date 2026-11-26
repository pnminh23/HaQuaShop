package vn.haqua.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import vn.haqua.shop.entity.KnowledgeSource;
import vn.haqua.shop.enums.ChatEnums.KnowledgeType;
import vn.haqua.shop.repository.KnowledgeSourceRepository;
import vn.haqua.shop.repository.ProductRepository;

class KnowledgeRetrieverTest {

    private final KnowledgeSourceRepository sources = mock(KnowledgeSourceRepository.class);
    private final KnowledgeRetriever retriever = new KnowledgeRetriever(mock(ProductRepository.class), sources);

    private static KnowledgeSource source(KnowledgeType type, String content) {
        KnowledgeSource s = mock(KnowledgeSource.class);
        when(s.getType()).thenReturn(type);
        when(s.getName()).thenReturn("nguon");
        when(s.getContent()).thenReturn(content);
        return s;
    }

    @Test
    void installmentQuestionGetsInstallmentPolicy() {
        KnowledgeSource faq = source(KnowledgeType.FAQ, "Chọn Trả góp 0% khi thanh toán.");
        when(sources.findByEnabledTrue()).thenReturn(List.of(faq));
        when(sources.findByTypeAndEnabledTrue(KnowledgeType.FAQ)).thenReturn(List.of(faq));

        assertThat(retriever.retrieve(List.of("shop có trả góp không")).context()).contains("Trả góp 0%");
    }
}
