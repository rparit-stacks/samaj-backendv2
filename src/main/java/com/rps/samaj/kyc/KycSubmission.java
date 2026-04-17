package com.rps.samaj.kyc;

import com.rps.samaj.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_kyc_submissions")
public class KycSubmission {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private KycSubmissionStatus status;

    /** JSON map: ID_FRONT, ID_BACK, SELFIE, ADDRESS_PROOF -> URL */
    @Column(name = "document_urls_json", nullable = false, columnDefinition = "text")
    private String documentUrlsJson;

    @Column(name = "id_document_type", length = 64)
    private String idDocumentType;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_user_id")
    private User reviewer;

    @Column(name = "review_notes", columnDefinition = "text")
    private String reviewNotes;

    protected KycSubmission() {
    }

    public KycSubmission(UUID id, User user, String documentUrlsJson, String idDocumentType) {
        this.id = id;
        this.user = user;
        this.status = KycSubmissionStatus.PENDING;
        this.documentUrlsJson = documentUrlsJson;
        this.idDocumentType = idDocumentType;
        this.submittedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public KycSubmissionStatus getStatus() {
        return status;
    }

    public void setStatus(KycSubmissionStatus status) {
        this.status = status;
    }

    public String getDocumentUrlsJson() {
        return documentUrlsJson;
    }

    public void setDocumentUrlsJson(String documentUrlsJson) {
        this.documentUrlsJson = documentUrlsJson;
    }

    public String getIdDocumentType() {
        return idDocumentType;
    }

    public void setIdDocumentType(String idDocumentType) {
        this.idDocumentType = idDocumentType;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }
}
