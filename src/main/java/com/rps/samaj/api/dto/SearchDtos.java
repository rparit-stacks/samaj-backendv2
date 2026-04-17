package com.rps.samaj.api.dto;

import java.util.List;

public final class SearchDtos {

    private SearchDtos() {
    }

    public record SearchResultDto(
            String service,
            String id,
            String title,
            String subtitle,
            String description,
            String imageUrl,
            String link
    ) {
    }

    public record SearchCategoryResponse(
            String service,
            long total,
            List<SearchResultDto> results
    ) {
    }

    public record SearchAllResponse(
            String query,
            List<SearchCategoryResponse> categories
    ) {
    }
}
