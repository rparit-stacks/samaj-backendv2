package com.rps.samaj.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;

    @Value("${samaj.otp.ttl-minutes:10}")
    private int otpTtlMinutes;

    @Value("${samaj.otp.max-attempts:5}")
    private int maxAttempts;

    public OtpService(OtpRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    /**
     * Generate and send OTP for login/signup
     */
    @Transactional
    public boolean generateAndSendOtp(String email, String purpose) {
        try {
            String code = generateOtpCode();
            Instant expiresAt = Instant.now().plusSeconds(otpTtlMinutes * 60L);

            OtpEntity otp = new OtpEntity(email.toLowerCase().trim(), code, purpose, expiresAt);
            otpRepository.save(otp);

            // Send OTP via email
            boolean sent = emailService.sendOtpEmail(email, code);
            if (!sent) {
                System.out.println("[OtpService] OTP created but email failed. Code for " + email + ": " + code);
            }

            return true;
        } catch (Exception e) {
            System.out.println("[OtpService] Failed to generate OTP: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate OTP
     */
    @Transactional
    public boolean validateOtp(String email, String code, String purpose) {
        try {
            Optional<OtpEntity> otpOpt = otpRepository.findByEmailAndPurpose(
                email.toLowerCase().trim(),
                purpose
            );

            if (otpOpt.isEmpty()) {
                return false;
            }

            OtpEntity otp = otpOpt.get();

            // Check if expired
            if (otp.isExpired()) {
                otpRepository.delete(otp);
                return false;
            }

            // Check attempts
            if (otp.getAttempts() >= maxAttempts) {
                otpRepository.delete(otp);
                return false;
            }

            // Check code
            if (!otp.getCode().equals(code)) {
                otp.setAttempts(otp.getAttempts() + 1);
                otpRepository.save(otp);
                return false;
            }

            // Valid OTP - delete it
            otpRepository.delete(otp);
            return true;
        } catch (Exception e) {
            System.out.println("[OtpService] Failed to validate OTP: " + e.getMessage());
            return false;
        }
    }

    /**
     * Resend OTP (generate new code)
     */
    @Transactional
    public boolean resendOtp(String email, String purpose) {
        try {
            // Delete old OTP
            otpRepository.findByEmailAndPurpose(email.toLowerCase().trim(), purpose)
                .ifPresent(otpRepository::delete);

            // Generate new one
            return generateAndSendOtp(email, purpose);
        } catch (Exception e) {
            System.out.println("[OtpService] Failed to resend OTP: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generate random 6-digit OTP code
     */
    private String generateOtpCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6 digits
        return String.valueOf(code);
    }
}
