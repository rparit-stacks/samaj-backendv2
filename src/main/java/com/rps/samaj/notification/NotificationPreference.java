package com.rps.samaj.notification;

import com.rps.samaj.user.model.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;
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

    /** Notification types this user has individually silenced (e.g. "COMMUNITY", "EVENT"). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "samaj_notification_disabled_types", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "type", length = 32)
    private Set<String> disabledTypes = new HashSet<>();

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

    public Set<String> getDisabledTypes() {
        return disabledTypes;
    }

    public void setDisabledTypes(Set<String> disabledTypes) {
        this.disabledTypes = disabledTypes == null ? new HashSet<>() : disabledTypes;
    }
}
