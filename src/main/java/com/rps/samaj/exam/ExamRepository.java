package com.rps.samaj.exam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    @Query("""
            select e from Exam e
            where (:q is null or :q = '' or lower(e.title) like lower(concat('%', :q, '%'))
                or lower(e.description) like lower(concat('%', :q, '%')))
            and (:type is null or :type = '' or lower(e.type) = lower(:type))
            and (:filter is null or :filter = ''
                or (:filter = 'new' and e.expired = false and (e.lastDate is null or e.lastDate >= CURRENT_DATE))
                or (:filter = 'old' and (e.expired = true or (e.lastDate is not null and e.lastDate < CURRENT_DATE))))
            order by e.createdAt desc
            """)
    Page<Exam> searchPublished(
            @Param("q") String q,
            @Param("type") String type,
            @Param("filter") String filter,
            Pageable pageable
    );
}
