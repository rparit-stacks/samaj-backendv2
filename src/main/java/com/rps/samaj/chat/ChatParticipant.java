package com.rps.samaj.chat;

import com.rps.samaj.user.model.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "samaj_chat_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
public class ChatParticipant {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private ChatConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 16)
    private String role; // MEMBER, ADMIN

    private boolean muted;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @Column(name = "joined_at")
    private Instant joinedAt;

    protected ChatParticipant() {}

    public ChatParticipant(ChatConversation conversation, User user, String role) {
        this.id = UUID.randomUUID();
        this.conversation = conversation;
        this.user = user;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public ChatConversation getConversation() { return conversation; }
    public User getUser() { return user; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    public Instant getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(Instant lastReadAt) { this.lastReadAt = lastReadAt; }
    public Instant getJoinedAt() { return joinedAt; }
}
