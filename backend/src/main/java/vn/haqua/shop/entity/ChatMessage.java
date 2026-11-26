package vn.haqua.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;

import vn.haqua.shop.enums.ChatEnums.Sender;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ChatConversation conversation;

    @Enumerated(EnumType.STRING)
    private Sender sender;

    @Column(length = 4000)
    private String content;

    /** Danh sách id sản phẩm được gợi ý, cách nhau bằng dấu phẩy. */
    private String productRefs;

    public ChatMessage(ChatConversation conversation, Sender sender, String content, String productRefs) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
        this.productRefs = productRefs;

    }
}
