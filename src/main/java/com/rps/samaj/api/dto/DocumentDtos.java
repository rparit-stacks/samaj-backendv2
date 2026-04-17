package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class DocumentDtos {

    private DocumentDtos() {
    }

    public record DocumentResponse(
            String id,
            String title,
            String description,
            String fileUrl,
            String fileName,
            Long fileSize,
            String fileType,
            String category,
            String visibility,
            String createdBy,
            String createdAt,
            boolean approved,
            String rejectionReason,
            long downloadCount
    ) {
    }

    public record DocumentCreateRequest(
            @NotBlank @Size(max = 500) String title,
            String description,
            @NotBlank @Size(max = 2000) String fileUrl,
            @NotBlank @Size(max = 500) String fileName,
            Long fileSize,
            String fileType,
            @NotBlank @Size(max = 64) String category,
            String visibility
    ) {
    }

    public record DocumentApprovalRequest(boolean approved, String rejectionReason) {
    }

    /** Partial update for admin moderation (null fields = leave unchanged). */
    public record DocumentAdminUpdateRequest(
            String title,
            String description,
            String category,
            String visibility,
            Boolean approved,
            String rejectionReason
    ) {
    }

    public record DownloadUrlResponse(String fileUrl) {
    }
}
