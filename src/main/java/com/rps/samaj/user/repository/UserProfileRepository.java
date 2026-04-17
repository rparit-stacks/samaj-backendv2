package com.rps.samaj.user.repository;

import com.rps.samaj.user.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUser_Id(UUID userId);

    List<UserProfile> findByIdIn(Collection<UUID> ids);

    Optional<UserProfile> findByProfileKeyIgnoreCase(String profileKey);

    @Query("""
            select p from UserProfile p
            join p.user u
            where u.status = 'ACTIVE'
            and (
              :q is null or trim(:q) = ''
              or lower(coalesce(p.fullName, '')) like lower(concat('%', :q, '%'))
              or lower(p.profileKey) like lower(concat('%', :q, '%'))
              or lower(coalesce(u.email, '')) like lower(concat('%', :q, '%'))
              or coalesce(u.phone, '') like concat('%', :q, '%')
            )
            order by p.fullName asc nulls last
            """)
    Page<UserProfile> searchActive(@Param("q") String q, Pageable pageable);

    @Query("""
            select p from UserProfile p
            join p.user u
            join UserSettings s on s.id = u.id
            where u.status = 'ACTIVE' and s.showInDirectory = true
            order by p.fullName asc nulls last
            """)
    Page<UserProfile> directoryMembers(Pageable pageable);
}
