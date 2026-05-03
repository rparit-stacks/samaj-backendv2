package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class BusinessDtos {

    private BusinessDtos() {
    }

    public record BusinessCreateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @Size(max = 100) String category,
            @Size(max = 20) String phone,
            @Size(max = 200) String email,
            @Size(max = 500) String address,
            @Size(max = 100) String city,
            @Size(max = 500) String website,
            @Size(max = 10) List<String> photos
    ) {
    }

    public record BusinessUpdateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @Size(max = 100) String category,
            @Size(max = 20) String phone,
            @Size(max = 200) String email,
            @Size(max = 500) String address,
            @Size(max = 100) String city,
            @Size(max = 500) String website,
            @Size(max = 10) List<String> photos
    ) {
    }

    public record BusinessSummary(
            String id,
            String name,
            String category,
            String city,
            String phone,
            String firstPhoto,
            String status,
            String ownerId,
            String ownerName,
            String ownerAvatar,
            boolean featured,
            long viewCount,
            String createdAt
    ) {
    }

    public record BusinessDetail(
            String id,
            String name,
            String description,
            String category,
            String phone,
            String email,
            String address,
            String city,
            String website,
            List<String> photos,
            String status,
            String rejectionReason,
            String ownerId,
            String ownerName,
            String ownerAvatar,
            String ownerProfileKey,
            boolean featured,
            long viewCount,
            boolean isOwner,
            String createdAt,
            String updatedAt
    ) {
    }

    public record BusinessPageResponse(
            List<BusinessSummary> content,
            int totalPages,
            long totalElements,
            int size,
            int number,
            boolean first,
            boolean last
    ) {
    }

    public record BusinessAdminSummary(
            String id,
            String name,
            String category,
            String city,
            String status,
            String ownerId,
            String ownerName,
            String ownerEmail,
            boolean featured,
            String createdAt,
            String updatedAt
    ) {
    }

    public record BusinessAdminPageResponse(
            List<BusinessAdminSummary> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    public record AdminApproveRequest(Boolean featured) {
    }

    public record AdminRejectRequest(@NotBlank @Size(max = 500) String reason) {
    }
}
