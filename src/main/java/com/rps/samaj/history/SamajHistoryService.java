package com.rps.samaj.history;

import com.rps.samaj.api.dto.HistoryDtos;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional
public class SamajHistoryService {

    private final SamajHistoryRepository samajHistoryRepository;
    private final UserRepository userRepository;

    public SamajHistoryService(SamajHistoryRepository samajHistoryRepository, UserRepository userRepository) {
        this.samajHistoryRepository = samajHistoryRepository;
        this.userRepository = userRepository;
    }

    public HistoryDtos.HistoryResponse adminCreate(UUID adminUserId, HistoryDtos.HistoryCreateRequest body) {
        var admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

        SamajHistoryEntry h = new SamajHistoryEntry();
        h.setTitle(body.title().trim());
        h.setType(body.type().trim());
        h.setDate(body.date());
        h.setTime(body.time() == null ? null : body.time().trim());
        h.setLocation(body.location().trim());
        h.setDescription(body.description());
        h.setImageUrl(body.imageUrl() == null ? null : body.imageUrl().trim());
        h.setCreatedBy(admin);
        h.setCreatedAt(Instant.now());
        h.setUpdatedAt(Instant.now());

        samajHistoryRepository.save(h);
        return toDto(h);
    }

    @Transactional(readOnly = true)
    public HistoryDtos.PageResponse<HistoryDtos.HistoryResponse> adminList(
            int page,
            int size,
            String type,
            LocalDate fromDate,
            LocalDate toDate,
            String q
    ) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        String tn = type == null || type.isBlank() ? null : type.trim();
        String qn = q == null || q.isBlank() ? null : q.trim();

        Page<SamajHistoryEntry> pg = samajHistoryRepository.pageForAdmin(tn, fromDate, toDate, qn, PageRequest.of(p, s));
        return new HistoryDtos.PageResponse<>(
                pg.stream().map(this::toDto).toList(),
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    @Transactional(readOnly = true)
    public HistoryDtos.HistoryResponse adminGet(long id) {
        SamajHistoryEntry h = samajHistoryRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "History not found"));
        return toDto(h);
    }

    public HistoryDtos.HistoryResponse adminUpdate(long id, HistoryDtos.HistoryUpdateRequest body) {
        SamajHistoryEntry h = samajHistoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "History not found"));

        h.setTitle(body.title().trim());
        h.setType(body.type().trim());
        h.setDate(body.date());
        h.setTime(body.time() == null ? null : body.time().trim());
        h.setLocation(body.location().trim());
        h.setDescription(body.description());
        h.setImageUrl(body.imageUrl() == null ? null : body.imageUrl().trim());
        h.setUpdatedAt(Instant.now());

        return toDto(h);
    }

    public void adminDelete(long id) {
        if (!samajHistoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "History not found");
        }
        samajHistoryRepository.deleteById(id);
    }

    private HistoryDtos.HistoryResponse toDto(SamajHistoryEntry h) {
        return new HistoryDtos.HistoryResponse(
                h.getId() == null ? 0 : h.getId(),
                h.getTitle(),
                h.getType(),
                h.getDate() == null ? null : DateTimeFormatter.ISO_LOCAL_DATE.format(h.getDate()),
                h.getTime(),
                h.getLocation(),
                h.getDescription(),
                h.getImageUrl(),
                h.getCreatedBy() == null ? null : h.getCreatedBy().getId().toString(),
                iso(h.getCreatedAt()),
                iso(h.getUpdatedAt())
        );
    }

    private static String iso(Instant i) {
        return i == null ? null : DateTimeFormatter.ISO_INSTANT.format(i);
    }
}

