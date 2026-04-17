package com.rps.samaj.admin.system;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AdminServiceGrantRepository extends JpaRepository<AdminServiceGrant, Long> {

    List<AdminServiceGrant> findByUser_Id(UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AdminServiceGrant g where g.user.id = :uid")
    void deleteByUser_Id(@Param("uid") UUID userId);

    boolean existsByUser_IdAndServiceKey(UUID userId, AdminServiceKey serviceKey);

    @Query("select g.serviceKey from AdminServiceGrant g where g.user.id = :uid")
    List<AdminServiceKey> findServiceKeysByUser_Id(@Param("uid") UUID userId);
}
