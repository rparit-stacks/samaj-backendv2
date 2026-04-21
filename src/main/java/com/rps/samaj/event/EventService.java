package com.rps.samaj.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.EventDtos;
import com.rps.samaj.config.cache.RedisCacheConfig;
import com.rps.samaj.notification.PublicNotificationPublisher;
import com.rps.samaj.security.JwtAuthenticationFilter;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {

    private static final Set<String> RSVP = Set.of("going", "interested", "not_going");

    private final EventEntityRepository eventRepository;
    private final EventRsvpRepository rsvpRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final ObjectMapper objectMapper;
    private final PublicNotificationPublisher notificationPublisher;

    public EventService(
            EventEntityRepository eventRepository,
            EventRsvpRepository rsvpRepository,
            UserRepository userRepository,
            UserProfileRepository profileRepository,
            ObjectMapper objectMapper,
            PublicNotificationPublisher notificationPublisher
    ) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = RedisCacheConfig.Names.EVENTS_LIST,
            key = "T(com.rps.samaj.security.JwtAuthenticationFilter).currentUserIdOrNull().toString().concat(':')\n+                    .concat(#organizerId == null ? 'all' : #organizerId.toString()).concat(':')\n+                    .concat(#type == null ? 'all' : #type).concat(':')\n+                    .concat(#sort == null ? 'list' : #sort)"
    )
    public List<EventDtos.EventItemResponse> list(UUID organizerId, String type, String sort) {
        requireUser();
        UUID currentUserId = requireUserId();
        String t = type == null || type.isBlank() ? null : type.trim();
        List<EventEntity> rows = eventRepository.findFiltered(organizerId, t);
        sortEvents(rows, sort == null ? "list" : sort);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(EventEntity::getId).toList();
        Map<Long, CountHolder> counts = loadCounts(ids);
        Map<Long, String> myRsvp = loadMyRsvp(currentUserId, ids);
        Map<UUID, UserProfile> profiles = loadProfiles(rows);
        return rows.stream()
                .map(e -> toItem(e, counts.getOrDefault(e.getId(), emptyCounts()), myRsvp.get(e.getId()), profiles))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventDtos.EventItemResponse> listForAdmin(UUID organizerId, String type, String sort) {
        String t = type == null || type.isBlank() ? null : type.trim();
        List<EventEntity> rows = eventRepository.findFiltered(organizerId, t);
        sortEvents(rows, sort == null ? "list" : sort);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(EventEntity::getId).toList();
        Map<Long, CountHolder> counts = loadCounts(ids);
        Map<UUID, UserProfile> profiles = loadProfiles(rows);
        return rows.stream()
                .map(e -> toItem(e, counts.getOrDefault(e.getId(), emptyCounts()), null, profiles))
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.EVENT_DETAIL,
            key = "T(com.rps.samaj.security.JwtAuthenticationFilter).currentUserIdOrNull().toString().concat(':').concat(T(String).valueOf(#id))")
    public EventDtos.EventDetailResponse getDetail(long id) {
        requireUser();
        UUID uid = requireUserId();
        EventEntity e = eventRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        CountHolder c = loadCounts(List.of(id)).getOrDefault(id, emptyCounts());
        String my = rsvpRepository.findByEvent_IdAndUser_Id(id, uid).map(EventRsvp::getStatus).orElse(null);
        Map<UUID, UserProfile> profiles = loadProfiles(List.of(e));
        List<EventRsvp> rsvps = rsvpRepository.findByEvent_Id(id);
        List<EventDtos.EventAttendeeResponse> goingAvatars = rsvps.stream()
                .filter(r -> "going".equals(r.getStatus()))
                .limit(24)
                .map(r -> toAttendee(r))
                .toList();
        boolean organizer = e.getOrganizer().getId().equals(uid);
        return toDetail(e, c, my, profiles, goingAvatars, organizer);
    }

    @Transactional(readOnly = true)
    public EventDtos.EventDetailResponse getDetailForAdmin(long id) {
        EventEntity e = eventRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        CountHolder c = loadCounts(List.of(id)).getOrDefault(id, emptyCounts());
        Map<UUID, UserProfile> profiles = loadProfiles(List.of(e));
        List<EventRsvp> rsvps = rsvpRepository.findByEvent_Id(id);
        List<EventDtos.EventAttendeeResponse> goingAvatars = rsvps.stream()
                .filter(r -> "going".equals(r.getStatus()))
                .limit(24)
                .map(this::toAttendee)
                .toList();
        return toDetail(e, c, null, profiles, goingAvatars, false);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.EVENTS_LIST,
            RedisCacheConfig.Names.EVENT_DETAIL,
            RedisCacheConfig.Names.EVENT_ANALYTICS
    }, allEntries = true)
    public EventDtos.EventItemResponse create(EventDtos.EventCreateRequest body) {
        UUID organizerId = requireUserId();
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        Instant now = Instant.now();
        EventEntity e = new EventEntity();
        e.setOrganizer(organizer);
        e.setTitle(body.title().trim());
        e.setType(body.type().trim().toLowerCase());
        e.setDate(body.date());
        e.setTime(trimToNull(body.time()));
        e.setLocation(body.location().trim());
        e.setDescription(trimToNull(body.description()));
        e.setImageUrl(trimToNull(body.imageUrl()));
        e.setScheduleJson(writeSchedule(body.schedule()));
        e.setCreatedAt(now);
        eventRepository.saveAndFlush(e);
        notificationPublisher.onEventCreated(e.getId(), organizerId, e.getTitle());
        Map<UUID, UserProfile> profiles = loadProfiles(List.of(e));
        return toItem(e, emptyCounts(), null, profiles);
    }

    @Transactional
    @CacheEvict(cacheNames = {
            RedisCacheConfig.Names.EVENTS_LIST,
            RedisCacheConfig.Names.EVENT_DETAIL,
            RedisCacheConfig.Names.EVENT_ANALYTICS
    }, allEntries = true)
    public EventDtos.EventItemResponse rsvp(long eventId, EventDtos.EventRsvpRequest body) {
        UUID userId = requireUserId();
        EventEntity event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        String st = normalizeRsvp(body.status());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (event.getOrganizer().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organizer cannot RSVP to own event");
        }
        EventRsvp r = rsvpRepository.findByEvent_IdAndUser_Id(eventId, userId).orElse(null);
        if (r == null) {
            r = new EventRsvp(UUID.randomUUID(), event, user, st);
        } else {
            r.setStatus(st);
        }
        r.setDisplayName(trimToNull(body.displayName()));
        r.setPhotoUrl(trimToNull(body.photoUrl()));
        rsvpRepository.save(r);
        CountHolder c = loadCounts(List.of(eventId)).getOrDefault(eventId, emptyCounts());
        Map<UUID, UserProfile> profiles = loadProfiles(List.of(event));
        return toItem(event, c, st, profiles);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.EVENT_ANALYTICS,
            key = "T(com.rps.samaj.security.JwtAuthenticationFilter).currentUserIdOrNull().toString().concat(':').concat(T(String).valueOf(#eventId))")
    public EventDtos.EventAnalyticsResponse analyticsForOrganizer(long eventId) {
        UUID uid = requireUserId();
        EventEntity e = eventRepository.findDetailedById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (!e.getOrganizer().getId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can view analytics");
        }
        return buildAnalytics(eventId);
    }

    @Transactional(readOnly = true)
    public EventDtos.EventAnalyticsResponse analyticsForAdmin(long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        return buildAnalytics(eventId);
    }

    private EventDtos.EventAnalyticsResponse buildAnalytics(long eventId) {
        List<EventRsvp> rsvps = rsvpRepository.findByEvent_Id(eventId);
        List<EventDtos.EventAttendeeResponse> going = new ArrayList<>();
        List<EventDtos.EventAttendeeResponse> interested = new ArrayList<>();
        List<EventDtos.EventAttendeeResponse> notGoing = new ArrayList<>();
        for (EventRsvp r : rsvps) {
            EventDtos.EventAttendeeResponse a = toAttendee(r);
            switch (r.getStatus()) {
                case "going" -> going.add(a);
                case "interested" -> interested.add(a);
                case "not_going" -> notGoing.add(a);
                default -> {
                }
            }
        }
        return new EventDtos.EventAnalyticsResponse(
                eventId,
                going.size(),
                interested.size(),
                notGoing.size(),
                going,
                interested,
                notGoing
        );
    }

    private EventDtos.EventAttendeeResponse toAttendee(EventRsvp r) {
        User u = r.getUser();
        String name = r.getDisplayName();
        if (name == null || name.isBlank()) {
            name = null;
        }
        return new EventDtos.EventAttendeeResponse(
                u.getId().toString(),
                name,
                r.getPhotoUrl(),
                r.getStatus(),
                null,
                null
        );
    }

    private static void sortEvents(List<EventEntity> rows, String sort) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Comparator<EventEntity> byDateRole = (a, b) -> {
            boolean aUp = !a.getDate().isBefore(today);
            boolean bUp = !b.getDate().isBefore(today);
            if (aUp != bUp) {
                return aUp ? -1 : 1;
            }
            if (aUp) {
                return a.getDate().compareTo(b.getDate());
            }
            return b.getDate().compareTo(a.getDate());
        };
        if ("topic".equalsIgnoreCase(sort)) {
            rows.sort(Comparator.comparing(EventEntity::getType, String.CASE_INSENSITIVE_ORDER).thenComparing(byDateRole));
        } else {
            rows.sort(byDateRole.thenComparing(EventEntity::getId));
        }
    }

    private Map<Long, CountHolder> loadCounts(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, CountHolder> map = new HashMap<>();
        for (Long id : eventIds) {
            map.put(id, new CountHolder());
        }
        for (Object[] row : rsvpRepository.countGroupedByEventAndStatus(eventIds)) {
            Long eid = (Long) row[0];
            String status = (String) row[1];
            long n = ((Number) row[2]).longValue();
            CountHolder sc = map.computeIfAbsent(eid, x -> new CountHolder());
            switch (status) {
                case "going" -> sc.going += n;
                case "interested" -> sc.interested += n;
                case "not_going" -> sc.notGoing += n;
                default -> {
                }
            }
        }
        return map;
    }

    private Map<Long, String> loadMyRsvp(UUID userId, List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> map = new HashMap<>();
        for (EventRsvp r : rsvpRepository.findByUser_IdAndEvent_IdIn(userId, eventIds)) {
            map.put(r.getEvent().getId(), r.getStatus());
        }
        return map;
    }

    private Map<UUID, UserProfile> loadProfiles(List<EventEntity> events) {
        Set<UUID> ids = events.stream().map(e -> e.getOrganizer().getId()).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UserProfile> m = new HashMap<>();
        for (UserProfile p : profileRepository.findByIdIn(ids)) {
            m.put(p.getId(), p);
        }
        return m;
    }

    private EventDtos.EventItemResponse toItem(
            EventEntity e,
            CountHolder c,
            String myRsvp,
            Map<UUID, UserProfile> profiles
    ) {
        return new EventDtos.EventItemResponse(
                e.getId(),
                e.getTitle(),
                e.getType(),
                e.getDate().toString(),
                e.getTime(),
                e.getLocation(),
                e.getDescription(),
                e.getImageUrl(),
                organizerOf(e, profiles),
                e.getScheduleJson(),
                c.going,
                c.interested,
                c.notGoing,
                myRsvp,
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null
        );
    }

    private EventDtos.EventDetailResponse toDetail(
            EventEntity e,
            CountHolder c,
            String myRsvp,
            Map<UUID, UserProfile> profiles,
            List<EventDtos.EventAttendeeResponse> goingAvatars,
            boolean isOrganizer
    ) {
        return new EventDtos.EventDetailResponse(
                e.getId(),
                e.getTitle(),
                e.getType(),
                e.getDate().toString(),
                e.getTime(),
                e.getLocation(),
                e.getDescription(),
                e.getImageUrl(),
                organizerOf(e, profiles),
                e.getScheduleJson(),
                parseSchedule(e.getScheduleJson()),
                c.going,
                c.interested,
                c.notGoing,
                myRsvp,
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null,
                goingAvatars,
                isOrganizer
        );
    }

    private EventDtos.EventOrganizerResponse organizerOf(EventEntity e, Map<UUID, UserProfile> profiles) {
        UUID oid = e.getOrganizer().getId();
        UserProfile p = profiles.get(oid);
        String name = p != null && p.getFullName() != null && !p.getFullName().isBlank()
                ? p.getFullName()
                : null;
        String photo = p != null ? p.getAvatarUrl() : null;
        return new EventDtos.EventOrganizerResponse(oid.toString(), name, photo);
    }

    private List<EventDtos.EventScheduleItem> parseSchedule(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String writeSchedule(List<EventDtos.EventScheduleItem> schedule) {
        if (schedule == null || schedule.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(schedule);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid schedule");
        }
    }

    private static String normalizeRsvp(String raw) {
        String s = raw == null ? "" : raw.trim().toLowerCase().replace('-', '_');
        if (!RSVP.contains(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid RSVP status");
        }
        return s;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static void requireUser() {
        if (JwtAuthenticationFilter.currentUserIdOrNull() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private static UUID requireUserId() {
        UUID id = JwtAuthenticationFilter.currentUserIdOrNull();
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return id;
    }

    private static CountHolder emptyCounts() {
        return new CountHolder();
    }

    private static final class CountHolder {
        long going;
        long interested;
        long notGoing;
    }
}
