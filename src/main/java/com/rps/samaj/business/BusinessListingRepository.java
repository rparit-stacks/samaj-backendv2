package com.rps.samaj.business;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BusinessListingRepository extends JpaRepository<BusinessListing, UUID> {

    Page<BusinessListing> findByStatusOrderByFeaturedDescCreatedAtDesc(BusinessStatus status, Pageable pageable);

    Page<BusinessListing> findByStatusAndCategoryIgnoreCaseOrderByFeaturedDescCreatedAtDesc(
            BusinessStatus status, String category, Pageable pageable);

    Page<BusinessListing> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<BusinessListing> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<BusinessListing> findByStatusOrderByCreatedAtDesc(BusinessStatus status, Pageable pageable);
}
