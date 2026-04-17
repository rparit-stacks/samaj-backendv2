package com.rps.samaj.community;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    @EntityGraph(attributePaths = "author")
    Page<CommunityComment> findByPost_IdOrderByCreatedAtAsc(long postId, Pageable pageable);

    void deleteByPost_Id(long postId);

    @EntityGraph(attributePaths = {"author", "post"})
    @Query("select c from CommunityComment c where c.id = :id")
    java.util.Optional<CommunityComment> findDetailedById(@Param("id") Long id);
}
