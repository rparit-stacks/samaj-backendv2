package com.rps.samaj.community;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostReportRepository extends JpaRepository<CommunityPostReport, Long> {

    void deleteByPost_Id(long postId);

    @EntityGraph(attributePaths = {"post", "reporter", "post.author"})
    @Query("""
            select r from CommunityPostReport r
            where (:status is null or r.status = :status)
            order by r.createdAt desc
            """)
    Page<CommunityPostReport> pageAdmin(@Param("status") CommunityReportStatus status, Pageable pageable);
}

