package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class NewsDtos {

    private NewsDtos() {
    }

    public record NewsCategoryResponse(long id, String name, String slug) {
    }

    public record NewsCategoryCreateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 120) String slug
    ) {
    }

    public record NewsCategoryUpdateRequest(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 120) String slug
    ) {
    }

    public record NewsArticleResponse(
            long id,
            String title,
            String summary,
            String content,
            long categoryId,
            String categoryName,
            String categorySlug,
            String imageUrl,
            boolean pinned,
            Instant publishedAt,
            long views
    ) {
    }

    public record NewsArticleAdminResponse(
            long id,
            String title,
            String summary,
            String content,
            long categoryId,
            String categoryName,
            String categorySlug,
            String imageUrl,
            boolean pinned,
            boolean active,
            Instant publishedAt,
            long views
    ) {
    }

    public record NewsArticleCreateRequest(
            @NotBlank @Size(max = 500) String title,
            @NotBlank String summary,
            @NotBlank String content,
            @NotNull Long categoryId,
            @Size(max = 2000) String imageUrl,
            boolean pinned,
            boolean active,
            Instant publishedAt
    ) {
    }

    public record NewsArticleUpdateRequest(
            @NotBlank @Size(max = 500) String title,
            @NotBlank String summary,
            @NotBlank String content,
            @NotNull Long categoryId,
            @Size(max = 2000) String imageUrl,
            boolean pinned,
            boolean active,
            Instant publishedAt
    ) {
    }

    public record PageResponse<T>(
            java.util.List<T> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }
}
