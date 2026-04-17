package com.rps.samaj.api.dto;

import java.util.List;
import java.util.Map;

/** Mirrors Frontend matrimony types (subset + extension maps for wizard PUT). */
public final class MatrimonyDtos {

    private MatrimonyDtos() {
    }

    public record MatrimonyProfileSummaryItem(
            String id,
            String displayName,
            String status,
            int draftStep,
            String profileSubject,
            String relativeRelation,
            int completionPercent
    ) {
    }

    public record MatrimonyMeSummary(
            boolean canBrowse,
            int activeProfileCount,
            int draftProfileCount,
            List<MatrimonyProfileSummaryItem> profiles
    ) {
    }

    public record MatrimonyDashboard(
            long interestsSent,
            long interestsReceived,
            long interestsAccepted,
            long profileViewsTotal,
            long shortlistCount,
            long blockedUsersCount
    ) {
    }

    public record MatrimonyProfileCard(
            String id,
            String displayName,
            int age,
            String gender,
            String profession,
            String education,
            String city,
            Integer heightCm,
            String primaryPhotoUrl,
            String bioShort,
            boolean verified,
            Boolean favorited
    ) {
    }

    public record MatrimonyProfileDetail(
            String id,
            String ownerUserId,
            String profileSubject,
            String relativeRelation,
            String displayName,
            int age,
            String gender,
            String dateOfBirth,
            Integer heightCm,
            Integer weightKg,
            String maritalStatus,
            String religion,
            String motherTongue,
            String caste,
            String profession,
            String company,
            String education,
            String college,
            String incomeBracket,
            String city,
            String state,
            String country,
            String nativePlace,
            String bio,
            List<String> hobbies,
            String smoking,
            String drinking,
            List<String> photoUrls,
            boolean photosLimited,
            Map<String, Object> family,
            Map<String, Object> partnerPreferences,
            String partnerOtherExpectations,
            Map<String, Object> privacy,
            String status,
            int draftStep,
            boolean verified,
            Integer completionPercent,
            String createdAt,
            String updatedAt,
            String lastActiveAt
    ) {
    }

    public record MatrimonyInterestResponse(
            String id,
            String fromProfileId,
            String toProfileId,
            String message,
            String status,
            String createdAt,
            String updatedAt
    ) {
    }

    public record MatrimonyConversationResponse(String id, String profileIdLower, String profileIdHigher, String createdAt) {
    }

    public record MatrimonyChatMessageResponse(
            String id,
            String conversationId,
            String senderProfileId,
            String senderUserId,
            String content,
            String createdAt,
            String readAt
    ) {
    }

    public record MatrimonyCreateProfileRequest(
            String displayName,
            String gender,
            String dateOfBirth,
            String profileSubject,
            String relativeRelation,
            Integer heightCm,
            String city,
            String state,
            String country
    ) {
    }

    public record MatrimonySendInterestRequest(
            String fromProfileId,
            String toProfileId,
            String message
    ) {
    }

    public record MatrimonyOpenConversationRequest(
            String myProfileId,
            String otherProfileId
    ) {
    }

    public record MatrimonySendMessageRequest(
            String senderProfileId,
            String content
    ) {
    }

    public record MatrimonyFavoriteToggleResponse(boolean favorited) {
    }

    public record MatrimonyChatWebhookRequest(
            String eventType,
            String conversationId,
            String senderProfileId,
            String content
    ) {
    }

    public record WebhookAckResponse(String status, String message) {
    }

    public record PaginatedMatrimonyProfiles(
            List<MatrimonyProfileCard> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    public record PaginatedMatrimonyInterests(
            List<MatrimonyInterestResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    public record PaginatedMatrimonyMessages(
            List<MatrimonyChatMessageResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    // Admin DTOs for matrimony profile management
    public record AdminMatrimonyProfileResponse(
            String id,
            String ownerEmail,
            String ownerUserId,
            String displayName,
            String gender,
            int age,
            String city,
            String state,
            String profileStatus,
            boolean verified,
            boolean visibleInSearch,
            Integer completionPercent,
            int photoCount,
            String createdAt,
            String lastActiveAt
    ) {
    }

    public record AdminMatrimonyProfileDetailResponse(
            String id,
            String ownerEmail,
            String ownerUserId,
            String displayName,
            String gender,
            int age,
            String dateOfBirth,
            Integer heightCm,
            Integer weightKg,
            String profileSubject,
            String relativeRelation,
            String city,
            String state,
            String country,
            String bio,
            List<String> hobbies,
            List<String> photoUrls,
            boolean verified,
            boolean visibleInSearch,
            Integer completionPercent,
            String profileStatus,
            String createdAt,
            String updatedAt,
            String lastActiveAt
    ) {
    }

    public record AdminMatrimonyAnalyticsResponse(
            long totalProfiles,
            long activeProfiles,
            long draftProfiles,
            long pausedProfiles,
            long verifiedProfiles,
            double verificationRate,
            long hiddenProfiles,
            long totalInterests,
            long totalConversations,
            long blockCount
    ) {
    }

    public record PaginatedAdminMatrimonyProfiles(
            List<AdminMatrimonyProfileResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    // Admin Interest & Safety DTOs
    public record AdminMatrimonyInterestResponse(
            String id,
            String fromProfileId,
            String fromProfileName,
            String fromUserEmail,
            String toProfileId,
            String toProfileName,
            String toUserEmail,
            String message,
            String status,
            String createdAt
    ) {
    }

    public record AdminMatrimonyBlockResponse(
            String id,
            String ownerEmail,
            String ownerUserId,
            String blockedEmail,
            String blockedUserId,
            String createdAt
    ) {
    }

    public record PaginatedAdminMatrimonyInterests(
            List<AdminMatrimonyInterestResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    public record PaginatedAdminMatrimonyBlocks(
            List<AdminMatrimonyBlockResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    // Admin Content Moderation DTOs
    public record AdminMatrimonyPhotoReviewDto(
            String profileId,
            String profileName,
            String ownerEmail,
            String photoUrl,
            boolean flagged,
            String createdAt
    ) {
    }

    public record AdminMatrimonyBioReviewDto(
            String profileId,
            String profileName,
            String ownerEmail,
            String bio,
            boolean flagged,
            String createdAt
    ) {
    }

    public record PaginatedAdminMatrimonyPhotos(
            List<AdminMatrimonyPhotoReviewDto> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }

    public record PaginatedAdminMatrimonyBios(
            List<AdminMatrimonyBioReviewDto> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {
    }
}
