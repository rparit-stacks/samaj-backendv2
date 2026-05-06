package com.rps.samaj.admin.system;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminInvitationRepository extends JpaRepository<AdminInvitation, UUID> {

    Optional<AdminInvitation> findByToken(String token);

    Optional<AdminInvitation> findByEmailIgnoreCaseAndAcceptedFalse(String email);

    List<AdminInvitation> findByAcceptedFalseAndExpiresAtAfterOrderByCreatedAtDesc(Instant now);

    void deleteByExpiresAtBefore(Instant cutoff);
}
