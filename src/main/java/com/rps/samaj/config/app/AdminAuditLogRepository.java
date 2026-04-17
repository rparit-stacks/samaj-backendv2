package com.rps.samaj.config.app;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {
    Page<AdminAuditLog> findByResourceOrderByCreatedAtDesc(String resource, Pageable pageable);
    Page<AdminAuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);
    Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
