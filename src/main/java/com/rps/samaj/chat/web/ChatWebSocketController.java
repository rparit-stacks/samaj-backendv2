package com.rps.samaj.chat.web;

import com.rps.samaj.api.dto.ChatDtos;
import com.rps.samaj.chat.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messaging;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messaging) {
        this.chatService = chatService;
        this.messaging = messaging;
    }

    /**
     * Client sends: /app/chat/{conversationId}/send
     * Fans out the saved message to all participants via /user/{userId}/queue/chat
     */
    @MessageMapping("/chat/{conversationId}/send")
    public void handleSend(@DestinationVariable String conversationId,
                            @Payload ChatDtos.SendMessageRequest req,
                            Principal principal) {
        UUID senderId = extractUserId(principal);
        UUID convId = UUID.fromString(conversationId);
        chatService.sendMessage(senderId, convId, req);
        /* NEW_MESSAGE is broadcast from ChatService.sendMessage */
    }

    /**
     * Client sends: /app/chat/{conversationId}/typing
     * Fans out typing indicator to other participants.
     */
    @MessageMapping("/chat/{conversationId}/typing")
    public void handleTyping(@DestinationVariable String conversationId, Principal principal) {
        UUID senderId = extractUserId(principal);
        UUID convId = UUID.fromString(conversationId);
        List<UUID> participants = chatService.participantUserIds(convId);
        ChatDtos.WsTyping event = new ChatDtos.WsTyping("TYPING", conversationId, senderId.toString(), null);
        for (UUID uid : participants) {
            if (!uid.equals(senderId)) {
                messaging.convertAndSendToUser(uid.toString(), "/queue/chat", event);
            }
        }
    }

    /**
     * Client sends: /app/chat/{conversationId}/read
     * Marks read and notifies others.
     */
    @MessageMapping("/chat/{conversationId}/read")
    public void handleRead(@DestinationVariable String conversationId, Principal principal) {
        UUID userId = extractUserId(principal);
        UUID convId = UUID.fromString(conversationId);
        chatService.markRead(userId, convId);
        /* READ_RECEIPT is broadcast from ChatService.markRead */
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth) {
            return (UUID) auth.getPrincipal();
        }
        throw new IllegalStateException("Not authenticated");
    }
}
