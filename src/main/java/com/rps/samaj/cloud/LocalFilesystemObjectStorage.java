package com.rps.samaj.cloud;

import com.rps.samaj.config.app.RuntimeConfigService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class LocalFilesystemObjectStorage {

    private final RuntimeConfigService runtimeConfig;

    public LocalFilesystemObjectStorage(RuntimeConfigService runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public StorageResult store(String folder, String objectKey, InputStream data, long contentLength, String contentType) {
        RuntimeConfigService.ResolvedLocalStorageConfig cfg = runtimeConfig.resolvedLocal();
        Path base = Path.of(cfg.root()).resolve(folder).normalize();
        Path target = base.resolve(objectKey).normalize();
        if (!target.startsWith(Path.of(cfg.root()).resolve(folder).normalize())) {
            throw new IllegalArgumentException("Invalid path");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
        String url = cfg.publicBaseUrl().replaceAll("/$", "") + "/" + folder + "/" + objectKey.replace("\\", "/");
        return new StorageResult(url, folder + "/" + objectKey, "LOCAL");
    }

    public void deleteByStorageKey(String storageKey) {
        RuntimeConfigService.ResolvedLocalStorageConfig cfg = runtimeConfig.resolvedLocal();
        Path path = Path.of(cfg.root()).resolve(storageKey).normalize();
        if (!path.startsWith(Path.of(cfg.root()).normalize())) {
            throw new IllegalArgumentException("Invalid path");
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
