package com.rps.samaj.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class ExamDtos {

    private ExamDtos() {
    }

    public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    }

    public record ExamResponse(
            String id,
            String title,
            String description,
            String type,
            String notificationDate,
            String lastDate,
            String examDate,
            String eligibility,
            String applyUrl,
            boolean expired,
            boolean saved,
            boolean alertEnabled,
            String createdAt,
            String updatedAt,
            JsonNode paper
    ) {
    }

    public record MessageResponse(String message) {
    }

    public record ExamCreateRequest(
            @NotBlank String title,
            @NotBlank String description,
            @NotBlank String type,
            String notificationDate,
            String lastDate,
            String examDate,
            String eligibility,
            String applyUrl,
            boolean expired,
            JsonNode paper
    ) {
    }

    public record ExamUpdateRequest(
            String title,
            String description,
            String type,
            String notificationDate,
            String lastDate,
            String examDate,
            String eligibility,
            String applyUrl,
            Boolean expired,
            JsonNode paper
    ) {
    }
}
