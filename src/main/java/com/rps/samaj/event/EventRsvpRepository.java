package com.rps.samaj.event;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRsvpRepository extends JpaRepository<EventRsvp, UUID> {

    Optional<EventRsvp> findByEvent_IdAndUser_Id(Long eventId, UUID userId);

    @EntityGraph(attributePaths = "user")
    List<EventRsvp> findByEvent_Id(Long eventId);

    @Query("select r.event.id, r.status, count(r) from EventRsvp r where r.event.id in :ids group by r.event.id, r.status")
    List<Object[]> countGroupedByEventAndStatus(@Param("ids") Collection<Long> eventIds);

    @EntityGraph(attributePaths = "event")
    @Query("select r from EventRsvp r where r.user.id = :uid and r.event.id in :eids")
    List<EventRsvp> findByUser_IdAndEvent_IdIn(@Param("uid") UUID userId, @Param("eids") Collection<Long> eventIds);
}
