package com.rps.samaj.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AdminSystemDtos {

    private AdminSystemDtos() {
    }

    public record ServiceCatalogEntry(String key, String description, String adminPathPrefix) {
    }

    public record AdminMeResponse(
            String userId,
            String role,
            boolean parentAdmin,
            boolean fullAccess,
            List<String> assignedServiceKeys
    ) {
    }

    public record ChildAdminSummaryResponse(
            String id,
            String email,
            String phone,
            String status,
            List<String> serviceKeys
    ) {
    }

    public record ChildAdminPageResponse(
            List<ChildAdminSummaryResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    public record ChildAdminCreateRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 200) String password,
            String phone,
            @NotEmpty List<@NotBlank String> serviceKeys
    ) {
    }

    public record ChildAdminUpdateRequest(
            @Email String email,
            @Size(min = 8, max = 200) String newPassword,
            String phone,
            String status,
            List<@NotBlank String> serviceKeys
    ) {
    }
}
