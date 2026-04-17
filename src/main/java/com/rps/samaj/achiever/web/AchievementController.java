package com.rps.samaj.achiever.web;

import com.rps.samaj.api.dto.AchievementDtos;
import com.rps.samaj.achiever.AchievementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping("/marquee")
    public List<AchievementDtos.AchievementMarqueeCard> marquee(Authentication auth) {
        requireUser(auth);
        return achievementService.listMarquee();
    }

    /** Active field templates for the dynamic achievement form (read-only). */
    @GetMapping("/field-templates")
    public List<AchievementDtos.AchievementFieldTemplateResponse> fieldTemplates(Authentication auth) {
        requireUser(auth);
        return achievementService.listTemplates(true);
    }

    @GetMapping
    public AchievementDtos.AchievementPageResponse list(
            @RequestParam(defaultValue = "approved") String view,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        UUID uid = requireUser(auth);
        if ("mine".equalsIgnoreCase(view)) {
            return achievementService.pageMine(uid, page, size);
        }
        return achievementService.pageApproved(page, size);
    }

    @GetMapping("/{id}")
    public AchievementDtos.AchievementDetailResponse get(@PathVariable UUID id, Authentication auth) {
        return achievementService.get(requireUser(auth), id);
    }

    @PostMapping
    public AchievementDtos.AchievementDetailResponse create(
            Authentication auth,
            @Valid @RequestBody AchievementDtos.AchievementCreateRequest body
    ) {
        return achievementService.create(requireUser(auth), body);
    }

    @PutMapping("/{id}")
    public AchievementDtos.AchievementDetailResponse update(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody AchievementDtos.AchievementUpdateRequest body
    ) {
        return achievementService.updateOwn(requireUser(auth), id, body);
    }

    private static UUID requireUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return u;
    }
}
