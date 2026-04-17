package com.rps.samaj.cms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CmsMobileBannerRepository extends JpaRepository<CmsMobileBanner, UUID> {
    List<CmsMobileBanner> findByActiveOrderByDisplayOrder(boolean active);
}
