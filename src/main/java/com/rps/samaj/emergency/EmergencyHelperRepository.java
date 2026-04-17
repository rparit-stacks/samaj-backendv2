package com.rps.samaj.emergency;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EmergencyHelperRepository extends JpaRepository<EmergencyHelper, UUID> {

    @EntityGraph(attributePaths = "helper")
    List<EmergencyHelper> findByEmergency_IdOrderByHelpedAtDesc(Long emergencyId);

    long countByHelper_Id(UUID helperId);

    @Query("select h from EmergencyHelper h join fetch h.emergency e join fetch e.creator where h.helper.id = :helperId")
    List<EmergencyHelper> findByHelper_IdWithEmergency(@Param("helperId") UUID helperId);
}
