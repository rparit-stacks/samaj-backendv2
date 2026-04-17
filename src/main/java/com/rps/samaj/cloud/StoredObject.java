package com.rps.samaj.cloud;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_stored_objects")
public class StoredObject {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "public_url", nullable = false, length = 2000)
    private String publicUrl;

    @Column(name = "storage_key", nullable = false, length = 1024)
    private String storageKey;

    @Column(nullable = false, length = 64)
    private String folder;

    @Column(name = "created_at")
    private Instant createdAt;

    protected StoredObject() {
    }

    public StoredObject(UUID id, User user, String publicUrl, String storageKey, String folder) {
        this.id = id;
        this.user = user;
        this.publicUrl = publicUrl;
        this.storageKey = storageKey;
        this.folder = folder;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getFolder() {
        return folder;
    }
}
