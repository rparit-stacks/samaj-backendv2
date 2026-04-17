package com.rps.samaj.directory;

import com.rps.samaj.api.dto.AdminDirectoryDtos;
import com.rps.samaj.api.dto.DirectoryDtos;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.model.UserSettings;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.repository.UserSettingsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class AdminDirectoryService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;
    private final DirectorySettingsRepository directorySettingsRepository;

    public AdminDirectoryService(
            UserProfileRepository profileRepository,
            UserRepository userRepository,
            UserSettingsRepository settingsRepository,
            DirectorySettingsRepository directorySettingsRepository
    ) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.directorySettingsRepository = directorySettingsRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminDirectoryDtos.DirectoryEntrySummary> list(String q) {
        String qn = q == null || q.isBlank() ? null : q.trim().toLowerCase(Locale.ROOT);
        var page = profileRepository.directoryMembers(PageRequest.of(0, 1000));
        List<AdminDirectoryDtos.DirectoryEntrySummary> out = new ArrayList<>();
        for (UserProfile p : page.getContent()) {
            User u = userRepository.findById(p.getId()).orElse(null);
            if (u == null) {
                continue;
            }
            if (u.getStatus() != UserStatus.ACTIVE) {
                continue;
            }
            if (qn != null) {
                String name = p.getFullName() == null ? "" : p.getFullName();
                String city = p.getCity() == null ? "" : p.getCity();
                if (!name.toLowerCase(Locale.ROOT).contains(qn)
                        && !city.toLowerCase(Locale.ROOT).contains(qn)
                        && !u.getEmail().toLowerCase(Locale.ROOT).contains(qn)) {
                    continue;
                }
            }
            UserSettings us = settingsRepository.findById(u.getId()).orElse(null);
            DirectorySettings ds = directorySettingsRepository.findById(u.getId()).orElse(null);
            boolean showInDirectory = us == null || us.isShowInDirectory();
            boolean visible = ds == null || ds.isVisible();
            List<DirectoryDtos.DirectoryActionDto> actions = resolveActions(u, us, ds);
            out.add(new AdminDirectoryDtos.DirectoryEntrySummary(
                    u.getId().toString(),
                    p.getFullName(),
                    p.getCity(),
                    visible,
                    showInDirectory,
                    actions
            ));
        }
        out.sort(Comparator.comparing(
                AdminDirectoryDtos.DirectoryEntrySummary::fullName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));
        return out;
    }

    @Transactional(readOnly = true)
    public AdminDirectoryDtos.DirectoryEntryDetail get(UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        if (u.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found");
        }
        UserProfile p = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        UserSettings us = settingsRepository.findById(userId).orElse(null);
        DirectorySettings ds = directorySettingsRepository.findById(userId).orElse(null);
        boolean showInDirectory = us == null || us.isShowInDirectory();
        boolean visible = ds == null || ds.isVisible();
        boolean showPhone = us == null || us.isShowPhone();
        String phone = showPhone && u.getPhone() != null ? u.getPhone() : null;
        String email = u.getEmail();
        List<DirectoryDtos.DirectoryActionDto> actions = resolveActions(u, us, ds);
        return new AdminDirectoryDtos.DirectoryEntryDetail(
                u.getId().toString(),
                p.getFullName(),
                p.getCity(),
                p.getProfession(),
                phone,
                email,
                visible,
                showInDirectory,
                actions
        );
    }

    public AdminDirectoryDtos.DirectoryEntryDetail update(UUID userId, AdminDirectoryDtos.DirectoryEntryUpdateRequest body) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        if (u.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member is not active");
        }
        UserSettings us = settingsRepository.findById(userId).orElseGet(() -> {
            UserSettings s = new UserSettings(u);
            return settingsRepository.save(s);
        });
        if (body.showInDirectory() != null) {
            us.setShowInDirectory(body.showInDirectory());
            settingsRepository.save(us);
        }
        DirectorySettings ds = directorySettingsRepository.findById(userId).orElseGet(() -> new DirectorySettings(u));
        ds.setVisible(body.visible());
        // actions are stored via DirectoryService.updateMySettings endpoint normally;
        // here we simply mirror new actions when provided.
        if (body.actions() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custom actions editing not supported yet");
        }
        directorySettingsRepository.save(ds);
        return get(userId);
    }

    public void delete(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        UserSettings us = settingsRepository.findById(userId).orElse(null);
        if (us != null) {
            us.setShowInDirectory(false);
            settingsRepository.save(us);
        }
        DirectorySettings ds = directorySettingsRepository.findById(userId).orElse(null);
        if (ds != null) {
            ds.setVisible(false);
            directorySettingsRepository.save(ds);
        }
    }

    private List<DirectoryDtos.DirectoryActionDto> resolveActions(User u, UserSettings us, DirectorySettings ds) {
        if (ds != null) {
            // Admin UI currently does not edit custom actions; rely on existing JSON if present.
            return new ArrayList<>();
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
}

