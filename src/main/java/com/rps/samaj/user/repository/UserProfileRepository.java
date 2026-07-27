package com.rps.samaj.user.repository;

import com.rps.samaj.directory.DirectoryListRow;
import com.rps.samaj.user.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = "user")
    @Query("""
            select p from UserProfile p
            join p.user u
            join UserSettings s on s.id = u.id
            where u.status = 'ACTIVE' and s.showInDirectory = true
            order by p.fullName asc nulls last
            """)
    Page<UserProfile> directoryMembers(Pageable pageable);

    /**
     * Single-query directory list with settings + optional directory actions (no N+1).
     */
    @Query("""
            select p.id as id,
                   p.fullName as fullName,
                   p.avatarUrl as avatarUrl,
                   p.city as city,
                   u.phone as phone,
                   u.email as email,
                   s.showPhone as showPhone,
                   ds.actionsJson as actionsJson
            from UserProfile p
            join p.user u
            join UserSettings s on s.id = u.id
            left join DirectorySettings ds on ds.id = u.id
            where u.status = 'ACTIVE'
              and s.showInDirectory = true
              and (ds is null or ds.visible = true)
            order by p.fullName asc nulls last
            """)
    List<DirectoryListRow> findDirectoryListRows(Pageable pageable);
}
