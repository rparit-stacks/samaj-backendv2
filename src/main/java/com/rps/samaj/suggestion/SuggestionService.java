package com.rps.samaj.suggestion;

import com.rps.samaj.api.dto.SuggestionDtos;
import com.rps.samaj.api.dto.SuggestionDtos.PageResponse;
import com.rps.samaj.api.dto.SuggestionDtos.SuggestionCreateRequest;
import com.rps.samaj.api.dto.SuggestionDtos.SuggestionResponse;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional
public class SuggestionService {

    private final SuggestionRepository suggestionRepository;
    private final UserRepository userRepository;

    public SuggestionService(SuggestionRepository suggestionRepository, UserRepository userRepository) {
        this.suggestionRepository = suggestionRepository;
        this.userRepository = userRepository;
    }

    public SuggestionResponse create(UUID userId, SuggestionCreateRequest body) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UUID id = UUID.randomUUID();
        String cat = body.category().trim().toLowerCase();
        Suggestion s = new Suggestion(id, user, body.title().trim(), body.description().trim(), cat);
        suggestionRepository.save(s);
        return toDto(s);
    }

    @Transactional(readOnly = true)
    public PageResponse<SuggestionResponse> listMine(UUID userId, int page, int size, String q, String status) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        String qn = q == null || q.isBlank() ? null : q.trim();
        String st = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) ? null : status.trim();
        Page<Suggestion> pg = suggestionRepository.pageMine(userId, qn, st, PageRequest.of(p, s));
        return new PageResponse<>(
                pg.stream().map(this::toDto).toList(),
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    @Transactional(readOnly = true)
    public SuggestionResponse getMine(UUID userId, UUID suggestionId) {
        Suggestion s = suggestionRepository.findByIdAndUser_Id(suggestionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Suggestion not found"));
        return toDto(s);
    }

    private SuggestionResponse toDto(Suggestion s) {
        return new SuggestionResponse(
                s.getId().toString(),
                s.getTitle(),
                s.getDescription(),
                s.getCategory(),
                s.getStatus(),
                s.getResponse(),
                iso(s.getCreatedAt()),
                iso(s.getUpdatedAt())
        );
    }

    private static String iso(Instant i) {
        return i == null ? null : DateTimeFormatter.ISO_INSTANT.format(i);
    }
}
