package com.rps.samaj.achiever.web;

import com.rps.samaj.api.dto.AchievementDtos;
import com.rps.samaj.achiever.AchievementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/achievement-templates")
public class AdminAchievementFieldTemplateController {

    private final AchievementService achievementService;

    public AdminAchievementFieldTemplateController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    public List<AchievementDtos.AchievementFieldTemplateResponse> list(
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return achievementService.listTemplates(activeOnly);
    }

    @PostMapping
    public AchievementDtos.AchievementFieldTemplateResponse create(
            @Valid @RequestBody AchievementDtos.AchievementFieldTemplateCreateRequest body
    ) {
        return achievementService.createTemplate(body);
    }

    @PutMapping("/{id}")
    public AchievementDtos.AchievementFieldTemplateResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody AchievementDtos.AchievementFieldTemplateUpdateRequest body
    ) {
        return achievementService.updateTemplate(id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        achievementService.deleteTemplate(id);
    }
}
