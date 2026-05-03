package com.rps.samaj.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobListingRepository extends JpaRepository<JobListing, UUID> {

    Page<JobListing> findByStatusOrderByFeaturedDescCreatedAtDesc(JobStatus status, Pageable pageable);

    Page<JobListing> findByStatusAndCategoryIgnoreCaseOrderByFeaturedDescCreatedAtDesc(
            JobStatus status, String category, Pageable pageable);

    Page<JobListing> findByStatusAndJobTypeIgnoreCaseOrderByFeaturedDescCreatedAtDesc(
            JobStatus status, String jobType, Pageable pageable);

    Page<JobListing> findByStatusAndCategoryIgnoreCaseAndJobTypeIgnoreCaseOrderByFeaturedDescCreatedAtDesc(
            JobStatus status, String category, String jobType, Pageable pageable);

    Page<JobListing> findByPostedBy_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<JobListing> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<JobListing> findByStatusOrderByCreatedAtDesc(JobStatus status, Pageable pageable);
}
