package com.rps.samaj.notification;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores FCM device registration tokens per user.
 * One user may have multiple tokens (multiple devices).
 * Tokens are updated in-place when the device refreshes its FCM token.
 */
@Entity
@Table(name = "samaj_device_tokens")
public class DeviceToken {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** FCM registration token — unique per device installation. */
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    /** Platform identifier — "ANDROID", "IOS", or "WEB". */
    @Column(name = "platform", nullable = false, length = 16)
    private String platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceToken() {}

    public DeviceToken(UUID id, User user, String token, String platform) {
        this.id = id;
        this.user = user;
        this.token = token;
        this.platform = platform;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getToken() { return token; }
    public String getPlatform() { return platform; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
