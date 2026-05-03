package com.rps.samaj.job.web;

import com.rps.samaj.api.dto.JobDtos;
import com.rps.samaj.job.JobService;
import com.rps.samaj.security.DevUserContextFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/admin/jobs")
public class JobAdminController {

    private final JobService jobService;

    public JobAdminController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public JobDtos.JobAdminPageResponse list(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        require(adminId);
        return jobService.adminList(status, page, size);
    }

    @GetMapping("/{id}")
    public JobDtos.JobDetail get(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id
    ) {
        require(adminId);
        return jobService.adminGet(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobDtos.JobDetail create(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @Valid @RequestBody JobDtos.JobCreateRequest req
    ) {
        require(adminId);
        return jobService.adminCreate(req);
    }

    @PutMapping("/{id}")
    public JobDtos.JobDetail update(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id,
            @Valid @RequestBody JobDtos.JobUpdateRequest req
    ) {
        require(adminId);
        return jobService.adminUpdate(id, req);
    }

    @PostMapping("/{id}/approve")
    public JobDtos.JobDetail approve(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id,
            @RequestBody(required = false) JobDtos.AdminApproveRequest req
    ) {
        require(adminId);
        return jobService.adminApprove(id, req);
    }

    @PostMapping("/{id}/reject")
    public JobDtos.JobDetail reject(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id,
            @Valid @RequestBody JobDtos.AdminRejectRequest req
    ) {
        require(adminId);
        return jobService.adminReject(id, req);
    }

    @PostMapping("/{id}/toggle-featured")
    public JobDtos.JobDetail toggleFeatured(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id
    ) {
        require(adminId);
        return jobService.adminToggleFeatured(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id
    ) {
        require(adminId);
        jobService.adminDelete(id);
    }

    private static void require(UUID adminId) {
        if (adminId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin context required");
    }
}
