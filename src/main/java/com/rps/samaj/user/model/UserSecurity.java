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
@Table(name = "samaj_user_security")
public class UserSecurity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "two_factor_enabled")
    private boolean twoFactorEnabled;

    @Column(name = "login_alerts_enabled")
    private boolean loginAlertsEnabled;

    protected UserSecurity() {
    }

    public UserSecurity(User user) {
        this.user = user;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
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
