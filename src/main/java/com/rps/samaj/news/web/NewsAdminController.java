package com.rps.samaj.news.web;

import com.rps.samaj.api.dto.NewsDtos;
import com.rps.samaj.news.NewsService;
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

import java.util.List;

@RestController
@RequestMapping("/admin/news")
public class NewsAdminController {

    private final NewsService newsService;

    public NewsAdminController(NewsService newsService) {
        this.newsService = newsService;
    }

    // Categories

    @GetMapping("/categories")
    public List<NewsDtos.NewsCategoryResponse> listCategories() {
        return newsService.adminListCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public NewsDtos.NewsCategoryResponse createCategory(@Valid @RequestBody NewsDtos.NewsCategoryCreateRequest body) {
        return newsService.adminCreateCategory(body);
    }

    @PutMapping("/categories/{id}")
    public NewsDtos.NewsCategoryResponse updateCategory(
            @PathVariable long id,
            @Valid @RequestBody NewsDtos.NewsCategoryUpdateRequest body
    ) {
        return newsService.adminUpdateCategory(id, body);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable long id) {
        newsService.adminDeleteCategory(id);
    }

    // Articles

    @GetMapping("/articles")
    public NewsDtos.PageResponse<NewsDtos.NewsArticleAdminResponse> listArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active
    ) {
        return newsService.adminListArticles(page, size, categoryId, active);
    }

    @GetMapping("/articles/{id}")
    public NewsDtos.NewsArticleAdminResponse getArticle(@PathVariable long id) {
        return newsService.adminGetArticle(id);
    }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public NewsDtos.NewsArticleAdminResponse createArticle(@Valid @RequestBody NewsDtos.NewsArticleCreateRequest body) {
        return newsService.adminCreateArticle(body);
    }

    @PutMapping("/articles/{id}")
    public NewsDtos.NewsArticleAdminResponse updateArticle(
            @PathVariable long id,
            @Valid @RequestBody NewsDtos.NewsArticleUpdateRequest body
    ) {
        return newsService.adminUpdateArticle(id, body);
    }

    @DeleteMapping("/articles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable long id) {
        newsService.adminDeleteArticle(id);
    }
}
