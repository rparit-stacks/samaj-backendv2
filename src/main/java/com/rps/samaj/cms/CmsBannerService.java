package com.rps.samaj.cms;

import com.rps.samaj.api.dto.AppConfigDtos;
import com.rps.samaj.config.cache.RedisCacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CmsBannerService {

    private final CmsMobileBannerRepository bannerRepository;

    public CmsBannerService(CmsMobileBannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.CMS_BANNERS_ALL, key = "'v1'")
    public List<AppConfigDtos.CmsMobileBannerResponse> listAll() {
        return bannerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.CMS_BANNERS_ACTIVE, key = "'v1'")
    public List<AppConfigDtos.CmsMobileBannerResponse> listActive() {
        return bannerRepository.findByActiveOrderByDisplayOrder(true).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.CMS_BANNERS_ACTIVE,
            RedisCacheConfig.Names.CMS_BANNERS_ALL
    }, allEntries = true)
    public AppConfigDtos.CmsMobileBannerResponse create(
            AppConfigDtos.CmsMobileBannerCreateRequest req,
            UUID createdByUserId
    ) {
        validateBannerInput(req.title(), req.imageUrl(), req.redirectType(), req.redirectTarget(), req.displayOrder());

        CmsMobileBanner banner = new CmsMobileBanner(
                UUID.randomUUID(),
                req.title().trim(),
                req.imageUrl().trim(),
                req.redirectType(),
                req.redirectTarget().trim(),
                req.displayOrder(),
                createdByUserId
        );

        bannerRepository.save(banner);
        return toResponse(banner);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.CMS_BANNERS_ACTIVE,
            RedisCacheConfig.Names.CMS_BANNERS_ALL
    }, allEntries = true)
    public AppConfigDtos.CmsMobileBannerResponse update(
            UUID bannerId,
            AppConfigDtos.CmsMobileBannerUpdateRequest req,
            UUID updatedByUserId
    ) {
        CmsMobileBanner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Banner not found"));

        if (req.title() != null && !req.title().isBlank()) {
            if (req.title().length() > 255) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title too long (max 255 characters)");
            }
            banner.setTitle(req.title().trim());
        }
        if (req.imageUrl() != null && !req.imageUrl().isBlank()) {
            if (!isValidUrl(req.imageUrl())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image URL format");
            }
            banner.setImageUrl(req.imageUrl().trim());
        }
        if (req.redirectType() != null && !req.redirectType().isBlank()) {
            if (!req.redirectType().equals("INTERNAL") && !req.redirectType().equals("EXTERNAL")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redirect type must be INTERNAL or EXTERNAL");
            }
            banner.setRedirectType(req.redirectType());
        }
        if (req.redirectTarget() != null && !req.redirectTarget().isBlank()) {
            if (req.redirectTarget().length() > 2000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redirect target too long (max 2000 characters)");
            }
            if (banner.getRedirectType().equals("EXTERNAL") && !isValidUrl(req.redirectTarget())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid external URL format");
            }
            banner.setRedirectTarget(req.redirectTarget().trim());
        }
        if (req.displayOrder() != null) {
            if (req.displayOrder() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display order must be >= 0");
            }
            banner.setDisplayOrder(req.displayOrder());
        }
        if (req.active() != null) {
            banner.setActive(req.active());
        }

        banner.setUpdatedAt(Instant.now());
        bannerRepository.save(banner);
        return toResponse(banner);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.CMS_BANNERS_ACTIVE,
            RedisCacheConfig.Names.CMS_BANNERS_ALL
    }, allEntries = true)
    public void delete(UUID bannerId) {
        if (!bannerRepository.existsById(bannerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Banner not found");
        }
        bannerRepository.deleteById(bannerId);
    }

    private AppConfigDtos.CmsMobileBannerResponse toResponse(CmsMobileBanner banner) {
        return new AppConfigDtos.CmsMobileBannerResponse(
                banner.getId().toString(),
                banner.getTitle(),
                banner.getImageUrl(),
                banner.getRedirectType(),
                banner.getRedirectTarget(),
                banner.getDisplayOrder(),
                banner.isActive(),
                banner.getCreatedAt().toString(),
                banner.getUpdatedAt().toString()
        );
    }

    private void validateBannerInput(String title, String imageUrl, String redirectType, String redirectTarget, int displayOrder) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title required");
        }
        if (title.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title too long (max 255 characters)");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image URL required");
        }
        if (!isValidUrl(imageUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image URL format");
        }
        if (redirectType == null || (!redirectType.equals("INTERNAL") && !redirectType.equals("EXTERNAL"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redirect type must be INTERNAL or EXTERNAL");
        }
        if (redirectTarget == null || redirectTarget.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redirect target required");
        }
        if (redirectTarget.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Redirect target too long (max 2000 characters)");
        }
        if (redirectType.equals("EXTERNAL") && !isValidUrl(redirectTarget)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid external URL format");
        }
        if (displayOrder < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display order must be >= 0");
        }
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) return false;
        return url.matches("^(https?://|/).*") || url.matches("^[a-zA-Z0-9/_-]+$");
    }
}
