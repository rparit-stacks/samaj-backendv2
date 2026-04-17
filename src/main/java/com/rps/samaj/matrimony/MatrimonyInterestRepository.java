package com.rps.samaj.matrimony;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface MatrimonyInterestRepository extends JpaRepository<MatrimonyInterest, UUID> {

    Optional<MatrimonyInterest> findByFromProfile_IdAndToProfile_Id(UUID fromProfileId, UUID toProfileId);

    @Query("select i from MatrimonyInterest i where i.fromProfile.owner.id = :uid order by i.createdAt desc")
    Page<MatrimonyInterest> findSentByUser(@Param("uid") UUID userId, Pageable pageable);

    @Query("select i from MatrimonyInterest i where i.toProfile.owner.id = :uid order by i.createdAt desc")
    Page<MatrimonyInterest> findReceivedByUser(@Param("uid") UUID userId, Pageable pageable);

    Optional<MatrimonyInterest> findByFromProfile_IdAndToProfile_IdAndStatus(UUID fromProfileId, UUID toProfileId, String status);

    @Query("""
            select case when count(i) > 0 then true else false end
            from MatrimonyInterest i
            where ((i.fromProfile.id = :a and i.toProfile.id = :b) or (i.fromProfile.id = :b and i.toProfile.id = :a))
              and i.status in :statuses
            """)
    boolean existsBetweenProfilesWithStatusIn(@Param("a") UUID a, @Param("b") UUID b, @Param("statuses") Collection<String> statuses);

    @Query("select count(i) from MatrimonyInterest i where i.fromProfile.owner.id = :uid")
    long countSentByUser(@Param("uid") UUID userId);

    @Query("select count(i) from MatrimonyInterest i where i.toProfile.owner.id = :uid")
    long countReceivedByUser(@Param("uid") UUID userId);

    @Query("select count(i) from MatrimonyInterest i where (i.fromProfile.owner.id = :uid or i.toProfile.owner.id = :uid) and i.status = 'ACCEPTED'")
    long countAcceptedForUser(@Param("uid") UUID userId);
}
