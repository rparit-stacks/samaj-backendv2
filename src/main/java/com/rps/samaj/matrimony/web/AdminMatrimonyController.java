package com.rps.samaj.matrimony.web;

import com.rps.samaj.api.dto.MatrimonyDtos;
import com.rps.samaj.matrimony.MatrimonyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/matrimony")
public class AdminMatrimonyController {

    private final MatrimonyService matrimonyService;

    public AdminMatrimonyController(MatrimonyService matrimonyService) {
        this.matrimonyService = matrimonyService;
    }

    @GetMapping("/profiles")
    public MatrimonyDtos.PaginatedAdminMatrimonyProfiles listProfiles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) Boolean visibleInSearch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return matrimonyService.adminListProfiles(q, status, gender, minAge, maxAge, city, verified, visibleInSearch, page, size);
    }

    @GetMapping("/profiles/{profileId}")
    public MatrimonyDtos.AdminMatrimonyProfileDetailResponse getProfile(@PathVariable UUID profileId) {
        return matrimonyService.adminGetProfile(profileId);
    }

    @PostMapping("/profiles/{profileId}/verify")
    public MatrimonyDtos.AdminMatrimonyProfileResponse verifyProfile(@PathVariable UUID profileId) {
        return matrimonyService.adminVerifyProfile(profileId);
    }

    @PostMapping("/profiles/{profileId}/visibility")
    public MatrimonyDtos.AdminMatrimonyProfileResponse toggleVisibility(@PathVariable UUID profileId) {
        return matrimonyService.adminToggleVisibility(profileId);
    }

    @GetMapping("/analytics")
    public MatrimonyDtos.AdminMatrimonyAnalyticsResponse getAnalytics() {
        return matrimonyService.adminGetAnalytics();
    }
}
