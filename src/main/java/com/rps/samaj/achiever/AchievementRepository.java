package com.rps.samaj.achiever;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    Page<Achievement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Achievement> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Achievement> findByStatusOrderByCreatedAtDesc(AchievementStatus status, Pageable pageable);

    @Query("""
            SELECT a FROM Achievement a
            WHERE a.status = 'APPROVED'
              AND a.marqueeEnabled = true
              AND a.marqueeStart IS NOT NULL
              AND a.marqueeEnd IS NOT NULL
              AND a.marqueeStart <= :now
              AND a.marqueeEnd >= :now
            ORDER BY a.marqueeEnd ASC, a.createdAt DESC
            """)
    List<Achievement> findForMarquee(@Param("now") Instant now, Pageable pageable);
}
