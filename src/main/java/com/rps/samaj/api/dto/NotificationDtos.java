package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int number, int size) {
    }

    public record NotificationResponse(
            String id,
            String title,
            String body,
            String type,
            boolean read,
            String link,
            String createdAt
    ) {
    }

    public record UnreadCountResponse(long unread) {
    }

    public record NotificationPreferencesResponse(
            boolean emailEnabled,
            boolean inAppEnabled,
            boolean securityEmailEnabled
    ) {
    }

    public record NotificationPreferencesUpdateRequest(
            Boolean emailEnabled,
            Boolean inAppEnabled,
            Boolean securityEmailEnabled
    ) {
    }

    public record AdminBroadcastRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String body,
            @Size(max = 32) String type,
            @Size(max = 2000) String link
    ) {
    }

    public record AdminBroadcastResponse(String message, long recipientCountEstimate) {
    }
}
