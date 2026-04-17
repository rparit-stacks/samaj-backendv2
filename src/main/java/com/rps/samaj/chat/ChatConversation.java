package com.rps.samaj.chat;

import com.rps.samaj.user.model.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_chat_conversations")
public class ChatConversation {

    @Id
    private UUID id;

    @Column(nullable = false, length = 16)
    private String type; // DIRECT, GROUP

    private String name;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_message_preview")
    private String lastMessagePreview;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ChatConversation() {}

    public static ChatConversation createDirect(User creator) {
        ChatConversation c = new ChatConversation();
        c.id = UUID.randomUUID();
        c.type = "DIRECT";
        c.createdBy = creator;
        c.createdAt = Instant.now();
        c.updatedAt = c.createdAt;
        return c;
    }

    public static ChatConversation createGroup(User creator, String name, String avatarUrl) {
        ChatConversation c = new ChatConversation();
        c.id = UUID.randomUUID();
        c.type = "GROUP";
        c.name = name;
        c.avatarUrl = avatarUrl;
        c.createdBy = creator;
        c.createdAt = Instant.now();
        c.updatedAt = c.createdAt;
        return c;
    }

    public UUID getId() { return id; }
    public String getType() { return type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public User getCreatedBy() { return createdBy; }
    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
