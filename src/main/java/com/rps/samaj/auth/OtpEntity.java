package com.rps.samaj.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "samaj_otps")
public class OtpEntity {

    @Id
    @Column(length = 100)
    private String id; // email:purpose (e.g., "user@example.com:login")

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(length = 50)
    private String purpose; // "login", "signup", "password_reset"

    @Column(length = 100)
    private String email;

    protected OtpEntity() {
    }

    public OtpEntity(String email, String code, String purpose, Instant expiresAt) {
        this.id = email + ":" + purpose;
        this.email = email;
        this.code = code;
        this.purpose = purpose;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.attempts = 0;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public String getPurpose() {
        return purpose;
    }

    public String getEmail() {
        return email;
    }
}
