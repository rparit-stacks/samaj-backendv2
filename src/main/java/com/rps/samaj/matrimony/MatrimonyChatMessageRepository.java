package com.rps.samaj.matrimony;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatrimonyChatMessageRepository extends JpaRepository<MatrimonyChatMessage, UUID> {

    Page<MatrimonyChatMessage> findByConversation_IdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
}
