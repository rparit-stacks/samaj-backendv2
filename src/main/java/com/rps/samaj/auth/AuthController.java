package com.rps.samaj.auth;

import com.rps.samaj.api.dto.AuthDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody AuthDtos.RegisterRequest body) {
        return authService.register(body);
    }

    @GetMapping("/setup/status")
    public AuthDtos.SetupStatusResponse setupStatus() {
        return authService.setupStatus();
    }

    @PostMapping("/setup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse completeSetup(@Valid @RequestBody AuthDtos.SetupRequest body) {
        return authService.completeSetup(body);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest body) {
        return authService.login(body);
    }

    @PostMapping("/refresh")
    public AuthDtos.AuthResponse refresh(@RequestBody(required = false) AuthDtos.RefreshTokenRequest body) {
        if (body == null || body.refreshToken() == null || body.refreshToken().isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "refreshToken required");
        }
        return authService.refresh(body.refreshToken());
    }

    @GetMapping("/me")
    public AuthDtos.UserResponse me(@AuthenticationPrincipal UUID userId) {
        requireUser(userId);
        return authService.me(userId);
    }

    @PutMapping("/me")
    public AuthDtos.UserResponse updateMe(
            @AuthenticationPrincipal UUID userId,
            @RequestBody AuthDtos.UpdateAuthProfileRequest body
    ) {
        requireUser(userId);
        return authService.updateMe(userId, body);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@AuthenticationPrincipal UUID userId) {
        requireUser(userId);
        return Map.of("message", "Logged out");
    }

    @PostMapping("/password/change")
    public Map<String, String> changePassword(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AuthDtos.ChangePasswordRequest body
    ) {
        requireUser(userId);
        authService.changePassword(userId, body);
        return Map.of("message", "Password updated");
    }

    @DeleteMapping("/account")
    public Map<String, String> deleteAccount(@AuthenticationPrincipal UUID userId) {
        requireUser(userId);
        authService.deleteAccount(userId);
        return Map.of("message", "Account deleted");
    }

    @PostMapping("/otp/send")
    public Map<String, String> otpSend(@Valid @RequestBody AuthDtos.OtpSendRequest body) {
        return authService.sendOtp(body);
    }

    @PostMapping("/login/otp")
    public AuthDtos.AuthResponse loginOtp(@Valid @RequestBody AuthDtos.LoginOtpRequest body) {
        return authService.loginWithOtp(body);
    }

    @PostMapping("/otp/verify")
    public AuthDtos.AuthResponse otpVerify(@Valid @RequestBody AuthDtos.OtpVerifyRequest body) {
        return authService.verifyOtpAndCompleteRegistration(body);
    }

    private static void requireUser(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }
}
