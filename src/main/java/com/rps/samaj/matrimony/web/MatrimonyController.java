package com.rps.samaj.matrimony.web;

import com.rps.samaj.api.dto.MatrimonyDtos;
import com.rps.samaj.matrimony.MatrimonyService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matrimony")
public class MatrimonyController {

    private final MatrimonyService matrimonyService;

    public MatrimonyController(MatrimonyService matrimonyService) {
        this.matrimonyService = matrimonyService;
    }

    @GetMapping("/me/summary")
    public MatrimonyDtos.MatrimonyMeSummary meSummary(Authentication auth) {
        return matrimonyService.meSummary(requireUserId(auth));
    }

    @GetMapping("/me/dashboard")
    public MatrimonyDtos.MatrimonyDashboard dashboard(Authentication auth) {
        return matrimonyService.dashboard(requireUserId(auth));
    }

    @PostMapping("/profiles/{profileId}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordView(Authentication auth, @PathVariable UUID profileId) {
        matrimonyService.recordProfileView(requireUserId(auth), profileId);
    }

    @PostMapping("/profiles/{profileId}/favorite")
    public MatrimonyDtos.MatrimonyFavoriteToggleResponse toggleFavorite(Authentication auth, @PathVariable UUID profileId) {
        return matrimonyService.toggleFavorite(requireUserId(auth), profileId);
    }

    @GetMapping("/favorites")
    public List<MatrimonyDtos.MatrimonyProfileCard> favorites(Authentication auth) {
        return matrimonyService.listFavorites(requireUserId(auth));
    }

    @GetMapping("/blocks")
    public List<String> blocks(Authentication auth) {
        return matrimonyService.listBlockedUserIds(requireUserId(auth));
    }

    @PostMapping("/blocks/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(Authentication auth, @PathVariable UUID userId) {
        matrimonyService.blockUser(requireUserId(auth), userId);
    }

    @DeleteMapping("/blocks/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(Authentication auth, @PathVariable UUID userId) {
        matrimonyService.unblockUser(requireUserId(auth), userId);
    }

    @GetMapping("/profiles")
    public MatrimonyDtos.PaginatedMatrimonyProfiles search(
            Authentication auth,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String profession,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return matrimonyService.searchProfiles(
                requireUserId(auth),
                gender,
                city,
                minAge,
                maxAge,
                profession,
                q,
                page,
                size
        );
    }

    @GetMapping("/profiles/{id}")
    public MatrimonyDtos.MatrimonyProfileDetail getProfile(Authentication auth, @PathVariable UUID id) {
        return matrimonyService.getProfile(requireUserId(auth), id);
    }

    @PostMapping("/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    public MatrimonyDtos.MatrimonyProfileDetail create(
            Authentication auth,
            @Valid @RequestBody MatrimonyDtos.MatrimonyCreateProfileRequest body
    ) {
        return matrimonyService.createProfile(requireUserId(auth), body);
    }

    @PutMapping("/profiles/{id}")
    public MatrimonyDtos.MatrimonyProfileDetail update(
            Authentication auth,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body
    ) {
        return matrimonyService.updateProfile(requireUserId(auth), id, body);
    }

    @PostMapping("/profiles/{id}/activate")
    public MatrimonyDtos.MatrimonyProfileDetail activate(Authentication auth, @PathVariable UUID id) {
        return matrimonyService.activateProfile(requireUserId(auth), id);
    }

    @DeleteMapping("/profiles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(Authentication auth, @PathVariable UUID id) {
        matrimonyService.archiveProfile(requireUserId(auth), id);
    }

    @PostMapping("/interests")
    @ResponseStatus(HttpStatus.CREATED)
    public MatrimonyDtos.MatrimonyInterestResponse sendInterest(
            Authentication auth,
            @Valid @RequestBody MatrimonyDtos.MatrimonySendInterestRequest body
    ) {
        return matrimonyService.sendInterest(requireUserId(auth), body);
    }

    @GetMapping("/interests/sent")
    public MatrimonyDtos.PaginatedMatrimonyInterests sent(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return matrimonyService.listInterestsSent(requireUserId(auth), page, size);
    }

    @GetMapping("/interests/received")
    public MatrimonyDtos.PaginatedMatrimonyInterests received(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return matrimonyService.listInterestsReceived(requireUserId(auth), page, size);
    }

    @PutMapping("/interests/{id}/accept")
    public MatrimonyDtos.MatrimonyInterestResponse accept(Authentication auth, @PathVariable UUID id) {
        return matrimonyService.acceptInterest(requireUserId(auth), id);
    }

    @PutMapping("/interests/{id}/reject")
    public MatrimonyDtos.MatrimonyInterestResponse reject(Authentication auth, @PathVariable UUID id) {
        return matrimonyService.rejectInterest(requireUserId(auth), id);
    }

    @PutMapping("/interests/{id}/withdraw")
    public MatrimonyDtos.MatrimonyInterestResponse withdraw(Authentication auth, @PathVariable UUID id) {
        return matrimonyService.withdrawInterest(requireUserId(auth), id);
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public MatrimonyDtos.MatrimonyConversationResponse openConversation(
            Authentication auth,
            @Valid @RequestBody MatrimonyDtos.MatrimonyOpenConversationRequest body
    ) {
        return matrimonyService.openConversation(requireUserId(auth), body);
    }

    @GetMapping("/conversations")
    public List<MatrimonyDtos.MatrimonyConversationResponse> listConversations(Authentication auth) {
        return matrimonyService.listConversations(requireUserId(auth));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public MatrimonyDtos.PaginatedMatrimonyMessages listMessages(
            Authentication auth,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return matrimonyService.listMessages(requireUserId(auth), conversationId, page, size);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MatrimonyDtos.MatrimonyChatMessageResponse sendMessage(
            Authentication auth,
            @PathVariable UUID conversationId,
            @Valid @RequestBody MatrimonyDtos.MatrimonySendMessageRequest body
    ) {
        return matrimonyService.sendMessage(requireUserId(auth), conversationId, body);
    }

    private static UUID requireUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID uid)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return uid;
    }
}
