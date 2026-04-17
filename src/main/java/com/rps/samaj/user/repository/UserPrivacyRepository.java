package com.rps.samaj.user.repository;

import com.rps.samaj.user.model.UserPrivacy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserPrivacyRepository extends JpaRepository<UserPrivacy, UUID> {
}
