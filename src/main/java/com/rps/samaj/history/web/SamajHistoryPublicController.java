package com.rps.samaj.history.web;

import com.rps.samaj.api.dto.HistoryDtos;
import com.rps.samaj.history.SamajHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Read-only timeline for logged-in members (same data admins curate under {@code /admin/history}).
 */
@RestController
@RequestMapping("/api/v1/history")
public class SamajHistoryPublicController {

    private final SamajHistoryService samajHistoryService;

    public SamajHistoryPublicController(SamajHistoryService samajHistoryService) {
        this.samajHistoryService = samajHistoryService;
    }

    @GetMapping
    public HistoryDtos.PageResponse<HistoryDtos.HistoryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String q
    ) {
        return samajHistoryService.adminList(page, size, type, fromDate, toDate, q);
    }

    @GetMapping("/{id}")
    public HistoryDtos.HistoryResponse get(@PathVariable long id) {
        return samajHistoryService.adminGet(id);
    }
}
