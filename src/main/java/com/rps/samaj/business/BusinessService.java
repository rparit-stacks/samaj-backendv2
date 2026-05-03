package com.rps.samaj.business;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.BusinessDtos;
import com.rps.samaj.config.cache.RedisCacheConfig;
import com.rps.samaj.notification.AppNotification;
import com.rps.samaj.notification.NotificationBatchWriter;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BusinessService {

    private static final int MAX_PHOTOS = 10;

    private final BusinessListingRepository businessRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final NotificationBatchWriter notificationBatchWriter;
    private final ObjectMapper objectMapper;

    public BusinessService(
            BusinessListingRepository businessRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            NotificationBatchWriter notificationBatchWriter,
            ObjectMapper objectMapper
    ) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.notificationBatchWriter = notificationBatchWriter;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.Names.BUSINESS_LISTINGS, allEntries = true)
    public BusinessDtos.BusinessDetail create(UUID userId, BusinessDtos.BusinessCreateRequest req) {
        User user = requireUser(userId);
        BusinessListing listing = new BusinessListing(UUID.randomUUID(), user, req.name().trim());
        applyFields(listing, req.name(), req.description(), req.category(), req.phone(),
                req.email(), req.address(), req.city(), req.website(), req.photos());
        businessRepository.save(listing);
        return toDetail(listing, userId);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.BUSINESS_LISTINGS,
            RedisCacheConfig.Names.BUSINESS_DETAIL
    }, allEntries = true)
    public BusinessDtos.BusinessDetail update(UUID userId, UUID listingId, BusinessDtos.BusinessUpdateRequest req) {
        BusinessListing listing = requireListing(listingId);
        if (!listing.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your listing");
        }
        if (listing.getStatus() == BusinessStatus.BANNED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Banned listings cannot be edited");
        }
        listing.setStatus(BusinessStatus.PENDING);
        listing.setRejectionReason(null);
        applyFields(listing, req.name(), req.description(), req.category(), req.phone(),
                req.email(), req.address(), req.city(), req.website(), req.photos());
        listing.setUpdatedAt(Instant.now());
        businessRepository.save(listing);
        return toDetail(listing, userId);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.BUSINESS_LISTINGS,
            RedisCacheConfig.Names.BUSINESS_DETAIL
    }, allEntries = true)
    public void delete(UUID userId, UUID listingId) {
        BusinessListing listing = requireListing(listingId);
        if (!listing.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your listing");
        }
        businessRepository.delete(listing);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.BUSINESS_LISTINGS,
            key = "T(String).valueOf(#page) + ':' + T(String).valueOf(#size) + ':' + (#category != null ? #category : '')")
    public BusinessDtos.BusinessPageResponse listApproved(String category, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<BusinessListing> pg;
        if (category != null && !category.isBlank()) {
            pg = businessRepository.findByStatusAndCategoryIgnoreCaseOrderByFeaturedDescCreatedAtDesc(
                    BusinessStatus.APPROVED, category.trim(), p);
        } else {
            pg = businessRepository.findByStatusOrderByFeaturedDescCreatedAtDesc(BusinessStatus.APPROVED, p);
        }
        return toPage(pg);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.BUSINESS_DETAIL, key = "#listingId.toString()")
    public BusinessDtos.BusinessDetail getApproved(UUID listingId) {
        BusinessListing listing = requireListing(listingId);
        if (listing.getStatus() != BusinessStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }
        return toDetail(listing, null);
    }

    @Transactional
    public BusinessDtos.BusinessDetail getApprovedAndTrack(UUID listingId) {
        BusinessListing listing = requireListing(listingId);
        if (listing.getStatus() != BusinessStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }
        listing.setViewCount(listing.getViewCount() + 1);
        businessRepository.save(listing);
        return toDetail(listing, null);
    }

    @Transactional(readOnly = true)
    public BusinessDtos.BusinessDetail getMine(UUID userId, UUID listingId) {
        BusinessListing listing = requireListing(listingId);
        if (!listing.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your listing");
        }
        return toDetail(listing, userId);
    }

    @Transactional(readOnly = true)
    public BusinessDtos.BusinessPageResponse listMine(UUID userId, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<BusinessListing> pg = businessRepository.findByUser_IdOrderByCreatedAtDesc(userId, p);
        return toPage(pg);
    }

    // ---- Admin ----

    @Transactional(readOnly = true)
    public BusinessDtos.BusinessAdminPageResponse adminList(String statusFilter, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        Page<BusinessListing> pg;
        if (statusFilter == null || statusFilter.isBlank()) {
            pg = businessRepository.findAllByOrderByCreatedAtDesc(p);
        } else {
            BusinessStatus st = parseStatus(statusFilter);
            pg = businessRepository.findByStatusOrderByCreatedAtDesc(st, p);
        }
        List<BusinessDtos.BusinessAdminSummary> content = pg.getContent().stream().map(this::toAdminSummary).toList();
        return new BusinessDtos.BusinessAdminPageResponse(content, pg.getTotalElements(), pg.getTotalPages(), pg.getNumber(), pg.getSize());
    }

    @Transactional(readOnly = true)
    public BusinessDtos.BusinessDetail adminGet(UUID listingId) {
        BusinessListing listing = requireListing(listingId);
        return toDetail(listing, listing.getUser().getId());
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.BUSINESS_LISTINGS,
            RedisCacheConfig.Names.BUSINESS_DETAIL
    }, allEntries = true)
    public BusinessDtos.BusinessDetail adminApprove(UUID listingId, BusinessDtos.AdminApproveRequest req) {
        BusinessListing listing = requireListing(listingId);
        listing.setStatus(BusinessStatus.APPROVED);
        listing.setRejectionReason(null);
        if (req != null && req.featured() != null) {
            listing.setFeatured(req.featured());
        }
        listing.setUpdatedAt(Instant.now());
        businessRepository.save(listing);
        sendNotification(listing.getUser(), "Business Listing Approved",
                "Your business \"" + listing.getName() + "\" has been approved and is now live.",
                "BUSINESS_APPROVED", "/business/" + listing.getId());
        return toDetail(listing, listing.getUser().getId());
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.BUSINESS_LISTINGS,
            RedisCacheConfig.Names.BUSINESS_DETAIL
    }, allEntries = true)
    public BusinessDtos.BusinessDetail adminReject(UUID listingId, BusinessDtos.AdminRejectRequest req) {
        BusinessListing listing = requireListing(listingId);
        listing.setStatus(BusinessStatus.REJECTED);
        listing.setRejectionReason(req.reason().trim());
        listing.setUpdatedAt(Instant.now());
        businessRepository.save(listing);
        sendNotification(listing.getUser(), "Business Listing Rejected",
                "Your business \"" + listing.getName() + "\" was not approved. Reason: " + req.reason().trim(),
                "BUSINESS_REJECTED", "/business/my");
        return toDetail(listing, listing.getUser().getId());
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.BUSINESS_LISTINGS,
            RedisCacheConfig.Names.BUSINESS_DETAIL
    }, allEntries = true)
    public BusinessDtos.BusinessDetail adminBan(UUID listingId) {
        BusinessListing listing = requireListing(listingId);
        listing.setStatus(BusinessStatus.BANNED);
        listing.setUpdatedAt(Instant.now());
        businessRepository.save(listing);
        sendNotification(listing.getUser(), "Business Listing Suspended",
                "Your business \"" + listing.getName() + "\" has been suspended due to policy violations.",
                "BUSINESS_BANNED", "/business/my");
        return toDetail(listing, listing.getUser().getId());
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.BUSINESS_LISTINGS,
            RedisCacheConfig.Names.BUSINESS_DETAIL
    }, allEntries = true)
    public void adminDelete(UUID listingId) {
        if (!businessRepository.existsById(listingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business listing not found");
        }
        businessRepository.deleteById(listingId);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.BUSINESS_LISTINGS,
            RedisCacheConfig.Names.BUSINESS_DETAIL
    }, allEntries = true)
    public BusinessDtos.BusinessDetail adminToggleFeatured(UUID listingId) {
        BusinessListing listing = requireListing(listingId);
        listing.setFeatured(!listing.isFeatured());
        listing.setUpdatedAt(Instant.now());
        businessRepository.save(listing);
        return toDetail(listing, listing.getUser().getId());
    }

    // ---- Private helpers ----

    private void applyFields(BusinessListing listing, String name, String description, String category,
                             String phone, String email, String address, String city,
                             String website, List<String> photos) {
        listing.setName(name != null ? name.trim() : "");
        listing.setDescription(description != null ? description.trim() : null);
        listing.setCategory(category != null ? category.trim() : null);
        listing.setPhone(phone != null ? phone.trim() : null);
        listing.setEmail(email != null ? email.trim() : null);
        listing.setAddress(address != null ? address.trim() : null);
        listing.setCity(city != null ? city.trim() : null);
        listing.setWebsite(website != null ? website.trim() : null);
        listing.setPhotosJson(serializePhotos(photos));
    }

    private String serializePhotos(List<String> photos) {
        if (photos == null || photos.isEmpty()) return "[]";
        List<String> trimmed = photos.stream()
                .filter(p -> p != null && !p.isBlank())
                .limit(MAX_PHOTOS)
                .toList();
        try {
            return objectMapper.writeValueAsString(trimmed);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parsePhotos(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private void sendNotification(User user, String title, String body, String type, String link) {
        try {
            AppNotification n = new AppNotification(UUID.randomUUID(), user, title, body, type);
            n.setLink(link);
            notificationBatchWriter.saveAllInNewTx(List.of(n));
        } catch (Exception ignored) {
        }
    }

    private BusinessDtos.BusinessDetail toDetail(BusinessListing listing, UUID viewerUserId) {
        User u = listing.getUser();
        UserProfile p = userProfileRepository.findById(u.getId()).orElse(null);
        String ownerName = resolveDisplayName(u, p);
        String ownerAvatar = p != null ? p.getAvatarUrl() : null;
        String ownerProfileKey = p != null ? p.getProfileKey() : null;
        List<String> photos = parsePhotos(listing.getPhotosJson());
        boolean isOwner = viewerUserId != null && viewerUserId.equals(u.getId());
        return new BusinessDtos.BusinessDetail(
                listing.getId().toString(),
                listing.getName(),
                listing.getDescription(),
                listing.getCategory(),
                listing.getPhone(),
                listing.getEmail(),
                listing.getAddress(),
                listing.getCity(),
                listing.getWebsite(),
                photos,
                listing.getStatus().name(),
                listing.getRejectionReason(),
                u.getId().toString(),
                ownerName,
                ownerAvatar,
                ownerProfileKey,
                listing.isFeatured(),
                listing.getViewCount(),
                isOwner,
                listing.getCreatedAt().toString(),
                listing.getUpdatedAt().toString()
        );
    }

    private BusinessDtos.BusinessAdminSummary toAdminSummary(BusinessListing listing) {
        User u = listing.getUser();
        UserProfile p = userProfileRepository.findById(u.getId()).orElse(null);
        String ownerName = p != null && p.getFullName() != null ? p.getFullName() : "";
        return new BusinessDtos.BusinessAdminSummary(
                listing.getId().toString(),
                listing.getName(),
                listing.getCategory(),
                listing.getCity(),
                listing.getStatus().name(),
                u.getId().toString(),
                ownerName,
                u.getEmail(),
                listing.isFeatured(),
                listing.getCreatedAt().toString(),
                listing.getUpdatedAt().toString()
        );
    }

    private BusinessDtos.BusinessPageResponse toPage(Page<BusinessListing> pg) {
        List<BusinessDtos.BusinessSummary> rows = pg.getContent().stream().map(this::toSummary).toList();
        return new BusinessDtos.BusinessPageResponse(
                rows, pg.getTotalPages(), pg.getTotalElements(),
                pg.getSize(), pg.getNumber(), pg.isFirst(), pg.isLast()
        );
    }

    private BusinessDtos.BusinessSummary toSummary(BusinessListing listing) {
        User u = listing.getUser();
        UserProfile p = userProfileRepository.findById(u.getId()).orElse(null);
        List<String> photos = parsePhotos(listing.getPhotosJson());
        return new BusinessDtos.BusinessSummary(
                listing.getId().toString(),
                listing.getName(),
                listing.getCategory(),
                listing.getCity(),
                listing.getPhone(),
                photos.isEmpty() ? null : photos.get(0),
                listing.getStatus().name(),
                u.getId().toString(),
                resolveDisplayName(u, p),
                p != null ? p.getAvatarUrl() : null,
                listing.isFeatured(),
                listing.getViewCount(),
                listing.getCreatedAt().toString()
        );
    }

    private static String resolveDisplayName(User u, UserProfile p) {
        if (p != null && p.getFullName() != null && !p.getFullName().isBlank()) return p.getFullName();
        if (p != null && p.getProfileKey() != null && !p.getProfileKey().isBlank()) return p.getProfileKey();
        return u.getEmail() != null ? u.getEmail() : "Member";
    }

    private BusinessListing requireListing(UUID id) {
        return businessRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business listing not found"));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static BusinessStatus parseStatus(String s) {
        try {
            return BusinessStatus.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + s);
        }
    }
}
