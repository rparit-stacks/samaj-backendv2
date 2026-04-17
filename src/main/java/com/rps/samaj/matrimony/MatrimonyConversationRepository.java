package com.rps.samaj.matrimony;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatrimonyConversationRepository extends JpaRepository<MatrimonyConversation, UUID> {

    Optional<MatrimonyConversation> findByProfileIdLowerAndProfileIdHigher(UUID profileIdLower, UUID profileIdHigher);

    @Query("""
            select distinct c from MatrimonyConversation c
            where c.profileIdLower in :ids or c.profileIdHigher in :ids
            order by c.createdAt desc
            """)
    List<MatrimonyConversation> findForProfileIds(@Param("ids") Collection<UUID> profileIds);
}
