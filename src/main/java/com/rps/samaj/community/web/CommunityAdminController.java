package com.rps.samaj.community.web;

import com.rps.samaj.api.dto.AdminCommunityDtos;
import com.rps.samaj.api.dto.CommunityAndFeedDtos;
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

import java.util.UUID;

@RestController
@RequestMapping("/admin/community")
public class CommunityAdminController {

    private final CommunityService communityService;

    public CommunityAdminController(CommunityService communityService) {
        this.communityService = communityService;
    }

    // Posts

    @GetMapping("/posts")
    public CommunityAndFeedDtos.PageResponse<CommunityAndFeedDtos.CommunityPostDto> listPosts(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID authorId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String q
    ) {
        return communityService.adminListPosts(page, size, authorId, tag, q, requireAdminUser(auth));
    }

    @GetMapping("/posts/{id}")
    public CommunityAndFeedDtos.CommunityPostDto getPost(Authentication auth, @PathVariable long id) {
        return communityService.adminGetPost(id, requireAdminUser(auth));
    }

    @PutMapping("/posts/{id}")
    public CommunityAndFeedDtos.CommunityPostDto updatePost(
            Authentication auth,
            @PathVariable long id,
            @RequestBody CommunityAndFeedDtos.CommunityPostPatchRequest body
    ) {
        return communityService.adminUpdatePost(id, body, requireAdminUser(auth));
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable long id) {
        communityService.adminDeletePost(id);
    }

    // Comments

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable long id) {
        communityService.adminDeleteComment(id);
    }

    // Tags

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCommunityDtos.TagResponse createTag(@Valid @RequestBody AdminCommunityDtos.TagCreateRequest body) {
        return communityService.adminCreateTag(body);
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable long id) {
        communityService.adminDeleteTag(id);
    }

    // Reports

    @GetMapping("/reports")
    public AdminCommunityDtos.ReportPageResponse listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        return communityService.adminListReports(page, size, status);
    }

    @PutMapping("/reports/{id}/review")
    public AdminCommunityDtos.ReportResponse reviewReport(
            Authentication auth,
            @PathVariable long id,
            @Valid @RequestBody AdminCommunityDtos.ReportReviewRequest body
    ) {
        return communityService.adminReviewReport(id, requireAdminUser(auth), body);
    }

    private static UUID requireAdminUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return u;
    }
}

