package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AdminCommunityDtos {

    private AdminCommunityDtos() {
    }

    public record TagCreateRequest(@NotBlank @Size(max = 100) String name) {
    }

    public record TagResponse(long id, String name, String slug) {
    }

    public record ReportResponse(
            long id,
            long postId,
            String postAuthorUserId,
            String reporterUserId,
            String reason,
            String details,
            String status,
            String createdAt,
            String reviewedAt,
            String reviewedByUserId
    ) {
    }

    public record ReportPageResponse(
            List<ReportResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    public record ReportReviewRequest(@NotBlank String status) {
    }
}

