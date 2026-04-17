package com.rps.samaj.notification;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "samaj_notification_preferences")
public class NotificationPreference {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "email_enabled")
    private boolean emailEnabled;

    @Column(name = "in_app_enabled")
    private boolean inAppEnabled;

    @Column(name = "security_email_enabled")
    private boolean securityEmailEnabled;

    protected NotificationPreference() {
    }

    public NotificationPreference(User user) {
        this.user = user;
        this.emailEnabled = true;
        this.inAppEnabled = true;
        this.securityEmailEnabled = true;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isInAppEnabled() {
        return inAppEnabled;
    }

    public void setInAppEnabled(boolean inAppEnabled) {
        this.inAppEnabled = inAppEnabled;
    }

    public boolean isSecurityEmailEnabled() {
        return securityEmailEnabled;
    }

    public void setSecurityEmailEnabled(boolean securityEmailEnabled) {
        this.securityEmailEnabled = securityEmailEnabled;
    }
}
