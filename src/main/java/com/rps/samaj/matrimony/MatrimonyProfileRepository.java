package com.rps.samaj.matrimony;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatrimonyProfileRepository extends JpaRepository<MatrimonyProfileEntity, UUID> {

    @EntityGraph(attributePaths = "owner")
    List<MatrimonyProfileEntity> findByOwner_IdOrderByUpdatedAtDesc(UUID ownerUserId);

    @EntityGraph(attributePaths = "owner")
    Optional<MatrimonyProfileEntity> findByIdAndOwner_Id(UUID id, UUID ownerUserId);

    long countByStatus(String status);

    long countByVerified(boolean verified);

    long countByVisibleInSearch(boolean visible);

}