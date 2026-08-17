package com.rps.samaj.user.repository;

import com.rps.samaj.user.model.ContactRequest;
import com.rps.samaj.user.model.ContactRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, UUID> {

    List<ContactRequest> findByTarget_IdOrderByCreatedAtDesc(UUID targetId);

    List<ContactRequest> findByRequester_IdOrderByCreatedAtDesc(UUID requesterId);

    /** Used to collapse duplicate sends while an earlier request is still pending. */
    Optional<ContactRequest> findByRequester_IdAndTarget_IdAndStatus(
            UUID requesterId, UUID targetId, ContactRequestStatus status);
}
