package com.rps.samaj.matrimony;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatrimonyFavoriteRepository extends JpaRepository<MatrimonyFavorite, UUID> {

    Optional<MatrimonyFavorite> findByUser_IdAndProfile_Id(UUID userId, UUID profileId);

    List<MatrimonyFavorite> findByUser_Id(UUID userId);

    long countByUser_Id(UUID userId);
}
