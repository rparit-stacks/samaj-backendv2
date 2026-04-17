package com.rps.samaj.admin.users.web;

import com.rps.samaj.api.dto.AdminUserDtos;
import com.rps.samaj.admin.users.AdminUserManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/admin/users")
@PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
public class AdminUserController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping
    public AdminUserDtos.UserPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        return adminUserManagementService.list(q, role, status, page, size);
    }

    @GetMapping("/{id}")
    public AdminUserDtos.UserFullDetail get(@PathVariable UUID id) {
        return adminUserManagementService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserDtos.UserCreateResponse create(@Valid @RequestBody AdminUserDtos.UserCreateRequest body) {
        return adminUserManagementService.create(body);
    }

    @PutMapping("/{id}")
    public AdminUserDtos.UserSummary update(
            @PathVariable UUID id,
            @RequestBody AdminUserDtos.UserUpdateRequest body
    ) {
        return adminUserManagementService.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        adminUserManagementService.deleteSoft(id);
    }
}
