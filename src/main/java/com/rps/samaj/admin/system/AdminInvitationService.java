package com.rps.samaj.admin.system;

import com.rps.samaj.api.dto.AdminSystemDtos;
import com.rps.samaj.auth.EmailService;
import com.rps.samaj.auth.EmailTemplates;
import com.rps.samaj.auth.OtpService;
import com.rps.samaj.security.JwtService;
import com.rps.samaj.user.model.KycStatus;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserRole;
import com.rps.samaj.user.model.UserStatus;
import com.rps.samaj.user.repository.UserRepository;
import com.rps.samaj.user.service.UserAccountProvisioner;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminInvitationService {

    private static final long INVITE_TTL_HOURS = 48;

    private final AdminInvitationRepository invitationRepository;
    private final AdminServiceGrantRepository grantRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountProvisioner userAccountProvisioner;
    private final JwtService jwtService;

    public AdminInvitationService(
            AdminInvitationRepository invitationRepository,
            AdminServiceGrantRepository grantRepository,
            UserRepository userRepository,
            EmailService emailService,
            OtpService otpService,
            PasswordEncoder passwordEncoder,
            UserAccountProvisioner userAccountProvisioner,
            JwtService jwtService
    ) {
        this.invitationRepository = invitationRepository;
        this.grantRepository = grantRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.userAccountProvisioner = userAccountProvisioner;
        this.jwtService = jwtService;
    }

    @Transactional
    public AdminSystemDtos.InvitationResponse createInvitation(
            AdminSystemDtos.ChildAdminInviteRequest body,
            UUID createdById,
            String frontendBaseUrl
    ) {
        String email = body.email().trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmailIgnoreCase(email).ifPresent(u -> {
            if (u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.MODERATOR) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An admin account already exists for this email");
            }
        });

        Set<AdminServiceKey> keys = parseServiceKeys(body.serviceKeys());
        if (keys.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one service key is required");
        }

        // Delete any existing pending invitation for this email (idempotent re-invite)
        invitationRepository.findByEmailIgnoreCaseAndAcceptedFalse(email)
                .ifPresent(invitationRepository::delete);

        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plusSeconds(INVITE_TTL_HOURS * 3600L);
        AdminInvitation invitation = new AdminInvitation(email, token, keys, createdById, expiresAt);
        invitationRepository.saveAndFlush(invitation);

        String inviteUrl = frontendBaseUrl + "/admin/invite/" + token;
        String serviceNames = keys.stream()
                .map(k -> {
                    String n = k.name();
                    return n.charAt(0) + n.substring(1).toLowerCase(Locale.ROOT);
                })
                .sorted()
                .collect(Collectors.joining(", "));

        emailService.sendEmail(
                email,
                "You've been invited to manage Samaj",
                buildInviteTextBody(email, inviteUrl, serviceNames),
                EmailTemplates.adminInviteHtml(email, inviteUrl, serviceNames, INVITE_TTL_HOURS)
        );

        return toInvitationResponse(invitation);
    }

    @Transactional(readOnly = true)
    public AdminSystemDtos.InvitationDetailsResponse getDetails(String token) {
        AdminInvitation inv = findValidInvitation(token);
        return new AdminSystemDtos.InvitationDetailsResponse(
                inv.getEmail(),
                inv.getServiceKeys().stream().map(AdminServiceKey::name).sorted().collect(Collectors.toList()),
                inv.getExpiresAt().toString()
        );
    }

    @Transactional
    public void setPassword(String token, String password) {
        AdminInvitation inv = findValidInvitation(token);
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        inv.setPasswordHash(passwordEncoder.encode(password));
        invitationRepository.save(inv);
        otpService.generateAndSendOtp(inv.getEmail(), "ADMIN_INVITE");
    }

    @Transactional
    public AdminSystemDtos.AuthTokenResponse verifyAndActivate(String token, String otp) {
        AdminInvitation inv = findValidInvitation(token);
        if (inv.getPasswordHash() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password has not been set for this invitation");
        }

        boolean otpValid = otpService.validateOtp(inv.getEmail(), otp, "ADMIN_INVITE");
        if (!otpValid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired OTP");
        }

        String email = inv.getEmail();
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user != null && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MODERATOR)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An admin account already exists for this email");
        }

        if (user == null) {
            UUID id = UUID.randomUUID();
            user = new User(id, email, null, inv.getPasswordHash(), UserStatus.ACTIVE, UserRole.MODERATOR);
            user.setEmailVerified(true);
            user.setParentAdmin(false);
            user.setKycStatus(KycStatus.NONE);
            user.setMetadata("{}");
            user.setUpdatedAt(Instant.now());
            user = userRepository.save(user);
            userAccountProvisioner.ensureSidecars(user);
        } else {
            user.setRole(UserRole.MODERATOR);
            user.setPasswordHash(inv.getPasswordHash());
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerified(true);
            user.setUpdatedAt(Instant.now());
            user = userRepository.save(user);
        }

        grantRepository.deleteByUser_Id(user.getId());
        for (AdminServiceKey k : inv.getServiceKeys()) {
            grantRepository.save(new AdminServiceGrant(user, k));
        }
        String csv = inv.getServiceKeys().stream().map(Enum::name).sorted().collect(Collectors.joining(","));
        user.setAdminServiceKeys(csv.isEmpty() ? null : csv);
        userRepository.save(user);

        inv.setAccepted(true);
        inv.setAcceptedUserId(user.getId());
        invitationRepository.save(inv);

        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = jwtService.createRefreshToken(user);
        return new AdminSystemDtos.AuthTokenResponse(
                accessToken,
                refreshToken,
                jwtService.accessTtlSeconds(),
                new AdminSystemDtos.AuthTokenResponse.UserInfo(user.getId().toString(), user.getRole().name())
        );
    }

    @Transactional(readOnly = true)
    public List<AdminSystemDtos.InvitationResponse> listPending() {
        return invitationRepository
                .findByAcceptedFalseAndExpiresAtAfterOrderByCreatedAtDesc(Instant.now())
                .stream()
                .map(this::toInvitationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelInvitation(UUID id) {
        invitationRepository.deleteById(id);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AdminInvitation findValidInvitation(String token) {
        AdminInvitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
        if (inv.isAccepted()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invitation has already been used");
        }
        if (inv.isExpired()) {
            throw new ResponseStatusException(HttpStatus.GONE, "This invitation has expired");
        }
        return inv;
    }

    private AdminSystemDtos.InvitationResponse toInvitationResponse(AdminInvitation inv) {
        return new AdminSystemDtos.InvitationResponse(
                inv.getId().toString(),
                inv.getEmail(),
                inv.getServiceKeys().stream().map(AdminServiceKey::name).sorted().collect(Collectors.toList()),
                inv.getCreatedAt().toString(),
                inv.getExpiresAt().toString(),
                inv.isAccepted()
        );
    }

    private static Set<AdminServiceKey> parseServiceKeys(List<String> raw) {
        if (raw == null) return Set.of();
        return raw.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> {
                    try {
                        return AdminServiceKey.valueOf(s.trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown service key: " + s);
                    }
                })
                .collect(Collectors.toSet());
    }

    private static String buildInviteTextBody(String email, String inviteUrl, String serviceNames) {
        return String.format(
                "Hello,\n\n" +
                "You have been invited to become an admin on Samaj.\n\n" +
                "Email: %s\n" +
                "Assigned services: %s\n\n" +
                "Click the link below to accept your invitation and set up your account:\n%s\n\n" +
                "This link expires in 48 hours. Do not share it with anyone.\n\n" +
                "Regards,\nSamaj Team",
                email, serviceNames, inviteUrl
        );
    }
}
