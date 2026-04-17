package com.rps.samaj.news;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NewsCategoryRepository extends JpaRepository<NewsCategory, Long> {

    Optional<NewsCategory> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

    @Query("""
            SELECT DISTINCT c FROM NewsCategory c
            WHERE EXISTS (SELECT 1 FROM NewsArticle a WHERE a.category = c AND a.active = true)
            ORDER BY c.name ASC
            """)
    List<NewsCategory> findAllWithActiveArticles();
}
