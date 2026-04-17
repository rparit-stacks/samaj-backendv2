package com.rps.samaj.suggestion.web;

import com.rps.samaj.api.dto.SuggestionDtos.PageResponse;
import com.rps.samaj.api.dto.SuggestionDtos.SuggestionCreateRequest;
import com.rps.samaj.api.dto.SuggestionDtos.SuggestionResponse;
import com.rps.samaj.suggestion.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuggestionResponse create(Authentication auth, @Valid @RequestBody SuggestionCreateRequest body) {
        return suggestionService.create(requireUser(auth), body);
    }

    @GetMapping("/me")
    public PageResponse<SuggestionResponse> listMine(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status
    ) {
        return suggestionService.listMine(requireUser(auth), page, size, q, status);
    }

    @GetMapping("/{id}")
    public SuggestionResponse getMine(Authentication auth, @PathVariable UUID id) {
        return suggestionService.getMine(requireUser(auth), id);
    }

    private static UUID requireUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return u;
    }
}
