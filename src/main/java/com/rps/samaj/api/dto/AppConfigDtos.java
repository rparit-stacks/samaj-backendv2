package com.rps.samaj.api.dto;

import java.util.List;
import java.util.Map;

public final class AppConfigDtos {

    private AppConfigDtos() {
    }

    public record AppConfigMapResponse(Map<String, String> entries) {
    }

    public record AppConfigPatchRequest(Map<String, String> entries) {
    }

    public record EffectiveStorageResponse(
            String provider,
            String s3Bucket,
            String s3Region,
            String publicBaseUrl,
            boolean s3Configured
    ) {
    }

    // SMTP & Maintenance Config
    public record SmtpConfigResponse(
            String host,
            int port,
            String username,
            String fromEmail,
            String fromName,
            boolean configured
    ) {
    }

    /**
     * Update request for SMTP.
     * Password is optional: when null/blank, keep the existing stored password.
     */
    public record SmtpConfigUpdateRequest(
            String host,
            int port,
            String username,
            String password,
            String fromEmail,
            String fromName
    ) {
    }

    public record MaintenanceModeResponse(
            boolean enabled,
            String message,
            String endTime
    ) {
    }

    public record AdminSettingsResponse(
            SmtpConfigResponse smtp,
            MaintenanceModeResponse maintenanceMode,
            StorageConfigResponse storageConfig,
            List<CmsMobileBannerResponse> cmsBanners
    ) {
    }

    public record SmtpTestResponse(
            boolean success,
            String message
    ) {
    }

    public record CmsMobileBannerResponse(
            String id,
            String title,
            String imageUrl,
            String redirectType,
            String redirectTarget,
            int displayOrder,
            boolean active,
            String createdAt,
            String updatedAt
    ) {
    }

    public record CmsMobileBannerCreateRequest(
            String title,
            String imageUrl,
            String redirectType,
            String redirectTarget,
            int displayOrder
    ) {
    }

    public record CmsMobileBannerUpdateRequest(
            String title,
            String imageUrl,
            String redirectType,
            String redirectTarget,
            Integer displayOrder,
            Boolean active
    ) {
    }

    // Storage Config
    public record StorageConfigResponse(
            String provider,
            String s3Bucket,
            String s3Region,
            String s3PublicBaseUrl,
            String s3Endpoint,
            boolean s3Configured,
            String localRoot,
            String localPublicBaseUrl,
            boolean localConfigured
    ) {
    }

    public record StorageConfigUpdateRequest(
            String provider,
            String s3Bucket,
            String s3Region,
            String s3PublicBaseUrl,
            String s3Endpoint,
            String s3AccessKeyId,
            String s3SecretAccessKey,
            String localRoot,
            String localPublicBaseUrl
    ) {
    }

    // Admin Audit Logs
    public record AdminAuditLogResponse(
            String id,
            String action,
            String resource,
            String changesBefore,
            String changesAfter,
            String adminUserId,
            String createdAt,
            String ipAddress
    ) {
    }

    public record AdminAuditLogPageResponse(
            List<AdminAuditLogResponse> content,
            int totalPages,
            long totalElements,
            int size,
            int number,
            boolean first,
            boolean last
    ) {
    }
}
