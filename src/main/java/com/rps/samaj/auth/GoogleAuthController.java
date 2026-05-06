package com.rps.samaj.auth;

import com.rps.samaj.api.dto.AuthDtos;
import com.rps.samaj.security.JwtService;
import com.rps.samaj.user.model.User;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Public Google Sign-In endpoints — used by both the web (Google Identity Services)
 * and the Android native app (Credential Manager).
 * All paths are permitAll in SecurityConfig.
 */
@RestController
@RequestMapping("/auth/google")
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;
    private final JwtService jwtService;

    public GoogleAuthController(GoogleAuthService googleAuthService, JwtService jwtService) {
        this.googleAuthService = googleAuthService;
        this.jwtService = jwtService;
    }

    /**
     * Step 1: Verify a Google ID token obtained from either:
     * - Web: Google Identity Services (One Tap / Sign-In button)
     * - Android: Credential Manager native sign-in
     *
     * Returns either a full auth response (existing user) or a temp token (new user).
     */
    @PostMapping("/id-token")
    public GoogleSignInResponse verifyIdToken(@Valid @RequestBody IdTokenRequest body) {
        GoogleAuthService.GoogleUserInfo info = googleAuthService.verifyIdToken(body.idToken());
        Optional<User> existing = googleAuthService.findExistingUser(info);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getStatus().name().equals("DELETED") || user.getStatus().name().equals("SUSPENDED")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
            }
            AuthDtos.AuthResponse auth = googleAuthService.loginExistingUser(user, info);
            return new GoogleSignInResponse(
                    "login",
                    auth.accessToken(), auth.refreshToken(), auth.expiresIn(), auth.user(),
                    null, null, null, null
            );
        }
        // New user — create a short-lived temp token so the frontend can collect extra details
        String tempToken = jwtService.createGoogleSignupToken(
                info.googleId(), info.email(), info.name(), info.picture());
        return new GoogleSignInResponse(
                "signup",
                null, null, null, null,
                tempToken, info.email(), info.name(), info.picture()
        );
    }

    /**
     * Step 2 (new users only): Complete signup after the user fills in their profile details.
     */
    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse completeSignup(@Valid @RequestBody CompleteSignupRequest body) {
        JwtService.GoogleSignupClaims claims;
        try {
            claims = jwtService.parseGoogleSignupToken(body.tempToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired signup token");
        }
        return googleAuthService.completeGoogleSignup(claims, body.name(), body.phone());
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record IdTokenRequest(@NotBlank String idToken) {
    }

    public record CompleteSignupRequest(
            @NotBlank String tempToken,
            String name,
            String phone
    ) {
    }

    public record GoogleSignInResponse(
            String kind,
            // "login" fields
            String accessToken,
            String refreshToken,
            Long expiresIn,
            AuthDtos.UserResponse user,
            // "signup" fields
            String tempToken,
            String email,
            String name,
            String picture
    ) {
    }
}
