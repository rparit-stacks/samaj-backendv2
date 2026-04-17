package com.rps.samaj.matrimony;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_matrimony_interests")
public class MatrimonyInterest {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_profile_id")
    private MatrimonyProfileEntity fromProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_profile_id")
    private MatrimonyProfileEntity toProfile;

    @Column(columnDefinition = "text")
    private String message;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected MatrimonyInterest() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MatrimonyProfileEntity getFromProfile() {
        return fromProfile;
    }

    public void setFromProfile(MatrimonyProfileEntity fromProfile) {
        this.fromProfile = fromProfile;
    }

    public MatrimonyProfileEntity getToProfile() {
        return toProfile;
    }

    public void setToProfile(MatrimonyProfileEntity toProfile) {
        this.toProfile = toProfile;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
