package vn.haqua.shop.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import vn.haqua.shop.entity.ChatConversation;
import vn.haqua.shop.enums.ChatEnums.ConversationStatus;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findBySessionToken(String sessionToken);

    List<ChatConversation> findAllByOrderByLastMessageAtDesc(org.springframework.data.domain.Pageable pageable);

    @Query("select count(c) from ChatConversation c where c.createdAt >= :from")
    long countSince(Instant from);

    @Query("select count(c) from ChatConversation c where c.createdAt >= :from and c.status in (:statuses)")
    long countSinceWithStatus(Instant from, java.util.Collection<ConversationStatus> statuses);

    @Query("select avg(c.csat) from ChatConversation c where c.csat is not null")
    Double averageCsat();

}
