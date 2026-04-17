package com.rps.samaj.document.web;

import com.rps.samaj.api.dto.DocumentDtos;
import com.rps.samaj.document.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<DocumentDtos.DocumentResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search
    ) {
        return documentService.listPublished(category, search);
    }

    @GetMapping("/me")
    public List<DocumentDtos.DocumentResponse> listMine() {
        return documentService.listMine();
    }

    @GetMapping("/{id}")
    public DocumentDtos.DocumentResponse get(@PathVariable UUID id) {
        return documentService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDtos.DocumentResponse create(@Valid @RequestBody DocumentDtos.DocumentCreateRequest body) {
        return documentService.create(body);
    }

    @PostMapping("/{id}/download")
    public DocumentDtos.DownloadUrlResponse recordDownload(@PathVariable UUID id) {
        return documentService.recordDownload(id);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Void> downloadFile(@PathVariable UUID id) {
        DocumentDtos.DownloadUrlResponse r = documentService.recordDownload(id);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(r.fileUrl())).build();
    }
}
