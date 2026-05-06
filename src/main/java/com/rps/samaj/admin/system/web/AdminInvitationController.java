package com.rps.samaj.admin.system.web;

import com.rps.samaj.admin.system.AdminInvitationService;
import com.rps.samaj.api.dto.AdminSystemDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoints for the admin invitation acceptance flow.
 * All paths under /auth/admin-invite/** are permitAll in SecurityConfig.
 */
@RestController
@RequestMapping("/auth/admin-invite")
public class AdminInvitationController {

    private final AdminInvitationService invitationService;

    public AdminInvitationController(AdminInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    /** Returns email and assigned services for the invitation page (no auth needed). */
    @GetMapping("/{token}")
    public AdminSystemDtos.InvitationDetailsResponse getDetails(@PathVariable String token) {
        return invitationService.getDetails(token);
    }

    /** Stores the BCrypt-hashed password and sends an OTP to the invited email. */
    @PostMapping("/{token}/set-password")
    public void setPassword(
            @PathVariable String token,
            @Valid @RequestBody SetPasswordRequest body
    ) {
        invitationService.setPassword(token, body.password());
    }

    /** Verifies the OTP, creates the admin account, and returns JWT tokens. */
    @PostMapping("/{token}/verify")
    public AdminSystemDtos.AuthTokenResponse verify(
            @PathVariable String token,
            @Valid @RequestBody VerifyOtpRequest body
    ) {
        return invitationService.verifyAndActivate(token, body.otp());
    }

    public record SetPasswordRequest(
            @NotBlank @Size(min = 8, max = 200) String password
    ) {
    }

    public record VerifyOtpRequest(
            @NotBlank String otp
    ) {
    }
}
