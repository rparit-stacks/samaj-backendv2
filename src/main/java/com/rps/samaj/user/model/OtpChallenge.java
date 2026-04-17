package com.rps.samaj.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "samaj_otp_challenges",
        indexes = @Index(name = "idx_otp_identifier_purpose", columnList = "identifier,purpose")
)
public class OtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String identifier;

    @Column(name = "identifier_type", nullable = false, length = 16)
    private String identifierType;

    @Column(nullable = false, length = 16)
    private String code;

    @Column(nullable = false, length = 32)
    private String purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** For REGISTRATION (and similar): resolves user even if identifier/email ever diverges. */
    @Column(name = "user_id")
    private UUID userId;

    protected OtpChallenge() {
    }

    public OtpChallenge(
            String identifier,
            String identifierType,
            String code,
            String purpose,
            Instant expiresAt,
            UUID userId
    ) {
        this.identifier = identifier;
        this.identifierType = identifierType;
        this.code = code;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.verified = false;
        this.attempts = 0;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public String getCode() {
        return code;
    }

    public String getPurpose() {
        return purpose;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public UUID getUserId() {
        return userId;
    }
}
