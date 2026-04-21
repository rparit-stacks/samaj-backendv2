package com.rps.samaj.admin.users;

import com.rps.samaj.api.dto.AdminUserDtos;
import com.rps.samaj.config.cache.RedisCacheConfig;
import com.rps.samaj.notification.NotificationPreference;
import com.rps.samaj.notification.NotificationPreferenceRepository;
import com.rps.samaj.user.model.KycStatus;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserPrivacy;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.model.UserRole;
import com.rps.samaj.user.model.UserSecurity;
import com.rps.samaj.user.model.UserSettings;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.UserPrivacyRepository;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.repository.UserSecurityRepository;
import com.rps.samaj.user.repository.UserSettingsRepository;
import com.rps.samaj.user.service.UserAccountProvisioner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminUserManagementService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserPrivacyRepository userPrivacyRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final UserAccountProvisioner userAccountProvisioner;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminUserManagementService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserSettingsRepository userSettingsRepository,
            UserPrivacyRepository userPrivacyRepository,
            UserSecurityRepository userSecurityRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            UserAccountProvisioner userAccountProvisioner,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.userPrivacyRepository = userPrivacyRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.userAccountProvisioner = userAccountProvisioner;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = RedisCacheConfig.Names.ADMIN_USERS_LIST,
            key = "T(String).valueOf(#page).concat(':').concat(T(String).valueOf(#size)).concat(':')\n+                    .concat(#q == null ? '' : #q).concat(':')\n+                    .concat(#roleStr == null ? 'all' : #roleStr).concat(':')\n+                    .concat(#statusStr == null ? 'all' : #statusStr)"
    )
    public AdminUserDtos.UserPageResponse list(String q, String roleStr, String statusStr, int page, int size) {
        String qq = blankToNull(q);
        UserRole role = parseRole(roleStr);
        UserStatus status = parseStatus(statusStr);
        Page<User> pg = userRepository.searchForAdmin(qq, role, status, PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        List<UUID> ids = pg.getContent().stream().map(User::getId).toList();
        Map<UUID, UserProfile> profiles = ids.isEmpty()
                ? Map.of()
                : userProfileRepository.findByIdIn(ids).stream().collect(Collectors.toMap(UserProfile::getId, p -> p));
        List<AdminUserDtos.UserSummary> content = pg.getContent().stream()
                .map(u -> toSummary(u, profiles.get(u.getId())))
                .toList();
        return new AdminUserDtos.UserPageResponse(
                content,
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.ADMIN_USER_DETAIL, key = "#id.toString()")
    public AdminUserDtos.UserFullDetail get(UUID id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toFullDetail(u);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.ADMIN_USERS_LIST,
            RedisCacheConfig.Names.ADMIN_USER_DETAIL
    }, allEntries = true)
    public AdminUserDtos.UserSummary update(UUID id, AdminUserDtos.UserUpdateRequest body) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (body.email() != null && !body.email().isBlank()) {
            String email = body.email().trim().toLowerCase(Locale.ROOT);
            userRepository.findByEmailIgnoreCase(email).filter(other -> !other.getId().equals(id))
                    .ifPresent(x -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
                    });
            u.setEmail(email);
        }
        if (body.phone() != null) {
            String phone = body.phone().trim().isEmpty() ? null : body.phone().trim();
            if (phone != null) {
                userRepository.findByPhone(phone).filter(other -> !other.getId().equals(id))
                        .ifPresent(x -> {
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already in use");
                        });
            }
            u.setPhone(phone);
        }
        if (body.role() != null && !body.role().isBlank()) {
            u.setRole(UserRole.valueOf(body.role().trim().toUpperCase(Locale.ROOT)));
        }
        if (body.status() != null && !body.status().isBlank()) {
            u.setStatus(UserStatus.valueOf(body.status().trim().toUpperCase(Locale.ROOT)));
        }
        if (body.emailVerified() != null) {
            u.setEmailVerified(body.emailVerified());
        }
        if (body.phoneVerified() != null) {
            u.setPhoneVerified(body.phoneVerified());
        }
        u.setUpdatedAt(Instant.now());
        userRepository.save(u);
        if (body.name() != null) {
            UserProfile p = userProfileRepository.findByUser_Id(id).orElse(null);
            if (p != null) {
                p.setFullName(body.name().trim().isEmpty() ? null : body.name().trim());
                userProfileRepository.save(p);
            }
        }
        UserProfile p = userProfileRepository.findByUser_Id(id).orElse(null);
        return toSummary(u, p);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.ADMIN_USERS_LIST,
            RedisCacheConfig.Names.ADMIN_USER_DETAIL
    }, allEntries = true)
    public AdminUserDtos.UserCreateResponse create(AdminUserDtos.UserCreateRequest body) {
        String email = body.email() != null ? body.email().trim().toLowerCase(Locale.ROOT) : null;
        if (email == null || email.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        String phone = blankToNull(body.phone());
        if (phone != null && userRepository.findByPhone(phone).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
        }
        String rawPassword = blankToNull(body.password());
        String tempPassword = rawPassword != null ? rawPassword : randomPassword(12);
        UserRole role = body.role() != null && !body.role().isBlank()
                ? UserRole.valueOf(body.role().trim().toUpperCase(Locale.ROOT))
                : UserRole.USER;
        UserStatus status = body.status() != null && !body.status().isBlank()
                ? UserStatus.valueOf(body.status().trim().toUpperCase(Locale.ROOT))
                : UserStatus.ACTIVE;
        UUID id = UUID.randomUUID();
        User user = new User(id, email, phone, passwordEncoder.encode(tempPassword), status, role);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setParentAdmin(false);
        user.setKycStatus(KycStatus.NONE);
        user.setMetadata("{}");
        user = userRepository.save(user);
        userAccountProvisioner.ensureSidecars(user);
        if (body.name() != null && !body.name().isBlank()) {
            userProfileRepository.findByUser_Id(user.getId()).ifPresent(p -> {
                p.setFullName(body.name().trim());
                userProfileRepository.save(p);
            });
        }
        UserProfile p = userProfileRepository.findByUser_Id(user.getId()).orElse(null);
        return new AdminUserDtos.UserCreateResponse(toSummary(user, p), rawPassword != null ? null : tempPassword);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.ADMIN_USERS_LIST,
            RedisCacheConfig.Names.ADMIN_USER_DETAIL
    }, allEntries = true)
    public void deleteSoft(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userProfileRepository.findByUser_Id(id).ifPresent(p -> {
            p.setProfileKey("del-" + id.toString().replace("-", ""));
            userProfileRepository.save(p);
        });
        user.setStatus(UserStatus.DELETED);
        user.setPasswordHash(null);
        user.setEmail("deleted+" + user.getId() + "@invalid.local");
        user.setPhone(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private AdminUserDtos.UserFullDetail toFullDetail(User u) {
        UserProfile p = userProfileRepository.findByUser_Id(u.getId()).orElse(null);
        UserSettings s = userSettingsRepository.findById(u.getId()).orElse(null);
        UserPrivacy pv = userPrivacyRepository.findById(u.getId()).orElse(null);
        UserSecurity sec = userSecurityRepository.findById(u.getId()).orElse(null);
        NotificationPreference n = notificationPreferenceRepository.findById(u.getId()).orElse(null);
        boolean passwordSet = u.getPasswordHash() != null && !u.getPasswordHash().isBlank();
        return new AdminUserDtos.UserFullDetail(
                u.getId().toString(),
                u.getEmail(),
                u.getPhone(),
                u.getRole().name(),
                u.getStatus().name(),
                u.getKycStatus().name(),
                u.isEmailVerified(),
                u.isPhoneVerified(),
                u.isParentAdmin(),
                u.getAdminServiceKeys(),
                passwordSet,
                u.getGoogleId(),
                u.getMetadata(),
                u.getCreatedAt() != null ? u.getCreatedAt().toString() : null,
                u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : null,
                p != null ? p.getFullName() : null,
                p != null ? p.getProfileKey() : null,
                p != null ? p.getBio() : null,
                p != null ? p.getCity() : null,
                p != null ? p.getProfession() : null,
                p != null ? p.getBloodGroup() : null,
                p != null ? p.getAvatarUrl() : null,
                p != null ? p.getCoverImageUrl() : null,
                s != null && s.isShowPhone(),
                s != null && s.isShowInDirectory(),
                s != null && s.isEmergencyAlerts(),
                s != null && s.isTwoFactorEnabled(),
                s != null && s.isLoginAlertsEnabled(),
                pv != null && pv.isShowEmail(),
                pv != null && pv.isShowBloodGroup(),
                pv != null && pv.isShowPhone(),
                pv != null && pv.isShowFamilyMembers(),
                pv != null ? pv.getProfileVisibility() : null,
                pv != null ? pv.getServicePrivacyJson() : null,
                sec != null && sec.isTwoFactorEnabled(),
                sec != null && sec.isLoginAlertsEnabled(),
                n != null && n.isEmailEnabled(),
                n != null && n.isInAppEnabled(),
                n != null && n.isSecurityEmailEnabled()
        );
    }

    private AdminUserDtos.UserSummary toSummary(User u, UserProfile p) {
        return new AdminUserDtos.UserSummary(
                u.getId().toString(),
                u.getEmail(),
                u.getPhone(),
                p != null ? p.getFullName() : null,
                p != null ? p.getProfileKey() : null,
                u.getRole().name(),
                u.getStatus().name(),
                u.getKycStatus().name(),
                u.isEmailVerified(),
                u.isPhoneVerified(),
                u.isParentAdmin(),
                u.getCreatedAt() != null ? u.getCreatedAt().toString() : null,
                u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : null
        );
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static UserRole parseRole(String s) {
        String t = blankToNull(s);
        if (t == null || "all".equalsIgnoreCase(t)) {
            return null;
        }
        return UserRole.valueOf(t.toUpperCase(Locale.ROOT));
    }

    private static UserStatus parseStatus(String s) {
        String t = blankToNull(s);
        if (t == null || "all".equalsIgnoreCase(t)) {
            return null;
        }
        return UserStatus.valueOf(t.toUpperCase(Locale.ROOT));
    }

    private String randomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(secureRandom.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
