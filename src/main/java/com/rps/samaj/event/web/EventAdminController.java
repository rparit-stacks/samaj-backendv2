package com.rps.samaj.event.web;

import com.rps.samaj.api.dto.EventDtos;
import com.rps.samaj.event.EventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/events")
public class EventAdminController {

    private final EventService eventService;

    public EventAdminController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventDtos.EventItemResponse> list(
            @RequestParam(required = false) UUID organizerId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String sort
    ) {
        return eventService.listForAdmin(organizerId, type, sort);
    }

    @GetMapping("/{id}")
    public EventDtos.EventDetailResponse getById(@PathVariable long id) {
        return eventService.getDetailForAdmin(id);
    }

    @GetMapping("/{id}/analytics")
    public EventDtos.EventAnalyticsResponse analytics(@PathVariable long id) {
        return eventService.analyticsForAdmin(id);
    }
}
