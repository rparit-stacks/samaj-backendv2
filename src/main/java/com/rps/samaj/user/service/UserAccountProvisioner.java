package com.rps.samaj.user.service;

import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserPrivacy;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.notification.NotificationPreference;
import com.rps.samaj.notification.NotificationPreferenceRepository;
import com.rps.samaj.user.model.UserSettings;
import com.rps.samaj.user.repository.UserPrivacyRepository;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.repository.UserSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountProvisioner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserPrivacyRepository userPrivacyRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ProfileKeyAllocator profileKeyAllocator;

    public UserAccountProvisioner(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserSettingsRepository userSettingsRepository,
            UserPrivacyRepository userPrivacyRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            ProfileKeyAllocator profileKeyAllocator
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.userPrivacyRepository = userPrivacyRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.profileKeyAllocator = profileKeyAllocator;
    }

    @Transactional
    public void ensureSidecars(User user) {
        // Caller may pass a detached instance (e.g. after save() with pre-assigned UUID uses merge).
        User managed = userRepository.getReferenceById(user.getId());
        UserProfile profile = userProfileRepository.findByUser_Id(managed.getId()).orElse(null);
        boolean created = false;
        if (profile == null) {
            profile = new UserProfile(managed);
            created = true;
        }
        boolean missingKey = profile.getProfileKey() == null || profile.getProfileKey().isBlank();
        profileKeyAllocator.ensureProfileKey(profile, managed);
        if (created || missingKey) {
            userProfileRepository.save(profile);
        }
        userSettingsRepository.findById(managed.getId()).orElseGet(() -> userSettingsRepository.save(new UserSettings(managed)));
        userPrivacyRepository.findById(managed.getId()).orElseGet(() -> userPrivacyRepository.save(new UserPrivacy(managed)));
        notificationPreferenceRepository.findById(managed.getId())
                .orElseGet(() -> notificationPreferenceRepository.save(new NotificationPreference(managed)));
    }
}
