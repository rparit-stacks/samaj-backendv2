package com.rps.samaj.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    Page<NewsArticle> findByActiveTrueOrderByPinnedDescPublishedAtDesc(Pageable pageable);

    Page<NewsArticle> findByActiveTrueAndCategory_IdOrderByPinnedDescPublishedAtDesc(Long categoryId, Pageable pageable);

    Optional<NewsArticle> findByIdAndActiveTrue(Long id);

    long countByCategory_Id(Long categoryId);

    Page<NewsArticle> findByCategory_Id(Long categoryId, Pageable pageable);

    Page<NewsArticle> findByActive(boolean active, Pageable pageable);

    Page<NewsArticle> findByCategory_IdAndActive(Long categoryId, boolean active, Pageable pageable);
}
