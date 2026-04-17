package com.rps.samaj.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntity, String> {
    @Query("SELECT o FROM OtpEntity o WHERE o.email = :email AND o.purpose = :purpose")
    Optional<OtpEntity> findByEmailAndPurpose(@Param("email") String email, @Param("purpose") String purpose);
}
