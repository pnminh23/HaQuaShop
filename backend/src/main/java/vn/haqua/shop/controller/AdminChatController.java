package vn.haqua.shop.controller;

import java.util.List;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import vn.haqua.shop.dto.ChatDtos.*;
import vn.haqua.shop.repository.OrderRepository;
import vn.haqua.shop.security.CurrentUser;
import vn.haqua.shop.service.ChatbotService;

@RestController
@RequestMapping("/api/admin/chatbot")
@RequiredArgsConstructor
public class AdminChatController {

    private final ChatbotService service;
    private final OrderRepository orders;

    @GetMapping("/stats")
    ChatStats stats() {
        var start = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        return service.stats(orders.countChatbotSince(start));
    }

    @GetMapping("/conversations")
    List<ConversationRow> list() {
        return service.adminList();
    }

    @GetMapping("/conversations/{id}")
    ConversationDetail get(@PathVariable Long id) {
        return service.adminGet(id);
    }

    @PostMapping("/conversations/{id}/takeover")
    ConversationDetail takeover(@PathVariable Long id) {
        return service.takeover(id, CurrentUser.requireId());
    }

    @PostMapping("/conversations/{id}/messages")
    ConversationDetail reply(@PathVariable Long id, @Valid @RequestBody StaffMessageRequest req) {
        return service.staffReply(id, CurrentUser.requireId(), req.content());
    }

    @PostMapping("/conversations/{id}/give-back")
    ConversationDetail giveBack(@PathVariable Long id) {
        return service.giveBackToAi(id);
    }

    @PostMapping("/conversations/{id}/close")
    ConversationDetail close(@PathVariable Long id) {
        return service.close(id);
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    ConfigDto config() {
        return service.getConfig();
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    ConfigDto saveConfig(@Valid @RequestBody ConfigDto req) {
        return service.saveConfig(req);

    }
}
