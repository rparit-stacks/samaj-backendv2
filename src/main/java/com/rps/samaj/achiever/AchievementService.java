package com.rps.samaj.achiever;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.AchievementDtos;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AchievementService {

    private static final int MAX_FIELDS = 40;
    private static final int MAX_HEADLINE = 200;
    private static final int MAX_LABEL = 120;
    private static final int MAX_TEXT = 2000;
    private static final int MAX_LONG_TEXT = 8000;
    private static final int MAX_URL = 2000;

    private final AchievementRepository achievementRepository;
    private final AchievementFieldTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    public AchievementService(
            AchievementRepository achievementRepository,
            AchievementFieldTemplateRepository templateRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            ObjectMapper objectMapper
    ) {
        this.achievementRepository = achievementRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AchievementDtos.AchievementDetailResponse create(UUID userId, AchievementDtos.AchievementCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        String json = validateAndSerializeFields(req.fields());
        Achievement a = new Achievement(UUID.randomUUID(), user, trimHeadline(req.headline()), json);
        achievementRepository.save(a);
        return toDetail(a, userId);
    }

    @Transactional
    public AchievementDtos.AchievementDetailResponse updateOwn(UUID userId, UUID achievementId, AchievementDtos.AchievementUpdateRequest req) {
        Achievement a = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found"));
        if (!a.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your achievement");
        }
        if (a.getStatus() != AchievementStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending achievements can be edited");
        }
        a.setHeadline(trimHeadline(req.headline()));
        a.setFieldsJson(validateAndSerializeFields(req.fields()));
        a.setUpdatedAt(Instant.now());
        achievementRepository.save(a);
        return toDetail(a, userId);
    }

    @Transactional(readOnly = true)
    public List<AchievementDtos.AchievementMarqueeCard> listMarquee() {
        Instant now = Instant.now();
        List<Achievement> list = achievementRepository.findForMarquee(now, PageRequest.of(0, 50));
        List<AchievementDtos.AchievementMarqueeCard> out = new ArrayList<>();
        for (Achievement a : list) {
            User u = a.getUser();
            UserProfile p = userProfileRepository.findById(u.getId()).orElse(null);
            String name = p != null && p.getFullName() != null && !p.getFullName().isBlank()
                    ? p.getFullName()
                    : (u.getEmail() != null ? u.getEmail() : "Member");
            String avatar = p != null ? p.getAvatarUrl() : null;
            out.add(new AchievementDtos.AchievementMarqueeCard(
                    a.getId().toString(),
                    a.getHeadline(),
                    u.getId().toString(),
                    name,
                    avatar
            ));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public AchievementDtos.AchievementPageResponse pageApproved(int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<Achievement> pg = achievementRepository.findByStatusOrderByCreatedAtDesc(AchievementStatus.APPROVED, p);
        return toPage(pg, null);
    }

    @Transactional(readOnly = true)
    public AchievementDtos.AchievementPageResponse pageMine(UUID userId, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<Achievement> pg = achievementRepository.findByUser_IdOrderByCreatedAtDesc(userId, p);
        return toPage(pg, userId);
    }

    @Transactional(readOnly = true)
    public AchievementDtos.AchievementDetailResponse get(UUID viewerUserId, UUID achievementId) {
        Achievement a = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found"));
        boolean owner = a.getUser().getId().equals(viewerUserId);
        if (!owner && a.getStatus() != AchievementStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found");
        }
        return toDetail(a, viewerUserId);
    }

    // --- Admin ---

    @Transactional(readOnly = true)
    public AchievementDtos.AchievementAdminPageResponse adminPage(String statusFilter, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        Page<Achievement> pg;
        if (statusFilter == null || statusFilter.isBlank()) {
            pg = achievementRepository.findAllByOrderByCreatedAtDesc(p);
        } else {
            AchievementStatus st = parseStatus(statusFilter);
            pg = achievementRepository.findByStatusOrderByCreatedAtDesc(st, p);
        }
        List<AchievementDtos.AchievementAdminSummary> content = pg.getContent().stream().map(this::toAdminSummary).toList();
        return new AchievementDtos.AchievementAdminPageResponse(
                content,
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AchievementDtos.AchievementDetailResponse adminGet(UUID id) {
        Achievement a = achievementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found"));
        return toDetail(a, a.getUser().getId());
    }

    @Transactional
    public void adminDelete(UUID id) {
        if (!achievementRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found");
        }
        achievementRepository.deleteById(id);
    }

    @Transactional
    public AchievementDtos.AchievementDetailResponse adminFullUpdate(UUID id, AchievementDtos.AchievementAdminUpdateRequest req) {
        Achievement a = achievementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found"));
        AchievementStatus newStatus = parseStatus(req.status());
        a.setHeadline(trimHeadline(req.headline()));
        a.setFieldsJson(validateAndSerializeFields(req.fields()));
        a.setStatus(newStatus);
        if (newStatus == AchievementStatus.REJECTED) {
            a.setRejectionReason(req.rejectionReason() != null ? req.rejectionReason().trim() : "");
        } else {
            a.setRejectionReason(null);
        }
        if (req.marqueeEnabled() != null) {
            a.setMarqueeEnabled(req.marqueeEnabled());
        }
        if (req.marqueeStart() != null && !req.marqueeStart().isBlank()) {
            a.setMarqueeStart(Instant.parse(req.marqueeStart().trim()));
        }
        if (req.marqueeEnd() != null && !req.marqueeEnd().isBlank()) {
            a.setMarqueeEnd(Instant.parse(req.marqueeEnd().trim()));
        }
        a.setUpdatedAt(Instant.now());
        achievementRepository.save(a);
        return toDetail(a, a.getUser().getId());
    }

    @Transactional
    public AchievementDtos.AchievementDetailResponse adminApprove(UUID id, AchievementDtos.AchievementApproveRequest req) {
        Achievement a = achievementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found"));
        int days = req.marqueeDays() != null ? req.marqueeDays() : 3;
        days = Math.min(30, Math.max(1, days));
        Instant start = Instant.now();
        Instant end = start.plus(days, ChronoUnit.DAYS);
        a.setStatus(AchievementStatus.APPROVED);
        a.setRejectionReason(null);
        a.setMarqueeStart(start);
        a.setMarqueeEnd(end);
        a.setMarqueeEnabled(req.marqueeEnabled() == null || req.marqueeEnabled());
        a.setUpdatedAt(Instant.now());
        achievementRepository.save(a);
        return toDetail(a, a.getUser().getId());
    }

    @Transactional
    public AchievementDtos.AchievementDetailResponse adminReject(UUID id, AchievementDtos.AchievementRejectRequest req) {
        Achievement a = achievementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found"));
        a.setStatus(AchievementStatus.REJECTED);
        a.setRejectionReason(req.reason().trim());
        a.setMarqueeEnabled(false);
        a.setMarqueeStart(null);
        a.setMarqueeEnd(null);
        a.setUpdatedAt(Instant.now());
        achievementRepository.save(a);
        return toDetail(a, a.getUser().getId());
    }

    @Transactional
    public AchievementDtos.AchievementDetailResponse adminPatchMarquee(UUID id, AchievementDtos.AchievementMarqueeAdminPatch req) {
        Achievement a = achievementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Achievement not found"));
        if (req.marqueeEnabled() != null) {
            a.setMarqueeEnabled(req.marqueeEnabled());
        }
        if (req.marqueeEnd() != null && !req.marqueeEnd().isBlank()) {
            a.setMarqueeEnd(Instant.parse(req.marqueeEnd().trim()));
        }
        a.setUpdatedAt(Instant.now());
        achievementRepository.save(a);
        return toDetail(a, a.getUser().getId());
    }

    // --- Templates ---

    @Transactional(readOnly = true)
    public List<AchievementDtos.AchievementFieldTemplateResponse> listTemplates(boolean activeOnly) {
        List<AchievementFieldTemplate> list = activeOnly
                ? templateRepository.findByActiveOrderByNameAsc(true)
                : templateRepository.findAll();
        return list.stream().map(this::toTemplateResponse).toList();
    }

    @Transactional
    public AchievementDtos.AchievementFieldTemplateResponse createTemplate(AchievementDtos.AchievementFieldTemplateCreateRequest req) {
        validateTemplateSchema(req.schemaJson());
        AchievementFieldTemplate t = new AchievementFieldTemplate(UUID.randomUUID(), req.name().trim(), req.schemaJson().trim());
        templateRepository.save(t);
        return toTemplateResponse(t);
    }

    @Transactional
    public AchievementDtos.AchievementFieldTemplateResponse updateTemplate(UUID id, AchievementDtos.AchievementFieldTemplateUpdateRequest req) {
        AchievementFieldTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
        if (req.name() != null && !req.name().isBlank()) {
            t.setName(req.name().trim());
        }
        if (req.schemaJson() != null && !req.schemaJson().isBlank()) {
            validateTemplateSchema(req.schemaJson());
            t.setSchemaJson(req.schemaJson().trim());
        }
        if (req.active() != null) {
            t.setActive(req.active());
        }
        t.setUpdatedAt(Instant.now());
        templateRepository.save(t);
        return toTemplateResponse(t);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        if (!templateRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
        }
        templateRepository.deleteById(id);
    }

    private AchievementDtos.AchievementFieldTemplateResponse toTemplateResponse(AchievementFieldTemplate t) {
        return new AchievementDtos.AchievementFieldTemplateResponse(
                t.getId().toString(),
                t.getName(),
                t.getSchemaJson(),
                t.isActive(),
                t.getCreatedAt().toString(),
                t.getUpdatedAt().toString()
        );
    }

    private void validateTemplateSchema(String schemaJson) {
        try {
            List<?> nodes = objectMapper.readValue(schemaJson, new TypeReference<List<?>>() {
            });
            if (nodes == null || nodes.isEmpty() || nodes.size() > MAX_FIELDS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template schema must have 1–" + MAX_FIELDS + " field definitions");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid template schema JSON");
        }
    }

    private AchievementDtos.AchievementAdminSummary toAdminSummary(Achievement a) {
        User u = a.getUser();
        UserProfile p = userProfileRepository.findById(u.getId()).orElse(null);
        String name = p != null && p.getFullName() != null ? p.getFullName() : "";
        return new AchievementDtos.AchievementAdminSummary(
                a.getId().toString(),
                a.getHeadline(),
                a.getStatus().name(),
                u.getId().toString(),
                u.getEmail(),
                name,
                a.isMarqueeEnabled(),
                a.getMarqueeStart() != null ? a.getMarqueeStart().toString() : null,
                a.getMarqueeEnd() != null ? a.getMarqueeEnd().toString() : null,
                a.getCreatedAt().toString(),
                a.getUpdatedAt().toString()
        );
    }

    private AchievementDtos.AchievementPageResponse toPage(Page<Achievement> pg, UUID viewerForPrivacy) {
        List<AchievementDtos.AchievementDetailResponse> rows = pg.getContent().stream()
                .map(a -> toDetail(a, viewerForPrivacy != null ? viewerForPrivacy : a.getUser().getId()))
                .toList();
        return new AchievementDtos.AchievementPageResponse(
                rows,
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    private AchievementDtos.AchievementDetailResponse toDetail(Achievement a, UUID viewerUserId) {
        User u = a.getUser();
        UserProfile p = userProfileRepository.findById(u.getId()).orElse(null);
        String profileKey = p != null ? p.getProfileKey() : null;
        List<AchievementDtos.AchievementFieldItem> fields = readFields(a.getFieldsJson());
        boolean isOwner = viewerUserId != null && viewerUserId.equals(u.getId());
        String displayName = "Member";
        if (p != null) {
            if (p.getFullName() != null && !p.getFullName().isBlank()) {
                displayName = p.getFullName();
            } else if (p.getProfileKey() != null && !p.getProfileKey().isBlank()) {
                displayName = p.getProfileKey();
            }
        }
        if (isOwner && (displayName.equals("Member") || displayName.isBlank()) && u.getEmail() != null) {
            displayName = u.getEmail();
        }
        return new AchievementDtos.AchievementDetailResponse(
                a.getId().toString(),
                a.getHeadline(),
                fields,
                a.getStatus().name(),
                a.getRejectionReason(),
                a.getCreatedAt().toString(),
                a.getUpdatedAt().toString(),
                u.getId().toString(),
                displayName,
                p != null ? p.getAvatarUrl() : null,
                profileKey,
                a.isMarqueeEnabled(),
                a.getMarqueeStart() != null ? a.getMarqueeStart().toString() : null,
                a.getMarqueeEnd() != null ? a.getMarqueeEnd().toString() : null
        );
    }

    private List<AchievementDtos.AchievementFieldItem> readFields(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<AchievementDtos.AchievementFieldItem>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String validateAndSerializeFields(List<AchievementDtos.AchievementFieldItem> fields) {
        if (fields == null || fields.isEmpty() || fields.size() > MAX_FIELDS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide 1–" + MAX_FIELDS + " fields");
        }
        List<AchievementDtos.AchievementFieldItem> normalized = new ArrayList<>();
        for (AchievementDtos.AchievementFieldItem f : fields) {
            String type = f.type() != null ? f.type().trim().toUpperCase(Locale.ROOT) : "";
            String label = f.label() != null ? f.label().trim() : "";
            String id = f.id() != null ? f.id().trim() : "";
            String value = f.value() == null ? "" : f.value();
            if (id.isEmpty() || id.length() > 64) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each field needs a stable id (max 64 chars)");
            }
            if (label.isEmpty() || label.length() > MAX_LABEL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid field label");
            }
            String checkedValue = switch (type) {
                case "TEXT" -> checkLen(value, MAX_TEXT, "TEXT");
                case "LONG_TEXT" -> checkLen(value, MAX_LONG_TEXT, "LONG_TEXT");
                case "DATE" -> checkLen(value, 40, "DATE");
                case "LINK", "IMAGE" -> checkUrl(value);
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported field type: " + type);
            };
            normalized.add(new AchievementDtos.AchievementFieldItem(id, type, label, checkedValue));
        }
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize fields");
        }
    }

    private static String checkLen(String value, int max, String label) {
        if (value.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " value too long (max " + max + ")");
        }
        return value;
    }

    private static String checkUrl(String value) {
        if (value.isBlank()) {
            return "";
        }
        if (value.length() > MAX_URL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL too long");
        }
        try {
            URI uri = URI.create(value.trim());
            if (uri.getScheme() == null || !(uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only http(s) URLs allowed");
            }
            return value.trim();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL");
        }
    }

    private static String trimHeadline(String h) {
        String t = h != null ? h.trim() : "";
        if (t.isEmpty() || t.length() > MAX_HEADLINE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Headline required (max " + MAX_HEADLINE + " chars)");
        }
        return t;
    }

    private static AchievementStatus parseStatus(String s) {
        try {
            return AchievementStatus.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
    }
}
