package com.rps.samaj.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Off-request-thread wrapper around {@link DirectNotificationService} so
 * sending a contact request returns immediately while the recipient's
 * notification is written and pushed in the background.
 */
@Component
public class DirectNotificationAsync {

    private static final Logger log = LoggerFactory.getLogger(DirectNotificationAsync.class);

    private final DirectNotificationService directNotificationService;

    public DirectNotificationAsync(DirectNotificationService directNotificationService) {
        this.directNotificationService = directNotificationService;
    }

    @Async("notificationExecutor")
    public void notifyUser(UUID recipientId, String title, String body, String type, String link) {
        try {
            directNotificationService.notifyUser(recipientId, title, body, type, link);
        } catch (Exception e) {
            log.error("Async direct notification failed: {}", e.getMessage(), e);
        }
    }
}
