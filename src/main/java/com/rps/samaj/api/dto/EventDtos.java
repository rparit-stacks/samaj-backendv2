package com.rps.samaj.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class EventDtos {

    private EventDtos() {
    }

    public record EventOrganizerResponse(
            String userId,
            String displayName,
            String photoUrl
    ) {
    }

    public record EventScheduleItem(String time, String activity) {
    }

    public record EventItemResponse(
            long id,
            String title,
            String type,
            String date,
            String time,
            String location,
            String description,
            String imageUrl,
            EventOrganizerResponse organizer,
            String scheduleJson,
            long goingCount,
            long interestedCount,
            long notGoingCount,
            String currentUserRsvpStatus,
            String createdAt
    ) {
    }

    public record EventAttendeeResponse(
            String userId,
            String displayName,
            String photoUrl,
            String status,
            String email,
            String phone
    ) {
    }

    public record EventDetailResponse(
            long id,
            String title,
            String type,
            String date,
            String time,
            String location,
            String description,
            String imageUrl,
            EventOrganizerResponse organizer,
            String scheduleJson,
            List<EventScheduleItem> schedule,
            long goingCount,
            long interestedCount,
            long notGoingCount,
            String currentUserRsvpStatus,
            String createdAt,
            List<EventAttendeeResponse> goingAttendees,
            boolean isOrganizer
    ) {
    }

    public record EventAnalyticsResponse(
            long eventId,
            long goingCount,
            long interestedCount,
            long notGoingCount,
            List<EventAttendeeResponse> goingAttendees,
            List<EventAttendeeResponse> interestedAttendees,
            List<EventAttendeeResponse> notGoingAttendees
    ) {
    }

    public record EventCreateRequest(
            @NotBlank @Size(max = 500) String title,
            @NotBlank @Size(max = 64) String type,
            @NotNull LocalDate date,
            @Size(max = 32) String time,
            @NotBlank @Size(max = 500) String location,
            String description,
            @Size(max = 2000) String imageUrl,
            String organizerDisplayName,
            String organizerPhotoUrl,
            List<EventScheduleItem> schedule
    ) {
    }

    public record EventRsvpRequest(
            @NotBlank @Size(max = 32) String status,
            @Size(max = 200) String displayName,
            @Size(max = 2000) String photoUrl
    ) {
    }
}
