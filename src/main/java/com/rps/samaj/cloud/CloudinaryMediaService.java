package com.rps.samaj.cloud;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * When {@code samaj.cloudinary.url} is set (typically via env {@code CLOUDINARY_URL}),
 * user uploads are sent to Cloudinary instead of local/S3 storage.
 */
@Service
public class CloudinaryMediaService {

    private final Cloudinary cloudinary;

    public CloudinaryMediaService(@Value("${samaj.cloudinary.url:}") String cloudinaryUrl) {
        String u = cloudinaryUrl == null ? "" : cloudinaryUrl.trim();
        this.cloudinary = u.isEmpty() ? null : new Cloudinary(u);
    }

    public boolean isReady() {
        return cloudinary != null;
    }

    @SuppressWarnings("unchecked")
    public StorageResult upload(MultipartFile file, String logicalFolder) throws Exception {
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary is not configured");
        }
        String folder = "samaj/" + logicalFolder;
        Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "auto"
        );
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
        String secureUrl = (String) result.get("secure_url");
        String publicId = (String) result.get("public_id");
        if (secureUrl == null || publicId == null) {
            throw new IllegalStateException("Cloudinary response missing url or public_id");
        }
        return new StorageResult(secureUrl, "cloudinary:" + publicId, "CLOUDINARY");
    }

    public void destroyByStorageKey(String storageKey) throws Exception {
        if (cloudinary == null || storageKey == null || !storageKey.startsWith("cloudinary:")) {
            return;
        }
        String publicId = storageKey.substring("cloudinary:".length());
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
