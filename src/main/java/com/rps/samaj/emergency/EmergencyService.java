package com.rps.samaj.emergency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.EmergencyDtos;
import com.rps.samaj.config.cache.RedisCacheConfig;
import com.rps.samaj.notification.PublicNotificationPublisher;
import com.rps.samaj.security.JwtAuthenticationFilter;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmergencyService {

    private static final Set<String> VALID_TYPES = Set.of("MEDICAL", "ACCIDENT", "FINANCIAL", "BLOOD", "OTHER");
    private static final Set<String> VALID_STATUSES = Set.of(
            "OPEN", "IN_PROGRESS", "HELP_RECEIVED", "RESOLVED", "CANCELLED", "CLOSED"
    );
    private static final Set<String> ACTIVE_STATUSES = Set.of("OPEN", "IN_PROGRESS", "HELP_RECEIVED");

    private final EmergencyCaseRepository caseRepository;
    private final EmergencyHelperRepository helperRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final ObjectMapper objectMapper;
    private final PublicNotificationPublisher notificationPublisher;

    public EmergencyService(
            EmergencyCaseRepository caseRepository,
            EmergencyHelperRepository helperRepository,
            UserRepository userRepository,
            UserProfileRepository profileRepository,
            ObjectMapper objectMapper,
            PublicNotificationPublisher notificationPublisher
    ) {
        this.caseRepository = caseRepository;
        this.helperRepository = helperRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.EMERGENCY_LIST,
            key = "'user:'.concat(#creatorFilter == null ? 'all' : #creatorFilter.toString())")
    public List<EmergencyDtos.EmergencyItemResponse> listForUser(UUID creatorFilter) {
        requireUser();
        List<EmergencyCase> rows = creatorFilter != null
                ? caseRepository.findByCreator_IdOrderByCreatedAtDesc(creatorFilter)
                : caseRepository.findAllByOrderByCreatedAtDesc();
        return toDtos(rows);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.EMERGENCY_LIST, key = "'admin:all'")
    public List<EmergencyDtos.EmergencyItemResponse> listForAdmin() {
        return toDtos(caseRepository.findAllByOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.EMERGENCY_LIST, key = "'mine:'.concat(#userId.toString())")
    public List<EmergencyDtos.EmergencyItemResponse> listMine(UUID userId) {
        requireUser();
        return toDtos(caseRepository.findByCreator_IdOrderByCreatedAtDesc(userId));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.EMERGENCY_DETAIL, key = "T(String).valueOf(#id)")
    public EmergencyDtos.EmergencyItemResponse getById(long id) {
        requireUser();
        EmergencyCase e = caseRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency not found"));
        return toDto(e, profileMap(List.of(e)));
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.EMERGENCY_LIST,
            RedisCacheConfig.Names.EMERGENCY_DETAIL
    }, allEntries = true)
    public EmergencyDtos.EmergencyItemResponse create(EmergencyDtos.EmergencyCreateRequest body) {
        UUID creatorId = requireUserId();
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        Instant now = Instant.now();
        EmergencyCase e = new EmergencyCase();
        e.setCreator(creator);
        e.setType(normalizeType(body.type()));
        e.setTitle(body.title().trim());
        e.setDescription(body.description().trim());
        e.setArea(trimOrNull(body.area()));
        e.setCity(body.city().trim());
        e.setState(body.state().trim());
        e.setCountry(body.country().trim());
        e.setLandmark(trimOrNull(body.landmark()));
        e.setLocationDescription(trimOrNull(body.locationDescription()));
        e.setLatitude(body.latitude());
        e.setLongitude(body.longitude());
        e.setStatus("OPEN");
        e.setEmergencyAt(body.emergencyAt() != null ? body.emergencyAt() : now);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        e.setHelperCount(0);
        e.setViewCount(0);
        e.setContactClickCount(0);
        e.setResolvedByExternal(false);
        e.setExternalHelperNote(null);
        e.setContactPrefsJson(writePrefs(new EmergencyDtos.EmergencyContactPreferencesResponse(
                trimOrNull(body.contactPhone()),
                trimOrNull(body.contactWhatsapp()),
                trimOrNull(body.contactEmail()),
                Boolean.TRUE.equals(body.allowPhone()),
                Boolean.TRUE.equals(body.allowWhatsapp()),
                Boolean.TRUE.equals(body.allowEmail())
        )));
        caseRepository.saveAndFlush(e);
        notificationPublisher.onEmergencyCreated(e.getId(), creatorId, e.getTitle());
        return toDto(e, profileMap(List.of(e)));
    }

    @Transactional
    public EmergencyDtos.EmergencyItemResponse update(long id, UUID userId, EmergencyDtos.EmergencyUpdateRequest body) {
        EmergencyCase e = loadOwnedCase(id, userId);
        if (isTerminal(e.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit a closed emergency");
        }
        if (body.type() != null && !body.type().isBlank()) {
            e.setType(normalizeType(body.type()));
        }
        if (body.title() != null && !body.title().isBlank()) {
            e.setTitle(body.title().trim());
        }
        if (body.description() != null) {
            e.setDescription(body.description().trim());
        }
        if (body.area() != null) {
            e.setArea(trimOrNull(body.area()));
        }
        if (body.city() != null && !body.city().isBlank()) {
            e.setCity(body.city().trim());
        }
        if (body.state() != null && !body.state().isBlank()) {
            e.setState(body.state().trim());
        }
        if (body.country() != null && !body.country().isBlank()) {
            e.setCountry(body.country().trim());
        }
        if (body.landmark() != null) {
            e.setLandmark(trimOrNull(body.landmark()));
        }
        if (body.locationDescription() != null) {
            e.setLocationDescription(trimOrNull(body.locationDescription()));
        }
        if (body.latitude() != null) {
            e.setLatitude(body.latitude());
        }
        if (body.longitude() != null) {
            e.setLongitude(body.longitude());
        }
        if (body.emergencyAt() != null) {
            e.setEmergencyAt(body.emergencyAt());
        }
        mergeContactPrefs(e, body);
        e.setUpdatedAt(Instant.now());
        return toDto(e, profileMap(List.of(e)));
    }

    @Transactional
    public void delete(long id, UUID userId) {
        EmergencyCase e = loadOwnedCase(id, userId);
        List<EmergencyHelper> helpers = helperRepository.findByEmergency_IdOrderByHelpedAtDesc(e.getId());
        if (!helpers.isEmpty()) {
            helperRepository.deleteAll(helpers);
        }
        caseRepository.delete(e);
    }

    @Transactional
    public void adminDelete(long id) {
        EmergencyCase e = caseRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency not found"));
        List<EmergencyHelper> helpers = helperRepository.findByEmergency_IdOrderByHelpedAtDesc(e.getId());
        if (!helpers.isEmpty()) {
            helperRepository.deleteAll(helpers);
        }
        caseRepository.delete(e);
    }

    @Transactional
    public EmergencyDtos.EmergencyItemResponse patchStatus(long id, UUID userId, String status) {
        EmergencyCase e = loadOwnedCase(id, userId);
        e.setStatus(normalizeStatus(status));
        e.setUpdatedAt(Instant.now());
        return toDto(e, profileMap(List.of(e)));
    }

    @Transactional
    public EmergencyDtos.EmergencyItemResponse adminPatchStatus(long id, String status) {
        EmergencyCase e = caseRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency not found"));
        e.setStatus(normalizeStatus(status));
        e.setUpdatedAt(Instant.now());
        return toDto(e, profileMap(List.of(e)));
    }

    @Transactional
    public EmergencyDtos.EmergencyItemResponse resolve(long id, UUID userId, EmergencyDtos.EmergencyResolveRequest body) {
        EmergencyCase e = loadOwnedCase(id, userId);
        if (!ACTIVE_STATUSES.contains(e.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Emergency is not active");
        }
        if (body.externalHelper()) {
            e.setResolvedByExternal(true);
            e.setExternalHelperNote(trimOrNull(
                    body.externalHelperNote() != null ? body.externalHelperNote() : body.note()
            ));
            e.setStatus("RESOLVED");
        } else {
            if (body.helperUserId() == null || body.helperUserId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "helperUserId required when not external");
            }
            UUID hid;
            try {
                hid = UUID.fromString(body.helperUserId().trim());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid helper user id");
            }
            User helper = userRepository.findById(hid)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Helper user not found"));
            String note = trimOrNull(body.note());
            EmergencyHelper h = new EmergencyHelper(UUID.randomUUID(), e, helper, Instant.now(), note);
            helperRepository.save(h);
            e.setHelperCount(e.getHelperCount() + 1);
            e.setResolvedByExternal(false);
            e.setStatus("RESOLVED");
        }
        e.setUpdatedAt(Instant.now());
        return toDto(e, profileMap(List.of(e)));
    }

    @Transactional
    public List<EmergencyDtos.EmergencyHelpItemResponse> listHelpers(long emergencyId) {
        requireUser();
        if (!caseRepository.existsById(emergencyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency not found");
        }
        return helperRepository.findByEmergency_IdOrderByHelpedAtDesc(emergencyId).stream()
                .map(h -> new EmergencyDtos.EmergencyHelpItemResponse(
                        emergencyId,
                        h.getHelper().getId().toString(),
                        h.getHelpedAt(),
                        h.getNote()
                ))
                .toList();
    }

    @Transactional
    public void trackView(long id) {
        requireUser();
        EmergencyCase e = caseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency not found"));
        e.setViewCount(e.getViewCount() + 1);
        e.setUpdatedAt(Instant.now());
    }

    @Transactional
    public void trackContactClick(long id) {
        requireUser();
        EmergencyCase e = caseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency not found"));
        e.setContactClickCount(e.getContactClickCount() + 1);
        e.setUpdatedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public EmergencyDtos.DashboardStatsResponse dashboardStats(UUID userId) {
        requireUser();
        List<EmergencyCase> mine = caseRepository.findByCreator_IdOrderByCreatedAtDesc(userId);
        long total = mine.size();
        long active = mine.stream().filter(c -> ACTIVE_STATUSES.contains(c.getStatus())).count();
        long resolved = mine.stream().filter(c -> "RESOLVED".equals(c.getStatus()) || "CLOSED".equals(c.getStatus())).count();
        long clicks = mine.stream().mapToLong(EmergencyCase::getContactClickCount).sum();
        long views = mine.stream().mapToLong(EmergencyCase::getViewCount).sum();
        long helped = helperRepository.countByHelper_Id(userId);
        return new EmergencyDtos.DashboardStatsResponse(total, active, resolved, clicks, views, helped);
    }

    @Transactional(readOnly = true)
    public EmergencyDtos.HelperStatsResponse helperStats(UUID helperUserId) {
        requireUser();
        List<EmergencyHelper> helps = helperRepository.findByHelper_IdWithEmergency(helperUserId);
        long total = helps.size();
        long distinct = helps.stream().map(h -> h.getEmergency().getCreator().getId()).distinct().count();
        Instant first = helps.stream().map(EmergencyHelper::getHelpedAt).min(Instant::compareTo).orElse(null);
        Instant last = helps.stream().map(EmergencyHelper::getHelpedAt).max(Instant::compareTo).orElse(null);
        return new EmergencyDtos.HelperStatsResponse(
                helperUserId.toString(),
                total,
                distinct,
                first,
                last
        );
    }

    private EmergencyCase loadOwnedCase(long id, UUID userId) {
        EmergencyCase e = caseRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Emergency not found"));
        if (!e.getCreator().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner");
        }
        return e;
    }

    private static boolean isTerminal(String status) {
        return "RESOLVED".equals(status) || "CLOSED".equals(status);
    }

    private void mergeContactPrefs(EmergencyCase e, EmergencyDtos.EmergencyUpdateRequest body) {
        EmergencyDtos.EmergencyContactPreferencesResponse cur = readPrefs(e.getContactPrefsJson());
        String phone = body.contactPhone() != null ? trimOrNull(body.contactPhone()) : cur.phone();
        String wa = body.contactWhatsapp() != null ? trimOrNull(body.contactWhatsapp()) : cur.whatsapp();
        String em = body.contactEmail() != null ? trimOrNull(body.contactEmail()) : cur.email();
        boolean ap = body.allowPhone() != null ? body.allowPhone() : cur.allowPhone();
        boolean aw = body.allowWhatsapp() != null ? body.allowWhatsapp() : cur.allowWhatsapp();
        boolean ae = body.allowEmail() != null ? body.allowEmail() : cur.allowEmail();
        e.setContactPrefsJson(writePrefs(new EmergencyDtos.EmergencyContactPreferencesResponse(
                phone, wa, em, ap, aw, ae
        )));
    }

    private EmergencyDtos.EmergencyContactPreferencesResponse readPrefs(String json) {
        if (json == null || json.isBlank()) {
            return new EmergencyDtos.EmergencyContactPreferencesResponse(null, null, null, false, false, false);
        }
        try {
            return objectMapper.readValue(json, EmergencyDtos.EmergencyContactPreferencesResponse.class);
        } catch (JsonProcessingException e) {
            return new EmergencyDtos.EmergencyContactPreferencesResponse(null, null, null, false, false, false);
        }
    }

    private String writePrefs(EmergencyDtos.EmergencyContactPreferencesResponse p) {
        try {
            return objectMapper.writeValueAsString(p);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save contact preferences");
        }
    }

    private List<EmergencyDtos.EmergencyItemResponse> toDtos(List<EmergencyCase> rows) {
        Map<UUID, UserProfile> profiles = profileMap(rows);
        return rows.stream().map(e -> toDto(e, profiles)).toList();
    }

    private Map<UUID, UserProfile> profileMap(List<EmergencyCase> rows) {
        Set<UUID> ids = rows.stream().map(e -> e.getCreator().getId()).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UserProfile> map = new HashMap<>();
        for (UserProfile p : profileRepository.findByIdIn(ids)) {
            map.put(p.getId(), p);
        }
        return map;
    }

    private EmergencyDtos.EmergencyItemResponse toDto(EmergencyCase e, Map<UUID, UserProfile> profiles) {
        UserProfile p = profiles.get(e.getCreator().getId());
        String display = p != null && p.getFullName() != null && !p.getFullName().isBlank()
                ? p.getFullName()
                : null;
        String photo = p != null ? p.getAvatarUrl() : null;
        return new EmergencyDtos.EmergencyItemResponse(
                e.getId(),
                e.getCreator().getId().toString(),
                display,
                photo,
                e.getType(),
                e.getTitle(),
                e.getDescription(),
                e.getArea(),
                e.getCity(),
                e.getState(),
                e.getCountry(),
                e.getLandmark(),
                e.getLocationDescription(),
                e.getLatitude(),
                e.getLongitude(),
                e.getStatus(),
                e.getEmergencyAt(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getHelperCount(),
                e.getViewCount(),
                e.getContactClickCount(),
                e.isResolvedByExternal(),
                e.getExternalHelperNote(),
                readPrefs(e.getContactPrefsJson())
        );
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "OTHER";
        }
        String u = type.trim().toUpperCase();
        return VALID_TYPES.contains(u) ? u : "OTHER";
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        String u = status.trim().toUpperCase();
        if (!VALID_STATUSES.contains(u)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        return u;
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
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
