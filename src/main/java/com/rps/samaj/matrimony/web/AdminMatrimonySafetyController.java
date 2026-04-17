package com.rps.samaj.matrimony.web;

import com.rps.samaj.api.dto.MatrimonyDtos;
import com.rps.samaj.matrimony.MatrimonyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/matrimony/safety")
public class AdminMatrimonySafetyController {

    private final MatrimonyService matrimonyService;

    public AdminMatrimonySafetyController(MatrimonyService matrimonyService) {
        this.matrimonyService = matrimonyService;
    }

    @GetMapping("/interests")
    public MatrimonyDtos.PaginatedAdminMatrimonyInterests listInterests(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return matrimonyService.adminListInterests(q, status, page, size);
    }

    @GetMapping("/blocks")
    public MatrimonyDtos.PaginatedAdminMatrimonyBlocks listBlocks(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return matrimonyService.adminListBlocks(q, page, size);
    }

    @PostMapping("/blocks/{blockingUserId}/{blockedUserId}")
    public MatrimonyDtos.AdminMatrimonyBlockResponse forceBlock(
            @PathVariable UUID blockingUserId,
            @PathVariable UUID blockedUserId
    ) {
        return matrimonyService.adminForceBlockUser(blockingUserId, blockedUserId);
    }

    @DeleteMapping("/blocks/{blockingUserId}/{blockedUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(
            @PathVariable UUID blockingUserId,
            @PathVariable UUID blockedUserId
    ) {
        matrimonyService.adminUnblockUser(blockingUserId, blockedUserId);
    }
}
