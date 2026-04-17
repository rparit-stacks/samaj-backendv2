package com.rps.samaj.config.app;

import com.rps.samaj.config.SamajProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Effective configuration: database {@link AppConfigEntry} overrides / fills gaps vs {@link SamajProperties}.
 */
@Service
public class RuntimeConfigService {

    public static final String KEY_STORAGE_PROVIDER = "storage.provider";
    public static final String KEY_STORAGE_S3_BUCKET = "storage.s3.bucket";
    public static final String KEY_STORAGE_S3_REGION = "storage.s3.region";
    public static final String KEY_STORAGE_S3_PUBLIC_BASE_URL = "storage.s3.public-base-url";
    public static final String KEY_STORAGE_S3_ENDPOINT = "storage.s3.endpoint";
    public static final String KEY_STORAGE_S3_ACCESS_KEY_ID = "storage.s3.access-key-id";
    public static final String KEY_STORAGE_S3_SECRET_ACCESS_KEY = "storage.s3.secret-access-key";
    public static final String KEY_STORAGE_LOCAL_ROOT = "storage.local.root";
    public static final String KEY_STORAGE_LOCAL_PUBLIC_BASE_URL = "storage.local.public-base-url";
    public static final String KEY_KYC_REQUIRED = "kyc.required";
    public static final String KEY_SITE_NAME = "site.name";
    public static final String KEY_SMTP_HOST = "smtp.host";
    public static final String KEY_SMTP_PORT = "smtp.port";
    public static final String KEY_SMTP_USERNAME = "smtp.username";
    public static final String KEY_SMTP_PASSWORD = "smtp.password";
    public static final String KEY_SMTP_FROM_EMAIL = "smtp.from-email";
    public static final String KEY_SMTP_FROM_NAME = "smtp.from-name";
    public static final String KEY_MAINTENANCE_MODE = "maintenance.mode.enabled";
    public static final String KEY_MAINTENANCE_MESSAGE = "maintenance.mode.message";
    public static final String KEY_MAINTENANCE_END_TIME = "maintenance.mode.end-time";

    private final AppConfigEntryRepository repo;
    private final SamajProperties properties;

    public RuntimeConfigService(AppConfigEntryRepository repo, SamajProperties properties) {
        this.repo = repo;
        this.properties = properties;
    }

    public Optional<String> getDbValue(String key) {
        return repo.findById(key).map(AppConfigEntry::getValue);
    }

    public String getString(String key, String fallback) {
        return getDbValue(key)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(fallback);
    }

    public boolean getBoolean(String key, boolean fallback) {
        return getDbValue(key)
                .map(s -> s.trim().equalsIgnoreCase("true") || s.trim().equals("1"))
                .orElse(fallback);
    }

    /** LOCAL or S3 (case-insensitive). */
    public String effectiveStorageProvider() {
        String fromDb = getDbValue(KEY_STORAGE_PROVIDER).orElse("").trim();
        if (!fromDb.isEmpty()) {
            return fromDb.toUpperCase(Locale.ROOT);
        }
        return properties.getStorage().getProvider().toUpperCase(Locale.ROOT);
    }

    public boolean isKycRequired() {
        return getBoolean(KEY_KYC_REQUIRED, false);
    }

    /** Resolved S3 settings for building the client (DB wins when set). */
    public ResolvedS3Config resolvedS3() {
        SamajProperties.Storage.S3 p = properties.getStorage().getS3();
        return new ResolvedS3Config(
                getString(KEY_STORAGE_S3_BUCKET, p.getBucket()),
                getString(KEY_STORAGE_S3_REGION, p.getRegion()),
                getString(KEY_STORAGE_S3_PUBLIC_BASE_URL, properties.getStorage().getPublicBaseUrl()),
                getString(KEY_STORAGE_S3_ENDPOINT, p.getEndpoint()),
                getString(KEY_STORAGE_S3_ACCESS_KEY_ID, p.getAccessKeyId()),
                getString(KEY_STORAGE_S3_SECRET_ACCESS_KEY, p.getSecretAccessKey())
        );
    }

