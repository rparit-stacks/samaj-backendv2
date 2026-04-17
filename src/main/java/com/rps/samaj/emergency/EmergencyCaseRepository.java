package com.rps.samaj.emergency;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyCaseRepository extends JpaRepository<EmergencyCase, Long> {

    @EntityGraph(attributePaths = "creator")
    List<EmergencyCase> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "creator")
    List<EmergencyCase> findByCreator_IdOrderByCreatedAtDesc(UUID creatorId);

    @EntityGraph(attributePaths = "creator")
    @Query("select e from EmergencyCase e where e.id = :id")
    Optional<EmergencyCase> findDetailedById(@Param("id") Long id);
}
