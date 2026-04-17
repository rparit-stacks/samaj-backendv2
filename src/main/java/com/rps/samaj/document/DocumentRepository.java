package com.rps.samaj.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<AppDocument, UUID> {

    @Query("""
            select d from AppDocument d join fetch d.createdBy
            where (:q is null or :q = '' or (
                lower(d.title) like lower(concat('%', :q, '%'))
                or lower(coalesce(d.description,'')) like lower(concat('%', :q, '%'))
                or lower(d.fileName) like lower(concat('%', :q, '%'))
            ))
            and (:cat is null or :cat = '' or lower(d.category) = lower(:cat))
            and (:approved is null or d.approved = :approved)
            order by d.createdAt desc
            """)
    List<AppDocument> findForAdmin(
            @Param("q") String q,
            @Param("cat") String cat,
            @Param("approved") Boolean approved
    );

    @Query("""
            select d from AppDocument d
            where d.approved = true and d.visibility = 'PUBLIC'
            and (:cat is null or :cat = '' or lower(d.category) = lower(:cat))
            and (:q is null or :q = '' or lower(d.title) like lower(concat('%', :q, '%')))
            order by d.createdAt desc
            """)
    List<AppDocument> findPublishedFiltered(@Param("cat") String category, @Param("q") String search);

    List<AppDocument> findByCreatedBy_IdOrderByCreatedAtDesc(UUID userId);

    List<AppDocument> findByApprovedIsFalseOrderByCreatedAtAsc();

    @Query("select d from AppDocument d join fetch d.createdBy where d.id = :id")
    Optional<AppDocument> findDetailedById(@Param("id") UUID id);
}
