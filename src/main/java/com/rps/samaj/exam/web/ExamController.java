package com.rps.samaj.exam.web;

import com.rps.samaj.api.dto.ExamDtos.ExamResponse;
import com.rps.samaj.api.dto.ExamDtos.MessageResponse;
import com.rps.samaj.api.dto.ExamDtos.PageResponse;
import com.rps.samaj.exam.ExamService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public PageResponse<ExamResponse> list(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String filter
    ) {
        return examService.list(requireUser(auth), page, size, q, type, filter);
    }

    @GetMapping("/saved")
    public PageResponse<ExamResponse> listSaved(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return examService.listSaved(requireUser(auth), page, size);
    }

    @GetMapping("/alerts")
    public PageResponse<ExamResponse> listAlerts(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return examService.listAlerts(requireUser(auth), page, size);
    }

    @GetMapping("/{id}")
    public ExamResponse get(Authentication auth, @PathVariable UUID id) {
        return examService.get(requireUser(auth), id);
    }

    @PostMapping("/{id}/save")
    public MessageResponse save(Authentication auth, @PathVariable UUID id) {
        return examService.save(requireUser(auth), id);
    }

    @DeleteMapping("/{id}/save")
    public MessageResponse unsave(Authentication auth, @PathVariable UUID id) {
        return examService.unsave(requireUser(auth), id);
    }

    @PostMapping("/{id}/alert")
    public MessageResponse enableAlert(Authentication auth, @PathVariable UUID id) {
        return examService.enableAlert(requireUser(auth), id);
    }

    @DeleteMapping("/{id}/alert")
    public MessageResponse disableAlert(Authentication auth, @PathVariable UUID id) {
        return examService.disableAlert(requireUser(auth), id);
    }

    private static UUID requireUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return u;
    }
}
