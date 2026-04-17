package com.rps.samaj.kyc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.KycDtos;
import com.rps.samaj.user.model.KycStatus;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KycService {

    private final KycSubmissionRepository submissionRepo;
    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final ObjectMapper objectMapper;

    public KycService(KycSubmissionRepository submissionRepo, UserRepository userRepo,
                      UserProfileRepository profileRepo, ObjectMapper objectMapper) {
        this.submissionRepo = submissionRepo;
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<KycDtos.KycSubmissionResponse> listPending() {
        return submissionRepo.findByStatusOrderBySubmittedAtAsc(KycSubmissionStatus.PENDING).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public KycDtos.KycMeResponse getForUser(UUID userId) {
        User u = userRepo.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new KycDtos.KycMeResponse(
                u.getKycStatus().name(),
                submissionRepo.findFirstByUser_IdOrderBySubmittedAtDesc(userId).map(this::toDto).orElse(null)
        );
    }

    @Transactional
    public KycDtos.KycSubmissionResponse submit(UUID userId, KycDtos.KycSubmitRequest req) {
        User u = userRepo.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.getKycStatus() == KycStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "KYC already pending review");
        }
        Map<String, String> urls = req.documentUrls();
        if (urls == null || urls.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documentUrls required");
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(urls);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid documentUrls");
        }
        KycSubmission s = new KycSubmission(UUID.randomUUID(), u, json, req.idDocumentType());
        submissionRepo.save(s);
        u.setKycStatus(KycStatus.PENDING);
        u.setUpdatedAt(Instant.now());
        userRepo.save(u);
        return toDto(s);
    }

    @Transactional
    public KycDtos.KycSubmissionResponse approve(UUID submissionId, UUID reviewerId, KycDtos.KycReviewRequest notes) {
        KycSubmission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (s.getStatus() != KycSubmissionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not pending");
        }
        User reviewer = userRepo.findById(reviewerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        s.setStatus(KycSubmissionStatus.APPROVED);
        s.setReviewedAt(Instant.now());
        s.setReviewer(reviewer);
        s.setReviewNotes(notes != null ? notes.notes() : null);
        submissionRepo.save(s);
        User u = s.getUser();
        u.setKycStatus(KycStatus.APPROVED);
        u.setUpdatedAt(Instant.now());
        userRepo.save(u);
        return toDto(s);
    }

    @Transactional
    public KycDtos.KycSubmissionResponse reject(UUID submissionId, UUID reviewerId, KycDtos.KycReviewRequest notes) {
        KycSubmission s = submissionRepo.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (s.getStatus() != KycSubmissionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not pending");
        }
        User reviewer = userRepo.findById(reviewerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        s.setStatus(KycSubmissionStatus.REJECTED);
        s.setReviewedAt(Instant.now());
        s.setReviewer(reviewer);
        s.setReviewNotes(notes != null ? notes.notes() : null);
        submissionRepo.save(s);
        User u = s.getUser();
        u.setKycStatus(KycStatus.REJECTED);
        u.setUpdatedAt(Instant.now());
        userRepo.save(u);
        return toDto(s);
    }

    private KycDtos.KycSubmissionResponse toDto(KycSubmission s) {
        User u = s.getUser();
        Map<String, String> urls = Map.of();
        try {
            urls = objectMapper.readValue(s.getDocumentUrlsJson(), new TypeReference<>() {
            });
        } catch (Exception ignored) {
        }
        String name = null;
        String email = u.getEmail();
        var prof = profileRepo.findById(u.getId());
        if (prof.isPresent()) {
            name = prof.get().getFullName();
        }
        return new KycDtos.KycSubmissionResponse(
                s.getId().toString(),
                u.getId().toString(),
                email,
                name,
                s.getStatus().name(),
                urls,
                s.getIdDocumentType(),
                s.getSubmittedAt(),
                s.getReviewedAt(),
                s.getReviewer() != null ? s.getReviewer().getId().toString() : null,
                s.getReviewNotes()
        );
    }
}
