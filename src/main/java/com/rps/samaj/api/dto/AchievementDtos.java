package com.rps.samaj.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AchievementDtos {

    private AchievementDtos() {
    }

    public record AchievementFieldItem(
            @NotBlank @Size(max = 64) String id,
            @NotBlank @Size(max = 32) String type,
            @NotBlank @Size(max = 120) String label,
            @Size(max = 12000) String value
    ) {
    }

    public record AchievementCreateRequest(
            @NotBlank @Size(max = 200) String headline,
            @NotNull @Size(min = 1, max = 40) List<AchievementFieldItem> fields
    ) {
    }

    public record AchievementUpdateRequest(
            @NotBlank @Size(max = 200) String headline,
            @NotNull @Size(min = 1, max = 40) List<AchievementFieldItem> fields
    ) {
    }

    public record AchievementUserSummary(
            String id,
            String headline,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    public record AchievementMarqueeCard(
            String id,
            String headline,
            String userId,
            String userName,
            String userAvatarUrl
    ) {
    }

    public record AchievementDetailResponse(
            String id,
            String headline,
            List<AchievementFieldItem> fields,
            String status,
            String rejectionReason,
            String createdAt,
            String updatedAt,
            String userId,
            String userName,
            String userAvatarUrl,
            String userProfileKey,
            boolean marqueeEnabled,
            String marqueeStart,
            String marqueeEnd
    ) {
    }

    public record AchievementPageResponse(
            List<AchievementDetailResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    public record AchievementAdminSummary(
            String id,
            String headline,
            String status,
            String userId,
            String userEmail,
            String userName,
            boolean marqueeEnabled,
            String marqueeStart,
            String marqueeEnd,
            String createdAt,
            String updatedAt
    ) {
    }

    public record AchievementAdminPageResponse(
            List<AchievementAdminSummary> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    public record AchievementApproveRequest(
            @Min(1) @Max(30) Integer marqueeDays,
            Boolean marqueeEnabled
    ) {
    }

    public record AchievementRejectRequest(
            @NotBlank @Size(max = 2000) String reason
    ) {
    }

    public record AchievementAdminUpdateRequest(
            @NotBlank @Size(max = 200) String headline,
            @NotNull @Size(min = 1, max = 40) List<AchievementFieldItem> fields,
            @NotBlank @Size(max = 20) String status,
            Boolean marqueeEnabled,
            String marqueeStart,
            String marqueeEnd,
            @Size(max = 2000) String rejectionReason
    ) {
    }

    public record AchievementMarqueeAdminPatch(
            Boolean marqueeEnabled,
            String marqueeEnd
    ) {
    }

    public record AchievementFieldTemplateResponse(
            String id,
            String name,
            String schemaJson,
            boolean active,
            String createdAt,
            String updatedAt
    ) {
    }

    public record AchievementFieldTemplateCreateRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 8000) String schemaJson
    ) {
    }

    public record AchievementFieldTemplateUpdateRequest(
            @Size(max = 160) String name,
            @Size(max = 8000) String schemaJson,
            Boolean active
    ) {
    }
}
