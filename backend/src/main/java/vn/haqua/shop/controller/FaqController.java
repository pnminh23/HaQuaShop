package vn.haqua.shop.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.haqua.shop.enums.ChatEnums.KnowledgeType;
import vn.haqua.shop.repository.KnowledgeSourceRepository;

/** Câu hỏi thường gặp: dùng chung nguồn kiến thức với chatbot (name = câu hỏi, content = trả lời). */
@RestController
@RequiredArgsConstructor
public class FaqController {

    record FaqDto(String question, String answer) {
    }

    private final KnowledgeSourceRepository sources;

    @GetMapping("/api/faq")
    List<FaqDto> faq() {
        return sources.findByTypeAndEnabledTrue(KnowledgeType.FAQ).stream()
                .map(s -> new FaqDto(s.getName(), s.getContent())).toList();

    }
}
