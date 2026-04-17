package com.rps.samaj.kyc;

import com.rps.samaj.api.dto.KycDtos;
import com.rps.samaj.security.DevUserContextFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/kyc")
public class AdminKycController {

    private final KycService kycService;

    public AdminKycController(KycService kycService) {
        this.kycService = kycService;
    }

    @GetMapping("/pending")
    public List<KycDtos.KycSubmissionResponse> pending(
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId
    ) {
        requireAdminContext(adminId);
        return kycService.listPending();
    }

    @PostMapping("/{id}/approve")
    public KycDtos.KycSubmissionResponse approve(
            @PathVariable("id") UUID id,
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @RequestBody(required = false) KycDtos.KycReviewRequest body
    ) {
        requireAdminContext(adminId);
        return kycService.approve(id, adminId, body);
    }

    @PostMapping("/{id}/reject")
    public KycDtos.KycSubmissionResponse reject(
            @PathVariable("id") UUID id,
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID adminId,
            @RequestBody(required = false) KycDtos.KycReviewRequest body
    ) {
        requireAdminContext(adminId);
        return kycService.reject(id, adminId, body);
    }

    private static void requireAdminContext(UUID adminId) {
        if (adminId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin context required (JWT or X-Admin-User-Id in dev)");
        }
    }
}
