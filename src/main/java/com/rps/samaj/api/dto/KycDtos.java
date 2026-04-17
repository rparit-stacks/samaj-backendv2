package com.rps.samaj.api.dto;

import java.time.Instant;
import java.util.Map;

public final class KycDtos {

    private KycDtos() {
    }

    public record KycSubmissionResponse(
            String id,
            String userId,
            String userEmail,
            String userName,
            String status,
            Map<String, String> documentUrls,
            String idDocumentType,
            Instant submittedAt,
            Instant reviewedAt,
            String reviewerUserId,
            String reviewNotes
    ) {
    }

    public record KycSubmitRequest(
            Map<String, String> documentUrls,
            String idDocumentType
    ) {
    }

    public record KycReviewRequest(String notes) {
    }

    public record KycMeResponse(
            String kycStatus,
            KycSubmissionResponse latestSubmission
    ) {
    }
}
