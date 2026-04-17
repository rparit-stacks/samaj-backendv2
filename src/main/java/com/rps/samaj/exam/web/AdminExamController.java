package com.rps.samaj.exam.web;

import com.rps.samaj.api.dto.ExamDtos;
import com.rps.samaj.api.dto.ExamDtos.ExamResponse;
import com.rps.samaj.api.dto.ExamDtos.PageResponse;
import com.rps.samaj.exam.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/exam")
public class AdminExamController {

    private final ExamService examService;

    public AdminExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public PageResponse<ExamResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q
    ) {
        return examService.adminList(page, size, q);
    }

    @GetMapping("/{id}")
    public ExamResponse get(@PathVariable UUID id) {
        return examService.get(null, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse create(@Valid @RequestBody ExamDtos.ExamCreateRequest body) {
        return examService.adminCreate(body);
    }

    @PutMapping("/{id}")
    public ExamResponse update(@PathVariable UUID id, @Valid @RequestBody ExamDtos.ExamUpdateRequest body) {
        return examService.adminUpdate(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        examService.adminDelete(id);
    }
}
