package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Community / posts — aligned with Frontend {@code communityApi}. */
public final class CommunityAndFeedDtos {

    private CommunityAndFeedDtos() {
    }

    public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    }

    public record CommunityPostMediaDto(long id, String url, String type, int sortOrder) {
    }

    public record CommunityPostTagDto(long id, String name, String slug) {
    }

    public record CommunityPostDto(
            long id,
            String authorUserId,
            String authorDisplayName,
            String authorPhotoUrl,
            String content,
            String location,
            List<String> emojiCodes,
            List<String> mentionedUserIds,
            List<CommunityPostTagDto> tags,
            List<CommunityPostMediaDto> media,
            int likeCount,
            int commentCount,
            int saveCount,
            int shareCount,
            int viewCount,
            String createdAt,
            String updatedAt,
            boolean likedByCurrentUser,
            boolean savedByCurrentUser
    ) {
    }

    public record CommunityCommentDto(long id, long postId, String authorUserId, String content, String createdAt) {
    }

    public record CommunityPostCreateRequest(
            @NotBlank String content,
            String location,
            List<String> emojiCodes,
            List<String> mentionedUserIds,
            List<String> tags,
            List<CommunityPostMediaIn> media
    ) {
    }

    /** Partial update: {@code null} field means leave unchanged; non-null lists replace tags/media. */
    public record CommunityPostPatchRequest(
            String content,
            String location,
            List<String> emojiCodes,
            List<String> mentionedUserIds,
            List<String> tags,
            List<CommunityPostMediaIn> media
    ) {
    }

    public record CommunityPostMediaIn(
            @NotBlank String url,
            @NotNull String type,
            Integer sortOrder
    ) {
    }

    public record CommentCreateRequest(@NotBlank String content) {
    }

    public record ReportRequest(String reason, String details) {
    }

    public record CommunityTagWithCount(long id, String name, String slug, long postCount) {
    }

    public record CommunityAnalytics(
            long totalPosts,
            long totalLikesGiven,
            long totalLikesReceived,
            long totalSaves,
            long totalViews
    ) {
    }
}
