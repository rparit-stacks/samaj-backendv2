package com.rps.samaj.directory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DirectorySettingsRepository extends JpaRepository<DirectorySettings, UUID> {
}
