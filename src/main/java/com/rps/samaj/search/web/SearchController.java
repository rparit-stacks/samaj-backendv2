package com.rps.samaj.search.web;

import com.rps.samaj.api.dto.SearchDtos.SearchAllResponse;
import com.rps.samaj.api.dto.SearchDtos.SearchCategoryResponse;
import com.rps.samaj.search.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchAllResponse searchAll(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return searchService.searchAll(q, page, size);
    }

    @GetMapping("/{service}")
    public SearchCategoryResponse searchByService(
            @PathVariable String service,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return searchService.searchByService(service, q, page, size);
    }
}
