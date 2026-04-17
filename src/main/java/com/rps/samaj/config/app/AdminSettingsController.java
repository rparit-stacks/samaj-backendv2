package com.rps.samaj.config.app;

import com.rps.samaj.api.dto.AppConfigDtos;
import com.rps.samaj.cms.CmsBannerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.rps.samaj.security.DevUserContextFilter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final AdminSettingsService settingsService;
    private final CmsBannerService bannerService;
    private final AdminAuditLogRepository auditLogRepository;

    public AdminSettingsController(AdminSettingsService settingsService, CmsBannerService bannerService, AdminAuditLogRepository auditLogRepository) {
        this.settingsService = settingsService;
        this.bannerService = bannerService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public AppConfigDtos.AdminSettingsResponse getAll(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId
    ) {
        requireAdmin(adminId);
        return settingsService.getAll();
    }

    @PutMapping("/smtp")
    public AppConfigDtos.SmtpConfigResponse updateSmtp(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @Valid @RequestBody AppConfigDtos.SmtpConfigResponse body,
            HttpServletRequest request
    ) {
        requireAdmin(adminId);
        return settingsService.updateSmtp(body, adminId, getClientIp(request));
    }

    @PutMapping("/maintenance-mode")
    public AppConfigDtos.MaintenanceModeResponse updateMaintenanceMode(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @RequestBody AppConfigDtos.MaintenanceModeResponse body,
            HttpServletRequest request
    ) {
        requireAdmin(adminId);
        return settingsService.updateMaintenanceMode(body, adminId, getClientIp(request));
    }

    @PutMapping("/storage")
    public AppConfigDtos.StorageConfigResponse updateStorageConfig(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @RequestBody AppConfigDtos.StorageConfigUpdateRequest body,
            HttpServletRequest request
    ) {
        requireAdmin(adminId);
        return settingsService.updateStorageConfig(body, adminId, getClientIp(request));
    }

    @GetMapping("/cms/banners")
    public List<AppConfigDtos.CmsMobileBannerResponse> listBanners(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId
    ) {
        requireAdmin(adminId);
        return bannerService.listAll();
    }

    @PostMapping("/cms/banners")
    @ResponseStatus(HttpStatus.CREATED)
    public AppConfigDtos.CmsMobileBannerResponse createBanner(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @Valid @RequestBody AppConfigDtos.CmsMobileBannerCreateRequest body
    ) {
        requireAdmin(adminId);
        return bannerService.create(body, adminId);
    }

    @PutMapping("/cms/banners/{bannerId}")
    public AppConfigDtos.CmsMobileBannerResponse updateBanner(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID bannerId,
            @RequestBody AppConfigDtos.CmsMobileBannerUpdateRequest body
    ) {
        requireAdmin(adminId);
        return bannerService.update(bannerId, body, adminId);
    }

    @DeleteMapping("/cms/banners/{bannerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBanner(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID bannerId
    ) {
        requireAdmin(adminId);
        bannerService.delete(bannerId);
    }

    @GetMapping("/audit-logs")
    public AppConfigDtos.AdminAuditLogPageResponse getAuditLogs(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String resource
    ) {
        requireAdmin(adminId);
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), 100);

        Page<AdminAuditLog> logs;
        if (resource != null && !resource.isBlank()) {
            logs = auditLogRepository.findByResourceOrderByCreatedAtDesc(resource, PageRequest.of(validPage, validSize));
        } else {
            logs = auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(validPage, validSize));
        }

        List<AppConfigDtos.AdminAuditLogResponse> content = logs.getContent().stream()
                .map(log -> new AppConfigDtos.AdminAuditLogResponse(
                        log.getId().toString(),
                        log.getAction(),
                        log.getResource(),
                        log.getChangesBefore(),
                        log.getChangesAfter(),
                        log.getAdminUserId().toString(),
                        log.getCreatedAt().toString(),
                        log.getIpAddress()
                ))
                .collect(Collectors.toList());

        return new AppConfigDtos.AdminAuditLogPageResponse(
                content,
                logs.getTotalPages(),
                logs.getTotalElements(),
                logs.getSize(),
                logs.getNumber(),
                logs.isFirst(),
                logs.isLast()
        );
    }

    private static void requireAdmin(UUID adminId) {
        if (adminId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin context required");
        }
    }

    private static String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
