package com.rps.samaj.matrimony;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_matrimony_conversations")
public class MatrimonyConversation {

    @Id
    private UUID id;

    @Column(name = "profile_id_lower", nullable = false)
    private UUID profileIdLower;

    @Column(name = "profile_id_higher", nullable = false)
    private UUID profileIdHigher;

    @Column(name = "created_at")
    private Instant createdAt;

    protected MatrimonyConversation() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProfileIdLower() {
        return profileIdLower;
    }

    public void setProfileIdLower(UUID profileIdLower) {
        this.profileIdLower = profileIdLower;
    }

    public UUID getProfileIdHigher() {
        return profileIdHigher;
    }

    public void setProfileIdHigher(UUID profileIdHigher) {
        this.profileIdHigher = profileIdHigher;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
