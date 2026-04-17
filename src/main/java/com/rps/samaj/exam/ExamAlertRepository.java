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

public interface ExamAlertRepository extends JpaRepository<ExamAlert, UUID> {

    @EntityGraph(attributePaths = "exam")
    Page<ExamAlert> findByUser_IdOrderByExam_CreatedAtDesc(UUID userId, Pageable pageable);

    Optional<ExamAlert> findByUser_IdAndExam_Id(UUID userId, UUID examId);

    void deleteByUser_IdAndExam_Id(UUID userId, UUID examId);

    void deleteByExam_Id(UUID examId);

    @Query("select ea.exam.id from ExamAlert ea where ea.user.id = :uid and ea.exam.id in :ids")
    List<UUID> findExamIdsWithAlert(@Param("uid") UUID userId, @Param("ids") Collection<UUID> examIds);
}
