package com.rps.samaj.community;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rps.samaj.api.dto.AdminCommunityDtos;
import com.rps.samaj.api.dto.CommunityAndFeedDtos;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommentCreateRequest;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityAnalytics;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityCommentDto;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityPostCreateRequest;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityPostDto;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityPostMediaDto;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityPostMediaIn;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityPostPatchRequest;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityTagWithCount;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.PageResponse;
import com.rps.samaj.notification.PublicNotificationPublisher;
import com.rps.samaj.config.cache.RedisCacheConfig;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommunityService {

    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostSaveRepository postSaveRepository;
    private final PostTagLinkRepository postTagLinkRepository;
    private final CommunityTagRepository tagRepository;
    private final CommunityPostReportRepository reportRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;
    private final PublicNotificationPublisher notificationPublisher;

    public CommunityService(
            CommunityPostRepository postRepository,
            CommunityCommentRepository commentRepository,
            PostLikeRepository postLikeRepository,
            PostSaveRepository postSaveRepository,
            PostTagLinkRepository postTagLinkRepository,
            CommunityTagRepository tagRepository,
            CommunityPostReportRepository reportRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            ObjectMapper objectMapper,
            PublicNotificationPublisher notificationPublisher
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.postSaveRepository = postSaveRepository;
        this.postTagLinkRepository = postTagLinkRepository;
        this.tagRepository = tagRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.objectMapper = objectMapper;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = RedisCacheConfig.Names.COMMUNITY_POSTS,
            key = "T(String).valueOf(#page).concat(':').concat(T(String).valueOf(#size)).concat(':')\n+                    .concat(#tag == null ? 'all' : #tag).concat(':')\n+                    .concat(#authorId == null ? 'all' : #authorId.toString()).concat(':')\n+                    .concat(#savedOnly == null ? '0' : T(String).valueOf(#savedOnly)).concat(':')\n+                    .concat(#currentUserId == null ? 'anon' : #currentUserId.toString())"
    )
    public PageResponse<CommunityPostDto> listPosts(
            int page,
            int size,
            String tag,
            UUID authorId,
            Boolean savedOnly,
            UUID currentUserId
    ) {
        if (Boolean.TRUE.equals(savedOnly) && currentUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required for saved posts");
        }
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(p, s);
        Page<CommunityPost> pg;
        if (Boolean.TRUE.equals(savedOnly)) {
            pg = postRepository.findSavedByUser(currentUserId, pageable);
        } else if (authorId != null) {
            pg = postRepository.findByAuthorId(authorId, pageable);
        } else if (tag != null && !tag.isBlank()) {
            pg = postRepository.findByTagSlug(tag.trim(), pageable);
        } else {
            pg = postRepository.findAllPaged(pageable);
        }
        return enrichPage(pg, currentUserId);
    }

    @CacheEvict(cacheNames = RedisCacheConfig.Names.COMMUNITY_POSTS, allEntries = true)
    public CommunityPostDto createPost(UUID authorId, CommunityPostCreateRequest body) {
        var author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        CommunityPost post = new CommunityPost();
        post.setAuthor(author);
        post.setContent(body.content().trim());
        post.setLocation(body.location());
        post.setEmojiCodesJson(writeJsonList(body.emojiCodes()));
        post.setMentionedUserIdsJson(writeJsonList(body.mentionedUserIds()));
        Instant now = Instant.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        applyMedia(post, body.media());
        post = postRepository.saveAndFlush(post);
        replaceTagLinks(post, body.tags());
        notificationPublisher.onCommunityPostCreated(post.getId(), authorId, post.getContent());
        return loadPostDto(post.getId(), authorId);
    }

    public CommunityPostDto updatePost(UUID userId, long postId, CommunityPostPatchRequest body) {
        CommunityPost post = postRepository.findDetailedById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (!post.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your post");
        }
        if (body.content() != null) {
            post.setContent(body.content().trim());
        }
        if (body.location() != null) {
            post.setLocation(body.location());
        }
        if (body.emojiCodes() != null) {
            post.setEmojiCodesJson(writeJsonList(body.emojiCodes()));
        }
        if (body.mentionedUserIds() != null) {
            post.setMentionedUserIdsJson(writeJsonList(body.mentionedUserIds()));
        }
        if (body.media() != null) {
            applyMedia(post, body.media());
        }
        if (body.tags() != null) {
            replaceTagLinks(post, body.tags());
        }
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
        return loadPostDto(postId, userId);
    }

    public void deletePost(UUID userId, long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (!post.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your post");
        }
        commentRepository.deleteByPost_Id(postId);
        postLikeRepository.deleteByPost_Id(postId);
        postSaveRepository.deleteByPost_Id(postId);
        postTagLinkRepository.deleteByPost_Id(postId);
        postRepository.delete(post);
    }

    public CommunityPostDto toggleLike(UUID userId, long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        var existing = postLikeRepository.findByPost_IdAndUser_Id(postId, userId);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        } else {
            postLikeRepository.save(new PostLike(UUID.randomUUID(), post, userRepository.getReferenceById(userId)));
            post.setLikeCount(post.getLikeCount() + 1);
        }
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
        return loadPostDto(postId, userId);
    }

    public CommunityPostDto toggleSave(UUID userId, long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        var existing = postSaveRepository.findByPost_IdAndUser_Id(postId, userId);
        if (existing.isPresent()) {
            postSaveRepository.delete(existing.get());
            post.setSaveCount(Math.max(0, post.getSaveCount() - 1));
        } else {
            postSaveRepository.save(new PostSave(UUID.randomUUID(), post, userRepository.getReferenceById(userId)));
            post.setSaveCount(post.getSaveCount() + 1);
        }
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
        return loadPostDto(postId, userId);
    }

    public void trackView(long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        post.setViewCount(post.getViewCount() + 1);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
    }

    public void trackShare(long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        post.setShareCount(post.getShareCount() + 1);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
    }

    public CommunityCommentDto addComment(UUID userId, long postId, CommentCreateRequest body) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        CommunityComment c = new CommunityComment();
        c.setPost(post);
        c.setAuthor(userRepository.getReferenceById(userId));
        c.setContent(body.content().trim());
        c.setCreatedAt(Instant.now());
        commentRepository.save(c);
        post.setCommentCount(post.getCommentCount() + 1);
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
        return new CommunityCommentDto(c.getId(), postId, userId.toString(), c.getContent(), iso(c.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunityCommentDto> listComments(long postId, int page, int size) {
        if (!postRepository.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 200);
        Page<CommunityComment> pg = commentRepository.findByPost_IdOrderByCreatedAtAsc(postId, PageRequest.of(p, s));
        List<CommunityCommentDto> content = pg.stream()
                .map(c -> new CommunityCommentDto(
                        c.getId(),
                        postId,
                        c.getAuthor().getId().toString(),
                        c.getContent(),
                        iso(c.getCreatedAt())
                ))
                .toList();
        return new PageResponse<>(content, pg.getTotalElements(), pg.getTotalPages(), pg.getNumber(), pg.getSize());
    }

    @Transactional(readOnly = true)
    public List<CommunityTagWithCount> topTags(int limit) {
        int lim = Math.min(Math.max(limit, 1), 100);
        List<Object[]> rows = tagRepository.topTagsRaw(lim);
        List<CommunityTagWithCount> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            long tid = ((Number) r[0]).longValue();
            String name = (String) r[1];
            String slug = (String) r[2];
            long cnt = ((Number) r[3]).longValue();
            out.add(new CommunityTagWithCount(tid, name, slug, cnt));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public CommunityAnalytics analytics(UUID userId) {
        long posts = postRepository.countByAuthor_Id(userId);
        long likesGiven = postLikeRepository.countByUser_Id(userId);
        long likesReceived = postRepository.sumLikeCountByAuthor(userId);
        long saves = postRepository.sumSaveCountByAuthor(userId);
        long views = postRepository.sumViewCountByAuthor(userId);
        return new CommunityAnalytics(posts, likesGiven, likesReceived, saves, views);
    }

    // ============================================================
    // Admin APIs (moderation / management)
    // ============================================================

    @Transactional(readOnly = true)
    public PageResponse<CommunityPostDto> adminListPosts(
            int page,
            int size,
            UUID authorId,
            String tag,
            String q,
            UUID currentAdminUserId
    ) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(p, s);
        String tn = tag == null || tag.isBlank() ? null : tag.trim();
        String qn = q == null || q.isBlank() ? null : q.trim();
        Page<CommunityPost> pg = postRepository.pageForAdmin(authorId, tn, qn, pageable);
        return enrichPage(pg, currentAdminUserId);
    }

    @Transactional(readOnly = true)
    public CommunityPostDto adminGetPost(long id, UUID currentAdminUserId) {
        return loadPostDto(id, currentAdminUserId);
    }

    public CommunityPostDto adminUpdatePost(long postId, CommunityPostPatchRequest body, UUID currentAdminUserId) {
        CommunityPost post = postRepository.findDetailedById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (body.content() != null) {
            post.setContent(body.content().trim());
        }
        if (body.location() != null) {
            post.setLocation(body.location());
        }
        if (body.emojiCodes() != null) {
            post.setEmojiCodesJson(writeJsonList(body.emojiCodes()));
        }
        if (body.mentionedUserIds() != null) {
            post.setMentionedUserIdsJson(writeJsonList(body.mentionedUserIds()));
        }
        if (body.media() != null) {
            applyMedia(post, body.media());
        }
        if (body.tags() != null) {
            replaceTagLinks(post, body.tags());
        }
        post.setUpdatedAt(Instant.now());
        postRepository.save(post);
        return loadPostDto(postId, currentAdminUserId);
    }

    public void adminDeletePost(long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        commentRepository.deleteByPost_Id(postId);
        postLikeRepository.deleteByPost_Id(postId);
        postSaveRepository.deleteByPost_Id(postId);
        postTagLinkRepository.deleteByPost_Id(postId);
        reportRepository.deleteByPost_Id(postId);
        postRepository.delete(post);
    }

    public void adminDeleteComment(long commentId) {
        CommunityComment c = commentRepository.findDetailedById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        long postId = c.getPost().getId();
        commentRepository.delete(c);
        postRepository.findById(postId).ifPresent(p -> {
            p.setCommentCount(Math.max(0, p.getCommentCount() - 1));
            p.setUpdatedAt(Instant.now());
            postRepository.save(p);
        });
    }

    public AdminCommunityDtos.TagResponse adminCreateTag(AdminCommunityDtos.TagCreateRequest body) {
        String label = body.name().trim();
        CommunityTag t = findOrCreateTag(label);
        return new AdminCommunityDtos.TagResponse(t.getId(), t.getName(), t.getSlug());
    }

    public void adminDeleteTag(long id) {
        if (!tagRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found");
        }
        tagRepository.deleteById(id);
    }

    public void createReport(UUID reporterId, long postId, CommunityAndFeedDtos.ReportRequest body) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        var reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        CommunityPostReport r = new CommunityPostReport();
        r.setPost(post);
        r.setReporter(reporter);
        if (body != null) {
            r.setReason(body.reason());
            r.setDetails(body.details());
        }
        r.setStatus(CommunityReportStatus.OPEN);
        r.setCreatedAt(Instant.now());
        reportRepository.save(r);
    }

    @Transactional(readOnly = true)
    public AdminCommunityDtos.ReportPageResponse adminListReports(int page, int size, String status) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        CommunityReportStatus st = null;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                st = CommunityReportStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
            }
        }
        Page<CommunityPostReport> pg = reportRepository.pageAdmin(st, PageRequest.of(p, s));
        List<AdminCommunityDtos.ReportResponse> rows = pg.stream().map(this::toReportDto).toList();
        return new AdminCommunityDtos.ReportPageResponse(
                rows,
                pg.getTotalElements(),
                pg.getTotalPages(),
                pg.getNumber(),
                pg.getSize()
        );
    }

    public AdminCommunityDtos.ReportResponse adminReviewReport(long reportId, UUID adminUserId, AdminCommunityDtos.ReportReviewRequest body) {
        CommunityPostReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        CommunityReportStatus st;
        try {
            st = CommunityReportStatus.valueOf(body.status().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        if (st == CommunityReportStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot review to OPEN");
        }
        r.setStatus(st);
        r.setReviewedAt(Instant.now());
        r.setReviewedBy(userRepository.getReferenceById(adminUserId));
        reportRepository.save(r);
        return toReportDto(r);
    }

    private AdminCommunityDtos.ReportResponse toReportDto(CommunityPostReport r) {
        return new AdminCommunityDtos.ReportResponse(
                r.getId(),
                r.getPost().getId(),
                r.getPost().getAuthor().getId().toString(),
                r.getReporter().getId().toString(),
                r.getReason(),
                r.getDetails(),
                r.getStatus().name(),
                iso(r.getCreatedAt()),
                iso(r.getReviewedAt()),
                r.getReviewedBy() == null ? null : r.getReviewedBy().getId().toString()
        );
    }

    private PageResponse<CommunityPostDto> enrichPage(Page<CommunityPost> pg, UUID currentUserId) {
        List<CommunityPost> list = pg.getContent();
        if (list.isEmpty()) {
            return new PageResponse<>(List.of(), pg.getTotalElements(), pg.getTotalPages(), pg.getNumber(), pg.getSize());
        }
        Set<UUID> authorIds = list.stream().map(p -> p.getAuthor().getId()).collect(Collectors.toSet());
        Map<UUID, UserProfile> profiles = userProfileRepository.findByIdIn(authorIds).stream()
                .collect(Collectors.toMap(UserProfile::getId, x -> x));
        List<Long> postIds = list.stream().map(CommunityPost::getId).toList();
        Map<Long, List<CommunityAndFeedDtos.CommunityPostTagDto>> tagsByPost = buildTagsMap(postIds);
        Set<Long> liked = new HashSet<>();
        Set<Long> saved = new HashSet<>();
        if (currentUserId != null) {
            liked.addAll(postLikeRepository.findLikedPostIds(currentUserId, postIds));
            saved.addAll(postSaveRepository.findSavedPostIds(currentUserId, postIds));
        }
        List<CommunityPostDto> content = new ArrayList<>(list.size());
        for (CommunityPost p : list) {
            UserProfile prof = profiles.get(p.getAuthor().getId());
            content.add(toDto(
                    p,
                    prof,
                    tagsByPost.getOrDefault(p.getId(), List.of()),
                    liked.contains(p.getId()),
                    saved.contains(p.getId())
            ));
        }
        return new PageResponse<>(content, pg.getTotalElements(), pg.getTotalPages(), pg.getNumber(), pg.getSize());
    }

    private CommunityPostDto loadPostDto(long postId, UUID currentUserId) {
        CommunityPost p = postRepository.findDetailedById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        UserProfile prof = userProfileRepository.findById(p.getAuthor().getId()).orElse(null);
        Map<Long, List<CommunityAndFeedDtos.CommunityPostTagDto>> tagsMap = buildTagsMap(List.of(postId));
        boolean liked = currentUserId != null && postLikeRepository.existsByPost_IdAndUser_Id(postId, currentUserId);
        boolean isSaved = currentUserId != null && postSaveRepository.existsByPost_IdAndUser_Id(postId, currentUserId);
        return toDto(p, prof, tagsMap.getOrDefault(postId, List.of()), liked, isSaved);
    }

    private Map<Long, List<CommunityAndFeedDtos.CommunityPostTagDto>> buildTagsMap(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        List<PostTagLink> links = postTagLinkRepository.findByPost_IdIn(postIds);
        Map<Long, List<CommunityAndFeedDtos.CommunityPostTagDto>> map = new HashMap<>();
        for (PostTagLink l : links) {
            CommunityTag t = l.getTag();
            long pid = l.getPost().getId();
            map.computeIfAbsent(pid, k -> new ArrayList<>())
                    .add(new CommunityAndFeedDtos.CommunityPostTagDto(t.getId(), t.getName(), t.getSlug()));
        }
        return map;
    }

    private CommunityPostDto toDto(
            CommunityPost p,
            UserProfile profile,
            List<CommunityAndFeedDtos.CommunityPostTagDto> tags,
            boolean liked,
            boolean isSaved
    ) {
        UUID aid = p.getAuthor().getId();
        String display;
        if (profile != null && profile.getFullName() != null && !profile.getFullName().isBlank()) {
            display = profile.getFullName();
        } else if (profile != null) {
            display = profile.getProfileKey();
        } else {
            display = "Member";
        }
        String photo = profile != null ? profile.getAvatarUrl() : null;
        List<CommunityPostMediaDto> media = p.getMedia().stream()
                .sorted(Comparator.comparingInt(CommunityPostMedia::getSortOrder))
                .map(m -> new CommunityPostMediaDto(m.getId(), m.getUrl(), m.getType(), m.getSortOrder()))
                .toList();
        return new CommunityPostDto(
                p.getId(),
                aid.toString(),
                display,
                photo,
                p.getContent(),
                p.getLocation(),
                readJsonList(p.getEmojiCodesJson()),
                readJsonList(p.getMentionedUserIdsJson()),
                tags,
                media,
                p.getLikeCount(),
                p.getCommentCount(),
                p.getSaveCount(),
                p.getShareCount(),
                p.getViewCount(),
                iso(p.getCreatedAt()),
                iso(p.getUpdatedAt()),
                liked,
                isSaved
        );
    }

    private void replaceTagLinks(CommunityPost post, List<String> tagLabels) {
        Long pid = post.getId();
        if (pid != null) {
            postTagLinkRepository.deleteByPost_Id(pid);
        }
        if (tagLabels == null || tagLabels.isEmpty()) {
            return;
        }
        for (String raw : tagLabels) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            CommunityTag tag = findOrCreateTag(raw.trim());
            postTagLinkRepository.save(new PostTagLink(UUID.randomUUID(), post, tag));
        }
    }

    private CommunityTag findOrCreateTag(String label) {
        String slug = slugify(label);
        return tagRepository.findBySlugIgnoreCase(slug)
                .orElseGet(() -> tagRepository.save(new CommunityTag(label, slug)));
    }

    private static String slugify(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s.isEmpty() ? "tag" : s;
    }

    private void applyMedia(CommunityPost post, List<CommunityPostMediaIn> items) {
        post.getMedia().clear();
        if (items == null || items.isEmpty()) {
            return;
        }
        int i = 0;
        for (CommunityPostMediaIn m : items) {
            CommunityPostMedia pm = new CommunityPostMedia();
            pm.setPost(post);
            pm.setUrl(m.url().trim());
            pm.setType(normalizeMediaType(m.type()));
            pm.setSortOrder(m.sortOrder() != null ? m.sortOrder() : i);
            post.getMedia().add(pm);
            i++;
        }
    }

    private static String normalizeMediaType(String type) {
        if (type == null) {
            return "IMAGE";
        }
        String u = type.trim().toUpperCase(Locale.ROOT);
        return "VIDEO".equals(u) ? "VIDEO" : "IMAGE";
    }

    private List<String> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeJsonList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list == null ? List.of() : list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String iso(Instant i) {
        return i == null ? null : DateTimeFormatter.ISO_INSTANT.format(i);
    }
}
