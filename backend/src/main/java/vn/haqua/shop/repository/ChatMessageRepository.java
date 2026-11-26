package vn.haqua.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.haqua.shop.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByIdAsc(Long conversationId);

}
