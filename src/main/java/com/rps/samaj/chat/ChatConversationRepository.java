package com.rps.samaj.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    @Query("""
            select c from ChatConversation c
            where c.id in (select p.conversation.id from ChatParticipant p where p.user.id = :userId)
            order by c.lastMessageAt desc nulls last, c.createdAt desc
            """)
    List<ChatConversation> findAllByParticipant(@Param("userId") UUID userId);

    @Query("""
            select c from ChatConversation c
            where c.type = 'DIRECT'
              and c.id in (select p1.conversation.id from ChatParticipant p1 where p1.user.id = :userA)
              and c.id in (select p2.conversation.id from ChatParticipant p2 where p2.user.id = :userB)
            """)
    Optional<ChatConversation> findDirectBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
