package com.rps.samaj.chat.web;

import com.rps.samaj.api.dto.ChatDtos;
import com.rps.samaj.chat.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public List<ChatDtos.ConversationResponse> listConversations(Authentication auth) {
        return chatService.listConversations(uid(auth));
    }

    @PostMapping("/conversations/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatDtos.ConversationResponse openDirect(Authentication auth,
                                                     @Valid @RequestBody ChatDtos.CreateDirectRequest body) {
        return chatService.openDirect(uid(auth), body);
    }

    @PostMapping("/conversations/group")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatDtos.ConversationResponse createGroup(Authentication auth,
                                                      @Valid @RequestBody ChatDtos.CreateGroupRequest body) {
        return chatService.createGroup(uid(auth), body);
    }

    @PutMapping("/conversations/{id}")
    public ChatDtos.ConversationResponse updateGroup(Authentication auth,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody ChatDtos.UpdateGroupRequest body) {
        return chatService.updateGroup(uid(auth), id, body);
    }

    @PostMapping("/conversations/{id}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addMembers(Authentication auth, @PathVariable UUID id,
                           @Valid @RequestBody ChatDtos.AddMembersRequest body) {
        chatService.addMembers(uid(auth), id, body);
    }

    @DeleteMapping("/conversations/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveGroup(Authentication auth, @PathVariable UUID id) {
        chatService.leaveGroup(uid(auth), id);
    }

    @PutMapping("/conversations/{id}/mute")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mute(Authentication auth, @PathVariable UUID id,
                     @Valid @RequestBody ChatDtos.MuteRequest body) {
        chatService.muteConversation(uid(auth), id, body.muted());
    }

    @GetMapping("/conversations/{id}/messages")
    public ChatDtos.PaginatedMessages listMessages(Authentication auth,
                                                    @PathVariable UUID id,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
        return chatService.listMessages(uid(auth), id, page, size);
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatDtos.MessageResponse sendMessage(Authentication auth,
                                                 @PathVariable UUID id,
                                                 @Valid @RequestBody ChatDtos.SendMessageRequest body) {
        return chatService.sendMessage(uid(auth), id, body);
    }

    @PutMapping("/conversations/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(Authentication auth, @PathVariable UUID id) {
        chatService.markRead(uid(auth), id);
    }

    @DeleteMapping("/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(Authentication auth, @PathVariable UUID messageId) {
        chatService.deleteMessage(uid(auth), messageId);
    }

    private static UUID uid(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UUID u)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return u;
    }
}
