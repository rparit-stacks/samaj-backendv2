package com.rps.samaj.config.app;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigEntryRepository extends JpaRepository<AppConfigEntry, String> {
}
