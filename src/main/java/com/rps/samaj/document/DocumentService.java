package com.rps.samaj.document;

import com.rps.samaj.api.dto.DocumentDtos;
import com.rps.samaj.config.cache.RedisCacheConfig;
import com.rps.samaj.notification.PublicNotificationPublisher;
import com.rps.samaj.security.JwtAuthenticationFilter;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PublicNotificationPublisher notificationPublisher;

    public DocumentService(
            DocumentRepository documentRepository,
            UserRepository userRepository,
            PublicNotificationPublisher notificationPublisher
    ) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = RedisCacheConfig.Names.DOCUMENTS_PUBLISHED,
            key = "(#category == null ? 'all' : #category).concat(':').concat(#search == null ? '' : #search)"
    )
    public List<DocumentDtos.DocumentResponse> listPublished(String category, String search) {
        requireUser();
        String c = category == null || category.isBlank() ? null : category.trim();
        String q = search == null || search.isBlank() ? null : search.trim();
        return documentRepository.findPublishedFiltered(c, q).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentResponse> listMine() {
        UUID uid = requireUserId();
        return documentRepository.findByCreatedBy_IdOrderByCreatedAtDesc(uid).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.DOCUMENT_DETAIL, key = "#id.toString()")
    public DocumentDtos.DocumentResponse get(UUID id) {
        UUID viewer = requireUserId();
        AppDocument d = documentRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!canView(d, viewer)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        return toDto(d);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.DOCUMENTS_PUBLISHED,
            RedisCacheConfig.Names.DOCUMENT_DETAIL
    }, allEntries = true)
    public DocumentDtos.DocumentResponse create(DocumentDtos.DocumentCreateRequest body) {
        UUID uid = requireUserId();
        User user = userRepository.findById(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        String vis = body.visibility() == null ? "PUBLIC" : body.visibility().trim().toUpperCase();
        if (!vis.equals("PUBLIC") && !vis.equals("PRIVATE")) {
            vis = "PUBLIC";
        }
        AppDocument d = new AppDocument(
                UUID.randomUUID(),
                user,
                body.title().trim(),
                body.fileUrl().trim(),
                body.fileName().trim(),
                body.category().trim().toLowerCase(),
                vis
        );
        d.setDescription(trim(body.description()));
        d.setFileSize(body.fileSize());
        d.setFileType(trim(body.fileType()));
        d.setApproved(false);
        documentRepository.save(d);
        return toDto(d);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.DOCUMENTS_PUBLISHED,
            RedisCacheConfig.Names.DOCUMENT_DETAIL
    }, allEntries = true)
    public DocumentDtos.DocumentResponse setApproval(UUID id, DocumentDtos.DocumentApprovalRequest body) {
        AppDocument d = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        boolean wasApproved = d.isApproved();
        d.setApproved(body.approved());
        if (body.approved()) {
            d.setRejectionReason(null);
        } else {
            d.setRejectionReason(body.rejectionReason() != null ? body.rejectionReason().trim() : null);
            if (d.getRejectionReason() != null && d.getRejectionReason().isEmpty()) {
                d.setRejectionReason(null);
            }
        }
        documentRepository.save(d);
        if (d.isApproved() && !wasApproved && "PUBLIC".equalsIgnoreCase(d.getVisibility())) {
            notificationPublisher.onDocumentApprovedPublic(d.getId(), d.getTitle());
        }
        return toDto(d);
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentResponse> listPending() {
        return documentRepository.findByApprovedIsFalseOrderByCreatedAtAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentDtos.DocumentResponse> adminList(String q, String category, Boolean approved) {
        return documentRepository.findForAdmin(blankToNull(q), blankToNull(category), approved).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDtos.DocumentResponse adminGet(UUID id) {
        AppDocument d = documentRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return toDto(d);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.DOCUMENTS_PUBLISHED,
            RedisCacheConfig.Names.DOCUMENT_DETAIL
    }, allEntries = true)
    public DocumentDtos.DocumentResponse adminUpdate(UUID id, DocumentDtos.DocumentAdminUpdateRequest body) {
        AppDocument d = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        boolean wasApproved = d.isApproved();
        if (body.title() != null && !body.title().isBlank()) {
            d.setTitle(body.title().trim());
        }
        if (body.description() != null) {
            d.setDescription(trim(body.description()));
        }
        if (body.category() != null && !body.category().isBlank()) {
            d.setCategory(body.category().trim().toLowerCase());
        }
        if (body.visibility() != null && !body.visibility().isBlank()) {
            String vis = body.visibility().trim().toUpperCase();
            if (!vis.equals("PUBLIC") && !vis.equals("PRIVATE")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "visibility must be PUBLIC or PRIVATE");
            }
            d.setVisibility(vis);
        }
        if (body.approved() != null) {
            d.setApproved(body.approved());
        }
        if (body.rejectionReason() != null) {
            String r = body.rejectionReason().trim();
            d.setRejectionReason(r.isEmpty() ? null : r);
        }
        if (d.isApproved()) {
            d.setRejectionReason(null);
        }
        documentRepository.save(d);
        AppDocument fresh = documentRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (fresh.isApproved() && !wasApproved && "PUBLIC".equalsIgnoreCase(fresh.getVisibility())) {
            notificationPublisher.onDocumentApprovedPublic(fresh.getId(), fresh.getTitle());
        }
        return toDto(fresh);
    }

    @Transactional
    public void adminDelete(UUID id) {
        AppDocument d = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        documentRepository.delete(d);
    }

    @Transactional
    public DocumentDtos.DownloadUrlResponse recordDownload(UUID id) {
        UUID viewer = requireUserId();
        AppDocument d = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (!canView(d, viewer)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        d.setDownloadCount(d.getDownloadCount() + 1);
        return new DocumentDtos.DownloadUrlResponse(d.getFileUrl());
    }

    private boolean canView(AppDocument d, UUID viewer) {
        if (d.getCreatedBy().getId().equals(viewer)) {
            return true;
        }
        return d.isApproved() && "PUBLIC".equalsIgnoreCase(d.getVisibility());
    }

    private DocumentDtos.DocumentResponse toDto(AppDocument d) {
        return new DocumentDtos.DocumentResponse(
                d.getId().toString(),
                d.getTitle(),
                d.getDescription(),
                d.getFileUrl(),
                d.getFileName(),
                d.getFileSize(),
                d.getFileType(),
                d.getCategory(),
                d.getVisibility(),
                d.getCreatedBy().getId().toString(),
                d.getCreatedAt() != null ? d.getCreatedAt().toString() : null,
                d.isApproved(),
                d.getRejectionReason(),
                d.getDownloadCount()
        );
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static void requireUser() {
        if (JwtAuthenticationFilter.currentUserIdOrNull() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private static UUID requireUserId() {
        UUID id = JwtAuthenticationFilter.currentUserIdOrNull();
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return id;
    }
}
