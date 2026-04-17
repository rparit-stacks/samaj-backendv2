package com.rps.samaj.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    boolean existsByPost_IdAndUser_Id(long postId, UUID userId);

    Optional<PostLike> findByPost_IdAndUser_Id(long postId, UUID userId);

    void deleteByPost_IdAndUser_Id(long postId, UUID userId);

    void deleteByPost_Id(long postId);

    long countByUser_Id(UUID userId);

    @Query("select pl.post.id from PostLike pl where pl.user.id = :uid and pl.post.id in :ids")
    List<Long> findLikedPostIds(@Param("uid") UUID uid, @Param("ids") Collection<Long> ids);
}
