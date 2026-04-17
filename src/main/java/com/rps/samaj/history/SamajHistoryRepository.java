package com.rps.samaj.history;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface SamajHistoryRepository extends JpaRepository<SamajHistoryEntry, Long> {

    @EntityGraph(attributePaths = "createdBy")
    @Query("""
            select h from SamajHistoryEntry h
            where (:type is null or :type = '' or lower(h.type) = lower(:type))
            and (:fromDate is null or h.date >= :fromDate)
            and (:toDate is null or h.date <= :toDate)
            and (:q is null or :q = '' or lower(h.title) like lower(concat('%', :q, '%'))
                or lower(h.location) like lower(concat('%', :q, '%'))
                or lower(h.description) like lower(concat('%', :q, '%')))
            order by h.date desc, h.createdAt desc
            """)
    Page<SamajHistoryEntry> pageForAdmin(
            @Param("type") String type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("q") String q,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "createdBy")
    @Query("select h from SamajHistoryEntry h where h.id = :id")
    Optional<SamajHistoryEntry> findDetailedById(@Param("id") Long id);
}

