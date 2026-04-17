package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class HistoryDtos {

    private HistoryDtos() {
    }

    public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    }

    public record HistoryCreateRequest(
            @NotBlank @Size(max = 500) String title,
            @NotBlank @Size(max = 64) String type,
            @NotNull LocalDate date,
            @Size(max = 32) String time,
            @NotBlank @Size(max = 500) String location,
            String description,
            @Size(max = 2000) String imageUrl
    ) {
    }

    public record HistoryUpdateRequest(
            @NotBlank @Size(max = 500) String title,
            @NotBlank @Size(max = 64) String type,
            @NotNull LocalDate date,
            @Size(max = 32) String time,
            @NotBlank @Size(max = 500) String location,
            String description,
            @Size(max = 2000) String imageUrl
    ) {
    }

    public record HistoryResponse(
            long id,
            String title,
            String type,
            String date,
            String time,
            String location,
            String description,
            String imageUrl,
            String createdByUserId,
            String createdAt,
            String updatedAt
    ) {
    }
}

