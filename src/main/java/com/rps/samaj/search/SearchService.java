package com.rps.samaj.search;

import com.rps.samaj.api.dto.SearchDtos.SearchAllResponse;
import com.rps.samaj.api.dto.SearchDtos.SearchCategoryResponse;
import com.rps.samaj.api.dto.SearchDtos.SearchResultDto;
import com.rps.samaj.exam.Exam;
import com.rps.samaj.exam.ExamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final ExamRepository examRepository;

    public SearchService(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public SearchAllResponse searchAll(String q, int page, int size) {
        String qn = q == null ? "" : q.trim();
        List<SearchCategoryResponse> categories = new ArrayList<>();
        categories.add(searchExamsCategory(qn, page, size));
        return new SearchAllResponse(qn, categories);
    }

    public SearchCategoryResponse searchByService(String serviceRaw, String q, int page, int size) {
        String svc = serviceRaw == null ? "" : serviceRaw.trim().toUpperCase(Locale.ROOT);
        String qn = q == null ? "" : q.trim();
        return switch (svc) {
            case "EXAMS" -> searchExamsCategory(qn, page, size);
            default -> emptyCategory(svc);
        };
    }

    private SearchCategoryResponse searchExamsCategory(String q, int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 50);
        String qn = q.isBlank() ? null : q;
        Page<Exam> pg = examRepository.searchPublished(qn, null, null, PageRequest.of(p, s));
        List<SearchResultDto> results = new ArrayList<>();
        for (Exam e : pg.getContent()) {
            results.add(new SearchResultDto(
                    "EXAMS",
                    e.getId().toString(),
                    e.getTitle(),
                    e.getType() != null ? e.getType().toUpperCase(Locale.ROOT) : "",
                    truncate(e.getDescription(), 220),
                    null,
                    "/exams?examId=" + e.getId()
            ));
        }
        return new SearchCategoryResponse("EXAMS", pg.getTotalElements(), results);
    }

    private static SearchCategoryResponse emptyCategory(String service) {
        String s = service.isBlank() ? "UNKNOWN" : service;
        return new SearchCategoryResponse(s, 0, List.of());
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
