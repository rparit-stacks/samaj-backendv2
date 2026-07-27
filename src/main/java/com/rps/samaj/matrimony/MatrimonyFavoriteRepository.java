package com.rps.samaj.matrimony;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MatrimonyFavoriteRepository extends JpaRepository<MatrimonyFavorite, UUID> {

    Optional<MatrimonyFavorite> findByUser_IdAndProfile_Id(UUID userId, UUID profileId);

    @EntityGraph(attributePaths = {"profile", "profile.owner"})
    List<MatrimonyFavorite> findByUser_Id(UUID userId);

    long countByUser_Id(UUID userId);

    @Query("""
            select f.profile.id from MatrimonyFavorite f
            where f.user.id = :userId and f.profile.id in :profileIds
            """)
    Set<UUID> findFavoritedProfileIds(
            @Param("userId") UUID userId,
            @Param("profileIds") Collection<UUID> profileIds
    );
}
