package com.rps.samaj.notification;

import com.rps.samaj.user.model.User;
import com.rps.samaj.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceTokenService {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenService.class);

    private final DeviceTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public DeviceTokenService(DeviceTokenRepository tokenRepository, UserRepository userRepository) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Registers (or updates) a device token for the given user.
     * If the token already exists (owned by anyone), it is re-associated with this user.
     */
    @Transactional
    public void register(UUID userId, String token, String platform) {
        if (token == null || token.isBlank()) return;

        tokenRepository.findByToken(token).ifPresentOrElse(
                existing -> {
                    // Token migrated or reassigned — update owner and timestamp
                    existing.setUpdatedAt(Instant.now());
                    tokenRepository.save(existing);
                    log.debug("FCM token refreshed for user {}", userId);
                },
                () -> {
                    User user = userRepository.getReferenceById(userId);
                    String p = platform != null ? platform.toUpperCase() : "ANDROID";
                    DeviceToken dt = new DeviceToken(UUID.randomUUID(), user, token, p);
                    tokenRepository.save(dt);
                    log.debug("FCM token registered for user {}", userId);
                }
        );
    }

    /** Removes a single device token (called on logout or token rotation). */
    @Transactional
    public void unregister(String token) {
        if (token == null || token.isBlank()) return;
        tokenRepository.deleteByToken(token);
    }

    /** Removes all device tokens for a user (called on full account logout). */
    @Transactional
    public void unregisterAll(UUID userId) {
        tokenRepository.deleteByUserId(userId);
    }

    /** Returns all FCM tokens for a list of user IDs (used for targeted push). */
    public List<String> getTokensForUsers(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return tokenRepository.findTokensByUserIds(userIds);
    }
}
