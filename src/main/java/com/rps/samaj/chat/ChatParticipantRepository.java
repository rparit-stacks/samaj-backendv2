package com.rps.samaj.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {

    List<ChatParticipant> findByConversation_Id(UUID conversationId);

    Optional<ChatParticipant> findByConversation_IdAndUser_Id(UUID conversationId, UUID userId);

    boolean existsByConversation_IdAndUser_Id(UUID conversationId, UUID userId);

    @Query("select count(m) from ChatMessage m where m.conversation.id = :convId and m.createdAt > :since and m.sender.id <> :userId and m.deleted = false")
    long countUnreadMessages(@Param("convId") UUID convId, @Param("since") java.time.Instant since, @Param("userId") UUID userId);
}
