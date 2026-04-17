package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class SuggestionDtos {

    private SuggestionDtos() {
    }

    public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    }

    public record SuggestionResponse(
            String id,
            String title,
            String description,
            String category,
            String status,
            String response,
            String createdAt,
            String updatedAt
    ) {
    }

    public record SuggestionCreateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 8000) String description,
            @NotBlank @Size(max = 64) String category
    ) {
    }
}
