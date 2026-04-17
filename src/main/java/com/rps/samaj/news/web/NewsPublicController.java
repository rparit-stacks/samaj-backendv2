package com.rps.samaj.news.web;

import com.rps.samaj.api.dto.NewsDtos;
import com.rps.samaj.news.NewsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Authenticated users only (see {@code SecurityConfig}): list + read active news.
 */
@RestController
@RequestMapping("/api/v1/news")
public class NewsPublicController {

    private final NewsService newsService;

    public NewsPublicController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/categories")
    public List<NewsDtos.NewsCategoryResponse> categories() {
        return newsService.userListCategories();
    }

    @GetMapping("/articles")
    public NewsDtos.PageResponse<NewsDtos.NewsArticleResponse> articles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId
    ) {
        return newsService.userListArticles(page, size, categoryId);
    }

    @GetMapping("/articles/{id}")
    public NewsDtos.NewsArticleResponse article(@PathVariable long id) {
        return newsService.userGetArticle(id);
    }
}
