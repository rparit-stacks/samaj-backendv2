package com.rps.samaj.user.web;

import com.rps.samaj.api.dto.UserProfileDtos;
import com.rps.samaj.user.service.UserProfileApiService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/contact-requests")
public class UserContactRequestController {

    private final UserProfileApiService userProfileApiService;

    public UserContactRequestController(UserProfileApiService userProfileApiService) {
        this.userProfileApiService = userProfileApiService;
    }

    @PostMapping
    public UserProfileDtos.ContactRequestItem create(
            Authentication auth,
            @RequestBody UserProfileDtos.ContactRequestCreate body
    ) {
        return userProfileApiService.createContactRequest(requireUser(auth), body);
    }

    @GetMapping("/incoming")
    public List<UserProfileDtos.ContactRequestItem> incoming(Authentication auth) {
        return userProfileApiService.incomingContact(requireUser(auth));
    }

    @GetMapping("/outgoing")
    public List<UserProfileDtos.ContactRequestItem> outgoing(Authentication auth) {
        return userProfileApiService.outgoingContact(requireUser(auth));
    }

    @PutMapping("/{id}/respond")
    public UserProfileDtos.ContactRequestItem respond(
            Authentication auth,
            @PathVariable("id") UUID id,
            @RequestBody UserProfileDtos.ContactRequestRespond body
    ) {
        return userProfileApiService.respondContact(requireUser(auth), id, body);
    }

    private static UUID requireUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Unauthorized"
            );
        }
        return u;
    }
}
