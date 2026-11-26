package vn.haqua.shop.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import vn.haqua.shop.enums.ChatEnums.ConversationStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chat_conversations")
public class ChatConversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String sessionToken;

    @Enumerated(EnumType.STRING)
    private ConversationStatus status = ConversationStatus.AI;

    @ManyToOne(fetch = FetchType.LAZY)
    private User assignedStaff;

    /** Nhãn hiển thị: "Đã chốt đơn", "AI xử lý", "Cần nhân viên". */
    private String tag = "AI xử lý";

    /** Điểm hài lòng 1–5 khi đóng hội thoại. */
    private Integer csat;

    private Instant lastMessageAt = Instant.now();

}
