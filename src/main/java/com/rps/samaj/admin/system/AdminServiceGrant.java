package com.rps.samaj.admin.system;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "samaj_admin_service_grants",
        uniqueConstraints = @UniqueConstraint(name = "uk_admin_grant_user_service", columnNames = {"user_id", "service_key"})
)
public class AdminServiceGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_key", nullable = false, length = 64)
    private AdminServiceKey serviceKey;

    protected AdminServiceGrant() {
    }

    public AdminServiceGrant(User user, AdminServiceKey serviceKey) {
        this.user = user;
        this.serviceKey = serviceKey;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public AdminServiceKey getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(AdminServiceKey serviceKey) {
        this.serviceKey = serviceKey;
    }
}
