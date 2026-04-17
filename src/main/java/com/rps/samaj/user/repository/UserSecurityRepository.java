package com.rps.samaj.user.repository;

import com.rps.samaj.user.model.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSecurityRepository extends JpaRepository<UserSecurity, UUID> {
}
