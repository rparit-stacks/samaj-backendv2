package com.rps.samaj.matrimony;

import com.rps.samaj.notification.AppNotification;
import com.rps.samaj.notification.AppNotificationRepository;
import com.rps.samaj.notification.NotificationPreferenceRepository;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MatrimonyInAppNotifier {

    private final AppNotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public MatrimonyInAppNotifier(
            AppNotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void send(UUID recipientUserId, String title, String body, String type, String link) {
        if (recipientUserId == null) {
            return;
        }
        if (!preferenceRepository.findById(recipientUserId).map(p -> p.isInAppEnabled()).orElse(true)) {
            return;
        }
        var user = userRepository.getReferenceById(recipientUserId);
        String normType = type == null || type.isBlank() ? "MATRIMONY" : type.trim().toUpperCase().length() > 32
                ? type.trim().substring(0, 32).toUpperCase()
                : type.trim().toUpperCase();
        AppNotification n = new AppNotification(UUID.randomUUID(), user, trunc(title, 200), trunc(body, 8000), normType);
        if (link != null && !link.isBlank()) {
            n.setLink(trunc(link.trim(), 2000));
        }
        notificationRepository.save(n);
    }

    private static String trunc(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }
}
