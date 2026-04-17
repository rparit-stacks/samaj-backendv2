package com.rps.samaj.notification.web;

import com.rps.samaj.api.dto.NotificationDtos;
import com.rps.samaj.notification.PublicNotificationPublisher;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    private final PublicNotificationPublisher publisher;
    private final UserRepository userRepository;

    public AdminNotificationController(PublicNotificationPublisher publisher, UserRepository userRepository) {
        this.publisher = publisher;
        this.userRepository = userRepository;
    }

    @PostMapping("/broadcast")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NotificationDtos.AdminBroadcastResponse broadcast(@Valid @RequestBody NotificationDtos.AdminBroadcastRequest body) {
        long active = userRepository.countByStatus(UserStatus.ACTIVE);
        publisher.adminBroadcast(body.title(), body.body(), body.type(), body.link());
        return new NotificationDtos.AdminBroadcastResponse(
                "Broadcast queued for active members (in-app disabled users are skipped).",
                active
        );
    }
}
