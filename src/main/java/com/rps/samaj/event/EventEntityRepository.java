package com.rps.samaj.event;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventEntityRepository extends JpaRepository<EventEntity, Long> {

    @EntityGraph(attributePaths = "organizer")
    @Query("""
            select e from EventEntity e
            where (:organizerId is null or e.organizer.id = :organizerId)
            and (:type is null or :type = '' or lower(e.type) = lower(:type))
            """)
    List<EventEntity> findFiltered(@Param("organizerId") UUID organizerId, @Param("type") String type);

    @EntityGraph(attributePaths = "organizer")
    @Query("select e from EventEntity e where e.id = :id")
    Optional<EventEntity> findDetailedById(@Param("id") Long id);
}
