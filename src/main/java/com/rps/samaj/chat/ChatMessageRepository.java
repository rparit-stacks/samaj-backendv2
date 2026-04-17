package com.rps.samaj.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByConversation_IdAndDeletedFalseOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
}
