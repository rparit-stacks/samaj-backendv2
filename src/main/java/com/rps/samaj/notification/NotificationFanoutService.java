package com.rps.samaj.notification;

import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationFanoutService {

    private static final Logger log = LoggerFactory.getLogger(NotificationFanoutService.class);
    private static final int PAGE_SIZE = 250;

    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationBatchWriter batchWriter;
    private final NotificationWsPushService wsPushService;

    public NotificationFanoutService(
            UserRepository userRepository,
            NotificationPreferenceRepository preferenceRepository,
            NotificationBatchWriter batchWriter,
            NotificationWsPushService wsPushService
    ) {
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
        this.batchWriter = batchWriter;
        this.wsPushService = wsPushService;
    }

    public int fanOutSync(String title, String body, String type, String link, UUID exceptUserId) {
        String normType = normalizeType(type);
        Set<UUID> inAppDisabled = new HashSet<>(preferenceRepository.findUserIdsWithInAppDisabled());
        Set<UUID> typeDisabled = new HashSet<>(preferenceRepository.findUserIdsWithTypeDisabled(normType));
        int total = 0;
        int page = 0;
        Page<UUID> slice;
        do {
            slice = userRepository.findIdsByStatus(UserStatus.ACTIVE, PageRequest.of(page, PAGE_SIZE));
            List<AppNotification> batch = new ArrayList<>();
            for (UUID uid : slice.getContent()) {
                if (exceptUserId != null && exceptUserId.equals(uid)) {
                    continue;
                }
                if (inAppDisabled.contains(uid) || typeDisabled.contains(uid)) {
                    continue;
                }
                User u = userRepository.getReferenceById(uid);
                AppNotification n = new AppNotification(UUID.randomUUID(), u, trunc(title, 200), trunc(body, 8000), normType);
                if (link != null && !link.isBlank()) {
                    n.setLink(trunc(link.trim(), 2000));
                }
                batch.add(n);
            }
            if (!batch.isEmpty()) {
                batchWriter.saveAllInNewTx(batch);
                wsPushService.pushBatch(batch);
                total += batch.size();
            }
            page++;
        } while (slice.hasNext());
        log.info("Notification fan-out: {} rows type={}", total, normType);
        return total;
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
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }
}
