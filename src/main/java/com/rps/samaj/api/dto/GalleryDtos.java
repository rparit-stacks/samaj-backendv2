package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class GalleryDtos {

    private GalleryDtos() {
    }

    public record GalleryAlbumResponse(String id, String name, String coverPhotoUrl, String createdBy, String createdAt,
                                       long photoCount, boolean approved) {
    }

    public record GalleryAlbumDetailResponse(String id, String name, String coverPhotoUrl, String createdBy,
                                             String createdAt, List<String> photoUrls, boolean approved) {
    }

    public record GalleryAlbumCreateRequest(@NotBlank @Size(max = 200) String name,
                                            @Size(max = 2000) String coverPhotoUrl,
                                            List<@NotBlank @Size(max = 2000) String> photoUrls) {
    }

    public record GalleryAlbumUpdateRequest(String name,
                                            String coverPhotoUrl,
                                            List<String> photoUrls,
                                            Boolean approved) {
    }

    public record AdminGalleryAlbumResponse(String id, String name, String coverPhotoUrl, String createdByName,
                                            String createdById, String createdAt, long photoCount, boolean approved,
                                            List<String> photoUrls) {
    }
}
