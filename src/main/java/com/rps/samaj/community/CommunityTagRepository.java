package com.rps.samaj.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityTagRepository extends JpaRepository<CommunityTag, Long> {

    Optional<CommunityTag> findBySlugIgnoreCase(String slug);

    @Query(value = """
            select t.id, t.name, t.slug, count(l.post_id) as cnt
            from samaj_community_tags t
            join samaj_post_tags l on l.tag_id = t.id
            group by t.id, t.name, t.slug
            order by cnt desc
            limit :lim
            """, nativeQuery = true)
    List<Object[]> topTagsRaw(@Param("lim") int lim);
}
