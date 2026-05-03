package com.rps.samaj.donation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DonationRepository extends JpaRepository<DonationEntity, Long> {

    Page<DonationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<DonationEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Optional<DonationEntity> findByRazorpayOrderId(String razorpayOrderId);

    @Query("SELECT COUNT(DISTINCT d.userId) FROM DonationEntity d WHERE d.status = 'SUCCESS'")
    long countDistinctDonors();

    @Query("SELECT COALESCE(SUM(d.amountPaise), 0) FROM DonationEntity d WHERE d.status = 'SUCCESS'")
    long sumSuccessAmountPaise();

    @Query("SELECT COALESCE(SUM(d.amountPaise), 0) FROM DonationEntity d WHERE d.status = 'SUCCESS' AND d.createdAt >= :since")
    long sumSuccessAmountPaiseSince(@Param("since") Instant since);

    long countByStatus(String status);
}
