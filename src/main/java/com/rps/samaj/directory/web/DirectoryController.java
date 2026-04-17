package com.rps.samaj.directory.web;

import com.rps.samaj.api.dto.DirectoryDtos;
import com.rps.samaj.directory.DirectoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/directory")
public class DirectoryController {

    private final DirectoryService directoryService;

    public DirectoryController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping
    public List<DirectoryDtos.DirectoryProfileSummary> list() {
        return directoryService.listSummaries();
    }

    @GetMapping("/{userId}")
    public DirectoryDtos.DirectoryProfileDetail get(@PathVariable UUID userId) {
        return directoryService.getDetail(userId);
    }

    @GetMapping("/me/settings")
    public DirectoryDtos.DirectorySettingsDto getMySettings() {
        return directoryService.getMySettings();
    }

    @PutMapping("/me/settings")
    public DirectoryDtos.DirectorySettingsDto updateMySettings(
            @Valid @RequestBody DirectoryDtos.DirectorySettingsUpdateDto body
    ) {
        return directoryService.updateMySettings(body);
    }
}
