package com.rps.samaj.auth;

import com.rps.samaj.user.model.OtpChallenge;
import com.rps.samaj.user.repository.OtpChallengeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Dev-friendly OTP: always logs the code to the server terminal (stdout + SLF4J).
 */
@Service
public class SamajOtpService {

    private static final Logger log = LoggerFactory.getLogger(SamajOtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    public static final String PURPOSE_REGISTRATION = "REGISTRATION";
    public static final String PURPOSE_LOGIN = "LOGIN";

    public static final String TYPE_EMAIL = "EMAIL";
    public static final String TYPE_PHONE = "PHONE";

    private final OtpChallengeRepository otpChallengeRepository;
    private final EmailService emailService;

    public SamajOtpService(OtpChallengeRepository otpChallengeRepository, EmailService emailService) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.emailService = emailService;
    }

    /**
     * @param linkedUserId optional; set for REGISTRATION so verify can load {@link com.rps.samaj.user.model.User} by id
     */
    @Transactional
    public String generateAndStore(String rawIdentifier, String type, String purpose, UUID linkedUserId) {
        String identifier = normalizeIdentifier(rawIdentifier, type);
        String code = generateDigits();
        String purposeKey = purpose.toUpperCase(Locale.ROOT);
        otpChallengeRepository.deleteByIdentifierAndPurpose(identifier, purposeKey);
        OtpChallenge row = new OtpChallenge(
                identifier,
                type.toUpperCase(Locale.ROOT),
                code,
                purposeKey,
                Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L),
                linkedUserId
        );
        otpChallengeRepository.save(row);

        String line = String.format(
                "====== OTP ====== For: %s | Type: %s | Purpose: %s | Code: %s ======",
                identifier,
                type,
                purposeKey,
                code
        );
        System.out.println(line);
        log.info(line);

        // Send email for email type OTP
        if (TYPE_EMAIL.equals(type)) {
            try {
                emailService.sendOtpEmail(identifier, code);
            } catch (Exception e) {
                log.warn("Failed to send OTP email to {}: {}", identifier, e.getMessage());
            }
        }

        return code;
    }

    @Transactional
    public Optional<OtpChallenge> verify(String rawIdentifier, String code, String purpose) {
        String identifier = normalizeIdentifier(rawIdentifier, guessType(rawIdentifier));
        String p = purpose == null || purpose.isBlank()
                ? PURPOSE_REGISTRATION
                : purpose.toUpperCase(Locale.ROOT);
        Optional<OtpChallenge> opt = otpChallengeRepository
                .findTopByIdentifierAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(identifier, p);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        OtpChallenge row = opt.get();
        if (row.isExpired()) {
            return Optional.empty();
        }
        if (row.getAttempts() >= MAX_ATTEMPTS) {
            return Optional.empty();
        }
        row.incrementAttempts();
        if (!row.getCode().equals(code != null ? code.trim() : "")) {
            otpChallengeRepository.save(row);
            return Optional.empty();
        }
        row.setVerified(true);
        otpChallengeRepository.save(row);
        return Optional.of(row);
    }

    private static String normalizeIdentifier(String raw, String type) {
        if (raw == null) {
            return "";
        }
        String t = type != null ? type.toUpperCase(Locale.ROOT) : TYPE_EMAIL;
        if (TYPE_PHONE.equals(t)) {
            return raw.trim().replaceAll("\\s+", "");
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String guessType(String raw) {
        if (raw != null && raw.contains("@")) {
            return TYPE_EMAIL;
        }
        return TYPE_PHONE;
    }

    private static String generateDigits() {
        StringBuilder sb = new StringBuilder(OTP_LENGTH);
        for (int i = 0; i < OTP_LENGTH; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
