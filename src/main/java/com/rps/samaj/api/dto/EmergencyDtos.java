package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class EmergencyDtos {

    private EmergencyDtos() {
    }

    public record EmergencyContactPreferencesResponse(
            String phone,
            String whatsapp,
            String email,
            boolean allowPhone,
            boolean allowWhatsapp,
            boolean allowEmail
    ) {
    }

    public record EmergencyItemResponse(
            long id,
            String creatorUserId,
            String creatorDisplayName,
            String creatorPhotoUrl,
            String type,
            String title,
            String description,
            String area,
            String city,
            String state,
            String country,
            String landmark,
            String locationDescription,
            Double latitude,
            Double longitude,
            String status,
            Instant emergencyAt,
            Instant createdAt,
            Instant updatedAt,
            int helperCount,
            int viewCount,
            int contactClickCount,
            boolean resolvedByExternal,
            String externalHelperNote,
            EmergencyContactPreferencesResponse contactPreferences
    ) {
    }

    public record EmergencyCreateRequest(
            @Size(max = 32) String type,
            @NotBlank @Size(max = 500) String title,
            @NotBlank String description,
            @Size(max = 200) String area,
            @NotBlank @Size(max = 200) String city,
            @NotBlank @Size(max = 200) String state,
            @NotBlank @Size(max = 200) String country,
            @Size(max = 500) String landmark,
            String locationDescription,
            Double latitude,
            Double longitude,
            Instant emergencyAt,
            @Size(max = 64) String contactPhone,
            @Size(max = 64) String contactWhatsapp,
            @Size(max = 320) String contactEmail,
            Boolean allowPhone,
            Boolean allowWhatsapp,
            Boolean allowEmail
    ) {
    }

    public record EmergencyUpdateRequest(
            @Size(max = 32) String type,
            @Size(max = 500) String title,
            String description,
            @Size(max = 200) String area,
            @Size(max = 200) String city,
            @Size(max = 200) String state,
            @Size(max = 200) String country,
            @Size(max = 500) String landmark,
            String locationDescription,
            Double latitude,
            Double longitude,
            Instant emergencyAt,
            @Size(max = 64) String contactPhone,
            @Size(max = 64) String contactWhatsapp,
            @Size(max = 320) String contactEmail,
            Boolean allowPhone,
            Boolean allowWhatsapp,
            Boolean allowEmail
    ) {
    }

    public record EmergencyStatusPatchRequest(@NotBlank @Size(max = 32) String status) {
    }

    public record EmergencyResolveRequest(
            String helperUserId,
            boolean externalHelper,
            String externalHelperNote,
            String note
    ) {
    }

    public record EmergencyHelpItemResponse(
            long emergencyId,
            String helperUserId,
            Instant helpedAt,
            String note
    ) {
    }

    public record DashboardStatsResponse(
            long totalEmergenciesCreated,
            long activeEmergencies,
            long resolvedEmergencies,
            long totalContactClicks,
            long totalViews,
            long totalPeopleHelped
    ) {
    }

    public record HelperStatsResponse(
            String helperUserId,
            long totalHelps,
            long distinctPeopleHelped,
            Instant firstHelpAt,
            Instant lastHelpAt
    ) {
    }
}
