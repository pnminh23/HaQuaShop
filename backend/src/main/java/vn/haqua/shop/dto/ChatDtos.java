package vn.haqua.shop.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import vn.haqua.shop.dto.CatalogDtos.ProductCardDto;
import vn.haqua.shop.enums.ChatEnums.*;

public final class ChatDtos {

    private ChatDtos() {
    }

    public record SendRequest(String sessionToken, @NotBlank @Size(max = 1000) String content) {
    }

    public record MessageDto(Long id, Sender sender, String content, List<ProductCardDto> products, Instant createdAt) {
    }

    public record ChatState(String sessionToken, ConversationStatus status, boolean botEnabled, String botName,
                            List<MessageDto> messages) {
    }

    public record CsatRequest(@Min(1) @Max(5) int score) {
    }

    // ---- admin ----
    public record ConversationRow(Long id, String customer, ConversationStatus status, String tag,
                                  String lastMessage, Instant lastMessageAt, String assignedStaff) {
    }

    public record ChatStats(long conversationsToday, int aiHandledPercent, long ordersFromChatbot, double csat) {
    }

    public record ConversationDetail(Long id, ConversationStatus status, String tag, String assignedStaff,
                                     List<MessageDto> messages) {
    }

    public record StaffMessageRequest(@NotBlank String content) {
    }

    public record ConfigDto(@NotBlank @Size(max = 60) String displayName, Tone tone, HandoffRule handoffRule,
                            @Size(max = 2000, message = "Hướng dẫn tối đa 2.000 ký tự") String extraInstructions,
                            boolean enabled, List<KnowledgeDto> sources) {
    }

    public record KnowledgeDto(Long id, KnowledgeType type, String name, boolean enabled) {
    }
}
