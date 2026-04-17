package com.rps.samaj.user.web;

import com.rps.samaj.api.dto.UserProfileDtos;
import com.rps.samaj.user.service.UserProfileApiService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me")
public class MeUserController {

    private final UserProfileApiService userProfileApiService;

    public MeUserController(UserProfileApiService userProfileApiService) {
        this.userProfileApiService = userProfileApiService;
    }

    @GetMapping("/profile")
    public UserProfileDtos.UserProfileResponse getProfile(Authentication auth) {
        return userProfileApiService.getMyProfile(requireUser(auth));
    }

    @PutMapping("/profile")
    public UserProfileDtos.UserProfileResponse putProfile(
            Authentication auth,
            @RequestBody UserProfileDtos.UserProfilePatch body
    ) {
        return userProfileApiService.patchMyProfile(requireUser(auth), body);
    }

    @GetMapping("/family")
    public List<UserProfileDtos.FamilyMemberResponse> family(Authentication auth) {
        return userProfileApiService.listFamily(requireUser(auth));
    }

    @PostMapping("/family")
    public UserProfileDtos.FamilyMemberResponse addFamily(
            Authentication auth,
            @RequestBody UserProfileDtos.FamilyMemberRequest body
    ) {
        return userProfileApiService.addFamily(requireUser(auth), body);
    }

    @PutMapping("/family/{id}")
    public UserProfileDtos.FamilyMemberResponse updateFamily(
            Authentication auth,
            @PathVariable("id") UUID memberId,
            @RequestBody UserProfileDtos.FamilyMemberRequest body
    ) {
        return userProfileApiService.updateFamily(requireUser(auth), memberId, body);
    }

    @DeleteMapping("/family/{id}")
    public void deleteFamily(Authentication auth, @PathVariable("id") UUID memberId) {
        userProfileApiService.deleteFamily(requireUser(auth), memberId);
    }

    @GetMapping("/settings")
    public UserProfileDtos.UserSettingsResponse getSettings(Authentication auth) {
        return userProfileApiService.getSettings(requireUser(auth));
    }

    @PutMapping("/settings")
    public UserProfileDtos.UserSettingsResponse putSettings(
            Authentication auth,
            @RequestBody UserProfileDtos.UserSettingsResponse body
    ) {
        return userProfileApiService.putSettings(requireUser(auth), body);
    }

    @GetMapping("/privacy")
    public UserProfileDtos.PrivacySettingsResponse getPrivacy(Authentication auth) {
        return userProfileApiService.getPrivacy(requireUser(auth));
    }

    @PutMapping("/privacy")
    public UserProfileDtos.PrivacySettingsResponse putPrivacy(
            Authentication auth,
            @RequestBody UserProfileDtos.PrivacyPatch body
    ) {
        return userProfileApiService.patchPrivacy(requireUser(auth), body);
    }

    @GetMapping("/security")
    public UserProfileDtos.SecuritySettingsResponse getSecurity(Authentication auth) {
        return userProfileApiService.getSecurity(requireUser(auth));
    }

    @PutMapping("/security")
    public UserProfileDtos.SecuritySettingsResponse putSecurity(
            Authentication auth,
            @RequestBody UserProfileDtos.SecurityPatch body
    ) {
        return userProfileApiService.patchSecurity(requireUser(auth), body);
    }

    private static UUID requireUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return u;
    }
}
