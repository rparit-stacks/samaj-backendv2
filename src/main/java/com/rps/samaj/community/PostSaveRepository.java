package com.rps.samaj.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostSaveRepository extends JpaRepository<PostSave, UUID> {

    boolean existsByPost_IdAndUser_Id(long postId, UUID userId);

    Optional<PostSave> findByPost_IdAndUser_Id(long postId, UUID userId);

    void deleteByPost_IdAndUser_Id(long postId, UUID userId);

    void deleteByPost_Id(long postId);

    @Query("select ps.post.id from PostSave ps where ps.user.id = :uid and ps.post.id in :ids")
    List<Long> findSavedPostIds(@Param("uid") UUID uid, @Param("ids") Collection<Long> ids);

    @Query("select count(ps) from PostSave ps where ps.post.id = :postId")
    long countByPost_Id(@Param("postId") long postId);
}
