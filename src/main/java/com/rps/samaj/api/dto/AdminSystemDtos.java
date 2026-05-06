package com.rps.samaj.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AdminSystemDtos {

    private AdminSystemDtos() {
    }

    // ── Catalog / Me ──────────────────────────────────────────────────────────

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

    // ── Child admin CRUD ──────────────────────────────────────────────────────

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

    // ── Invitation flow ───────────────────────────────────────────────────────

    /** Request body for the parent admin when inviting a new sub-admin. */
    public record ChildAdminInviteRequest(
            @NotBlank @Email String email,
            @NotEmpty List<@NotBlank String> serviceKeys
    ) {
    }

    /** Returned to the parent admin after sending an invitation. */
    public record InvitationResponse(
            String id,
            String email,
            List<String> serviceKeys,
            String createdAt,
            String expiresAt,
            boolean accepted
    ) {
    }

    /** Returned to the public invite-accept page when it loads the token. */
    public record InvitationDetailsResponse(
            String email,
            List<String> serviceKeys,
            String expiresAt
    ) {
    }

    /** Returned after OTP verification; mirrors the standard auth login response. */
    public record AuthTokenResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserInfo user
    ) {
        public record UserInfo(String id, String role) {
        }
    }
}
