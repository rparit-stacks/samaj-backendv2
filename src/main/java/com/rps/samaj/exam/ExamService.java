package com.rps.samaj.exam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.ExamDtos;
import com.rps.samaj.api.dto.ExamDtos.ExamResponse;
import com.rps.samaj.api.dto.ExamDtos.MessageResponse;
import com.rps.samaj.api.dto.ExamDtos.PageResponse;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ExamService {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final ExamRepository examRepository;
    private final ExamSavedRepository examSavedRepository;
    private final ExamAlertRepository examAlertRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ExamPaperValidator examPaperValidator;

    public ExamService(
            ExamRepository examRepository,
            ExamSavedRepository examSavedRepository,
            ExamAlertRepository examAlertRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            ExamPaperValidator examPaperValidator
    ) {
        this.examRepository = examRepository;
        this.examSavedRepository = examSavedRepository;
        this.examAlertRepository = examAlertRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.examPaperValidator = examPaperValidator;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExamResponse> list(UUID userId, int page, int size, String q, String type, String filter) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        String qn = q == null || q.isBlank() ? null : q.trim();
        String tn = type == null || type.isBlank() || "all".equalsIgnoreCase(type) ? null : type.trim().toLowerCase();
        String fn = filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter) ? null : filter.trim().toLowerCase();
        Page<Exam> pg = examRepository.searchPublished(qn, tn, fn, PageRequest.of(p, s));
        return toExamPage(pg, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ExamResponse> listSaved(UUID userId, int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        Page<ExamSaved> pg = examSavedRepository.findByUser_IdOrderByExam_CreatedAtDesc(userId, PageRequest.of(p, s));
        List<ExamResponse> content = pg.stream()
                .map(es -> toDto(es.getExam(), userId, true, alertEnabled(userId, es.getExam().getId()), false))
                .toList();
        return new PageResponse<>(content, pg.getTotalElements(), pg.getTotalPages(), pg.getNumber(), pg.getSize());
    }

    @Transactional(readOnly = true)
    public PageResponse<ExamResponse> listAlerts(UUID userId, int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        Page<ExamAlert> pg = examAlertRepository.findByUser_IdOrderByExam_CreatedAtDesc(userId, PageRequest.of(p, s));
        List<ExamResponse> content = pg.stream()
                .map(ea -> toDto(ea.getExam(), userId, true, true, false))
                .toList();
        return new PageResponse<>(content, pg.getTotalElements(), pg.getTotalPages(), pg.getNumber(), pg.getSize());
    }

    @Transactional(readOnly = true)
    public ExamResponse get(UUID userId, UUID examId) {
        Exam e = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        boolean saved = userId != null && examSavedRepository.findByUser_IdAndExam_Id(userId, examId).isPresent();
        boolean alert = userId != null && examAlertRepository.findByUser_IdAndExam_Id(userId, examId).isPresent();
        return toDto(e, userId, saved, alert, true);
    }

    public MessageResponse save(UUID userId, UUID examId) {
        requireExam(examId);
        userRepository.getReferenceById(userId);
        if (examSavedRepository.findByUser_IdAndExam_Id(userId, examId).isPresent()) {
            return new MessageResponse("Already saved");
        }
        Exam exam = examRepository.getReferenceById(examId);
        examSavedRepository.save(new ExamSaved(UUID.randomUUID(), userRepository.getReferenceById(userId), exam));
        return new MessageResponse("Saved");
    }

    public MessageResponse unsave(UUID userId, UUID examId) {
        requireExam(examId);
        examSavedRepository.deleteByUser_IdAndExam_Id(userId, examId);
        return new MessageResponse("Removed");
    }

    public MessageResponse enableAlert(UUID userId, UUID examId) {
        requireExam(examId);
        if (examAlertRepository.findByUser_IdAndExam_Id(userId, examId).isPresent()) {
            return new MessageResponse("Alert already on");
        }
        Exam exam = examRepository.getReferenceById(examId);
        examAlertRepository.save(new ExamAlert(UUID.randomUUID(), userRepository.getReferenceById(userId), exam));
        return new MessageResponse("Alert enabled");
    }

    public MessageResponse disableAlert(UUID userId, UUID examId) {
        requireExam(examId);
        examAlertRepository.deleteByUser_IdAndExam_Id(userId, examId);
        return new MessageResponse("Alert disabled");
    }

    @Transactional(readOnly = true)
    public PageResponse<ExamResponse> adminList(int page, int size, String q) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        String qn = q == null || q.isBlank() ? null : q.trim();
        Page<Exam> pg = examRepository.searchPublished(qn, null, null, PageRequest.of(p, s));
        return toExamPage(pg, null);
    }

    public ExamResponse adminCreate(ExamDtos.ExamCreateRequest body) {
        examPaperValidator.validateOrNull(body.paper());
        Exam e = new Exam(UUID.randomUUID(), body.title(), body.description(), body.type());
        applyDates(e, body.notificationDate(), body.lastDate(), body.examDate());
        e.setEligibility(body.eligibility());
        e.setApplyUrl(body.applyUrl());
        e.setExpired(body.expired());
        applyPaper(e, body.paper());
        return toDto(examRepository.save(e), null, false, false, true);
    }

    public ExamResponse adminUpdate(UUID examId, ExamDtos.ExamUpdateRequest body) {
        Exam e = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        if (body.title() != null) {
            e.setTitle(body.title());
        }
        if (body.description() != null) {
            e.setDescription(body.description());
        }
        if (body.type() != null) {
            e.setType(body.type());
        }
        if (body.expired() != null) {
            e.setExpired(body.expired());
        }
        applyDates(e, body.notificationDate(), body.lastDate(), body.examDate());
        if (body.eligibility() != null) {
            e.setEligibility(body.eligibility());
        }
        if (body.applyUrl() != null) {
            e.setApplyUrl(body.applyUrl());
        }
        if (body.paper() != null) {
            if (body.paper().isNull()) {
                e.setPaperJson(null);
            } else {
                examPaperValidator.validateOrNull(body.paper());
                e.setPaperJson(body.paper().toString());
            }
        }
        e.setUpdatedAt(Instant.now());
        return toDto(examRepository.save(e), null, false, false, true);
    }

    public void adminDelete(UUID examId) {
        if (!examRepository.existsById(examId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found");
        }
        examSavedRepository.deleteByExam_Id(examId);
        examAlertRepository.deleteByExam_Id(examId);
        examRepository.deleteById(examId);
    }

    private void requireExam(UUID examId) {
        if (!examRepository.existsById(examId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found");
        }
    }

    private boolean alertEnabled(UUID userId, UUID examId) {
        return userId != null && examAlertRepository.findByUser_IdAndExam_Id(userId, examId).isPresent();
    }

    private PageResponse<ExamResponse> toExamPage(Page<Exam> pg, UUID userId) {
        List<Exam> list = pg.getContent();
        Set<UUID> savedIds = new HashSet<>();
        Set<UUID> alertIds = new HashSet<>();
        if (userId != null && !list.isEmpty()) {
            List<UUID> ids = list.stream().map(Exam::getId).toList();
            savedIds.addAll(examSavedRepository.findExamIdsSaved(userId, ids));
            alertIds.addAll(examAlertRepository.findExamIdsWithAlert(userId, ids));
        }
        List<ExamResponse> content = list.stream()
                .map(e -> toDto(e, userId, savedIds.contains(e.getId()), alertIds.contains(e.getId()), false))
                .toList();
        return new PageResponse<>(content, pg.getTotalElements(), pg.getTotalPages(), pg.getNumber(), pg.getSize());
    }

    private ExamResponse toDto(Exam e, UUID userId, boolean saved, boolean alertEnabled, boolean includePaper) {
        return new ExamResponse(
                e.getId().toString(),
                e.getTitle(),
                e.getDescription(),
                e.getType(),
                localDateIso(e.getNotificationDate()),
                localDateIso(e.getLastDate()),
                localDateIso(e.getExamDate()),
                e.getEligibility(),
                e.getApplyUrl(),
                effectiveExpired(e),
                saved,
                alertEnabled,
                iso(e.getCreatedAt()),
                iso(e.getUpdatedAt()),
                includePaper ? readPaperJson(e.getPaperJson()) : null
        );
    }

    private void applyPaper(Exam e, JsonNode paper) {
        if (paper == null || paper.isNull()) {
            e.setPaperJson(null);
            return;
        }
        e.setPaperJson(paper.toString());
    }

    private JsonNode readPaperJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Stored exam paper is invalid JSON");
        }
    }

    private static boolean effectiveExpired(Exam e) {
        if (e.isExpired()) {
            return true;
        }
        LocalDate last = e.getLastDate();
        return last != null && last.isBefore(LocalDate.now(ZONE));
    }

    private static String localDateIso(LocalDate d) {
        return d == null ? null : d.toString();
    }

    private static String iso(Instant i) {
        return i == null ? null : DateTimeFormatter.ISO_INSTANT.format(i);
    }

    private static void applyDates(Exam e, String notif, String last, String exam) {
        if (notif != null) {
            e.setNotificationDate(notif.isBlank() ? null : LocalDate.parse(notif));
        }
        if (last != null) {
            e.setLastDate(last.isBlank() ? null : LocalDate.parse(last));
        }
        if (exam != null) {
            e.setExamDate(exam.isBlank() ? null : LocalDate.parse(exam));
        }
    }
}
