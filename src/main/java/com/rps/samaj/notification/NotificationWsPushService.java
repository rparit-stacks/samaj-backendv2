package com.rps.samaj.notification;

import com.rps.samaj.api.dto.NotificationDtos;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;

/**
 * Pushes newly-saved notifications to connected WebSocket clients via STOMP.
 * Each user receives their own notification on /user/queue/notifications,
 * so the frontend can update the bell badge without polling.
 */
@Component
public class NotificationWsPushService {

    private final SimpMessagingTemplate messaging;

    public NotificationWsPushService(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void pushBatch(List<AppNotification> notifications) {
        for (AppNotification n : notifications) {
            NotificationDtos.WsNotificationEvent event = new NotificationDtos.WsNotificationEvent(
                    "NEW_NOTIFICATION", toPayload(n));
            messaging.convertAndSendToUser(
                    n.getUser().getId().toString(),
                    "/queue/notifications",
                    event);
        }
    }

    public void pushOne(AppNotification n) {
        NotificationDtos.WsNotificationEvent event = new NotificationDtos.WsNotificationEvent(
                "NEW_NOTIFICATION", toPayload(n));
        messaging.convertAndSendToUser(
                n.getUser().getId().toString(),
                "/queue/notifications",
                event);
    }

    private static NotificationDtos.NotificationResponse toPayload(AppNotification n) {
        return new NotificationDtos.NotificationResponse(
                n.getId().toString(),
                n.getTitle(),
                n.getBody(),
                n.getType(),
                n.isRead(),
                n.getLink(),
                n.getCreatedAt() == null ? null : n.getCreatedAt().atOffset(ZoneOffset.UTC).toString());
    }
}
