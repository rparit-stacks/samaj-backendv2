package com.rps.samaj.directory.web;

import com.rps.samaj.api.dto.AdminDirectoryDtos;
import com.rps.samaj.directory.AdminDirectoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/directory")
public class AdminDirectoryController {

    private final AdminDirectoryService adminDirectoryService;

    public AdminDirectoryController(AdminDirectoryService adminDirectoryService) {
        this.adminDirectoryService = adminDirectoryService;
    }

    @GetMapping
    public List<AdminDirectoryDtos.DirectoryEntrySummary> list(
            @RequestParam(required = false) String q
    ) {
        return adminDirectoryService.list(q);
    }

    @GetMapping("/{userId}")
    public AdminDirectoryDtos.DirectoryEntryDetail get(@PathVariable UUID userId) {
        return adminDirectoryService.get(userId);
    }

    @PutMapping("/{userId}")
    public AdminDirectoryDtos.DirectoryEntryDetail update(
            @PathVariable UUID userId,
            @RequestBody AdminDirectoryDtos.DirectoryEntryUpdateRequest body
    ) {
        return adminDirectoryService.update(userId, body);
    }

    @DeleteMapping("/{userId}")
    public void delete(@PathVariable UUID userId) {
        adminDirectoryService.delete(userId);
    }
}

