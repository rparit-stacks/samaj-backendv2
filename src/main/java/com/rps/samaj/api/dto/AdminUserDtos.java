package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class AdminUserDtos {

    private AdminUserDtos() {
    }

    public record UserSummary(
            String id,
            String email,
            String phone,
            String fullName,
            String profileKey,
            String role,
            String status,
            String kycStatus,
            boolean emailVerified,
            boolean phoneVerified,
            boolean parentAdmin,
            String createdAt,
            String updatedAt
    ) {
    }

    public record UserPageResponse(
            List<UserSummary> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    /** Full account + profile + privacy/settings as seen by main admins (not redacted). */
    public record UserFullDetail(
            String id,
            String email,
            String phone,
            String role,
            String status,
            String kycStatus,
            boolean emailVerified,
            boolean phoneVerified,
            boolean parentAdmin,
            String adminServiceKeysCsv,
            boolean passwordSet,
            String googleId,
            String metadata,
            String createdAt,
            String updatedAt,
            String fullName,
            String profileKey,
            String bio,
            String city,
            String profession,
            String bloodGroup,
            String avatarUrl,
            String coverImageUrl,
            boolean settingsShowPhone,
            boolean settingsShowInDirectory,
            boolean settingsEmergencyAlerts,
            boolean settingsTwoFactorEnabled,
            boolean settingsLoginAlertsEnabled,
            boolean privacyShowEmail,
            boolean privacyShowBloodGroup,
            boolean privacyShowPhone,
            boolean privacyShowFamilyMembers,
            String profileVisibility,
            String servicePrivacyJson,
            boolean securityTwoFactorEnabled,
            boolean securityLoginAlertsEnabled,
            boolean notificationEmailEnabled,
            boolean notificationInAppEnabled,
            boolean notificationSecurityEmailEnabled
    ) {
    }

    public record UserCreateRequest(
            String name,
            @NotBlank String email,
            String phone,
            String password,
            String role,
            String status
    ) {
    }

    public record UserCreateResponse(
            UserSummary user,
            String tempPassword
    ) {
    }

    public record UserUpdateRequest(
            String name,
            String email,
            String phone,
            String role,
            String status,
            Boolean emailVerified,
            Boolean phoneVerified
    ) {
    }
}
