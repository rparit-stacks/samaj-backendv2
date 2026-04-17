package com.rps.samaj.admin.system;

import com.rps.samaj.api.dto.AdminSystemDtos;
import com.rps.samaj.user.model.KycStatus;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserRole;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.service.UserAccountProvisioner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChildAdminManagementService {

    private static final Map<AdminServiceKey, String> DESCRIPTIONS = new EnumMap<>(AdminServiceKey.class);
    private static final Map<AdminServiceKey, String> PATH_PREFIX = new EnumMap<>(AdminServiceKey.class);

    static {
        DESCRIPTIONS.put(AdminServiceKey.COMMUNITY, "Community posts, comments, moderation");
        DESCRIPTIONS.put(AdminServiceKey.DIRECTORY, "Directory listings and settings");
        DESCRIPTIONS.put(AdminServiceKey.EMERGENCY, "Emergency cases and helpers");
        DESCRIPTIONS.put(AdminServiceKey.DOCUMENTS, "Documents library (admin)");
        DESCRIPTIONS.put(AdminServiceKey.CHAT, "Chat administration");
        DESCRIPTIONS.put(AdminServiceKey.NEWS, "News categories and articles");
        DESCRIPTIONS.put(AdminServiceKey.EVENTS, "Events and RSVPs");
        DESCRIPTIONS.put(AdminServiceKey.KYC, "KYC review");
        DESCRIPTIONS.put(AdminServiceKey.NOTIFICATIONS, "Push / app notifications");
        DESCRIPTIONS.put(AdminServiceKey.HISTORY, "Samaj history log");
        DESCRIPTIONS.put(AdminServiceKey.APP_CONFIG, "Runtime app configuration");
        DESCRIPTIONS.put(AdminServiceKey.EXAM, "Exam content and alerts");
        DESCRIPTIONS.put(AdminServiceKey.MATRIMONY, "Matrimony administration");
        DESCRIPTIONS.put(AdminServiceKey.GALLERY, "Gallery albums");
        DESCRIPTIONS.put(AdminServiceKey.SUGGESTION, "User suggestions");
        DESCRIPTIONS.put(AdminServiceKey.ACHIEVER, "Community achievers & marquee");

        PATH_PREFIX.put(AdminServiceKey.EMERGENCY, "/admin/emergencies");
        PATH_PREFIX.put(AdminServiceKey.NOTIFICATIONS, "/admin/notifications");
        PATH_PREFIX.put(AdminServiceKey.HISTORY, "/admin/history");
        PATH_PREFIX.put(AdminServiceKey.EVENTS, "/admin/events");
        PATH_PREFIX.put(AdminServiceKey.NEWS, "/admin/news");
        PATH_PREFIX.put(AdminServiceKey.APP_CONFIG, "/admin/app-config");
        PATH_PREFIX.put(AdminServiceKey.DOCUMENTS, "/admin/documents");
        PATH_PREFIX.put(AdminServiceKey.KYC, "/admin/kyc");
        PATH_PREFIX.put(AdminServiceKey.COMMUNITY, "/admin/community");
        PATH_PREFIX.put(AdminServiceKey.DIRECTORY, "/admin/directory");
        PATH_PREFIX.put(AdminServiceKey.CHAT, "/admin/chat");
        PATH_PREFIX.put(AdminServiceKey.EXAM, "/admin/exam");
        PATH_PREFIX.put(AdminServiceKey.MATRIMONY, "/admin/matrimony");
        PATH_PREFIX.put(AdminServiceKey.GALLERY, "/admin/gallery");
        PATH_PREFIX.put(AdminServiceKey.SUGGESTION, "/admin/suggestions");
        PATH_PREFIX.put(AdminServiceKey.ACHIEVER, "/admin/achievements");
    }

    private final UserRepository userRepository;
    private final AdminServiceGrantRepository grantRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountProvisioner userAccountProvisioner;
    private final AdminAuthorizationService adminAuthorizationService;

    public ChildAdminManagementService(
            UserRepository userRepository,
            AdminServiceGrantRepository grantRepository,
            PasswordEncoder passwordEncoder,
            UserAccountProvisioner userAccountProvisioner,
            AdminAuthorizationService adminAuthorizationService
    ) {
        this.userRepository = userRepository;
        this.grantRepository = grantRepository;
        this.passwordEncoder = passwordEncoder;
        this.userAccountProvisioner = userAccountProvisioner;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<AdminSystemDtos.ServiceCatalogEntry> catalog() {
        return Arrays.stream(AdminServiceKey.values())
                .sorted(Comparator.comparing(AdminServiceKey::name))
                .map(k -> new AdminSystemDtos.ServiceCatalogEntry(
                        k.name(),
                        DESCRIPTIONS.getOrDefault(k, "Administration"),
                        PATH_PREFIX.getOrDefault(k, "(not mapped yet)")
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminSystemDtos.AdminMeResponse me(User user) {
        boolean full = adminAuthorizationService.hasFullAdminServiceAccess(user);
        List<String> keys;
        if (full) {
            keys = Arrays.stream(AdminServiceKey.values()).map(Enum::name).sorted().toList();
        } else if (user.getRole() == UserRole.MODERATOR) {
            keys = grantRepository.findServiceKeysByUser_Id(user.getId()).stream()
                    .map(Enum::name)
                    .sorted()
                    .toList();
        } else {
            keys = List.of();
        }
        return new AdminSystemDtos.AdminMeResponse(
                user.getId().toString(),
                user.getRole().name(),
                user.isParentAdmin(),
                full,
                keys
        );
    }

    @Transactional(readOnly = true)
    public AdminSystemDtos.ChildAdminPageResponse listChildAdmins(int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        Page<User> pg = userRepository.findByRole(UserRole.MODERATOR, PageRequest.of(p, s));
        List<AdminSystemDtos.ChildAdminSummaryResponse> rows = pg.stream()
                .map(this::toSummary)
                .toList();
        return new AdminSystemDtos.ChildAdminPageResponse(
                rows,
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminSystemDtos.ChildAdminSummaryResponse getChildAdmin(UUID id) {
        User u = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Child admin not found"));
        if (u.getRole() != UserRole.MODERATOR) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child admin not found");
        }
        return toSummary(u);
    }

    public AdminSystemDtos.ChildAdminSummaryResponse createChildAdmin(AdminSystemDtos.ChildAdminCreateRequest body) {
        String email = body.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        String phone = normalizePhone(body.phone());
        if (phone != null && userRepository.findByPhone(phone).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already in use");
        }
        Set<AdminServiceKey> keys = parseServiceKeys(body.serviceKeys());
        if (keys.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one service is required");
        }

        UUID id = UUID.randomUUID();
        User user = new User(
                id,
                email,
                phone,
                passwordEncoder.encode(body.password()),
                UserStatus.ACTIVE,
                UserRole.MODERATOR
        );
        user.setEmailVerified(true);
        user.setPhoneVerified(phone != null);
        user.setParentAdmin(false);
        user.setKycStatus(KycStatus.NONE);
        user.setMetadata("{}");
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);
        userAccountProvisioner.ensureSidecars(user);
        replaceGrants(user, keys);
        syncCsv(user, keys);
        return toSummary(userRepository.getReferenceById(user.getId()));
    }

    public AdminSystemDtos.ChildAdminSummaryResponse updateChildAdmin(UUID id, AdminSystemDtos.ChildAdminUpdateRequest body) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Child admin not found"));
        if (user.getRole() != UserRole.MODERATOR) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Child admin not found");
        }
        if (body.email() != null && !body.email().isBlank()) {
            String email = body.email().trim().toLowerCase(Locale.ROOT);
            userRepository.findByEmailIgnoreCase(email).filter(u -> !u.getId().equals(id)).ifPresent(u -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
            });
            user.setEmail(email);
        }
        if (body.phone() != null) {
            String phone = normalizePhone(body.phone());
            if (phone != null) {
                userRepository.findByPhone(phone).filter(u -> !u.getId().equals(id)).ifPresent(u -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already in use");
                });
            }
            user.setPhone(phone);
        }
        if (body.newPassword() != null && !body.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(body.newPassword()));
        }
        if (body.status() != null && !body.status().isBlank()) {
            user.setStatus(parseChildAdminStatus(body.status()));
        }
        if (body.serviceKeys() != null) {
            Set<AdminServiceKey> keys = parseServiceKeys(body.serviceKeys());
            if (keys.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one service is required");
            }
            replaceGrants(user, keys);
            syncCsv(user, keys);
        }
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return toSummary(userRepository.getReferenceById(user.getId()));
    }

    private AdminSystemDtos.ChildAdminSummaryResponse toSummary(User u) {
        List<String> keys = grantRepository.findServiceKeysByUser_Id(u.getId()).stream()
                .map(Enum::name)
                .sorted()
                .toList();
        return new AdminSystemDtos.ChildAdminSummaryResponse(
                u.getId().toString(),
                u.getEmail(),
                u.getPhone(),
                u.getStatus().name(),
                keys
        );
    }

    private void replaceGrants(User user, Set<AdminServiceKey> keys) {
        grantRepository.deleteByUser_Id(user.getId());
        for (AdminServiceKey k : keys) {
            grantRepository.save(new AdminServiceGrant(user, k));
        }
    }

    private void syncCsv(User user, Set<AdminServiceKey> keys) {
        String csv = keys.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        user.setAdminServiceKeys(csv.isEmpty() ? null : csv);
        userRepository.save(user);
    }

    private static Set<AdminServiceKey> parseServiceKeys(List<String> raw) {
        LinkedHashSet<AdminServiceKey> out = new LinkedHashSet<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            try {
                out.add(AdminServiceKey.valueOf(s.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown service key: " + s);
            }
        }
        return out;
    }

    private static UserStatus parseChildAdminStatus(String raw) {
        try {
            UserStatus s = UserStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            if (s != UserStatus.ACTIVE && s != UserStatus.SUSPENDED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ACTIVE or SUSPENDED");
            }
            return s;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String p = phone.replaceAll("\\s+", "").trim();
        return p.isEmpty() ? null : p;
    }
}
