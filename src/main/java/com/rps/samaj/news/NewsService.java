package com.rps.samaj.news;

import com.rps.samaj.api.dto.NewsDtos;
import com.rps.samaj.config.cache.RedisCacheConfig;
import com.rps.samaj.notification.PublicNotificationPublisher;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Service
public class NewsService {

    private final NewsCategoryRepository categoryRepository;
    private final NewsArticleRepository articleRepository;
    private final PublicNotificationPublisher notificationPublisher;

    public NewsService(
            NewsCategoryRepository categoryRepository,
            NewsArticleRepository articleRepository,
            PublicNotificationPublisher notificationPublisher
    ) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
        this.notificationPublisher = notificationPublisher;
    }

    // ——— Admin: categories ———

    @Transactional(readOnly = true)
    public List<NewsDtos.NewsCategoryResponse> adminListCategories() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.NEWS_CATEGORIES,
            RedisCacheConfig.Names.NEWS_ARTICLES
    }, allEntries = true)
    public NewsDtos.NewsCategoryResponse adminCreateCategory(NewsDtos.NewsCategoryCreateRequest req) {
        String baseSlug = req.slug() != null && !req.slug().isBlank()
                ? slugify(req.slug())
                : slugify(req.name());
        String slug = uniqueCategorySlug(baseSlug);
        NewsCategory c = new NewsCategory(req.name().trim(), slug);
        c = categoryRepository.save(c);
        return toCategoryResponse(c);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.NEWS_CATEGORIES,
            RedisCacheConfig.Names.NEWS_ARTICLES
    }, allEntries = true)
    public NewsDtos.NewsCategoryResponse adminUpdateCategory(long id, NewsDtos.NewsCategoryUpdateRequest req) {
        NewsCategory c = categoryRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        String slug = slugify(req.slug());
        if (categoryRepository.existsBySlugIgnoreCaseAndIdNot(slug, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already in use");
        }
        c.setName(req.name().trim());
        c.setSlug(slug);
        return toCategoryResponse(categoryRepository.save(c));
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.NEWS_CATEGORIES,
            RedisCacheConfig.Names.NEWS_ARTICLES
    }, allEntries = true)
    public void adminDeleteCategory(long id) {
        NewsCategory c = categoryRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        long count = articleRepository.countByCategory_Id(id);
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category has articles; reassign or delete them first");
        }
        categoryRepository.delete(c);
    }

    // ——— Admin: articles ———

    @Transactional(readOnly = true)
    public NewsDtos.PageResponse<NewsDtos.NewsArticleAdminResponse> adminListArticles(
            int page,
            int size,
            Long categoryId,
            Boolean active
    ) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "pinned", "publishedAt"));
        Page<NewsArticle> pg;
        if (categoryId != null && active != null) {
            pg = articleRepository.findByCategory_IdAndActive(categoryId, active, p);
        } else if (categoryId != null) {
            pg = articleRepository.findByCategory_Id(categoryId, p);
        } else if (active != null) {
            pg = articleRepository.findByActive(active, p);
        } else {
            pg = articleRepository.findAll(p);
        }
        return toAdminArticlePage(pg);
    }

    @Transactional(readOnly = true)
    public NewsDtos.NewsArticleAdminResponse adminGetArticle(long id) {
        NewsArticle a = articleRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
        return toAdminArticleResponse(a);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.NEWS_CATEGORIES,
            RedisCacheConfig.Names.NEWS_ARTICLES
    }, allEntries = true)
    public NewsDtos.NewsArticleAdminResponse adminCreateArticle(NewsDtos.NewsArticleCreateRequest req) {
        NewsCategory cat = categoryRepository.findById(req.categoryId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category"));
        NewsArticle a = new NewsArticle();
        applyArticle(a, req.title(), req.summary(), req.content(), cat, req.imageUrl(), req.pinned(), req.active(), req.publishedAt());
        a = articleRepository.save(a);
        if (a.isActive()) {
            notificationPublisher.onNewsArticlePublished(a.getId(), a.getTitle());
        }
        return toAdminArticleResponse(a);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.NEWS_CATEGORIES,
            RedisCacheConfig.Names.NEWS_ARTICLES
    }, allEntries = true)
    public NewsDtos.NewsArticleAdminResponse adminUpdateArticle(long id, NewsDtos.NewsArticleUpdateRequest req) {
        NewsArticle a = articleRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
        boolean wasActive = a.isActive();
        NewsCategory cat = categoryRepository.findById(req.categoryId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category"));
        applyArticle(a, req.title(), req.summary(), req.content(), cat, req.imageUrl(), req.pinned(), req.active(), req.publishedAt());
        a = articleRepository.save(a);
        if (a.isActive() && !wasActive) {
            notificationPublisher.onNewsArticlePublished(a.getId(), a.getTitle());
        }
        return toAdminArticleResponse(a);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.NEWS_CATEGORIES,
            RedisCacheConfig.Names.NEWS_ARTICLES
    }, allEntries = true)
    public void adminDeleteArticle(long id) {
        if (!articleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        }
        articleRepository.deleteById(id);
    }

    // ——— User (authenticated): read-only ———

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.NEWS_CATEGORIES, key = "'v1'")
    public List<NewsDtos.NewsCategoryResponse> userListCategories() {
        return categoryRepository.findAllWithActiveArticles().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.NEWS_ARTICLES, key = "T(String).valueOf(#page).concat(':').concat(T(String).valueOf(#size)).concat(':').concat(#categoryId == null ? 'all' : T(String).valueOf(#categoryId))")
    public NewsDtos.PageResponse<NewsDtos.NewsArticleResponse> userListArticles(int page, int size, Long categoryId) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<NewsArticle> pg = categoryId == null
                ? articleRepository.findByActiveTrueOrderByPinnedDescPublishedAtDesc(p)
                : articleRepository.findByActiveTrueAndCategory_IdOrderByPinnedDescPublishedAtDesc(categoryId, p);
        return toUserArticlePage(pg);
    }

    @Transactional
    public NewsDtos.NewsArticleResponse userGetArticle(long id) {
        NewsArticle a = articleRepository.findByIdAndActiveTrue(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
        a.setViews(a.getViews() + 1);
        articleRepository.save(a);
        return toUserArticleResponse(a);
    }

    // ——— helpers ———

    private void applyArticle(
            NewsArticle a,
            String title,
            String summary,
            String content,
            NewsCategory cat,
            String imageUrl,
            boolean pinned,
            boolean active,
            Instant publishedAt
    ) {
        a.setTitle(title.trim());
        a.setSummary(summary);
        a.setContent(content);
        a.setCategory(cat);
        a.setImageUrl(imageUrl != null && !imageUrl.isBlank() ? imageUrl.trim() : null);
        a.setPinned(pinned);
        a.setActive(active);
        if (publishedAt != null) {
            a.setPublishedAt(publishedAt);
        } else if (active && a.getPublishedAt() == null) {
            a.setPublishedAt(Instant.now());
        }
    }

    private NewsDtos.NewsCategoryResponse toCategoryResponse(NewsCategory c) {
        return new NewsDtos.NewsCategoryResponse(c.getId(), c.getName(), c.getSlug());
    }

    private NewsDtos.NewsArticleResponse toUserArticleResponse(NewsArticle a) {
        NewsCategory c = a.getCategory();
        return new NewsDtos.NewsArticleResponse(
                a.getId(),
                a.getTitle(),
                a.getSummary(),
                a.getContent(),
                c.getId(),
                c.getName(),
                c.getSlug(),
                a.getImageUrl(),
                a.isPinned(),
                a.getPublishedAt(),
                a.getViews()
        );
    }

    private NewsDtos.NewsArticleAdminResponse toAdminArticleResponse(NewsArticle a) {
        NewsCategory c = a.getCategory();
        return new NewsDtos.NewsArticleAdminResponse(
                a.getId(),
                a.getTitle(),
                a.getSummary(),
                a.getContent(),
                c.getId(),
                c.getName(),
                c.getSlug(),
                a.getImageUrl(),
                a.isPinned(),
                a.isActive(),
                a.getPublishedAt(),
                a.getViews()
        );
    }

    private NewsDtos.PageResponse<NewsDtos.NewsArticleResponse> toUserArticlePage(Page<NewsArticle> pg) {
        return mapPage(pg, this::toUserArticleResponse);
    }

    private NewsDtos.PageResponse<NewsDtos.NewsArticleAdminResponse> toAdminArticlePage(Page<NewsArticle> pg) {
        return mapPage(pg, this::toAdminArticleResponse);
    }

    private static <T> NewsDtos.PageResponse<T> mapPage(Page<NewsArticle> pg, Function<NewsArticle, T> mapper) {
        List<T> content = pg.getContent().stream().map(mapper).toList();
        return new NewsDtos.PageResponse<>(
                content,
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    private String uniqueCategorySlug(String base) {
        String s = base.isEmpty() ? "category" : base;
        if (!isSlugTaken(s, null)) {
            return s;
        }
        int i = 2;
        while (isSlugTaken(s + "-" + i, null)) {
            i++;
        }
        return s + "-" + i;
    }

    /** True if another category already uses this slug. */
    private boolean isSlugTaken(String slug, Long excludeId) {
        return categoryRepository.findBySlugIgnoreCase(slug)
                .map(c -> excludeId == null || !c.getId().equals(excludeId))
                .orElse(false);
    }

    private static String slugify(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s.isEmpty() ? "category" : s;
    }
}
