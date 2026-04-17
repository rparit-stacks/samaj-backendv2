package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class AdminDirectoryDtos {

    private AdminDirectoryDtos() {
    }

    public record DirectoryEntrySummary(
            String userId,
            String fullName,
            String city,
            boolean visible,
            boolean showInDirectory,
            List<DirectoryDtos.DirectoryActionDto> actions
    ) {
    }

    public record DirectoryEntryDetail(
            String userId,
            String fullName,
            String city,
            String profession,
            String phone,
            String email,
            boolean visible,
            boolean showInDirectory,
            List<DirectoryDtos.DirectoryActionDto> actions
    ) {
    }

    public record DirectoryEntryUpdateRequest(
            @NotNull Boolean visible,
            Boolean showInDirectory,
            List<DirectoryDtos.DirectoryActionDto> actions
    ) {
    }
}

