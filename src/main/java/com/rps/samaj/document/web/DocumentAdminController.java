package com.rps.samaj.document.web;

import com.rps.samaj.api.dto.DocumentDtos;
import com.rps.samaj.document.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/documents")
@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
public class DocumentAdminController {

    private final DocumentService documentService;

    public DocumentAdminController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** Pending queue — path must stay before `/{id}` so "pending" is not parsed as UUID. */
    @GetMapping("/pending")
    public List<DocumentDtos.DocumentResponse> pending() {
        return documentService.listPending();
    }

    @GetMapping
    public List<DocumentDtos.DocumentResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean approved
    ) {
        return documentService.adminList(q, category, approved);
    }

    @GetMapping("/{id}")
    public DocumentDtos.DocumentResponse get(@PathVariable UUID id) {
        return documentService.adminGet(id);
    }

    @PatchMapping("/{id}/approval")
    public DocumentDtos.DocumentResponse approval(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentDtos.DocumentApprovalRequest body
    ) {
        return documentService.setApproval(id, body);
    }

    @PatchMapping("/{id}")
    public DocumentDtos.DocumentResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentDtos.DocumentAdminUpdateRequest body
    ) {
        return documentService.adminUpdate(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        documentService.adminDelete(id);
    }
}
