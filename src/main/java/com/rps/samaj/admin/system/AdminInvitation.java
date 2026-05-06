package com.rps.samaj.admin.system;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "admin_invitations")
public class AdminInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String token;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "admin_invitation_services",
            joinColumns = @JoinColumn(name = "invitation_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "service_key", nullable = false)
    private Set<AdminServiceKey> serviceKeys = new HashSet<>();

    @Column(name = "created_by_id")
    private UUID createdById;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** BCrypt-hashed password, populated when the invitee calls /set-password. */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private boolean accepted = false;

    @Column(name = "accepted_user_id")
    private UUID acceptedUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AdminInvitation() {
    }

    public AdminInvitation(String email, String token, Set<AdminServiceKey> serviceKeys, UUID createdById, Instant expiresAt) {
        this.email = email;
        this.token = token;
        this.serviceKeys = new HashSet<>(serviceKeys);
        this.createdById = createdById;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getToken() { return token; }
    public Set<AdminServiceKey> getServiceKeys() { return serviceKeys; }
    public UUID getCreatedById() { return createdById; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
    public UUID getAcceptedUserId() { return acceptedUserId; }
    public void setAcceptedUserId(UUID id) { this.acceptedUserId = id; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
