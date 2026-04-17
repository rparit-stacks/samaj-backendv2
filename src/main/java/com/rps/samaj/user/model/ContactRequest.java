package com.rps.samaj.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_contact_requests")
public class ContactRequest {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id")
    private User target;

    @Enumerated(EnumType.STRING)
    private ContactRequestStatus status;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected ContactRequest() {
    }

    public ContactRequest(UUID id, User requester, User target, String message) {
        this.id = id;
        this.requester = requester;
        this.target = target;
        this.message = message;
        this.status = ContactRequestStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getRequester() {
        return requester;
    }

    public User getTarget() {
        return target;
    }

    public ContactRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ContactRequestStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }
}
