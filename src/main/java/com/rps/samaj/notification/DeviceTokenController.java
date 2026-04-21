package com.rps.samaj.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

/**
 * Lets the Android app register / unregister FCM device tokens.
 *
 * The web app calls these endpoints after login using:
 *   window.SamajNative.getFcmToken()  → reads the token the Android app stored
 *   POST  /api/v1/device-tokens  { token, platform }
 *   DELETE /api/v1/device-tokens  { token }   (on logout)
 */
@RestController
@RequestMapping("/api/v1/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    /** Register (or refresh) the FCM token for the authenticated user. */
    @PostMapping
    public ResponseEntity<Void> register(
            Authentication auth,
            @Valid @RequestBody RegisterRequest body
    ) {
        UUID userId = UUID.fromString(auth.getName());
        deviceTokenService.register(userId, body.token(), body.platform());
        return ResponseEntity.ok().build();
    }

    /** Unregister a single device token (on logout from this device). */
    @DeleteMapping
    public ResponseEntity<Void> unregister(
            @Valid @RequestBody UnregisterRequest body
    ) {
        deviceTokenService.unregister(body.token());
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record RegisterRequest(
            @NotBlank @Size(max = 512) String token,
            @Size(max = 16) String platform   // "ANDROID" | "IOS" | "WEB"
    ) {}

    public record UnregisterRequest(
            @NotBlank @Size(max = 512) String token
    ) {}
}
