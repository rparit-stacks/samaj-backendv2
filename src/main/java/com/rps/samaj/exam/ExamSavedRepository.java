package com.rps.samaj.exam;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamSavedRepository extends JpaRepository<ExamSaved, UUID> {

    @EntityGraph(attributePaths = "exam")
    Page<ExamSaved> findByUser_IdOrderByExam_CreatedAtDesc(UUID userId, Pageable pageable);

    Optional<ExamSaved> findByUser_IdAndExam_Id(UUID userId, UUID examId);

    void deleteByUser_IdAndExam_Id(UUID userId, UUID examId);

    void deleteByExam_Id(UUID examId);

    @Query("select es.exam.id from ExamSaved es where es.user.id = :uid and es.exam.id in :ids")
    List<UUID> findExamIdsSaved(@Param("uid") UUID userId, @Param("ids") Collection<UUID> examIds);
}
