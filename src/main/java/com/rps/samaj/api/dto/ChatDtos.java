package com.rps.samaj.api.dto;

import java.util.List;

public final class ChatDtos {
    private ChatDtos() {}

    // --- Conversation ---
    public record ConversationResponse(
            String id,
            String type,
            String name,
            String avatarUrl,
            String lastMessagePreview,
            String lastMessageAt,
            long unreadCount,
            List<ParticipantSummary> participants
    ) {}

    public record ParticipantSummary(
            String userId,
            String displayName,
            String avatarUrl,
            String role,
            /** ISO-8601 instant when this participant last opened the thread; used for read receipts */
            String lastReadAt
    ) {}

    public record CreateDirectRequest(String otherUserId) {}

    public record CreateGroupRequest(
            String name,
            String avatarUrl,
            List<String> memberUserIds
    ) {}

    public record UpdateGroupRequest(String name, String avatarUrl) {}

    // --- Messages ---
    public record MessageResponse(
            String id,
            String conversationId,
            String senderId,
            String senderDisplayName,
            String senderAvatarUrl,
            String content,
            String type,
            String fileUrl,
            String fileName,
            Long fileSize,
            String fileType,
            ReplySnippet replyTo,
            boolean deleted,
            String createdAt
    ) {}

    public record ReplySnippet(
            String id,
            String senderId,
            String senderDisplayName,
            String content,
            String type
    ) {}

    public record SendMessageRequest(
            String content,
            String type,
            String fileUrl,
            String fileName,
            Long fileSize,
            String fileType,
            String replyToId
    ) {}

    public record PaginatedMessages(
            List<MessageResponse> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {}

    // --- WebSocket events ---
    public record WsNewMessage(String event, MessageResponse message) {}
    public record WsTyping(String event, String conversationId, String userId, String displayName) {}
    public record WsReadReceipt(String event, String conversationId, String userId, String readAt) {}
    public record WsOnlineStatus(String event, String userId, boolean online) {}

    // --- Misc ---
    public record MuteRequest(boolean muted) {}
    public record AddMembersRequest(List<String> userIds) {}
}
