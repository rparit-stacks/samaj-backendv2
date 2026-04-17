package com.rps.samaj.user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.UserProfileDtos;
import com.rps.samaj.user.model.ContactRequest;
import com.rps.samaj.user.model.ContactRequestStatus;
import com.rps.samaj.user.model.FamilyMember;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserPrivacy;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.model.UserSettings;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.ContactRequestRepository;
import com.rps.samaj.user.repository.FamilyMemberRepository;
import com.rps.samaj.user.repository.UserPrivacyRepository;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.repository.UserSettingsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserProfileApiService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final UserPrivacyRepository userPrivacyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final ContactRequestRepository contactRequestRepository;
    private final UserAccountProvisioner userAccountProvisioner;
    private final ObjectMapper objectMapper;

    public UserProfileApiService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserSettingsRepository userSettingsRepository,
            UserPrivacyRepository userPrivacyRepository,
            FamilyMemberRepository familyMemberRepository,
            ContactRequestRepository contactRequestRepository,
            UserAccountProvisioner userAccountProvisioner,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.userPrivacyRepository = userPrivacyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.contactRequestRepository = contactRequestRepository;
        this.userAccountProvisioner = userAccountProvisioner;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.UserProfileResponse getMyProfile(UUID userId) {
        User user = loadActiveUser(userId);
        userAccountProvisioner.ensureSidecars(user);
        UserProfile p = userProfileRepository.findByUser_Id(userId).orElseThrow();
        return toUserProfileResponse(user, p);
    }

    @Transactional
    public UserProfileDtos.UserProfileResponse patchMyProfile(UUID userId, UserProfileDtos.UserProfilePatch patch) {
        User user = loadActiveUser(userId);
        userAccountProvisioner.ensureSidecars(user);
        UserProfile p = userProfileRepository.findByUser_Id(userId).orElseThrow();
        if (patch.fullName() != null) {
            p.setFullName(patch.fullName());
        }
        if (patch.city() != null) {
            p.setCity(patch.city());
        }
        if (patch.profession() != null) {
            p.setProfession(patch.profession());
        }
        if (patch.bio() != null) {
            p.setBio(patch.bio());
        }
        if (patch.avatarUrl() != null) {
            p.setAvatarUrl(patch.avatarUrl());
        }
        if (patch.coverImageUrl() != null) {
            p.setCoverImageUrl(patch.coverImageUrl());
        }
        if (patch.bloodGroup() != null) {
            p.setBloodGroup(patch.bloodGroup());
        }
        userProfileRepository.save(p);
        return toUserProfileResponse(user, p);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDtos.FamilyMemberResponse> listFamily(UUID userId) {
        loadActiveUser(userId);
        return familyMemberRepository.findByOwner_IdOrderByNameAsc(userId).stream()
                .map(this::toFamilyResponse)
                .toList();
    }

    @Transactional
    public UserProfileDtos.FamilyMemberResponse addFamily(UUID userId, UserProfileDtos.FamilyMemberRequest req) {
        User owner = loadActiveUser(userId);
        FamilyMember fm = new FamilyMember(UUID.randomUUID(), owner, req.name(), req.relation());
        fm.setCity(req.city());
        fm.setPhone(req.phone());
        fm.setEmail(req.email());
        return toFamilyResponse(familyMemberRepository.save(fm));
    }

    @Transactional
    public UserProfileDtos.FamilyMemberResponse updateFamily(UUID userId, UUID memberId, UserProfileDtos.FamilyMemberRequest req) {
        loadActiveUser(userId);
        FamilyMember fm = familyMemberRepository.findByIdAndOwner_Id(memberId, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Family member not found"));
        fm.setName(req.name());
        fm.setRelation(req.relation());
        fm.setCity(req.city());
        fm.setPhone(req.phone());
        fm.setEmail(req.email());
        return toFamilyResponse(familyMemberRepository.save(fm));
    }

    @Transactional
    public void deleteFamily(UUID userId, UUID memberId) {
        loadActiveUser(userId);
        FamilyMember fm = familyMemberRepository.findByIdAndOwner_Id(memberId, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Family member not found"));
        familyMemberRepository.delete(fm);
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.UserSettingsResponse getSettings(UUID userId) {
        User user = loadActiveUser(userId);
        userAccountProvisioner.ensureSidecars(user);
        UserSettings s = userSettingsRepository.findById(userId).orElseThrow();
        return new UserProfileDtos.UserSettingsResponse(s.isShowPhone(), s.isShowInDirectory(), s.isEmergencyAlerts());
    }

    @Transactional
    public UserProfileDtos.UserSettingsResponse putSettings(UUID userId, UserProfileDtos.UserSettingsResponse body) {
        User user = loadActiveUser(userId);
        userAccountProvisioner.ensureSidecars(user);
        UserSettings s = userSettingsRepository.findById(userId).orElseThrow();
        s.setShowPhone(body.showPhone());
        s.setShowInDirectory(body.showInDirectory());
        s.setEmergencyAlerts(body.emergencyAlerts());
        userSettingsRepository.save(s);
        return new UserProfileDtos.UserSettingsResponse(s.isShowPhone(), s.isShowInDirectory(), s.isEmergencyAlerts());
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.PrivacySettingsResponse getPrivacy(UUID userId) {
        User user = loadActiveUser(userId);
        userAccountProvisioner.ensureSidecars(user);
        UserPrivacy pr = userPrivacyRepository.findById(userId).orElseThrow();
        return toPrivacyResponse(pr);
    }

    @Transactional
    public UserProfileDtos.PrivacySettingsResponse patchPrivacy(UUID userId, UserProfileDtos.PrivacyPatch patch) {
        User user = loadActiveUser(userId);
        userAccountProvisioner.ensureSidecars(user);
        UserPrivacy pr = userPrivacyRepository.findById(userId).orElseThrow();
        if (patch.showEmail() != null) {
            pr.setShowEmail(patch.showEmail());
        }
        if (patch.showBloodGroup() != null) {
            pr.setShowBloodGroup(patch.showBloodGroup());
        }
        if (patch.showPhone() != null) {
            pr.setShowPhone(patch.showPhone());
        }
        if (patch.showFamilyMembers() != null) {
            pr.setShowFamilyMembers(patch.showFamilyMembers());
        }
        if (patch.profileVisibility() != null && !patch.profileVisibility().isBlank()) {
            pr.setProfileVisibility(patch.profileVisibility());
        }
        if (patch.servicePrivacy() != null) {
            try {
                pr.setServicePrivacyJson(objectMapper.writeValueAsString(patch.servicePrivacy()));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid servicePrivacy");
            }
        }
        userPrivacyRepository.save(pr);
        return toPrivacyResponse(pr);
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.SecuritySettingsResponse getSecurity(UUID userId) {
        User user = loadActiveUser(userId);
        userAccountProvisioner.ensureSidecars(user);
        UserSettings s = userSettingsRepository.findById(userId).orElseThrow();
        return new UserProfileDtos.SecuritySettingsResponse(s.isTwoFactorEnabled(), s.isLoginAlertsEnabled());
    }

    @Transactional
    public UserProfileDtos.SecuritySettingsResponse patchSecurity(UUID userId, UserProfileDtos.SecurityPatch patch) {
        User user = loadActiveUser(userId);
        userAccountProvisioner.ensureSidecars(user);
        UserSettings s = userSettingsRepository.findById(userId).orElseThrow();
        if (patch.twoFactorEnabled() != null) {
            s.setTwoFactorEnabled(patch.twoFactorEnabled());
        }
        if (patch.loginAlertsEnabled() != null) {
            s.setLoginAlertsEnabled(patch.loginAlertsEnabled());
        }
        userSettingsRepository.save(s);
        return new UserProfileDtos.SecuritySettingsResponse(s.isTwoFactorEnabled(), s.isLoginAlertsEnabled());
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.PublicProfileResponse getPublicProfile(UUID targetId, UUID viewerId) {
        User target = userRepository.findById(targetId).filter(u -> u.getStatus() == UserStatus.ACTIVE).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        userAccountProvisioner.ensureSidecars(target);
        UserProfile p = userProfileRepository.findByUser_Id(targetId).orElseThrow();
        UserPrivacy pr = userPrivacyRepository.findById(targetId).orElseThrow();
        boolean isSelf = viewerId != null && viewerId.equals(targetId);
        List<UserProfileDtos.FamilySummary> fam = List.of();
        if (isSelf || pr.isShowFamilyMembers()) {
            fam = familyMemberRepository.findByOwner_IdOrderByNameAsc(targetId).stream()
                    .map(f -> new UserProfileDtos.FamilySummary(f.getName(), f.getRelation()))
                    .toList();
        }
        Map<String, Object> sp = readServicePrivacy(pr.getServicePrivacyJson());
        return new UserProfileDtos.PublicProfileResponse(
                targetId.toString(),
                p.getProfileKey(),
                p.getFullName(),
                p.getCity(),
                p.getProfession(),
                p.getBio(),
                p.getAvatarUrl(),
                p.getCoverImageUrl(),
                maskEmail(target, pr, isSelf),
                maskPhone(target, pr, isSelf),
                maskBlood(p, pr, isSelf),
                fam,
                false,
                flag(sp, "events", "showOnProfile", true),
                flag(sp, "community", "showOnProfile", true),
                flag(sp, "emergency", "showOnProfile", true)
        );
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.ContactInfoResponse getContact(UUID targetId) {
        User target = userRepository.findById(targetId).filter(u -> u.getStatus() == UserStatus.ACTIVE).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userAccountProvisioner.ensureSidecars(target);
        UserPrivacy pr = userPrivacyRepository.findById(targetId).orElseThrow();
        UserProfile p = userProfileRepository.findByUser_Id(targetId).orElseThrow();
        return new UserProfileDtos.ContactInfoResponse(
                pr.isShowPhone() ? target.getPhone() : null,
                pr.isShowEmail() ? target.getEmail() : null,
                pr.isShowBloodGroup() ? p.getBloodGroup() : null
        );
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.VisibleProfileResponse getVisibleProfile(UUID targetId, String context) {
        User target = userRepository.findById(targetId).filter(u -> u.getStatus() == UserStatus.ACTIVE).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userAccountProvisioner.ensureSidecars(target);
        UserProfile p = userProfileRepository.findByUser_Id(targetId).orElseThrow();
        UserPrivacy pr = userPrivacyRepository.findById(targetId).orElseThrow();
        List<UserProfileDtos.FamilySummary> fam = pr.isShowFamilyMembers()
                ? familyMemberRepository.findByOwner_IdOrderByNameAsc(targetId).stream()
                .map(f -> new UserProfileDtos.FamilySummary(f.getName(), f.getRelation()))
                .toList()
                : List.of();
        String display = p.getFullName() != null ? p.getFullName() : target.getEmail();
        return new UserProfileDtos.VisibleProfileResponse(
                targetId.toString(),
                display,
                p.getAvatarUrl(),
                pr.isShowEmail() ? target.getEmail() : null,
                pr.isShowPhone() ? target.getPhone() : null,
                p.getCity(),
                p.getProfession(),
                fam,
                true
        );
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.PaginatedUserProfiles search(String q, int page, int size) {
        String t = q == null ? "" : q.trim();
        int p = Math.max(0, page);
        int s = Math.min(100, Math.max(1, size));
        // Exact user id lookup (full UUID string)
        if (!t.isEmpty()) {
            try {
                UUID id = UUID.fromString(t);
                Optional<UserProfile> op = userProfileRepository.findById(id);
                if (op.isPresent()) {
                    User u = userRepository.findById(id).orElse(null);
                    if (u != null && u.getStatus() == UserStatus.ACTIVE) {
                        UserProfile profile = op.get();
                        List<UserProfileDtos.UserProfileResponse> one =
                                List.of(toUserProfileResponse(u, profile));
                        return new UserProfileDtos.PaginatedUserProfiles(one, 1, 1, 0, s);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // not a UUID, fall through to text search
            }
        }
        Page<UserProfile> pg = userProfileRepository.searchActive(t, PageRequest.of(p, s));
        List<UserProfileDtos.UserProfileResponse> content = new ArrayList<>();
        for (UserProfile prof : pg.getContent()) {
            User u = userRepository.findById(prof.getId()).orElse(null);
            if (u != null && u.getStatus() == UserStatus.ACTIVE) {
                content.add(toUserProfileResponse(u, prof));
            }
        }
        return new UserProfileDtos.PaginatedUserProfiles(
                content,
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.PaginatedUserProfiles directory(int page, int size) {
        Page<UserProfile> pg = userProfileRepository.directoryMembers(
                PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)))
        );
        List<UserProfileDtos.UserProfileResponse> content = new ArrayList<>();
        for (UserProfile p : pg.getContent()) {
            User u = userRepository.findById(p.getId()).orElse(null);
            if (u != null) {
                content.add(toUserProfileResponse(u, p));
            }
        }
        return new UserProfileDtos.PaginatedUserProfiles(
                content,
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    @Transactional
    public UserProfileDtos.ContactRequestItem createContactRequest(UUID requesterId, UserProfileDtos.ContactRequestCreate body) {
        User requester = loadActiveUser(requesterId);
        UUID targetId = UUID.fromString(body.targetUserId());
        if (targetId.equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot request self");
        }
        User target = userRepository.findById(targetId).filter(u -> u.getStatus() == UserStatus.ACTIVE).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Target not found"));
        ContactRequest cr = new ContactRequest(UUID.randomUUID(), requester, target, body.message());
        contactRequestRepository.save(cr);
        return toContactItem(cr);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDtos.ContactRequestItem> incomingContact(UUID userId) {
        loadActiveUser(userId);
        return contactRequestRepository.findByTarget_IdOrderByCreatedAtDesc(userId).stream()
                .map(this::toContactItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserProfileDtos.ContactRequestItem> outgoingContact(UUID userId) {
        loadActiveUser(userId);
        return contactRequestRepository.findByRequester_IdOrderByCreatedAtDesc(userId).stream()
                .map(this::toContactItem)
                .toList();
    }

    @Transactional
    public UserProfileDtos.ContactRequestItem respondContact(UUID userId, UUID requestId, UserProfileDtos.ContactRequestRespond body) {
        loadActiveUser(userId);
        ContactRequest cr = contactRequestRepository.findById(requestId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!cr.getTarget().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your request to answer");
        }
        if (cr.getStatus() != ContactRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already responded");
        }
        cr.setStatus(body.approve() ? ContactRequestStatus.APPROVED : ContactRequestStatus.DENIED);
        cr.setRespondedAt(Instant.now());
        contactRequestRepository.save(cr);
        return toContactItem(cr);
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.PublicProfileResponse getPublicProfileByProfileKey(String rawKey, UUID viewerId) {
        UserProfile p = requireActiveProfileByKey(rawKey);
        return getPublicProfile(p.getUser().getId(), viewerId);
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.ContactInfoResponse getContactByProfileKey(String rawKey) {
        UserProfile p = requireActiveProfileByKey(rawKey);
        return getContact(p.getUser().getId());
    }

    @Transactional(readOnly = true)
    public UserProfileDtos.VisibleProfileResponse getVisibleProfileByProfileKey(String rawKey, String context) {
        UserProfile p = requireActiveProfileByKey(rawKey);
        return getVisibleProfile(p.getUser().getId(), context);
    }

    private UserProfile requireActiveProfileByKey(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }
        String key = rawKey.trim().toLowerCase();
        if (!key.matches("[a-z0-9._-]{1,80}")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }
        UserProfile p = userProfileRepository.findByProfileKeyIgnoreCase(key).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (p.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }
        return p;
    }

    private User loadActiveUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfileDtos.UserProfileResponse toUserProfileResponse(User user, UserProfile p) {
        return new UserProfileDtos.UserProfileResponse(
                user.getId().toString(),
                p.getProfileKey(),
                p.getFullName(),
                p.getCity(),
                p.getProfession(),
                p.getBio(),
                p.getAvatarUrl(),
                p.getCoverImageUrl(),
                user.getEmail(),
                user.getPhone(),
                p.getBloodGroup()
        );
    }

    private UserProfileDtos.FamilyMemberResponse toFamilyResponse(FamilyMember f) {
        return new UserProfileDtos.FamilyMemberResponse(
                f.getId().toString(),
                f.getName(),
                f.getRelation(),
                f.getCity(),
                f.getPhone(),
                f.getEmail()
        );
    }

    private UserProfileDtos.PrivacySettingsResponse toPrivacyResponse(UserPrivacy pr) {
        return new UserProfileDtos.PrivacySettingsResponse(
                pr.isShowEmail(),
                pr.isShowBloodGroup(),
                pr.isShowPhone(),
                pr.isShowFamilyMembers(),
                pr.getProfileVisibility(),
                readServicePrivacy(pr.getServicePrivacyJson())
        );
    }

    private Map<String, Object> readServicePrivacy(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean flag(Map<String, Object> sp, String serviceKey, String field, boolean def) {
        Object svc = sp.get(serviceKey);
        if (!(svc instanceof Map)) {
            return def;
        }
        Object v = ((Map<String, Object>) svc).get(field);
        return v instanceof Boolean b ? b : def;
    }

    private String maskEmail(User target, UserPrivacy pr, boolean self) {
        return self || pr.isShowEmail() ? target.getEmail() : null;
    }

    private String maskPhone(User target, UserPrivacy pr, boolean self) {
        return self || pr.isShowPhone() ? target.getPhone() : null;
    }

    private String maskBlood(UserProfile p, UserPrivacy pr, boolean self) {
        return self || pr.isShowBloodGroup() ? p.getBloodGroup() : null;
    }

    private UserProfileDtos.ContactRequestItem toContactItem(ContactRequest cr) {
        userAccountProvisioner.ensureSidecars(cr.getRequester());
        userAccountProvisioner.ensureSidecars(cr.getTarget());
        UserProfile rp = userProfileRepository.findByUser_Id(cr.getRequester().getId()).orElse(null);
        UserProfile tp = userProfileRepository.findByUser_Id(cr.getTarget().getId()).orElse(null);
        String rn = rp != null && rp.getFullName() != null ? rp.getFullName() : cr.getRequester().getEmail();
        String tn = tp != null && tp.getFullName() != null ? tp.getFullName() : cr.getTarget().getEmail();
        return new UserProfileDtos.ContactRequestItem(
                cr.getId().toString(),
                cr.getRequester().getId().toString(),
                cr.getTarget().getId().toString(),
                rn,
                rp != null ? rp.getAvatarUrl() : null,
                tn,
                tp != null ? tp.getAvatarUrl() : null,
                cr.getStatus().name(),
                cr.getMessage(),
                cr.getCreatedAt().toString(),
                cr.getRespondedAt() != null ? cr.getRespondedAt().toString() : null
        );
    }
}
