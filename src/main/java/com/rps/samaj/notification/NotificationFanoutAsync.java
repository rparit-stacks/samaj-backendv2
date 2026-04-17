package com.rps.samaj.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationFanoutAsync {

    private static final Logger log = LoggerFactory.getLogger(NotificationFanoutAsync.class);

    private final NotificationFanoutService fanoutService;

    public NotificationFanoutAsync(NotificationFanoutService fanoutService) {
        this.fanoutService = fanoutService;
    }

    @Async("notificationExecutor")
    public void fanOut(String title, String body, String type, String link, UUID exceptUserId) {
        try {
            fanoutService.fanOutSync(title, body, type, link, exceptUserId);
        } catch (Exception e) {
            log.error("Async notification fan-out failed: {}", e.getMessage(), e);
        }
    }
}
