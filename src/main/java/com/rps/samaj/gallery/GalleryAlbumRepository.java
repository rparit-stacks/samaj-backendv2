package com.rps.samaj.gallery;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GalleryAlbumRepository extends JpaRepository<GalleryAlbum, UUID> {

    @EntityGraph(attributePaths = {"photos", "createdBy"})
    @Query("select a from GalleryAlbum a where a.approved = true order by a.createdAt desc")
    List<GalleryAlbum> findApprovedWithPhotos();

    @EntityGraph(attributePaths = "photos")
    @Query("select a from GalleryAlbum a where a.createdBy.id = :uid order by a.createdAt desc")
    List<GalleryAlbum> findByCreatedBy_IdWithPhotos(@Param("uid") UUID userId);

    @EntityGraph(attributePaths = {"photos", "createdBy"})
    @Query("select a from GalleryAlbum a where a.id = :id")
    Optional<GalleryAlbum> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"photos", "createdBy"})
    @Query("select a from GalleryAlbum a order by a.createdAt desc")
    List<GalleryAlbum> findAllForAdmin();
}
