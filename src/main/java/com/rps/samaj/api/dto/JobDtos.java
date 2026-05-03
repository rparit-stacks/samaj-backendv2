package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class JobDtos {

    private JobDtos() {
    }

    public record JobCreateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 200) String company,
            @NotBlank String description,
            @Size(max = 200) String location,
            @Size(max = 30)  String jobType,
            @Size(max = 100) String category,
            String requirements,
            Long salaryMin,
            Long salaryMax,
            @Size(max = 500) String applyUrl,
            @Size(max = 200) String contactEmail,
            @Size(max = 20)  String contactPhone,
            String deadline
    ) {
    }

    public record JobUpdateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 200) String company,
            @NotBlank String description,
            @Size(max = 200) String location,
            @Size(max = 30)  String jobType,
            @Size(max = 100) String category,
            String requirements,
            Long salaryMin,
            Long salaryMax,
            @Size(max = 500) String applyUrl,
            @Size(max = 200) String contactEmail,
            @Size(max = 20)  String contactPhone,
            String deadline
    ) {
    }

    public record JobSummary(
            String id,
            String title,
            String company,
            String location,
            String jobType,
            String category,
            Long salaryMin,
            Long salaryMax,
            String status,
            boolean featured,
            boolean postedByAdmin,
            String deadline,
            long viewCount,
            String createdAt
    ) {
    }

    public record JobDetail(
            String id,
            String title,
            String company,
            String location,
            String jobType,
            String category,
            String description,
            String requirements,
            Long salaryMin,
            Long salaryMax,
            String applyUrl,
            String contactEmail,
            String contactPhone,
            String status,
            String rejectionReason,
            boolean featured,
            boolean postedByAdmin,
            String submittedById,
            String submittedByName,
            String deadline,
            long viewCount,
            boolean isOwner,
            String createdAt,
            String updatedAt
    ) {
    }

    public record JobPageResponse(
            List<JobSummary> content,
            int totalPages,
            long totalElements,
            int size,
            int number,
            boolean first,
            boolean last
    ) {
    }

    public record JobAdminSummary(
            String id,
            String title,
            String company,
            String location,
            String jobType,
            String category,
            String status,
            boolean postedByAdmin,
            boolean featured,
            String submittedById,
            String submittedByName,
            String submittedByEmail,
            String deadline,
            String createdAt,
            String updatedAt
    ) {
    }

    public record JobAdminPageResponse(
            List<JobAdminSummary> content,
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
