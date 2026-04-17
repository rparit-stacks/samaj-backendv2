package com.rps.samaj.chat;

import com.rps.samaj.user.model.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_chat_messages")
public class ChatMessage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private ChatConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(columnDefinition = "text")
    private String content;

    @Column(nullable = false, length = 16)
    private String type; // TEXT, IMAGE, FILE, SYSTEM

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_type")
    private String fileType; // mime type

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private ChatMessage replyTo;

    private boolean deleted;

    @Column(name = "created_at")
    private Instant createdAt;

    protected ChatMessage() {}

    public static ChatMessage text(ChatConversation conv, User sender, String content, ChatMessage replyTo) {
        ChatMessage m = new ChatMessage();
        m.id = UUID.randomUUID();
        m.conversation = conv;
        m.sender = sender;
        m.content = content;
        m.type = "TEXT";
        m.replyTo = replyTo;
        m.createdAt = Instant.now();
        return m;
    }

    public static ChatMessage file(ChatConversation conv, User sender, String content,
                                    String fileUrl, String fileName, Long fileSize, String fileType,
                                    String msgType, ChatMessage replyTo) {
        ChatMessage m = new ChatMessage();
        m.id = UUID.randomUUID();
        m.conversation = conv;
        m.sender = sender;
        m.content = content;
        m.type = msgType;
        m.fileUrl = fileUrl;
        m.fileName = fileName;
        m.fileSize = fileSize;
        m.fileType = fileType;
        m.replyTo = replyTo;
        m.createdAt = Instant.now();
        return m;
    }

    public UUID getId() { return id; }
    public ChatConversation getConversation() { return conversation; }
    public User getSender() { return sender; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public String getFileUrl() { return fileUrl; }
    public String getFileName() { return fileName; }
    public Long getFileSize() { return fileSize; }
    public String getFileType() { return fileType; }
    public ChatMessage getReplyTo() { return replyTo; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Instant getCreatedAt() { return createdAt; }
}
