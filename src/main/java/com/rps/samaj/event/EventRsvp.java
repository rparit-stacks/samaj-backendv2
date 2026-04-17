package com.rps.samaj.event;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "samaj_event_rsvps", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"}))
public class EventRsvp {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "photo_url", length = 2000)
    private String photoUrl;

    protected EventRsvp() {
    }

    public EventRsvp(UUID id, EventEntity event, User user, String status) {
        this.id = id;
        this.event = event;
        this.user = user;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public EventEntity getEvent() {
        return event;
    }

    public User getUser() {
        return user;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
