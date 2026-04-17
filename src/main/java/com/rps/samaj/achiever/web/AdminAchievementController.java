package com.rps.samaj.achiever.web;

import com.rps.samaj.api.dto.AchievementDtos;
import com.rps.samaj.achiever.AchievementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/admin/achievements")
public class AdminAchievementController {

    private final AchievementService achievementService;

    public AdminAchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    public AchievementDtos.AchievementAdminPageResponse list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return achievementService.adminPage(status, page, size);
    }

    @GetMapping("/{id}")
    public AchievementDtos.AchievementDetailResponse get(@PathVariable UUID id) {
        return achievementService.adminGet(id);
    }

    @PutMapping("/{id}")
    public AchievementDtos.AchievementDetailResponse fullUpdate(
            @PathVariable UUID id,
            @Valid @RequestBody AchievementDtos.AchievementAdminUpdateRequest body
    ) {
        return achievementService.adminFullUpdate(id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        achievementService.adminDelete(id);
    }

    @PostMapping("/{id}/approve")
    public AchievementDtos.AchievementDetailResponse approve(
            @PathVariable UUID id,
            @RequestBody(required = false) AchievementDtos.AchievementApproveRequest body
    ) {
        AchievementDtos.AchievementApproveRequest req = body != null ? body : new AchievementDtos.AchievementApproveRequest(null, null);
        return achievementService.adminApprove(id, req);
    }

    @PostMapping("/{id}/reject")
    public AchievementDtos.AchievementDetailResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody AchievementDtos.AchievementRejectRequest body
    ) {
        return achievementService.adminReject(id, body);
    }

    @PatchMapping("/{id}/marquee")
    public AchievementDtos.AchievementDetailResponse patchMarquee(
            @PathVariable UUID id,
            @RequestBody AchievementDtos.AchievementMarqueeAdminPatch body
    ) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body required");
        }
        return achievementService.adminPatchMarquee(id, body);
    }
}
