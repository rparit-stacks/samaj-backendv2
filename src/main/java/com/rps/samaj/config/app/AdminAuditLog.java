package com.rps.samaj.config.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_admin_audit_logs")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 100)
    private String resource;

    @Column(columnDefinition = "text")
    private String changesBefore;

    @Column(columnDefinition = "text")
    private String changesAfter;

    @Column(name = "admin_user_id", nullable = false)
    private UUID adminUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 50)
    private String ipAddress;

    protected AdminAuditLog() {
    }

    public AdminAuditLog(String action, String resource, String changesBefore, String changesAfter, UUID adminUserId, String ipAddress) {
        this.action = action;
        this.resource = resource;
        this.changesBefore = changesBefore;
        this.changesAfter = changesAfter;
        this.adminUserId = adminUserId;
        this.createdAt = Instant.now();
        this.ipAddress = ipAddress;
    }

    public UUID getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public String getResource() {
        return resource;
    }

    public String getChangesBefore() {
        return changesBefore;
    }

    public String getChangesAfter() {
        return changesAfter;
    }

    public UUID getAdminUserId() {
        return adminUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
