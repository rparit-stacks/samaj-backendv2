package com.rps.samaj.cms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_cms_mobile_banners")
public class CmsMobileBanner {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String imageUrl;

    @Column(nullable = false, length = 50)
    private String redirectType;

    @Column(columnDefinition = "text")
    private String redirectTarget;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    protected CmsMobileBanner() {
    }

    public CmsMobileBanner(UUID id, String title, String imageUrl, String redirectType, String redirectTarget, int displayOrder, UUID createdByUserId) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.redirectType = redirectType;
        this.redirectTarget = redirectTarget;
        this.displayOrder = displayOrder;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.createdByUserId = createdByUserId;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getRedirectType() {
        return redirectType;
    }

    public void setRedirectType(String redirectType) {
        this.redirectType = redirectType;
    }

    public String getRedirectTarget() {
        return redirectTarget;
    }

    public void setRedirectTarget(String redirectTarget) {
        this.redirectTarget = redirectTarget;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }
}
