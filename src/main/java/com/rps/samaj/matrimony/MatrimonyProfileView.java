package com.rps.samaj.matrimony;

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

@Entity
@Table(name = "samaj_matrimony_profile_views")
public class MatrimonyProfileView {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private MatrimonyProfileEntity profile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viewer_user_id")
    private User viewer;

    @Column(name = "created_at")
    private Instant createdAt;

    protected MatrimonyProfileView() {
    }

    public MatrimonyProfileView(UUID id, MatrimonyProfileEntity profile, User viewer, Instant createdAt) {
        this.id = id;
        this.profile = profile;
        this.viewer = viewer;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public MatrimonyProfileEntity getProfile() {
        return profile;
    }

    public User getViewer() {
        return viewer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
