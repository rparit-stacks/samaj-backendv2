package com.rps.samaj.achiever;

import com.rps.samaj.user.model.User;
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
@Table(name = "samaj_achievements")
public class Achievement {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String headline;

    @Column(name = "fields_json", nullable = false, columnDefinition = "text")
    private String fieldsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AchievementStatus status = AchievementStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "marquee_enabled")
    private boolean marqueeEnabled;

    @Column(name = "marquee_start")
    private Instant marqueeStart;

    @Column(name = "marquee_end")
    private Instant marqueeEnd;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Achievement() {
    }

    public Achievement(UUID id, User user, String headline, String fieldsJson) {
        this.id = id;
        this.user = user;
        this.headline = headline;
        this.fieldsJson = fieldsJson;
        this.status = AchievementStatus.PENDING;
        this.marqueeEnabled = false;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getFieldsJson() {
        return fieldsJson;
    }

    public void setFieldsJson(String fieldsJson) {
        this.fieldsJson = fieldsJson;
    }

    public AchievementStatus getStatus() {
        return status;
    }

    public void setStatus(AchievementStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public boolean isMarqueeEnabled() {
        return marqueeEnabled;
    }

    public void setMarqueeEnabled(boolean marqueeEnabled) {
        this.marqueeEnabled = marqueeEnabled;
    }

    public Instant getMarqueeStart() {
        return marqueeStart;
    }

    public void setMarqueeStart(Instant marqueeStart) {
        this.marqueeStart = marqueeStart;
    }

    public Instant getMarqueeEnd() {
        return marqueeEnd;
    }

    public void setMarqueeEnd(Instant marqueeEnd) {
        this.marqueeEnd = marqueeEnd;
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
