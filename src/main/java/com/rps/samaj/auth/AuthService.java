package com.rps.samaj.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.AuthDtos;
import com.rps.samaj.security.JwtService;
import com.rps.samaj.user.model.KycStatus;
import com.rps.samaj.user.model.OtpChallenge;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.service.UserAccountProvisioner;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.model.UserRole;
import com.rps.samaj.user.model.UserStatus;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final UserAccountProvisioner userAccountProvisioner;
    private final SamajOtpService samajOtpService;

    public AuthService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ObjectMapper objectMapper,
            UserAccountProvisioner userAccountProvisioner,
            SamajOtpService samajOtpService
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.userAccountProvisioner = userAccountProvisioner;
        this.samajOtpService = samajOtpService;
    }

    @Transactional
    public Map<String, Object> register(AuthDtos.RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        String phone = normalizePhone(req.phone());
        if (phone != null && userRepository.findByPhone(phone).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already registered");
        }
        UUID id = UUID.randomUUID();
        User user = new User(id, email, phone, passwordEncoder.encode(req.password()), UserStatus.PENDING, UserRole.USER);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setParentAdmin(false);
        user.setKycStatus(KycStatus.NONE);
        user.setMetadata("{}");
        // save() may merge() when id is pre-assigned; use returned managed instance
        user = userRepository.save(user);
        samajOtpService.generateAndStore(email, SamajOtpService.TYPE_EMAIL, SamajOtpService.PURPOSE_REGISTRATION, user.getId());
        return Map.of(
                "message", "OTP sent to your email (see server console in development)",
                "otpRequired", true
        );
    }

    @Transactional(readOnly = true)
    public AuthDtos.SetupStatusResponse setupStatus() {
        boolean required = !userRepository.existsByParentAdminIsTrueAndStatus(UserStatus.ACTIVE);
        return new AuthDtos.SetupStatusResponse(required);
    }

    /**
     * One-time creation of the parent admin when {@link #setupStatus()} returns {@code setupRequired=true}.
     */
    @Transactional
    public AuthDtos.AuthResponse completeSetup(AuthDtos.SetupRequest req) {
        if (userRepository.existsByParentAdminIsTrueAndStatus(UserStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Setup already completed");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        UUID id = UUID.randomUUID();
        User user = new User(
                id,
                email,
                null,
                passwordEncoder.encode(req.password()),
                UserStatus.ACTIVE,
                UserRole.ADMIN
        );
        user.setEmailVerified(true);
        user.setPhoneVerified(false);
        user.setParentAdmin(true);
        user.setKycStatus(KycStatus.NONE);
        user.setMetadata("{}");
        user = userRepository.save(user);
        userAccountProvisioner.ensureSidecars(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        User user = resolveUser(req.identifier().trim()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (user.getStatus() == UserStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Please verify your email with the OTP sent at registration"
            );
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken required");
        }
        JwtService.ParsedJwt parsed;
        try {
            parsed = jwtService.parse(refreshToken.trim());
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        if (!JwtService.TYP_REFRESH.equals(parsed.typ())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        User user = userRepository.findById(parsed.userId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDtos.UserResponse me(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userAccountProvisioner.ensureSidecars(user);
        return toUserResponse(user);
    }

    @Transactional
    public AuthDtos.UserResponse updateMe(UUID userId, AuthDtos.UpdateAuthProfileRequest body) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userAccountProvisioner.ensureSidecars(user);
        if (body.phone() != null) {
            String p = normalizePhone(body.phone());
            if (p != null) {
                userRepository.findByPhone(p).filter(u -> !u.getId().equals(userId)).ifPresent(u -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Phone already in use");
                });
            }
            user.setPhone(p);
        }
        if (body.metadata() != null) {
            try {
                Map<String, Object> existing = readMetadata(user.getMetadata());
                existing.putAll(body.metadata());
                user.setMetadata(objectMapper.writeValueAsString(existing));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid metadata");
            }
        }
        if (body.name() != null && !body.name().isBlank()) {
            try {
                Map<String, Object> existing = readMetadata(user.getMetadata());
                existing.put("name", body.name().trim());
                user.setMetadata(objectMapper.writeValueAsString(existing));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid metadata");
            }
            UserProfile profile = userProfileRepository.findByUser_Id(userId).orElseThrow();
            profile.setFullName(body.name().trim());
            userProfileRepository.save(profile);
        }
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return toUserResponse(user);
    }

    @Transactional
    public void changePassword(UUID userId, AuthDtos.ChangePasswordRequest body) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(body.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(body.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userProfileRepository.findByUser_Id(userId).ifPresent(p -> {
            p.setProfileKey("del-" + userId.toString().replace("-", ""));
            userProfileRepository.save(p);
        });
        user.setStatus(UserStatus.DELETED);
        user.setPasswordHash(null);
        user.setEmail("deleted+" + user.getId() + "@invalid.local");
        user.setPhone(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public Map<String, String> sendOtp(AuthDtos.OtpSendRequest req) {
        String purpose = req.purpose().trim().toUpperCase(Locale.ROOT);
        String type = req.type().trim().toUpperCase(Locale.ROOT);
        String ident = req.identifier().trim();
        if (SamajOtpService.PURPOSE_REGISTRATION.equals(purpose)) {
            String email = ident.toLowerCase(Locale.ROOT);
            User u = userRepository.findByEmailIgnoreCase(email).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found; register first"));
            if (u.getStatus() != UserStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account already verified");
            }
            samajOtpService.generateAndStore(email, SamajOtpService.TYPE_EMAIL, purpose, u.getId());
        } else if (SamajOtpService.PURPOSE_LOGIN.equals(purpose)) {
            User u = resolveUser(ident).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            if (u.getStatus() != UserStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
            }
            String otpType = type.contains("PHONE") ? SamajOtpService.TYPE_PHONE : SamajOtpService.TYPE_EMAIL;
            samajOtpService.generateAndStore(ident, otpType, purpose, u.getId());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported OTP purpose");
        }
        return Map.of("message", "OTP sent (see server console in development)");
    }

    @Transactional
    public AuthDtos.AuthResponse verifyOtpAndCompleteRegistration(AuthDtos.OtpVerifyRequest req) {
        String purpose = req.purpose() != null && !req.purpose().isBlank()
                ? req.purpose().trim().toUpperCase(Locale.ROOT)
                : SamajOtpService.PURPOSE_REGISTRATION;
        if (!SamajOtpService.PURPOSE_REGISTRATION.equals(purpose)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use the login OTP endpoint for this purpose");
        }
        OtpChallenge row = samajOtpService.verify(req.identifier(), req.code(), purpose)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP"));
        User user = resolveUserForVerifiedOtp(row, req.identifier());
        if (user.getStatus() != UserStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already verified");
        }
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse loginWithOtp(AuthDtos.LoginOtpRequest req) {
        String ident = req.identifier().trim();
        OtpChallenge row = samajOtpService.verify(ident, req.otp(), SamajOtpService.PURPOSE_LOGIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP"));
        User user = row.getUserId() != null
                ? userRepository.findById(row.getUserId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"))
                : resolveUser(ident).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        return buildAuthResponse(user);
    }

    private User resolveUserForVerifiedOtp(OtpChallenge row, String rawIdentifier) {
        if (row.getUserId() != null) {
            return userRepository.findById(row.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        }
        return userRepository.findByEmailIgnoreCase(row.getIdentifier())
                .or(() -> userRepository.findByEmailIgnoreCase(
                        rawIdentifier == null ? "" : rawIdentifier.trim().toLowerCase(Locale.ROOT)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AuthDtos.AuthResponse buildAuthResponse(User user) {
        userAccountProvisioner.ensureSidecars(user);
        String access = jwtService.createAccessToken(user);
        String refresh = jwtService.createRefreshToken(user);
        return new AuthDtos.AuthResponse(
                access,
                refresh,
                jwtService.accessTtlSeconds(),
                toUserResponse(user)
        );
    }

    private AuthDtos.UserResponse toUserResponse(User user) {
        String profileKey = userProfileRepository.findByUser_Id(user.getId())
                .map(UserProfile::getProfileKey)
                .orElse(null);
        return new AuthDtos.UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getPhone(),
                user.getGoogleId(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                user.getStatus().name(),
                user.getRole().name(),
                user.getKycStatus().name(),
                readMetadata(user.getMetadata()),
                profileKey
        );
    }

    private Map<String, Object> readMetadata(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Optional<User> resolveUser(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmailIgnoreCase(identifier);
        }
        return userRepository.findByPhone(normalizePhone(identifier));
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String p = phone.replaceAll("\\s+", "").trim();
        return p.isEmpty() ? null : p;
    }
}
