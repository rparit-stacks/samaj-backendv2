package com.rps.samaj.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AppNotificationRepository extends JpaRepository<AppNotification, UUID> {

    Page<AppNotification> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AppNotification> findByUser_IdAndReadIsFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUser_IdAndReadIsFalse(UUID userId);

    @Modifying
    @Query("update AppNotification n set n.read = true where n.user.id = :uid and n.read = false")
    int markAllReadForUser(@Param("uid") UUID userId);

    @Modifying
    @Query("delete from AppNotification n where n.user.id = :uid")
    int deleteAllForUser(@Param("uid") UUID userId);

    @Query("select n from AppNotification n where n.id = :id and n.user.id = :uid")
    Optional<AppNotification> findByIdAndUser_Id(@Param("id") UUID id, @Param("uid") UUID userId);
}
