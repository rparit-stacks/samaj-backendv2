package com.rps.samaj.user.web;

import com.rps.samaj.api.dto.UserProfileDtos;
import com.rps.samaj.user.service.UserProfileApiService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class PublicUserController {

    private final UserProfileApiService userProfileApiService;

    public PublicUserController(UserProfileApiService userProfileApiService) {
        this.userProfileApiService = userProfileApiService;
    }

    @GetMapping("/search")
    public UserProfileDtos.PaginatedUserProfiles search(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return userProfileApiService.search(q, page, size);
    }

    @GetMapping("/directory")
    public UserProfileDtos.PaginatedUserProfiles directory(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "100") int size
    ) {
        return userProfileApiService.directory(page, size);
    }

    @GetMapping("/p/{profileKey}/profile")
    public UserProfileDtos.PublicProfileResponse publicProfileByKey(
            @PathVariable String profileKey,
            Authentication auth
    ) {
        return userProfileApiService.getPublicProfileByProfileKey(profileKey, viewerId(auth));
    }

    @GetMapping("/p/{profileKey}/contact")
    public UserProfileDtos.ContactInfoResponse contactByKey(@PathVariable String profileKey) {
        return userProfileApiService.getContactByProfileKey(profileKey);
    }

    @GetMapping("/p/{profileKey}/visible-profile")
    public UserProfileDtos.VisibleProfileResponse visibleByKey(
            @PathVariable String profileKey,
            @RequestParam(value = "context", required = false) String context
    ) {
        return userProfileApiService.getVisibleProfileByProfileKey(profileKey, context);
    }

    @GetMapping("/{userId}/profile")
    public UserProfileDtos.PublicProfileResponse publicProfile(
            @PathVariable UUID userId,
            Authentication auth
    ) {
        return userProfileApiService.getPublicProfile(userId, viewerId(auth));
    }

    @GetMapping("/{userId}/contact")
    public UserProfileDtos.ContactInfoResponse contact(@PathVariable UUID userId) {
        return userProfileApiService.getContact(userId);
    }

    @GetMapping("/{userId}/visible-profile")
    public UserProfileDtos.VisibleProfileResponse visibleProfile(
            @PathVariable UUID userId,
            @RequestParam(value = "context", required = false) String context
    ) {
        return userProfileApiService.getVisibleProfile(userId, context);
    }

    private static UUID viewerId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof UUID u) {
            return u;
        }
        return null;
    }
}
