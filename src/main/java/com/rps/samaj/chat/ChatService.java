package com.rps.samaj.chat;

import com.rps.samaj.api.dto.ChatDtos;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final ChatConversationRepository conversationRepo;
    private final ChatParticipantRepository participantRepo;
    private final ChatMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final ChatPushService chatPushService;

    public ChatService(ChatConversationRepository conversationRepo,
                       ChatParticipantRepository participantRepo,
                       ChatMessageRepository messageRepo,
                       UserRepository userRepo,
                       UserProfileRepository profileRepo,
                       ChatPushService chatPushService) {
        this.conversationRepo = conversationRepo;
        this.participantRepo = participantRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.chatPushService = chatPushService;
    }

    // ── Conversations ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ChatDtos.ConversationResponse> listConversations(UUID userId) {
        List<ChatConversation> convs = conversationRepo.findAllByParticipant(userId);
        return convs.stream().map(c -> toConversationResponse(c, userId)).toList();
    }

    @Transactional
    public ChatDtos.ConversationResponse openDirect(UUID userId, ChatDtos.CreateDirectRequest req) {
        UUID otherUserId = parseUuid(req.otherUserId(), "otherUserId");
        if (otherUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot chat with yourself");
        }
        var existing = conversationRepo.findDirectBetween(userId, otherUserId);
        if (existing.isPresent()) {
            return toConversationResponse(existing.get(), userId);
        }
        User me = userRepo.getReferenceById(userId);
        User other = userRepo.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        ChatConversation conv = ChatConversation.createDirect(me);
        conversationRepo.save(conv);
        participantRepo.save(new ChatParticipant(conv, me, "MEMBER"));
        participantRepo.save(new ChatParticipant(conv, other, "MEMBER"));
        return toConversationResponse(conv, userId);
    }

    @Transactional
    public ChatDtos.ConversationResponse createGroup(UUID userId, ChatDtos.CreateGroupRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name required");
        }
        User creator = userRepo.getReferenceById(userId);
        ChatConversation conv = ChatConversation.createGroup(creator, req.name().trim(), req.avatarUrl());
        conversationRepo.save(conv);
        participantRepo.save(new ChatParticipant(conv, creator, "ADMIN"));
        if (req.memberUserIds() != null) {
            for (String uid : req.memberUserIds()) {
                UUID memberId = parseUuid(uid, "memberUserId");
                if (memberId.equals(userId)) continue;
                userRepo.findById(memberId).ifPresent(u ->
                        participantRepo.save(new ChatParticipant(conv, u, "MEMBER")));
            }
        }
        return toConversationResponse(conv, userId);
    }

    @Transactional
    public ChatDtos.ConversationResponse updateGroup(UUID userId, UUID conversationId, ChatDtos.UpdateGroupRequest req) {
        ChatConversation conv = getConversationOrThrow(conversationId);
        requireParticipant(conv.getId(), userId);
        if (!"GROUP".equals(conv.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a group conversation");
        }
        if (req.name() != null && !req.name().isBlank()) conv.setName(req.name().trim());
        if (req.avatarUrl() != null) conv.setAvatarUrl(req.avatarUrl());
        conv.setUpdatedAt(Instant.now());
        return toConversationResponse(conv, userId);
    }

    @Transactional
    public void addMembers(UUID userId, UUID conversationId, ChatDtos.AddMembersRequest req) {
        ChatConversation conv = getConversationOrThrow(conversationId);
        requireParticipant(conv.getId(), userId);
        if (!"GROUP".equals(conv.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a group conversation");
        }
        for (String uid : req.userIds()) {
            UUID memberId = parseUuid(uid, "userId");
            if (!participantRepo.existsByConversation_IdAndUser_Id(conversationId, memberId)) {
                userRepo.findById(memberId).ifPresent(u ->
                        participantRepo.save(new ChatParticipant(conv, u, "MEMBER")));
            }
        }
    }

    @Transactional
    public void leaveGroup(UUID userId, UUID conversationId) {
        ChatConversation conv = getConversationOrThrow(conversationId);
        if (!"GROUP".equals(conv.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot leave a direct chat");
        }
        participantRepo.findByConversation_IdAndUser_Id(conversationId, userId)
                .ifPresent(participantRepo::delete);
    }

    @Transactional
    public void muteConversation(UUID userId, UUID conversationId, boolean muted) {
        ChatParticipant p = requireParticipant(conversationId, userId);
        p.setMuted(muted);
    }

    // ── Messages ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ChatDtos.PaginatedMessages listMessages(UUID userId, UUID conversationId, int page, int size) {
        requireParticipant(conversationId, userId);
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        Page<ChatMessage> pg = messageRepo.findByConversation_IdAndDeletedFalseOrderByCreatedAtDesc(conversationId, PageRequest.of(p, s));
        List<ChatDtos.MessageResponse> content = pg.getContent().stream().map(this::toMessageResponse).toList();
        return new ChatDtos.PaginatedMessages(content, pg.getTotalElements(), pg.getTotalPages(), pg.getNumber(), pg.getSize());
    }

    @Transactional
    public ChatDtos.MessageResponse sendMessage(UUID senderId, UUID conversationId, ChatDtos.SendMessageRequest req) {
        ChatConversation conv = getConversationOrThrow(conversationId);
        requireParticipant(conversationId, senderId);
        User sender = userRepo.getReferenceById(senderId);

        String msgType = req.type() != null ? req.type().trim().toUpperCase() : "TEXT";
        ChatMessage replyTo = null;
        if (req.replyToId() != null && !req.replyToId().isBlank()) {
            replyTo = messageRepo.findById(parseUuid(req.replyToId(), "replyToId")).orElse(null);
        }

        ChatMessage msg;
        if ("TEXT".equals(msgType)) {
            if (req.content() == null || req.content().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content required for text message");
            }
            msg = ChatMessage.text(conv, sender, req.content().trim(), replyTo);
        } else {
            if (req.fileUrl() == null || req.fileUrl().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileUrl required for file message");
            }
            msg = ChatMessage.file(conv, sender,
                    req.content() != null ? req.content().trim() : null,
                    req.fileUrl(), req.fileName(), req.fileSize(), req.fileType(),
                    msgType, replyTo);
        }

        messageRepo.save(msg);

        String preview = "TEXT".equals(msgType)
                ? truncate(msg.getContent(), 80)
                : ("\uD83D\uDCCE " + (msg.getFileName() != null ? msg.getFileName() : msgType));
        conv.setLastMessageAt(msg.getCreatedAt());
        conv.setLastMessagePreview(preview);
        conv.setUpdatedAt(msg.getCreatedAt());

        ChatDtos.MessageResponse response = toMessageResponse(msg);
        chatPushService.broadcastNewMessage(conversationId, response);
        return response;
    }

    @Transactional
    public void deleteMessage(UUID userId, UUID messageId) {
        ChatMessage msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!msg.getSender().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only sender can delete");
        }
        msg.setDeleted(true);
    }

    @Transactional
    public void markRead(UUID userId, UUID conversationId) {
        ChatParticipant p = requireParticipant(conversationId, userId);
        Instant now = Instant.now();
        p.setLastReadAt(now);
        chatPushService.broadcastReadReceipt(conversationId, userId, now);
    }

    // ── Participant list (for WS fan-out) ──────────────────────────

    @Transactional(readOnly = true)
    public List<UUID> participantUserIds(UUID conversationId) {
        return participantRepo.findByConversation_Id(conversationId).stream()
                .map(p -> p.getUser().getId()).toList();
    }

    // ── Mapping helpers ────────────────────────────────────────────

    private ChatDtos.ConversationResponse toConversationResponse(ChatConversation c, UUID viewerId) {
        List<ChatParticipant> parts = participantRepo.findByConversation_Id(c.getId());
        List<ChatDtos.ParticipantSummary> pSummaries = parts.stream().map(p -> {
            var profile = profileRepo.findByUser_Id(p.getUser().getId()).orElse(null);
            String displayName = profile != null ? profile.getFullName() : "User";
            String avatar = profile != null ? profile.getAvatarUrl() : null;
            String lastRead = p.getLastReadAt() != null ? p.getLastReadAt().toString() : null;
            return new ChatDtos.ParticipantSummary(
                    p.getUser().getId().toString(), displayName, avatar, p.getRole(), lastRead);
        }).toList();

        long unread = 0;
        var viewerPart = parts.stream().filter(p -> p.getUser().getId().equals(viewerId)).findFirst().orElse(null);
        if (viewerPart != null) {
            Instant since = viewerPart.getLastReadAt() != null ? viewerPart.getLastReadAt() : viewerPart.getJoinedAt();
            unread = participantRepo.countUnreadMessages(c.getId(), since, viewerId);
        }

        String name = c.getName();
        String avatar = c.getAvatarUrl();
        if ("DIRECT".equals(c.getType())) {
            var other = pSummaries.stream()
                    .filter(ps -> !ps.userId().equals(viewerId.toString()))
                    .findFirst().orElse(null);
            if (other != null) {
                name = other.displayName();
                avatar = other.avatarUrl();
            }
        }

        return new ChatDtos.ConversationResponse(
                c.getId().toString(),
                c.getType(),
                name,
                avatar,
                c.getLastMessagePreview(),
                c.getLastMessageAt() != null ? c.getLastMessageAt().toString() : null,
                unread,
                pSummaries
        );
    }

    private ChatDtos.MessageResponse toMessageResponse(ChatMessage m) {
        var profile = profileRepo.findByUser_Id(m.getSender().getId()).orElse(null);
        String displayName = profile != null ? profile.getFullName() : "User";
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

        ChatDtos.ReplySnippet replySnippet = null;
        if (m.getReplyTo() != null) {
            var rp = profileRepo.findByUser_Id(m.getReplyTo().getSender().getId()).orElse(null);
            replySnippet = new ChatDtos.ReplySnippet(
                    m.getReplyTo().getId().toString(),
                    m.getReplyTo().getSender().getId().toString(),
                    rp != null ? rp.getFullName() : "User",
                    truncate(m.getReplyTo().getContent(), 100),
                    m.getReplyTo().getType()
            );
        }

        return new ChatDtos.MessageResponse(
                m.getId().toString(),
                m.getConversation().getId().toString(),
                m.getSender().getId().toString(),
                displayName,
                avatarUrl,
                m.isDeleted() ? null : m.getContent(),
                m.getType(),
                m.isDeleted() ? null : m.getFileUrl(),
                m.isDeleted() ? null : m.getFileName(),
                m.isDeleted() ? null : m.getFileSize(),
                m.isDeleted() ? null : m.getFileType(),
                replySnippet,
                m.isDeleted(),
                m.getCreatedAt().toString()
        );
    }

    private ChatConversation getConversationOrThrow(UUID id) {
        return conversationRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
    }

    private ChatParticipant requireParticipant(UUID conversationId, UUID userId) {
        return participantRepo.findByConversation_IdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant"));
    }

    private static UUID parseUuid(String s, String field) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + field);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
