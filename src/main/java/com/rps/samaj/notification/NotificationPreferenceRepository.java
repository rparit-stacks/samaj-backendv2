package com.rps.samaj.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    @Query("select p.id from NotificationPreference p where p.inAppEnabled = false")
    List<UUID> findUserIdsWithInAppDisabled();
}
