package com.rps.samaj.matrimony;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.MatrimonyDtos;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MatrimonyService {

    private final MatrimonyProfileRepository profileRepository;
    private final MatrimonyInterestRepository interestRepository;
    private final MatrimonyConversationRepository conversationRepository;
    private final MatrimonyChatMessageRepository messageRepository;
    private final MatrimonyFavoriteRepository favoriteRepository;
    private final MatrimonyBlockRepository blockRepository;
    private final MatrimonyProfileViewRepository viewRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final MatrimonyInAppNotifier notifier;
    private final EntityManager entityManager;

    public MatrimonyService(
            MatrimonyProfileRepository profileRepository,
            MatrimonyInterestRepository interestRepository,
            MatrimonyConversationRepository conversationRepository,
            MatrimonyChatMessageRepository messageRepository,
            MatrimonyFavoriteRepository favoriteRepository,
            MatrimonyBlockRepository blockRepository,
            MatrimonyProfileViewRepository viewRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            MatrimonyInAppNotifier notifier,
            EntityManager entityManager
    ) {
        this.profileRepository = profileRepository;
        this.interestRepository = interestRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.favoriteRepository = favoriteRepository;
        this.blockRepository = blockRepository;
        this.viewRepository = viewRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.notifier = notifier;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.MatrimonyMeSummary meSummary(UUID userId) {
        List<MatrimonyProfileEntity> mine = profileRepository.findByOwner_IdOrderByUpdatedAtDesc(userId);
        int active = (int) mine.stream().filter(p -> "ACTIVE".equals(p.getStatus())).count();
        int draft = (int) mine.stream().filter(p -> "DRAFT".equals(p.getStatus())).count();
        List<MatrimonyDtos.MatrimonyProfileSummaryItem> items = mine.stream()
                .map(p -> new MatrimonyDtos.MatrimonyProfileSummaryItem(
                        p.getId().toString(),
                        p.getDisplayName(),
                        p.getStatus(),
                        p.getDraftStep(),
                        p.getProfileSubject(),
                        p.getRelativeRelation(),
                        p.getCompletionPercent() != null ? p.getCompletionPercent() : 0
                ))
                .toList();
        return new MatrimonyDtos.MatrimonyMeSummary(active > 0, active, draft, items);
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.MatrimonyDashboard dashboard(UUID userId) {
        return new MatrimonyDtos.MatrimonyDashboard(
                interestRepository.countSentByUser(userId),
                interestRepository.countReceivedByUser(userId),
                interestRepository.countAcceptedForUser(userId),
                viewRepository.countByProfileOwnerUserId(userId),
                favoriteRepository.countByUser_Id(userId),
                blockRepository.countByOwner_Id(userId)
        );
    }

    @Transactional
    public void recordProfileView(UUID viewerUserId, UUID profileId) {
        MatrimonyProfileEntity p = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (!"ACTIVE".equals(p.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }
        if (p.getOwner().getId().equals(viewerUserId)) {
            return;
        }
        assertNotBlocked(viewerUserId, p.getOwner().getId());
        User viewer = userRepository.findById(viewerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        viewRepository.save(new MatrimonyProfileView(UUID.randomUUID(), p, viewer, Instant.now()));
    }

    @Transactional
    public MatrimonyDtos.MatrimonyFavoriteToggleResponse toggleFavorite(UUID userId, UUID profileId) {
        MatrimonyProfileEntity p = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (p.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot favorite own profile");
        }
        assertNotBlocked(userId, p.getOwner().getId());
        User u = userRepository.getReferenceById(userId);
        Optional<MatrimonyFavorite> ex = favoriteRepository.findByUser_IdAndProfile_Id(userId, profileId);
        if (ex.isPresent()) {
            favoriteRepository.delete(ex.get());
            return new MatrimonyDtos.MatrimonyFavoriteToggleResponse(false);
        }
        favoriteRepository.save(new MatrimonyFavorite(UUID.randomUUID(), u, p));
        return new MatrimonyDtos.MatrimonyFavoriteToggleResponse(true);
    }

    @Transactional(readOnly = true)
    public List<MatrimonyDtos.MatrimonyProfileCard> listFavorites(UUID userId) {
        return favoriteRepository.findByUser_Id(userId).stream()
                .map(f -> toCard(f.getProfile(), userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listBlockedUserIds(UUID userId) {
        return blockRepository.findByOwner_Id(userId).stream()
                .map(b -> b.getBlocked().getId().toString())
                .toList();
    }

    @Transactional
    public void blockUser(UUID ownerUserId, UUID blockedUserId) {
        if (ownerUserId.equals(blockedUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot block yourself");
        }
        User owner = userRepository.getReferenceById(ownerUserId);
        User blocked = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (blockRepository.existsByOwner_IdAndBlocked_Id(ownerUserId, blockedUserId)) {
            return;
        }
        blockRepository.save(new MatrimonyBlock(UUID.randomUUID(), owner, blocked));
    }

    @Transactional
    public void unblockUser(UUID ownerUserId, UUID blockedUserId) {
        blockRepository.findByOwner_IdAndBlocked_Id(ownerUserId, blockedUserId)
                .ifPresent(blockRepository::delete);
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.PaginatedMatrimonyProfiles searchProfiles(
            UUID viewerId,
            String gender,
            String city,
            Integer minAge,
            Integer maxAge,
            String profession,
            String q,
            int page,
            int size
    ) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);

        String genderVal = blankToNull(gender);
        String cityVal = blankToNull(city);
        String professionVal = blankToNull(profession);
        String qVal = blankToNull(q);
        LocalDate dobStart = minAge != null ? LocalDate.now().minusYears(minAge + 1).plusDays(1) : null;
        LocalDate dobEnd = maxAge != null ? LocalDate.now().minusYears(maxAge) : null;

        StringBuilder jpql = new StringBuilder("""
                select p from MatrimonyProfileEntity p join fetch p.owner
                where p.status = 'ACTIVE'
                  and p.visibleInSearch = true
                  and p.owner.id <> :viewerId
                """);
        StringBuilder countJpql = new StringBuilder("""
                select count(p) from MatrimonyProfileEntity p
                where p.status = 'ACTIVE'
                  and p.visibleInSearch = true
                  and p.owner.id <> :viewerId
                """);

        String blockClause = """
                  and not exists (
                      select b from MatrimonyBlock b
                      where (b.owner.id = :viewerId and b.blocked.id = p.owner.id)
                         or (b.owner.id = p.owner.id and b.blocked.id = :viewerId)
                  )
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("viewerId", viewerId);

        if (genderVal != null) {
            String clause = "  and p.gender = :gender\n";
            jpql.append(clause);
            countJpql.append(clause);
            params.put("gender", genderVal);
        }
        if (cityVal != null) {
            String clause = "  and lower(p.city) like :cityPattern\n";
            jpql.append(clause);
            countJpql.append(clause);
            params.put("cityPattern", "%" + cityVal.toLowerCase() + "%");
        }
        if (professionVal != null) {
            String clause = "  and lower(p.detailJson) like :profPattern\n";
            jpql.append(clause);
            countJpql.append(clause);
            params.put("profPattern", "%" + professionVal.toLowerCase() + "%");
        }
        if (qVal != null) {
            String clause = "  and lower(p.displayName) like :namePattern\n";
            jpql.append(clause);
            countJpql.append(clause);
            params.put("namePattern", "%" + qVal.toLowerCase() + "%");
        }
        if (dobStart != null) {
            String clause = "  and p.dateOfBirth >= :dobStart\n";
            jpql.append(clause);
            countJpql.append(clause);
            params.put("dobStart", dobStart);
        }
        if (dobEnd != null) {
            String clause = "  and p.dateOfBirth <= :dobEnd\n";
            jpql.append(clause);
            countJpql.append(clause);
            params.put("dobEnd", dobEnd);
        }

        jpql.append(blockClause);
        countJpql.append(blockClause);

        TypedQuery<MatrimonyProfileEntity> dataQuery = entityManager.createQuery(jpql.toString(), MatrimonyProfileEntity.class);
        TypedQuery<Long> cntQuery = entityManager.createQuery(countJpql.toString(), Long.class);

        params.forEach((k, v) -> { dataQuery.setParameter(k, v); cntQuery.setParameter(k, v); });

        long total = cntQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) total / s);

        List<MatrimonyProfileEntity> results = dataQuery
                .setFirstResult(p * s)
                .setMaxResults(s)
                .getResultList();

        List<MatrimonyDtos.MatrimonyProfileCard> content = results.stream()
                .map(x -> toCard(x, viewerId))
                .toList();
        return new MatrimonyDtos.PaginatedMatrimonyProfiles(
                content,
                total,
                totalPages,
                p,
                s
        );
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.MatrimonyProfileDetail getProfile(UUID viewerUserId, UUID profileId) {
        MatrimonyProfileEntity p = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (!"ACTIVE".equals(p.getStatus()) && !p.getOwner().getId().equals(viewerUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }
        if (!p.getOwner().getId().equals(viewerUserId)) {
            assertNotBlocked(viewerUserId, p.getOwner().getId());
        }
        return toDetail(p, viewerUserId);
    }

    @Transactional
    public MatrimonyDtos.MatrimonyProfileDetail createProfile(UUID ownerUserId, MatrimonyDtos.MatrimonyCreateProfileRequest req) {
        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (req.displayName() == null || req.displayName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName required");
        }
        if (req.gender() == null || req.gender().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gender required");
        }
        LocalDate dob = parseDob(req.dateOfBirth());
        String subj = req.profileSubject() != null ? req.profileSubject().trim().toUpperCase() : "SELF";
        if (!subj.equals("SELF") && !subj.equals("RELATIVE")) {
            subj = "SELF";
        }
        Instant now = Instant.now();
        MatrimonyProfileEntity p = new MatrimonyProfileEntity();
        p.setId(UUID.randomUUID());
        p.setOwner(owner);
        p.setDisplayName(req.displayName().trim());
        p.setGender(req.gender().trim().toUpperCase());
        p.setDateOfBirth(dob);
        p.setProfileSubject(subj);
        p.setRelativeRelation(trimToNull(req.relativeRelation()));
        p.setHeightCm(req.heightCm());
        p.setCity(trimToNull(req.city()));
        p.setState(trimToNull(req.state()));
        p.setCountry(trimToNull(req.country()));
        p.setStatus("DRAFT");
        p.setVisibleInSearch(true);
        p.setDraftStep(1);
        p.setVerified(false);
        p.setCompletionPercent(10);
        p.setPhotoUrlsJson("[]");
        p.setHobbiesJson("[]");
        p.setDetailJson("{}");
        p.setCreatedAt(now);
        p.setUpdatedAt(now);
        p.setLastActiveAt(now);
        profileRepository.save(p);
        return toDetail(p, ownerUserId);
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public MatrimonyDtos.MatrimonyProfileDetail updateProfile(UUID ownerUserId, UUID profileId, Map<String, Object> body) {
        MatrimonyProfileEntity p = profileRepository.findByIdAndOwner_Id(profileId, ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        Map<String, Object> detail = parseJsonMap(p.getDetailJson());
        applyScalarIfPresent(body, "displayName", v -> p.setDisplayName(String.valueOf(v).trim()));
        applyScalarIfPresent(body, "gender", v -> p.setGender(String.valueOf(v).trim().toUpperCase()));
        applyScalarIfPresent(body, "dateOfBirth", v -> p.setDateOfBirth(parseDob(String.valueOf(v))));
        applyScalarIfPresent(body, "profileSubject", v -> p.setProfileSubject(String.valueOf(v).trim().toUpperCase()));
        applyScalarIfPresent(body, "relativeRelation", v -> p.setRelativeRelation(trimToNull(String.valueOf(v))));
        applyIntIfPresent(body, "heightCm", p::setHeightCm);
        applyIntIfPresent(body, "weightKg", p::setWeightKg);
        applyScalarIfPresent(body, "city", v -> p.setCity(trimToNull(String.valueOf(v))));
        applyScalarIfPresent(body, "state", v -> p.setState(trimToNull(String.valueOf(v))));
        applyScalarIfPresent(body, "country", v -> p.setCountry(trimToNull(String.valueOf(v))));
        applyScalarIfPresent(body, "bio", v -> p.setBio(trimToNull(String.valueOf(v))));
        applyIntIfPresent(body, "draftStep", p::setDraftStep);
        mergeNestedMap(body, "family", detail);
        mergeNestedMap(body, "partnerPreferences", detail);
        mergeNestedMap(body, "privacy", detail);
        if (body.containsKey("privacy") && body.get("privacy") instanceof Map<?, ?> pm) {
            Object vis = ((Map<String, Object>) pm).get("visibleInSearch");
            if (vis instanceof Boolean b) {
                p.setVisibleInSearch(b);
            }
        }
        putDetailScalarIfPresent(body, "maritalStatus", detail);
        putDetailScalarIfPresent(body, "religion", detail);
        putDetailScalarIfPresent(body, "motherTongue", detail);
        putDetailScalarIfPresent(body, "caste", detail);
        putDetailScalarIfPresent(body, "profession", detail);
        putDetailScalarIfPresent(body, "company", detail);
        putDetailScalarIfPresent(body, "education", detail);
        putDetailScalarIfPresent(body, "college", detail);
        putDetailScalarIfPresent(body, "incomeBracket", detail);
        putDetailScalarIfPresent(body, "nativePlace", detail);
        putDetailScalarIfPresent(body, "smoking", detail);
        putDetailScalarIfPresent(body, "drinking", detail);
        putDetailScalarIfPresent(body, "partnerOtherExpectations", detail);
        if (body.get("hobbies") instanceof List<?> l) {
            try {
                p.setHobbiesJson(objectMapper.writeValueAsString(l));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid hobbies");
            }
        }
        if (body.get("photoUrls") instanceof List<?> l) {
            try {
                p.setPhotoUrlsJson(objectMapper.writeValueAsString(l));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid photoUrls");
            }
        }
        try {
            p.setDetailJson(objectMapper.writeValueAsString(detail));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid profile data");
        }
        p.setCompletionPercent(estimateCompletion(p, detail));
        p.setUpdatedAt(Instant.now());
        p.setLastActiveAt(Instant.now());
        profileRepository.save(p);
        return toDetail(p, ownerUserId);
    }

    @Transactional
    public MatrimonyDtos.MatrimonyProfileDetail activateProfile(UUID ownerUserId, UUID profileId) {
        MatrimonyProfileEntity p = profileRepository.findByIdAndOwner_Id(profileId, ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        p.setStatus("ACTIVE");
        p.setDraftStep(Math.max(p.getDraftStep(), 5));
        p.setUpdatedAt(Instant.now());
        profileRepository.save(p);
        return toDetail(p, ownerUserId);
    }

    @Transactional
    public void archiveProfile(UUID ownerUserId, UUID profileId) {
        MatrimonyProfileEntity p = profileRepository.findByIdAndOwner_Id(profileId, ownerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        p.setStatus("ARCHIVED");
        p.setVisibleInSearch(false);
        p.setUpdatedAt(Instant.now());
        profileRepository.save(p);
    }

    @Transactional
    public MatrimonyDtos.MatrimonyInterestResponse sendInterest(UUID userId, MatrimonyDtos.MatrimonySendInterestRequest req) {
        UUID fromId = UUID.fromString(req.fromProfileId());
        UUID toId = UUID.fromString(req.toProfileId());
        MatrimonyProfileEntity from = profileRepository.findByIdAndOwner_Id(fromId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid from profile"));
        MatrimonyProfileEntity to = profileRepository.findById(toId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target profile not found"));
        if (!"ACTIVE".equals(from.getStatus()) || !"ACTIVE".equals(to.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profiles must be active");
        }
        if (from.getOwner().getId().equals(to.getOwner().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot express interest to own profile");
        }
        assertNotBlocked(userId, to.getOwner().getId());
        Instant now = Instant.now();
        Optional<MatrimonyInterest> ex = interestRepository.findByFromProfile_IdAndToProfile_Id(fromId, toId);
        MatrimonyInterest row;
        if (ex.isPresent()) {
            String st = ex.get().getStatus();
            if ("PENDING".equals(st) || "ACCEPTED".equals(st)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Interest already exists");
            }
            row = ex.get();
            row.setStatus("PENDING");
            row.setMessage(trimToNull(req.message()));
            row.setUpdatedAt(now);
        } else {
            row = new MatrimonyInterest();
            row.setId(UUID.randomUUID());
            row.setFromProfile(from);
            row.setToProfile(to);
            row.setMessage(trimToNull(req.message()));
            row.setStatus("PENDING");
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
        }
        interestRepository.save(row);
        notifier.send(
                to.getOwner().getId(),
                "New matrimony interest",
                from.getDisplayName() + " sent you an interest.",
                "MATRIMONY",
                "/matrimony"
        );
        return toInterestResponse(row);
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.PaginatedMatrimonyInterests listInterestsSent(UUID userId, int page, int size) {
        return pageInterests(interestRepository.findSentByUser(userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.PaginatedMatrimonyInterests listInterestsReceived(UUID userId, int page, int size) {
        return pageInterests(interestRepository.findReceivedByUser(userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
    }

    @Transactional
    public MatrimonyDtos.MatrimonyInterestResponse acceptInterest(UUID userId, UUID interestId) {
        MatrimonyInterest i = interestRepository.findById(interestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interest not found"));
        if (!i.getToProfile().getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your interest to accept");
        }
        if (!"PENDING".equals(i.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interest not pending");
        }
        i.setStatus("ACCEPTED");
        i.setUpdatedAt(Instant.now());
        interestRepository.save(i);
        notifier.send(
                i.getFromProfile().getOwner().getId(),
                "Interest accepted",
                i.getToProfile().getDisplayName() + " accepted your interest.",
                "MATRIMONY",
                "/matrimony/chats"
        );
        return toInterestResponse(i);
    }

    @Transactional
    public MatrimonyDtos.MatrimonyInterestResponse rejectInterest(UUID userId, UUID interestId) {
        MatrimonyInterest i = interestRepository.findById(interestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interest not found"));
        if (!i.getToProfile().getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your interest to reject");
        }
        if (!"PENDING".equals(i.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interest not pending");
        }
        i.setStatus("REJECTED");
        i.setUpdatedAt(Instant.now());
        interestRepository.save(i);
        return toInterestResponse(i);
    }

    @Transactional
    public MatrimonyDtos.MatrimonyInterestResponse withdrawInterest(UUID userId, UUID interestId) {
        MatrimonyInterest i = interestRepository.findById(interestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interest not found"));
        if (!i.getFromProfile().getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your interest to withdraw");
        }
        if (!"PENDING".equals(i.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interest not pending");
        }
        i.setStatus("WITHDRAWN");
        i.setUpdatedAt(Instant.now());
        interestRepository.save(i);
        return toInterestResponse(i);
    }

    @Transactional
    public MatrimonyDtos.MatrimonyConversationResponse openConversation(UUID userId, MatrimonyDtos.MatrimonyOpenConversationRequest req) {
        UUID mine = UUID.fromString(req.myProfileId());
        UUID other = UUID.fromString(req.otherProfileId());
        MatrimonyProfileEntity my = profileRepository.findByIdAndOwner_Id(mine, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid profile"));
        MatrimonyProfileEntity ot = profileRepository.findById(other)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (!"ACTIVE".equals(my.getStatus()) || !"ACTIVE".equals(ot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profiles must be active");
        }
        assertNotBlocked(userId, ot.getOwner().getId());
        if (!canMessage(my, ot)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Messaging not allowed yet");
        }
        UUID lo = mine.compareTo(other) < 0 ? mine : other;
        UUID hi = mine.compareTo(other) < 0 ? other : mine;
        MatrimonyConversation c = conversationRepository.findByProfileIdLowerAndProfileIdHigher(lo, hi)
                .orElseGet(() -> {
                    MatrimonyConversation n = new MatrimonyConversation();
                    n.setId(UUID.randomUUID());
                    n.setProfileIdLower(lo);
                    n.setProfileIdHigher(hi);
                    n.setCreatedAt(Instant.now());
                    return conversationRepository.save(n);
                });
        return toConv(c);
    }

    @Transactional(readOnly = true)
    public List<MatrimonyDtos.MatrimonyConversationResponse> listConversations(UUID userId) {
        List<UUID> ids = profileRepository.findByOwner_IdOrderByUpdatedAtDesc(userId).stream()
                .map(MatrimonyProfileEntity::getId)
                .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return conversationRepository.findForProfileIds(ids).stream().map(this::toConv).toList();
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.PaginatedMatrimonyMessages listMessages(UUID userId, UUID conversationId, int page, int size) {
        MatrimonyConversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        assertParticipant(userId, c);
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 200);
        Page<MatrimonyChatMessage> pg = messageRepository.findByConversation_IdOrderByCreatedAtDesc(conversationId, PageRequest.of(p, s));
        List<MatrimonyDtos.MatrimonyChatMessageResponse> content = pg.getContent().stream().map(this::toMsg).toList();
        return new MatrimonyDtos.PaginatedMatrimonyMessages(
                content,
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    @Transactional
    public MatrimonyDtos.MatrimonyChatMessageResponse sendMessage(UUID userId, UUID conversationId, MatrimonyDtos.MatrimonySendMessageRequest req) {
        UUID senderProfileId = UUID.fromString(req.senderProfileId());
        MatrimonyProfileEntity sp = profileRepository.findByIdAndOwner_Id(senderProfileId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid sender profile"));
        MatrimonyConversation c = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!senderProfileId.equals(c.getProfileIdLower()) && !senderProfileId.equals(c.getProfileIdHigher())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sender not in conversation");
        }
        UUID otherId = senderProfileId.equals(c.getProfileIdLower()) ? c.getProfileIdHigher() : c.getProfileIdLower();
        MatrimonyProfileEntity other = profileRepository.findById(otherId).orElseThrow();
        assertParticipant(userId, c);
        assertNotBlocked(userId, other.getOwner().getId());
        if (!canMessage(sp, other)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Messaging not allowed");
        }
        if (req.content() == null || req.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message required");
        }
        User senderUser = userRepository.getReferenceById(userId);
        MatrimonyChatMessage m = new MatrimonyChatMessage();
        m.setId(UUID.randomUUID());
        m.setConversation(c);
        m.setSenderProfileId(senderProfileId);
        m.setSenderUser(senderUser);
        m.setContent(req.content().trim());
        m.setCreatedAt(Instant.now());
        messageRepository.save(m);
        notifier.send(
                other.getOwner().getId(),
                "New matrimony message",
                sp.getDisplayName() + ": " + truncate(req.content().trim(), 120),
                "MATRIMONY",
                "/matrimony/chats/" + conversationId
        );
        return toMsg(m);
    }

    /**
     * External chat bridge (push provider). Secured via {@code X-Matrimony-Webhook-Secret}.
     */
    @Transactional
    public MatrimonyDtos.MatrimonyChatMessageResponse ingestWebhookMessage(
            String secretConfigured,
            String secretHeader,
            MatrimonyDtos.MatrimonyChatWebhookRequest body
    ) {
        if (secretConfigured == null || secretConfigured.isBlank() || !secretConfigured.equals(secretHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook secret");
        }
        if (body == null || body.conversationId() == null || body.senderProfileId() == null || body.content() == null || body.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload");
        }
        UUID convId = UUID.fromString(body.conversationId());
        UUID senderProfileId = UUID.fromString(body.senderProfileId());
        MatrimonyConversation c = conversationRepository.findById(convId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        if (!senderProfileId.equals(c.getProfileIdLower()) && !senderProfileId.equals(c.getProfileIdHigher())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "senderProfileId not in conversation");
        }
        MatrimonyProfileEntity sp = profileRepository.findById(senderProfileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender profile not found"));
        User senderUser = sp.getOwner();
        MatrimonyChatMessage m = new MatrimonyChatMessage();
        m.setId(UUID.randomUUID());
        m.setConversation(c);
        m.setSenderProfileId(senderProfileId);
        m.setSenderUser(senderUser);
        m.setContent(body.content().trim());
        m.setCreatedAt(Instant.now());
        messageRepository.save(m);
        UUID otherId = senderProfileId.equals(c.getProfileIdLower()) ? c.getProfileIdHigher() : c.getProfileIdLower();
        MatrimonyProfileEntity other = profileRepository.findById(otherId).orElseThrow();
        notifier.send(
                other.getOwner().getId(),
                "New matrimony message",
                sp.getDisplayName() + ": " + truncate(body.content().trim(), 120),
                "MATRIMONY",
                "/matrimony/chats/" + convId
        );
        return toMsg(m);
    }

    private void assertParticipant(UUID userId, MatrimonyConversation c) {
        boolean ok = profileRepository.findByOwner_IdOrderByUpdatedAtDesc(userId).stream()
                .anyMatch(p -> p.getId().equals(c.getProfileIdLower()) || p.getId().equals(c.getProfileIdHigher()));
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant");
        }
    }

    private boolean canMessage(MatrimonyProfileEntity a, MatrimonyProfileEntity b) {
        return messagePolicyAllows(a, b) && messagePolicyAllows(b, a);
    }

    private boolean messagePolicyAllows(MatrimonyProfileEntity perspective, MatrimonyProfileEntity other) {
        Map<String, Object> detail = parseJsonMap(perspective.getDetailJson());
        Map<String, Object> priv = castMap(detail.getOrDefault("privacy", defaultPrivacyMap()));
        String policy = String.valueOf(priv.getOrDefault("messagePolicy", "ACCEPTED_ONLY"));
        if ("ALL_ACTIVE".equalsIgnoreCase(policy)) {
            return true;
        }
        return interestRepository.existsBetweenProfilesWithStatusIn(perspective.getId(), other.getId(), List.of("ACCEPTED"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object o) {
        if (o instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return new HashMap<>();
    }

    private void assertNotBlocked(UUID viewerId, UUID profileOwnerId) {
        if (blockRepository.existsByOwner_IdAndBlocked_Id(viewerId, profileOwnerId)
                || blockRepository.existsByOwner_IdAndBlocked_Id(profileOwnerId, viewerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }
    }

    private MatrimonyDtos.PaginatedMatrimonyInterests pageInterests(Page<MatrimonyInterest> pg) {
        List<MatrimonyDtos.MatrimonyInterestResponse> content = pg.getContent().stream().map(this::toInterestResponse).toList();
        return new MatrimonyDtos.PaginatedMatrimonyInterests(
                content,
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    private MatrimonyDtos.MatrimonyInterestResponse toInterestResponse(MatrimonyInterest i) {
        return new MatrimonyDtos.MatrimonyInterestResponse(
                i.getId().toString(),
                i.getFromProfile().getId().toString(),
                i.getToProfile().getId().toString(),
                i.getMessage(),
                i.getStatus(),
                i.getCreatedAt() != null ? i.getCreatedAt().toString() : null,
                i.getUpdatedAt() != null ? i.getUpdatedAt().toString() : null
        );
    }

    private MatrimonyDtos.MatrimonyConversationResponse toConv(MatrimonyConversation c) {
        return new MatrimonyDtos.MatrimonyConversationResponse(
                c.getId().toString(),
                c.getProfileIdLower().toString(),
                c.getProfileIdHigher().toString(),
                c.getCreatedAt() != null ? c.getCreatedAt().toString() : null
        );
    }

    private MatrimonyDtos.MatrimonyChatMessageResponse toMsg(MatrimonyChatMessage m) {
        return new MatrimonyDtos.MatrimonyChatMessageResponse(
                m.getId().toString(),
                m.getConversation().getId().toString(),
                m.getSenderProfileId().toString(),
                m.getSenderUser().getId().toString(),
                m.getContent(),
                m.getCreatedAt() != null ? m.getCreatedAt().toString() : null,
                m.getReadAt() != null ? m.getReadAt().toString() : null
        );
    }

    private MatrimonyDtos.MatrimonyProfileCard toCard(MatrimonyProfileEntity p, UUID viewerUserId) {
        Map<String, Object> detail = parseJsonMap(p.getDetailJson());
        String profession = str(detail.get("profession"));
        String education = str(detail.get("education"));
        List<String> photos = readStringList(p.getPhotoUrlsJson());
        boolean fav = favoriteRepository.findByUser_IdAndProfile_Id(viewerUserId, p.getId()).isPresent();
        return new MatrimonyDtos.MatrimonyProfileCard(
                p.getId().toString(),
                p.getDisplayName(),
                age(p.getDateOfBirth()),
                p.getGender(),
                profession,
                education,
                p.getCity(),
                p.getHeightCm(),
                photos.isEmpty() ? null : photos.get(0),
                bioShort(p.getBio()),
                p.isVerified(),
                fav
        );
    }

    private MatrimonyDtos.MatrimonyProfileDetail toDetail(MatrimonyProfileEntity p, UUID viewerUserId) {
        Map<String, Object> detail = parseJsonMap(p.getDetailJson());
        Map<String, Object> family = castMap(detail.getOrDefault("family", Map.of()));
        Map<String, Object> partnerPrefs = castMap(detail.getOrDefault("partnerPreferences", Map.of()));
        Map<String, Object> privacy = castMap(detail.getOrDefault("privacy", defaultPrivacyMap()));
        List<String> photos = readStringList(p.getPhotoUrlsJson());
        boolean owner = p.getOwner().getId().equals(viewerUserId);
        boolean acceptedAny = profileRepository.findByOwner_IdOrderByUpdatedAtDesc(viewerUserId).stream()
                .anyMatch(vp -> interestRepository.existsBetweenProfilesWithStatusIn(vp.getId(), p.getId(), List.of("ACCEPTED")));
        boolean limited = !owner && "AFTER_ACCEPTANCE".equalsIgnoreCase(String.valueOf(privacy.getOrDefault("photoVisibility", "ALL"))) && !acceptedAny;
        List<String> photosOut = photos;
        if (limited && !photos.isEmpty()) {
            int idx = 0;
            if (privacy.get("primaryPhotoIndex") instanceof Number n) {
                idx = Math.max(0, Math.min(n.intValue(), photos.size() - 1));
            }
            photosOut = List.of(photos.get(idx));
        }
        return new MatrimonyDtos.MatrimonyProfileDetail(
                p.getId().toString(),
                p.getOwner().getId().toString(),
                p.getProfileSubject(),
                p.getRelativeRelation(),
                p.getDisplayName(),
                age(p.getDateOfBirth()),
                p.getGender(),
                p.getDateOfBirth().toString(),
                p.getHeightCm(),
                p.getWeightKg(),
                str(detail.get("maritalStatus")),
                str(detail.get("religion")),
                str(detail.get("motherTongue")),
                str(detail.get("caste")),
                str(detail.get("profession")),
                str(detail.get("company")),
                str(detail.get("education")),
                str(detail.get("college")),
                str(detail.get("incomeBracket")),
                p.getCity(),
                p.getState(),
                p.getCountry(),
                str(detail.get("nativePlace")),
                p.getBio(),
                readStringList(p.getHobbiesJson()),
                str(detail.get("smoking")),
                str(detail.get("drinking")),
                photosOut,
                limited,
                family,
                partnerPrefs,
                str(detail.get("partnerOtherExpectations")),
                privacy,
                p.getStatus(),
                p.getDraftStep(),
                p.isVerified(),
                p.getCompletionPercent(),
                p.getCreatedAt() != null ? p.getCreatedAt().toString() : null,
                p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null,
                p.getLastActiveAt() != null ? p.getLastActiveAt().toString() : null
        );
    }

    private Map<String, Object> defaultPrivacyMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("visibleInSearch", true);
        m.put("photoVisibility", "ALL");
        m.put("showContactDetails", false);
        m.put("hideLastSeen", false);
        m.put("messagePolicy", "ACCEPTED_ONLY");
        m.put("primaryPhotoIndex", 0);
        return m;
    }

    private int estimateCompletion(MatrimonyProfileEntity p, Map<String, Object> detail) {
        int score = 0;
        if (p.getDisplayName() != null && !p.getDisplayName().isBlank()) score += 5;
        if (p.getBio() != null && !p.getBio().isBlank()) score += 10;
        if (p.getCity() != null && !p.getCity().isBlank()) score += 5;
        if (p.getHeightCm() != null) score += 5;
        if (detail.get("profession") != null && !String.valueOf(detail.get("profession")).isBlank()) score += 10;
        if (detail.get("education") != null && !String.valueOf(detail.get("education")).isBlank()) score += 10;
        List<String> ph = readStringList(p.getPhotoUrlsJson());
        if (!ph.isEmpty()) score += 15;
        if (!readStringList(p.getHobbiesJson()).isEmpty()) score += 10;
        return Math.min(100, score + p.getDraftStep() * 5);
    }

    private Map<String, Object> parseJsonMap(String json) {
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

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> l = objectMapper.readValue(json, new TypeReference<>() {
            });
            return l == null ? List.of() : new ArrayList<>(l);
        } catch (Exception e) {
            return List.of();
        }
    }

    private void mergeNestedMap(Map<String, Object> body, String key, Map<String, Object> detailRoot) {
        Object raw = body.get(key);
        if (!(raw instanceof Map<?, ?> incoming)) {
            return;
        }
        Map<String, Object> target = castMap(detailRoot.computeIfAbsent(key, k -> new HashMap<>()));
        for (Map.Entry<?, ?> e : incoming.entrySet()) {
            if (e.getKey() != null) {
                target.put(String.valueOf(e.getKey()), e.getValue());
            }
        }
    }

    private void putDetailScalarIfPresent(Map<String, Object> body, String key, Map<String, Object> detail) {
        if (!body.containsKey(key) || body.get(key) == null) {
            return;
        }
        detail.put(key, body.get(key));
    }

    private void applyScalarIfPresent(Map<String, Object> body, String key, java.util.function.Consumer<String> consumer) {
        if (!body.containsKey(key) || body.get(key) == null) {
            return;
        }
        consumer.accept(String.valueOf(body.get(key)));
    }

    private void applyIntIfPresent(Map<String, Object> body, String key, java.util.function.Consumer<Integer> consumer) {
        if (!body.containsKey(key) || body.get(key) == null) {
            return;
        }
        Object v = body.get(key);
        if (v instanceof Number n) {
            consumer.accept(n.intValue());
        } else {
            try {
                consumer.accept(Integer.parseInt(String.valueOf(v)));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static LocalDate parseDob(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dateOfBirth required");
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dateOfBirth");
        }
    }

    private static int age(LocalDate dob) {
        return Period.between(dob, LocalDate.now(ZoneOffset.UTC)).getYears();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String bioShort(String bio) {
        if (bio == null || bio.isBlank()) {
            return "";
        }
        String t = bio.replace('\n', ' ').trim();
        return t.length() <= 160 ? t : t.substring(0, 159) + "\u2026";
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + "\u2026";
    }

    // ==================== ADMIN METHODS ====================

    @Transactional(readOnly = true)
    public MatrimonyDtos.PaginatedAdminMatrimonyProfiles adminListProfiles(
            String q,
            String status,
            String gender,
            Integer minAge,
            Integer maxAge,
            String city,
            Boolean verified,
            Boolean visibleInSearch,
            int page,
            int size
    ) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);

        StringBuilder whereClause = new StringBuilder("where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (blankToNull(q) != null) {
            whereClause.append("and lower(p.displayName) like :q ");
            params.put("q", "%" + q.toLowerCase() + "%");
        }
        if (blankToNull(status) != null) {
            whereClause.append("and p.status = :status ");
            params.put("status", status.trim().toUpperCase());
        }
        if (blankToNull(gender) != null) {
            whereClause.append("and p.gender = :gender ");
            params.put("gender", gender.trim().toUpperCase());
        }
        if (minAge != null) {
            LocalDate dobStart = LocalDate.now().minusYears(minAge + 1).plusDays(1);
            whereClause.append("and p.dateOfBirth >= :dobStart ");
            params.put("dobStart", dobStart);
        }
        if (maxAge != null) {
            LocalDate dobEnd = LocalDate.now().minusYears(maxAge);
            whereClause.append("and p.dateOfBirth <= :dobEnd ");
            params.put("dobEnd", dobEnd);
        }
        if (blankToNull(city) != null) {
            whereClause.append("and lower(p.city) like :city ");
            params.put("city", "%" + city.toLowerCase() + "%");
        }
        if (verified != null) {
            whereClause.append("and p.verified = :verified ");
            params.put("verified", verified);
        }
        if (visibleInSearch != null) {
            whereClause.append("and p.visibleInSearch = :visible ");
            params.put("visible", visibleInSearch);
        }

        String dataJpql = "select p from MatrimonyProfileEntity p join fetch p.owner " + whereClause + "order by p.createdAt desc";
        String countJpql = "select count(p) from MatrimonyProfileEntity p " + whereClause;

        TypedQuery<MatrimonyProfileEntity> dataQuery = entityManager.createQuery(dataJpql, MatrimonyProfileEntity.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        params.forEach((k, v) -> {
            dataQuery.setParameter(k, v);
            countQuery.setParameter(k, v);
        });

        long total = countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) total / s);

        List<MatrimonyProfileEntity> results = dataQuery
                .setFirstResult(p * s)
                .setMaxResults(s)
                .getResultList();

        List<MatrimonyDtos.AdminMatrimonyProfileResponse> content = results.stream()
                .map(this::toAdminResponse)
                .toList();

        return new MatrimonyDtos.PaginatedAdminMatrimonyProfiles(content, total, totalPages, p, s);
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.AdminMatrimonyProfileDetailResponse adminGetProfile(UUID profileId) {
        MatrimonyProfileEntity p = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        return toAdminDetail(p);
    }

    @Transactional
    public MatrimonyDtos.AdminMatrimonyProfileResponse adminVerifyProfile(UUID profileId) {
        MatrimonyProfileEntity p = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        p.setVerified(!p.isVerified());
        p.setUpdatedAt(Instant.now());
        profileRepository.save(p);
        return toAdminResponse(p);
    }

    @Transactional
    public MatrimonyDtos.AdminMatrimonyProfileResponse adminToggleVisibility(UUID profileId) {
        MatrimonyProfileEntity p = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        p.setVisibleInSearch(!p.isVisibleInSearch());
        p.setUpdatedAt(Instant.now());
        profileRepository.save(p);
        return toAdminResponse(p);
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.AdminMatrimonyAnalyticsResponse adminGetAnalytics() {
        long total = profileRepository.count();
        long active = profileRepository.countByStatus("ACTIVE");
        long draft = profileRepository.countByStatus("DRAFT");
        long paused = profileRepository.countByStatus("PAUSED");
        long verified = profileRepository.countByVerified(true);
        long hidden = profileRepository.countByVisibleInSearch(false);
        long interests = interestRepository.count();
        long conversations = conversationRepository.count();
        long blocks = blockRepository.count();

        double verificationRate = total > 0 ? (double) verified / total * 100 : 0.0;

        return new MatrimonyDtos.AdminMatrimonyAnalyticsResponse(
                total,
                active,
                draft,
                paused,
                verified,
                verificationRate,
                hidden,
                interests,
                conversations,
                blocks
        );
    }

    private MatrimonyDtos.AdminMatrimonyProfileResponse toAdminResponse(MatrimonyProfileEntity p) {
        List<String> photos = readStringList(p.getPhotoUrlsJson());
        return new MatrimonyDtos.AdminMatrimonyProfileResponse(
                p.getId().toString(),
                p.getOwner().getEmail(),
                p.getOwner().getId().toString(),
                p.getDisplayName(),
                p.getGender(),
                age(p.getDateOfBirth()),
                p.getCity(),
                p.getState(),
                p.getStatus(),
                p.isVerified(),
                p.isVisibleInSearch(),
                p.getCompletionPercent(),
                photos.size(),
                p.getCreatedAt() != null ? p.getCreatedAt().toString() : null,
                p.getLastActiveAt() != null ? p.getLastActiveAt().toString() : null
        );
    }

    private MatrimonyDtos.AdminMatrimonyProfileDetailResponse toAdminDetail(MatrimonyProfileEntity p) {
        List<String> hobbies = readStringList(p.getHobbiesJson());
        List<String> photos = readStringList(p.getPhotoUrlsJson());
        return new MatrimonyDtos.AdminMatrimonyProfileDetailResponse(
                p.getId().toString(),
                p.getOwner().getEmail(),
                p.getOwner().getId().toString(),
                p.getDisplayName(),
                p.getGender(),
                age(p.getDateOfBirth()),
                p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : null,
                p.getHeightCm(),
                p.getWeightKg(),
                p.getProfileSubject(),
                p.getRelativeRelation(),
                p.getCity(),
                p.getState(),
                p.getCountry(),
                p.getBio(),
                hobbies,
                photos,
                p.isVerified(),
                p.isVisibleInSearch(),
                p.getCompletionPercent(),
                p.getStatus(),
                p.getCreatedAt() != null ? p.getCreatedAt().toString() : null,
                p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null,
                p.getLastActiveAt() != null ? p.getLastActiveAt().toString() : null
        );
    }

    // ==================== ADMIN SAFETY METHODS ====================

    @Transactional(readOnly = true)
    public MatrimonyDtos.PaginatedAdminMatrimonyInterests adminListInterests(
            String q,
            String status,
            int page,
            int size
    ) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);

        StringBuilder whereClause = new StringBuilder("where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (blankToNull(q) != null) {
            whereClause.append("and (lower(i.fromProfile.displayName) like :q or lower(i.toProfile.displayName) like :q) ");
            params.put("q", "%" + q.toLowerCase() + "%");
        }
        if (blankToNull(status) != null) {
            whereClause.append("and i.status = :status ");
            params.put("status", status.trim().toUpperCase());
        }

        String dataJpql = "select i from MatrimonyInterest i join fetch i.fromProfile join fetch i.fromProfile.owner join fetch i.toProfile join fetch i.toProfile.owner " + whereClause + "order by i.createdAt desc";
        String countJpql = "select count(i) from MatrimonyInterest i " + whereClause;

        TypedQuery<MatrimonyInterest> dataQuery = entityManager.createQuery(dataJpql, MatrimonyInterest.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        params.forEach((k, v) -> {
            dataQuery.setParameter(k, v);
            countQuery.setParameter(k, v);
        });

        long total = countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) total / s);

        List<MatrimonyInterest> results = dataQuery
                .setFirstResult(p * s)
                .setMaxResults(s)
                .getResultList();

        List<MatrimonyDtos.AdminMatrimonyInterestResponse> content = results.stream()
                .map(this::toAdminInterestResponse)
                .toList();

        return new MatrimonyDtos.PaginatedAdminMatrimonyInterests(content, total, totalPages, p, s);
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.PaginatedAdminMatrimonyBlocks adminListBlocks(
            String q,
            int page,
            int size
    ) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);

        StringBuilder whereClause = new StringBuilder("where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (blankToNull(q) != null) {
            whereClause.append("and (lower(b.owner.email) like :q or lower(b.blocked.email) like :q) ");
            params.put("q", "%" + q.toLowerCase() + "%");
        }

        String dataJpql = "select b from MatrimonyBlock b join fetch b.owner join fetch b.blocked " + whereClause + "order by b.id desc";
        String countJpql = "select count(b) from MatrimonyBlock b " + whereClause;

        TypedQuery<MatrimonyBlock> dataQuery = entityManager.createQuery(dataJpql, MatrimonyBlock.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        params.forEach((k, v) -> {
            dataQuery.setParameter(k, v);
            countQuery.setParameter(k, v);
        });

        long total = countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) total / s);

        List<MatrimonyBlock> results = dataQuery
                .setFirstResult(p * s)
                .setMaxResults(s)
                .getResultList();

        List<MatrimonyDtos.AdminMatrimonyBlockResponse> content = results.stream()
                .map(this::toAdminBlockResponse)
                .toList();

        return new MatrimonyDtos.PaginatedAdminMatrimonyBlocks(content, total, totalPages, p, s);
    }

    @Transactional
    public MatrimonyDtos.AdminMatrimonyBlockResponse adminForceBlockUser(UUID blockingUserId, UUID blockedUserId) {
        if (blockingUserId.equals(blockedUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot block yourself");
        }
        User blocker = userRepository.findById(blockingUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blocking user not found"));
        User blocked = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blocked user not found"));

        Optional<MatrimonyBlock> existing = blockRepository.findByOwner_IdAndBlocked_Id(blockingUserId, blockedUserId);
        if (existing.isPresent()) {
            return toAdminBlockResponse(existing.get());
        }

        MatrimonyBlock block = new MatrimonyBlock(UUID.randomUUID(), blocker, blocked);
        blockRepository.save(block);
        return toAdminBlockResponse(block);
    }

    @Transactional
    public void adminUnblockUser(UUID blockingUserId, UUID blockedUserId) {
        blockRepository.findByOwner_IdAndBlocked_Id(blockingUserId, blockedUserId)
                .ifPresent(blockRepository::delete);
    }

    private MatrimonyDtos.AdminMatrimonyInterestResponse toAdminInterestResponse(MatrimonyInterest i) {
        return new MatrimonyDtos.AdminMatrimonyInterestResponse(
                i.getId().toString(),
                i.getFromProfile().getId().toString(),
                i.getFromProfile().getDisplayName(),
                i.getFromProfile().getOwner().getEmail(),
                i.getToProfile().getId().toString(),
                i.getToProfile().getDisplayName(),
                i.getToProfile().getOwner().getEmail(),
                i.getMessage(),
                i.getStatus(),
                i.getCreatedAt() != null ? i.getCreatedAt().toString() : null
        );
    }

    private MatrimonyDtos.AdminMatrimonyBlockResponse toAdminBlockResponse(MatrimonyBlock b) {
        return new MatrimonyDtos.AdminMatrimonyBlockResponse(
                b.getId().toString(),
                b.getOwner().getEmail(),
                b.getOwner().getId().toString(),
                b.getBlocked().getEmail(),
                b.getBlocked().getId().toString(),
                b.getId().toString()
        );
    }

    // ==================== ADMIN CONTENT MODERATION ====================

    @Transactional(readOnly = true)
    public MatrimonyDtos.PaginatedAdminMatrimonyPhotos adminListPhotos(
            String q,
            Boolean flagged,
            int page,
            int size
    ) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);

        StringBuilder whereClause = new StringBuilder("where 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (blankToNull(q) != null) {
            whereClause.append("and lower(p.displayName) like :q ");
            params.put("q", "%" + q.toLowerCase() + "%");
        }

        String dataJpql = "select p from MatrimonyProfileEntity p join fetch p.owner " + whereClause + "order by p.createdAt desc";
        String countJpql = "select count(p) from MatrimonyProfileEntity p " + whereClause;

        TypedQuery<MatrimonyProfileEntity> dataQuery = entityManager.createQuery(dataJpql, MatrimonyProfileEntity.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        params.forEach((k, v) -> {
            dataQuery.setParameter(k, v);
            countQuery.setParameter(k, v);
        });

        long total = countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) total / s);

        List<MatrimonyProfileEntity> results = dataQuery
                .setFirstResult(p * s)
                .setMaxResults(s)
                .getResultList();

        List<MatrimonyDtos.AdminMatrimonyPhotoReviewDto> content = new ArrayList<>();
        for (MatrimonyProfileEntity profile : results) {
            List<String> photos = readStringList(profile.getPhotoUrlsJson());
            for (String photoUrl : photos) {
                content.add(new MatrimonyDtos.AdminMatrimonyPhotoReviewDto(
                        profile.getId().toString(),
                        profile.getDisplayName(),
                        profile.getOwner().getEmail(),
                        photoUrl,
                        false,
                        profile.getCreatedAt() != null ? profile.getCreatedAt().toString() : null
                ));
            }
        }

        return new MatrimonyDtos.PaginatedAdminMatrimonyPhotos(
                content.stream().skip((long) p * s).limit(s).toList(),
                content.size(),
                totalPages,
                p,
                s
        );
    }

    @Transactional(readOnly = true)
    public MatrimonyDtos.PaginatedAdminMatrimonyBios adminListBios(
            String q,
            Boolean flagged,
            int page,
            int size
    ) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);

        StringBuilder whereClause = new StringBuilder("where p.bio is not null and p.bio <> '' ");
        Map<String, Object> params = new HashMap<>();

        if (blankToNull(q) != null) {
            whereClause.append("and lower(p.displayName) like :q ");
            params.put("q", "%" + q.toLowerCase() + "%");
        }

        String dataJpql = "select p from MatrimonyProfileEntity p join fetch p.owner " + whereClause + "order by p.createdAt desc";
        String countJpql = "select count(p) from MatrimonyProfileEntity p " + whereClause;

        TypedQuery<MatrimonyProfileEntity> dataQuery = entityManager.createQuery(dataJpql, MatrimonyProfileEntity.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        params.forEach((k, v) -> {
            dataQuery.setParameter(k, v);
            countQuery.setParameter(k, v);
        });

        long total = countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) total / s);

        List<MatrimonyProfileEntity> results = dataQuery
                .setFirstResult(p * s)
                .setMaxResults(s)
                .getResultList();

        List<MatrimonyDtos.AdminMatrimonyBioReviewDto> content = results.stream()
                .map(profile -> new MatrimonyDtos.AdminMatrimonyBioReviewDto(
                        profile.getId().toString(),
                        profile.getDisplayName(),
                        profile.getOwner().getEmail(),
                        profile.getBio(),
                        false,
                        profile.getCreatedAt() != null ? profile.getCreatedAt().toString() : null
                ))
                .toList();

        return new MatrimonyDtos.PaginatedAdminMatrimonyBios(content, total, totalPages, p, s);
    }
}
