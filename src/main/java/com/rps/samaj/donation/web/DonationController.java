package com.rps.samaj.donation.web;

import com.rps.samaj.api.dto.DonationDtos;
import com.rps.samaj.donation.DonationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donations")
public class DonationController {

    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @GetMapping("/config")
    public DonationDtos.DonationPublicConfigResponse config() {
        return donationService.getPublicConfig();
    }

    @PostMapping("/order")
    @ResponseStatus(HttpStatus.CREATED)
    public DonationDtos.CreateOrderResponse createOrder(
            Authentication auth,
            @Valid @RequestBody DonationDtos.CreateOrderRequest body
    ) {
        return donationService.createOrder(requireUserId(auth), body);
    }

    @PostMapping("/verify")
    public DonationDtos.DonationItem verifyPayment(
            Authentication auth,
            @Valid @RequestBody DonationDtos.VerifyPaymentRequest body
    ) {
        return donationService.verifyPayment(requireUserId(auth), body);
    }

    @GetMapping("/my")
    public DonationDtos.DonationPageResponse myDonations(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return donationService.myDonations(requireUserId(auth), page, size);
    }

    private static UUID requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return u;
    }
}
