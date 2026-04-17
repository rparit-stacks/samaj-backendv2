package com.rps.samaj.user.service;

import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class ProfileKeyAllocator {

    private final UserProfileRepository userProfileRepository;

    public ProfileKeyAllocator(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * URL-safe handle from email local-part (e.g. rohitparit1934@gmail.com → rohitparit1934).
     */
    public static String baseKeyFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "user";
        }
        String local = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT);
        String slug = local.replaceAll("[^a-z0-9._-]", "");
        if (slug.isEmpty()) {
            return "user";
        }
        if (slug.length() > 64) {
            slug = slug.substring(0, 64);
        }
        return slug;
    }

    public void ensureProfileKey(UserProfile profile, User user) {
        if (profile.getProfileKey() != null && !profile.getProfileKey().isBlank()) {
            return;
        }
        String base = baseKeyFromEmail(user.getEmail());
        String candidate = base;
        int attempt = 0;
        while (true) {
            var existing = userProfileRepository.findByProfileKeyIgnoreCase(candidate);
            if (existing.isEmpty()) {
                break;
            }
            if (existing.get().getUser().getId().equals(user.getId())) {
                break;
            }
            attempt++;
            String suffix = Integer.toHexString((user.getId().hashCode() ^ attempt) & 0xffff);
            candidate = base + "-" + suffix;
            if (candidate.length() > 92) {
                candidate = base.substring(0, Math.min(32, base.length())) + "-" + UUID.randomUUID().toString().substring(0, 8);
            }
        }
        profile.setProfileKey(candidate);
    }
}
