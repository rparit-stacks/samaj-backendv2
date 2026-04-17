package com.rps.samaj.gallery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "samaj_gallery_photos")
public class GalleryPhoto {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "album_id")
    private GalleryAlbum album;

    @Column(nullable = false, length = 2000)
    private String url;

    @Column(name = "sort_order")
    private int sortOrder;

    protected GalleryPhoto() {
    }

    public GalleryPhoto(UUID id, GalleryAlbum album, String url, int sortOrder) {
        this.id = id;
        this.album = album;
        this.url = url;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public GalleryAlbum getAlbum() {
        return album;
    }

    public String getUrl() {
        return url;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
