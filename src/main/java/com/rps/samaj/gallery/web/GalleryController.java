package com.rps.samaj.gallery.web;

import com.rps.samaj.api.dto.GalleryDtos;
import com.rps.samaj.gallery.GalleryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gallery/albums")
public class GalleryController {

    private final GalleryService galleryService;

    public GalleryController(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    @GetMapping
    public List<GalleryDtos.GalleryAlbumResponse> listApproved() {
        return galleryService.listApproved();
    }

    @GetMapping("/me")
    public List<GalleryDtos.GalleryAlbumResponse> listMine() {
        return galleryService.listMine();
    }

    @GetMapping("/{id}")
    public GalleryDtos.GalleryAlbumDetailResponse get(@PathVariable UUID id) {
        return galleryService.getAlbum(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GalleryDtos.GalleryAlbumResponse create(@Valid @RequestBody GalleryDtos.GalleryAlbumCreateRequest body) {
        return galleryService.create(body);
    }
}
