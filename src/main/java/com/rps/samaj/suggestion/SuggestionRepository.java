package com.rps.samaj.suggestion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SuggestionRepository extends JpaRepository<Suggestion, UUID> {

    @Query("""
            select s from Suggestion s
            where s.user.id = :uid
            and (:q is null or :q = '' or lower(s.title) like lower(concat('%', :q, '%'))
                or lower(s.description) like lower(concat('%', :q, '%')))
            and (:status is null or :status = '' or s.status = :status)
            order by s.createdAt desc
            """)
    Page<Suggestion> pageMine(
            @Param("uid") UUID userId,
            @Param("q") String q,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("select s from Suggestion s where s.id = :id and s.user.id = :uid")
    Optional<Suggestion> findByIdAndUser_Id(@Param("id") UUID id, @Param("uid") UUID userId);
}
