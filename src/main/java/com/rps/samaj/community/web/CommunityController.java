package com.rps.samaj.community.web;

import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommentCreateRequest;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityAnalytics;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityCommentDto;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityPostCreateRequest;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityPostDto;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityPostPatchRequest;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.CommunityTagWithCount;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.PageResponse;
import com.rps.samaj.api.dto.CommunityAndFeedDtos.ReportRequest;
import com.rps.samaj.community.CommunityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/posts")
    public PageResponse<CommunityPostDto> listPosts(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) Boolean savedOnly
    ) {
        UUID uid = principalOrNull(auth);
        return communityService.listPosts(page, size, tag, authorId, savedOnly, uid);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityPostDto create(Authentication auth, @Valid @RequestBody CommunityPostCreateRequest body) {
        return communityService.createPost(requireUser(auth), body);
    }

    @PutMapping("/posts/{id}")
    public CommunityPostDto update(
            Authentication auth,
            @PathVariable long id,
            @RequestBody CommunityPostPatchRequest body
    ) {
        return communityService.updatePost(requireUser(auth), id, body);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable long id) {
        communityService.deletePost(requireUser(auth), id);
    }

    @PostMapping("/posts/{id}/like")
    public CommunityPostDto toggleLike(Authentication auth, @PathVariable long id) {
        return communityService.toggleLike(requireUser(auth), id);
    }

    @PostMapping("/posts/{id}/save")
    public CommunityPostDto toggleSave(Authentication auth, @PathVariable long id) {
        return communityService.toggleSave(requireUser(auth), id);
    }

    @PostMapping("/posts/{id}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackView(@PathVariable long id) {
        communityService.trackView(id);
    }

    @PostMapping("/posts/{id}/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trackShare(@PathVariable long id) {
        communityService.trackShare(id);
    }

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommunityCommentDto addComment(
            Authentication auth,
            @PathVariable long postId,
            @Valid @RequestBody CommentCreateRequest body
    ) {
        return communityService.addComment(requireUser(auth), postId, body);
    }

    @GetMapping("/posts/{postId}/comments")
    public PageResponse<CommunityCommentDto> listComments(
            @PathVariable long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return communityService.listComments(postId, page, size);
    }

    @PostMapping("/posts/{id}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(Authentication auth, @PathVariable long id, @RequestBody(required = false) ReportRequest body) {
        communityService.createReport(requireUser(auth), id, body);
    }

    @GetMapping("/tags")
    public List<CommunityTagWithCount> topTags(@RequestParam(defaultValue = "20") int limit) {
        return communityService.topTags(limit);
    }

    @GetMapping("/me/analytics")
    public CommunityAnalytics myAnalytics(Authentication auth) {
        return communityService.analytics(requireUser(auth));
    }

    private static UUID requireUser(Authentication auth) {
        UUID u = principalOrNull(auth);
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return u;
    }

    private static UUID principalOrNull(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            return null;
        }
        return u;
    }
}
