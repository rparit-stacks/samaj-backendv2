package com.rps.samaj.user.repository;

import com.rps.samaj.user.model.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, UUID> {

    List<FamilyMember> findByOwner_IdOrderByNameAsc(UUID ownerId);

    Optional<FamilyMember> findByIdAndOwner_Id(UUID id, UUID ownerId);
}
