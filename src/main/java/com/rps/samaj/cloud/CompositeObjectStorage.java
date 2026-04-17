package com.rps.samaj.cloud;

import com.rps.samaj.config.app.RuntimeConfigService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Primary
public class CompositeObjectStorage implements ObjectStoragePort {

    private final RuntimeConfigService runtimeConfig;
    private final LocalFilesystemObjectStorage local;
    private final S3ObjectStorage s3;

    public CompositeObjectStorage(RuntimeConfigService runtimeConfig,
                                  LocalFilesystemObjectStorage local,
                                  S3ObjectStorage s3) {
        this.runtimeConfig = runtimeConfig;
        this.local = local;
        this.s3 = s3;
    }

    @Override
    public StorageResult store(String folder, String objectKey, InputStream data, long contentLength, String contentType) {
        if ("S3".equals(runtimeConfig.effectiveStorageProvider())) {
            RuntimeConfigService.ResolvedS3Config c = runtimeConfig.resolvedS3();
            if (c.usable()) {
                return s3.store(folder, objectKey, data, contentLength, contentType);
            }
        }
        return local.store(folder, objectKey, data, contentLength, contentType);
    }

    @Override
    public void deleteByStorageKey(String storageKey) {
        if ("S3".equals(runtimeConfig.effectiveStorageProvider())) {
            RuntimeConfigService.ResolvedS3Config c = runtimeConfig.resolvedS3();
            if (c.usable()) {
                s3.deleteByStorageKey(storageKey);
                return;
            }
        }
        local.deleteByStorageKey(storageKey);
    }
}
