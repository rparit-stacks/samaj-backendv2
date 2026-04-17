package com.rps.samaj.matrimony;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface MatrimonyProfileViewRepository extends JpaRepository<MatrimonyProfileView, UUID> {

    @Query("select count(v) from MatrimonyProfileView v where v.profile.owner.id = :ownerUserId")
    long countByProfileOwnerUserId(@Param("ownerUserId") UUID ownerUserId);
}
