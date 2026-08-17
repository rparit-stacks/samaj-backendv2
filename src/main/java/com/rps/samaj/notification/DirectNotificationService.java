package com.rps.samaj.notification;

import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Sends a notification to a single user.
 *
 * The existing {@link NotificationFanoutService} broadcasts to every active
 * member, which is wrong for person-to-person events such as contact requests.
 * This service persists one row, pushes it over STOMP so the recipient's bell
 * updates without waiting for a poll, and sends an FCM push so the phone is
 * notified while the app is closed.
 */
@Service
public class DirectNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DirectNotificationService.class);

    private final UserRepository userRepository;
    private final AppNotificationRepository appNotificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationWsPushService wsPushService;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;

    public DirectNotificationService(
            UserRepository userRepository,
            AppNotificationRepository appNotificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            NotificationWsPushService wsPushService,
            DeviceTokenRepository deviceTokenRepository,
            FcmService fcmService
    ) {
        this.userRepository = userRepository;
        this.appNotificationRepository = appNotificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.wsPushService = wsPushService;
        this.deviceTokenRepository = deviceTokenRepository;
        this.fcmService = fcmService;
    }

    /**
     * Notifies one user. Runs in its own transaction so a delivery failure can
     * never roll back the business action that triggered it.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyUser(UUID recipientId, String title, String body, String type, String link) {
        if (recipientId == null) {
            return;
        }
        try {
            User recipient = userRepository.findById(recipientId).orElse(null);
            if (recipient == null || recipient.getStatus() != UserStatus.ACTIVE) {
                return;
            }

            String normType = normalizeType(type);
            NotificationPreference pref = preferenceRepository.findById(recipientId).orElse(null);
            if (pref != null && (!pref.isInAppEnabled() || pref.getDisabledTypes().contains(normType))) {
                return;
            }

            AppNotification n = new AppNotification(
                    UUID.randomUUID(), recipient, trunc(title, 200), trunc(body, 8000), normType);
            if (link != null && !link.isBlank()) {
                n.setLink(trunc(link.trim(), 2000));
            }
            appNotificationRepository.save(n);

            // Instant in-app delivery (bell badge + list) for open sessions.
            wsPushService.pushOne(n);

            // Push to the device so a closed app still surfaces it.
            if (fcmService.isReady()) {
                List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(recipientId));
                if (!tokens.isEmpty()) {
                    fcmService.sendToTokens(tokens, n.getTitle(), n.getBody(), n.getLink(), normType);
                }
            }
        } catch (Exception e) {
            // Never let notification delivery break the caller's flow.
            log.error("Direct notification to {} failed: {}", recipientId, e.getMessage(), e);
        }
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "INFO";
        }
        String u = type.trim().toUpperCase(Locale.ROOT);
        return u.length() > 32 ? u.substring(0, 32) : u;
    }

    private static String trunc(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
