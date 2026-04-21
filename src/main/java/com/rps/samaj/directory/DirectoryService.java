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
import java.util.UUID;

@Service
public class DirectoryService {

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
        var page = profileRepository.directoryMembers(PageRequest.of(0, 500));
        List<DirectoryDtos.DirectoryProfileSummary> out = new ArrayList<>();
        for (UserProfile p : page.getContent()) {
            User u = userRepository.findById(p.getId()).orElse(null);
            if (u == null || u.getStatus() != UserStatus.ACTIVE) {
                continue;
            }
            UserSettings us = settingsRepository.findById(u.getId()).orElse(null);
            if (us != null && !us.isShowInDirectory()) {
                continue;
            }
            DirectorySettings ds = directorySettingsRepository.findById(u.getId()).orElse(null);
            if (ds != null && !ds.isVisible()) {
                continue;
            }
            List<DirectoryDtos.DirectoryActionDto> actions = resolveActions(u, us, ds);
            out.add(new DirectoryDtos.DirectoryProfileSummary(
                    u.getId().toString(),
                    p.getFullName(),
                    p.getAvatarUrl(),
                    p.getCity(),
                    actions
            ));
        }
        out.sort(Comparator.comparing(
                DirectoryDtos.DirectoryProfileSummary::fullName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));
        return out;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.DIRECTORY_DETAIL, key = "#userId.toString()")
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
        if (us != null && !us.isShowInDirectory()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not in directory");
        }
        DirectorySettings ds = directorySettingsRepository.findById(userId).orElse(null);
        if (ds != null && !ds.isVisible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not in directory");
        }
        String phone = (us == null || us.isShowPhone()) && u.getPhone() != null ? u.getPhone() : null;
        String email = u.getEmail();
        List<DirectoryDtos.DirectoryActionDto> actions = resolveActions(u, us, ds);
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
        boolean visible = ds == null || ds.isVisible();
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
        try {
            ds.setActionsJson(objectMapper.writeValueAsString(body.actions() == null ? List.of() : body.actions()));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid actions");
        }
        directorySettingsRepository.save(ds);
        return getMySettings();
    }

    private List<DirectoryDtos.DirectoryActionDto> resolveActions(User u, UserSettings us, DirectorySettings ds) {
        List<DirectoryDtos.DirectoryActionDto> fromJson = ds != null ? parseActions(ds.getActionsJson()) : List.of();
        if (!fromJson.isEmpty()) {
            return fromJson.stream().sorted(Comparator.comparingInt(DirectoryDtos.DirectoryActionDto::sortOrder)).toList();
        }
        List<DirectoryDtos.DirectoryActionDto> defaults = new ArrayList<>();
        int order = 0;
        if (us != null && us.isShowPhone() && u.getPhone() != null && !u.getPhone().isBlank()) {
            defaults.add(new DirectoryDtos.DirectoryActionDto("CALL", "Call", u.getPhone(), order++));
            defaults.add(new DirectoryDtos.DirectoryActionDto("WHATSAPP", "WhatsApp", u.getPhone(), order++));
        }
        if (u.getEmail() != null && !u.getEmail().isBlank()) {
            defaults.add(new DirectoryDtos.DirectoryActionDto("EMAIL", "Email", u.getEmail(), order++));
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
