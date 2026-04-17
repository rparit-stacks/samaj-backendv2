package com.rps.samaj.chat;

import com.rps.samaj.api.dto.ChatDtos;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ChatPushService {

    private final ChatParticipantRepository participantRepo;
    private final SimpMessagingTemplate messaging;

    public ChatPushService(ChatParticipantRepository participantRepo, SimpMessagingTemplate messaging) {
        this.participantRepo = participantRepo;
        this.messaging = messaging;
    }

    public void broadcastNewMessage(UUID conversationId, ChatDtos.MessageResponse msg) {
        ChatDtos.WsNewMessage event = new ChatDtos.WsNewMessage("NEW_MESSAGE", msg);
        for (UUID uid : participantUserIds(conversationId)) {
            messaging.convertAndSendToUser(uid.toString(), "/queue/chat", event);
        }
    }

    public void broadcastReadReceipt(UUID conversationId, UUID readerId, Instant readAt) {
        ChatDtos.WsReadReceipt event = new ChatDtos.WsReadReceipt(
                "READ_RECEIPT",
                conversationId.toString(),
                readerId.toString(),
                readAt.toString());
        for (UUID uid : participantUserIds(conversationId)) {
            if (!uid.equals(readerId)) {
                messaging.convertAndSendToUser(uid.toString(), "/queue/chat", event);
            }
        }
    }

    private List<UUID> participantUserIds(UUID conversationId) {
        return participantRepo.findByConversation_Id(conversationId).stream()
                .map(p -> p.getUser().getId())
                .toList();
    }
}
