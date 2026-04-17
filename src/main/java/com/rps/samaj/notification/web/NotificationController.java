package com.rps.samaj.notification.web;

import com.rps.samaj.api.dto.NotificationDtos;
import com.rps.samaj.notification.NotificationService;
import com.rps.samaj.security.JwtAuthenticationFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationDtos.PageResponse<NotificationDtos.NotificationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly
    ) {
        UUID uid = requireUser();
        return notificationService.list(uid, page, size, Boolean.TRUE.equals(unreadOnly));
    }

    @GetMapping("/unread")
    public NotificationDtos.UnreadCountResponse unread() {
        return notificationService.unreadCount(requireUser());
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable("id") UUID id) {
        notificationService.markRead(requireUser(), id);
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead() {
        notificationService.markAllRead(requireUser());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") UUID id) {
        notificationService.deleteOne(requireUser(), id);
    }

    @DeleteMapping("/clear-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAll() {
        notificationService.clearAll(requireUser());
    }

    @GetMapping("/preferences")
    public NotificationDtos.NotificationPreferencesResponse getPreferences() {
        return notificationService.getPreferences(requireUser());
    }

    @PutMapping("/preferences")
    public NotificationDtos.NotificationPreferencesResponse updatePreferences(
            @Valid @RequestBody NotificationDtos.NotificationPreferencesUpdateRequest body
    ) {
        return notificationService.updatePreferences(requireUser(), body);
    }

    private static UUID requireUser() {
        UUID id = JwtAuthenticationFilter.currentUserIdOrNull();
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return id;
    }
}
