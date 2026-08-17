package com.rps.samaj.directory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.DirectoryDtos;
import com.rps.samaj.config.cache.RedisCacheConfig;
import com.rps.samaj.security.JwtAuthenticationFilter;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.model.UserSettings;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.repository.UserSettingsRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DirectoryService {

    private static final int MAX_ACTIONS = 10;
    private static final Set<String> ALLOWED_ACTION_TYPES = Set.of("CALL", "WHATSAPP", "EMAIL", "LINK");

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;
    private final DirectorySettingsRepository directorySettingsRepository;
    private final ObjectMapper objectMapper;

    public DirectoryService(
            UserProfileRepository profileRepository,
            UserRepository userRepository,
            UserSettingsRepository settingsRepository,
            DirectorySettingsRepository directorySettingsRepository,
            ObjectMapper objectMapper
    ) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.directorySettingsRepository = directorySettingsRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.DIRECTORY_LIST, key = "'v1'")
    public List<DirectoryDtos.DirectoryProfileSummary> listSummaries() {
        requireUser();
        List<DirectoryListRow> rows = profileRepository.findDirectoryListRows(PageRequest.of(0, 500));
        List<DirectoryDtos.DirectoryProfileSummary> out = new ArrayList<>(rows.size());
        for (DirectoryListRow row : rows) {
            List<DirectoryDtos.DirectoryActionDto> actions = resolveActions(
                    row.getPhone(),
                    row.getEmail(),
                    Boolean.TRUE.equals(row.getShowPhone()),
                    row.getActionsJson()
            );
            out.add(new DirectoryDtos.DirectoryProfileSummary(
                    row.getId().toString(),
                    row.getFullName(),
                    row.getAvatarUrl(),
                    row.getCity(),
                    actions
            ));
        }
        out.sort(Comparator.comparing(
                DirectoryDtos.DirectoryProfileSummary::fullName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));
        return out;
    }

    // Key includes whether this is a self-view: a hidden profile is visible to its
    // owner but 404s for everyone else, so the two results must not share a slot.
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = RedisCacheConfig.Names.DIRECTORY_DETAIL,
            key = "#userId.toString() + ':' + (#userId.equals(T(com.rps.samaj.security.JwtAuthenticationFilter).currentUserIdOrNull()) ? 'self' : 'other')"
    )
    public DirectoryDtos.DirectoryProfileDetail getDetail(UUID userId) {
        requireUser();
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        if (u.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found");
        }
        UserProfile p = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        UserSettings us = settingsRepository.findById(userId).orElse(null);
        DirectorySettings ds = directorySettingsRepository.findById(userId).orElse(null);
        // Viewing your own card must always work, even while hidden from others.
        boolean self = userId.equals(JwtAuthenticationFilter.currentUserIdOrNull());
        if (!self) {
            if (us != null && !us.isShowInDirectory()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not in directory");
            }
            if (ds != null && !ds.isVisible()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not in directory");
            }
        }
        String phone = (us == null || us.isShowPhone()) && u.getPhone() != null ? u.getPhone() : null;
        String email = u.getEmail();
        List<DirectoryDtos.DirectoryActionDto> actions = resolveActions(
                u.getPhone(),
                u.getEmail(),
                us == null || us.isShowPhone(),
                ds != null ? ds.getActionsJson() : null
        );
        return new DirectoryDtos.DirectoryProfileDetail(
                u.getId().toString(),
                p.getFullName(),
                phone,
                email,
                p.getAvatarUrl(),
                p.getCity(),
                p.getProfession(),
                p.getBio(),
                p.getBloodGroup(),
                actions
        );
    }

    @Transactional(readOnly = true)
    public DirectoryDtos.DirectorySettingsDto getMySettings() {
        UUID uid = requireUserId();
        DirectorySettings ds = directorySettingsRepository.findById(uid).orElse(null);
        UserSettings us = settingsRepository.findById(uid).orElse(null);
        // The list query requires BOTH flags, so report the effective value.
        boolean visible = (ds == null || ds.isVisible()) && (us == null || us.isShowInDirectory());
        List<DirectoryDtos.DirectoryActionDto> actions = ds != null ? parseActions(ds.getActionsJson()) : List.of();
        return new DirectoryDtos.DirectorySettingsDto(visible, actions);
    }

    @Transactional
    @Caching(evict = {
            @org.springframework.cache.annotation.CacheEvict(cacheNames = RedisCacheConfig.Names.DIRECTORY_LIST, allEntries = true),
            @org.springframework.cache.annotation.CacheEvict(cacheNames = RedisCacheConfig.Names.DIRECTORY_DETAIL, allEntries = true)
    })
    public DirectoryDtos.DirectorySettingsDto updateMySettings(DirectoryDtos.DirectorySettingsUpdateDto body) {
        UUID uid = requireUserId();
        User u = userRepository.findById(uid).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        DirectorySettings ds = directorySettingsRepository.findById(uid).orElseGet(() -> new DirectorySettings(u));
        ds.setVisible(body.visible());

        List<DirectoryDtos.DirectoryActionDto> sanitized = sanitizeActions(body.actions());
        try {
            ds.setActionsJson(objectMapper.writeValueAsString(sanitized));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid actions");
        }
        directorySettingsRepository.save(ds);

        // A user hidden from the directory must also be hidden by the global
        // profile setting, otherwise the list query (which ANDs both flags)
        // and this page would disagree about the user's visibility.
        UserSettings us = settingsRepository.findById(uid).orElse(null);
        if (us != null && us.isShowInDirectory() != body.visible()) {
            us.setShowInDirectory(body.visible());
            settingsRepository.save(us);
        }

        // Build the response from what was just persisted rather than calling
        // getMySettings() — a self-invocation would bypass the Spring proxy.
        return new DirectoryDtos.DirectorySettingsDto(ds.isVisible(), sanitized);
    }

    /**
     * Normalises action rows coming from the client: drops blanks, forces a known
     * type, fills in a default label and renumbers sortOrder so the stored order
     * always matches the order the user arranged them in.
     */
    private static List<DirectoryDtos.DirectoryActionDto> sanitizeActions(List<DirectoryDtos.DirectoryActionDto> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        List<DirectoryDtos.DirectoryActionDto> out = new ArrayList<>(input.size());
        int order = 0;
        for (DirectoryDtos.DirectoryActionDto a : input) {
            if (a == null || a.value() == null || a.value().isBlank()) {
                continue;
            }
            String type = normalizeActionType(a.type());
            String label = (a.label() == null || a.label().isBlank()) ? defaultLabel(type) : a.label().trim();
            out.add(new DirectoryDtos.DirectoryActionDto(type, trunc(label, 40), trunc(a.value().trim(), 500), order++));
            if (order >= MAX_ACTIONS) {
                break;
            }
        }
        return out;
    }

    private static String normalizeActionType(String type) {
        if (type == null) {
            return "LINK";
        }
        String t = type.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_ACTION_TYPES.contains(t) ? t : "LINK";
    }

    private static String defaultLabel(String type) {
        return switch (type) {
            case "CALL" -> "Call";
            case "WHATSAPP" -> "WhatsApp";
            case "EMAIL" -> "Email";
            default -> "Website";
        };
    }

    private static String trunc(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private List<DirectoryDtos.DirectoryActionDto> resolveActions(
            String phone,
            String email,
            boolean showPhone,
            String actionsJson
    ) {
        List<DirectoryDtos.DirectoryActionDto> fromJson = parseActions(actionsJson);
        if (!fromJson.isEmpty()) {
            return fromJson.stream().sorted(Comparator.comparingInt(DirectoryDtos.DirectoryActionDto::sortOrder)).toList();
        }
        List<DirectoryDtos.DirectoryActionDto> defaults = new ArrayList<>();
        int order = 0;
        if (showPhone && phone != null && !phone.isBlank()) {
            defaults.add(new DirectoryDtos.DirectoryActionDto("CALL", "Call", phone, order++));
            defaults.add(new DirectoryDtos.DirectoryActionDto("WHATSAPP", "WhatsApp", phone, order++));
        }
        if (email != null && !email.isBlank()) {
            defaults.add(new DirectoryDtos.DirectoryActionDto("EMAIL", "Email", email, order++));
        }
        return defaults;
    }

    private List<DirectoryDtos.DirectoryActionDto> parseActions(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static void requireUser() {
        if (JwtAuthenticationFilter.currentUserIdOrNull() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private static UUID requireUserId() {
        UUID id = JwtAuthenticationFilter.currentUserIdOrNull();
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return id;
    }
}
