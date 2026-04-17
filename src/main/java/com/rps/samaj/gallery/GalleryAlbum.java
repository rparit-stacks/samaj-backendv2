package com.rps.samaj.gallery;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "samaj_gallery_albums")
public class GalleryAlbum {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private String name;

    @Column(name = "cover_photo_url", length = 2000)
    private String coverPhotoUrl;

    private boolean approved;

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GalleryPhoto> photos = new ArrayList<>();

    protected GalleryAlbum() {
    }

    public GalleryAlbum(UUID id, User createdBy, String name, String coverPhotoUrl, boolean approved) {
        this.id = id;
        this.createdBy = createdBy;
        this.name = name;
        this.coverPhotoUrl = coverPhotoUrl;
        this.approved = approved;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCoverPhotoUrl() {
        return coverPhotoUrl;
    }

    public void setCoverPhotoUrl(String coverPhotoUrl) {
        this.coverPhotoUrl = coverPhotoUrl;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<GalleryPhoto> getPhotos() {
        return photos;
    }
}
