package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class DirectoryDtos {

    private DirectoryDtos() {
    }

    public record DirectoryActionDto(String type, String label, String value, int sortOrder) {
    }

    public record DirectoryProfileSummary(String userId, String fullName, String photoUrl, String city,
                                          List<DirectoryActionDto> actions) {
    }

    public record DirectoryProfileDetail(String userId, String fullName, String phone, String email, String photoUrl,
                                         String city, String profession, String bio, String bloodGroup,
                                         List<DirectoryActionDto> actions) {
    }

    public record DirectorySettingsDto(boolean visible, List<DirectoryActionDto> actions) {
    }

    public record DirectorySettingsUpdateDto(@NotNull Boolean visible, List<DirectoryActionDto> actions) {
    }
}
