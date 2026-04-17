package com.rps.samaj.user.model;

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
@Table(name = "samaj_user_settings")
public class UserSettings {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "show_phone")
    private boolean showPhone;

    @Column(name = "show_in_directory")
    private boolean showInDirectory;

    @Column(name = "emergency_alerts")
    private boolean emergencyAlerts;

    @Column(name = "two_factor_enabled")
    private boolean twoFactorEnabled;

    @Column(name = "login_alerts_enabled")
    private boolean loginAlertsEnabled;

    protected UserSettings() {
    }

    public UserSettings(User user) {
        this.user = user;
        this.showPhone = false;
        this.showInDirectory = true;
        this.emergencyAlerts = true;
        this.twoFactorEnabled = false;
        this.loginAlertsEnabled = false;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public boolean isShowPhone() {
        return showPhone;
    }

    public void setShowPhone(boolean showPhone) {
        this.showPhone = showPhone;
    }

    public boolean isShowInDirectory() {
        return showInDirectory;
    }

    public void setShowInDirectory(boolean showInDirectory) {
        this.showInDirectory = showInDirectory;
    }

    public boolean isEmergencyAlerts() {
        return emergencyAlerts;
    }

    public void setEmergencyAlerts(boolean emergencyAlerts) {
        this.emergencyAlerts = emergencyAlerts;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public boolean isLoginAlertsEnabled() {
        return loginAlertsEnabled;
    }

    public void setLoginAlertsEnabled(boolean loginAlertsEnabled) {
        this.loginAlertsEnabled = loginAlertsEnabled;
    }
}
