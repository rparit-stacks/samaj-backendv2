package com.rps.samaj.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUser_Id(UUID userId);

    @Query("SELECT dt.token FROM DeviceToken dt WHERE dt.user.id IN :userIds")
    List<String> findTokensByUserIds(List<UUID> userIds);

    @Modifying
    @Query("DELETE FROM DeviceToken dt WHERE dt.token = :token")
    void deleteByToken(String token);

    @Modifying
    @Query("DELETE FROM DeviceToken dt WHERE dt.user.id = :userId")
    void deleteByUserId(UUID userId);
}
