package com.rps.samaj.notification;

import com.rps.samaj.api.dto.NotificationDtos;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationService(
            AppNotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public NotificationDtos.PageResponse<NotificationDtos.NotificationResponse> list(
            UUID userId,
            int page,
            int size,
            boolean unreadOnly
    ) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        Page<AppNotification> pg = unreadOnly
                ? notificationRepository.findByUser_IdAndReadIsFalseOrderByCreatedAtDesc(userId, PageRequest.of(p, s))
                : notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(p, s));
        return new NotificationDtos.PageResponse<>(
                pg.getContent().stream().map(this::toResponse).toList(),
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    @Transactional(readOnly = true)
    public NotificationDtos.UnreadCountResponse unreadCount(UUID userId) {
        return new NotificationDtos.UnreadCountResponse(notificationRepository.countByUser_IdAndReadIsFalse(userId));
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        AppNotification n = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        n.setRead(true);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    @Transactional
    public void deleteOne(UUID userId, UUID notificationId) {
        AppNotification n = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notificationRepository.delete(n);
    }

    @Transactional
    public void clearAll(UUID userId) {
        notificationRepository.deleteAllForUser(userId);
    }

    @Transactional(readOnly = true)
    public NotificationDtos.NotificationPreferencesResponse getPreferences(UUID userId) {
        NotificationPreference pref = preferenceRepository.findById(userId).orElse(null);
        if (pref == null) {
            return new NotificationDtos.NotificationPreferencesResponse(true, true, true);
        }
        return new NotificationDtos.NotificationPreferencesResponse(
                pref.isEmailEnabled(),
                pref.isInAppEnabled(),
                pref.isSecurityEmailEnabled()
        );
    }

    @Transactional
    public NotificationDtos.NotificationPreferencesResponse updatePreferences(
            UUID userId,
            NotificationDtos.NotificationPreferencesUpdateRequest body
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        NotificationPreference pref = preferenceRepository.findById(userId).orElseGet(() -> new NotificationPreference(user));
        if (body.emailEnabled() != null) {
            pref.setEmailEnabled(body.emailEnabled());
        }
        if (body.inAppEnabled() != null) {
            pref.setInAppEnabled(body.inAppEnabled());
        }
        if (body.securityEmailEnabled() != null) {
            pref.setSecurityEmailEnabled(body.securityEmailEnabled());
        }
        preferenceRepository.save(pref);
        return new NotificationDtos.NotificationPreferencesResponse(
                pref.isEmailEnabled(),
                pref.isInAppEnabled(),
                pref.isSecurityEmailEnabled()
        );
    }

    private NotificationDtos.NotificationResponse toResponse(AppNotification n) {
        return new NotificationDtos.NotificationResponse(
                n.getId().toString(),
                n.getTitle(),
                n.getBody(),
                n.getType(),
                n.isRead(),
                n.getLink(),
                n.getCreatedAt() == null ? null : n.getCreatedAt().atOffset(ZoneOffset.UTC).toString()
        );
    }
}
