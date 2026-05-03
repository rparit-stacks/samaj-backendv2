package com.rps.samaj.business.web;

import com.rps.samaj.api.dto.BusinessDtos;
import com.rps.samaj.business.BusinessService;
import com.rps.samaj.security.DevUserContextFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/admin/business")
public class BusinessAdminController {

    private final BusinessService businessService;

    public BusinessAdminController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping
    public BusinessDtos.BusinessAdminPageResponse list(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        requireAdmin(adminId);
        return businessService.adminList(status, page, size);
    }

    @GetMapping("/{id}")
    public BusinessDtos.BusinessDetail get(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id
    ) {
        requireAdmin(adminId);
        return businessService.adminGet(id);
    }

    @PostMapping("/{id}/approve")
    public BusinessDtos.BusinessDetail approve(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id,
            @RequestBody(required = false) BusinessDtos.AdminApproveRequest req
    ) {
        requireAdmin(adminId);
        return businessService.adminApprove(id, req);
    }

    @PostMapping("/{id}/reject")
    public BusinessDtos.BusinessDetail reject(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id,
            @Valid @RequestBody BusinessDtos.AdminRejectRequest req
    ) {
        requireAdmin(adminId);
        return businessService.adminReject(id, req);
    }

    @PostMapping("/{id}/ban")
    public BusinessDtos.BusinessDetail ban(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id
    ) {
        requireAdmin(adminId);
        return businessService.adminBan(id);
    }

    @PostMapping("/{id}/toggle-featured")
    public BusinessDtos.BusinessDetail toggleFeatured(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id
    ) {
        requireAdmin(adminId);
        return businessService.adminToggleFeatured(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @PathVariable UUID id
    ) {
        requireAdmin(adminId);
        businessService.adminDelete(id);
    }

    private static void requireAdmin(UUID adminId) {
        if (adminId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin context required");
    }
}
