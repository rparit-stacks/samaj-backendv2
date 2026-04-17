package com.rps.samaj.config.app;

import com.rps.samaj.api.dto.AppConfigDtos;
import com.rps.samaj.cms.CmsBannerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminSettingsService {

    private final RuntimeConfigService runtimeConfig;
    private final CmsBannerService bannerService;
    private final AdminAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AdminSettingsService(
            RuntimeConfigService runtimeConfig,
            CmsBannerService bannerService,
            AdminAuditLogRepository auditLogRepository,
            ObjectMapper objectMapper
    ) {
        this.runtimeConfig = runtimeConfig;
        this.bannerService = bannerService;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public AppConfigDtos.AdminSettingsResponse getAll() {
        var smtp = runtimeConfig.getSmtpConfig();
        var storageConfig = runtimeConfig.getStorageConfig();

        return new AppConfigDtos.AdminSettingsResponse(
                new AppConfigDtos.SmtpConfigResponse(
                        smtp.host(),
                        smtp.port(),
                        smtp.username(),
                        smtp.fromEmail(),
                        smtp.fromName(),
                        smtp.isConfigured()
                ),
                new AppConfigDtos.MaintenanceModeResponse(
                        runtimeConfig.isMaintenanceModeEnabled(),
                        runtimeConfig.getMaintenanceMessage(),
                        runtimeConfig.getMaintenanceEndTime().orElse(null)
                ),
                new AppConfigDtos.StorageConfigResponse(
                        storageConfig.provider(),
                        storageConfig.s3Bucket(),
                        storageConfig.s3Region(),
                        storageConfig.s3PublicBaseUrl(),
                        storageConfig.s3Endpoint(),
                        storageConfig.s3Configured(),
                        storageConfig.localRoot(),
                        storageConfig.localPublicBaseUrl(),
                        storageConfig.localConfigured()
                ),
                bannerService.listAll()
        );
    }

    @Transactional
    public AppConfigDtos.SmtpConfigResponse updateSmtp(
            AppConfigDtos.SmtpConfigResponse body,
            UUID adminId,
            String ipAddress
    ) {
        String before = objectMapper.valueToTree(runtimeConfig.getSmtpConfig()).toString();

        runtimeConfig.upsert(RuntimeConfigService.KEY_SMTP_HOST, body.host(), adminId);
        runtimeConfig.upsert(RuntimeConfigService.KEY_SMTP_PORT, String.valueOf(body.port()), adminId);
        runtimeConfig.upsert(RuntimeConfigService.KEY_SMTP_USERNAME, body.username(), adminId);
        runtimeConfig.upsert(RuntimeConfigService.KEY_SMTP_FROM_EMAIL, body.fromEmail(), adminId);
        runtimeConfig.upsert(RuntimeConfigService.KEY_SMTP_FROM_NAME, body.fromName(), adminId);

        var updated = runtimeConfig.getSmtpConfig();
        String after = objectMapper.valueToTree(updated).toString();

        logAudit("UPDATE", "SMTP_CONFIG", before, after, adminId, ipAddress);

        return new AppConfigDtos.SmtpConfigResponse(
                updated.host(),
                updated.port(),
                updated.username(),
                updated.fromEmail(),
                updated.fromName(),
                updated.isConfigured()
        );
    }

    @Transactional
    public AppConfigDtos.MaintenanceModeResponse updateMaintenanceMode(
            AppConfigDtos.MaintenanceModeResponse body,
            UUID adminId,
            String ipAddress
    ) {
        String before = objectMapper.valueToTree(
                new AppConfigDtos.MaintenanceModeResponse(
                        runtimeConfig.isMaintenanceModeEnabled(),
                        runtimeConfig.getMaintenanceMessage(),
                        runtimeConfig.getMaintenanceEndTime().orElse(null)
                )
        ).toString();

        runtimeConfig.upsert(RuntimeConfigService.KEY_MAINTENANCE_MODE, String.valueOf(body.enabled()), adminId);
        runtimeConfig.upsert(RuntimeConfigService.KEY_MAINTENANCE_MESSAGE, body.message() != null ? body.message() : "", adminId);
        if (body.endTime() != null && !body.endTime().isBlank()) {
            runtimeConfig.upsert(RuntimeConfigService.KEY_MAINTENANCE_END_TIME, body.endTime(), adminId);
        } else {
            runtimeConfig.upsert(RuntimeConfigService.KEY_MAINTENANCE_END_TIME, "", adminId);
        }

        var response = new AppConfigDtos.MaintenanceModeResponse(
                runtimeConfig.isMaintenanceModeEnabled(),
                runtimeConfig.getMaintenanceMessage(),
                runtimeConfig.getMaintenanceEndTime().orElse(null)
        );

        String after = objectMapper.valueToTree(response).toString();
        logAudit("UPDATE", "MAINTENANCE_MODE", before, after, adminId, ipAddress);

        return response;
    }

    @Transactional
    public AppConfigDtos.StorageConfigResponse updateStorageConfig(
            AppConfigDtos.StorageConfigUpdateRequest body,
            UUID adminId,
            String ipAddress
    ) {
        String before = objectMapper.valueToTree(runtimeConfig.getStorageConfig()).toString();

        if (body.provider() != null) {
            runtimeConfig.upsert(RuntimeConfigService.KEY_STORAGE_PROVIDER, body.provider(), adminId);
        }
        if (body.s3Bucket() != null) {
            runtimeConfig.upsert(RuntimeConfigService.KEY_STORAGE_S3_BUCKET, body.s3Bucket(), adminId);
        }
        if (body.s3Region() != null) {
            runtimeConfig.upsert(RuntimeConfigService.KEY_STORAGE_S3_REGION, body.s3Region(), adminId);
        }
        if (body.s3PublicBaseUrl() != null) {
            runtimeConfig.upsert(RuntimeConfigService.KEY_STORAGE_S3_PUBLIC_BASE_URL, body.s3PublicBaseUrl(), adminId);
        }
        if (body.s3Endpoint() != null) {
            runtimeConfig.upsert(RuntimeConfigService.KEY_STORAGE_S3_ENDPOINT, body.s3Endpoint(), adminId);
        }
        if (body.s3AccessKeyId() != null) {
            runtimeConfig.upsert(RuntimeConfigService.KEY_STORAGE_S3_ACCESS_KEY_ID, body.s3AccessKeyId(), adminId);
        }
        if (body.s3SecretAccessKey() != null) {
            runtimeConfig.upsert(RuntimeConfigService.KEY_STORAGE_S3_SECRET_ACCESS_KEY, body.s3SecretAccessKey(), adminId);
        }
        if (body.localRoot() != null) {
            runtimeConfig.upsert(RuntimeConfigService.KEY_STORAGE_LOCAL_ROOT, body.localRoot(), adminId);
        }
        if (body.localPublicBaseUrl() != null) {
            runtimeConfig.upsert(RuntimeConfigService.KEY_STORAGE_LOCAL_PUBLIC_BASE_URL, body.localPublicBaseUrl(), adminId);
        }

        var config = runtimeConfig.getStorageConfig();
        String after = objectMapper.valueToTree(config).toString();

        logAudit("UPDATE", "STORAGE_CONFIG", before, after, adminId, ipAddress);

        return new AppConfigDtos.StorageConfigResponse(
                config.provider(),
                config.s3Bucket(),
                config.s3Region(),
                config.s3PublicBaseUrl(),
                config.s3Endpoint(),
                config.s3Configured(),
                config.localRoot(),
                config.localPublicBaseUrl(),
                config.localConfigured()
        );
    }

    private void logAudit(String action, String resource, String before, String after, UUID adminId, String ipAddress) {
        try {
            AdminAuditLog log = new AdminAuditLog(action, resource, before, after, adminId, ipAddress);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Audit failures should not break the operation
        }
    }
}
