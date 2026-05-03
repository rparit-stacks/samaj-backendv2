package com.rps.samaj.job.web;

import com.rps.samaj.api.dto.JobDtos;
import com.rps.samaj.job.JobService;
import com.rps.samaj.security.JwtAuthenticationFilter;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public JobDtos.JobPageResponse list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String jobType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return jobService.listApproved(category, jobType, page, size);
    }

    @GetMapping("/{id}")
    public JobDtos.JobDetail get(@PathVariable UUID id) {
        return jobService.getAndTrack(id);
    }

    @GetMapping("/my")
    public JobDtos.JobPageResponse listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return jobService.listMine(requireUserId(), page, size);
    }

    @GetMapping("/my/{id}")
    public JobDtos.JobDetail getMine(@PathVariable UUID id) {
        return jobService.getMine(requireUserId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobDtos.JobDetail submit(@Valid @RequestBody JobDtos.JobCreateRequest req) {
        return jobService.submitJob(requireUserId(), req);
    }

    @PutMapping("/{id}")
    public JobDtos.JobDetail update(@PathVariable UUID id, @Valid @RequestBody JobDtos.JobUpdateRequest req) {
        return jobService.updateOwn(requireUserId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        jobService.deleteOwn(requireUserId(), id);
    }

    private static UUID requireUserId() {
        UUID id = JwtAuthenticationFilter.currentUserIdOrNull();
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return id;
    }
}
