package com.rps.samaj.event.web;

import com.rps.samaj.api.dto.EventDtos;
import com.rps.samaj.event.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventDtos.EventItemResponse> list(
            @RequestParam(required = false) UUID organizerId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sort
    ) {
        return eventService.list(organizerId, type, sort);
    }

    @GetMapping("/{id}")
    public EventDtos.EventDetailResponse getById(@PathVariable long id) {
        return eventService.getDetail(id);
    }

    @GetMapping("/{id}/analytics")
    public EventDtos.EventAnalyticsResponse analytics(@PathVariable long id) {
        return eventService.analyticsForOrganizer(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDtos.EventItemResponse create(
            Authentication auth,
            @Valid @RequestBody EventDtos.EventCreateRequest body
    ) {
        requireUser(auth);
        return eventService.create(body);
    }

    @PostMapping("/{id}/rsvp")
    public EventDtos.EventItemResponse rsvp(
            Authentication auth,
            @PathVariable long id,
            @Valid @RequestBody EventDtos.EventRsvpRequest body
    ) {
        requireUser(auth);
        return eventService.rsvp(id, body);
    }

    private static void requireUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
