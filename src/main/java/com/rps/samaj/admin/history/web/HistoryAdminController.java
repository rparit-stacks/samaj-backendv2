package com.rps.samaj.admin.history.web;

import com.rps.samaj.api.dto.HistoryDtos;
import com.rps.samaj.history.SamajHistoryService;
import com.rps.samaj.security.DevUserContextFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/admin/history")
public class HistoryAdminController {

    private final SamajHistoryService samajHistoryService;

    public HistoryAdminController(SamajHistoryService samajHistoryService) {
        this.samajHistoryService = samajHistoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HistoryDtos.HistoryResponse create(
            Authentication auth,
            @RequestAttribute(name = DevUserContextFilter.ATTR_ADMIN_USER_ID, required = false) UUID devAdminUserId,
            @Valid @RequestBody HistoryDtos.HistoryCreateRequest body
    ) {
        return samajHistoryService.adminCreate(requireAdminUserId(auth, devAdminUserId), body);
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

    @PutMapping("/{id}")
    public HistoryDtos.HistoryResponse update(@PathVariable long id, @Valid @RequestBody HistoryDtos.HistoryUpdateRequest body) {
        return samajHistoryService.adminUpdate(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        samajHistoryService.adminDelete(id);
    }

    private static UUID requireAdminUserId(Authentication auth, UUID devAdminUserId) {
        if (auth != null && auth.getPrincipal() instanceof UUID u) {
            return u;
        }
        if (devAdminUserId != null) {
            return devAdminUserId;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}

