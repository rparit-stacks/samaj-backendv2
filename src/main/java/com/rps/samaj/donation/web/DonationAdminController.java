package com.rps.samaj.donation.web;

import com.rps.samaj.api.dto.DonationDtos;
import com.rps.samaj.donation.DonationService;
import com.rps.samaj.security.DevUserContextFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/admin/donations")
public class DonationAdminController {

    private final DonationService donationService;

    public DonationAdminController(DonationService donationService) {
        this.donationService = donationService;
    }

    @GetMapping
    public DonationDtos.DonationPageResponse list(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        requireAdmin(adminId);
        return donationService.adminList(page, size, status);
    }

    @GetMapping("/stats")
    public DonationDtos.DonationStatsResponse stats(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId
    ) {
        requireAdmin(adminId);
        return donationService.adminStats();
    }

    @GetMapping("/config")
    public DonationDtos.DonationAdminConfigResponse getConfig(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId
    ) {
        requireAdmin(adminId);
        return donationService.getAdminConfig();
    }

    @PutMapping("/config")
    public DonationDtos.DonationAdminConfigResponse updateConfig(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @RequestBody DonationDtos.DonationConfigUpdateRequest body
    ) {
        requireAdmin(adminId);
        return donationService.updateConfig(body, adminId);
    }

    private static void requireAdmin(UUID adminId) {
        if (adminId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin context required");
        }
    }
}
