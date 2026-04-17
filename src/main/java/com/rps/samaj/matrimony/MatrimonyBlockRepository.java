package com.rps.samaj.matrimony;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatrimonyBlockRepository extends JpaRepository<MatrimonyBlock, UUID> {

    Optional<MatrimonyBlock> findByOwner_IdAndBlocked_Id(UUID ownerUserId, UUID blockedUserId);

    boolean existsByOwner_IdAndBlocked_Id(UUID ownerUserId, UUID blockedUserId);

    List<MatrimonyBlock> findByOwner_Id(UUID ownerUserId);

    List<MatrimonyBlock> findByBlocked_Id(UUID blockedUserId);

    long countByOwner_Id(UUID ownerUserId);
}
