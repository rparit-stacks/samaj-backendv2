package com.rps.samaj.user.repository;

import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserRole;
import com.rps.samaj.user.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByGoogleId(String googleId);

    @Query("select u.id from User u where u.status = :status")
    Page<UUID> findIdsByStatus(@Param("status") UserStatus status, Pageable pageable);

    long countByStatus(UserStatus status);

    Page<User> findByRole(UserRole role, Pageable pageable);

    boolean existsByParentAdminIsTrueAndStatus(UserStatus status);

    @Query("""
            select u from User u
            where (:q is null or :q = ''
                or (u.email is not null and lower(u.email) like lower(concat('%', :q, '%')))
                or (u.phone is not null and lower(u.phone) like lower(concat('%', :q, '%')))
                or exists (select 1 from UserProfile p where p.id = u.id and (
                    lower(coalesce(p.fullName, '')) like lower(concat('%', :q, '%'))
                    or lower(coalesce(p.profileKey, '')) like lower(concat('%', :q, '%'))
                ))
            )
            and (:role is null or u.role = :role)
            and (:status is null or u.status = :status)
            order by u.createdAt desc
            """)
    Page<User> searchForAdmin(
            @Param("q") String q,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
