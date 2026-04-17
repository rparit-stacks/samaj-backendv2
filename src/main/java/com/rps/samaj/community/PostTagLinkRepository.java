package com.rps.samaj.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PostTagLinkRepository extends JpaRepository<PostTagLink, UUID> {

    void deleteByPost_Id(long postId);

    @Query("select l from PostTagLink l join fetch l.tag where l.post.id in :ids")
    List<PostTagLink> findByPost_IdIn(@Param("ids") Collection<Long> ids);
}
