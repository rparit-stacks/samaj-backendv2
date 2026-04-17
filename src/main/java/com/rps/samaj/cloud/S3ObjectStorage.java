package com.rps.samaj.cloud;

import com.rps.samaj.config.app.RuntimeConfigService;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class S3ObjectStorage {

    private final RuntimeConfigService runtimeConfig;

    public S3ObjectStorage(RuntimeConfigService runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    public StorageResult store(String folder, String objectKey, InputStream data, long contentLength, String contentType) {
        RuntimeConfigService.ResolvedS3Config c = runtimeConfig.resolvedS3();
        if (!c.usable()) {
            throw new IllegalStateException("S3 is not fully configured (bucket, region, credentials)");
        }
        String key = folder + "/" + objectKey;
        try (S3Client client = buildClient(c)) {
            PutObjectRequest.Builder req = PutObjectRequest.builder()
                    .bucket(c.bucket())
                    .key(key)
                    .contentType(contentType != null ? contentType : "application/octet-stream");
            if (contentLength > 0) {
                client.putObject(req.build(), RequestBody.fromInputStream(data, contentLength));
            } else {
                Path tmp = Files.createTempFile("upload-", ".bin");
                try {
                    Files.copy(data, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    client.putObject(req.build(), RequestBody.fromFile(tmp));
                } finally {
                    Files.deleteIfExists(tmp);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("S3 upload failed", e);
        }
        String pub = c.publicBaseUrl().replaceAll("/$", "") + "/" + key;
        return new StorageResult(pub, key, "S3");
    }

    public void deleteByStorageKey(String storageKey) {
        RuntimeConfigService.ResolvedS3Config c = runtimeConfig.resolvedS3();
        if (!c.usable()) {
            return;
        }
        try (S3Client client = buildClient(c)) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(c.bucket()).key(storageKey).build());
        }
    }

    private static S3Client buildClient(RuntimeConfigService.ResolvedS3Config c) {
        AwsCredentialsProvider creds;
        if (c.accessKeyId() != null && !c.accessKeyId().isBlank()) {
            creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(c.accessKeyId(), c.secretAccessKey()));
        } else {
            creds = DefaultCredentialsProvider.create();
        }
        var builder = S3Client.builder()
                .region(Region.of(c.region()))
                .credentialsProvider(creds);
        if (c.endpoint() != null && !c.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(c.endpoint()))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }
}
