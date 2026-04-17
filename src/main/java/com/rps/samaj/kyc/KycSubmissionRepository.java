package com.rps.samaj.kyc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycSubmissionRepository extends JpaRepository<KycSubmission, UUID> {

    List<KycSubmission> findByStatusOrderBySubmittedAtAsc(KycSubmissionStatus status);

    Optional<KycSubmission> findFirstByUser_IdOrderBySubmittedAtDesc(UUID userId);
}
