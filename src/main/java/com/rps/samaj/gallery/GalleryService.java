package com.rps.samaj.gallery;

import com.rps.samaj.api.dto.GalleryDtos;
import com.rps.samaj.notification.PublicNotificationPublisher;
import com.rps.samaj.security.JwtAuthenticationFilter;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class GalleryService {

    private final GalleryAlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final PublicNotificationPublisher notificationPublisher;

    public GalleryService(
            GalleryAlbumRepository albumRepository,
            UserRepository userRepository,
            PublicNotificationPublisher notificationPublisher
    ) {
        this.albumRepository = albumRepository;
        this.userRepository = userRepository;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional(readOnly = true)
    public List<GalleryDtos.GalleryAlbumResponse> listApproved() {
        requireUser();
        return albumRepository.findApprovedWithPhotos().stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<GalleryDtos.GalleryAlbumResponse> listMine() {
        UUID uid = requireUserId();
        return albumRepository.findByCreatedBy_IdWithPhotos(uid).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public GalleryDtos.GalleryAlbumDetailResponse getAlbum(UUID id) {
        requireUser();
        GalleryAlbum a = albumRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
        if (!a.isApproved() && !a.getCreatedBy().getId().equals(requireUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found");
        }
        return toDetail(a);
    }

    @Transactional
    public GalleryDtos.GalleryAlbumResponse create(GalleryDtos.GalleryAlbumCreateRequest body) {
        UUID uid = requireUserId();
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String cover = body.coverPhotoUrl() != null && !body.coverPhotoUrl().isBlank()
                ? body.coverPhotoUrl().trim()
                : null;
        List<String> urls = body.photoUrls() == null ? List.of() : body.photoUrls().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (cover == null && !urls.isEmpty()) {
            cover = urls.get(0);
        }
        GalleryAlbum album = new GalleryAlbum(UUID.randomUUID(), user, body.name().trim(), cover, false);
        int order = 0;
        for (String url : urls) {
            album.getPhotos().add(new GalleryPhoto(UUID.randomUUID(), album, url, order++));
        }
        albumRepository.saveAndFlush(album);
        return toSummary(albumRepository.findDetailedById(album.getId()).orElse(album));
    }

    private GalleryDtos.GalleryAlbumResponse toSummary(GalleryAlbum a) {
        long n = a.getPhotos() == null ? 0 : a.getPhotos().size();
        return new GalleryDtos.GalleryAlbumResponse(
                a.getId().toString(),
                a.getName(),
                a.getCoverPhotoUrl(),
                a.getCreatedBy().getId().toString(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                n,
                a.isApproved()
        );
    }

    private GalleryDtos.GalleryAlbumDetailResponse toDetail(GalleryAlbum a) {
        List<String> urls = new ArrayList<>();
        if (a.getPhotos() != null) {
            a.getPhotos().stream()
                    .sorted(Comparator.comparingInt(GalleryPhoto::getSortOrder))
                    .forEach(p -> urls.add(p.getUrl()));
        }
        return new GalleryDtos.GalleryAlbumDetailResponse(
                a.getId().toString(),
                a.getName(),
                a.getCoverPhotoUrl(),
                a.getCreatedBy().getId().toString(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                urls,
                a.isApproved()
        );
    }

    private static void requireUser() {
        if (JwtAuthenticationFilter.currentUserIdOrNull() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private static UUID requireUserId() {
        UUID id = JwtAuthenticationFilter.currentUserIdOrNull();
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return id;
    }

    @Transactional(readOnly = true)
    public List<GalleryDtos.AdminGalleryAlbumResponse> adminList() {
        return albumRepository.findAllForAdmin().stream().map(this::toAdminResponse).toList();
    }

    @Transactional(readOnly = true)
    public GalleryDtos.AdminGalleryAlbumResponse adminGet(UUID id) {
        GalleryAlbum a = albumRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
        return toAdminResponse(a);
    }

    @Transactional
    public GalleryDtos.AdminGalleryAlbumResponse adminUpdate(UUID id, GalleryDtos.GalleryAlbumUpdateRequest body) {
        GalleryAlbum a = albumRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
        boolean wasApproved = a.isApproved();
        if (body.name() != null && !body.name().isBlank()) {
            a.setName(body.name().trim());
        }
        if (body.coverPhotoUrl() != null) {
            a.setCoverPhotoUrl(body.coverPhotoUrl().isBlank() ? null : body.coverPhotoUrl().trim());
        }
        if (body.photoUrls() != null) {
            a.getPhotos().clear();
            int order = 0;
            for (String url : body.photoUrls()) {
                if (url != null && !url.isBlank()) {
                    a.getPhotos().add(new GalleryPhoto(UUID.randomUUID(), a, url.trim(), order++));
                }
            }
        }
        if (body.approved() != null) {
            a.setApproved(body.approved());
        }
        albumRepository.saveAndFlush(a);
        GalleryAlbum fresh = albumRepository.findDetailedById(id).orElse(a);
        if (fresh.isApproved() && !wasApproved) {
            notificationPublisher.onGalleryAlbumCreated(
                    fresh.getId(),
                    fresh.getCreatedBy().getId(),
                    fresh.getName()
            );
        }
        return toAdminResponse(fresh);
    }

    @Transactional
    public void adminDelete(UUID id) {
        if (!albumRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found");
        }
        albumRepository.deleteById(id);
    }

    private GalleryDtos.AdminGalleryAlbumResponse toAdminResponse(GalleryAlbum a) {
        List<String> urls = new ArrayList<>();
        if (a.getPhotos() != null) {
            a.getPhotos().stream()
                    .sorted(Comparator.comparingInt(GalleryPhoto::getSortOrder))
                    .forEach(p -> urls.add(p.getUrl()));
        }
        long n = urls.size();
        return new GalleryDtos.AdminGalleryAlbumResponse(
                a.getId().toString(),
                a.getName(),
                a.getCoverPhotoUrl(),
                a.getCreatedBy().getEmail(),
                a.getCreatedBy().getId().toString(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                n,
                a.isApproved(),
                urls
        );
    }
}
