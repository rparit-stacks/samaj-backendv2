package com.rps.samaj.user.repository;

import com.rps.samaj.user.model.ContactRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, UUID> {

    List<ContactRequest> findByTarget_IdOrderByCreatedAtDesc(UUID targetId);

    List<ContactRequest> findByRequester_IdOrderByCreatedAtDesc(UUID requesterId);
}
