package com.rps.samaj.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record UserResponse(
            String id,
            String email,
            String phone,
            String googleId,
            boolean emailVerified,
            boolean phoneVerified,
            String status,
            String role,
            String kycStatus,
            Map<String, Object> metadata,
            String profileKey
    ) {
    }

    public record AuthResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            String phone,
            @NotBlank @Size(min = 8, max = 200) String password
    ) {
    }

    public record LoginRequest(
            @NotBlank String identifier,
            @NotBlank String password
    ) {
    }

    public record RefreshTokenRequest(String refreshToken) {
    }

    /** True when no active parent admin exists — UI should show one-time install. */
    public record SetupStatusResponse(boolean setupRequired) {
    }

    public record SetupRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 200) String password
    ) {
    }

    public record UpdateAuthProfileRequest(String name, String phone, Map<String, Object> metadata) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 200) String newPassword
    ) {
    }

    public record OtpSendRequest(
            @NotBlank String identifier,
            @NotBlank String type,
            @NotBlank String purpose
    ) {
    }

    public record OtpVerifyRequest(
            @NotBlank String identifier,
            @NotBlank String code,
            String purpose
    ) {
    }

    public record LoginOtpRequest(
            @NotBlank String identifier,
            @NotBlank String otp
    ) {
    }
}
