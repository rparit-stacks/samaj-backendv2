package com.rps.samaj.business.web;

import com.rps.samaj.api.dto.BusinessDtos;
import com.rps.samaj.business.BusinessService;
import com.rps.samaj.security.JwtAuthenticationFilter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/business")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping
    public BusinessDtos.BusinessPageResponse list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return businessService.listApproved(category, page, size);
    }

    @GetMapping("/{id}")
    public BusinessDtos.BusinessDetail get(@PathVariable UUID id) {
        return businessService.getApprovedAndTrack(id);
    }

    @GetMapping("/my")
    public BusinessDtos.BusinessPageResponse listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return businessService.listMine(requireUserId(), page, size);
    }

    @GetMapping("/my/{id}")
    public BusinessDtos.BusinessDetail getMine(@PathVariable UUID id) {
        return businessService.getMine(requireUserId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessDtos.BusinessDetail create(@Valid @RequestBody BusinessDtos.BusinessCreateRequest req) {
        return businessService.create(requireUserId(), req);
    }

    @PutMapping("/{id}")
    public BusinessDtos.BusinessDetail update(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessDtos.BusinessUpdateRequest req
    ) {
        return businessService.update(requireUserId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        businessService.delete(requireUserId(), id);
    }

    private static UUID requireUserId() {
        UUID id = JwtAuthenticationFilter.currentUserIdOrNull();
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return id;
    }
}
