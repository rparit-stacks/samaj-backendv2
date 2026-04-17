package com.rps.samaj.gallery.web;

import com.rps.samaj.api.dto.GalleryDtos;
import com.rps.samaj.gallery.GalleryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/gallery")
public class AdminGalleryController {

    private final GalleryService galleryService;

    public AdminGalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @GetMapping
    public List<GalleryDtos.AdminGalleryAlbumResponse> list() {
        return galleryService.adminList();
    }

    @GetMapping("/{id}")
    public GalleryDtos.AdminGalleryAlbumResponse get(@PathVariable UUID id) {
        return galleryService.adminGet(id);
    }

    @PutMapping("/{id}")
    public GalleryDtos.AdminGalleryAlbumResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody GalleryDtos.GalleryAlbumUpdateRequest body
    ) {
        return galleryService.adminUpdate(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        galleryService.adminDelete(id);
    }
}
