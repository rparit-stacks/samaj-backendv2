package com.rps.samaj.user.repository;

import com.rps.samaj.user.model.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findTopByIdentifierAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
            String identifier,
            String purpose
    );

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM OtpChallenge o WHERE o.identifier = :identifier AND o.purpose = :purpose")
    void deleteByIdentifierAndPurpose(@Param("identifier") String identifier, @Param("purpose") String purpose);
}
