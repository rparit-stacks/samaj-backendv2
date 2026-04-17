package com.rps.samaj.community;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    @EntityGraph(attributePaths = {"author", "media"})
    @Query("select p from CommunityPost p order by p.createdAt desc")
    Page<CommunityPost> findAllPaged(Pageable pageable);

    @EntityGraph(attributePaths = {"author", "media"})
    @Query("""
            select distinct p from CommunityPost p
            join PostTagLink l on l.post.id = p.id
            join l.tag t
            where lower(t.slug) = lower(:slug)
            order by p.createdAt desc
            """)
    Page<CommunityPost> findByTagSlug(@Param("slug") String slug, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "media"})
    @Query("""
            select p from CommunityPost p
            join PostSave s on s.post.id = p.id
            where s.user.id = :uid
            order by p.createdAt desc
            """)
    Page<CommunityPost> findSavedByUser(@Param("uid") UUID uid, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "media"})
    @Query("select p from CommunityPost p where p.author.id = :aid order by p.createdAt desc")
    Page<CommunityPost> findByAuthorId(@Param("aid") UUID aid, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "media"})
    @Query("select p from CommunityPost p where p.id = :id")
    java.util.Optional<CommunityPost> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"author", "media"})
    @Query("""
            select distinct p from CommunityPost p
            left join PostTagLink l on l.post.id = p.id
            left join l.tag t
            where (:authorId is null or p.author.id = :authorId)
            and (:tag is null or :tag = '' or lower(t.slug) = lower(:tag))
            and (:q is null or :q = '' or lower(p.content) like lower(concat('%', :q, '%'))
                or lower(p.location) like lower(concat('%', :q, '%')))
            order by p.createdAt desc
            """)
    Page<CommunityPost> pageForAdmin(
            @Param("authorId") UUID authorId,
            @Param("tag") String tag,
            @Param("q") String q,
            Pageable pageable
    );

    long countByAuthor_Id(UUID authorId);

    @Query("select coalesce(sum(p.likeCount), 0) from CommunityPost p where p.author.id = :uid")
    long sumLikeCountByAuthor(@Param("uid") UUID uid);

    @Query("select coalesce(sum(p.viewCount), 0) from CommunityPost p where p.author.id = :uid")
    long sumViewCountByAuthor(@Param("uid") UUID uid);

    @Query("select coalesce(sum(p.saveCount), 0) from CommunityPost p where p.author.id = :uid")
    long sumSaveCountByAuthor(@Param("uid") UUID uid);
}