    public ResolvedLocalStorageConfig resolvedLocal() {
        return new ResolvedLocalStorageConfig(
                getString(KEY_STORAGE_LOCAL_ROOT, properties.getStorage().getRoot()),
                getString(KEY_STORAGE_LOCAL_PUBLIC_BASE_URL, properties.getStorage().getPublicBaseUrl())
        );
    }

    @Transactional
    public void upsert(String key, String value, UUID updatedBy) {
        AppConfigEntry e = repo.findById(key)
                .orElseGet(() -> new AppConfigEntry(key, value != null ? value : "", updatedBy));
        e.setValue(value != null ? value : "");
        e.setUpdatedByUserId(updatedBy);
        e.setUpdatedAt(java.time.Instant.now());
        repo.save(e);
    }

    @Transactional
    public Map<String, String> replaceAll(Map<String, String> entries, UUID updatedBy) {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, String> en : entries.entrySet()) {
            upsert(en.getKey(), en.getValue() != null ? en.getValue() : "", updatedBy);
            out.put(en.getKey(), en.getValue());
        }
        return out;
    }

    public Map<String, String> listAll() {
        List<AppConfigEntry> all = repo.findAll();
        Map<String, String> m = new HashMap<>();
        for (AppConfigEntry e : all) {
            m.put(e.getKey(), e.getValue());
        }
        return m;
    }

    public record ResolvedS3Config(
            String bucket,
            String region,
            String publicBaseUrl,
            String endpoint,
            String accessKeyId,
            String secretAccessKey
    ) {
        /** Bucket + region required; keys optional (IAM role / default chain). */
        public boolean usable() {
            return bucket != null && !bucket.isBlank() && region != null && !region.isBlank();
        }

        public boolean hasExplicitCredentials() {
            return accessKeyId != null && !accessKeyId.isBlank()
                    && secretAccessKey != null && !secretAccessKey.isBlank();
        }
    }

    public record ResolvedLocalStorageConfig(String root, String publicBaseUrl) {
    }

    // ==================== SMTP CONFIG ====================
    public record SmtpConfig(
            String host,
            int port,
            String username,
            String password,
            String fromEmail,
            String fromName
    ) {
        public boolean isConfigured() {
            return host != null && !host.isBlank()
                    && port > 0
                    && fromEmail != null && !fromEmail.isBlank();
        }
    }

    public SmtpConfig getSmtpConfig() {
        return new SmtpConfig(
                getString(KEY_SMTP_HOST, ""),
                Integer.parseInt(getString(KEY_SMTP_PORT, "587")),
                getString(KEY_SMTP_USERNAME, ""),
                getString(KEY_SMTP_PASSWORD, ""),
                getString(KEY_SMTP_FROM_EMAIL, ""),
                getString(KEY_SMTP_FROM_NAME, "Samaj")
        );
    }

    // ==================== MAINTENANCE MODE ====================
    public boolean isMaintenanceModeEnabled() {
        return getBoolean(KEY_MAINTENANCE_MODE, false);
    }

    public String getMaintenanceMessage() {
        return getString(KEY_MAINTENANCE_MESSAGE, "The system is under maintenance. Please check back later.");
    }

    public Optional<String> getMaintenanceEndTime() {
        return getDbValue(KEY_MAINTENANCE_END_TIME)
                .filter(s -> !s.isBlank());
    }

    // ==================== STORAGE CONFIG ====================
    public StorageConfigSnapshot getStorageConfig() {
        SamajProperties.Storage.S3 s3 = properties.getStorage().getS3();
        ResolvedS3Config rs3 = resolvedS3();

        String localRoot = getString(KEY_STORAGE_LOCAL_ROOT, properties.getStorage().getRoot());
        String localPublicBaseUrl = getString(KEY_STORAGE_LOCAL_PUBLIC_BASE_URL, properties.getStorage().getPublicBaseUrl());

        return new StorageConfigSnapshot(
                effectiveStorageProvider(),
                rs3.bucket(),
                rs3.region(),
                rs3.publicBaseUrl(),
                rs3.endpoint(),
                rs3.usable(),
                localRoot,
                localPublicBaseUrl,
                localRoot != null && !localRoot.isBlank()
        );
    }

    public record StorageConfigSnapshot(
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
}
