package com.rps.samaj.api.dto;

import java.util.List;
import java.util.Map;

/** Mirrors Frontend UserProfile, PublicProfileResponse, family, settings, privacy, security. */
public final class UserProfileDtos {

    private UserProfileDtos() {
    }

    public record UserProfileResponse(
            String userId,
            String profileKey,
            String fullName,
            String city,
            String profession,
            String bio,
            String avatarUrl,
            String coverImageUrl,
            String email,
            String phone,
            String bloodGroup
    ) {
    }

    public record FamilyMemberResponse(String id, String name, String relation, String city, String phone, String email) {
    }

    public record FamilyMemberRequest(String name, String relation, String city, String phone, String email) {
    }

    public record UserSettingsResponse(boolean showPhone, boolean showInDirectory, boolean emergencyAlerts) {
    }

    public record PrivacySettingsResponse(
            boolean showEmail,
            boolean showBloodGroup,
            Boolean showPhone,
            Boolean showFamilyMembers,
            String profileVisibility,
            Map<String, Object> servicePrivacy
    ) {
    }

    public record SecuritySettingsResponse(boolean twoFactorEnabled, boolean loginAlertsEnabled) {
    }

    public record PublicProfileResponse(
            String userId,
            String profileKey,
            String fullName,
            String city,
            String profession,
            String bio,
            String avatarUrl,
            String coverImageUrl,
            String email,
            String phone,
            String bloodGroup,
            List<FamilySummary> familyMembers,
            boolean privateProfile,
            boolean showEventsOnProfile,
            boolean showCommunityOnProfile,
            boolean showEmergenciesOnProfile
    ) {
    }

    public record FamilySummary(String name, String relation) {
    }

    public record ContactInfoResponse(String phone, String email, String bloodGroup) {
    }

    public record VisibleProfileResponse(
            String userId,
            String displayName,
            String photoUrl,
            String email,
            String phone,
            String city,
            String profession,
            List<FamilySummary> familyMembers,
            Boolean showLocation
    ) {
    }

    public record PaginatedUserProfiles(
            List<UserProfileResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    /** Partial update for PUT /api/v1/users/me/profile */
    public record UserProfilePatch(
            String fullName,
            String city,
            String profession,
            String bio,
            String avatarUrl,
            String coverImageUrl,
            String bloodGroup
    ) {
    }

    public record PrivacyPatch(
            Boolean showEmail,
            Boolean showBloodGroup,
            Boolean showPhone,
            Boolean showFamilyMembers,
            String profileVisibility,
            Map<String, Object> servicePrivacy
    ) {
    }

    public record SecurityPatch(Boolean twoFactorEnabled, Boolean loginAlertsEnabled) {
    }

    public record ContactRequestCreate(String targetUserId, String message) {
    }

    public record ContactRequestRespond(boolean approve) {
    }

    public record ContactRequestItem(
            String id,
            String requesterUserId,
            String targetUserId,
            String requesterName,
            String requesterAvatarUrl,
            String targetName,
            String targetAvatarUrl,
            String status,
            String message,
            String createdAt,
            String respondedAt
    ) {
    }

    public record DirectoryEntry(
            String userId,
            String fullName,
            String city,
            String profession,
            String avatarUrl,
            String phone,
            String bloodGroup
    ) {
    }
}
