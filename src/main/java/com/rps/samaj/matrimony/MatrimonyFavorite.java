package com.rps.samaj.matrimony;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "samaj_matrimony_favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "profile_id"}))
public class MatrimonyFavorite {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id")
    private MatrimonyProfileEntity profile;

    protected MatrimonyFavorite() {
    }

    public MatrimonyFavorite(UUID id, User user, MatrimonyProfileEntity profile) {
        this.id = id;
        this.user = user;
        this.profile = profile;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public MatrimonyProfileEntity getProfile() {
        return profile;
    }
}
