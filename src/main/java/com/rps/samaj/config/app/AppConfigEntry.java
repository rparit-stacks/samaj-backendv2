package com.rps.samaj.config.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Key-value website / runtime configuration. Admin can change values; {@link com.rps.samaj.config.app.RuntimeConfigService}
 * merges with {@link com.rps.samaj.config.SamajProperties} when a key is absent.
 */
@Entity
@Table(name = "samaj_app_config")
public class AppConfigEntry {

    @Id
    @Column(name = "config_key", length = 255)
    private String key;

    @Column(name = "config_value", columnDefinition = "text")
    private String value;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;

    protected AppConfigEntry() {
    }

    public AppConfigEntry(String key, String value, UUID updatedByUserId) {
        this.key = key;
        this.value = value;
        this.updatedAt = Instant.now();
        this.updatedByUserId = updatedByUserId;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(UUID updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }
}
