package com.rps.samaj.auth;

import com.rps.samaj.api.dto.AuthDtos;
import com.rps.samaj.config.SamajProperties;
import com.rps.samaj.security.JwtService;
import com.rps.samaj.user.model.KycStatus;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserRole;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.service.UserAccountProvisioner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final JwtService jwtService;
    private final UserAccountProvisioner userAccountProvisioner;
    private final SamajProperties properties;
    private final RestClient restClient;

    public GoogleAuthService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            JwtService jwtService,
            UserAccountProvisioner userAccountProvisioner,
            SamajProperties properties,
            RestClient.Builder restClientBuilder
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.jwtService = jwtService;
        this.userAccountProvisioner = userAccountProvisioner;
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public record GoogleUserInfo(String googleId, String email, String name, String picture) {
    }

    /**
     * Verifies a Google ID token by calling Google's tokeninfo endpoint.
     * Validates that the email is verified and (when configured) the audience matches our client ID.
     */
    public GoogleUserInfo verifyIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google ID token is required");
        }
        Map<String, Object> claims;
        try {
            claims = restClient.get()
                    .uri(TOKENINFO_URL + idToken.trim())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google token");
                    })
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not verify Google token");
        }
        if (claims == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Empty token response from Google");
        }
        String emailVerified = String.valueOf(claims.get("email_verified"));
        if (!"true".equals(emailVerified)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google email is not verified");
        }
        // Validate audience against our web client ID when configured.
        String configuredClientId = properties.getGoogle().getWebClientId();
        if (configuredClientId != null && !configuredClientId.isBlank()) {
            String aud = String.valueOf(claims.get("aud"));
            if (!configuredClientId.equals(aud)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token audience mismatch");
            }
        }
        String googleId = String.valueOf(claims.get("sub"));
        String email = String.valueOf(claims.get("email"));
        String name = claims.getOrDefault("name", "").toString();
        String picture = claims.getOrDefault("picture", "").toString();
        return new GoogleUserInfo(googleId, email, name, picture);
    }

    /**
     * Handles sign-in for an existing user (found by googleId or email).
     * Links googleId to the account if not yet linked.
     */
    @Transactional
    public AuthDtos.AuthResponse loginExistingUser(User user, GoogleUserInfo info) {
        if (user.getGoogleId() == null || !user.getGoogleId().equals(info.googleId())) {
            user.setGoogleId(info.googleId());
        }
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
        }
        if (user.getStatus() == UserStatus.PENDING) {
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    /**
     * Creates the account and profile for a new Google user after they complete the profile form.
     */
    @Transactional
    public AuthDtos.AuthResponse completeGoogleSignup(
            JwtService.GoogleSignupClaims claims,
            String name,
            String phone
    ) {
        // Double-check the account wasn't created by a concurrent request
        Optional<User> byGoogleId = userRepository.findByGoogleId(claims.googleId());
        if (byGoogleId.isPresent()) {
            return loginExistingUser(byGoogleId.get(), new GoogleUserInfo(
                    claims.googleId(), claims.email(), claims.name(), claims.picture()));
        }

        String email = claims.email().trim().toLowerCase();
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            existing.setGoogleId(claims.googleId());
            return loginExistingUser(existing, new GoogleUserInfo(
                    claims.googleId(), claims.email(), claims.name(), claims.picture()));
        }

        String resolvedName = (name != null && !name.isBlank()) ? name.trim() : claims.name();
        String resolvedPhone = (phone != null && !phone.isBlank()) ? phone.trim() : null;

        UUID id = UUID.randomUUID();
        User user = new User(id, email, resolvedPhone, null, UserStatus.ACTIVE, UserRole.USER);
        user.setGoogleId(claims.googleId());
        user.setEmailVerified(true);
        user.setPhoneVerified(false);
        user.setParentAdmin(false);
        user.setKycStatus(KycStatus.NONE);
        // Store name in metadata so ensureSidecars picks it up
        user.setMetadata("{\"name\":\"" + escapeJson(resolvedName) + "\"}");
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);
        userAccountProvisioner.ensureSidecars(user);
        return buildAuthResponse(user);
    }

    public Optional<User> findExistingUser(GoogleUserInfo info) {
        Optional<User> byGoogleId = userRepository.findByGoogleId(info.googleId());
        if (byGoogleId.isPresent()) return byGoogleId;
        return userRepository.findByEmailIgnoreCase(info.email());
    }

    private AuthDtos.AuthResponse buildAuthResponse(User user) {
        userAccountProvisioner.ensureSidecars(user);
        String access = jwtService.createAccessToken(user);
        String refresh = jwtService.createRefreshToken(user);
        String profileKey = userProfileRepository.findByUser_Id(user.getId())
                .map(com.rps.samaj.user.model.UserProfile::getProfileKey)
                .orElse(null);
        AuthDtos.UserResponse userResp = new AuthDtos.UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getPhone(),
                user.getGoogleId(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getStatus().name(),
                user.getRole().name(),
                user.getKycStatus().name(),
                Map.of(),
                profileKey
        );
        return new AuthDtos.AuthResponse(access, refresh, jwtService.accessTtlSeconds(), userResp);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
