package com.rps.samaj.cloud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoredObjectRepository extends JpaRepository<StoredObject, UUID> {

    Optional<StoredObject> findByPublicUrlAndUser_Id(String publicUrl, UUID userId);
}
