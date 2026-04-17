package com.rps.samaj.achiever;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AchievementFieldTemplateRepository extends JpaRepository<AchievementFieldTemplate, UUID> {
    List<AchievementFieldTemplate> findByActiveOrderByNameAsc(boolean active);
}
