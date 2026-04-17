package com.rps.samaj.cloud;

import com.rps.samaj.api.dto.CloudDtos;
import com.rps.samaj.security.DevUserContextFilter;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/cloud")
public class CloudUploadController {

    private final ObjectStoragePort storage;
    private final UserRepository userRepository;
    private final StoredObjectRepository storedObjectRepository;
    private final CloudinaryMediaService cloudinaryMediaService;

    public CloudUploadController(ObjectStoragePort storage, UserRepository userRepository,
                                 StoredObjectRepository storedObjectRepository,
                                 CloudinaryMediaService cloudinaryMediaService) {
        this.storage = storage;
        this.userRepository = userRepository;
        this.storedObjectRepository = storedObjectRepository;
        this.cloudinaryMediaService = cloudinaryMediaService;
    }

    @PostMapping("/profile-image")
    public CloudDtos.CloudUploadResponse profileImage(
            @RequestAttribute(name = DevUserContextFilter.ATTR_USER_ID, required = false) UUID userId,
            @RequestParam("file") MultipartFile file
    ) {
        return upload(userId, file, "profile");
    }

    @PostMapping("/background-image")
    public CloudDtos.CloudUploadResponse backgroundImage(
            @RequestAttribute(name = DevUserContextFilter.ATTR_USER_ID, required = false) UUID userId,
            @RequestParam("file") MultipartFile file
    ) {
        return upload(userId, file, "background");
    }

    @PostMapping("/upload")
    public CloudDtos.CloudUploadResponse uploadWithFolder(
            @RequestAttribute(name = DevUserContextFilter.ATTR_USER_ID, required = false) UUID userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "misc") String folder
    ) {
        String f = folder.replaceAll("[^a-zA-Z0-9_-]", "");
        if (f.isEmpty()) {
            f = "misc";
        }
        return upload(userId, file, f);
    }

    @DeleteMapping("/delete")
    public java.util.Map<String, String> deleteByUrl(
            @RequestAttribute(name = DevUserContextFilter.ATTR_USER_ID, required = false) UUID userId,
            @RequestParam("url") String url
    ) {
        requireUser(userId);
        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
        var opt = storedObjectRepository.findByPublicUrlAndUser_Id(decoded, userId);
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object not found or not owned");
        }
        StoredObject so = opt.get();
        if (so.getStorageKey() != null && so.getStorageKey().startsWith("cloudinary:") && cloudinaryMediaService.isReady()) {
            try {
                cloudinaryMediaService.destroyByStorageKey(so.getStorageKey());
            } catch (Exception ignored) {
                /* best-effort delete from Cloudinary */
            }
        } else {
            storage.deleteByStorageKey(so.getStorageKey());
        }
        storedObjectRepository.delete(so);
        return java.util.Map.of("message", "Deleted");
    }

    private CloudDtos.CloudUploadResponse upload(UUID userId, MultipartFile file, String folder) {
        requireUser(userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String safeName = file.getOriginalFilename() != null ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_") : "file";
        String objectKey = userId + "/" + UUID.randomUUID() + "_" + safeName;
        try {
            if (cloudinaryMediaService.isReady()) {
                StorageResult r = cloudinaryMediaService.upload(file, folder);
                StoredObject so = new StoredObject(UUID.randomUUID(), user, r.publicUrl(), r.storageKey(), folder);
                storedObjectRepository.save(so);
                return new CloudDtos.CloudUploadResponse(r.publicUrl(), r.provider());
            }
            try (InputStream in = file.getInputStream()) {
                StorageResult r = storage.store(folder, objectKey, in, file.getSize(), file.getContentType());
                StoredObject so = new StoredObject(UUID.randomUUID(), user, r.publicUrl(), r.storageKey(), folder);
                storedObjectRepository.save(so);
                return new CloudDtos.CloudUploadResponse(r.publicUrl(), r.provider());
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed: " + e.getMessage());
        }
    }

    private static void requireUser(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User context required");
        }
    }
}
