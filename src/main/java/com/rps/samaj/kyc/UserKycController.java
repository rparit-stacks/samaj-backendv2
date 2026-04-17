package com.rps.samaj.kyc;

import com.rps.samaj.api.dto.KycDtos;
import com.rps.samaj.security.DevUserContextFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/kyc")
public class UserKycController {

    private final KycService kycService;

    public UserKycController(KycService kycService) {
        this.kycService = kycService;
    }

    @GetMapping
    public KycDtos.KycMeResponse me(@RequestAttribute(name = DevUserContextFilter.ATTR_USER_ID, required = false) UUID userId) {
        requireUser(userId);
        return kycService.getForUser(userId);
    }

    @PostMapping("/submit")
    public KycDtos.KycSubmissionResponse submit(
            @RequestAttribute(name = DevUserContextFilter.ATTR_USER_ID, required = false) UUID userId,
            @RequestBody KycDtos.KycSubmitRequest body
    ) {
        requireUser(userId);
        return kycService.submit(userId, body);
    }

    private static void requireUser(UUID userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User context required (JWT or X-User-Id in dev)");
        }
    }
}
